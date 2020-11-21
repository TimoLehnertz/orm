package testing;
import java.util.List;

import annotations.ManyToMany;
import annotations.Table;
import annotations.ToString;
import orm.Entity;

@ToString
@Table(name = "student_m2m")
public class StudentManyToMany extends Entity<StudentManyToMany>{

	String name;
	
	@ManyToMany(referenceTable = Dozent.class)
	List<Dozent> dozenten;
	
	public StudentManyToMany() {
		super();
	}

	/**
	 * @param name
	 * @param dozenten
	 */
	protected StudentManyToMany(String name, List<Dozent> dozenten) {
		super();
		this.name = name;
		this.dozenten = dozenten;
	}
	
	
}
