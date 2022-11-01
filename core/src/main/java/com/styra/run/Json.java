package com.styra.run;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The Json service serializes and deserializes objects to and from JSON.
 */
public interface Json {
    /**
     * Serializes the given <code>value</code> to a JSON String.
     *
     * @param value the object to serialize
     * @return the serialized JSON String
     * @throws IOException on serialization error
     */
    String from(Object value) throws IOException;

    /**
     * Deserializes the given <code>source</code> JSON String into a Map.
     *
     * @param source the JSON String to deserialize into a Map
     * @return the deserialized Map
     * @throws IOException on deserialization error
     */
    Map<String, ?> toMap(String source) throws IOException;

    default Optional<Map<String, ?>> toOptionalMap(String source) {
        if (source == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(toMap(source));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Deserializes the given <code>source</code> JSON String into a typed List.
     *
     * @param type the type of each list entry
     * @param source the JSON String to deserialize into a List
     * @return the deserialized List
     * @throws IOException on deserialization error
     */
    <T> List<T> toList(Class<T> type, String source) throws IOException;

    default <T> Optional<List<T>> toOptionalList(Class<T> type, String source) {
        if (source == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(toList(type, source));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    /**
     * Deserializes the given <code>source</code> JSON String into an Object of the given <code>type</code>.
     *
     * @param type the type of the Object to deserialize
     * @param source the JSON String to deserialize
     * @return the deserialized Object
     * @throws IOException on deserialization error
     */
    <T> T to(Class<T> type, String source) throws IOException;

    default <T> Optional<T> toOptional(Class<T> type, String source) throws IOException {
        if (source == null) {
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(to(type, source));
        } catch (IOException e) {
            return Optional.empty();
        }
    }
}
