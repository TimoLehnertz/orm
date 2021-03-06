package testing;

import java.sql.Time;

import annotations.NotNull;
import annotations.Table;
import annotations.ToString;
import orm.Entity;

@ToString()
@Table(name = "hour")
public class Hour extends Entity<Lesson> {

	
	@NotNull
	int index;
	
	@NotNull
	Time startTime;
	
	@ToString
	@NotNull
	Time endTime;
	
	public Hour() {
		super();
	}
	
	public Hour(int index, Time startTime, Time endTime) {
		super();
		this.index = index;
		this.startTime = startTime;
		this.endTime = endTime;
	}	
}