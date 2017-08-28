package org.keyspring.poc.spring.vault.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Subhankar on 8/14/2017.
 */

@Data
public class Secrets implements Serializable {
    //Pojo class
    String username;
    String password;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
