package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import orm.Entity;

/**
 * One To One Annotation
 * 
 * Used for declaring a field to be representing a one to one relationship
 * 
 * Schould Only be applied to valid Table Entities
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface OneToOne {
	Class<? extends Entity<?>> referenceTable();
}
