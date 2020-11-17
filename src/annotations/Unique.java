package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for declaring a field as Unique
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface Unique {

}
