package com.styra.run.servlet.rbac;

import com.styra.run.StyraRun;
import com.styra.run.rbac.RbacManager;
import com.styra.run.rbac.User;
import com.styra.run.servlet.StyraRunServlet;
import com.styra.run.servlet.pagination.Paginator.PagedData;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.TenantSession;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

import static com.styra.run.utils.Types.cast;

/**
 * A servlet for enumerating <code>user-bindings</code> in a <code>tenant</code>, as defined by a Styra Run <code>project</code>.
 * <p>
 * E.g.
 * <p>
 * Getting the user-binding for <code>alice</code>:
 * <pre>
 * GET /roles
 * ->
 * 200 OK
 * {
 *    "result": [
 *       "ADMIN",
 *       "VIEWER",
 *       "EDITOR"
 *    ]
 * }
 * </pre>
 *
 * <p>
 * If a {@link UserProvider} is provided on construction or as an {@link #USER_PAGINATOR_ATTR attribute} servlet context,
 * user-bindings for only those {@link User users} enumerated by the provider will be fetched from the Styra Run <code>project environment</code>.
 * The UserProvider defines pagination, if any.
 * <p>
 * If no UserProvider is provided, all user-bindings in the Styra Run <code>project environment</code> will be fetched.
 *
 * @see StyraRunServlet
 */
public class RbacUserBindingsListServlet extends AbstractRbacServlet {
    public static final String USER_PAGINATOR_ATTR = "com.styra.run.user-paginator";

    private volatile UserProvider userProvider = null;

    public RbacUserBindingsListServlet() {
        super();
    }

    public RbacUserBindingsListServlet(StyraRun styraRun,
                                       SessionManager<TenantSession> sessionManager,
                                       UserProvider userProvider) {
        super(styraRun, sessionManager);
        this.userProvider = userProvider;
    }

    private UserProvider getUserProvider() throws ServletException {
        if (userProvider == null) {
            userProvider = cast(UserProvider.class, getServletConfig().getServletContext().getAttribute(USER_PAGINATOR_ATTR),
                    () -> new ServletException(String.format("'%s' attribute on servlet context was not UserPaginator type", USER_PAGINATOR_ATTR)));
        }
        return userProvider;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        UserProvider userProvider = getUserProvider();

        handleAsync(request, response, (body, out, async) -> {
            TenantSession session = getSession(request);
            RbacManager rbac = getRbacManager();

            if (userProvider != null) {
                PagedData<User> pagedUsers = userProvider.get(request.getParameter("page"), session);

                rbac.getUserBindings(pagedUsers.getData(), session)
                        .thenAccept((bindings) -> writeResult(bindings, pagedUsers.getPage(), response, out, async))
                        .exceptionally((e) -> {
                            handleError("Failed to GET user bindings for", e, async, response);
                            return null;
                        });
            } else {
                rbac.listUserBindings(session)
                        .thenAccept((bindings) -> writeResult(bindings, response, out, async))
                        .exceptionally((e) -> {
                            handleError("Failed to GET user bindings for", e, async, response);
                            return null;
                        });
            }
        });
    }
}
