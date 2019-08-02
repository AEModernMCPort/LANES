package lanes;

/**
 * Base interface for all LANES listeners.<br>
 * Listeners listen to things happening outside their scope, but that are related to them. Therefore you are responsible for notifying respective listeners when things outside the scope of LANES occur (bur are related).<br>
 * Thankfully all such events are listed, and should be called, in the {@linkplain LanesListeners main external listener}.
 */
public interface LListener {}
