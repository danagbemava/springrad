package dev.springrad.core;

import java.nio.file.Path;
import java.util.List;

public record GenerationResult(Path outputPath, List<String> topLevelEntries) {
}
