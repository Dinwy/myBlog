package xyz.dinwy.blog;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class WebVerticle extends AbstractVerticle {
	@Override
	public void start(Future<Void> fut) throws Exception{
		
//		HttpServerOptions serverOptions = new HttpServerOptions().setMaxWebsocketFrameSize(1000000);
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);

		router.route("/").handler(StaticHandler.create().setWebRoot("../app-root/repo/webroot").setCachingEnabled(true));
		router.route("/static/*").handler(StaticHandler.create().setWebRoot("../app-root/repo/webroot/static").setCachingEnabled(true));
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