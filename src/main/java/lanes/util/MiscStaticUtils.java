package lanes.util;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.stream.Stream;

public class MiscStaticUtils { //Implement as extensions when Java can

	@NonNull
	public static <T> Stream<T> StreamFilterCast(@NonNull Stream<?> stream, @NonNull Class<? super T> type){
		return stream.filter(type::isInstance).map(o -> (T) o);
	}

}
