package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import orm.Entity;

/**
 * One To Many Annotation used for declaring a field to be Referenced as One To many
 * Should ony be applied to List fields
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface OneToMany {
	/**
	 * Table to reference to.
	 * Creates a foreign key in the referenced table referencing the primary key in this table
	 * @return
	 */
	Class<? extends Entity<?>> referenceTable();
}
