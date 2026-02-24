package dev.springrad.core;

import java.nio.file.Path;
import java.util.List;

interface GitCommandRunner {
    CommandResult run(Path workingDirectory, List<String> command);

    record CommandResult(int exitCode, String stdout, String stderr) {
        boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
