# The Styra Run Java SDK

## How to Install

TODO: Gradle dependency

TODO: Maven dependency

## How to Use

### Instantiating a Styra Run Client

```java
// Options are pulled from the environment
var styraRun = StyraRun.builder(System.getenv("STYRA_URL"), System.getenv("STYRA_TOKEN"))
        .build();
```

### Query

Query a policy rule for arbitrary data.

```java
var input = ...
var result = styraRun.query("my/policy/roles", input).join();
var roles = result.getListOf(String.class);
```

### Check

Query a policy rule for a boolean result.

```java
var input = ...
var allowed = styraRun.check("my/policy/allow", input).join();
if (allowed) {
    ...
}
```

### Data

Methods for getting, upserting, and deleting data are provided.

#### Get

Retrieve data at a given `path`.

```java
var data = styraRun.getData("foo/bar").join().get();
```

#### Put

Upsert (create or replace) data at a given `path`.

The `data` argument can be of any type and will be serialized to JSON by the [pluggable `Json`](#json) service. 

```java
var data = ...
styraRun.putData("foo/bar", data).join();
```

#### Delete

Deletes data at a given `path`.

```java
styraRun.deleteData("foo/bar").join();
```

### Proxy

TODO

### RBAC Management

TODO

## Project Structure

### Core

The core functionality of this SDK. Requires Java 8.

### Sdk

Additional functionality for this SDK. Requires Java 11.

This auxiliary library adds an async `ApiClient` implementation; used by default if no other `ApiClient` is injected via
the builder.

### Servlet

This library adds support for exposing [Proxy](#proxy) and [RBAC management](#rbac-management) endpoints via Jetty Servlets.

### JSON

The Styra Run Java SDK has native JSON support through [jackson-core](https://github.com/FasterXML/jackson-core).
Should custom JSON serialization/deserialization be required, an instance of a custom implementation of the 
`com.styra.run.Json` interface can be provided via the builder.

## Java 10 and Older

The Styra Run SDK for Java 11, and newer, makes use of `java.net.http.HttpClient` for communicating with the Styra Run API,
which isn't available in Java 10, and older. 

The core Styra Run SDK exposes the `ApiClient` interface, which can be implemented to provide a custom HTTP client
for connecting to the Styra Run API. Implementations can be injected either via the Styra Run builder, 
or by registering it as a Java Service Provider Interface (SPI).

### Example Implementation

`MyApiClient.java`:
```java
package com.example;

import com.styra.run.ApiClient;
import com.styra.run.ApiClient.Config;

public class DefaultApiClient implements ApiClient {
    public DefaultApiClient(Config config) {
        throw new IllegalStateException("Not implemented");
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        throw new IllegalStateException("Not implemented");
    }
}
```

`MyApiClientFactory.java`:
```java
package com.example;

import com.styra.run.ApiClient.Config;
import com.styra.run.spi.ApiClientFactory;

public class MyApiClientFactory implements ApiClientFactory {
    @Override
    public ApiClient create(Config config) {
        return new MyApiClient(config);
    }
}
```

### Builder Injection

```java
var styraRun = StyraRun.builder(System.getenv("STYRA_URL"), System.getenv("STYRA_TOKEN"))
            .apiClientFactory(new MyApiClientFactory())
            .build();
```

### SPI Injection

**Note**: The Styra Run SDK library will register a default `ApiClient` SPI implementation; when registering your own SPI 
implementation, only include the Core library on your class-path, not the SDK library.

`META-INF/services/com.styra.run.spi.ApiClientFactory`:
```
com.example.MyApiClientFactory
```

## Releasing

A release is created through the following steps:

1. Update library versions (release task will fail if versions doesn't match the pushed tag - ignoring `v` prefix)
   1. `core/build.gradle`
   2. `sdk/build.gradle`
2. Push a version tag in the format `vX.Y.Z`; e.g. `v0.1.4`. 
3. Once the `Release` workflow has successfully completed:
   1. Complete the GH draft release
   2. Promote the staged libraries at [OSSRH](https://s01.oss.sonatype.org) to Maven Central by `Closing` the staged repository.
