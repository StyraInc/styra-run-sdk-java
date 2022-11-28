package com.styra.run.servlet.rbac;

import com.styra.run.rbac.User;
import com.styra.run.servlet.pagination.Paginator;
import com.styra.run.session.TenantSession;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;

public interface UserProvider extends Paginator<User, TenantSession> {
    static UserProvider from(Map<String, List<User>> usersByTenant) {
        return new UnpagedUserProvider(usersByTenant);
    }

    static UserProvider from(Map<String, List<User>> usersByTenant, int pageSize) {
        UserSupplier userSupplier = (offset, limit, session) -> {
            List<User> users = usersByTenant.getOrDefault(session.getTenant(), emptyList());
            if (offset < 0 || offset >= users.size()) {
                return emptyList();
            }
            return users.subList(offset, Math.min(offset + limit, users.size()));
        };

        UserCountSupplier userCountSupplier = session -> {
            List<User> users = usersByTenant.getOrDefault(session.getTenant(), emptyList());
            return users.size();
        };

        return from(userSupplier, userCountSupplier, pageSize);
    }

    static UserProvider from(UserSupplier userSupplier, int pageSize) {
        return new IndexedUserProvider(pageSize) {
            @Override
            public List<User> get(int offset, int limit, TenantSession session) {
                return userSupplier.get(offset, limit, session);
            }
        };
    }

    static UserProvider from(UserSupplier userSupplier,
                             UserCountSupplier totalUserCountSupplier,
                             int pageSize) {
        return new IndexedUserProvider(pageSize) {
            @Override
            public List<User> get(int offset, int limit, TenantSession session) {
                return userSupplier.get(offset, limit, session);
            }

            @Override
            public int getTotalCount(TenantSession session) {
                return totalUserCountSupplier.get(session);
            }
        };
    }

    interface UserSupplier {
        List<User> get(int offset, int limit, TenantSession session);
    }

    interface UserCountSupplier {
        int get(TenantSession session);
    }
}
