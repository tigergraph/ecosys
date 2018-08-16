package PG;

import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.computer.Memory;
import org.apache.tinkerpop.gremlin.process.computer.MessageCombiner;
import org.apache.tinkerpop.gremlin.process.computer.MessageScope;
import org.apache.tinkerpop.gremlin.process.computer.Messenger;
import org.apache.tinkerpop.gremlin.process.computer.VertexComputeKey;
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

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 * modified by Litong Shen litong.shen@gmail.com
 */

public class PG implements VertexProgram<Double> {


    //public static final String PAGE_RANK = "pageRank";
    public static final String PAGE_RANK = "gremlin.pageRankVertexProgram.pageRank";
    private static final String EDGE_COUNT = "gremlin.pageRankVertexProgram.edgeCount";
    private static final String PROPERTY = "gremlin.pageRankVertexProgram.property";
    private static final String VERTEX_COUNT = "gremlin.pageRankVertexProgram.vertexCount";
    private static final String ALPHA = "gremlin.pageRankVertexProgram.alpha";
    private static final String TOTAL_ITERATIONS = "gremlin.pageRankVertexProgram.totalIterations";
    private static final String EDGE_TRAVERSAL = "gremlin.pageRankVertexProgram.edgeTraversal";
    private static final String INITIAL_RANK_TRAVERSAL = "gremlin.pageRankVertexProgram.initialRankTraversal";

    private MessageScope.Local<Double> incidentMessageScope = MessageScope.Local.of(__::outE);
    private MessageScope.Local<Double> countMessageScope = MessageScope.Local.of(new MessageScope.Local.ReverseTraversalSupplier(this.incidentMessageScope));
    private PureTraversal<Vertex, Edge> edgeTraversal = null;
    private PureTraversal<Vertex, ? extends Number> initialRankTraversal = null;
    private double vertexCountAsDouble = 1.0d;
    private double alpha = 0.85d;
    private int totalIterations = 10;
    private String property = PAGE_RANK;
    private Set<VertexComputeKey> vertexComputeKeys;

    private PG() {

    }

    @Override
    public  void loadState(final Graph graph, final Configuration configuration) {
        if (configuration.containsKey(INITIAL_RANK_TRAVERSAL))
            this.initialRankTraversal = PureTraversal.loadState(configuration, INITIAL_RANK_TRAVERSAL, graph);
        if (configuration.containsKey(EDGE_TRAVERSAL)) {
            this.edgeTraversal = PureTraversal.loadState(configuration, EDGE_TRAVERSAL, graph);
            this.incidentMessageScope = MessageScope.Local.of(() -> this.edgeTraversal.get().clone());
            this.countMessageScope = MessageScope.Local.of(new MessageScope.Local.ReverseTraversalSupplier(this.incidentMessageScope));
        }
        this.vertexCountAsDouble = configuration.getDouble(VERTEX_COUNT, 1.0d);
        this.alpha = configuration.getDouble(ALPHA, 0.85d);

	// set iteration to 10	

        this.totalIterations = configuration.getInt(TOTAL_ITERATIONS, 10);
        this.property = configuration.getString(PROPERTY, PAGE_RANK);
        this.vertexComputeKeys = new HashSet<>(Arrays.asList(VertexComputeKey.of(this.property, false), VertexComputeKey.of(EDGE_COUNT, true)));
    }

    @Override
    public  void storeState(final Configuration configuration) {
        VertexProgram.super.storeState(configuration);
        configuration.setProperty(VERTEX_COUNT, this.vertexCountAsDouble);
        configuration.setProperty(ALPHA, this.alpha);
        configuration.setProperty(TOTAL_ITERATIONS, this.totalIterations);
        configuration.setProperty(PROPERTY, this.property);
        if (null != this.edgeTraversal)
            this.edgeTraversal.storeState(configuration, EDGE_TRAVERSAL);
        if (null != this.initialRankTraversal)
            this.initialRankTraversal.storeState(configuration, INITIAL_RANK_TRAVERSAL);
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
    public  Optional<MessageCombiner<Double>> getMessageCombiner() {
        return (Optional) PageRankMessageCombiner.instance();
    }

    @Override
    public  Set<MessageScope> getMessageScopes(final Memory memory) {
        final Set<MessageScope> set = new HashSet<>();
        set.add(memory.isInitialIteration() ? this.countMessageScope : this.incidentMessageScope);
        return set;
    }

    @Override
    public  PG clone() {
        try {
            final PG clone = (PG) super.clone();
            if (null != this.initialRankTraversal)
                clone.initialRankTraversal = this.initialRankTraversal.clone();
            return clone;
        } catch (final CloneNotSupportedException e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    @Override
    public  void setup(final Memory memory) {
	System.out.println("============== start at " + System.currentTimeMillis() + " ==================");

    }

    @Override
    public  void execute(final Vertex vertex, Messenger<Double> messenger, final Memory memory) {
	//double maxDiff = -1.0;
        if (memory.isInitialIteration()) {
            messenger.sendMessage(this.countMessageScope, 1.0d);
        } else if (1 == memory.getIteration()) {
            double initialPageRank = (null == this.initialRankTraversal ?
                    1.0d :
                    TraversalUtil.apply(vertex, this.initialRankTraversal.get()).doubleValue()) / this.vertexCountAsDouble;
            double edgeCount = IteratorUtils.reduce(messenger.receiveMessages(), 0.0d, (a, b) -> a + b);
            vertex.property(VertexProperty.Cardinality.single, this.property, initialPageRank);
            vertex.property(VertexProperty.Cardinality.single, EDGE_COUNT, edgeCount);
            if (!this.terminateInternal(memory)) // don't send messages if this is the last iteration
                messenger.sendMessage(this.incidentMessageScope, initialPageRank / edgeCount);
        } else {
            double newPageRank = IteratorUtils.reduce(messenger.receiveMessages(), 0.0d, (a, b) -> a + b);
            newPageRank = (this.alpha * newPageRank) + ((1.0d - this.alpha) / this.vertexCountAsDouble);
            vertex.property(VertexProperty.Cardinality.single, this.property, newPageRank);
            if (!this.terminateInternal(memory)) // don't send messages if this is the last iteration
                messenger.sendMessage(this.incidentMessageScope, newPageRank / vertex.<Double>value(EDGE_COUNT));
        }
    }

    public  boolean terminateInternal(final Memory memory) {
        return memory.getIteration() >= this.totalIterations;
    }

    @Override
    public  boolean terminate(final Memory memory) {
	System.out.println("complete iteration: " + memory.getIteration() + " at " + System.currentTimeMillis());
        return memory.getIteration() >= this.totalIterations;
    }

    @Override
    public  String toString() {
        return StringFactory.vertexProgramString(this, "alpha=" + this.alpha + ", iterations=" + this.totalIterations);
    }

    //////////////////////////////

    public static  Builder build() {
        return new Builder();
    }

    public final static class Builder extends AbstractVertexProgramBuilder<Builder> {

        private Builder() {
            super(PG.class);
        }

        public  Builder iterations(final int iterations) {
            this.configuration.setProperty(TOTAL_ITERATIONS, iterations);
            return this;
        }

        public  Builder alpha(final double alpha) {
            this.configuration.setProperty(ALPHA, alpha);
            return this;
        }

        public  Builder property(final String key) {
            this.configuration.setProperty(PROPERTY, key);
            return this;
        }

        public  Builder edges(final Traversal.Admin<Vertex, Edge> edgeTraversal) {
            PureTraversal.storeState(this.configuration, EDGE_TRAVERSAL, edgeTraversal);
            return this;
        }

        public  Builder initialRank(final Traversal.Admin<Vertex, ? extends Number> initialRankTraversal) {
            PureTraversal.storeState(this.configuration, INITIAL_RANK_TRAVERSAL, initialRankTraversal);
            return this;
        }

        /**
         * @deprecated As of release 3.2.0, replaced by {@link org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram.Builder#initialRank(Traversal.Admin)}
         */
        @Deprecated
        public  Builder vertexCount(final long vertexCount) {
            this.configuration.setProperty(VERTEX_COUNT, (double) vertexCount);
            return this;
        }

        /**
         * @deprecated As of release 3.2.0, replaced by {@link org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram.Builder#edges(Traversal.Admin)}
         */
        @Deprecated
        public  Builder traversal(final TraversalSource traversalSource, final String scriptEngine, final String traversalScript, final Object... bindings) {
            return this.edges(new ScriptTraversal<>(traversalSource, scriptEngine, traversalScript, bindings));
        }

        /**
         * @deprecated As of release 3.2.0, replaced by {@link org.apache.tinkerpop.gremlin.process.computer.ranking.pagerank.PageRankVertexProgram.Builder#edges(Traversal.Admin)}
         */
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
