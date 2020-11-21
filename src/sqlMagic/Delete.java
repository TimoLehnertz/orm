package sqlMagic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import orm.FK;
import orm.LinkTable;
import orm.OrmUtils;
import orm.RegisterManager;

public class Delete extends SqlParams {

	private DbConnector db = DbConnector.getInstance();
	private RegisterManager manager = RegisterManager.getInstance();
	
	public Where where;
	private Class<?> from;
	
	/**
	 * temp variable for remembering wich lines have to be deleted after this line it self got deleted used for One to One relations
	 */
	private Map<FK, List<Integer>> blackList = new HashMap<>();
	private List<LinkTable> m2mdelete = new ArrayList<>();
	private List<Integer> ownIds = new ArrayList<>();
	
	public Delete(Class<?> class1) {
		super("DELETE FROM `" + OrmUtils.getTableName(class1) + "`");
		this.from = class1;
		this.where = new Where(this);
	}
	
	
	private List<Integer> getIdList(){
		SqlParams select = new SqlParams("SELECT `" + OrmUtils.ENTITY_PK_FIELDNAME + "` FROM `" + OrmUtils.getTableName(from) + "`");
		if(!where.isEmpty()) {
			select.sql += " WHERE ";
			select.append(where);
		}
		select.sql += ";";
		List<Map<String, Object>> result = db.executeQuery(select);
		List<Integer> idList = new ArrayList<>();
		for (Map<String, Object> map : result) {
			idList.add((int) map.get(OrmUtils.ENTITY_PK_FIELDNAME));
		}
		return idList;
	}
	
	
	@Override
	protected void beforeExecute() {
		if(!where.isEmpty()) {
			sql += " WHERE ";
			append(where);
		}
		ownIds = getIdList();
		if(ownIds.size() == 0) {
			super.beforeExecute();//have to be last calls in method
			sql += ";";//have to be last calls in method
			return;
		}
		/**
		 * Delete relations dependent on those ids
		 */
		
		/**
		 * One To many relations
		 */
		List<FK> fks = manager.getOneToManyFksPointingTo(from);
		for (FK fk : fks) {
			List<Integer> idList = fk.getIdListPointingTo(ownIds);
			Delete delete = new Delete(fk.getOwnTable());
			delete.where.pkIn(idList).execute();
		}
		/**
		 * One to One relations
		 * only save all fks and ids that have to get deleted but wait with deletion until this Delete operation is finished
		 * used for afterExecute
		 */
		fks = manager.getOneToOneFksFrom(from);
		blackList = new HashMap<>();
		for (FK fk : fks) {
			blackList.put(fk, fk.getPointedIdsFrom(ownIds));
		}
		
		/**
		 * Many to many relations
		 * only remember and delete in afterExecute() because otherwise some infinite recursion is about to happen
		 */
		m2mdelete = manager.getLinkTablesFromLinkA(from);
		
		super.beforeExecute();//have to be last calls in method
		sql += ";";//have to be last calls in method
	}
	
	@Override
	protected void afterExecute(List<Map<String, Object>> result) {
		/**
		 * One To One Relations
		 * have to be deleted after the own object is deleted because the own object is referencing its one to one relations in a Foreign Key
		 */
		for (Entry<FK, List<Integer>> black : blackList.entrySet()) {
			Delete delete = new Delete(black.getKey().getReferenceTable());
			delete.where.pkIn(black.getValue()).execute();
		}
		
		/**
		 * many to Many
		 */	
		for (LinkTable linkTable : m2mdelete) {
			linkTable.deleteLinkBRowsOwnedBy(ownIds);
		}
	}
}