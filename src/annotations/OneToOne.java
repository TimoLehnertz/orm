package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import orm.Entity;
import orm.OrmUtils;

/**
 * One To One Annotation
 * 
 * Used for declaring a field to be representing a one to one relationship
 * 
 * Schould Only be applied to valid Table Entities(Entity extending class fields)
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface OneToOne {
	/**
	 * Adds a foreign key in the own table with @Unique referencing the referenced tables primary key
	 * @return
	 */
	Class<? extends Entity<?>> referenceTable();
}