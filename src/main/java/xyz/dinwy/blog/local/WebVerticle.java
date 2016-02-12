package xyz.dinwy.blog.local;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
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
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.FaviconHandler;
import io.vertx.ext.web.handler.RedirectAuthHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.UserSessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class WebVerticle extends AbstractVerticle {
	@Override
	public void start(Future<Void> fut) throws Exception{
		
		HttpServerOptions serverOptions = new HttpServerOptions();
		serverOptions.setCompressionSupported(true);
		HttpServer server = vertx.createHttpServer(serverOptions);
		Router router = Router.router(vertx);
		JDBCClient client = JDBCVerticle.getJDBCinfo(vertx);
		
		router.route().handler(CookieHandler.create());
	    router.route().handler(BodyHandler.create());
	    router.route().handler(SessionHandler
	        .create(LocalSessionStore.create(vertx))
//	        .setCookieHttpOnlyFlag(false)
//	        .setCookieSecureFlag(false)
	    );

	    JsonObject config = new JsonObject().put("properties_path", "misc/vertx-users.properties");
	    ShiroAuthOptions SAO = new ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES);
	    AuthProvider authProvider = ShiroAuth.create(vertx, SAO);
	    router.route().handler(UserSessionHandler.create(authProvider));

	    router.route("/about").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.setChunked(true);
			if(!response.headWritten())
			response.putHeader("content-type", "text/html")
					.setStatusCode(200)
					.sendFile("webroot/view/about.html");
		});
	    
//	    redirectAuthHandler Setting
	    AuthHandler redirectAuthHandler = RedirectAuthHandler.create(authProvider,"/signin");
	    router.route("/signin").handler(routingContext -> {
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html")
					.setStatusCode(200)
					.sendFile("webroot/view/signin.html");
		});
	    
	    //redirect every connection to admin/*
	    router.route("/admin/*").handler(redirectAuthHandler);
	    router.route("/webroot/*").handler(routingContext ->{
	    	routingContext.response().putHeader("location", "/").setStatusCode(301).end();
	    });
	    
	    router.route("/admin/dashboard").handler(routingContext -> {
	    	System.out.println("admin/dashboard");
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html")
					.sendFile("webroot/admin/dashboard.html");
		});
	    
	    router.route("/admin/posts").handler(routingContext -> {
	    	System.out.println("admin/posts");
			HttpServerResponse response = routingContext.response();
			response.putHeader("content-type", "text/html")
					.sendFile("webroot/admin/posts.html");
		});
	    
		router.route().handler(FaviconHandler.create("webroot/favicon.ico", 3868000));
		router.route("/").handler(StaticHandler.create("webroot").setMaxAgeSeconds(3868000));
		router.route("/view/*").handler(StaticHandler.create().setWebRoot("webroot/view").setCachingEnabled(true));
		router.route("/static/*").handler(StaticHandler.create("webroot/static").setMaxAgeSeconds(3868000));
	    router.post("/api/signin").handler(routingContext -> {
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
	        context.response().putHeader("location", "/").setStatusCode(301).end();
	    });

	    router.get("/api/userInfo").handler(this::getUserInfo);
	    router.get("/announcements").handler(this::redirectToABB);
	    router.get("/announcements/:num/:subject").handler(this::redirectMessageBB);
	    router.get("/api/posts").handler(this::getPosts);
	    router.get("/api/posts/count").handler(this::countPosts);
	    router.get("/api/posts/:num/:subject").handler(this::redirectMessageBB);
	    router.get("/api/posts/:num").handler(this::getOneMessageBB);
	    router.post("/api/posts").handler(this::addMessageBB);
	    router.route("/webroot/*").handler(StaticHandler.create().setWebRoot("webroot").setCachingEnabled(true));
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
	
	private void redirectToABB(RoutingContext routingContext) {
    	routingContext.reroute("/api/bulletinBoard");
	}
	
	private void getUserInfo(RoutingContext routingContext) {
		System.out.println("getUserInfo called!");
		routingContext.response()
					  .putHeader("content-type", "application/json; charset=utf-8")
					  .end(Json.encodePrettily(routingContext.session().get("userInfo")));
	}

	private void countPosts(RoutingContext routingContext) {
		System.out.println("getMessage called!");
		JDBCClient client = JDBCVerticle.getJDBCinfo(vertx);
		PostsDAO MBDAO = new PostsDAO();
		MBDAO.countPosts(vertx, client, routingContext);
	}
	
	private void getPosts(RoutingContext routingContext) {
		System.out.println("GET POSTS");
		String pageNum = routingContext.request().getParam("page");
		if(pageNum == null){
			pageNum = "1";
		};
		System.out.println("getPosts called!");
		JDBCClient client = JDBCVerticle.getJDBCinfo(vertx);
		PostsDAO PostsDAO = new PostsDAO();
		PostsDAO.getPosts(vertx, client, routingContext, pageNum);
	}

	private void redirectMessageBB(RoutingContext routingContext) {
		HttpServerResponse response = routingContext.response();
		response.putHeader("content-type", "text/html;")
				.sendFile("webroot/view/boardData.html");
	}
	
	private void getOneMessageBB(RoutingContext routingContext) {
		String num = routingContext.request().getParam("num");
		System.out.println("getOneMessage called!");
		JDBCClient client = JDBCVerticle.getJDBCinfo(vertx);
		PostsDAO MBDAO = new PostsDAO();
		MBDAO.getOneMessageBB(vertx, client, num, routingContext);
	}

	private void addMessageBB(RoutingContext routingContext) {
		System.out.println("addBulletinBoard called!");
		System.out.println(routingContext.user().isAuthorised("role:*", res -> {
	    	if (res.succeeded()) {
	    		boolean hasAuthority = res.result();
	    	    if (hasAuthority) {
	    	    	System.out.println("User has a authority BB");
	    	    	String messageData = Json.encodePrettily(routingContext.getBodyAsJson());
	    	    	System.out.println(routingContext.getBodyAsJson());
	    	    	System.out.println(messageData);
	    			JDBCClient client = JDBCVerticle.getJDBCinfo(vertx);
	    			PostsDAO MBDAO = new PostsDAO();
	    			MBDAO.addMessageBB(vertx, client, routingContext.getBodyAsJson(), routingContext);
	    	    } else {
	    	      System.out.println("User does not have a authority NONO");
	    	      routingContext.response().setStatusCode(401)
	    	      .putHeader("content-type", "text/plain; charset=utf-8")
	    	      .end("권한이 없습니다.");
	    	      return;
	    	    }
	        } else {
	    	    res.cause().printStackTrace();
	    	}
	    }));
	}
}