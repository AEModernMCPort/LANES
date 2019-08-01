package lanes;

import java.lang.annotation.*;

/**
 * Indicates that the annotated object is a test 🐢, existing only for testing purposes.<br>
 * AKA: It is used exclusively for unit tests (ex - state checks). Its' existence and maintenance are not guaranteed. However nothing stops you from using it at your own risk, if you find it useful. ¯\_(ツ)_/¯
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.TYPE, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.TYPE_PARAMETER})
public @interface TestTortoise {}
