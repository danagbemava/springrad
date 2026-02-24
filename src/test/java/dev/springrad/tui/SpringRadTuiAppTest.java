package dev.springrad.tui;

import dev.springrad.cli.CliArgs;
import dev.springrad.core.ProjectConfig;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class SpringRadTuiAppTest {

    @Test
    void acceptsDefaultsAndBuildsSelection() {
        String input = String.join("\n",
                "1",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "y"
        ) + "\n";

        SpringRadTuiApp app = appWithInput(input);
        SpringRadTuiApp.InteractiveSelection selection = app.start("demo-app", List.of("event-driven", "web-api"));

        assertNotNull(selection);
        assertEquals("event-driven", selection.presetName());
        CliArgs args = selection.cliArgs();
        assertEquals("demo-app", args.name());
        assertEquals("com.example", args.groupId());
        assertEquals("demo-app", args.artifactId());
        assertEquals(ProjectConfig.Packaging.jar, args.packaging());
        assertEquals(ProjectConfig.BuildTool.gradle, args.buildTool());
        assertEquals(ProjectConfig.AuthStyle.jwt, args.authStyle());
        assertEquals(ProjectConfig.Database.postgresql, args.database());
    }

    @Test
    void retriesInvalidSelectionsAndParsesDependenciesWithDeduping() {
        String input = String.join("\n",
                "9",
                "2",
                "my app",
                "dev.acme",
                "",
                "21",
                "",
                "2",
                "2",
                "3",
                "2",
                "web,  kafka,web ,actuator",
                "",
                "yes"
        ) + "\n";

        SpringRadTuiApp app = appWithInput(input);
        SpringRadTuiApp.InteractiveSelection selection = app.start("", List.of("event-driven", "web-api"));

        assertNotNull(selection);
        assertEquals("web-api", selection.presetName());
        CliArgs args = selection.cliArgs();
        assertEquals("my-app", args.artifactId());
        assertEquals(ProjectConfig.Packaging.war, args.packaging());
        assertEquals(ProjectConfig.BuildTool.maven, args.buildTool());
        assertEquals(ProjectConfig.AuthStyle.none, args.authStyle());
        assertEquals(ProjectConfig.Database.mysql, args.database());
        assertEquals(List.of("web", "kafka", "actuator"), args.dependencies());
    }

    @Test
    void returnsNullWhenNotConfirmed() {
        String input = String.join("\n",
                "1",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "",
                "n"
        ) + "\n";

        SpringRadTuiApp app = appWithInput(input);
        SpringRadTuiApp.InteractiveSelection selection = app.start("demo", List.of("web-api"));

        assertNull(selection);
    }

    private static SpringRadTuiApp appWithInput(String input) {
        StringWriter output = new StringWriter();
        return new SpringRadTuiApp(new StringReader(input), new PrintWriter(output, true));
    }
}
