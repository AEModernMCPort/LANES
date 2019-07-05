package lanes.util.reg;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public final class UniRegEntry<T> implements NamedRegistry.Entry<UniRegEntry<T>> {

	public final String name;
	public final T elem;

	public UniRegEntry(@NonNull String name, @NonNull T elem){
		this.name = name;
		this.elem = elem;
	}

	@NonNull
	@Override
	public String getName(){
		return name;
	}

	@NonNull
	public T getElem(){
		return elem;
	}

	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(!(o instanceof UniRegEntry)) return false;
		UniRegEntry<?> that = (UniRegEntry<?>) o;
		return name.equals(that.name) && elem.equals(that.elem);
	}

	@Override
	public int hashCode(){
		return Objects.hash(name, elem);
	}

}
