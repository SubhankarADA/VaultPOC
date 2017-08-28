package org.keyspring.poc.spring.vault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.*;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.config.AbstractVaultConfiguration;
import org.springframework.vault.support.VaultToken;


/**
 * Created by subhankar on 8/14/2017.
 */
@Configuration
public class VaultClientConfiguration extends AbstractVaultConfiguration {

    @Value("${vault.host}")
    public String host;

    @Value("${vault.port}")
    public int port;

    @Value("${vault.scheme}")
    public String scheme;

    @Value("${vault.token}")
    public String token;

    @Value("${spring.datasource.connection_url}")
    public String datasourceConnection_url;

    @Value("${spring.datasource.plugin_name}")
    public String datasourcePlugin_name;

    @Value("${spring.datasource.allowed_roles}")
    public String datasourceAllowed_roles;

    @Value("${spring.datasource.db_name}")
    public String datasourceDb_name;

    @Value("${spring.datasource.default_ttl}")
    public String datasourceDefault_ttl;

    @Value("${spring.datasource.max_ttl}")
    public String datasourceMax_ttl;


    @Override
    public VaultEndpoint vaultEndpoint() {
        VaultEndpoint vaultEndpoint = VaultEndpoint.create(host, port);
        vaultEndpoint.setScheme(scheme);
        return vaultEndpoint;
    }

    @Override
    public ClientAuthentication clientAuthentication() {
        System.out.println("fetch initial token---"+token);
        return new TokenAuthentication(token);
    }
}
