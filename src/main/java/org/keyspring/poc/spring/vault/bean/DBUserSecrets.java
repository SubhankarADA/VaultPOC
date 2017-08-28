package org.keyspring.poc.spring.vault.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Subhankar on 8/14/2017.
 */

@Data
public class DBUserSecrets implements Serializable {

    String db_name;
    String creation_statements;
    String default_ttl;
    String max_ttl;
    //Pojo class
    public String getDb_name() {
        return db_name;
    }

    public void setDb_name(String db_name) {
        this.db_name = db_name;
    }

    public String getCreation_statements() {
        return creation_statements;
    }

    public void setCreation_statements(String creation_statements) {
        this.creation_statements = creation_statements;
    }

    public String getDefault_ttl() {
        return default_ttl;
    }

    public void setDefault_ttl(String default_ttl) {
        this.default_ttl = default_ttl;
    }

    public String getMax_ttl() {
        return max_ttl;
    }

    public void setMax_ttl(String max_ttl) {
        this.max_ttl = max_ttl;
    }
}
