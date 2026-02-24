package dev.springrad.core;

@FunctionalInterface
public interface ProjectGenerator {
    GenerationResult generate(ProjectConfig config);
}
