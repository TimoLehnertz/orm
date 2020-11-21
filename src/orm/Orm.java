package orm;

import java.util.List;

import sqlMagic.DbConnector;
import sqlMagic.Select;

public class Orm {

	/**
	 * States
	 */
	public static final int READ_ONLY = 0;
	public static final int READ_WRITE = 1;

	/**
	 * Simplified acces to DbConnector Object
	 */
	private static DbConnector db = DbConnector.getInstance();
	
	/**
	 * Better logging for eleminating dublicate logs and enabling log level option
	 */
	public static Logger logger = new Logger();
	
	/**
	 * Select all from type and return them
	 * @param <T> Type
	 * @param from Type
	 * @return list
	 */
	public <T extends Entity<?>> List<T> selectAll(Class<T> from){
		Select<T> select = new Select<>(from);
		select.query();
		return select.getResult();
	}
	
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
	
	public static boolean dropDatabase() {
		logger.info("Dropping Database \"" + db.getDbName() + "\"");
		return db.execute("DROP DATABASE IF EXISTS " + db.getDbName() + ";");
	}
	
	@SafeVarargs
	public static boolean initTables(Class<? extends Entity<?>> ... entities) {
		return OrmUtils.initTables(entities);
	}
	
	public static boolean isEntityValidForSafe(Entity<?> e) {
		return OrmUtils.isEntityValidForSave(e);
	}
	
	public static int save(Entity<?> entity) {
		return OrmUtils.saveEntity(entity, null, 0);
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
	
	public static int getMode() {
		return OrmUtils.state;
	}

	public static void setmode(int state) {
		OrmUtils.state = state;
	}
}
