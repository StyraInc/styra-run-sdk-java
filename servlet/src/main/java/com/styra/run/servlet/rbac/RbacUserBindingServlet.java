package com.styra.run.servlet.rbac;

import com.styra.run.ApiError;
import com.styra.run.StyraRun;
import com.styra.run.rbac.RbacManager;
import com.styra.run.rbac.Role;
import com.styra.run.rbac.User;
import com.styra.run.rbac.UserBinding;
import com.styra.run.servlet.BadRequestException;
import com.styra.run.servlet.session.SessionManager;
import com.styra.run.session.InputTransformer;
import com.styra.run.session.Session;
import com.styra.run.utils.Url;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class RbacUserBindingServlet extends AbstractRbacServlet {
    public RbacUserBindingServlet() {
        super();
    }

    private RbacUserBindingServlet(StyraRun styraRun,
                                   SessionManager<Session> sessionManager,
                                   InputTransformer<Session> inputTransformer) {
        super(styraRun, sessionManager, inputTransformer);
    }

    public static <S extends Session> RbacUserBindingServlet from(StyraRun styraRun,
                                                                  SessionManager<S> sessionManager,
                                                                  InputTransformer<S> inputTransformer) {
        //noinspection unchecked
        return new RbacUserBindingServlet(styraRun,
                (SessionManager<Session>) sessionManager,
                (InputTransformer<Session>) inputTransformer);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RbacManager rbac = getRbacManager();
        String path = getPath(request);

        handleAsync(request, response, (body, out, async) -> {
            String userId = getUserId(path);
            rbac.getUserBinding(new User(userId), getSession(request))
                    .thenAccept((binding) -> writeResult(binding.getRoles().stream()
                            .map(Role::getName)
                            .collect(Collectors.toList()), response, out, async))
                    .exceptionally((e) -> {
                        handleError(String.format("Failed to GET user binding for '%s'", userId), e, async, response);
                        return null;
                    });
        });

    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RbacManager rbac = getRbacManager();
        String path = getPath(request);

        handleAsync(request, response, (body, out, async) -> {
            String userId = getUserId(path);
            rbac.deleteUserBinding(new User(userId), getSession(request))
                    .thenAccept((Void) -> writeResult(null, response, out, async))
                    .exceptionally((e) -> {
                        handleError(String.format("Failed to PUT user binding for '%s'", userId), e, async, response);
                        return null;
                    });
        });
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        RbacManager rbac = getRbacManager();
        String path = getPath(request);

        handleAsync(request, response, (body, out, async) -> {
            String userId = getUserId(path);
            User user = new User(userId);
            List<Role> roles = getStyraRun().getJson().toList(String.class, body)
                    .stream()
                    .map(Role::new)
                    .collect(Collectors.toList());
            UserBinding userBinding = new UserBinding(user, roles);
            rbac.putUserBinding(userBinding, getSession(request))
                    .thenAccept((Void) -> writeResult(null, response, out, async))
                    .exceptionally((e) -> {
                        handleError(String.format("Failed to DELETE user binding for '%s'", userId), e, async, response);
                        return null;
                    });
        });
    }

    private String getUserId(String path)
            throws BadRequestException {
        List<String> parts = Url.splitPath(path);

        if (parts.size() == 1) {
            return parts.get(0);
        }

        throw new BadRequestException();
    }
}
