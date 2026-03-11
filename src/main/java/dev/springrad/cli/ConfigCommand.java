package dev.springrad.cli;

import picocli.CommandLine.Command;

@Command(
        name = "config",
        description = "Manage global springrad configuration",
        subcommands = {
                ConfigInitCommand.class
        }
)
public final class ConfigCommand implements Runnable {
    @Override
    public void run() {
        // Intentionally empty; show usage for subcommands.
    }
}
