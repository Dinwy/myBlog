package xyz.dinwy.blog.local;

import java.util.List;

import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.RoutingContext;

public class PostsDAO{

	public void countPosts(Vertx vertx,JDBCClient client,RoutingContext routingContext){
		client.getConnection(res -> {
		if(res.succeeded()) {
			SQLConnection connection = res.result();
		    String query = "SELECT count(*) FROM test.bulletin_board;";
		    connection.query(query, res2-> {
			    if(res2.failed()) {
			    	 sendError(500, routingContext.response());
			    	 System.out.println(res2.cause().getMessage());
			    	 System.out.println("Connection failed!");
			    }else if(res2.succeeded()){
		    		System.out.println("Connection succeed");
			    	ResultSet rs = res2.result();
			    	String messageCount = rs.getResults().get(0).getLong(0).toString();
//			    	System.out.println(rs.getResults().get(0).getLong(0) +" rs.tosing data");
		    		routingContext.response()
					.setStatusCode(200)
			        .putHeader("content-type", "text/plain; charset=utf-8")
					.end(messageCount);
	    		}
//		    	!!important!! Don't forget to close connection!
//		    	If you forgot to write this, it will make JDBC pool full.
		    	connection.close();
		    	return;
			    });
	    	}
	    });		
	}
	
	public void getPosts(Vertx vertx,JDBCClient client,RoutingContext routingContext, String pageNum){
		System.out.println("getPosts Start");
		client.getConnection(res -> {
		if(res.succeeded()) {
			SQLConnection connection = res.result();
		    String query = "SELECT * FROM test.bulletin_board order by no desc limit " + (Long.parseLong(pageNum)*5-5) + ",5;";
		    connection.query(query, res2-> {
			    if(res2.failed()) {
			    	 sendError(500, routingContext.response());
			    	 System.out.println(res2.cause().getMessage());
			    	 System.out.println("Connection failed!");
			    }else if(res2.succeeded()){
		    		System.out.println("Connection succeed");
			    	ResultSet rs = res2.result();
			    	List<JsonArray> results = rs.getResults();
			    	System.out.println(Json.encodePrettily(results) + "json data");
		    		routingContext.response()
					.setStatusCode(200)
			        .putHeader("content-type", "application/json; charset=utf-8")
					.end(Json.encodePrettily(results));
	    		}
//		    	!!important!! Don't forget to close connection!
//		    	If you forgot to write this, it will make JDBC pool full.
		    	connection.close();
		    	return;
			    });
	    	}
	    });
	  }

	public void getOneMessageBB(Vertx vertx,JDBCClient client, String num, RoutingContext routingContext){
		System.out.println("getOneMessage from BB");
		if(routingContext.user()==null){
			routingContext.response()
			.setStatusCode(401)
			.putHeader("content-type", "text/plain; charset=utf-8")
			.end("ERROR 401, 로그인 이후 이용해 주세요.");
			return;
		};
		client.getConnection(res -> {
		if(res.succeeded()) {
			SQLConnection connection = res.result();
		    String query = "SELECT * FROM bulletin_board where bulletin_board.no = ?";
		    JsonArray params = new JsonArray()
		    					.add(Integer.parseInt(num));
		    connection.queryWithParams(query,params,res2 -> {
			    if(res2.failed()) {
			    	 sendError(500, routingContext.response());
			    	 System.out.println(res2.cause().getMessage());
			    	 System.out.println("Connection failed!");
			    }else if(res2.succeeded()){
		    		System.out.println("Connection succeed");
			    	ResultSet rs = res2.result();
			    	if(rs.getNumRows() == 0){
			    		routingContext.response()
			    			.setStatusCode(404)
			    			.putHeader("content-type", "text/plain; charset=utf-8")
			    			.end("요청하신 페이지를 찾을 수 없습니다.");
			    		connection.close();
			    		return;
			    	};
			    	List<JsonArray> results = rs.getResults();
			    	System.out.println(Json.encodePrettily(results) + "json data");
		    		routingContext.response()
					.setStatusCode(200)
			        .putHeader("content-type", "text/html; charset=utf-8")
					.end(Json.encodePrettily(results));
	    		}
//		    	!!important!! Don't forget to close connection!
//		    	If you forgot to write this, it will make JDBC pool full.
		    	connection.close();
		    	return;
			    });
	    	}
	    });
	  }
	
	public void addMessageBB(Vertx vertx,JDBCClient client,JsonObject messageData, RoutingContext routingContext){
		  client.getConnection(res -> {
		  if(res.succeeded()) {
			    SQLConnection connection = res.result();
			    String updatequery = "insert into bulletin_board(writer,subject,message,datetime)"
			    		+ " values(?,?,?,now())";
			    JsonArray params = new JsonArray()
			    					.add(messageData.getString("userName"))
			    					.add(messageData.getString("title"))
			    					.add(messageData.getString("body"));
			    System.out.println(params);
			    connection.updateWithParams(updatequery,params,res2 -> {
				    if(res2.failed()) {
				    	String errorMessage = res2.cause().getMessage();
				    	if(errorMessage.contains("for key 'PRIMARY'")){
				    		  routingContext.response()
				    		  	.setStatusCode(400).end("도배 금지");
				    	}else if(errorMessage.contains("nickname_UNIQUE")){
				    		  routingContext.response()
				    		  	.setStatusCode(400).end("중복된 닉네임입니다. 다른 닉네임을 입력해 주세요.");
				    	}else{
				    		System.out.println(errorMessage);
			    		  routingContext.response()
			    		  	.setStatusCode(400).end("ERROR!!!");
				    	}
			    		connection.close();
			    		return;
				    }else if(res2.succeeded()){
			    		System.out.println("Connection succeed");
			    		routingContext.response().setStatusCode(201)
			    		.putHeader("content-type", "text/plain; charset=utf-8").end("공지 추가 완료!");
				    }
			    });
		    	//!!important!! Don't forget to close connection!
		    	//if not, it will cause JDBC pool full.
		    	connection.close();
		    	return;
		  }
		});
	  }
	
	private void sendError(int statusCode, HttpServerResponse response) {
		  response.setStatusCode(statusCode).end();
    }
}