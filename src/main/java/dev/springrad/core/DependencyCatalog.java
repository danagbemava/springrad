package dev.springrad.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * In-memory catalog of Initializr dependencies with lookup and search.
 */
public final class DependencyCatalog {

    private final List<DependencyEntry> entries;
    private final Map<String, DependencyEntry> byId;

    public DependencyCatalog(List<DependencyEntry> entries) {
        this.entries = List.copyOf(entries);
        Map<String, DependencyEntry> map = new LinkedHashMap<>();
        for (DependencyEntry entry : entries) {
            map.put(entry.id().toLowerCase(Locale.ROOT), entry);
        }
        this.byId = Collections.unmodifiableMap(map);
    }

    public List<DependencyEntry> all() {
        return entries;
    }

    public DependencyEntry findById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return byId.get(id.toLowerCase(Locale.ROOT));
    }

    public boolean isValid(String id) {
        return findById(id) != null;
    }

    /**
     * Search by prefix across id and name. Returns up to {@code limit} matches.
     */
    public List<DependencyEntry> search(String query, int limit) {
        if (query == null || query.isBlank()) {
            return entries.subList(0, Math.min(limit, entries.size()));
        }
        String q = query.toLowerCase(Locale.ROOT);
        List<DependencyEntry> idMatches = new ArrayList<>();
        List<DependencyEntry> nameMatches = new ArrayList<>();
        for (DependencyEntry entry : entries) {
            if (entry.id().toLowerCase(Locale.ROOT).startsWith(q)) {
                idMatches.add(entry);
            } else if (entry.name().toLowerCase(Locale.ROOT).contains(q)
                    || entry.id().toLowerCase(Locale.ROOT).contains(q)) {
                nameMatches.add(entry);
            }
        }
        List<DependencyEntry> result = new ArrayList<>(idMatches);
        result.addAll(nameMatches);
        return result.subList(0, Math.min(limit, result.size()));
    }

    /**
     * Validates a comma-separated dependency string and returns invalid IDs.
     */
    public List<String> findInvalid(List<String> ids) {
        List<String> invalid = new ArrayList<>();
        for (String id : ids) {
            if (!id.isBlank() && !isValid(id)) {
                invalid.add(id);
            }
        }
        return invalid;
    }

    public int size() {
        return entries.size();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
