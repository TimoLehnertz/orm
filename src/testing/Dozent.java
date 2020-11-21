package testing;
import annotations.Table;
import orm.Entity;

@Table(name = "dozent")
public class Dozent  extends Entity<Dozent>{

	String name;
	
	public Dozent() {
		super();
	}

	/**
	 * @param name
	 */
	protected Dozent(String name) {
		super();
		this.name = name;
	}	
}