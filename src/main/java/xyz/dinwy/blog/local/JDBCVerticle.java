package xyz.dinwy.blog.local;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;

public class JDBCVerticle {

	  public static JDBCClient getJDBCinfo(Vertx vertx) {
		  System.out.println("JDBC VERTICLE START");
		  JsonObject config = new JsonObject()
				  .put("url", "jdbc:mysql://localhost:3306/test")
				  .put("driver_class", "com.mysql.jdbc.Driver")
				  .put("max_pool_size", 15)
				  .put("user", "test")
				  .put("password", "test");
			JDBCClient client = JDBCClient.createShared(vertx, config);
			return client;
	  }
}