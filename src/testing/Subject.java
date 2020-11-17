package testing;

import java.util.List;

import annotations.ManyToMany;
import annotations.Table;
import annotations.Varchar;
import orm.Entity;

@Table(name = "subject")
public class Subject extends Entity<Lesson> {

	@Varchar(size = 200)
	String name;
	
	@Varchar(size = 200)
	String description;
	
	@ManyToMany(referenceTable = Teacher.class, linkTable = TeacherHasSubjects.class)
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
