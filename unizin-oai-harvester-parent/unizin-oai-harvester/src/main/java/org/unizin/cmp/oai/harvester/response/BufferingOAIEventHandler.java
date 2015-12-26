package org.unizin.cmp.oai.harvester.response;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.stream.events.XMLEvent;

import org.unizin.cmp.oai.harvester.Functions;


public class BufferingOAIEventHandler implements OAIEventHandler {
    public static enum BufferAction
    implements BiConsumer<BufferingOAIEventHandler, XMLEvent>{
        /** Discard the buffer contents. Do not add the current event. */
        DISCARD((h, e) -> h.discard()),
        /**
         * Flush the buffer to the consumer, clearing it. Do not add the current
         * event.
         */
        FLUSH((h, e) -> h.flush()),
        /** Add the current event to the buffer. */
        ADD((h, e) -> h.add(e)),
        /**
         * Add the current event to the buffer, then flush the buffer to the
         * consumer.
         */
        ADD_THEN_FLUSH((h, e) -> { h.add(e); h.flush(); }),
        /**
         * Flush the current buffer to the consumer, then add the current event
         * to the buffer.
         */
        FLUSH_THEN_ADD((h, e) -> { h.flush(); h.add(e); } ),
        /**
         * Discard the buffer contents, then add the current event to the
         * buffer.
         */
        DISCARD_THEN_ADD((h, e) -> { h.discard(); h.add(e); });

        private final BiConsumer<BufferingOAIEventHandler, XMLEvent> fn;
        private BufferAction(
                BiConsumer<BufferingOAIEventHandler, XMLEvent> fn) {
            this.fn = fn;
        }

        @Override
        public void accept(final BufferingOAIEventHandler t, final XMLEvent u) {
            this.fn.accept(t,  u);
        }
    }


    public static Consumer<List<XMLEvent>> consumerFromEventHandler(
            final OAIEventHandler handler) {
        final Consumer<XMLEvent> eventConsumer = Functions.wrap(
                e -> handler.onEvent(e));
        return events -> events.forEach(eventConsumer);
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

    private void add(final XMLEvent event) {
        eventBuffer.add(event);
    }

    @Override
    public void onEvent(final XMLEvent e) {
        final BufferAction action = actionFunction.apply(e);
        if (action != null) {
            action.accept(this, e);
        }
    }
}
