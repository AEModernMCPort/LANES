package lanes;

/**
 * All static settings for {@linkplain LANES}, existing in implementable interface form to allow side-loading / end-user modification /....<br>
 * Each setting is represented by a public no-args method in this interface. Default methods are for configuration as well, they simply provide, well, default settings.<br>
 * All settings must be (effectively) final (at the moment of initialization of LANES) and cost no more that a direct field access.<br>
 * LANES <i>can, if it desires so</i>, deep copy the configuration during build finalization, to guarantee immutability (and access performance).
 */
public interface LanesConfiguration {}
