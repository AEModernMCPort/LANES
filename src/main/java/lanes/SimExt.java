package lanes;

import lanes.util.guuid.GUUID;

import java.lang.annotation.*;

/**
 * Indicates that the annotated api-exposed object is used internally to drive the simulation. In other words, it cannot be used in the simulation itself, and its' usage by the engine must not affect the simulation state.<br>
 * In other words, the implementation must do literally what the documentation tells to, and obey. You are fully responsible for any problems that arise due to not following the instructions.<br>
 * Any usage of this annotation on a method whose implementation directives are undocumented and/or ambiguous is a bug and must be reported.<br>
 * <br>
 * One of reasons for existence of such declarations is usage of interfaces as primary API basis for implementations (and absence of multi-inheritance support in java *cough*).
 * For instance, to drive the simulation, the engine may sometime need to associate values external to the simulation (such as {@linkplain GUUID GUUIDs}) for intermediate operations; but because interfaces in java do not support non-static fields, the implementation has to take care of storing the value in question.
 */
@Documented
@Retention(RetentionPolicy.CLASS)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE_PARAMETER})
public @interface SimExt {}
