package com.styra.run.utils;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class Objects {
    public static <T, E extends Exception> T require(T value,
                                                     Predicate<T> predicate,
                                                     Supplier<E> exceptionSupplier) throws E {
        if (predicate.test(value)) {
            return value;
        }
        throw exceptionSupplier.get();
    }
}
