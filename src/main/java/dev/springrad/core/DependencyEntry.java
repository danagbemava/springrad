package dev.springrad.core;

/**
 * A single dependency available from the Spring Initializr catalog.
 *
 * @param id          Initializr dependency ID (e.g. "web", "data-jpa")
 * @param name        human-readable name (e.g. "Spring Web")
 * @param description short description of the dependency
 * @param group       category group (e.g. "Web", "SQL", "Developer Tools")
 */
public record DependencyEntry(String id, String name, String description, String group) {

    /**
     * Returns a compact display string suitable for autocomplete suggestions.
     */
    public String displayLabel() {
        return id + " — " + name;
    }
}
