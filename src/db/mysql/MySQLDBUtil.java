package db.mysql;
//MySQL DBConnection implementation
public class MySQLDBUtil {
	//host address
	private static final String HOSTNAME = "localhost";
	//port for mysql in the host
	private static final String PORT_NUM = "3306"; // change it to your mysql port number
	//database name
	public static final String DB_NAME = "Events";
	private static final String USERNAME = "root";
	private static final String PASSWORD = "root";
	private static final String CHARSET = "useUnicode=true&characterEncoding=utf8";
	public static final String URL = "jdbc:mysql://"
			+ HOSTNAME + ":" + PORT_NUM + "/" + DB_NAME + "?" + CHARSET
			+ "&user=" + USERNAME + "&password=" + PASSWORD
			+ "&autoReconnect=true&serverTimezone=UTC";
}
