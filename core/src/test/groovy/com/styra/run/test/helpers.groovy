package com.styra.run.test

import com.styra.run.discovery.Gateway

static Gateway gatewayFrom(String uri) {
    return new Gateway(URI.create(uri))
}

static List<Gateway> gatewaysFrom(int count, String baseUri = 'https://localhost:1234/') {
    return (1..count).collect { gatewayFrom(baseUri + it) }
}
