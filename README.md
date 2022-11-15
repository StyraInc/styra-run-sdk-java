# The Styra Run Java SDK

## How to Install

### Gradle

```gradle
implementation 'com.styra.run:styra-run-sdk:0.1.0'
```

### Apache Maven

```maven
<dependency>
  <groupId>com.styra.run</groupId>
  <artifactId>styra-run-sdk</artifactId>
  <version>0.1.0</version>
</dependency>
```

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

Functionality for proxying queries is provided by the `Proxy` class in the `styra-run-sdk-core` library, and the `ProxyServlet` in the `styra-run-sdk-servlet` library.
These facilities provide extra security in the form of input- and output sanitization. Therefore, their use is recommended if policy queries and RBAC management is desirable in a client-/front-end component, such as a browser app.

#### Using the proxy servlet

The `ProxyServlet` is a Jakarta Servlet that makes use of the `Proxy` helper.

```gradle
implementation 'com.styra.run:styra-run-sdk-servlet:0.1.0'
```

Direct instantiation:

```java
var root = new ServletContextHandler();
var proxyHolder = new ServletHolder();
var sessionManager = new CookieSessionManager("user");
var inputTransformer = new TenantInputTransformer();
proxyHolder.setServlet(ProxyServlet.from(styraRun, sessionManager, inputTransformer));
root.addServlet(proxyHolder, "/api/authz");
```

Indirect instantiation:

```java
var root = new ServletContextHandler();
root.setAttribute(StyraRunServlet.STYRA_RUN_ATTR, styraRun);
root.setAttribute(StyraRunServlet.SESSION_MANAGER_ATTR, sessionManager);
root.setAttribute(StyraRunServlet.INPUT_TRANSFORMER_ATTR, inputTransformer);
root.addServlet(ProxyServlet.class, "/api/authz");
```

#### Using the Proxy helper directly

In cases where the provided Servlet implementation doesn't fit your needs, the `Proxy` helper can be used directly.

```java
var proxy = Proxy.builder(styraRun).build();
var session = new TenantSession("alice", "acmecorp");
var query = new BatchQuery()
        .withQuery("/my/policy/rule", MapInput.of("foo", "bar"))
        .withQuery("/my/other/policy/rule");
var result = proxy.proxy(query, session).join();
```

### RBAC Management

Functionality for RBAC management is provided by the `RbacManager` class in the `styra-run-sdk-core` library, and the `RbacServlet` in the `styra-run-sdk-servlet` library.
The RBAC-manager, and -servlet, provides functionality for getting, upserting, and deleting user bindings.

#### Using the RBAC Servlet

```gradle
implementation 'com.styra.run:styra-run-sdk-servlet:0.1.0'
```

Direct instantiation:

```java
var root = new ServletContextHandler();
var rbacHolder = new ServletHolder();
var sessionManager = new CookieSessionManager("user");
var inputTransformer = new TenantInputTransformer();
rbacHolder.setServlet(RbacServlet.from(styraRun, sessionManager, inputTransformer));
root.addServlet(rbacHolder, "/api/rbac/*")
```

Indirect instantiation:

```java
var root = new ServletContextHandler();
root.setAttribute(StyraRunServlet.STYRA_RUN_ATTR, styraRun);
root.setAttribute(StyraRunServlet.SESSION_MANAGER_ATTR, sessionManager);
root.setAttribute(StyraRunServlet.INPUT_TRANSFORMER_ATTR, inputTransformer);
root.addServlet(RbacServlet.class, "/api/rbac/*");
```

#### Using the RBAC Manager directly

In cases where the provided Servlet implementation doesn't fit your needs, the `RbacManager` can be used directly.

```java
var rbac = new RbacManager(styraRun);
var session = new TenantSession("alice", "acmecorp");
var user = new User("bob");
var userBinding = rbac.getUserBinding(user, session).join();
```

## Project Structure

### Core

The core functionality of this SDK. Requires Java 8.

### Sdk

Additional functionality for this SDK. Requires Java 11.

This auxiliary library adds an async `ApiClient` implementation; used by default if no other `ApiClient` is injected via
the builder.

### Servlet

This library adds support for exposing [Proxy](#proxy) and [RBAC management](#rbac-management) endpoints via Jetty Servlets.

## JSON

The Styra Run Java SDK has native JSON support through [jackson-core](https://github.com/FasterXML/jackson-core).
Should custom JSON serialization/deserialization be required, an instance of a custom implementation of the 
`com.styra.run.Json` interface can be provided via the builder.

## Java 10 and Older

The Styra Run SDK for Java 11, and newer, facilitates asynchronous communication with the Styra Run API.
This implementation isn't available in Java 10, and older; instead, the SDK will fall back to a blocking
client for Java 8 and up to, but not including, Java 11.

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
