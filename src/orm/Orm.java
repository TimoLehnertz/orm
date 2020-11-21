package orm;

import java.util.List;

import sqlMagic.DbConnector;
import sqlMagic.Delete;
import sqlMagic.Select;

public class Orm {

	/**
	 * Simplified acces to DbConnector singelton
	 */
	private static DbConnector db = DbConnector.getInstance();
	
	/**
	 * Better logging for eleminating dublicate logs and enabling log level option
	 */
	public static Logger logger = new Logger();
	
	
	/**
	 * returns a list of all saved entities
	 * @param <T>
	 * @param type
	 * @return
	 */
	public static <T extends Entity<?>> List<T> selectAll(Class<T> type){
		Select<T> select = new Select(type);
		select.query();
		return select.getResult();
	}
	
	/**
	 * Returns a select Object for the given type
	 * @param <T>
	 * @param type
	 * @return
	 */
	public static <T extends Entity<?>> Select<T> selectFrom(Class<T> type){
		Select<T> select = new Select(type);
		return select;
	}
	
	/**
	 * Deletes all of a type
	 * @param type
	 * @return
	 */
	public static boolean deleteAll(Class<?> type) {
		return new Delete(type).execute();
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
}
