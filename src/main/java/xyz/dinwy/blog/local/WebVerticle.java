package xyz.dinwy.blog.local;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
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
		JDBCClient client = JDBCVerticle.getJDBCinfo(vertx);

	    router.route().handler(CookieHandler.create());
	    router.route().handler(BodyHandler.create());
	    router.route().handler(SessionHandler
	        .create(LocalSessionStore.create(vertx))
	        .setCookieHttpOnlyFlag(false)
	        .setCookieSecureFlag(false)
	    );

	    JsonObject config = new JsonObject().put("properties_path", "misc/vertx-users.properties");
	    ShiroAuthOptions SAO = new ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES);
	    AuthProvider authProvider = ShiroAuth.create(vertx, SAO);
	    router.route().handler(UserSessionHandler.create(authProvider));
//	    ShiroAuth Setting
	    AuthHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider,"/login");
	    
	    router.route("/login").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html;")
					.sendFile("webroot/view/signin.html");
		});
	    
	    router.route("/admin/*").handler(redirectAuthHandler);

	    router.route("/admin/*").handler(StaticHandler.create().setWebRoot("webroot/admin").setCachingEnabled(false));
		router.route("/view/*").handler(StaticHandler.create().setWebRoot("webroot/view").setCachingEnabled(true));
		router.route("/").handler(StaticHandler.create().setWebRoot("webroot").setCachingEnabled(true));
		router.route("/static/*").handler(StaticHandler.create().setWebRoot("webroot/static").setCachingEnabled(true));
		router.route("/api/*").handler(StaticHandler.create("api").setCachingEnabled(false));
	    
	    router.post("/signin").handler(routingContext -> {
			HttpServerRequest request = routingContext.request();
			JsonObject userInfo = new JsonObject();
			userInfo.put("ID", request.getFormAttribute("ID"));
			userInfo.put("PSW", request.getFormAttribute("PSW"));
			System.out.println(Json.encodePrettily(userInfo) + " json userinfo");
			//https://github.com/vert-x3/vertx-examples/blob/master/web-examples/src/main/java/io/vertx/example/web/angular_realtime/Server.java
			UserDAO userDAO = new UserDAO();
			userDAO.certify(vertx, client, routingContext, userInfo.getString("ID"), userInfo.getString("PSW"));
		});
	    
	    router.route("/logout").handler(context -> {
	        context.clearUser();
	        context.session().destroy();
	        // Redirect back to the index page
	        context.response().putHeader("location", "/").setStatusCode(302).end();
	    });

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