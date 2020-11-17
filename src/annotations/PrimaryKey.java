package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for declaring a field to be a primary Key
 * 
 * mus be a numeric value
 * 
 * without a primary key the orm will automaticly create one named id
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface PrimaryKey {

	boolean autoIncrement() default true;
}
