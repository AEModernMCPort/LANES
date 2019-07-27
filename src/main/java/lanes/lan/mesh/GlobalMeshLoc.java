package lanes.lan.mesh;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;

public final class GlobalMeshLoc {

	@NonNull public final MeshId mesh;
	@NonNull public final MeshElemId elem;

	public GlobalMeshLoc(@NonNull MeshId mesh, @NonNull MeshElemId elem){
		this.mesh = mesh;
		this.elem = elem;
	}

	@NonNull
	public GlobalMeshLoc setMesh(@NonNull MeshId mesh){
		return new GlobalMeshLoc(mesh, elem);
	}

	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(!(o instanceof GlobalMeshLoc)) return false;
		GlobalMeshLoc that = (GlobalMeshLoc) o;
		return mesh.equals(that.mesh) && elem.equals(that.elem);
	}

	@Override
	public int hashCode(){
		return Objects.hash(mesh, elem);
	}

}
