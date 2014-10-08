package com.tinkerpop.gremlin.process.graph.step.util;

import com.tinkerpop.gremlin.process.Traversal;
import com.tinkerpop.gremlin.process.Traverser;
import com.tinkerpop.gremlin.process.graph.marker.Barrier;
import com.tinkerpop.gremlin.process.util.AbstractStep;
import com.tinkerpop.gremlin.process.util.FastNoSuchElementException;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Consumer;

/**
 * @author Marko A. Rodriguez (http://markorodriguez.com)
 */
public abstract class BarrierStep<S> extends AbstractStep<S, S> implements Barrier {
    private final Queue<Traverser.Admin<S>> previousTraversers = new LinkedList<>();
    private Consumer<List<Traverser<S>>> barrierConsumer;

    public BarrierStep(final Traversal traversal) {
        super(traversal);
    }

    public void setConsumer(final Consumer<List<Traverser<S>>> barrierConsumer) {
        this.barrierConsumer = barrierConsumer;
    }

    @Override
    public Traverser<S> processNextStart() {
        while (true) {
            if (this.starts.hasNext()) {
                this.starts.forEachRemaining(start -> {
                    final Optional<Traverser.Admin<S>> optional = this.previousTraversers.stream().filter(previous -> previous.equals(start)).findAny();
                    if (optional.isPresent()) {
                        final Traverser.Admin<S> previous = optional.get();
                        previous.setBulk(previous.getBulk() + start.getBulk());
                    } else {
                        this.previousTraversers.add(start);
                    }
                });
                this.barrierConsumer.accept((List) this.previousTraversers);
            } else {
                if (this.previousTraversers.isEmpty())
                    throw FastNoSuchElementException.instance();
                return this.previousTraversers.remove().makeSibling();
            }
        }
    }

    @Override
    public void reset() {
        super.reset();
        this.previousTraversers.clear();
    }
}