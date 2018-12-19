package db;

import db.mongodb.MongoDBConnection;
import db.mysql.MySQLConnection;

//create different db instances
public class DBConnectionFactory {
	// This should change based on the pipeline.
	private static final String DEFAULT_DB = "mongodb";
	public static DBConnection getConnection(String db) {
		switch (db) {
		case "mysql":
			return new MySQLConnection();
		case "mongodb":
			return new MongoDBConnection();
		default:
			throw new IllegalArgumentException("Invalid db:" + db);
		}
	}
	//overloading for connection 
	//with specified default database connection
	public static DBConnection getConnection() {
		return getConnection(DEFAULT_DB);
	}
}

