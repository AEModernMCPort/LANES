package lanes.util.reg;

import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class RegistriesTest {

	@Test
	public void testUniRegEntry(){
		var ae = new UniRegEntry<>("duh", "(⌐■_■)");
		assertEquals("duh", ae.name);
		assertEquals("(⌐■_■)", ae.elem);
		assertEquals(ae, new UniRegEntry<>("duh", "(⌐■_■)"));
		assertNotEquals(ae, new UniRegEntry<>("duh", "(•_•)"));
		assertNotEquals(ae, new UniRegEntry<>("dood", "(•_•)"));
		assertNotEquals(ae, new UniRegEntry<>("dood", "(⌐■_■)"));
	}

	@Test
	public void testRegistries(){
		var reg = new MutableNamedRegistry<UniRegEntry<String>>();
		assertFalse(reg.has("🧀"));
		assertFalse(reg.has("🥔"));
		assertFalse(reg.has("🍣"));
		reg.register(new UniRegEntry<>("🧀", "😶"));
		assertTrue(reg.has("🧀"));
		assertFalse(reg.has("🥔"));
		assertFalse(reg.has("🍣"));
		reg.register(new UniRegEntry<>("🥔", "😮"));
		assertTrue(reg.has("🧀"));
		assertTrue(reg.has("🥔"));
		assertFalse(reg.has("🍣"));
		assertThrows(IllegalArgumentException.class, () -> reg.register(new UniRegEntry<>("🧀", "🤨")));
		assertTrue(reg.has("🧀"));
		assertTrue(reg.has("🥔"));
		assertFalse(reg.has("🍣"));
		assertEquals("😶", reg.get("🧀").map(UniRegEntry::getElem).orElseThrow());
		assertEquals("😮", reg.get("🥔").map(UniRegEntry::getElem).orElseThrow());
		assertTrue(reg.get("🍣").isEmpty());
		assertEquals(Set.of(new UniRegEntry<>("🧀", "😶"), new UniRegEntry<>("🥔", "😮")), reg.entries().collect(Collectors.toSet()));
		//All
		var reg2 = new MutableNamedRegistry<UniRegEntry<String>>();
		reg2.register(new UniRegEntry<>("🍣", "🧐"));
		assertFalse(reg2.has("🧀"));
		assertFalse(reg2.has("🥔"));
		assertTrue(reg2.has("🍣"));
		reg2.registerAll(reg);
		assertTrue(reg2.has("🧀"));
		assertTrue(reg2.has("🥔"));
		assertTrue(reg2.has("🍣"));
		assertTrue(reg.has("🧀"));
		assertTrue(reg.has("🥔"));
		assertFalse(reg.has("🍣"));
		var reg3 = MutableNamedRegistry.copyOf(reg);
		assertTrue(reg3.has("🧀"));
		assertTrue(reg3.has("🥔"));
		assertFalse(reg3.has("🍣"));
		//Immutable
		var ireg = reg.finaliz();
		assertTrue(reg3.has("🧀"));
		assertTrue(reg3.has("🥔"));
		assertFalse(reg3.has("🍣"));
		assertEquals(Set.of(new UniRegEntry<>("🧀", "😶"), new UniRegEntry<>("🥔", "😮")), reg3.entries().collect(Collectors.toSet()));
		assertEquals(reg3.entries().collect(Collectors.toSet()), ImmutableNamedRegistry.copyOf(reg).entries().collect(Collectors.toSet()));
		assertThrows(UnsupportedOperationException.class, () -> ireg.reg.put("🌮", new UniRegEntry<>("🌮", "🐱‍👤")));
	}

	@Test
	public void testMutableRegistryPredicates(){
		var flr = new MutableNamedRegistry<UniRegEntry<String>>(s -> s.length() < 5);
		assertDoesNotThrow(() -> flr.register(new UniRegEntry<>("🖊", "🍍")));
		assertThrows(IllegalArgumentException.class, () -> flr.register(new UniRegEntry<>("apple", "🖊")));
		var pr = new MutableNamedRegistry<UniRegEntry<String>>(Pattern.compile("[a-z0-9_]+"));
		assertDoesNotThrow(() -> pr.register(new UniRegEntry<>("hello_21", "👋")));
		assertThrows(IllegalArgumentException.class, () -> pr.register(new UniRegEntry<>("hI wAZzUp?", "🎧")));
		var prc = MutableNamedRegistry.copyOfWithNamePredicate(pr);
		assertTrue(prc.has("hello_21"));
		assertDoesNotThrow(() -> prc.register(new UniRegEntry<>("hi_63_back", "👋")));
		assertThrows(IllegalArgumentException.class, () -> prc.register(new UniRegEntry<>("yah wazzup", "🎧")));
	}

}
