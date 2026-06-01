package museon_online.astor_butler.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI astorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Astor Butler MVP API")
                        .description("Backend API для Astor Butler MVP: C3FLEX.com lead-gen frontend, Telegram/FSM guest assistant и System Design delivery контур.")
                        .version("1.0.0"));
    }
}
