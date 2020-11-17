package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for efficiently configuring the length the varchar representation of a String
 * in the database
 * 
 * @author timo
 * 
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface Varchar {

	int size() default 100;
}
