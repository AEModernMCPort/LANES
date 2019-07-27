package lanes.lan;

import lanes.ConnectParam;
import lanes.Layer;
import lanes.LayerSpecific;

public interface PhysicalPreprocessor<CP extends ConnectParam<CP>, L extends Layer<CP, L>> extends LayerSpecific<CP, L> {}
