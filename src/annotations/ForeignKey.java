package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import orm.Entity;

/**
 * Foreign Key Annotation used for declaring a field as ForeignKey
 * 
 * Has to be a Integer or Long value
 * 
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface ForeignKey {

	Class<? extends Entity<?>> referenceTable();
	String field() default "id";
}
