package org.keyspring.poc.spring.vault;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * Created by Subhankar on 8/21/2017.
 */
public class VaultServerRestVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {

        Router router = Router.router(vertx);

        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            response.putHeader("content-type", "text/html");
        });

        //Endpoint to be triggered from a different application
        router.route("/vault/requestreceive*").handler(BodyHandler.create());
        router.post("/vault/requestreceive").handler(this::requestReceive);
        router.post("/vault/requestreceivecreateuser").handler(this::requestReceiveCreateUser);
        router.post("/vault/requestreceiverevoke").handler(this::revokeCurrentUser);
        router.post("/vault/requestreceiverenew").handler(this::renewCurrentUser);

        vertx
            .createHttpServer()
            .requestHandler(router::accept)
            .listen(
                    // Retrieve the port from the configuration,
                    // default to 8080.
                    config().getInteger("http.port", 8082),
                    result -> {
                        if (result.succeeded()) {
                            fut.complete();
                        } else {
                            fut.fail(result.cause());
                        }
                    }
            );
    }

    //send receive to and from worker verticle
    private void requestReceive(RoutingContext routingContext) {
        String data = routingContext.getBodyAsString();
        vertx.eventBus().send("/vault/sendresponse",data, resp -> {
            if (resp.succeeded()) {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end("Success");
            } else {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.GATEWAY_TIMEOUT.code()).end("ERROR..");
            }
        });

    }

    //send receive to and from worker verticle
    private void requestReceiveCreateUser(RoutingContext routingContext) {
        String data = routingContext.getBodyAsString();
        vertx.eventBus().send("/vault/sendresponsecreateuser",data, resp -> {
            if (resp.succeeded()) {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end("Successfully creating user in mysql");
            } else {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.GATEWAY_TIMEOUT.code()).end("ERROR in creating user");
            }
        });
    }

    //send receive to and from worker verticle
    private void revokeCurrentUser(RoutingContext routingContext) {
        String data = routingContext.getBodyAsString();
        vertx.eventBus().send("/vault/sendresponserevoke",data, resp -> {
            if (resp.succeeded()) {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end("Successfully revoked");
            } else {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.GATEWAY_TIMEOUT.code()).end("ERROR in revoking lease");
            }
        });
    }

    //send receive to and from worker verticle
    private void renewCurrentUser(RoutingContext routingContext) {
        String data = routingContext.getBodyAsString();
        vertx.eventBus().send("/vault/sendresponserenew",data, resp -> {
            if (resp.succeeded()) {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end("Successfully renewed");
            } else {
                routingContext.response().putHeader ("content-type", "text/plain");
                routingContext.response().setStatusCode(HttpResponseStatus.GATEWAY_TIMEOUT.code()).end("ERROR in renewing lease");
            }
        });
    }
    @Override
    public void stop() {

    }
}
