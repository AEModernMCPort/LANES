package lanes.util.reg;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImmutableNamedRegistry<T extends NamedRegistry.Entry<T>> implements NamedRegistry<T> {

	protected final Map<String, T> reg;

	ImmutableNamedRegistry(@NonNull Map<String, T> reg){
		this.reg = Map.copyOf(reg);
	}

	ImmutableNamedRegistry(@NonNull Stream<T> entries){
		this.reg = entries.collect(Collectors.toUnmodifiableMap(NamedRegistry.Entry::getName, Function.identity()));
	}

	public ImmutableNamedRegistry(@NonNull NamedRegistry<T> reg){
		this(reg.entries());
	}

	@Override
	public boolean has(@NonNull String name){
		return reg.containsKey(name);
	}

	@Override
	@NonNull
	public Optional<T> get(@NonNull String name){
		return Optional.ofNullable(reg.get(name));
	}

	@Override
	@NonNull
	public Stream<T> entries(){
		return reg.values().stream();
	}
}
