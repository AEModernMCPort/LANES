package lanes;

/**
 * Objects with this property can themselves determine whether their state is valid.<br>
 * Obviously, in running conditions (unless the engine is driven into UB), all objects remain valid at all times. Therefore the purpose of this interface is to allow automatic state verification during testing.<br>
 * <br>
 * The check can be potentially computationally expensive, and no concurrency constraints are imposed. So it is not recommended to use this (unless you're writing unit tests, in which case - <i>good job&thank you!</i>), however you can rely on it to guarantee the validity.
 */
@TestTortoise
public interface Validatable {

	/**
	 * Checks the state of this object.
	 * @return validity of the state
	 */
	boolean isValid();

}
