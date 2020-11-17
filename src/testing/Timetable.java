package testing;

import java.sql.Date;
import java.util.List;

import annotations.OneToMany;
import annotations.OneToOne;
import annotations.Table;
import orm.Entity;

@Table(name = "timetable")
public class Timetable extends Entity<Lesson> {

	@OneToMany(referenceTable = Day.class)
	List<Day> tage;
	
	@OneToOne(referenceTable = SchoolClass.class)
	SchoolClass schoolClass;
	Date date;
	
	public Timetable() {
		super();
	}

	public Timetable(List<Day> tage, SchoolClass schoolClass, Date date) {
		super();
		this.tage = tage;
		this.schoolClass = schoolClass;
		this.date = date;
	}
}
