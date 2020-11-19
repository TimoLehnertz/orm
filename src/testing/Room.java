package testing;

import annotations.Table;
import annotations.SqlVarchar;
import orm.Entity;

@Table(name = "room")
public class Room extends Entity<Lesson> {

	@SqlVarchar(size = 50)
	String name;
	int size;
	boolean computerRoom;
	
	public Room() {
		super();
	}

	public Room(String name, int size, boolean computerRoom) {
		super();
		this.name = name;
		this.size = size;
		this.computerRoom = computerRoom;
	}
}
