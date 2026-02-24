package dev.springrad;

import dev.springrad.cli.SpringRadCommand;
import picocli.CommandLine;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new SpringRadCommand()).execute(args);
        System.exit(exitCode);
    }
}
