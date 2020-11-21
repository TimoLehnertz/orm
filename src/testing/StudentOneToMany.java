package testing;
import java.util.List;

import annotations.OneToMany;
import annotations.Table;
import annotations.ToString;
import orm.Entity;

@ToString
@Table(name = "student_otm")
public class StudentOneToMany extends Entity<StudentOneToMany>{

	String name;
	
	@OneToMany(referenceTable = Dozent.class)
	List<Dozent> dozenten;
	
	public StudentOneToMany() {
		super();
	}

	/**
	 * @param name
	 * @param dozenten
	 */
	protected StudentOneToMany(String name, List<Dozent> dozenten) {
		super();
		this.name = name;
		this.dozenten = dozenten;
	}
	
	
}
