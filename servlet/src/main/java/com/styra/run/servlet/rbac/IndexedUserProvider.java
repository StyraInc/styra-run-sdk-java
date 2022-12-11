package com.styra.run.servlet.rbac;

import com.styra.run.rbac.User;
import com.styra.run.servlet.pagination.Page;
import com.styra.run.session.TenantSession;
import com.styra.run.utils.Null;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A paginated, indexed {@link UserProvider}, that enumerates ordered user ID:s based on an offset and a limit.
 * Where <code>offset</code> is the index of the first user of the requested page, and <code>limit</code> is the maximum
 * number of users to enumerate for the page.
 */
public abstract class IndexedUserProvider implements UserProvider {
    private final int pageSize;

    protected IndexedUserProvider() {
        this.pageSize = 20;
    }

    protected IndexedUserProvider(int pageSize) {
        this.pageSize = pageSize;
    }

    // TODO: Must take Session to determine tenant
    public abstract List<User> get(int offset, int limit, TenantSession session);

    /**
     * Override to return the total user count, if known. Returning -1 will inform the pagination
     * there is no known upper page limit.
     *
     * @return the total user count. <code>-1</code>, by default.
     */
    // TODO: Must take Session to determine tenant
    public int getTotalCount(TenantSession session) {
        return -1;
    }

    @Override
    public PagedData<User> get(String page, TenantSession session) {
        int pageNumber = 1;
        if (page != null && !page.isEmpty()) {
            try {
                pageNumber = Integer.parseInt(page);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("page is not a valid integer", e);
            }
        }

        int totalUserCount = getTotalCount(session);
        int offset = Math.max(pageNumber - 1, 0) * pageSize;

        Integer totalPageCount = null;
        if (totalUserCount >= 0) {
            totalPageCount = (int) Math.ceil((float) totalUserCount / pageSize);
        }

        List<User> users;
        if (totalUserCount >= 0 && offset >= totalUserCount) {
            users = Collections.emptyList();
        } else {
            users = get(offset, pageSize, session);
        }

        return new PagedData<>(users, new IndexedPage(pageNumber, totalPageCount));
    }

    public static class IndexedPage implements Page {
        private final int index;
        private final Integer totalPages;

        public IndexedPage(int index, Integer totalPages) {
            this.index = index;
            this.totalPages = totalPages;
        }

        @Override
        public Object serialize() {
            Map<String, Integer> map = new HashMap<>();
            map.put("index", index);
            Null.ifNotNull(totalPages, v -> map.put("total", v));
            return map;
        }
    }
}
