package dev.springrad.cli;

import picocli.CommandLine.Command;

@Command(name = "preset", description = "Manage project presets")
public final class PresetCommand implements Runnable {
    @Override
    public void run() {
        System.out.println("Preset management will be implemented in upcoming issues.");
    }
}
