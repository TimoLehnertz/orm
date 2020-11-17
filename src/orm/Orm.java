package orm;

public class Orm {

	/**
	 * Simplified acces to DbConnector Object
	 */
	private static DbConnector db = DbConnector.getInstance();
	
	/**
	 * Better logging for eleminating dublicate logs and enabling log level option
	 */
	public static Logger logger = new Logger();
	
	/**
	 * 
	 * @param host URL to database server default: localhost
	 * @param username to database server default: root
	 * @param password to database server default: <empty>
	 * @param port 	   to database server default: 3306
	 */
	public static void initDb(String host, String username, String password, String dbName,  int port) {
		setDbUrl(host);
		setDbUser(username);
		setDbPassowrd(password);
		setDbName(dbName);
		setDbPort(port);
	}
	
	public static void initDb(String host, String username, String password, String dbName) {
		initDb(host, username, password, dbName,  3306);
	}
	
	public static void initDb(String host, String username, String password) {
		initDb(host, username, password, "test");
	}
	
	
	@SafeVarargs
	public static boolean initTables(Class<? extends Entity<?>> ... entities) {
		return OrmUtils.initTables(entities);
	}
	
	
	/**
	 * Getters /  Setters
	 */
	public static String getDbUser() {
		return db.getDbUser();
	}

	public static void setDbUser(String dbUser) {
		db.setDbUser(dbUser);
	}

	public static String getDbPassowrd() {
		return db.getDbPassowrd();
	}

	public static void setDbPassowrd(String dbPassowrd) {
		db.setDbPassowrd(dbPassowrd);
	}

	public static String getDbName() {
		return db.getDbName();
	}

	public static void setDbName(String dbName) {
		db.setDbName(dbName);
	}

	public static int getDbPort() {
		return db.getDbPort();
	}

	public static void setDbPort(int dbPort) {
		db.setDbPort(dbPort);
	}

	public static void setDbUrl(String dbUrl) {
		db.setDbUrl(dbUrl);
	}

	public static void setDebug(boolean degug) {
		db.setDebugMode(degug);
	}
}
