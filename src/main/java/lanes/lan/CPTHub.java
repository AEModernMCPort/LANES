package lanes.lan;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Optional;

public interface CPTHub {

	/**
	 * Retrieve the cpt in this hub for given medium, or {@linkplain Optional#empty() nothing} if the hub does not have any cpt.<br>
	 * <code>getCPT(medium).medium() == medium</code> (when <code>getCPT(medium).isPresent()</code>)
	 * @param medium the medium
	 * @return cpt in the medium, or
	 */
	@NonNull
	Optional<ConnectPassthrough> getCPT(@NonNull Medium medium);

	/**
	 * Amplification for conversion between different mediums in this hub
	 * @return conversion amplification
	 */
	@NonNegative
	default double getIntermodalConversionAmp(){
		return 1;
	}

}
