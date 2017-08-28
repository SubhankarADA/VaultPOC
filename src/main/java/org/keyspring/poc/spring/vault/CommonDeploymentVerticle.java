package org.keyspring.poc.spring.vault;

import io.vertx.core.Vertx;

/**
 * Created by Subhankar on 8/21/2017.
 */
public class CommonDeploymentVerticle {

    public static void main(String[] args) {

        Vertx vertx = Vertx.vertx();
        //Deploy all the standard and worker verticles.
        vertx.deployVerticle(new VaultServerRestVerticle());
        vertx.deployVerticle(new VaultServerWorkerVerticle());
    }
}
