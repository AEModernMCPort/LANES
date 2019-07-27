package lanes.lan;

import lanes.ConnectParam;
import lanes.SimExt;
import lanes.lan.mesh.GlobalMeshLoc;
import lanes.lan.mesh.Meshable;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

public interface ConnectPassthrough extends Meshable {

	@NonNull
	CPTId getId();

	/**
	 * The hub in which this is located
	 * @return this cpt's hub
	 */
	@NonNull
	CPTHub getHub();

	/**
	 * Medium which this cpt uses.<br>
	 * <code>getHub().getCPT(this.medium()) == this</code>
	 * @return this cpt's medium
	 */
	@NonNull
	Medium medium();

	/**
	 * The length of this cpt
	 * @return length of this cpt
	 */
	@NonNull
	double selfLength(); //TODO Layer specific..?

	/**
	 * The limit (maximum) CP that can pass through this, or {@linkplain Optional#empty() nothing} if CP does not exist on said layer.<br>
	 * Obviously the {@linkplain ConnectPassthrough#medium() medium} must exist in the layer as well!
	 * @param layer layer in question
	 * @param <L> layer type
	 * @param <CP> CP type
	 * @return CP limit for the layer, or {@linkplain Optional#empty()} if this does not exist on said layer
	 */
	@NonNull
	<CP extends ConnectParam<CP>> Optional<CP> passthroughLimit(@NonNull String layer);

	@Override
	default boolean existsIn(@NonNull String layer){
		return passthroughLimit(layer).isPresent();
	}

	/*
	 * Sim-Internal
	 * Just obey.
	 */

	@SimExt
	void setGMLoc(@NonNull GlobalMeshLoc loc);
	@SimExt
	@NonNull GlobalMeshLoc getGMLoc();

}
