package org.keyspring.poc.spring.vault.bean;

import lombok.Data;

import java.io.Serializable;

/**
 * Created by Subhankar on 8/14/2017.
 */

@Data
public class RenewSecrets implements Serializable {
    //Pojo class
    String lease_id;
    int increment;

    public String getLease_id() {
        return lease_id;
    }

    public void setLease_id(String lease_id) {
        this.lease_id = lease_id;
    }

    public int getIncrement() {
        return increment;
    }

    public void setIncrement(int increment) {
        this.increment = increment;
    }
}
