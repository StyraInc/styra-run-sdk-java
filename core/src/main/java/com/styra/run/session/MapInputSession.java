package com.styra.run.session;

import com.styra.run.MapInput;

public interface MapInputSession extends Session {
    MapInput<String, ?> toInput();
}
