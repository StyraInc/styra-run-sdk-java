package com.styra.run.servlet.pagination;

import com.styra.run.session.Session;

import java.util.List;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

public interface Paginator<T, S extends Session> {
    PagedData<T> get(String page, S session);

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
