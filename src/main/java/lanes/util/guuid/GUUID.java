package lanes.util.guuid;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Objects;
import java.util.UUID;

public class GUUID {

	@NonNull
	private final UUID uuid;

	protected GUUID(@NonNull UUID uuid){
		this.uuid = uuid;
	}

	protected GUUID(){
		this(UUID.randomUUID());
	}

	@Override
	public boolean equals(Object o){
		if(this == o) return true;
		if(o == null || this.getClass() != o.getClass()) return false; //Type-strict
		GUUID guuid = (GUUID) o;
		return uuid.equals(guuid.uuid);
	}

	@Override
	public int hashCode(){
		return Objects.hash(uuid);
	}
}
