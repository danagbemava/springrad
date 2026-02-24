package dev.springrad.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class GitInitializerTest {
    @TempDir
    Path tempDir;

    @Test
    void missingGitSkipsGracefullyWithWarning() {
        GitInitializer initializer = new GitInitializer((wd, cmd) -> new GitCommandRunner.CommandResult(127, "", "not found"));
        StringWriter warnings = new StringWriter();

        initializer.initializeRepository(tempDir, new PrintWriter(warnings, true));

        assertTrue(warnings.toString().contains("git is not installed"));
    }

    @Test
    void existingGitRepoSkipsInitWithWarning() throws Exception {
        Files.createDirectories(tempDir.resolve(".git"));
        GitInitializer initializer = new GitInitializer((wd, cmd) -> {
            if (cmd.equals(List.of("git", "--version"))) {
                return new GitCommandRunner.CommandResult(0, "git version 2.0", "");
            }
            return new GitCommandRunner.CommandResult(0, "", "");
        });
        StringWriter warnings = new StringWriter();

        initializer.initializeRepository(tempDir, new PrintWriter(warnings, true));

        assertTrue(warnings.toString().contains("already a git repository"));
    }

    @Test
    void initializesGitAndCreatesSingleCommitWhenGitAvailable() throws Exception {
        ProcessGitCommandRunner runner = new ProcessGitCommandRunner();
        assumeTrue(runner.run(null, List.of("git", "--version")).isSuccess());

        Files.writeString(tempDir.resolve(".gitignore"), "build/\n");
        Files.writeString(tempDir.resolve("README.md"), "demo\n");
        GitInitializer initializer = new GitInitializer(runner);
        StringWriter warnings = new StringWriter();

        initializer.initializeRepository(tempDir, new PrintWriter(warnings, true));

        GitCommandRunner.CommandResult count = runner.run(tempDir, List.of("git", "rev-list", "--count", "HEAD"));
        GitCommandRunner.CommandResult log = runner.run(tempDir, List.of("git", "log", "--oneline", "-1"));

        assertEquals("1", count.stdout().trim());
        assertTrue(log.stdout().contains(GitInitializer.INITIAL_COMMIT_MESSAGE));
    }

    @Test
    void gitAddFailurePrintsWarningAndSkipsCommit() {
        List<List<String>> commands = new ArrayList<>();
        GitInitializer initializer = new GitInitializer((wd, cmd) -> {
            commands.add(cmd);
            if (cmd.equals(List.of("git", "--version"))) {
                return new GitCommandRunner.CommandResult(0, "git version 2.0", "");
            }
            if (cmd.equals(List.of("git", "add", "."))) {
                return new GitCommandRunner.CommandResult(1, "", "add failed");
            }
            return new GitCommandRunner.CommandResult(0, "", "");
        });
        StringWriter warnings = new StringWriter();

        initializer.initializeRepository(tempDir, new PrintWriter(warnings, true));

        assertTrue(warnings.toString().contains("git step failed: git add ."));
        assertTrue(commands.stream().noneMatch(c -> c.contains("commit")));
    }
}
