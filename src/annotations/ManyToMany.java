package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import orm.Entity;

/**
 * Anotation used for defining a field to have a Many to Many relationship
 * Should only be used on Lists
 * using it on any other fields will produce warnings
 * doesnt influence dependencies between tables as no additional columns are added to tables annotated with many To Many Fields
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface ManyToMany {
	Class<? extends Entity<?>> referenceTable();
	Class<? extends Entity<?>> linkTable();
}
