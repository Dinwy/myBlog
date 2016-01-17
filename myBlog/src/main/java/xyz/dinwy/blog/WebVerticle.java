package xyz.dinwy.blog;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class WebVerticle extends AbstractVerticle {
	@Override
	public void start(Future<Void> fut) throws Exception{
		
		HttpServerOptions serverOptions = new HttpServerOptions().setMaxWebsocketFrameSize(1000000);
		HttpServer server = vertx.createHttpServer(serverOptions);
		Router router = Router.router(vertx);
		router.route("/").handler(routingContext -> {
			  HttpServerResponse response = routingContext.response();
		      response
		        .putHeader("content-type", "text/html")
				.sendFile("../app-root/repo/webroot/index.html");
		});

		router.route("/webroot/*").handler(StaticHandler.create("../app-root/repo/webroot").setCachingEnabled(false));
		router.route("/api/*").handler(StaticHandler.create("api").setCachingEnabled(false));

		server.requestHandler(router::accept)
		.listen(Integer.getInteger("http.port", 8080), System.getProperty("http.address"),result -> {
	        if (result.succeeded()) {
	            fut.complete();
	          } else {
	            fut.fail(result.cause());
	          }
        });
	}
}