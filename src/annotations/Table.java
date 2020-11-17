/**
 * 
 */
package annotations;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;


/**
 * Annotation for declaring a table
 * 
 * Basic setting s about a table can be made via this useful Annotation
 * 
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(TYPE)
public @interface Table {
	String name();
	String charset() default "UTF8";
	String engine() default "InnoDB";
}
