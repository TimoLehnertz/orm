package testing;

import java.sql.Time;
import java.util.Arrays;

import orm.Logger;
import orm.Orm;
import sqlMagic.Select;

/**
 * Only intended for testing
 * @author timo
 *
 */

public class A_Tester {

	/**
	 * selection of test
	 */
	public void test() {
//		testBasic();
//		testSelectWhere();
		testDeleteWhere();
//		differentRelations();
	}
	
	private void setup() {
		Orm.initDb("localhost", "root", "", "orm_test");//db connection / dbname sutup
		Orm.logger.setLoglevel(Logger.WARN);//reduce console output
		Orm.dropDatabase();//Drop the database to create a fresh new one in the next step (optional)
		Orm.initTables(Day.class, Hour.class, Lesson.class, Room.class, SchoolClass.class, Subject.class, Teacher.class, Timetable.class);//Initiating / warning creating tables
	}
	
	/**
	 * Tests basic initialization / save / update / delete
	 */
	public void testBasic() {
		setup();
		DummyData dummy1 = new DummyData();
		DummyData dummy2 = new DummyData();
		Timetable timetable1 = dummy1.timetable;
		Timetable timetable2 = dummy2.timetable;
		
		/**
		 * 2 should only appear once in the database as the second call to timetable2.save(); should ony update instead of insert
		 */
		
		System.out.println("----------------------------------------------------------------------------------------------------------------------");
		System.out.println(Orm.selectAll(Timetable.class));// Should be emtpy array
		
		timetable1.save();
		timetable2.save();
		timetable2.save();
		
		System.out.println("----------------------------------------------------------------------------------------------------------------------");
		System.out.println(Orm.selectAll(Timetable.class));// Should 2 times the same timetable be emtpy array
		
		
		/**
		 * Testing simple delete function
		 */
		timetable2.delete();
		System.out.println("----------------------------------------------------------------------------------------------------------------------");
		System.out.println(Orm.selectAll(Timetable.class));// Should one timetable
	}
	
	public void testSelectWhere() {
		setup();
		/**
		 * saving two new instances of timetable
		 */
		new DummyData().timetable.save();
		new DummyData().timetable.save();
		
		/**
		 * testing select with where
		 */
		Select<Hour> select = Orm.selectFrom(Hour.class);
		select.query();//calling query will send the Select save the result in the List<T> result and resets the instance of Select
		System.out.println(select.getResult());
		
		Time time = new DummyData().hours.get(5).startTime;
		System.out.println(time);
//		12:45:00
		
		
		select.where.columnBigger("startTime", time).or().fieldIn("index", 1, 2, 3).query();//SELECT all hours which start later that 12:45:00 or which index is in 1, 2, or 3
		
		
		System.out.println("----------------------------------------------------------------------------------------------------------------------");
		System.out.println(select.getResult());
	}
	
	/**
	 * Desting delete and its where object
	 */
	public void testDeleteWhere() {
		setup();
		/**
		 * saving two new instances of timetable
		 */
		new DummyData().timetable.save();
		new DummyData().timetable.save();
		
		/**
		 * testing select with where
		 */
		
		System.out.println(Orm.selectAll(Hour.class));//Show all at start
		
		Time time = new DummyData().hours.get(5).startTime;
		System.out.println(time);//		12:45:00

		Orm.deleteFrom(Hour.class).where.columnBigger("startTime", time).or().fieldIn("index", 1, 2, 3).execute();//DELETE all hours which start later that 12:45:00 or which index is in 1, 2, or 3
		
		
		System.out.println("----------------------------------------------------------------------------------------------------------------------");
		System.out.println(Orm.selectAll(Hour.class));// expected output: all from start minus hours which start later that 12:45:00 or which index is in 1, 2, or 3
		
		System.out.println("----------------------------------------------------------------------------------------------------------------------");
		System.out.println(Orm.selectAll(Timetable.class));//(see effect on parents hours get deleted here too)
	}
	
	/**
	 * Understanding the fundamental difference between One To Many and Many To Many
	 */
	public void differentRelations() {
		Orm.logger.setLoglevel(Logger.WARN);
		Orm.initDb("localhost", "root", "", "orm_test1");
		Orm.dropDatabase();
		Orm.initTables(Dozent.class, StudentOneToMany.class, StudentManyToMany.class);
		
		Dozent a = new Dozent("Dozent a");
		Dozent b = new Dozent("Dozent b");
		Dozent c = new Dozent("Dozent c");
		
		/**
		 * One To Many
		 */
		
		StudentOneToMany s1 = new StudentOneToMany("s1", Arrays.asList(a,b));
		StudentOneToMany s2 = new StudentOneToMany("s2", Arrays.asList(a,c));
		StudentOneToMany s3 = new StudentOneToMany("s3", Arrays.asList(b));
		
		s1.save();
		s2.save();
		s3.save();
		
		/**
		 * Notice the lecteurs dont all make their way into the students lists because they can only be aplied to one Student as ONE to many
		 */
		System.out.println("--------------------------------------One To Many-------------------------------------------------------------------------");
		System.out.println(Orm.selectAll(StudentOneToMany.class));
		
		/**
		 * many TO Many
		 */
		
		StudentManyToMany s4 = new StudentManyToMany("s4", Arrays.asList(a,b));
		StudentManyToMany s5 = new StudentManyToMany("s5", Arrays.asList(a,c));
		StudentManyToMany s6 = new StudentManyToMany("s6", Arrays.asList(b));
		
		s4.save();
		s5.save();
		s6.save();
		
		/**
		 * Notice the lecteurs are maintained correctly after safe as they are able to map to multiple Students
		 */
		System.out.println("-------------------------------Many To Many--------------------------------------------------------------------");
		System.out.println(Orm.selectAll(StudentManyToMany.class));
	}
}
