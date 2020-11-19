package testing;

import java.util.List;

import annotations.OneToMany;
import annotations.Table;
import annotations.SqlVarchar;
import orm.Entity;

@Table(name = "schoolClass")
public class SchoolClass extends Entity<Lesson> {

	@SqlVarchar(size = 200)
	String name;

	@OneToMany(referenceTable = Teacher.class)
	List<Teacher> teacher;
	
	public SchoolClass() {
		super();
	}

	public SchoolClass(String name, List<Teacher> teacher) {
		super();
		this.name = name;
		this.teacher = teacher;
	}
}