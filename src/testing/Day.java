package testing;

import java.sql.Date;
import java.util.List;

import annotations.OneToMany;
import annotations.Table;
import orm.Entity;

@Table(name = "day")
public class Day extends Entity<Day> {

	@OneToMany(referenceTable = Lesson.class)
	List<Lesson> lessons;
	Date date;
	
	public Day() {
		super();
	}
	
	public Day(List<Lesson> lessons, Date date) {
		super();
		this.lessons = lessons;
		this.date = date;
	}
	public List<Lesson> getLessons() {
		return lessons;
	}
	public void setLessons(List<Lesson> lessons) {
		this.lessons = lessons;
	}
	public Date getDate() {
		return date;
	}
	public void setDate(Date date) {
		this.date = date;
	}
}