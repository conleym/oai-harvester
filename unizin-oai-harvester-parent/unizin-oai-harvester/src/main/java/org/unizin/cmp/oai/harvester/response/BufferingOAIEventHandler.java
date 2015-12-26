package org.unizin.cmp.oai.harvester.response;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.harvester.Functions;


public class BufferingOAIEventHandler implements OAIEventHandler {
    public static enum BufferAction {
        /** Discard the buffer contents. Do not the current event. */
        DISCARD,
        /**
         * Flush the buffer to the consumer, clearing it. Do not add the current
         * event.
         */
        FLUSH,
        /** Add the current event to the buffer. */
        ADD,
        /**
         * Add the current event to the buffer, then flush the buffer to the
         * consumer.
         */
        ADD_THEN_FLUSH,
        /**
         * Flush the current buffer to the consumer, then add the current event
         * to the buffer.
         */
        FLUSH_THEN_ADD,
        /**
         * Discard the buffer contents, then add the current event to the
         * buffer.
         */
        DISCARD_THEN_ADD
    }


    public static Consumer<List<XMLEvent>> consumerFromEventHandler(
            final OAIEventHandler handler) {
        return events -> events.forEach(Functions.wrap(
                (x) -> handler.onEvent(x)));
    }

    private final List<XMLEvent> eventBuffer = new ArrayList<>();
    private final Function<XMLEvent, BufferAction> actionFunction;
    private final Consumer<List<XMLEvent>> bufferConsumer;

    public BufferingOAIEventHandler(
            final Function<XMLEvent, BufferAction> actionFunction,
            final Consumer<List<XMLEvent>> bufferConsumer) {
        this.actionFunction = actionFunction;
        this.bufferConsumer = bufferConsumer;
    }

    private void discard() {
        eventBuffer.clear();
    }

    private void flush() {
        bufferConsumer.accept(new ArrayList<>(eventBuffer));
        discard();
    }

    @Override
    public void onEvent(final XMLEvent e) {
        final BufferAction action = actionFunction.apply(e);
        if (action == BufferAction.DISCARD ||
                action == BufferAction.DISCARD_THEN_ADD) {
            discard();
        } else if (action == BufferAction.FLUSH ||
                action == BufferAction.FLUSH_THEN_ADD) {
            flush();
        }
        if (action == BufferAction.ADD ||
                action == BufferAction.FLUSH_THEN_ADD ||
                action == BufferAction.DISCARD_THEN_ADD) {
            eventBuffer.add(e);
        } else if (action == BufferAction.ADD_THEN_FLUSH) {
            eventBuffer.add(e);
            flush();
        }
    }
}
