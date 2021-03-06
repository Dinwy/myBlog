package xyz.dinwy.blog;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.shiro.ShiroAuth;
import io.vertx.ext.auth.shiro.ShiroAuthOptions;
import io.vertx.ext.auth.shiro.ShiroAuthRealmType;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

public class UserDAO{

	public void certify(Vertx vertx,JDBCClient client,RoutingContext routingContext, String loginID, String loginPsw){
		System.out.println("User Certify Start");
		client.getConnection(res -> {
		if(res.succeeded()) {
			SQLConnection connection = res.result();
		    String query = "SELECT * FROM user_info where login_id=? and login_psw=sha2(?,256)";
		    JsonArray params = new JsonArray()
		    					.add(loginID)
		    					.add(loginPsw);
		    connection.queryWithParams(query,params,res2 -> {
			    if(res2.failed()) {
			    	 sendError(500, routingContext.response());
			    	 System.out.println(res2.cause().getMessage());
			    	 System.out.println("Connection failed!");
			    }else if(res2.succeeded()){
//		    		System.out.println("Connection succeed");
			    	ResultSet rs = res2.result();
			    	if(rs.getNumRows() == 0){
			    		routingContext.response()
			    			.setStatusCode(401)
			    			.putHeader("content-type", "text/plain; charset=utf-8")
			    			.end("No UserData Exist");
			    		connection.close();
			    		return;
		    	};
		    	JsonObject authInfo = null;
		    	if(loginID.equals("adminAccount")){
		    		authInfo = new JsonObject().put("username", "admin").put("password", "admin");
		    	}else{
		    		authInfo = new JsonObject().put("username", "user").put("password", "user");	
		    	}
			    JsonObject config = new JsonObject().put("properties_path", "../app-root/repo/misc/vertx-users.properties");
			    ShiroAuthOptions SAO = new ShiroAuthOptions().setConfig(config).setType(ShiroAuthRealmType.PROPERTIES);
			    AuthProvider authProvider = ShiroAuth.create(vertx, SAO);
		    	authProvider.authenticate(authInfo, resA -> {
		    		if (resA.succeeded()) {
		    	    User user = resA.result();
		    	    System.out.println("User " + user.principal() + " is now authenticated");
		    	    user.isAuthorised("role:employee", resB -> {
		    	    	if (res.succeeded()) {
		    	    		boolean hasAuthority = resB.result();
		    	    	    if (hasAuthority) {
		    	    	      System.out.println("User has a authority");
		    	    	    } else {
		    	    	      System.out.println("User does not have a authority");
		    	    	    }
		    	        } else {
		    	    	    res.cause().printStackTrace();
		    	    	}
		    	    });
		    		routingContext.setUser(user);
			        routingContext.response().putHeader("location", "/admin/dashboard").setStatusCode(301).end();
		    		} else {
			    	    res.cause().printStackTrace();
			    	  }
			    	});
		    	}
		    });
//	    	!!important!! Don't forget to close connection!
//	    	If you forgot to write this, it will make JDBC pool full.
	    	connection.close();
	    	return;
		  }
		});
	}

	private void sendError(int statusCode, HttpServerResponse response) {
		  response.setStatusCode(statusCode).end();
    }
}