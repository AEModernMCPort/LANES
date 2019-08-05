package lanes.lan.mesh;

import lanes.ConnectParam;
import lanes.ConnectParamDoubleSample;
import lanes.LayerDoubleSample;
import lanes.lan.CPTHub;
import lanes.lan.CPTId;
import lanes.lan.ConnectPassthrough;
import lanes.lan.Medium;
import org.checkerframework.checker.index.qual.NonNegative;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PhysicalMesherTest {

	LayerDoubleSample sampleLayer = new LayerDoubleSample("sample");
	DummyMedium dummyMedium = new DummyMedium("sample");
	DummyMedium dummyMedium2 = new DummyMedium("sample2");

	@Nested
	class TestWithoutMeshables {

		PhysicalMesher<ConnectParamDoubleSample, LayerDoubleSample, Meshable> sampleMesher = sampleLayer.newInstance().setPhysicalMesher(PhysicalMesher::new).build().getPhysicalMesher();
		Supplier<PhysicalMesher<ConnectParamDoubleSample, LayerDoubleSample, Meshable>.ChangeSetPrimitive> lazyNewChangeSet = () -> sampleMesher.newChangeSetPrimitive(id -> sampleMesher.meshes.values().stream().flatMap(mesh -> mesh.getElem(id).stream()).findAny().orElse(null));

		@Test
		public void testRawOps(){
			assertEquals(0, sampleMesher.meshes.size());
			var aMesh = sampleMesher.newMesh();
			sampleMesher.addMeshRaw(aMesh);
			assertEquals(1, sampleMesher.meshes.size());
			final int semC = 25;
			var someEmptyMeshes = Stream.generate(sampleMesher::newMesh).limit(semC).collect(Collectors.toList());
			someEmptyMeshes.forEach(sampleMesher::addMeshRaw);
			assertEquals(semC + 1, sampleMesher.meshes.size());
			assertTrue(sampleMesher.meshExists(aMesh.ID));
			assertSame(aMesh, sampleMesher.getMesh(aMesh.ID).orElse(null));
			someEmptyMeshes.forEach(sampleMesher::removeMeshRaw);
			assertEquals(1, sampleMesher.meshes.size());
			assertTrue(sampleMesher.meshExists(aMesh.ID));
			assertSame(aMesh, sampleMesher.getMesh(aMesh.ID).orElse(null));
			sampleMesher.removeMeshRaw(aMesh);
			assertEquals(0, sampleMesher.meshes.size());
			assertFalse(sampleMesher.meshExists(aMesh.ID));
			assertTrue(sampleMesher.getMesh(aMesh.ID).isEmpty());
		}

		@Test
		public void testRawMeshOps(){
			final CPTId aId1 = new CPTId(), aId2 = new CPTId();
			final var mesh = sampleMesher.newMesh();
			var aNode1 = sampleMesher.newNode(new CPTId(), dummyMedium);
			var aNode2 = sampleMesher.newNode(new CPTId(), dummyMedium);
			mesh.addElemRaw(aNode1);
			assertEquals(1, mesh.elements.size());
			mesh.addElemRaw(aNode1);
			assertEquals(1, mesh.elements.size());
			mesh.addElemRaw(aNode2);
			assertEquals(2, mesh.elements.size());
			assertTrue(mesh.hasElem(aNode1.ID));
			assertTrue(mesh.hasElem(aNode2.ID));
			mesh.removeElemRaw(aNode1);
			assertEquals(1, mesh.elements.size());
			assertFalse(mesh.hasElem(aNode1.ID));
			assertTrue(mesh.hasElem(aNode2.ID));
			mesh.removeElemRaw(aNode1);
			assertEquals(1, mesh.elements.size());
			mesh.addElemRaw(aNode1);
			var aLink = sampleMesher.newLink(aNode1.ID, aNode2.ID,  Stream.generate(CPTId::new).limit(5).collect(Collectors.toList()));
			assertFalse(mesh.hasElem(aLink.ID));
			mesh.addElemRaw(aLink);
			assertTrue(mesh.hasElem(aLink.ID));
			assertSame(aLink, mesh.getElem(aLink.ID).orElse(null));
			assertSame(aNode1, mesh.getElem(aLink.from).orElse(null));
			assertSame(aNode2, mesh.getElem(aLink.to).orElse(null));
		}

		@Test
		public void testElemsLinkage(){
			final CPTId aId1 = new CPTId(), aId2 = new CPTId();
			final var aIds1 = Stream.generate(CPTId::new).limit(5).collect(Collectors.toList()); final var aIds2 = Stream.generate(CPTId::new).limit(3).collect(Collectors.toList());
			var aNode1 = sampleMesher.newNode(aId1, dummyMedium);
			var aNode2 = sampleMesher.newNode(aId2, dummyMedium);
			var aLink1 = sampleMesher.newLink(aNode1.ID, aNode2.ID, aIds1);
			var aLink2 = sampleMesher.newLink(aNode2.ID, aNode1.ID, aIds2);
			aNode1.addLinkRaw(aLink1.ID);
			aNode2.addLinkRaw(aLink1.ID);
			aNode1.addLinkRaw(aLink2.ID);
			aNode2.addLinkRaw(aLink2.ID);
			//adj
			assertEquals(Set.of(aLink1.ID, aLink2.ID), aNode1.adjacent().collect(Collectors.toSet()));
			assertEquals(Set.of(aLink1.ID, aLink2.ID), aNode2.adjacent().collect(Collectors.toSet()));
			assertEquals(Set.of(aNode1.ID, aNode2.ID), aLink1.adjacent().collect(Collectors.toSet()));
			assertEquals(Set.of(aNode1.ID, aNode2.ID), aLink2.adjacent().collect(Collectors.toSet()));
			aNode1.removeLinkRaw(aLink2.ID);
			aNode2.removeLinkRaw(aLink2.ID);
			assertEquals(Set.of(aLink1.ID), aNode1.adjacent().collect(Collectors.toSet()));
			assertEquals(Set.of(aLink1.ID), aNode2.adjacent().collect(Collectors.toSet()));
			//CPTs
			assertEquals(Set.of(aId1), aNode1.getCPTs().collect(Collectors.toSet()));
			assertEquals(Set.of(aId2), aNode2.getCPTs().collect(Collectors.toSet()));
			assertEquals(new HashSet<>(aIds1), aLink1.getCPTs().collect(Collectors.toSet()));
			assertEquals(new HashSet<>(aIds2), aLink2.getCPTs().collect(Collectors.toSet()));
		}

		@Nested
		class TestChangeSets {

			@Test
			public void testPrimitiveLO(){
				final CPTId aId1 = new CPTId(), aId2 = new CPTId();
				final var aIds = Stream.generate(CPTId::new).limit(5).collect(Collectors.toList());
				var aChangeSet = sampleMesher.newChangeSetPrimitive(id -> null);
				Function<PhysicalMesher.MeshElem, Boolean> resultingElemState = e -> aChangeSet.accChanges().filter(c -> c.elem == e).map(c -> c.newState).findAny().orElse(false);
				//Unrelated creation & deletion
				int rawChanges = 0, uniqueChanges = 0;
				var aNode1 = aChangeSet.createNode(aId1, dummyMedium);
				assertEquals(++rawChanges, aChangeSet.changes.size());
				assertEquals(++uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aNode1));
				var aNode2 = aChangeSet.createNode(aId2, dummyMedium);
				assertEquals(++rawChanges, aChangeSet.changes.size());
				assertEquals(++uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aNode2));
				aChangeSet.destroyNode(aNode1);
				assertEquals(++rawChanges, aChangeSet.changes.size());
				assertEquals(--uniqueChanges, aChangeSet.accChanges().count(), "Changes on the same element did not accumulate");
				assertFalse(resultingElemState.apply(aNode1));
				aNode1 = aChangeSet.createNode(aId1, dummyMedium);
				assertEquals(++rawChanges, aChangeSet.changes.size());
				assertEquals(++uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aNode1));
				//Related creation & deletion
				var aLink = aChangeSet.createLink(aNode1, aNode2, aIds);
				assertEquals(rawChanges += 3, aChangeSet.changes.size());
				assertEquals(++uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aLink));
				assertEquals(Set.of(aLink.ID), aNode1.links);
				assertEquals(Set.of(aLink.ID), aNode2.links);
				aChangeSet.destroyLink(aLink);
				assertEquals(rawChanges += 3, aChangeSet.changes.size());
				assertEquals(--uniqueChanges, aChangeSet.accChanges().count());
				assertFalse(resultingElemState.apply(aLink));
				assertEquals(Set.of(), aNode1.links);
				assertEquals(Set.of(), aNode2.links);
				aLink = aChangeSet.createLink(aNode1, aNode2, aIds);
				assertEquals(rawChanges += 3, aChangeSet.changes.size());
				assertEquals(++uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aLink));
				assertEquals(Set.of(aLink.ID), aNode1.links);
				assertEquals(Set.of(aLink.ID), aNode2.links);
				aChangeSet.destroyNode(aNode2);
				assertEquals(rawChanges += 1 + 3, aChangeSet.changes.size());
				assertEquals(uniqueChanges -= 2, aChangeSet.accChanges().count());
				assertFalse(resultingElemState.apply(aNode2));
				assertFalse(resultingElemState.apply(aLink));
				assertEquals(Set.of(), aNode1.links);
			}

			@Test
			public void testPrimitiveMO(){
				final var nIds = Stream.generate(CPTId::new).limit(6).collect(Collectors.toList());
				final var lIds = Stream.generate(() -> Stream.generate(CPTId::new).limit(8).collect(Collectors.toList())).limit(8).collect(Collectors.toList());
				var aChangeSet = sampleMesher.newChangeSetPrimitive(id -> null);
				Function<PhysicalMesher.MeshElem, Boolean> resultingElemState = e -> aChangeSet.accChanges().filter(c -> c.elem == e).map(c -> c.newState).findAny().orElse(false);
				var aNodes = nIds.stream().map(n -> aChangeSet.createNode(n, dummyMedium)).collect(Collectors.toList());
				int rawChanges = aChangeSet.changes.size(), uniqueChanges = (int) aChangeSet.accChanges().count();
				//Simplification
				assertNull(aChangeSet.simplify(aNodes.get(0)));
				assertEquals(rawChanges, aChangeSet.changes.size());
				assertEquals(uniqueChanges, aChangeSet.accChanges().count());
				var aLink1 = aChangeSet.createLink(aNodes.get(1), aNodes.get(2), lIds.get(0));
				var aLink2 = aChangeSet.createLink(aNodes.get(2), aNodes.get(3), lIds.get(1));
				var aLink3 = aChangeSet.createLink(aNodes.get(4), aNodes.get(3), lIds.get(2));
				var aLink4 = aChangeSet.createLink(aNodes.get(0), aNodes.get(3), lIds.get(3));
				rawChanges = aChangeSet.changes.size(); uniqueChanges = (int) aChangeSet.accChanges().count();
				assertNull(aChangeSet.simplify(aNodes.get(1)));
				assertEquals(rawChanges, aChangeSet.changes.size());
				assertEquals(uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aNodes.get(1)));
				assertNull(aChangeSet.simplify(aNodes.get(3)));
				assertEquals(rawChanges, aChangeSet.changes.size());
				assertEquals(uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aNodes.get(3)));
				{ //Medium check
					var ncs = sampleMesher.newChangeSetPrimitive(id -> null);
					var ns = List.of(ncs.createNode(nIds.get(0), dummyMedium), ncs.createNode(nIds.get(1), dummyMedium), ncs.createNode(nIds.get(2), dummyMedium2));
					ncs.createLink(ns.get(0), ns.get(1), lIds.get(0));
					ncs.createLink(ns.get(1), ns.get(2), lIds.get(1));
					ncs.createLink(ns.get(2), ns.get(0), lIds.get(2));
					ns.forEach(n -> assertNull(ncs.simplify(n))); //In no order the 3 nodes share a medium, so no simplification can take place
				}
				{ //🔁
					var ncs = sampleMesher.newChangeSetPrimitive(id -> null);
					var n1 = ncs.createNode(nIds.get(0), dummyMedium);
					var n2 = ncs.createNode(nIds.get(1), dummyMedium);
					var l1 = ncs.createLink(n1, n2, lIds.get(0));
					var l2 = ncs.createLink(n2, n1, lIds.get(1));
					assertNull(ncs.simplify(n1));
					assertNull(ncs.simplify(n2));
				}
				var newLink = aChangeSet.simplify(aNodes.get(2));
				assertNotNull(newLink);
				assertEquals(rawChanges += (1 + 3 + 3) + 3, aChangeSet.changes.size());
				assertEquals(uniqueChanges = uniqueChanges - 3 + 1, aChangeSet.accChanges().count());
				assertFalse(resultingElemState.apply(aNodes.get(2)));
				assertFalse(resultingElemState.apply(aLink1));
				assertFalse(resultingElemState.apply(aLink2));
				assertTrue(resultingElemState.apply(newLink));
				assertFalse(aNodes.get(1).links.contains(aLink1.ID));
				assertFalse(aNodes.get(3).links.contains(aLink2.ID));
				var copySet = new HashSet<>(aNodes.get(1).links);
				copySet.retainAll(aNodes.get(3).links);
				assertEquals(Set.of(newLink.ID), copySet);
				Function<List<?>, List<?>> rev = l -> {
					Collections.reverse(l = new ArrayList<>(l));
					return l;
				};
				var nlEEs = new ArrayList<>(lIds.get(0));
				nlEEs.add(aNodes.get(2).cpt);
				nlEEs.addAll(lIds.get(1));
				assertEquals(newLink.from.equals(aNodes.get(1).ID) ? nlEEs : rev.apply(nlEEs), newLink.cpts);
				{ //CPT order conservation
					var ncs = sampleMesher.newChangeSetPrimitive(id -> null);
					var ns = nIds.stream().map(n -> ncs.createNode(n, dummyMedium)).collect(Collectors.toList());
					int nlid = 0;
					var ls = List.of(
						ncs.createLink(ns.get(0), ns.get(2), lIds.get(0)), ncs.createLink(ns.get(2), ns.get(1), lIds.get(4)),
						ncs.createLink(ns.get(0), ns.get(3), lIds.get(1)), ncs.createLink(ns.get(1), ns.get(3), lIds.get(5)),
						ncs.createLink(ns.get(4), ns.get(0), lIds.get(2)), ncs.createLink(ns.get(1), ns.get(4), lIds.get(6)),
						ncs.createLink(ns.get(5), ns.get(0), lIds.get(3)), ncs.createLink(ns.get(5), ns.get(1), lIds.get(7))
					);
					var expected = new ArrayList<>(List.of(
						Stream.concat(Stream.concat(	lIds.get(0).stream(),				Stream.of(ns.get(2).cpt)),	lIds.get(4).stream()			).collect(Collectors.toList()),
						Stream.concat(Stream.concat(	lIds.get(1).stream(),				Stream.of(ns.get(3).cpt)),	rev.apply(lIds.get(5)).stream()	).collect(Collectors.toList()),
						Stream.concat(Stream.concat(	rev.apply(lIds.get(2)).stream(),	Stream.of(ns.get(4).cpt)),	rev.apply(lIds.get(6)).stream()	).collect(Collectors.toList()),
						Stream.concat(Stream.concat(	rev.apply(lIds.get(3)).stream(),	Stream.of(ns.get(5).cpt)),	lIds.get(7).stream()			).collect(Collectors.toList())
					));
					var simpl = List.of(ncs.simplify(ns.get(2)), ncs.simplify(ns.get(3)), ncs.simplify(ns.get(4)), ncs.simplify(ns.get(5)));
					for(int i = 0; i < 4; i++) if(!simpl.get(i).from.equals(ns.get(0).ID)) expected.set(i, rev.apply(expected.get(i)));
					for(int i = 0; i < 4; i++) assertEquals(expected.get(i), simpl.get(i).cpts, String.format("Simplification - CPT order not conserved [direction: %s]", i));
				}
				//Desimplification
				var aEmptyLink = aChangeSet.createLink(aNodes.get(4), aNodes.get(5), new ArrayList<>());
				rawChanges = aChangeSet.changes.size(); uniqueChanges = (int) aChangeSet.accChanges().count();
				assertNull(aChangeSet.desimplify(aEmptyLink, nIds.get(4)));
				assertEquals(rawChanges, aChangeSet.changes.size());
				assertEquals(uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(aEmptyLink));
				assertNull(aChangeSet.desimplify(newLink, nIds.get(1)));
				assertEquals(rawChanges, aChangeSet.changes.size());
				assertEquals(uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(newLink));
				assertNull(aChangeSet.desimplify(newLink, nIds.get(3)));
				assertEquals(rawChanges, aChangeSet.changes.size());
				assertEquals(uniqueChanges, aChangeSet.accChanges().count());
				assertTrue(resultingElemState.apply(newLink));
				{ //Medium check
					var ncs = sampleMesher.newChangeSetPrimitive(id -> null);
					var ns = List.of(ncs.createNode(nIds.get(0), dummyMedium), ncs.createNode(nIds.get(1), dummyMedium), ncs.createNode(nIds.get(2), dummyMedium2));
					var l1 = ncs.createLink(ns.get(0), ns.get(1), lIds.get(0));
					var l2 = ncs.createLink(ns.get(1), ns.get(2), lIds.get(1));
					var l3 = ncs.createLink(ns.get(2), ns.get(0), lIds.get(2));
					assertNull(ncs.desimplify(l2, lIds.get(1).get(2)));
					assertNull(ncs.desimplify(l3, lIds.get(1).get(2)));
					var nn = ncs.desimplify(l1, lIds.get(0).get(2));
					assertNotNull(nn);
					assertSame(dummyMedium, nn.n2.medium);
				}
				var desim = aChangeSet.desimplify(newLink, nIds.get(2));
				assertNotNull(desim);
				assertEquals(rawChanges += 3 + (1 + 3 + 3), aChangeSet.changes.size());
				assertEquals(uniqueChanges = uniqueChanges - 1 + 3, aChangeSet.accChanges().count());
				assertFalse(resultingElemState.apply(newLink));
				assertTrue(resultingElemState.apply(desim.l1));
				assertTrue(resultingElemState.apply(desim.n2));
				assertTrue(resultingElemState.apply(desim.l3));
				assertEquals(aNodes.get(2).cpt, desim.n2.cpt);
				if(newLink.from.equals(aNodes.get(1).ID)){ //[n1]≣l1≣[n2]≣l2≣[n3] ➡ [n1]≣l≣[n3]
					assertEquals(aLink1.cpts, desim.l1.cpts);
					assertEquals(aLink2.cpts, desim.l3.cpts);
				} else { //[n1]≣l2≣[n2]≣l1≣[n3] ➡ [n1]≣l≣[n3]
					assertEquals(aLink1.cpts, rev.apply(desim.l3.cpts));
					assertEquals(aLink2.cpts, rev.apply(desim.l1.cpts));
				}
			}

			@Test
			public void testBasicApplyWhatever(){
				final var nIds = Stream.generate(CPTId::new).limit(3).collect(Collectors.toList());
				final var lIds = Stream.generate(() -> Stream.generate(CPTId::new).limit(2).collect(Collectors.toList())).limit(8).collect(Collectors.toList());
				var aChangeSet = lazyNewChangeSet.get();
				aChangeSet.createNode(nIds.get(0), dummyMedium);
				aChangeSet.apply(Stream.empty()).apply(DummyCPT::new); //We don't care about CPT propagations, just yet
				assertTrue(sampleMesher.isValid());
				assertEquals(1, sampleMesher.meshes.size());
				var aMesh = sampleMesher.meshes.values().stream().findAny().orElse(null);
				assertNotNull(aMesh);
				assertEquals(1, aMesh.elements.size());
				var aElem = aMesh.elements.values().stream().findAny().orElse(null);
				assertNotNull(aElem);
				assertTrue(aElem instanceof PhysicalMesher.Node);
				assertEquals(nIds.get(0), ((PhysicalMesher.Node) aElem).cpt);
				aChangeSet = lazyNewChangeSet.get();
				aChangeSet.destroyNode(aElem.ID);
				aChangeSet.apply(sampleMesher.meshes.values().stream()).apply(DummyCPT::new);
				assertTrue(sampleMesher.isValid());
				assertTrue(sampleMesher.meshes.isEmpty());
				//Basic mesh merger & splitter tests, extended ones shipped separately
				aChangeSet = lazyNewChangeSet.get();
				var n1 = aChangeSet.createNode(nIds.get(1), dummyMedium);
				var n2 = aChangeSet.createNode(nIds.get(2), dummyMedium);
				aChangeSet.apply(sampleMesher.meshes.values().stream()).apply(DummyCPT::new);
				assertTrue(sampleMesher.isValid());
				assertEquals(2, sampleMesher.meshes.size());
				aChangeSet = lazyNewChangeSet.get();
				var l1 = aChangeSet.createLink(n1, n2, lIds.get(0));
				aChangeSet.apply(sampleMesher.meshes.values().stream()).apply(DummyCPT::new);
				assertTrue(sampleMesher.isValid());
				assertEquals(1, sampleMesher.meshes.size());
				aMesh = sampleMesher.meshes.values().stream().findAny().orElse(null);
				assertNotNull(aMesh);
				assertEquals(3, aMesh.elements.size());
				aChangeSet = lazyNewChangeSet.get();
				aChangeSet.destroyLink(l1);
				aChangeSet.apply(sampleMesher.meshes.values().stream()).apply(DummyCPT::new);
				assertTrue(sampleMesher.isValid());
				assertEquals(2, sampleMesher.meshes.size());
			}

		}

	}

	static class DummyMedium implements Medium {

		public final String name;

		public DummyMedium(String name){
			this.name = name;
		}

		@NonNull
		@Override
		public String getName(){
			return name;
		}

		@Override
		public @NonNull OptionalDouble decay(@NonNull String layer, @NonNegative double dist){
			return OptionalDouble.of(1);
		}

	}

	static class DummyCPT implements ConnectPassthrough {

		public final CPTId id;

		public DummyCPT(CPTId id){
			this.id = id;
		}

		@Override
		public @NonNull CPTId getId(){ return id; }

		@Override
		public @NonNull CPTHub getHub(){ return null; }

		@Override
		public @NonNull Medium medium(){ return null; }

		@Override
		public @NonNull double selfLength(){ return 0; }

		@Override
		public @NonNull <CP extends ConnectParam<CP>> Optional<CP> passthroughLimit(@NonNull String layer){ return Optional.empty(); }

		protected GlobalMeshLoc gmLoc;
		@Override
		public void setGMLoc(@NonNull GlobalMeshLoc loc){ this.gmLoc = loc; }
		@Override
		public @NonNull GlobalMeshLoc getGMLoc(){ return gmLoc; }
	}

}
