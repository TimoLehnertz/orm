package orm;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import sqlMagic.DbConnector;
import sqlMagic.Delete;
import sqlMagic.SqlParams;

/**
 * Class for storing and managing A link table between two Many To Many Tables
 * @author timo.lehnertz
 *
 */

public class LinkTable {

	public Class<?> linkA;
	public Class<?> linkB;
	
	public Field linkAReferenceField;
	
	/**
	 * LinkB reference field is the primary key defined in Entity in all cases
	 */

	public LinkTable(Class<?> linkA, Class<?> linkB, Field linkAReferenceField) {
		super();
		this.linkA = linkA;
		this.linkB = linkB;
		this.linkAReferenceField = linkAReferenceField;
		linkAReferenceField.setAccessible(true);
	}
	
	public String getCreateSql() {
		String s = "CREATE TABLE `" + getTableName() + "`(";
		s += "`" + OrmUtils.getTableName(linkA) + "` INT NOT NULL";
		s += ", `" + OrmUtils.getTableName(linkB) + "` INT NOT NULL";
		s += ", PRIMARY KEY(`" + OrmUtils.getTableName(linkA) + "`, `" + OrmUtils.getTableName(linkB) + "`)";//Primary key
		s += ", FOREIGN KEY(`" + OrmUtils.getTableName(linkA) + "`) REFERENCES `" + OrmUtils.getTableName(linkA) + "`(`" + OrmUtils.ENTITY_PK_FIELDNAME + "`) ON UPDATE CASCADE ON DELETE CASCADE";//foreign key
		s += ", FOREIGN KEY(`" + OrmUtils.getTableName(linkB) + "`) REFERENCES `" + OrmUtils.getTableName(linkB) + "`(`" + OrmUtils.ENTITY_PK_FIELDNAME + "`) ON UPDATE CASCADE ON DELETE CASCADE";//foreign key
		return s + ");";
	}
	
	public SqlParams getInsertSql(Entity<?> a, Entity<?> b) {
		long idA = RegisterManager.getInstance().getIdFromEntity(a);
		long idB = RegisterManager.getInstance().getIdFromEntity(b);
		if(Math.min(idA, idB) == -1) {
			Orm.logger.warn("getInsertSql Error: at least on entity was not in the database Yet");
			return null;
		}
		SqlParams sql = new SqlParams("INSERT IGNORE INTO " + getTableName() + "(`" + OrmUtils.getTableName(linkA) + "`, `" + OrmUtils.getTableName(linkB) + "`) VALUES(?, ?);");
		sql.add(idA, idB);
		return sql;
	}
	
	/**
	 * Deletes all entries wich are pointing from given idA but are not contained in listB
	 * used for cleanup after UPDATE
	 * @param idA
	 * @param listB
	 * @return List of deleted ids
	 */
	public List<Integer> deleteDeadLinks(int idA, List<Integer> listB) {
		SqlParams delete = new SqlParams("DELETE FROM `" + getTableName() + "` WHERE `" + OrmUtils.getTableName(linkA) + "` = ?");
		delete.add(idA);
		delete.sql += " AND `" + OrmUtils.getTableName(linkB) + "` NOT IN(";
		String delimiter = "";
		for (int id : listB) {
			delete.sql += delimiter + "?";
			delete.add(id);
			delimiter = ",";
		}
		delete.sql += ");";
		boolean succsess = delete.execute();
		List<Integer> deletedIds = new ArrayList<>();
		if(succsess) {
			List<Integer> existingIds = getLinkBIdsByLinkA(idA);
			for (int existedId : existingIds) {
				if(!listB.contains(existedId)) {
					deletedIds.add(existedId);
				}
			}
			return deletedIds;
		} else {
			return deletedIds;
		}
	}
	
	@SuppressWarnings("unchecked")//@TODo solution...
	public List<Entity<?>> getLinkAlistContent(Entity<?> reference){
		List<Entity<?>> out = new ArrayList<>();
		try {
			out = (List<Entity<?>>) linkAReferenceField.get(reference);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			Orm.logger.warn("getLinkAlistContent failed. message: " + e.getMessage());
		}
		return out;
	}
	
	public List<Integer> getLinkBIdsByLinkA(int idA){
		List<Integer> out = new ArrayList<>();
		SqlParams select = new SqlParams("SELECT `" + OrmUtils.ENTITY_PK_FIELDNAME +"` FROM `" + OrmUtils.getTableName(linkB) + "` WHERE `" + OrmUtils.ENTITY_PK_FIELDNAME + "` IN ( SELECT `" + OrmUtils.getTableName(linkB) + "` FROM `" + getTableName() + "` WHERE `" + OrmUtils.getTableName(linkA) + "` = ?);");
		select.add(idA);
		for (Map<String, Object> row : select.query()) {
			out.add((Integer) row.get(OrmUtils.ENTITY_PK_FIELDNAME));
		}
		return out;
	}
	
	/**
	 * Deletes all rows from linkB wich are related to one of the ownerIds from linkA
	 * @param ownerIds
	 * @return succsess
	 */
	public boolean deleteLinkBRowsOwnedBy(List<Integer> ownerIds) {
		Orm.logger.incrementTab("Deleting " + getTableName() + " many to many contents from ownerIds " + ownerIds + "...", Logger.INFO);
		if(ownerIds.size() == 0) {
			return false;
		}
		Delete delete = new Delete(linkB);
		delete.where.pkIn(ownerIds);
		
		Orm.logger.decrementTab("Done", Logger.INFO);
		return delete.execute();
	}
	
	public String getTableName() {
		return OrmUtils.getTableName(linkA) + "x" + OrmUtils.getTableName(linkB);
	}
	
	public boolean hasLink(Class<?> link) {
		return linkA == link || linkB == link;
	}

	public Class<?> getLinkA() {
		return linkA;
	}

	public void setLinkA(Class<?> linkA) {
		this.linkA = linkA;
	}

	public Class<?> getLinkB() {
		return linkB;
	}

	public void setLinkB(Class<?> linkB) {
		this.linkB = linkB;
	}

	public Field getLinkAReferenceField() {
		return linkAReferenceField;
	}

	public void setLinkAReferenceField(Field linkAReferenceField) {
		this.linkAReferenceField = linkAReferenceField;
	}

	public boolean isCreated() {
		return DbConnector.getInstance().doesTableExist(getTableName());
	}
	
	@Override
	public String toString() {
		return "M2M -> LinkA: " + OrmUtils.getTableName(linkA) + "." + linkAReferenceField.getName() + ", LinkB: " + OrmUtils.getTableName(linkB);
	}
}