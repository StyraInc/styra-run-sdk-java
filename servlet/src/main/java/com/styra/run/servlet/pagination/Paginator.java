package com.styra.run.servlet.pagination;

import com.styra.run.session.Session;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A Paginator provides a means to paginate data returned by a server.
 * <p>
 * A specific page of data can be requested by the client via the <code>page</code> query parameter.
 * A page of data is returned to the client encapsulated in a JSON object; where the <code>result</code>
 * attribute contains the data, and the optional <code>page</code> attribute contains information about the
 * returned page.
 * <p>
 * The <code>page</code> query parameter and the returned <code>page</code> JSON attribute are both opaque data to this SDK,
 * and is agreed upon by the implementor of this interface and the requesting HTTP client.
 * <p>
 * Page size and sorting of returned data are implementation details of the implementor of this interface.
 *
 * <pre>
 * GET /user_bindings?page=3
 * ->
 * 200 OK
 * {
 *    "result": [
 *       {
 *          "id": "alice",
 *          "roles": [
 *             "ADMIN"
 *          ]
 *       },
 *       {
 *          "id": "billy",
 *          "roles": [
 *             "VIEWER"
 *          ]
 *       },
 *    ]
 *    "page": 3
 * }
 * </pre>
 *
 * @param <T> the type of the paged data
 * @param <S> the {@link Session} type
 */
public interface Paginator<T, S extends Session> {
    /**
     * @param page    the value of the <code>page</code> HTTP URL query parameter. May be <code>null</code>.
     * @param session the active {@link Session session} descriptor
     * @return a {@link PagedData} object containing the data and a description of
     */
    PagedData<T> get(String page, S session);

    /**
     * Container of the data of a returned page, as well as information about that page.
     *
     * @param <T> the type of the paginated data
     */
    class PagedData<T> {
        private final List<T> data;
        private final Page page;

        public PagedData(List<T> data, Page page) {
            this.data = unmodifiableList(requireNonNull(data, "result must not be null"));
            this.page = page;
        }

        public List<T> getData() {
            return data;
        }

        public Page getPage() {
            return page;
        }
    }
}
