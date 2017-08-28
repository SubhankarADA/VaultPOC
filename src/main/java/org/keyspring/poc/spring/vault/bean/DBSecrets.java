package org.keyspring.poc.spring.vault.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Subhankar on 8/14/2017.
 */

@Data
public class DBSecrets implements Serializable {


    String plugin_name;
    String connection_url;
    String allowed_roles;
    //Pojo class
    public String getPlugin_name() {
        return plugin_name;
    }

    public void setPlugin_name(String plugin_name) {
        this.plugin_name = plugin_name;
    }

    public String getConnection_url() {
        return connection_url;
    }

    public void setConnection_url(String connection_url) {
        this.connection_url = connection_url;
    }

    public String getAllowed_roles() {
        return allowed_roles;
    }

    public void setAllowed_roles(String allowed_roles) {
        this.allowed_roles = allowed_roles;
    }
}
