package com.styra.run.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

// TODO: Make streaming version?
public final class Collections {
    public static <T> List<List<T>> chunk(List<T> list, int chunkSize) {
        List<List<T>> chunks = new LinkedList<>();
        if (chunkSize == 0) {
            chunks.add(list);
        } else {
            final AtomicReference<List<T>> currentChunk = new AtomicReference<>(new LinkedList<>());
            list.forEach((item) -> {
                List<T> chunk = currentChunk.get();
                if (chunk.size() < chunkSize) {
                    chunk.add(item);
                } else {
                    chunks.add(chunk);
                    chunk = new LinkedList<>();
                    chunk.add(item);
                    currentChunk.set(chunk);

                }
            });
            if (!currentChunk.get().isEmpty()) {
                chunks.add(currentChunk.get());
            }
        }
        return chunks;
    }
}
