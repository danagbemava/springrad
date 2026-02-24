package {{packageName}}.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("{{artifactId}} API")
                        .version("v1")
                        .description("API documentation for {{artifactId}}")
                        .contact(new Contact().name("API Support").email("support@example.com"))
                        .license(new License().name("Apache-2.0")));
    }
}
