package testing;

import annotations.OneToOne;
import annotations.Table;
import annotations.Varchar;
import orm.Entity;

@Table(name = "lesson")
public class Lesson extends Entity<Lesson>{

	@OneToOne(referenceTable = Subject.class)
	Subject subject;
	
	@OneToOne(referenceTable = Room.class)
	Room room;
	
	@OneToOne(referenceTable = Teacher.class)
	Teacher teacher;
	
	@Varchar(size = 200)
	String info;
	
	@OneToOne(referenceTable = Hour.class)
	Hour hour;
	
	public Lesson() {
		super();
	}

	public Lesson(Subject subject, Room room, Teacher teacher, String info, Hour hour) {
		super();
		this.subject = subject;
		this.room = room;
		this.teacher = teacher;
		this.info = info;
		this.hour = hour;
	}
}