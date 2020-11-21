package sqlMagic;

import java.util.List;
import java.util.Map;

/**
 * going the cheater way
 * @author timo
 *
 * @param <T>
 */

public class WhereLogic {
	
	/**
	 * nex where in row to be appended by reference to last Where
	 */
	private Where next;
	
	/**
	 * @param next in the chain
	 */
	protected WhereLogic(Where reference) {
		super();
		next = new Where(reference.getExecuteReference());
		reference.appendReference(next);
	}

	public Where and(){
		next.sql += " AND ";
		return  next;
	}

	public Where or(){
		next.sql += " OR ";
		return  next;
	}
	
	public boolean execute() {
		return next.getExecuteReference().execute();
	}
	
	public long insert() {
		return next.getExecuteReference().insert();
	}
	
	public List<Map<String, Object>> query() {
		return next.getExecuteReference().query();
	}
}