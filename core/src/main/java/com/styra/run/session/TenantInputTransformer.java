package com.styra.run.session;

import java.util.List;

import static com.styra.run.session.TenantSession.SUBJECT_KEY;
import static com.styra.run.session.TenantSession.TENANT_KEY;
import static java.util.Arrays.asList;

public class TenantInputTransformer extends MergingInputTransformer<TenantSession> {
    private static final List<String> RESERVED_SESSION_ATTRIBUTES = asList(TENANT_KEY, SUBJECT_KEY);

    public TenantInputTransformer() {
        super(RESERVED_SESSION_ATTRIBUTES);
    }
}
