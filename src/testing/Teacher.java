package testing;

import java.util.List;

import annotations.ManyToMany;
import annotations.Table;
import annotations.Varchar;
import orm.Entity;

@Table(name = "teacher")
public class Teacher extends Entity<Lesson> {
	
	long id1;
	
	@Varchar(size = 200)
	String name;
	
	@Varchar(size = 200)
	String surename;
	
	@ManyToMany(referenceTable = Subject.class, linkTable = TeacherHasSubjects.class)
	List<Subject> subjects;
	
	public Teacher() {
		super();
	}

	public Teacher(String name, String surename, List<Subject> subjects) {
		super();
		this.name = name;
		this.surename = surename;
		this.subjects = subjects;
	}
}
