package lanes.util.ltask;

/**
 * Different reasons for interrupting execution of a {@linkplain LTask task} (or {@linkplain LTaskContext context}).
 */
public enum InterruptReason {

	/**
	 * The operations will resume in this (same) execution session.
	 */
	PAUSE,
	/**
	 * The operations will [most likely] resume in a different execution session, but not necessarily.<br>
	 * Thus a serialization/deserialization call can/will occur soon.
	 */
	SUSPEND,
	/**
	 * The operations will not resume.<br>
	 * The operator is allowed to clean up the resources.
	 */
	TERMINATE;

}
