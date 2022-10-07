package com.styra.run;

/**
 * An <code>Input</code> encapsulates the <code>input</code> consumed by a Styra Run <code>policy</code>.
 *
 * @param <T> the type of the <code>policy</code> <code>input</code> value
 */
public class Input<T> {
    private static final Input<Void> EMPTY = new Input<>(null);
    private final T value;

    /**
     * Instantiates an <code>Input</code> object.
     *
     * @param value the <code>input</code> value
     */
    public Input(T value) {
        this.value = value;
    }

    /**
     * Creates an empty <code>Input</code>, with no assigned value.
     *
     * @return an empty input
     */
    public static Input<?> empty() {
        return EMPTY;
    }

    /**
     * Returns the value of this <code>Input</code>, or <code>null</code>.
     *
     * @return this <code>Input</code>'s value
     */
    public T getValue() {
        return value;
    }

    /**
     * Returns <code>true</code>, if the value of this <code>Input</code> is <code>null</code>; <code>false</code> otherwise.
     *
     * @return <code>true</code>, if the value is <code>null</code>; <code>false</code> otherwise.
     */
    public boolean isEmpty() {
        return value == null;
    }

    @Override
    public String toString() {
        return "Input{" +
                "value=" + value +
                '}';
    }
}
