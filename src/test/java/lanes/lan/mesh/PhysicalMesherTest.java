package lanes.lan.mesh;

import lanes.ConnectParamDoubleSample;
import lanes.LayerDoubleSample;
import lanes.lan.CPTId;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class PhysicalMesherTest {

	LayerDoubleSample sampleLayer = new LayerDoubleSample("sample");

	@Nested
	class TestWithoutMeshables {

		PhysicalMesher<ConnectParamDoubleSample, LayerDoubleSample, Meshable> sampleMesher = new PhysicalMesher<>(sampleLayer);

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
			var aNode1 = sampleMesher.newNode(new CPTId());
			var aNode2 = sampleMesher.newNode(new CPTId());
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
			var aNode1 = sampleMesher.newNode(aId1);
			var aNode2 = sampleMesher.newNode(aId2);
			var aLink1 = sampleMesher.newLink(aNode1.ID, aNode2.ID, aIds1);
			var aLink2 = sampleMesher.newLink(aNode2.ID, aNode1.ID, aIds2);
			aNode1.addLinkRaw(aLink1.ID);
			aNode2.addLinkRaw(aLink1.ID);
			aNode1.addLinkRaw(aLink2.ID);
			aNode2.addLinkRaw(aLink2.ID);
			//adj
			assertEquals(Stream.of(aLink1.ID, aLink2.ID).collect(Collectors.toSet()), aNode1.adjacent().collect(Collectors.toSet()));
			assertEquals(Stream.of(aLink1.ID, aLink2.ID).collect(Collectors.toSet()), aNode2.adjacent().collect(Collectors.toSet()));
			assertEquals(Stream.of(aNode1.ID, aNode2.ID).collect(Collectors.toSet()), aLink1.adjacent().collect(Collectors.toSet()));
			assertEquals(Stream.of(aNode1.ID, aNode2.ID).collect(Collectors.toSet()), aLink2.adjacent().collect(Collectors.toSet()));
			aNode1.removeLinkRaw(aLink2.ID);
			aNode2.removeLinkRaw(aLink2.ID);
			assertEquals(Stream.of(aLink1.ID).collect(Collectors.toSet()), aNode1.adjacent().collect(Collectors.toSet()));
			assertEquals(Stream.of(aLink1.ID).collect(Collectors.toSet()), aNode2.adjacent().collect(Collectors.toSet()));
			//CPTs
			assertEquals(Stream.of(aId1).collect(Collectors.toSet()), aNode1.getCPTs().collect(Collectors.toSet()));
			assertEquals(Stream.of(aId2).collect(Collectors.toSet()), aNode2.getCPTs().collect(Collectors.toSet()));
			assertEquals(new HashSet<>(aIds1), aLink1.getCPTs().collect(Collectors.toSet()));
			assertEquals(new HashSet<>(aIds2), aLink2.getCPTs().collect(Collectors.toSet()));
		}

	}

}
