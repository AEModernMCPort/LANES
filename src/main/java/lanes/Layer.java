package lanes;

import lanes.util.reg.NamedRegistry;
import org.checkerframework.checker.nullness.qual.NonNull;

public abstract class Layer<L extends Layer<L, CP>, CP extends ConnectParam<L, CP>> implements NamedRegistry.Entry<L> {

	public final String name;

	public Layer(String name){
		this.name = name;
	}

	@NonNull
	@Override
	public String getName(){
		return name;
	}

	//TODO Access to (0,1) constants, in a more proper-"static" way
	@Deprecated public abstract CP zeroCP(); //0: ∀cp, 0⋂cp=0 & 0⋃cp=cp
	//@Deprecated public abstract CP oneCP(); //1:
	@Deprecated public abstract CP infCP(); //∞: ∀cp, cp⋂∞=cp & cp⋃∞=∞

}
