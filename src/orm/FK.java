package orm;

public class FK {

	Class<? extends Entity<?>> referenceTable;
	String referenceField;
	
	boolean notNull = false;
	boolean unique = false;
	
	public FK(Class<? extends Entity<?>> referenceTable, String referenceField) {
		super();
		this.referenceTable = referenceTable;
		this.referenceField = referenceField;
	}
	
	public FK(Class<? extends Entity<?>> referenceTable, String referenceField, boolean notNull, boolean unique) {
		super();
		this.referenceTable = referenceTable;
		this.referenceField = referenceField;
		this.notNull = notNull;
		this.unique = unique;
	}

	public Class<? extends Entity<?>> getReferenceTable() {
		return referenceTable;
	}
	public void setReferenceTable(Class<? extends Entity<?>> referenceTable) {
		this.referenceTable = referenceTable;
	}
	public String getReferenceField() {
		return referenceField;
	}
	public void setReferenceField(String referenceField) {
		this.referenceField = referenceField;
	}
	
	public boolean isNotNull() {
		return notNull;
	}

	public void setNotNull(boolean notNull) {
		this.notNull = notNull;
	}

	public boolean isUnique() {
		return unique;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	@Override
	public String toString() {
		return referenceTable.getSimpleName() + "." + referenceField;
	}
}