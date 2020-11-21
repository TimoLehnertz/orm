package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation for Overriding the default to String methode by a Json toString implementation
 * @author timo
 *
 */

@Retention(RUNTIME)
@Target({ TYPE, FIELD })
public @interface ToString {

	int fields() default ALL;
	int inherit() default INHERIT;//Beta
	
	public static final int INHERIT = 0;
	public static final int NO_INHERIT = 1;
	
	public static final int ALL = 0;
	public static final int ONLY_ANNOTATED = 1;
}