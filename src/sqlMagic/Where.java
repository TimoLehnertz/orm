package sqlMagic;

import java.util.ArrayList;
import java.util.List;

import orm.OrmUtils;

public class Where extends SqlParams{

	/**
	 * Reference to invoke execute on
	 */
	SqlParams executeReference;
	
	public Where (SqlParams executeReference) {
		super();
		this.executeReference = executeReference;
	}
	
	public boolean execute() {
		return executeReference.execute();
	}
	
	public WhereLogic pkIn(int ... ids) {
		List<Integer> idList = new ArrayList<>();
		for (int id : ids) {
			idList.add(id);
		}
		return pkIn(idList);
	}
	
	public WhereLogic pkIn(List<Integer> ids) {
		if(ids.size() == 0) {
			sql += " FALSE ";//cann not be true so simply inserting false
			return new WhereLogic(this);
		}
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` IN (";
		String delimiter = "";
		for (Integer id : ids) {
			sql += delimiter + "?";
			add(id);
			delimiter = ", ";
		}
		sql += ")";
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic pkNotIn(int ... ids) {
		List<Integer> idList = new ArrayList<>();
		for (int id : ids) {
			idList.add(id);
		}
		return pkNotIn(idList);
	}
	
	public WhereLogic pkNotIn(List<Integer> ids) {
		if(ids.size() == 0) {
			sql += " FALSE ";//cann not be true so simply inserting false
			return new WhereLogic(this);
		}
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` NOT IN (";
		String delimiter = "";
		for (Integer id : ids) {
			sql += delimiter + "?";
			add(id);
			delimiter = ", ";
		}
		sql += ")";
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic pkEquals(int id) {
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` = ?";
		add(id);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic pkNotEqual(int id) {
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` != ?";
		add(id);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic pkBigger(int id) {
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` > ?";
		add(id);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic pkSmaller(int id) {
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` < ?";
		add(id);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic pkBetween(int id1, int id2) {
		sql += "`" + OrmUtils.ENTITY_PK_FIELDNAME + "` BETWEEN ? AND ?";
		add(id1, id2);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic columnEquals(String column, Object val) {
		sql += "`" + column + "` = ?";
		add(val);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic columnNotEqual(String column, Object val) {
		sql += "`" + column + "` != ?";
		add(val);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}
	
	public WhereLogic columnLike(String column, String val) {
		sql += "`" + column + "` LIKE ?";
		add(val);
		/**
		 * stacking
		 */
		return new WhereLogic(this);
	}

	protected SqlParams getExecuteReference() {
		return executeReference;
	}
}