package lanes;

import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.units.qual.C;

import java.util.stream.Stream;

public interface ConnectParam<CP extends ConnectParam<CP>> {

	private CP cast(){
		return (CP) this;
	}

	/**
	 * Checks whether this cp is sufficient for given cp.<br>
	 * For linear parameters, this method is equivalent to <code>this >= param</code>.
	 * @param param second cp to check with
	 * @return <code>this ⊃ cp</code>
	 */
	boolean isSufficientFor(@NonNull CP param);

	/**
	 * Inverse of {@linkplain ConnectParam#isSufficientFor(ConnectParam)} - <tt>this</tt> and <tt>param</tt> are reversed.
	 * @param param second cp to check with
	 * @return <code>this ⊂ cp</code>
	 */
	default boolean canBeIn(@NonNull CP param){ return param.isSufficientFor(cast()); }

	/**
	 * Adds given cp to this cp.<br>
	 * Opposite operation of {@linkplain ConnectParam#subtract(ConnectParam) subtraction}.
	 * @param param cp to add
	 * @return <code>this + param</code>
	 */
	@NonNull
	CP add(@NonNull CP param);

	/**
	 * Subtracts given cp from this cp, only possible if this cp {@linkplain ConnectParam#isSufficientFor(ConnectParam) is sufficient for} the given one. Undefined otherwise.<br>
	 * Opposite operation of {@linkplain ConnectParam#add(ConnectParam) addition}.
	 * @param param cp to subtract
	 * @return <code>this - param</code>
	 */
	@NonNull
	CP subtract(@NonNull CP param);

	/**
	 * Creates an union of this and given cp.<br>
	 * An union <tt>u</tt> of <tt>cps</tt> <code>{cp₁, cp₂, ..., cpₙ}</code> is the "smallest" <tt>u</tt> such that <code>∀c∈cps, c⊂u</code>.
	 * @param param second cp for union
	 * @return <code>this ⋃ param</code>
	 */
	@NonNull
	CP union(@NonNull CP param);

	/**
	 * Creates an intersection of this and given cp.<br>
	 * An intersection <tt>i</tt> of <tt>cps</tt> <code>{cp₁, cp₂, ..., cpₙ}</code> is the "largest" <tt>i</tt> such that <code>∀c∈cps, i⊂c</code>.
	 * @param param second cp for intersection
	 * @return <code>this ⋂ param</code>
	 */
	@NonNull
	CP intersection(@NonNull CP param);

	@NonNull
	default CP sumWith(@NonNull Stream<CP> cps){
		return cps.reduce(cast(), ConnectParam::add);
	}

	@NonNull
	default CP unionWith(@NonNull Stream<CP> cps){
		return cps.reduce(cast(), ConnectParam::union);
	}

	@NonNull
	default CP intersectionWith(@NonNull Stream<CP> cps){
		return cps.reduce(cast(), ConnectParam::intersection);
	}

	@NonNull
	static <C extends ConnectParam<C>, L extends Layer<C, L>> C sum(L layer, Stream<C> cps){
		return layer.zeroCP().sumWith(cps);
	}

	@NonNull
	static <C extends ConnectParam<C>, L extends Layer<C, L>> C union(L layer, Stream<C> cps){
		return layer.zeroCP().unionWith(cps);
	}

	@NonNull
	static <C extends ConnectParam<C>, L extends Layer<C, L>> C intersection(L layer, Stream<C> cps){
		return layer.infCP().intersectionWith(cps);
	}

	/**
	 * Amplifies this cp by given amplification
	 * @param amp amplification
	 * @return <code>this ∗ amp</code>
	 */
	@NonNull
	CP amplify(@NonNegative double amp);

}
