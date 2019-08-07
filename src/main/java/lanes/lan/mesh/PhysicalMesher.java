package lanes.lan.mesh;

import lanes.ConnectParam;
import lanes.Layer;
import lanes.TestTortoise;
import lanes.Validatable;
import lanes.lan.*;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @param <CP> connect param & layer specific types
 * @param <L> connect param & layer specific type
 * @param <M> base {@linkplain Meshable meshable} [grouping] type
 */
public class PhysicalMesher<CP extends ConnectParam<CP>, L extends Layer<CP, L>, M extends Meshable> implements PhysicalPreprocessor<CP, L>, PhysicalListener<M>, Validatable {

	private final L.Instance<M> layer;

	public PhysicalMesher(L.Instance<M> layer){
		this.layer = layer;
	}

	@NonNull
	@Override
	public L.Instance<?> getLayerInstance(){
		return layer;
	}

	//Listener

	@Override
	public void onCreated(@NonNull M m){
		if(!m.existsIn(getLayer())) return;
		if(m instanceof CPTHub) onCPTCreated((CPTHub) m);
		else throw new IllegalArgumentException(String.format("Given Meshable type [%s] is not supported by this mesher", m.getClass().getName()));
	}

	@Override
	public void onDestroyed(@NonNull M m){
		if(!m.existsIn(getLayer())) return;
		if(m instanceof CPTHub) onCPTDestroyed((CPTHub) m);
		else throw new IllegalArgumentException(String.format("Given Meshable type [%s] is not supported by this mesher", m.getClass().getName()));
	}

	@Override
	public void onLoaded(@NonNull M m){
		throw new UnsupportedOperationException("Load state handling not yet implemented");
	}

	@Override
	public void onUnloaded(@NonNull M m){
		throw new UnsupportedOperationException("Load state handling not yet implemented");
	}

	//CPT

	protected void onCPTCreated(@NonNull CPTHub cpt){

	}

	protected void onCPTDestroyed(@NonNull CPTHub cpt){

	}

	//Pre-processor

	//Test 🐢

	@Override
	public boolean isValid(){
		return meshes.values().stream().allMatch(Mesh::isValid);
	}

	//Meshing

	protected final Map<MeshId, Mesh> meshes = new HashMap<>();

	@NonNull
	protected Optional<Mesh> getMesh(@NonNull MeshId id){
		return Optional.ofNullable(meshes.get(id));
	}
	protected boolean meshExists(@NonNull MeshId id){ return getMesh(id).isPresent(); }

	protected void addMeshRaw(@NonNull Mesh mesh){ meshes.put(mesh.ID, mesh); }
	protected void removeMeshRaw(@NonNull MeshId mesh){ meshes.remove(mesh); }
	protected void removeMeshRaw(@NonNull Mesh mesh){ removeMeshRaw(mesh.ID); }

	protected Mesh newMesh(){ return new Mesh(); }
	public class Mesh implements Validatable {

		public final MeshId ID;

		protected final Map<MeshElemId, MeshElem> elements = new HashMap<>();

		protected Mesh(){
			ID = new MeshId();
		}

		@NonNull
		protected <E extends MeshElem> Optional<E> getElem(@NonNull MeshElemId id){
			return Optional.ofNullable((E) elements.get(id));
		}
		protected boolean hasElem(@NonNull MeshElemId id){ return getElem(id).isPresent(); }

		@NonNull
		protected <E extends MeshElem> E getPresentElem(@NonNull MeshElemId id){
			return this.<E>getElem(id).orElseThrow(() -> new IllegalArgumentException(String.format("You though this mesh had element @[%s]?! Well, you failed :P", id)));
		}

		protected void addElemRaw(@NonNull MeshElem elem){ elements.put(elem.ID, elem); }

		protected void removeElemRaw(@NonNull MeshElemId elem){ elements.remove(elem); }
		protected void removeElemRaw(@NonNull MeshElem elem){ removeElemRaw(elem.ID); }

		@Override
		public boolean isValid(){
			return !elements.isEmpty() && elements.values().stream().allMatch(e -> e.isValid(this));
		}

		@Override
		public boolean equals(Object o){
			if(this == o) return true;
			if(!(o instanceof PhysicalMesher.Mesh)) return false;
			Mesh mesh = (Mesh) o;
			return ID.equals(mesh.ID);
		}

		@Override
		public int hashCode(){
			return Objects.hash(ID);
		}
	}

	/*
	 * Pseudo-Statics
	 *
	 * ⚠ Raw ⇒ Updates *Not* Propagated Beyond change level
	 */

	/*
	 * Changes
	 */

	protected ChangeSetBasic newChangeSetBasic(@NonNull List<CPTChange> cptChanges, @NonNull List<Mesh> changed, @NonNull List<Mesh> destroyed, @NonNull List<Mesh> created){ return new ChangeSetBasic(cptChanges, changed, destroyed, created); }
	public class ChangeSetBasic {

		protected final List<Mesh> changed, destroyed, created;
		protected final List<CPTChange> cptChanges;

		/**
		 * Constructs mid-order change set (usually based on low-order changes).<br>
		 * <br>
		 * The changes to elements have already been applied (both creation/destruction as well as relocation between meshes). The application allowed to compute the changes to CPTs (which are more useful to determining actual changes, as mesh elements are immutable by definition). Therefore <code>cptChanges</code> are accumulated&finalized and can be used safely to determine changes to propagate, however CPTs do not yet contain updated mesh element ownership info.<br>
		 * <br>
		 * The 3 mesh lists contain changes to meshes, which in contrary have <i>not</i> been applied:
		 * <ul>
		 *     <li><code>destroyed</code>&<code>created</code> contain destroyed and created meshes respectively (as result of element changes)</li>
		 *     <li><code>changed</code> contains all meshes that were somehow, but not entirely, affected by element changes</li>
		 * </ul>
		 * Destroyed and created meshes are <i>not</i> included in changed, as their contained elements have changed entirely. In fact, all <code>changed</code> meshes have at least 1 element that was contained before, and is contained after all modifications.<br>
		 * <br>
		 * When applied, this change set will apply mesh changes (deleting destroyed meshes from memory and registering created meshes) and will update CPT ownership information following CPT changes.
		 *
		 * @param cptChanges changes to the CPTs; all have already been applied (both creation/destruction as well as relocation between meshes)
		 * @param changed changed meshes - all meshes that were somehow, but not entirely, affected by element changes
		 * @param destroyed destroyed meshes
		 * @param created created meshes
		 */
		protected ChangeSetBasic(@NonNull List<CPTChange> cptChanges, @NonNull List<Mesh> changed, @NonNull List<Mesh> destroyed, @NonNull List<Mesh> created){
			this.cptChanges = Collections.unmodifiableList(cptChanges);
			this.changed = changed;
			this.destroyed = destroyed;
			this.created = created;
		}

		protected void apply(@NonNull Function<CPTId, ConnectPassthrough> ptSupplier){
			destroyed.forEach(PhysicalMesher.this::removeMeshRaw);
			created.forEach(PhysicalMesher.this::addMeshRaw);
			cptChanges.stream().filter(CPTChange::exists).forEach(Δ -> ptSupplier.apply(Δ.cpt).setGMLoc(new GlobalMeshLoc(Δ.newMesh.ID, Δ.newElem.ID)));
			//TODO Return next order change set
		}
	}

	public class CPTChange {

		public final CPTId cpt;

		public final MeshElem prevElem, newElem;

		public final Mesh prevMesh, newMesh;

		protected CPTChange(@NonNull CPTId cpt, @Nullable MeshElem prevElem, @Nullable MeshElem newElem, @Nullable Mesh prevMesh, @Nullable Mesh newMesh){
			this.cpt = cpt;
			this.prevElem = prevElem;
			this.newElem = newElem;
			this.prevMesh = prevMesh;
			this.newMesh = newMesh;
		}

		protected CPTChange(@NonNull CPTId cpt, @Nullable MeshElem elem, @Nullable Mesh mesh){
			this(cpt, elem, elem, mesh, mesh);
		}

		protected CPTChange(@NonNull CPTId cpt){
			this(cpt, null, null, null, null);
		}

		//Accumulation

		@NonNull
		protected CPTChange change(@Nullable MeshElem newElem, @Nullable Mesh newMesh){
			return new CPTChange(cpt, prevElem, newElem, prevMesh, newMesh);
		}

		@NonNull
		protected CPTChange changeElem(@Nullable MeshElem newElem){
			return change(newElem, newMesh);
		}
		@NonNull
		protected CPTChange changeMesh(@Nullable Mesh newMesh){
			return change(newElem, newMesh);
		}

		@NonNull
		protected CPTChange destroy(){
			return change(null, null);
		}

		@NonNull
		protected CPTChange then(@NonNull CPTChange change){
			if(change.cpt != cpt) throw new IllegalArgumentException("Changes accumulate only over the same element!");
			if(change.prevElem != newElem || change.prevMesh != newMesh) throw new IllegalArgumentException("Cannot accumulate non-consecutive changes!");
			return change(change.newElem, change.newMesh);
		}

		//Finalized access

		public boolean cptCreated(){
			return prevElem == null && prevMesh == null;
		}
		public boolean cptDestroyed(){
			return newElem == null && newMesh == null;
		}
		public boolean exists(){
			return !cptDestroyed();
		}

		public boolean changed(){
			return elementChanged() || meshChanged();
		}
		public boolean elementChanged(){
			return newElem != prevElem;
		}
		public boolean meshChanged(){
			return newMesh != prevMesh;
		}

	}

	protected ChangeSetPrimitive newChangeSetPrimitive(@NonNull Function<MeshElemId, MeshElem> elemSupp){ return new ChangeSetPrimitive(elemSupp); }
	public class ChangeSetPrimitive {

		private final Function<MeshElemId, MeshElem> elemSupp;
		protected final Map<MeshElemId, MeshElem> elementsCache = new HashMap<>();

		/**
		 * Constructs low-order change set.<br>
		 * Allows to perform all low-order operations, as well as a few mid and high-order, directly on elements, without worrying about meshes and propagating the changes just yet.
		 *
		 * @param elemSupp element accessor by its' GUUID (because the change set bypasses meshes, and thus is to some extent detached from the mesher)
		 */
		public ChangeSetPrimitive(@NonNull Function<MeshElemId, MeshElem> elemSupp){
			this.elemSupp = elemSupp;
		}

		@NonNull
		protected <E extends MeshElem> E getElem(@NonNull MeshElemId id){
			var elem = elementsCache.get(id);
			if(elem == null) elementsCache.put(id, elem = elemSupp.apply(id));
			return (E) elem;
		}

		//protected final List<MeshElem> destroyed = new ArrayList<>(), created = new ArrayList<>();
		protected final List<MeshElemChange> changes = new ArrayList<>();

		//LO-create

		@NonNull
		private <E extends MeshElem> E created(@NonNull E e){
			changes.add(new MeshElemChange(e, false).create());
			elementsCache.put(e.ID, e);
			return e;
		}

		@NonNull
		protected Node createNode(@NonNull CPTId cpt, @NonNull Medium medium){
			return created(newNode(cpt, medium));
		}

		protected void nodeAddLink(@NonNull Node node, @NonNull Link link){
			node.addLinkRaw(link.ID);
			changes.add(new MeshElemChange(node, true, new MeshElemInnerChange("links", link.ID, false, true)));
		}

		@NonNull
		protected Link createLink(@NonNull Node from, @NonNull Node to, List<CPTId> cpts){
			var link = newLink(from.ID, to.ID, new ArrayList<>(cpts));
			nodeAddLink(from, link);
			nodeAddLink(to, link);
			return created(link);
		}
		@NonNull protected Link createLink(@NonNull MeshElemId from, @NonNull MeshElemId to, List<CPTId> cpts){ return createLink(this.getElem(from), this.getElem(to), cpts); }

		//LO-destroy

		private <E extends MeshElem> void destroyed(@NonNull E e){
			changes.add(new MeshElemChange(e, true).destroy());
		}

		protected void nodeRemoveLink(@NonNull Node node, @NonNull Link link){
			node.removeLinkRaw(link.ID);
			changes.add(new MeshElemChange(node, true, new MeshElemInnerChange("links", link.ID, true, false)));
		}
		protected void nodeRemoveLink(@NonNull MeshElemId node, @NonNull Link link){ nodeRemoveLink(getElem(node), link); }

		protected void destroyLink(@NonNull Link link){
			nodeRemoveLink(link.from, link);
			nodeRemoveLink(link.to, link);
			destroyed(link);
		}
		protected void destroyLink(@NonNull MeshElemId link){ destroyLink(getElem(link)); }

		protected void destroyNode(@NonNull Node node){
			new ArrayList<>(node.links).stream().map(this::<Link>getElem).forEach(this::destroyLink);
			destroyed(node);
		}
		protected void destroyNode(@NonNull MeshElemId node){ destroyNode(getElem(node)); }

		//MO-e

		@Nullable
		protected Link simplify(@NonNull Node node){
			if(node.links.size() != 2) return null;
			var links = node.links.stream().map(this::<Link>getElem).collect(Collectors.toList());
			var l1 = links.get(0);
			var l2 = links.get(1);
			var from = this.<Node>getElem(l1.getOtherEnd(node.ID));
			var to = this.<Node>getElem(l2.getOtherEnd(node.ID));
			if(from == to) return null;
			var medium = node.medium;
			if(medium != from.medium || medium != to.medium) return null;
			if(!node.canBeSimplified()) return null;
			var cpts = node.getCPTs();
			if(l1.from == node.ID){
				var lc = new ArrayList<>(l1.cpts);
				Collections.reverse(lc);
				cpts = Stream.concat(lc.stream(), cpts);
			} else cpts = Stream.concat(l1.getCPTs(), cpts);
			if(l2.to == node.ID){
				var lc = new ArrayList<>(l2.cpts);
				Collections.reverse(lc);
				cpts = Stream.concat(cpts, lc.stream());
			} else cpts = Stream.concat(cpts, l2.getCPTs());
			destroyNode(node);
			return createLink(from, to, cpts.collect(Collectors.toList()));
		}

		@Nullable
		protected DesimplificationResult desimplify(@NonNull Link link, @NonNull CPTId cpt){
			var index = link.cpts.indexOf(cpt);
			if(index < 0 || link.cpts.size() <= index) return null;
			var n1 = this.<Node>getElem(link.from);
			var n3 = this.<Node>getElem(link.to);
			if(n1.medium != n3.medium) return null;
			var medium = n1.medium;
			var n2 = createNode(cpt, medium);
			var l1 = createLink(n1, n2, link.cpts.subList(0, index));
			var l3 = createLink(n2, n3, link.cpts.subList(index+1, link.cpts.size()));
			destroyLink(link);
			return new DesimplificationResult(l1, n2, l3);
		}

		public class DesimplificationResult {

			/*
			 * [a]≣cpts-c-stpc≣[b] ➡ [a]≣cpts≣[c]≣stpc≣[b]
			 * ⇔
			 * [?]≣l≣[?] ➡ [?]≣l1≣[n2]≣l3≣[?]
			 */
			public final Link l1;
			public final Node n2;
			public final Link l3;

			public DesimplificationResult(@NonNull Link l1, @NonNull Node n2, @NonNull Link l3){
				this.l1 = l1;
				this.n2 = n2;
				this.l3 = l3;
			}

		}

		//Apply

		@NonNull
		protected Stream<MeshElemChange> accChanges(){
			return changes.stream().collect(Collectors.groupingBy(e -> e.elem.ID, LinkedHashMap::new, Collectors.reducing(MeshElemChange::then))).values().stream().filter(Optional::isPresent).map(Optional::get).filter(MeshElemChange::changed);
		}

		/**
		 * Applies the primitive change set and computes the change set of higher order.<br>
		 * Modifies content of meshes, deleting destroyed elements from memory and registering created ones. Rechecks meshes, and computes whether any changes occurred for individual CPTs, but does not apply either, packing them in the {@linkplain ChangeSetBasic basic change set} instead.<br>
		 * <br>
		 * No modifications occur before beginning of the application. Therefore the change set can be simply forgotten if needed to discard; and multiple change sets can be constructed simultaneously (considering they operate disjointedly).<br>
		 * <br>
		 * <b>⚠ WARNING: </b>Once <code>apply</code> is invoked on the lowest order change set, there is no going back - <b>every higher-order change set must be applied</b>, until reaching the Δ-descriptor (highest-order change set describing all the changes, but with none left to apply), and <b>BEFORE</b> beginning next application of any other change set (even if disjoint).
		 * Doing otherwise will throw the entire {@linkplain PhysicalMesher mesher} in limbo, where it does not have to obey any API definitions (including nullability) and is equivalent to driving the entire engine into UB state.
		 * @param mic potentially affected meshes (all meshes can be dumped, it is however recommended to pre-filter to a narrower set of meshes for significant performance benefits)
		 * @return basic change set
		 */
		@NonNull
		public ChangeSetBasic apply(@NonNull Stream<Mesh> mic){
			var changes = accChanges().collect(Collectors.toList());
			//TODO this is rather inefficient way of filtering affected meshes, especially if all existing are dumped. However, it also computes `elem2meshBefore` at the same time. Consider whether optimizations are necessary.
			var μeshes = mic.collect(Collectors.toSet()); //meshes but can include completely unaffected
			var elem2meshBefore = changes.stream().map(ch -> ch.elem.ID).collect(Collectors.toMap(Function.identity(), id -> μeshes.stream().filter(m -> m.hasElem(id)).findAny())); //before
			var meshes = elem2meshBefore.values().stream().flatMap(Optional::stream).collect(Collectors.toSet()); //μeshes but filtered to affected

			var elems = changes.stream().filter(c -> c.newState).map(e -> e.elem).collect(Collectors.toMap(e -> e.ID, Function.identity()));
			var ffElems = new HashMap<Mesh, Set<MeshElem>>();
			Consumer<Mesh> meshRefill = mesh -> {
				if(mesh.elements.isEmpty()) return;
				var selem = mesh.elements.values().stream().filter(e -> elems.containsKey(e.ID)).findAny();
				if(selem.isEmpty()){ //IFF all elems from this mesh have been relocated to other mesh(es)
					ffElems.put(mesh, new HashSet<>());
					return;
				}
				var proc = new HashSet<MeshElem>();
				Queue<MeshElem> q = new LinkedList<>();
				Consumer<MeshElem> nom = e -> {
					if(e != null && !proc.contains(e)){
						proc.add(e);
						e.adjacent().map(elems::remove).forEach(q::offer);
					}
				};
				elems.remove(selem.get().ID);
				nom.accept(selem.get());
				while(!q.isEmpty()) nom.accept(q.poll());
				ffElems.put(mesh, proc);
				//TODO This is complicated and must be thoroughly tested!
			};
			meshes.forEach(meshRefill);
			while(!elems.isEmpty()){ //IFF and while there are previously non-existing disjoint meshes
				var el = elems.values().stream().findAny().get();
				var mesh = newMesh(); //TODO maybe track separately the created meshes?
				mesh.addElemRaw(el);
				meshRefill.accept(mesh);
			}
			var elem2meshAfter = new HashMap<MeshElem, Optional<Mesh>>();
			ffElems.forEach(((mesh, es) -> es.forEach(e -> elem2meshAfter.put(e, Optional.of(mesh)))));
			/*
			 * So, at this point, we're only interested in in `elem2meshBefore`, `elem2meshAfter` and `ffElems`.
			 *
			 * Any change of mesh of an element can be observed with `elem2meshBefore` and `elem2meshAfter`, respectively before and after, with these special cases:
			 * 	empty -> a mesh			:	created
			 * 	a mesh -> absent/empty	:	destroyed
			 *
			 * `ffElems` provides information about mesh changes, more precisely elements contained in the mesh after the change. Special cases:
			 * 	an empty set indicates the mesh no longer exists (all elements have been destroyed and/or distributed to other mesh(es))
			 * 	if the mesh is not contained in `meshes`, it has been created. (as per the to-do, we may want to track them in an additional collection)
			 */
			var cptChanges = changes.stream().flatMap(change -> change.elem.getCPTs().map(cpt -> new CPTChange(cpt, change.prevState ? change.elem : null, change.newState ? change.elem : null, elem2meshBefore.get(change.elem.ID).orElse(null), Optional.ofNullable(elem2meshAfter.get(change.elem)).flatMap(Function.identity()).orElse(null)))).collect(Collectors.groupingBy(c -> c.cpt, LinkedHashMap::new, Collectors.reducing(CPTChange::then))).values().stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
			List<Mesh> mA = new ArrayList<>(), mD = new ArrayList<>(), mC = new ArrayList<>();
			ffElems.forEach((m, es) -> {
				boolean c = !meshes.contains(m), d = es.isEmpty();
				if(!(c && d)) (d ? mD : c ? mC : mA).add(m);
			});
			mA.forEach(mesh -> {
				mesh.elements.clear();
				ffElems.get(mesh).forEach(mesh::addElemRaw);
			});
			return newChangeSetBasic(cptChanges, mA, mD, mC);
		}

	}

	public class MeshElemChange {

		public final MeshElem elem;
		public final boolean prevState;
		public final boolean newState;
		public final Map<PropertyComponent, MeshElemInnerChange> innerChanges;

		protected MeshElemChange(@NonNull MeshElem elem, boolean prevState, boolean newState, @NonNull Map<PropertyComponent, MeshElemInnerChange> innerChanges){
			this.elem = elem;
			this.prevState = prevState;
			this.newState = newState;
			this.innerChanges = Map.copyOf(innerChanges);
		}

		protected MeshElemChange(@NonNull MeshElem elem, boolean prevState, boolean newState){
			this(elem, prevState, newState, new HashMap<>());
		}

		protected MeshElemChange(@NonNull MeshElem elem, boolean state, @NonNull Map<PropertyComponent, MeshElemInnerChange> innerChanges){
			this(elem, state, state, innerChanges);
		}

		protected MeshElemChange(@NonNull MeshElem elem, boolean state, @NonNull MeshElemInnerChange innerChange){
			this(elem, state, Map.of(innerChange.propCo, innerChange));
		}

		protected MeshElemChange(@NonNull MeshElem elem, boolean state){
			this(elem, state, new HashMap<>());
		}

		//Acc

		@NonNull
		protected MeshElemChange create(){
			return new MeshElemChange(elem, prevState, true);
		}
		@NonNull
		protected MeshElemChange destroy(){
			return new MeshElemChange(elem, prevState, false);
		}

		@NonNull
		protected MeshElemChange then(@NonNull MeshElemChange change){
			if(change.elem != elem) throw new IllegalArgumentException("Changes accumulate only over the same element!");
			if(change.prevState != newState) throw new IllegalArgumentException("Cannot accumulate non-consecutive changes!");
			return new MeshElemChange(elem, prevState, change.newState, innerChanges.values().stream().map(ic -> Optional.ofNullable(change.innerChanges.get(ic.propCo)).map(ic::then).orElse(ic)).collect(Collectors.toMap(ic -> ic.propCo, Function.identity())));
		}

		//Fac

		public boolean changed(){
			return newState != prevState || !innerChanges.isEmpty();
		}

		public boolean created(){
			return !prevState && newState;
		}
		public boolean destroyed(){
			return prevState && !newState;
		}

	}

	protected static class PropertyComponent<T> {

		protected final String prop;
		protected final T component;

		public PropertyComponent(@NonNull String prop, @NonNull T component){
			this.prop = prop;
			this.component = component;
		}

		@Override
		public boolean equals(Object o){
			if(this == o) return true;
			if(!(o instanceof PropertyComponent)) return false;
			PropertyComponent<?> that = (PropertyComponent<?>) o;
			return prop.equals(that.prop) && component.equals(that.component);
		}

		@Override
		public int hashCode(){
			return Objects.hash(prop, component);
		}

	}

	protected class MeshElemInnerChange<T> {

		protected final PropertyComponent<T> propCo;
		protected final boolean prevState, newState;

		protected MeshElemInnerChange(@NonNull PropertyComponent<T> propCo, boolean prevState, boolean newState){
			this.propCo = propCo;
			this.prevState = prevState;
			this.newState = newState;
		}

		public MeshElemInnerChange(@NonNull String prop, @NonNull T component, boolean prevState, boolean newState){
			this(new PropertyComponent<>(prop, component), prevState, newState);
		}

		@NonNull
		protected MeshElemInnerChange<T> then(@NonNull MeshElemInnerChange<T> change){
			if(!change.propCo.equals(propCo)) throw new IllegalArgumentException("Changes accumulate only over the same property!");
			if(change.prevState != newState) throw new IllegalArgumentException("Cannot accumulate non-consecutive changes!");
			return new MeshElemInnerChange<>(propCo, prevState, change.newState);
		}

		public boolean changed(){
			return prevState != newState;
		}

	}

	/*
	 * Elements
	 */

	protected abstract class MeshElem {

		@NonNull public final MeshElemId ID;

		protected MeshElem(){
			ID = new MeshElemId();
		}

		@NonNull
		protected abstract Stream<CPTId> getCPTs();
		@NonNull
		protected abstract Stream<MeshElemId> adjacent();

		@TestTortoise
		protected boolean isValid(@NonNull Mesh mesh){
			return adjacent().allMatch(mesh::hasElem);
		}

		@Override
		public boolean equals(Object o){
			if(this == o) return true;
			if(!(o instanceof PhysicalMesher.MeshElem)) return false;
			MeshElem meshElem = (MeshElem) o;
			return ID.equals(meshElem.ID);
		}

		@Override
		public int hashCode(){
			return Objects.hash(ID);
		}
	}

	protected Node newNode(@NonNull CPTId cpt, @NonNull Medium medium){ return new Node(cpt, medium); }
	public class Node extends MeshElem {

		protected final CPTId cpt;
		protected final Medium medium;

		protected final Set<MeshElemId> links = new HashSet<>();

		public Node(@NonNull CPTId cpt, @NonNull Medium medium){
			this.cpt = cpt;
			this.medium = medium;
		}

		@Override
		protected @NonNull Stream<CPTId> getCPTs(){
			return Stream.of(cpt);
		}
		@Override
		protected @NonNull Stream<MeshElemId> adjacent(){
			return links.stream();
		}

		protected void addLinkRaw(@NonNull MeshElemId link){
			links.add(link);
		}

		protected void removeLinkRaw(@NonNull MeshElemId link){
			links.remove(link);
		}

		/**
		 * Checks whether this node can be simplified.<br>
		 * Note: having exactly 2 links is a hard requirement (which is hardcoded externally), thus can be ignored here.
		 * @return whether <code>this</code> node can be simplified, ignoring 2 links requirement
		 */
		public boolean canBeSimplified(){
			return true;
		}

		@Override
		protected boolean isValid(@NonNull Mesh mesh){
			return super.isValid(mesh) && links.stream().map(mesh::<Link>getPresentElem).allMatch(l -> l.from.equals(ID) || l.to.equals(ID));
		}

	}

	protected Link newLink(@NonNull MeshElemId from, @NonNull MeshElemId to, @NonNull List<CPTId> cpts){ return new Link(from, to, cpts); }
	public class Link extends MeshElem {

		@NonNull protected final MeshElemId from;
		@NonNull protected final MeshElemId to;

		protected final List<CPTId> cpts;

		protected Link(@NonNull MeshElemId from, @NonNull MeshElemId to, @NonNull List<CPTId> cpts){
			this.from = from;
			this.to = to;
			this.cpts = List.copyOf(cpts);
		}

		@Override
		protected @NonNull Stream<CPTId> getCPTs(){
			return cpts.stream();
		}
		@Override
		protected @NonNull Stream<MeshElemId> adjacent(){
			return Stream.of(from, to);
		}

		@NonNull
		public MeshElemId getOtherEnd(@NonNull MeshElemId aEnd){
			return aEnd.equals(from) ? to : from;
		}

		@Override
		protected boolean isValid(@NonNull Mesh mesh){
			return super.isValid(mesh) && mesh.<Node>getPresentElem(from).links.contains(ID) && mesh.<Node>getPresentElem(to).links.contains(ID);
		}

	}

}
