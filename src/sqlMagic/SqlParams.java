package sqlMagic;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import orm.*;

/**
 * Class fo
 * @author Timo Lehnertz
 *
 */

public class SqlParams {
	
	private DbConnector db = DbConnector.getInstance();

	public String sql = "";
	private String argTypes = "";
	private List<Object> data = new ArrayList<>();
	private List<SqlParams> appended = new ArrayList<>();
	
	public SqlParams() {
		super();
	}
	
	public SqlParams(String sql) {
		super();
		this.sql = sql;
	}
	
	/**
	 * Not recomended
	 * @param argTypes
	 * @param sql
	 * @param data
	 */
	public SqlParams(String argTypes, String sql, Object[] data) {
		this(argTypes, sql, Arrays.asList(data));
	}
	
	/**
	 * Not recomended
	 * @param argTypes
	 * @param sql
	 * @param data
	 */
	public SqlParams(String argTypes, String sql, List<Object> data) {
		super();
		this.argTypes = argTypes;
		this.sql = sql;
		this.data = data;
	}
	
	public boolean add(Object ... o) {
		for (Object object : o) {
			if(object == null) {
				Orm.logger.debug("SqlParams::add -> adding null");
				argTypes += SupportedTypes.getCharRepresentationOftype(null);
				data.add(null);
			} else if(!SupportedTypes.isTypeSupported(object.getClass())) {
				Orm.logger.debug("SqlParams::add -> skipping " + object.getClass() + "(not supported)");
				continue;
			} else{
				argTypes += SupportedTypes.getCharRepresentationOftype(object.getClass());
				data.add(object);
			}
		}
		return true;
	}
	
	/**
	 * simplified bind params functionality
	 * @param stmt
	 * @return
	 * @throws SQLException
	 */
	protected PreparedStatement bindParams(PreparedStatement stmt) throws SQLException {
		return bindParams(stmt, argTypes, data.toArray());
	}
	
	/**
	 * Binds the given / added data to the given statement
	 * @param stmt
	 * @param argTypes
	 * @param objects
	 * @return
	 * @throws SQLException
	 */
	protected static PreparedStatement bindParams(PreparedStatement stmt, String argTypes, Object[] objects) throws SQLException {
		if(argTypes != null) {
			if(argTypes.length() != objects.length) {
				throw new IllegalArgumentException("Number of given objects doesnt match argTypes String! given " + objects.length);
			} else {
				for (int i = 0; i < argTypes.length(); i++) {
					try {
						switch(argTypes.charAt(i)) {
						case SupportedTypes.BYTE: stmt.setByte(i + 1, (byte) objects[i]); break;
						case SupportedTypes.SHORT: stmt.setShort(i + 1, (short) objects[i]); break;
						case SupportedTypes.INT: stmt.setInt(i + 1, (int) objects[i]); break;
						case SupportedTypes.LONG: stmt.setLong(i + 1, (long) objects[i]); break;
						case SupportedTypes.FLOAT: stmt.setFloat(i + 1, (float) objects[i]); break;
						case SupportedTypes.DOUBLE: stmt.setDouble(i + 1, (double) objects[i]); break;
						case SupportedTypes.BOOLEAN: stmt.setBoolean(i + 1, (boolean) objects[i]); break;
						case SupportedTypes.STRING: stmt.setString(i + 1, (String) objects[i]); break;
						case SupportedTypes.DATE: stmt.setDate(i + 1, (java.sql.Date) objects[i]); break;
						case SupportedTypes.TIMESTAMP: stmt.setTimestamp(i + 1, (java.sql.Timestamp) objects[i]); break;
						case SupportedTypes.TIME: stmt.setTime(i + 1, (java.sql.Time) objects[i]); break;
						default: throw new IllegalArgumentException("Invalid use ofargTypes in Bind param!");
						}
					} catch(ClassCastException e) {
						e.printStackTrace();
					}
				}
			}
		}
		return stmt;
	}
	
	protected boolean isEmpty() {
		String fullSql = sql;
		for (SqlParams append : appended) {
			fullSql += append.sql;
		}
		return fullSql.length() == 0;
	}
	
	/**
	 * Append another SqlParam to this one
	 * @param append
	 */
	protected void append(SqlParams append) {
		append.beforeExecute();
		sql += append.sql;
		for (Object o : append.data) {
			add(o);
		}
	}
	
	protected void appendReference(SqlParams reference) {
		appended.add(reference);
	}
	
	/**
	 * DbConnector executes this method before this statement gets executed
	 * 
	 * Ment to be overidden
	 * 
	 * if overridden 
	 * super .beforeExecute should still be executed but order can be controlled
	 */
	protected void beforeExecute() {
		for (SqlParams reference : appended) {
			append(reference);
		}
		appended.clear();
	}
	
	/**
	 * DbConnector executes this method after this statement gets executed
	 * only gets called in case of succsess
	 * Ment to be overidden
	 */
	protected void afterExecute(List<Map<String, Object>> result) {
//		Do nothing by default
	}

	protected void reset() {
		sql = "";
		argTypes = "";
		data = new ArrayList<>();
		appended = new ArrayList<>();
	}
	
	
	@Override
	public String toString() {
		return "SQL: " + sql + " | Data: " + data;
	}

	/**
	 * Execute methods reset this instance afer execution
	 * @return
	 */
	public boolean execute() {
		boolean succsess = db.execute(this);
		reset();
		return succsess;
	}
	
	public long insert() {
		long insertedId = db.executeInsert(this);
		reset();
		return insertedId;
	}
	
	public List<Map<String, Object>> query() {
		List<Map<String, Object>> result = db.executeQuery(this);
		reset();
		return result;
	}
}