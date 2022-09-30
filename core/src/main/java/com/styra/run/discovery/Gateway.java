package com.styra.run.discovery;

import java.net.URI;

public class Gateway {
    private final URI uri;

    public Gateway(URI uri) {
        this.uri = uri;
    }

    public URI getUri() {
        return uri;
    }
}
