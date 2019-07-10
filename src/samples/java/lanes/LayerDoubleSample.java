package lanes;

public class LayerDoubleSample extends Layer<LayerDoubleSample, ConnectParamDoubleSample> {

	public LayerDoubleSample(String name){
		super(name);
	}

	@Override
	public ConnectParamDoubleSample zeroCP(){
		return ConnectParamDoubleSample.ZERO;
	}

	@Override
	public ConnectParamDoubleSample infCP(){
		return ConnectParamDoubleSample.INF;
	}

}
