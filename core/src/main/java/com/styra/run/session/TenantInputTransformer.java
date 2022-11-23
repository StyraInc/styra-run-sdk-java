package com.styra.run.session;

import com.styra.run.Input;
import com.styra.run.MapInput;
import com.styra.run.utils.Types;

import java.util.Map;

public class TenantInputTransformer implements InputTransformer<TenantSession> {
    @Override
    public Input<?> transform(Input<?> input, String path, TenantSession session) {
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
                    .filter(e -> !session.getValue().containsKey(e.getKey().toString()))
                    .forEach(e -> inputBuilder.put(e.getKey().toString(), e.getValue()));
        }

        if (session != null) {
            inputBuilder.putAll(session.getValue());
        }

        return inputBuilder.build();
    }
}
