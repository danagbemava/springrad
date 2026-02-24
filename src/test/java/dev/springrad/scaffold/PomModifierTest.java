package dev.springrad.scaffold;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PomModifierTest {
    @TempDir
    Path tempDir;

    @Test
    void addNewDependencyWritesItToPom() throws Exception {
        Path pom = writePom(basePom());
        PomModifier modifier = new PomModifier(pom);

        modifier.addDependency("org.springdoc", "springdoc-openapi-starter-webmvc-ui", "2.6.0", null);
        modifier.save();

        String updated = Files.readString(pom);
        assertTrue(updated.contains("<groupId>org.springdoc</groupId>"));
        assertTrue(updated.contains("<artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>"));
        assertTrue(updated.contains("<version>2.6.0</version>"));
    }

    @Test
    void addDuplicateDependencyIsNoOp() throws Exception {
        String pomContent = """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>dev.springrad</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <dependencies>
                    <dependency>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                  </dependencies>
                </project>
                """;
        Path pom = writePom(pomContent);
        PomModifier modifier = new PomModifier(pom);

        modifier.addDependency("org.springframework.boot", "spring-boot-starter-web", null, null);
        modifier.save();

        String updated = Files.readString(pom);
        int first = updated.indexOf("<artifactId>spring-boot-starter-web</artifactId>");
        int second = updated.indexOf("<artifactId>spring-boot-starter-web</artifactId>", first + 1);
        assertEquals(-1, second);
    }

    @Test
    void addPropertyWritesToPropertiesBlock() throws Exception {
        Path pom = writePom(basePom());
        PomModifier modifier = new PomModifier(pom);

        modifier.addProperty("java.version", "21");
        modifier.save();

        String updated = Files.readString(pom);
        assertTrue(updated.contains("<properties>"));
        assertTrue(updated.contains("<java.version>21</java.version>"));
    }

    @Test
    void unmodifiedRoundTripKeepsContentIdentical() throws Exception {
        String original = basePom();
        Path pom = writePom(original);
        PomModifier modifier = new PomModifier(pom);

        modifier.save();

        assertEquals(original, Files.readString(pom));
    }

    @Test
    void setParentVersionUpdatesParentOnly() throws Exception {
        Path pom = writePom(basePom());
        PomModifier modifier = new PomModifier(pom);

        modifier.setParentVersion("3.4.5");
        modifier.save();

        String updated = Files.readString(pom);
        assertTrue(updated.contains("<version>3.4.5</version>"));
        assertTrue(updated.contains("<artifactId>spring-boot-starter-parent</artifactId>"));
    }

    @Test
    void addPluginDoesNotDuplicateExistingPlugin() throws Exception {
        String pomContent = """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>dev.springrad</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                  <build>
                    <plugins>
                      <plugin>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-maven-plugin</artifactId>
                        <version>3.4.1</version>
                      </plugin>
                    </plugins>
                  </build>
                </project>
                """;
        Path pom = writePom(pomContent);
        PomModifier modifier = new PomModifier(pom);

        modifier.addPlugin("org.springframework.boot", "spring-boot-maven-plugin", "3.4.1");
        modifier.save();

        String updated = Files.readString(pom);
        String needle = "<artifactId>spring-boot-maven-plugin</artifactId>";
        assertEquals(updated.indexOf(needle), updated.lastIndexOf(needle));
    }

    private Path writePom(String content) throws Exception {
        Path pom = tempDir.resolve("pom.xml");
        Files.writeString(pom, content);
        return pom;
    }

    private static String basePom() {
        return """
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>3.4.1</version>
                  </parent>
                  <groupId>dev.springrad</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.1.0-SNAPSHOT</version>
                </project>
                """;
    }
}
