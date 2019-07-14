package lanes;

import org.checkerframework.checker.nullness.qual.NonNull;

public interface LayerSpecific<CP extends ConnectParam<CP>, L extends Layer<CP, L>> {

	@NonNull
	L getLayer();

}
