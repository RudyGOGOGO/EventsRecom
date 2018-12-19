package db.mysql;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.sql.ResultSet;
import db.DBConnection;
import entity.Item;
import entity.Item.ItemBuilder;
import external.TicketMasterAPI;

public class MySQLConnection implements DBConnection {
	private Connection conn;
	//build up the connection
	public MySQLConnection() {
		try {
			//Class.forName() would not result in exception
			Class.forName("com.mysql.cj.jdbc.Driver").
			getConstructor().newInstance();
			//getConnection would result in exception
			conn = DriverManager.getConnection(MySQLDBUtil.URL);
		} catch (Exception e) {
			/*
			 * step 1: record the log
			 * step 2: check whether the exception can be solved
			 * if we cannot solve it(like system exception, 
			 * the database service is stopped),
			 * we should tell the user "try it later"
			 * */
			e.printStackTrace();
		}
	}
	@Override
	public void close() {
		//try to close the connection if it is not null
		if (conn == null) {
			System.out.println("DB Connection is null");
			return;
		}
		try {
			conn.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			System.out.println("DB Connection is null");
			return;
		}
		try {
			String sql = "INSERT IGNORE INTO history(user_id,item_id) VALUES(?,?)";
			PreparedStatement pStatement = conn.prepareStatement(sql);
			pStatement.setString(1, userId);
			for (String itemId : itemIds) {
				pStatement.setString(2, itemId);
				pStatement.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void unsetFavoriteItems(String userId, List<String> itemIds) {
		if (conn == null) {
			System.out.println("DB Connection is null");
			return;
		}
		try {
			String sql = "DELETE FROM history WHERE user_id = ? AND item_id = ?";
			PreparedStatement pStatement = conn.prepareStatement(sql);
			pStatement.setString(1, userId);
			for (String itemId : itemIds) {
				pStatement.setString(2, itemId);
				pStatement.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public Set<String> getFavoriteItemIds(String userId) {
		if (conn == null) {
			System.out.println("DB Connection is null");
			return new HashSet<>();//user does not need to handle null
		}
		Set<String> favoriteItems = new HashSet<>();
		try {
			//retrieve all favorite items_id based on the user_id
			String sql = "SELECT item_id FROM history WHERE user_id = ?";
			PreparedStatement pStatement = conn.prepareStatement(sql);
			pStatement.setString(1, userId);
			ResultSet rSet = pStatement.executeQuery();
			while (rSet.next()) {
				String itemId = rSet.getString("item_id");
				favoriteItems.add(itemId);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}

	@Override
	public Set<Item> getFavoriteItems(String userId) {
		if (conn == null) {
			return new HashSet<>();
		}
		Set<Item> favoriteItems = new HashSet<>();
		Set<String> itemIds = getFavoriteItemIds(userId);
		try {
			String sql = "SELECT * FROM items WHERE item_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			for (String itemId : itemIds) {
				stmt.setString(1, itemId);
				ResultSet rSet = stmt.executeQuery();
				ItemBuilder builder = new ItemBuilder();
				/**
				 * considering that rSet is the container
				 * therefore, even though we know stmt.executeQuery()
				 * always return only one record based on the specified
				 * query, we should also use while-loop in case of 
				 * extensibility
				 */
				while (rSet.next()) {
					builder.setItemId(rSet.getString("item_id"));
					builder.setName(rSet.getString("name"));
					builder.setAddress(rSet.getString("address"));
					builder.setImageUrl(rSet.getString("image_url"));
					builder.setUrl(rSet.getString("url"));
					//one itemId might have several corresponding categories
					builder.setCategories(getCategories(itemId));
					builder.setDistance(rSet.getDouble("distance"));
					builder.setRating(rSet.getDouble("rating"));
					favoriteItems.add(builder.build());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return favoriteItems;
	}
	
	@Override
	public Set<String> getCategories(String itemId) {
		if (conn == null) {
			return null;
		} 
		Set<String> categories = new HashSet<>();
		try {
			String sql = "SELECT category from categories WHERE item_id = ?";
			PreparedStatement pStatement = conn.prepareStatement(sql);
			pStatement.setString(1, itemId);
			ResultSet rSet = pStatement.executeQuery();
			while (rSet.next()) {
				categories.add(rSet.getString("category"));
			}
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
		return categories;
	}
	/*
	 * Previously we call TicketMasterAPI.search from our SearchItem 
	 * servlet directly. But actually out recommendation code also
	 * needs to call the same search function, so we make a designated function
	 * here to do the search call
	 * 
	 * */
	@Override
	public List<Item> searchItems(double lat, double lon, String term) {
		TicketMasterAPI api = new TicketMasterAPI();
		List<Item> items = api.search(lat, lon, term);
		for (Item item : items) {
			saveItem(item);
		}
		return items;
	}

	@Override
	public void saveItem(Item item) {
		if (conn == null) {
			System.out.println("DB connection failed");
			return;
		}
		try {
			//add item info into the table items
			String sql = "INSERT IGNORE INTO items VALUES(?,?,?,?,?,?,?)";
			PreparedStatement pStatement = conn.prepareStatement(sql);// in case of sql injection
			pStatement.setString(1, item.getItemId());
			pStatement.setString(2, item.getName());
			pStatement.setDouble(3, item.getRating());
			pStatement.setString(4, item.getAddress());
			pStatement.setString(5, item.getImageUrl());
			pStatement.setString(6, item.getUrl());
			pStatement.setDouble(7, item.getDistance());
			pStatement.execute();
			//add all categories info into the table categories
			sql = "INSERT IGNORE INTO categories VALUES(?,?)";//IGNORE for de-duplication
			pStatement = conn.prepareStatement(sql);
			pStatement.setString(1, item.getItemId());
			for (String category : item.getCategories()) {
				pStatement.setString(2, category);
				pStatement.execute();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getFullname(String userId) {
		if (conn == null) {
			return "";
		}
		
		String name = "";
		
		try {
			String sql = "SELECT first_name, last_name FROM users WHERE user_id = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				name = String.join(" ", rs.getString("first_name"), rs.getString("last_name"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return name;

	}

	@Override
	public boolean verifyLogin(String userId, String password) {
		if (conn == null) {
			return false;
		}
		
		try {
			String sql = "SELECT user_id FROM users WHERE user_id = ? AND password = ?";
			PreparedStatement stmt = conn.prepareStatement(sql);
			stmt.setString(1, userId);
			stmt.setString(2, password);
			ResultSet rs = stmt.executeQuery();
			
			while (rs.next()) {
				return true;
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return false;

	}
	@Override
	public boolean createTable() {
		try {
			/**
			 * Step 1 Connect to MySQL.
			 */
			System.out.println("Connecting to " + MySQLDBUtil.URL);
			/*
			 * Class.forName("com.mysql.cj.jdbc.Driver"):
			 * loads the class com.mysql.cj.jdbc.Driver into JVM to have
			 * one Drive class instance registered 
			 * so that The <code>DriverManager</code> could attempt to 
			 * select an appropriate driver from the set of registered JDBC drivers
			 * 
			 * as for version older than Java 1.7
			 * .getConstructor().newInstance()
			 * guarantee that JVM would implement the static blocks
			 * */
			Class.forName("com.mysql.cj.jdbc.Driver").getConstructor().newInstance();
			Connection conn = DriverManager.getConnection(MySQLDBUtil.URL);
			if (conn == null) {
				return false;
			}
			/**
			 * Step 2 Drop tables in case they have existed
			 * DROP TABLE IF EXISTS + table_name
			 * When there is foreign key, 
			 * we should drop the table with foreign key at first
			 */
			Statement stmt = conn.createStatement();
			String sql = "DROP TABLE IF EXISTS categories";//with foreign keys
			stmt.executeUpdate(sql);
			sql = "DROP TABLE IF EXISTS history";//with foreign keys
			stmt.executeUpdate(sql);
			sql = "DROP TABLE IF EXISTS items";
			stmt.executeUpdate(sql);
			sql = "DROP TABLE IF EXISTS users";
			stmt.executeUpdate(sql);
			/**
			 *  Step 3 Create new tables
			 *  CREATE TABLE table_name (
			 *  	column1 data_type,
			 *  	column2 data_type,
			 *  	column3 data_type,
			 *  )
			 */
			sql = "CREATE TABLE items (" 
				  + "item_id VARCHAR(255) NOT NULL,"
				  + "name VARCHAR(255),"
				  + "rating FLOAT,"
				  + "address VARCHAR(255),"
				  + "image_url VARCHAR(255),"
				  + "url VARCHAR(255),"
				  + "distance FLOAT,"
				  + "PRIMARY KEY (item_id)"
				  + ")";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE users (" 
					  + "user_id VARCHAR(255) NOT NULL,"
					  + "password VARCHAR(255) NOT NULL,"
					  + "first_name VARCHAR(255),"
					  + "last_name VARCHAR(255),"
					  + "PRIMARY KEY (user_id)"
					  + ")";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE categories (" 
					  + "item_id VARCHAR(255) NOT NULL,"
					  + "category VARCHAR(255) NOT NULL,"
					  + "PRIMARY KEY (item_id, category),"
					  + "FOREIGN KEY (item_id) REFERENCES items(item_id)"
					  + ")";
			stmt.executeUpdate(sql);
			sql = "CREATE TABLE history (" 
					  + "user_id VARCHAR(255) NOT NULL,"
					  + "item_id VARCHAR(255) NOT NULL,"
					  + "last_favor_item TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
					  + "PRIMARY KEY (user_id, item_id),"
					  + "FOREIGN KEY (user_id) REFERENCES users(user_id),"
					  + "FOREIGN KEY (item_id) REFERENCES items(item_id)"
					  + ")";
			stmt.executeUpdate(sql);
			/**
			 * Step 4 Insert fake users
			 * with encrypted password
			 */
			sql = "INSERT INTO users VALUES ("
				  + "'1111', '3229c1097c00d497a0fd282d586be050', 'John', 'Smith')";
			stmt.executeUpdate(sql);
			System.out.println("Import done successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

}
