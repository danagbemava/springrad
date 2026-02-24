# springrad

Spring Boot RAD CLI for scaffolding production-ready Spring projects from presets.

## Requirements

- macOS/Linux terminal
- JDK 21+ for build/test
- JDK 22+ for interactive TamboUI mode (`new -i`) with `tamboui-panama-backend`

## Build

```bash
./gradlew clean build
```

## Test

```bash
./gradlew test
```

Test execution generates JaCoCo coverage reports at:

- `build/reports/jacoco/test/html/index.html`
- `build/reports/jacoco/test/jacocoTestReport.xml`

## Run CLI

```bash
./gradlew run --args='--help'
```

Or install a local distribution:

```bash
./gradlew installDist
./build/install/springrad/bin/springrad --help
```

## Generate Project (Non-interactive)

```bash
./build/install/springrad/bin/springrad new demo-app \
  --preset web-api \
  --group dev.example \
  --artifact demo-app \
  --output ./demo-app
```

Available built-in presets:

- `web-api`
- `event-driven`

## Generate Project (Interactive TamboUI)

Interactive mode requires Java 22+ at runtime because the current backend uses Panama APIs.

```bash
JAVA_HOME="$HOME/Library/Java/JavaVirtualMachines/azul-22.0.2/Contents/Home" \
JDK_JAVA_OPTIONS="--enable-native-access=ALL-UNNAMED" \
./build/install/springrad/bin/springrad new demo-app -i
```

## Preset Commands

List presets:

```bash
./build/install/springrad/bin/springrad preset list
```

Save a preset:

```bash
./build/install/springrad/bin/springrad preset save my-preset \
  --from web-api
```

Delete a preset:

```bash
./build/install/springrad/bin/springrad preset delete my-preset
```

Preset storage location:

- `~/.springrad/presets.json`

## Notes

- `new` applies Spring Initializr generation first, then overlays templates/scaffolds.
- Existing files are not overwritten by template overlay unless force behavior is added.
- CLI initializes a git repository and creates an initial commit in generated output.
