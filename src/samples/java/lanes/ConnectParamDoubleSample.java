package lanes;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ConnectParamDoubleSample implements ConnectParam<LayerDoubleSample, ConnectParamDoubleSample> {

	public static final ConnectParamDoubleSample	ZERO = new ConnectParamDoubleSample(0),
													ONE = new ConnectParamDoubleSample(1),
													INF = new ConnectParamDoubleSample(Double.POSITIVE_INFINITY);

	public final double val;

	public ConnectParamDoubleSample(double val){
		if(val < 0) throw new IllegalArgumentException("Negative values not allowed!");
		if(Double.isNaN(val)) throw new IllegalArgumentException("NaN not allowed!");
		this.val = val;
	}

	@Override
	public boolean isSufficientFor(@NonNull ConnectParamDoubleSample param){
		return this.val >= param.val;
	}

	@NonNull
	@Override
	public ConnectParamDoubleSample add(@NonNull ConnectParamDoubleSample param){
		return new ConnectParamDoubleSample(this.val + param.val);
	}

	@NonNull
	@Override
	public ConnectParamDoubleSample subtract(@NonNull ConnectParamDoubleSample param){
		return new ConnectParamDoubleSample(this.val - param.val);
	}

	@NonNull
	@Override
	public ConnectParamDoubleSample union(@NonNull ConnectParamDoubleSample param){
		return new ConnectParamDoubleSample(Math.max(this.val, param.val));
	}

	@NonNull
	@Override
	public ConnectParamDoubleSample intersection(@NonNull ConnectParamDoubleSample param){
		return new ConnectParamDoubleSample(Math.min(this.val, param.val));
	}

	@NonNull
	@Override
	public ConnectParamDoubleSample amplify(@NonNegative double amp){
		return new ConnectParamDoubleSample(this.val * amp);
	}
}
