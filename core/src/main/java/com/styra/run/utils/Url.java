package com.styra.run.utils;

import com.styra.run.StyraRunException;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Url {
    public static String joinPath(String root, String... path) {
        return Stream.concat(
                        Stream.of(root.split("/")),
                        Stream.of(path)
                                .filter(Objects::nonNull)
                                .flatMap((elem) -> Stream.of(elem.split("/"))))
                .map(String::trim)
                .filter((elem) -> !elem.isEmpty())
                .collect(Collectors.joining("/", "/", ""));
    }

    public static URI appendPath(URI base, String... path) throws StyraRunException {
        String joinedPath = joinPath(base.getPath(), path);
        try {
            return new URI(base.getScheme(), base.getUserInfo(), base.getHost(), base.getPort(), joinedPath,
                    base.getQuery(), base.getFragment());
        } catch (URISyntaxException e) {
            throw new StyraRunException(String.format("Failed to construct URI from base '%s' and path '%s'",
                    base, Arrays.toString(path)), e);
        }
    }
}
