package sqlMagic;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import orm.Orm;

/**
 * Class for interactions with the database
 * 
 * @author Timo Lehnertz
 *
 */

public class DbConnector {
	
	static final String DB_ARGS = "?useUnicode=true&useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC";
	static final String JSBC_URL = "jdbc:mysql://";
	String dbUrl = "localhost";
	String dbUser = "root";
	String dbPassowrd = "";
	String dbName = "test";
	int dbPort = 3306;
	
	private Connection conn = null;
	
	private static DbConnector instance = new DbConnector();
	
	private DbConnector() {
		super();
	}
	
	public static DbConnector getInstance() {
		return instance;
	}
	
	public boolean isOperatable() {
		Connection conn = getConnection();
		boolean operatable = conn != null;
		try {
			conn.close();
		} catch (Exception e) {
			operatable = false;
		}
		return operatable;
	}
	
	private boolean openConnection() {
	    Properties connectionProps = new Properties();
	    connectionProps.put("user", dbUser);
	    connectionProps.put("password", dbPassowrd);
		try {
			conn =  DriverManager.getConnection("jdbc:mysql://" + dbUrl + ":" + dbPort + DB_ARGS, connectionProps);
			boolean dbExists = false;
			try {
				ResultSet resultSet = conn.getMetaData().getCatalogs();
				while (resultSet.next()) {
					String name = resultSet.getString(1);
					if(name.contentEquals(dbName.toLowerCase())) {
						dbExists = true;
						break;
				  	}
				}
				resultSet.close();
			} catch (SQLException e) {
				Orm.logger.error(e.getMessage());
				return false;
			}
			Statement stmt = conn.createStatement();
			if(!dbExists) {
				try {
					String createSql = "CREATE DATABASE `" + dbName + "`;";
					Orm.logger.info("Creating DATABASE: \"" + dbName + "\"");
					Orm.logger.debug("Executing: " + createSql);
					stmt.execute(createSql);
				} catch (SQLException e) {
					e.printStackTrace();
					return false;
				}
			}
			stmt.execute("USE " + dbName + ";");
			stmt.close();
			return true;
		} catch (SQLException e) {
			Orm.logger.error(e.getMessage());
			return false;
		}
	} 
	
	private Connection getConnection() {
		if(openConnection()) {
			return conn;
		} else {
			return null;
		}
	}
	
	public long executeInsert(String sql, String argTypes, Object... objects) {
		return executeInsert(new SqlParams(argTypes, sql, objects));
	}
	
	public int executeInsert(SqlParams params) {
		Orm.logger.debug("executingInsert: " + params);
		if(!isOperatable()) {
			return -1;
		}
		params.beforeExecute();
		try(Connection conn = getConnection()) {
			try (PreparedStatement stmt = conn.prepareStatement(params.sql, Statement.RETURN_GENERATED_KEYS)){
				params.bindParams(stmt);
				stmt.execute();
				ResultSet rs = stmt.getGeneratedKeys();
				int id = -1;
				if(rs.next()) {
					id = rs.getInt(1);
				}
				rs.close();
				params.afterExecute(null);
				return id;
			} catch (SQLException e) {
				e.printStackTrace();
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	public List<Map<String, Object>> executeQuery(String sql, String argTypes, Object... objects) {
		return executeQuery(new SqlParams(argTypes, sql, objects));
	}
	
	public List<Map<String, Object>> executeQuery(SqlParams params){
		if(!isOperatable()) {
			return new ArrayList<Map<String, Object>>();
		}
		Orm.logger.debug("executeQuery: " + params);
		params.beforeExecute();
		List<Map<String, Object>> rsList = new ArrayList<Map<String, Object>>();
		try(Connection conn = getConnection()) {
			try(PreparedStatement stmt = conn.prepareStatement(params.sql)){
				params.bindParams(stmt);
				ResultSet rs = stmt.executeQuery();
				while (rs.next()) {
					Map<String, Object> rsMap = new HashMap<String, Object>();
					for (int i = 1; i < rs.getMetaData().getColumnCount() + 1; i++) {
						rsMap.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
					}
					rsList.add(rsMap);
				}
				rs.close();
			}catch(SQLException e) {
				Orm.logger.warn("executeQuery failed: " + params + " | Message: " + e.getMessage());
				return rsList;
			}
		}catch(SQLException e) {
			Orm.logger.warn("executeQuery failed: " + params + " | Message: " + e.getMessage());
			return rsList;
		}
		params.afterExecute(rsList);
		return rsList;
	}
	
	public boolean execute(String sql) {
		return execute(sql, null);
	}
	
	public boolean execute(String sql, String argTypes, Object... objects) {
		return execute(new SqlParams(argTypes, sql, objects));
	}
	
	public boolean execute(SqlParams params) {
		if(params == null || !isOperatable()) {
			return false;
		}
		params.beforeExecute();
		
		Orm.logger.debug("execute: " + params);
		try(Connection conn = getConnection()) {
			try(PreparedStatement stmt = conn.prepareStatement(params.sql)) {
				params.bindParams(stmt);
				stmt.execute();
			} catch (SQLException e) {
				Orm.logger.warn("execute failed: " + params + " | Message: " + e.getMessage());
				return false;
			}
			params.afterExecute(null);
			return true;
		} catch (SQLException e1) {
			Orm.logger.warn("execute failed: " + params + " | Message: " + e1.getMessage());
			return false;
		}
	}


	public boolean doesTableExist(String tableName) {
		if(tableName == null || !isOperatable()) {
			return false;
		}
		try (Connection conn = getConnection()){
			DatabaseMetaData dbm = conn.getMetaData();
			ResultSet rs = dbm.getTables(getDbName(), null,  tableName.toLowerCase(), null);
		    if (rs.next()) {
	    		rs.close();
	    		return true;
		    }
		    rs.close();
		    return false;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	/*
	 * Getters /  Setters
	 */
	
	public String getDbUser() {
		return dbUser;
	}

	public void setDbUser(String dbUser) {
		this.dbUser = dbUser;
	}

	public String getDbPassowrd() {
		return dbPassowrd;
	}

	public void setDbPassowrd(String dbPassowrd) {
		this.dbPassowrd = dbPassowrd;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public int getDbPort() {
		return dbPort;
	}

	public void setDbPort(int dbPort) {
		this.dbPort = dbPort;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}
}