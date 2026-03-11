package dev.springrad.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DependencyCatalogTest {

    private static final List<DependencyEntry> SAMPLE_ENTRIES = List.of(
            new DependencyEntry("web", "Spring Web", "Build web applications", "Web"),
            new DependencyEntry("webflux", "Spring Reactive Web", "Build reactive web apps", "Web"),
            new DependencyEntry("data-jpa", "Spring Data JPA", "Persist data with JPA", "SQL"),
            new DependencyEntry("data-rest", "Spring Data REST", "Expose repos over REST", "SQL"),
            new DependencyEntry("security", "Spring Security", "Secure your app", "Security"),
            new DependencyEntry("actuator", "Spring Boot Actuator", "Monitor your app", "Ops")
    );

    private final DependencyCatalog catalog = new DependencyCatalog(SAMPLE_ENTRIES);

    @Test
    void findById_returnsEntry() {
        DependencyEntry entry = catalog.findById("web");
        assertNotNull(entry);
        assertEquals("Spring Web", entry.name());
    }

    @Test
    void findById_caseInsensitive() {
        assertNotNull(catalog.findById("WEB"));
        assertNotNull(catalog.findById("Data-JPA"));
    }

    @Test
    void findById_returnsNullForUnknown() {
        assertNull(catalog.findById("nonexistent"));
        assertNull(catalog.findById(null));
        assertNull(catalog.findById(""));
    }

    @Test
    void isValid_checksExistence() {
        assertTrue(catalog.isValid("web"));
        assertTrue(catalog.isValid("data-jpa"));
        assertFalse(catalog.isValid("unknown"));
    }

    @Test
    void search_byIdPrefix() {
        List<DependencyEntry> results = catalog.search("web", 10);
        assertEquals(2, results.size());
        assertEquals("web", results.get(0).id());
        assertEquals("webflux", results.get(1).id());
    }

    @Test
    void search_byNameContains() {
        List<DependencyEntry> results = catalog.search("reactive", 10);
        assertEquals(1, results.size());
        assertEquals("webflux", results.get(0).id());
    }

    @Test
    void search_byIdContains() {
        List<DependencyEntry> results = catalog.search("jpa", 10);
        assertEquals(1, results.size());
        assertEquals("data-jpa", results.get(0).id());
    }

    @Test
    void search_respectsLimit() {
        List<DependencyEntry> results = catalog.search("data", 1);
        assertEquals(1, results.size());
    }

    @Test
    void search_emptyQueryReturnsAll() {
        List<DependencyEntry> results = catalog.search("", 100);
        assertEquals(SAMPLE_ENTRIES.size(), results.size());
    }

    @Test
    void search_noMatchesReturnsEmpty() {
        List<DependencyEntry> results = catalog.search("zzzzz", 10);
        assertTrue(results.isEmpty());
    }

    @Test
    void findInvalid_returnsUnknownIds() {
        List<String> invalid = catalog.findInvalid(List.of("web", "unknown", "data-jpa", "fake"));
        assertEquals(List.of("unknown", "fake"), invalid);
    }

    @Test
    void findInvalid_allValidReturnsEmpty() {
        List<String> invalid = catalog.findInvalid(List.of("web", "security"));
        assertTrue(invalid.isEmpty());
    }

    @Test
    void findInvalid_emptyInputReturnsEmpty() {
        assertTrue(catalog.findInvalid(List.of()).isEmpty());
    }

    @Test
    void size_matchesEntries() {
        assertEquals(6, catalog.size());
        assertFalse(catalog.isEmpty());
    }

    @Test
    void emptyCatalog() {
        DependencyCatalog empty = new DependencyCatalog(List.of());
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertNull(empty.findById("web"));
        assertTrue(empty.search("web", 10).isEmpty());
    }
}
