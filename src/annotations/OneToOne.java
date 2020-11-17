package annotations;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import orm.Entity;
import orm.OrmUtils;

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
	
	/**
	 * Field Column in the <b>OWN</b> table wich will be used to map the foreign Entity
	 * 
	 * if default value is given(<empty string>) an automatic Column with @Unique, @AutoIncrement will be created
	 * 
	 * Needs to be referencing a @Unique field in all cases if its not the OneToOne Relation is NOT operatable
	 * 
	 * Can be set to OrmUtils.ENTITY_PK_FIELDNAME or whats set on the other side to crate a dual sided one to one relation
	 */
	String ownReferenceField() default "";
	
	/**
	 * Field Column in the <b>FOREIGN</b> table wich will be used to map the foreign Entity
	 * 
	 * if default value is given(ENTITY_PK_FIELDNAME) the relations primary key will be used
	 * 
	 * Needs to be referencing a @Unique field in all cases if its not the OneToOne Relation is NOT operatable
	 */
	String foreignReferenceField() default OrmUtils.ENTITY_PK_FIELDNAME;
}