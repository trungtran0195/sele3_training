package com.extension;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ServiceLoader;

public final class ProviderRegistry<T extends NamedProvider> {

    private final Class<T> providerType;
    private final Map<String, T> providers;

    public ProviderRegistry(Class<T> providerType, ClassLoader classLoader) {
        this.providerType = providerType;
        this.providers = discover(classLoader);
    }

    public T require(String id) {
        String normalized = normalize(id);
        T provider = providers.get(normalized);
        if (provider == null) {
            throw new IllegalArgumentException(
                    "No " + providerType.getSimpleName() + " registered for '" + normalized
                            + "'. Available: " + providers.keySet());
        }
        return provider;
    }

    public Map<String, T> providers() {
        return providers;
    }

    private Map<String, T> discover(ClassLoader classLoader) {
        Map<String, T> discovered = new LinkedHashMap<>();
        ServiceLoader.load(providerType, classLoader).forEach(provider -> {
            register(discovered, provider.id(), provider);
            provider.aliases().forEach(alias -> register(discovered, alias, provider));
        });
        return Map.copyOf(discovered);
    }

    private void register(Map<String, T> discovered, String id, T provider) {
        String normalized = normalize(id);
        T previous = discovered.putIfAbsent(normalized, provider);
        if (previous != null && previous != provider) {
            throw new IllegalStateException(
                    "Duplicate " + providerType.getSimpleName() + " id: " + normalized);
        }
    }

    private String normalize(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException(providerType.getSimpleName() + " id must not be blank");
        }
        return id.trim().toLowerCase(Locale.ROOT);
    }
}
