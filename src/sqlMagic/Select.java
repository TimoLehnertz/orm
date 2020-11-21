package sqlMagic;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import orm.Entity;
import orm.FK;
import orm.LinkTable;
import orm.Orm;
import orm.OrmUtils;
import orm.RegisterManager;

public class Select<T extends Entity<?>> extends SqlParams{

	/**
	 * Simplified access var
	 */
	private RegisterManager manager = RegisterManager.getInstance();
	
	public Where where;
	private Class<T> from;
	
	List<T> queryResult = new ArrayList<>();
	
	public Select(Class<T> from) {
		super("SELECT * FROM `" + OrmUtils.getTableName(from) + "`");
		this.from = from;
		this.where = new Where(this);
	}
	
	@Override
	protected void reset() {
		super.reset();
		sql = "SELECT * FROM `" + OrmUtils.getTableName(from) + "`";
	}
	
	private T getNewType() {
		try {
			return from.getConstructor().newInstance();
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException e) {
			Orm.logger.error("Error creating new Entity of type " + from.getSimpleName() + "! Did you implement a default Constructor with 0 arguments?");
			return null;
		}
	}
	
	/**
	 * Get a new Entity of type From. Sets only Primitive supported fields wich are no Annotated with NoOrm
	 * @param row from db table to fetch from
	 * @return New Entity
	 */
	private T getEntityFromRow(Map<String, Object> row) {
		T entity = getNewType();
		if(entity == null) {
			return null;
		}
		for (Field field : OrmUtils.getPrimitiveFields(from)) {
			field.setAccessible(true);
			if(row.containsKey(field.getName())) {
				try {
					if(row.get(field.getName()) != null) {
						field.set(entity, row.get(field.getName()));
					} else {
						Orm.logger.info("Field " + field + " was null skipped");
					}
				} catch (IllegalArgumentException | IllegalAccessException e) {
					Orm.logger.warn("Cant set Field " + field + ":( Error message: " + e.getMessage());
				}
			} else {
				Orm.logger.warn("Cant set Field " + field + ":(");
			}
		}
		return entity;
	}
	
	@Override
	protected void beforeExecute() {
		if(!where.isEmpty()) {
			sql += " WHERE ";
			append(where);
		}
		super.beforeExecute();//have to be last calls in method
		sql += ";";//have to be last calls in method
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void afterExecute(List<Map<String, Object>> result) {
		for (Map<String, Object> row : result) {
			int ownId = (int) row.get(OrmUtils.ENTITY_PK_FIELDNAME);
			T entity = getEntityFromRow(row);
			queryResult.add(entity);
			/**
			 * Relations
			 */
			/**
			 * One to One
			 */
			for (FK fk : manager.getOneToOneFksFrom(from)) {
				Field field = fk.getOwnField();
				field.setAccessible(true);
				if(Entity.class.isAssignableFrom(field.getType())) {
					Select<?> select = new Select(field.getType());
					select.where.pkEquals(fk.getPointedId(ownId)).query();
					List<Entity<?>> selectResult = (List<Entity<?>>) select.getResult();
					if(selectResult.size() > 0) {
						Entity<?> otoContent = selectResult.get(0);
						if(otoContent != null) {
							try {
								field.set(entity, otoContent);
							} catch (IllegalArgumentException | IllegalAccessException e) {
								Orm.logger.warn("Could not set field" + field + " :( Message: " + e.getMessage());
							}
						}
					} else {
						Orm.logger.info("Could not set field " + field + ". Select result was 0 long");
					}
				}
			}
			/**
			 * One to Many
			 */
			for (FK fk : manager.getOneToManyFksPointingTo(from)) {
				Field field = fk.getOwnField();
				field.setAccessible(true);
				Select<?> select = new Select(fk.getOwnTable());
				select.where.columnEquals(fk.getColumnname(), ownId).query();
				List<?> otmList = select.getResult();
				try {
					field.set(entity, field.getType().cast(otmList));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					Orm.logger.warn("Could not set field" + field + " :( Message: " + e.getMessage());
				}
			}
			/**
			 * Many to Many
			 */
			for (LinkTable linkTable : manager.getLinkTablesFromLinkA(from)) {
				Field field = linkTable.getLinkAReferenceField();
				field.setAccessible(true);
				List<Integer> foreignIds = linkTable.getLinkBIdsByLinkA(ownId);
				Select<?> select = new Select(linkTable.getLinkB());
				select.where.pkIn(foreignIds).query();
				try {
					field.set(entity, field.getType().cast(select.getResult()));
				} catch (IllegalArgumentException | IllegalAccessException e) {
					Orm.logger.warn("Could not set field" + field + " :( Message: " + e.getMessage());
				}
			}
		}
	}
	
	/**
	 * Not allowed for use so print warning
	 */
	@Override
	public boolean execute() {
		Orm.logger.warn("Cant use execute() on Select! Use query instead. Skipped");
		return false;
	}
	
	@Override
	public long insert() {
		Orm.logger.warn("Cant use insert() on Select! Use query instead. Skipped");
		return -1;
	}
	
	protected Class<?> getFrom() {
		return from;
	}

	/**
	 * returns the last result
	 * and empties it
	 * @return
	 */
	public List<T> getResult() {
		List<T> tmp = queryResult;
		queryResult = new ArrayList<>();
		return tmp;
	}
}