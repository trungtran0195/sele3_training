package com.extension;

import java.util.Set;

public interface NamedProvider {
    String id();

    default Set<String> aliases() {
        return Set.of();
    }
}
