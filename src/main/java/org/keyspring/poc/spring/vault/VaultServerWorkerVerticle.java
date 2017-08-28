package org.keyspring.poc.spring.vault;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import org.keyspring.poc.spring.vault.bean.DBSecrets;
import org.keyspring.poc.spring.vault.bean.DBUserSecrets;
import org.keyspring.poc.spring.vault.bean.RenewSecrets;
import org.keyspring.poc.spring.vault.bean.Secrets;
import org.keyspring.poc.spring.vault.config.VaultClientConfiguration;
import org.keyspring.poc.spring.vault.utility.KeySpringUtilily;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.vault.authentication.ClientAuthentication;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.core.VaultTokenTemplate;
import org.springframework.vault.core.VaultTransitOperations;
import org.springframework.vault.support.*;

/**
 * Created by Subhankar on 8/21/2017.
 */

@SpringBootApplication
public class VaultServerWorkerVerticle extends AbstractVerticle {

    ConfigurableApplicationContext context = null;
    VaultTemplate vaultTemplate = null;
    VaultClientConfiguration vaultClientConfiguration = null;
    String keyToSealUnseal = null;
    VaultResponseSupport<Secrets> response = null;

    @Override
    public void start() {
        context = SpringApplication.run(VaultServerWorkerVerticle.class);
        context.start();

        vaultTemplate = context.getBean(VaultTemplate.class);
        vaultClientConfiguration = context.getBean(VaultClientConfiguration.class);
        vaultTemplate = vaultInitialize(vaultTemplate, vaultClientConfiguration);

        //creating mysql db connection
        createDBConnection();

        vertx.eventBus().consumer("/vault/sendresponse", this::receivedDataFromApp);
        vertx.eventBus().consumer("/vault/sendresponsecreateuser", this::receivedDataToCreateUser);
        vertx.eventBus().consumer("/vault/sendresponserevoke", this::vaultRevoke);
        vertx.eventBus().consumer("/vault/sendresponserenew", this::vaultRenew);
    }

    //method used to create mysql db connection
    private void createDBConnection() {
        vaultUnseal(vaultTemplate);
        DBSecrets dbSecrets = new DBSecrets();
        dbSecrets.setPlugin_name(vaultClientConfiguration.datasourcePlugin_name);
        dbSecrets.setConnection_url(vaultClientConfiguration.datasourceConnection_url);
        dbSecrets.setAllowed_roles(vaultClientConfiguration.datasourceAllowed_roles);
        String path = "database/config/mysql";
        //method called to write data using lease duration
        vaultWriteForDatabaseLease(vaultTemplate, dbSecrets, path);

        DBUserSecrets dbUserSecrets = new DBUserSecrets();
        dbUserSecrets.setDb_name(vaultClientConfiguration.datasourceDb_name);
        dbUserSecrets.setCreation_statements("CREATE USER '{{name}}'@'%' IDENTIFIED BY '{{password}}';GRANT SELECT ON *.* TO '{{name}}'@'%';");
        dbUserSecrets.setDefault_ttl(vaultClientConfiguration.datasourceDefault_ttl);
        dbUserSecrets.setMax_ttl(vaultClientConfiguration.datasourceMax_ttl);
        path = "database/roles/readonly";
        //method called to write data using lease duration
        vaultWriteForDatabaseLease(vaultTemplate, dbUserSecrets, path);
        vaultSeal(vaultTemplate);
    }

    //method used to create and write data using lease duration
    private void vaultWriteForDatabaseLease(VaultTemplate vaultTemplate, Object mySecretData, String path) {
        if (null == vaultTemplate.opsForSys().getMounts().get("database/")) {
            vaultTemplate.opsForSys().mount("database", VaultMount.create("database"));
        }
        vaultTemplate.write(path, mySecretData);
    }

    //request send to vault
    private void receivedDataFromApp(final Message<String> message) {
        Secrets sc = Json.decodeValue(message.body(),Secrets.class);
        sc.setUsername(sc.getUsername());
        sc.setPassword(sc.getPassword());
        sendToVault(sc);
        message.reply("Success");
    }

    //request send to vault
    private void receivedDataToCreateUser(final Message<String> message) {
        String path = "database/creds/readonly";
        //method called to fetch dynamic username and password using lease
        VaultResponseSupport<Secrets> response = vaultLeaseUserCreate(vaultTemplate, path);
        System.out.println("Retrieved data from Vault :: " + response.getData());
        System.out.println("Lease id: "+response.getLeaseId());
        System.out.println("Active for: "+response.getLeaseDuration() +" sec");
        message.reply("Successfully read");
    }

    //used to create user
    private VaultResponseSupport<Secrets>  vaultLeaseUserCreate(VaultTemplate vaultTemplate, String path) {
        vaultUnseal(vaultTemplate);
        response = vaultTemplate.read(path, Secrets.class);
        return response;
    }

    //Send Data to Vault Server to perform operations
    private void sendToVault(Secrets sc) {
        vaultUnseal(vaultTemplate);
        String path = "secret/vault";

        //below methods is used to read and write data without any encryption/decryption policy
        vaultWrite(vaultTemplate, sc, path);
        vaultRead(vaultTemplate, path);

        path = "vaultEncrypt";
        //below methods is used to read and write data with encryption/decryption policy
        String cipherText = encryptData(vaultTemplate, sc, path);
        Secrets secrets = (Secrets) decryptData(vaultTemplate, cipherText, path);

        System.out.println(secrets.getUsername());
        System.out.println(secrets.getPassword());

        //below method is used to seal vault key
        vaultSeal(vaultTemplate);
    }

    //used to fetch default root token and unseal the key assuming there is one key
    private VaultTemplate vaultInitialize(VaultTemplate vaultTemplate, VaultClientConfiguration vaultClientConfiguration) {
        VaultInitializationResponse vaultInitializationResponse = null;
        if (!vaultTemplate.opsForSys().health().isInitialized()) {
            vaultInitializationResponse = vaultTemplate.opsForSys().initialize(VaultInitializationRequest.create(1, 1));
            vaultTemplate = new VaultTemplate(vaultEndpoint(vaultClientConfiguration),
                    clientAuthentication(vaultInitializationResponse.getRootToken().getToken()));

            keyToSealUnseal = vaultInitializationResponse.getKeys().get(0);
        }
        return vaultTemplate;
    }

    //used to re-create vault endpoint
    private VaultEndpoint vaultEndpoint(VaultClientConfiguration vaultClientConfiguration) {
        VaultEndpoint vaultEndpoint = VaultEndpoint.create(vaultClientConfiguration.host, vaultClientConfiguration.port);
        vaultEndpoint.setScheme(vaultClientConfiguration.scheme);
        return vaultEndpoint;
    }

    //used to re-create and fetch dynamically root token
    private ClientAuthentication clientAuthentication(String token) {
        return new TokenAuthentication(token);
    }

    //used to write data in vault
    private void vaultWrite(VaultTemplate vaultTemplate, Secrets mySecretData, String path) {

        if (null == vaultTemplate.opsForSys().getMounts().get("secret/")) {
            vaultTemplate.opsForSys().mount("transit", VaultMount.create("transit"));
        }
        vaultTemplate.write(path, mySecretData);
        System.out.println("Data successfully written to Vault " +path);
    }

    //used to read data in vault
    private void vaultRead(VaultTemplate vaultTemplate, String path) {
        VaultResponseSupport<Secrets> response = vaultTemplate.read(path, Secrets.class);
        System.out.println("Retrieved data from Vault :: " + response.getData().getPassword() + " & " +
                response.getData().getUsername());
    }

    //used to encrypt data in vault
    private String encryptData(VaultTemplate vaultTemplate, Object obj, String path) {
        VaultTransitOperations transitOperations = vaultTemplate.opsForTransit();
        if (null == vaultTemplate.opsForSys().getMounts().get("transit/")) {
            vaultTemplate.opsForSys().mount("transit", VaultMount.create("transit"));
        }
        byte[] data = KeySpringUtilily.convertToBytes(obj);
        if (null == data) {
            System.out.println("Error in Secret");
            return null;
        }

        String cipherText = transitOperations.encrypt(path, data, VaultTransitContext.builder().build());
        System.out.println("Encrypted: " + cipherText);
        return cipherText;
    }

    //used to decrypt data in vault
    private Object decryptData(VaultTemplate vaultTemplate, String cipherText, String path) {
        VaultTransitOperations transitOperations = vaultTemplate.opsForTransit();
        byte[] plaintext = transitOperations.decrypt(path, cipherText,
                VaultTransitContext.builder().build());

        Object obj = KeySpringUtilily.convertFromBytes(plaintext);
        System.out.println(cipherText+" decrypt data "+ obj);
        return obj;
    }

    //used to seal vault data
    private void vaultSeal(VaultTemplate vaultTemplate) {
        vaultTemplate.opsForSys().seal();
    }

    //used to Unseal vault data
    private void vaultUnseal(VaultTemplate vaultTemplate) {
        if (vaultTemplate.opsForSys().getUnsealStatus().isSealed()) {
            vaultTemplate.opsForSys().unseal(keyToSealUnseal);
        }
    }

    //used to revoke current lease
    private void vaultRevoke(final Message<String> message) {
        VaultOperations vaultOperations = vaultTemplate;
        VaultTokenTemplate vaultTokenTemplate = new VaultTokenTemplate(vaultOperations);
        vaultUnseal(vaultTemplate);
        vaultTokenTemplate.write(String.format("/sys/leases/revoke/%s", response.getLeaseId()),
                VaultToken.of(response.getLeaseId()), VaultTokenResponse.class);
        vaultSeal(vaultTemplate);
        message.reply("Successfully revoked");
    }

    //used to renew lease current for about 40sec
    private void vaultRenew(final Message<String> message) {
        VaultOperations vaultOperations = vaultTemplate;
        VaultTokenTemplate vaultTokenTemplate = new VaultTokenTemplate(vaultOperations);
        vaultUnseal(vaultTemplate);

        RenewSecrets object = new RenewSecrets();
        object.setIncrement(40);
        object.setLease_id(response.getLeaseId());

        vaultTokenTemplate.write(String.format("/sys/leases/renew/%s", response.getLeaseId()),
                object, VaultTokenResponse.class);
        message.reply("Successfully renewed");
    }

    @Override
    public void stop() {
        context.stop();
    }
}
