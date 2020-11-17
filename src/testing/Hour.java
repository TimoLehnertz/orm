package testing;

import java.sql.Time;

import annotations.Table;
import orm.Entity;

@Table(name = "hour")
public class Hour extends Entity<Lesson> {

	int index;
	Time startTime;
	Time endTime;
	
	public Hour(int index, Time startTime, Time endTime) {
		super();
		this.index = index;
		this.startTime = startTime;
		this.endTime = endTime;
	}	
}