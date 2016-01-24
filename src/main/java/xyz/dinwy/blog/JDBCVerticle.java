package xyz.dinwy.blog;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class JDBCVerticle {

	  public static JDBCClient getJDBCinfo(Vertx vertx) {
		  System.out.println("JDBC VERTICLE START");
		  JsonObject config = new JsonObject()
				  .put("url", "jdbc:mysql://127.2.159.2:3306/blog")
				  .put("driver_class", "com.mysql.jdbc.Driver")
				  .put("max_pool_size", 15)
				  .put("user", "blogAdmin")
				  .put("password", "ilovecat1!");
			JDBCClient client = JDBCClient.createShared(vertx, config);
			return client;
	  }
}