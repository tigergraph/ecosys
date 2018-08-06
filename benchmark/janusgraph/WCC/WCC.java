package WCC;

import org.apache.commons.configuration.Configuration; 
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer; 
import org.apache.tinkerpop.gremlin.process.computer.Memory;
import org.apache.tinkerpop.gremlin.process.computer.MessageCombiner;
import org.apache.tinkerpop.gremlin.process.computer.MessageScope;
import org.apache.tinkerpop.gremlin.process.computer.Messenger;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey; 
import org.apache.tinkerpop.gremlin.process.computer.MemoryComputeKey; 
import org.apache.tinkerpop.gremlin.process.computer.VertexProgram;
import org.apache.tinkerpop.gremlin.process.computer.util.AbstractVertexProgramBuilder; 
import org.apache.tinkerpop.gremlin.process.traversal.Traversal;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__; 
import org.apache.tinkerpop.gremlin.process.traversal.util.PureTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.util.ScriptTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.util.TraversalUtil; 
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory; 
import org.apache.tinkerpop.gremlin.util.iterator.IteratorUtils;

import java.util.Arrays; 
import java.util.HashSet;
import java.util.Optional;
import java.util.Set; 

import java.util.concurrent.ConcurrentHashMap;
/**
 * @author Litong Shen
 */

public class WCC implements VertexProgram<Long> {
	public static final String GROUP_ID = "WCC.groupId";
	private static final String INITIAL_WCC_TRAVERSAL = "WCC.initialWCCTraversal";
	private static final String EDGE_TRAVERSAL = "WCC.edgeTraversal";
	private static final String ID = "id";

	private MessageScope.Local<Double> allMessageScope = MessageScope.Local.of(__::bothE);
	private MessageScope.Local<Double> countMessageScope = MessageScope.Local.of(new MessageScope.Local.ReverseTraversalSupplier(this.allMessageScope));

	private PureTraversal<Vertex, Edge> edgeTraversal = null; 
	private PureTraversal<Vertex, ? extends Number> initialWCCTraversal = null;  	
	private Set<VertexComputeKey> vertexComputeKeys;	
	private Set<MemoryComputeKey> memoryComputeKeys;
	private ConcurrentHashMap<Long, String> result = new ConcurrentHashMap<Long,String>();

	// defaut value for groupId, should be greater than any vertex ID
	private long defaultGroupId = Long.MAX_VALUE;
	
	private WCC() {
	
	}
	
	public void updateResult(ConcurrentHashMap<Long,String> result, Long delete){
		if(result.containsKey(delete)){
			result.remove(delete);
		}
	}
	
	public void initialResult(ConcurrentHashMap<Long,String> result, Long addNew){
		if(!result.containsKey(addNew)){
			result.put(addNew, "1");
		}
	}

	@Override
	public void loadState(final Graph graph, final Configuration configuration) {
		if(configuration.containsKey(INITIAL_WCC_TRAVERSAL))
			this.initialWCCTraversal = PureTraversal.loadState(configuration, INITIAL_WCC_TRAVERSAL, graph);
		if (configuration.containsKey(EDGE_TRAVERSAL)) {
			this.edgeTraversal = PureTraversal.loadState(configuration, EDGE_TRAVERSAL, graph);
			this.allMessageScope = MessageScope.Local.of(() -> this.edgeTraversal.get().clone());
			this.countMessageScope = MessageScope.Local.of(new MessageScope.Local.ReverseTraversalSupplier(this.allMessageScope));	
		}
		this.vertexComputeKeys = new HashSet<>(Arrays.asList(VertexComputeKey.of(this.GROUP_ID, true)));			
		this.memoryComputeKeys = new HashSet<>(Arrays.asList(MemoryComputeKey.of("Terminate", (a, b) -> {return (Boolean)a && (Boolean)b;}, true, true)));			
		
	}
	
	@Override
	public  void storeState(final Configuration configuration) {
		VertexProgram.super.storeState(configuration);
		if (null != this.edgeTraversal) 
			this.edgeTraversal.storeState(configuration, EDGE_TRAVERSAL); 
		if (null != this.initialWCCTraversal)
			this.initialWCCTraversal.storeState(configuration, INITIAL_WCC_TRAVERSAL);
	} 	
		
	@Override
	public  GraphComputer.ResultGraph getPreferredResultGraph() { 
		return GraphComputer.ResultGraph.NEW;
	}

	@Override 
	public  GraphComputer.Persist getPreferredPersist() {
		return GraphComputer.Persist.VERTEX_PROPERTIES;
	}
	
	@Override
	public  Set<VertexComputeKey> getVertexComputeKeys() {		
		return this.vertexComputeKeys;
	}

	@Override
	public  Optional<MessageCombiner<Long>> getMessageCombiner() {
		return (Optional) WCCMessageCombiner.instance();
	}

	@Override
    	public Set<MemoryComputeKey> getMemoryComputeKeys() {
        	return this.memoryComputeKeys;
    	}

	@Override
	public  Set<MessageScope> getMessageScopes(final Memory memory) {
		final Set<MessageScope> set = new HashSet<>();
		set.add(memory.isInitialIteration() ? this.countMessageScope : this.allMessageScope);
		return set;
	}

	@Override
	public  WCC clone() {
		try {
			final WCC clone = (WCC) super.clone();
			if (null != this.initialWCCTraversal)
				clone.initialWCCTraversal = this.initialWCCTraversal.clone();
			return clone;
		} catch (final CloneNotSupportedException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	@Override
	public  void setup(final Memory memory) {
	
	}

	@Override
	public  void execute(final Vertex vertex, Messenger<Long> messenger, final Memory memory) {

		Long groupId = vertex.value(GROUP_ID);
		boolean changedGroup = false;
		if(memory.isInitialIteration()){
			Long iniGroupId = Long.parseLong(vertex.value(ID));
                        vertex.property(VertexProperty.Cardinality.single, GROUP_ID, iniGroupId);
                        initialResult(result, iniGroupId);
                        messenger.sendMessage(this.countMessageScope, iniGroupId);

			memory.add("Terminate", false);	
		}else{
			
			//reducer recieved message
			
			Long newGroupId = IteratorUtils.reduce(messenger.receiveMessages(), defaultGroupId, (a, b) -> {
				long compareResult = a.compareTo(b);
				if(compareResult > 0) return b;
				else return a;	
			}); 	
			//update groupId then send Message
				
			if(newGroupId < groupId) {
				changedGroup = true;
				System.out.println("new groupId: " + newGroupId);	
				updateResult(result, groupId);
				vertex.property(VertexProperty.Cardinality.single, GROUP_ID, newGroupId);
				messenger.sendMessage(this.countMessageScope, newGroupId);
			}else{
				messenger.sendMessage(this.countMessageScope,groupId);
			}
			
			//update terminate condition
			if(changedGroup == true){
				memory.add("Terminate", false);
			}
		
		}

	}

	@Override	
	public  boolean terminate(final Memory memory) {
		if(memory.get("Terminate")) {
			System.out.println("##########################################################");
			System.out.println("Terminate");
			System.out.println("final group size: " + result.size());
			System.out.println("##########################################################");
			return true;

		}else {
			memory.set("Terminate", true);
			//Terminate = true;
			System.out.println("##########################################################");
			System.out.println("new group size: " + result.size());
			System.out.println("###########################################################");
			return false;
		}
	}

	//////////////////////////////
	
	public static  Builder build() {
		return new Builder();
	}

	public final static class Builder extends AbstractVertexProgramBuilder<Builder> {
		private Builder() {
			super(WCC.class);
		}

		public  Builder edges(final Traversal.Admin<Vertex, Edge> edgeTraversal) {
			PureTraversal.storeState(this.configuration, EDGE_TRAVERSAL, edgeTraversal);
			return this;
		}

		public  Builder initialRank(final Traversal.Admin<Vertex, ? extends Number> initialWCCTraversal) {
			PureTraversal.storeState(this.configuration, INITIAL_WCC_TRAVERSAL, initialWCCTraversal);
			return this;
		}

		@Deprecated
		public  Builder traversal(final TraversalSource traversalSource, final String scriptEngine, final String traversalScript, final Object... bindings) {
		    return this.edges(new ScriptTraversal<>(traversalSource, scriptEngine, traversalScript, bindings));
		}

		@Deprecated
		public  Builder traversal(final Traversal.Admin<Vertex, Edge> traversal) {
			return this.edges(traversal);
		}
	}

	////////////////////////////
	
	@Override
	public  Features getFeatures() {
		return new Features() {
			@Override
			public  boolean requiresLocalMessageScopes() {
				return true;
			}
			@Override
			public  boolean requiresVertexPropertyAddition() {
				return true;
			}
		};
	}	

}
