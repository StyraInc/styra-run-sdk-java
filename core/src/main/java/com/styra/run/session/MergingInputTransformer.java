package com.styra.run.session;

import com.styra.run.Input;
import com.styra.run.MapInput;
import com.styra.run.utils.Types;

import java.util.List;
import java.util.Map;

/**
 * An {@link InputTransformer} where the {@link MapInput} retrieved from the <code>session</code> is merged
 * into the incoming {@link Input}.
 * <p>
 * If the incoming <code>input</code> is <code>null</code>, empty, or doesn't have a {@link Map} value,
 * then the overriding <code>session</code> input is used.
 *
 * @param <S>
 */
public class MergingInputTransformer<S extends MapInputSession> implements InputTransformer<S> {
    private final List<String> reservedAttributes;

    public MergingInputTransformer(List<String> reservedAttributes) {
        this.reservedAttributes = reservedAttributes;
    }

    @Override
    public Input<?> transform(Input<?> input, String path, S session) {
        MapInput.Builder<String, Object> inputBuilder = new MapInput.Builder<>();

        if (input != null && !input.isEmpty()) {
            Map<?, ?> inputValue = Types.safeCast(Map.class, input.getValue());
            if (inputValue == null) {
                // The given input isn't a map, so we can't inject session info into it.
                // Use existing input, and leave it up to the policy to reject or decline.
                return input;
            }
            // Don't let client-side override or define session attributes
            inputValue.entrySet().stream()
                    .filter(e -> !reservedAttributes.contains(e.getKey().toString()))
                    .forEach(e -> inputBuilder.put(e.getKey().toString(), e.getValue()));
        }

        if (session != null) {
            session.toInput().getValue().forEach(inputBuilder::put);
        }

        return inputBuilder.build();
    }
}
