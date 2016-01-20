package xyz.dinwy.blog.local;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.FormLoginHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class WebVerticle extends AbstractVerticle {
	@Override
	public void start(Future<Void> fut) throws Exception{
		
		HttpServer server = vertx.createHttpServer();
		Router router = Router.router(vertx);
		
	    router.route().handler(CookieHandler.create());
	    router.route().handler(BodyHandler.create());
	    router.route().handler(SessionHandler
	        .create(LocalSessionStore.create(vertx))
	        .setCookieHttpOnlyFlag(true)
	        .setCookieSecureFlag(true)
	    );

	    JsonObject config = new JsonObject().put("properties_path", "misc/vertx-users.properties");
	    ShiroAuthOptions SAO = new ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES);
	    AuthProvider authProvider = ShiroAuth.create(vertx, SAO);
	    router.route().handler(UserSessionHandler.create(authProvider));
//	    ShiroAuth Setting
	    AuthHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider,"/private/signin.html");

	    router.route("/private/").handler(redirectAuthHandler);
	    router.route("/private/*").handler(StaticHandler.create().setWebRoot("webroot/view").setCachingEnabled(false));
	    router.route("/loginhandler").handler(FormLoginHandler.create(authProvider));
	    
		router.route("/").handler(StaticHandler.create().setWebRoot("webroot").setCachingEnabled(true));
		router.route("/static/*").handler(StaticHandler.create().setWebRoot("webroot/static").setCachingEnabled(true));
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