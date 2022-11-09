package com.styra.run;

import com.styra.run.utils.Futures;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Even though this client returns a {@link CompletableFuture}, requests will block the calling thread.
 */
public class BlockingApiClient implements ApiClient {
    private static final String READ_TIMEOUT_MSG = "Read timed out";

    private final Config config;

    public BlockingApiClient(Config config) {
        this.config = config;
    }

    @Override
    public CompletableFuture<ApiResponse> request(Method method, URI uri, Map<String, String> headers, String body) {
        try {
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            connection.setConnectTimeout((int) config.getConnectionTimeout().toMillis());
            connection.setReadTimeout((int) config.getRequestTimeout().toMillis());
            connection.setRequestMethod(method.name());
            connection.setRequestProperty("User-Agent", config.getUserAgent());
            headers.forEach(connection::setRequestProperty);

            if (connection instanceof HttpsURLConnection) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(config.getSslContext().getSocketFactory());
            }

            if (body != null && method.allowsBody()) {
                connection.setDoOutput(true);
                OutputStream out = connection.getOutputStream();
                out.write(body.getBytes(StandardCharsets.UTF_8));
                out.flush();
                out.close();
            }

            int statusCode = connection.getResponseCode();
            InputStream in;
            if (statusCode >= 400) {
                in = connection.getErrorStream();
            } else {
                in = connection.getInputStream();
            }

            String responseBody = null;
            if (in != null) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                in.close();
                responseBody = sb.toString();
            }

            return CompletableFuture.completedFuture(new ApiResponse(statusCode, responseBody));
        } catch (Exception e) {
            if (e instanceof ConnectException ||
                    (e instanceof SocketTimeoutException && READ_TIMEOUT_MSG.equals(e.getMessage()))) {
                return Futures.failedFuture(new RetryException(e));
            }
            return Futures.failedFuture(e);
        }
    }

    @Override
    public void close() {
    }
}
