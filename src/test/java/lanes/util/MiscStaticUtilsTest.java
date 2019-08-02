package lanes.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static lanes.util.MiscStaticUtils.*;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class MiscStaticUtilsTest {

	protected void assertStreamFilterCast(Set<?> expected, Set<?> input, Class<?> filter){
		assertEquals(expected, StreamFilterCast(input.stream(), filter).collect(Collectors.toSet())); //Java erases type params, so no point in testing parametrization of result (and no way really)
	}

	@Test
	public void testStreamFilterCast(){
		assertStreamFilterCast(Set.of(), Set.of(), Object.class);
		assertStreamFilterCast(Set.of(), Set.of(), Double.class);
		assertStreamFilterCast(Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Object.class);
		assertStreamFilterCast(Set.of(false), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Boolean.class);
		assertStreamFilterCast(Set.of("a", "(ヘ･_･)ヘ┳━┳"), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), String.class);
		assertStreamFilterCast(Set.of(15), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Integer.class);
		assertStreamFilterCast(Set.of(12.5d), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Double.class);
		assertStreamFilterCast(Set.of(15, 12.5d), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Number.class);
		assertStreamFilterCast(Set.of(new ArrayList<>(), new HashSet<>()), Set.of("a", 15, false, new ArrayList<>(), 12.5d, new HashSet<>(), "(ヘ･_･)ヘ┳━┳"), Collection.class);
	}

}
