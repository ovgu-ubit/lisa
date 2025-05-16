package guard;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotation interface to represent a demanding access guard.
 * 
 * @author Christian Schulz
 * @version 1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface AccessGuard {

	/**
	 * The annotation interface to represent a demanded permission.
	 * 
	 * @author Christian Schulz
	 * @version 1.0
	 */
	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@Inherited
	public @interface Permission {
		public String role() default "";

		public String app() default "";
	}

	public Permission[] permissions() default {};
}
