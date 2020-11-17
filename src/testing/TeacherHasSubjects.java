package testing;

import annotations.ForeignKey;
import annotations.Table;
import orm.Entity;

@Table(name = "TeacherHasSubjects")
public class TeacherHasSubjects extends Entity<TeacherHasSubjects> {

	@ForeignKey(referenceTable = Teacher.class, field = "id1")
	long teacher;
	
	@ForeignKey(referenceTable = Subject.class)
	long subject;
	
	public TeacherHasSubjects() {
		super();
	}
}