package testing;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DummyData {

	public Subject prg = new Subject("Prg", "Programmieren");
	public Subject wfmd = new Subject("WF_MD", "Photoshop");
	public Subject ep = new Subject("EP", "Elektro-Prozesstechnik");
	
	public Teacher ti = new Teacher("Valentina", "Tikko", Arrays.asList(prg));
	public Teacher goi = new Teacher("Giovanni", "Giovinazzo", Arrays.asList(ep));
	public Teacher ro = new Teacher("Wolfgang", "Rosenthatl", Arrays.asList(wfmd));
	
	public Room b207 = new Room("B207", 30, true);
	
	public SchoolClass ita58 = new SchoolClass("ita58", Arrays.asList(ro, goi));
	
	public List<Hour> hours = Arrays.asList(
			new Hour(1, Time.valueOf(LocalTime.of(8, 30)), Time.valueOf(LocalTime.of(9, 15))),
			new Hour(2, Time.valueOf(LocalTime.of(9, 20)), Time.valueOf(LocalTime.of(10, 5))),
			new Hour(3, Time.valueOf(LocalTime.of(10, 15)), Time.valueOf(LocalTime.of(11, 0))),
			new Hour(4, Time.valueOf(LocalTime.of(11, 5)), Time.valueOf(LocalTime.of(11, 50))),
			new Hour(5, Time.valueOf(LocalTime.of(12, 0)), Time.valueOf(LocalTime.of(12, 45))),
			new Hour(6, Time.valueOf(LocalTime.of(12, 45)), Time.valueOf(LocalTime.of(13, 30))),
			new Hour(7, Time.valueOf(LocalTime.of(13, 30)), Time.valueOf(LocalTime.of(14, 15))),
			new Hour(8, Time.valueOf(LocalTime.of(14, 20)), Time.valueOf(LocalTime.of(15, 5))),
			new Hour(9, Time.valueOf(LocalTime.of(15, 15)), Time.valueOf(LocalTime.of(16, 0))),
			new Hour(10, Time.valueOf(LocalTime.of(16, 5)), Time.valueOf(LocalTime.of(16, 50))));
	
	public List<Lesson> lessons = Arrays.asList(new Lesson(
					   wfmd, b207, ro, null, hours.get(0)),
			new Lesson(wfmd, b207, ro, null, hours.get(1)),
			new Lesson(wfmd, b207, ro, null, hours.get(2)),
			new Lesson(ep, b207, goi, null, hours.get(3)),
			new Lesson(ep, b207, goi, null, hours.get(4)),
			new Lesson(ep, b207, goi, null, hours.get(5)),
			new Lesson(null, null, null, "Mittagspause", hours.get(6)),
			new Lesson(prg, b207, ti, null, hours.get(7)),
			new Lesson(prg, b207, ti, null, hours.get(8)),
			new Lesson(prg, b207, ti, null, hours.get(9)));
	
	public Day day = new Day(lessons, Date.valueOf("2020-11-16"));
	
	public Timetable timetable = new Timetable(new ArrayList<Day>(Arrays.asList(day)), ita58, Date.valueOf("2020-11-16"));
}
