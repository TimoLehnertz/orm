package testing;

import java.util.List;

import annotations.ManyToMany;
import annotations.Table;
import annotations.SqlVarchar;
import orm.Entity;

@Table(name = "subject")
public class Subject extends Entity<Lesson> {

	@SqlVarchar(size = 200)
	String name;
	
	@SqlVarchar(size = 200)
	String description;
	
	@ManyToMany(referenceTable = Teacher.class)
	List<Teacher> teachers;
	
	public Subject() {
		super();
	}

	public Subject(String name, String description) {
		super();
		this.name = name;
		this.description = description;
	}
}
