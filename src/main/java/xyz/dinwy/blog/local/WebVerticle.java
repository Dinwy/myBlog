package xyz.dinwy.blog.local;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.StaticHandler;

public class WebVerticle extends AbstractVerticle {
	@Override
	public void start(Future<Void> fut) throws Exception{
		
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);

		router.route("/").handler(StaticHandler.create().setWebRoot("webroot").setCachingEnabled(true));
		router.route("/api/*").handler(StaticHandler.create("api").setCachingEnabled(false));

		server.requestHandler(router::accept)
		.listen(Integer.getInteger("http.port", 8080), result -> {
	        if (result.succeeded()) {
	            fut.complete();
	          } else {
	            fut.fail(result.cause());
	          }
        });
	}
}