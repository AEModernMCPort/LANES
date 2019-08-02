package lanes.util.reg;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class MutableNamedRegistry<T extends NamedRegistry.Entry<T>> implements NamedRegistry<T> {

	protected final Map<String, T> reg;
	protected final Predicate<String> namePred;

	protected MutableNamedRegistry(Map<String, T> reg, Predicate<String> namePred){
		this.reg = reg;
		this.namePred = namePred;
	}

	public MutableNamedRegistry(@Nullable Predicate<String> namePred){
		this(new HashMap<>(), namePred);
	}
	public MutableNamedRegistry(@NonNull Pattern nameRegex){
		this(nameRegex.asMatchPredicate());
	}
	public MutableNamedRegistry(){
		this((Predicate<String>) null);
	}

	@NonNull
	public static <T extends NamedRegistry.Entry<T>> MutableNamedRegistry<T> copyOf(@NonNull NamedRegistry<T> reg, @Nullable Predicate<String> namePred){
		var r = new MutableNamedRegistry<T>(namePred);
		r.registerAll(reg);
		return r;
	}
	@NonNull public static <T extends NamedRegistry.Entry<T>> MutableNamedRegistry<T> copyOf(@NonNull NamedRegistry<T> reg, @NonNull Pattern nameRegex){ return copyOf(reg, nameRegex.asMatchPredicate()); }
	@NonNull public static <T extends NamedRegistry.Entry<T>> MutableNamedRegistry<T> copyOf(@NonNull NamedRegistry<T> reg){ return copyOf(reg, (Predicate<String>) null); }
	@NonNull public static <T extends NamedRegistry.Entry<T>> MutableNamedRegistry<T> copyOfWithNamePredicate(@NonNull MutableNamedRegistry<T> reg){
		return new MutableNamedRegistry<>(new HashMap<>(reg.reg), reg.namePred);
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

	public void register(@NonNull T t){
		if(reg.containsKey(t.getName())) throw new IllegalArgumentException(String.format("Cannot register %s - name (%s) already used.", t, t.getName()));
		if(namePred != null && !namePred.test(t.getName())) throw new IllegalArgumentException(String.format("Cannot register %s - name (%s) does not validate registry's naming convention.", t, t.getName()));
		reg.put(t.getName(), t);
	}

	public void registerAll(@NonNull Stream<T> ts){ ts.forEach(this::register); }
	public void registerAll(@NonNull NamedRegistry<T> reg){ registerAll(reg.entries()); }

	@NonNull
	public ImmutableNamedRegistry<T> finaliz(){
		return new ImmutableNamedRegistry<>(reg);
	}

	@Override
	@NonNull
	public Stream<T> entries(){
		return reg.values().stream();
	}

}
