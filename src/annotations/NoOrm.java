package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotatio for fields wich are not intendet to be written into the database.
 * Fields with this Annotation will be skipped at creation of the database and saving of entitys
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target(FIELD)
public @interface NoOrm {
	
}