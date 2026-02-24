package dev.springrad.core;

import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class GitInitializer {
    public static final String INITIAL_COMMIT_MESSAGE = "chore: initial project scaffold via springrad";

    private final GitCommandRunner runner;

    public GitInitializer() {
        this(new ProcessGitCommandRunner());
    }

    GitInitializer(GitCommandRunner runner) {
        this.runner = runner;
    }

    public void initializeRepository(Path projectDirectory, PrintWriter warningsOut) {
        if (!isGitAvailable()) {
            warningsOut.println("Warning: git is not installed or unavailable on PATH. Skipping repository initialization.");
            return;
        }

        if (Files.isDirectory(projectDirectory.resolve(".git"))) {
            warningsOut.println("Warning: target directory is already a git repository. Skipping git init.");
            return;
        }

        if (!run(projectDirectory, warningsOut, "git", "init")) {
            return;
        }
        if (!run(projectDirectory, warningsOut, "git", "add", ".")) {
            return;
        }

        run(
                projectDirectory,
                warningsOut,
                "git",
                "-c",
                "user.name=springrad",
                "-c",
                "user.email=springrad@local",
                "commit",
                "-m",
                INITIAL_COMMIT_MESSAGE
        );
    }

    private boolean isGitAvailable() {
        return runner.run(null, List.of("git", "--version")).isSuccess();
    }

    private boolean run(Path projectDirectory, PrintWriter warningsOut, String... command) {
        GitCommandRunner.CommandResult result = runner.run(projectDirectory, List.of(command));
        if (result.isSuccess()) {
            return true;
        }
        warningsOut.println("Warning: git step failed: " + String.join(" ", command));
        if (!result.stderr().isBlank()) {
            warningsOut.println(result.stderr().trim());
        }
        return false;
    }
}
