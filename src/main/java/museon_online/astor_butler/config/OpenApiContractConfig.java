package museon_online.astor_butler.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class OpenApiContractConfig {

    @Bean
    public OpenApiCustomizer standardApiResponsesCustomizer() {
        return openApi -> {
            ensureErrorSchema(openApi);
            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation ->
                    standardResponses().forEach(operation.getResponses()::addApiResponse)
            ));
        };
    }

    private void ensureErrorSchema(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new LinkedHashMap<>());
        }
        openApi.getComponents().getSchemas().putIfAbsent("ApiErrorResponse", new Schema<>()
                .type("object")
                .addProperty("code", new Schema<>().type("string"))
                .addProperty("message", new Schema<>().type("string"))
                .addProperty("traceId", new Schema<>().type("string"))
                .addProperty("details", new Schema<>().type("object"))
                .addProperty("timestamp", new Schema<>().type("string").format("date-time")));
    }

    private Map<String, ApiResponse> standardResponses() {
        Map<String, ApiResponse> responses = new LinkedHashMap<>();
        responses.put("304", response("Not Modified", false));
        responses.put("400", response("Bad Request", true));
        responses.put("401", response("Unauthorized", true));
        responses.put("403", response("Forbidden", true));
        responses.put("404", response("Not Found", true));
        responses.put("409", response("Conflict", true));
        responses.put("422", response("Validation Failed", true));
        responses.put("429", response("Too Many Requests", true));
        responses.put("500", response("Internal Server Error", true));
        responses.put("502", response("Bad Gateway", true));
        responses.put("503", response("Service Unavailable", true));
        return responses;
    }

    private ApiResponse response(String description, boolean withErrorBody) {
        ApiResponse response = new ApiResponse().description(description);
        if (withErrorBody) {
            response.setContent(new Content().addMediaType(
                    "application/json",
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse"))
            ));
        }
        return response;
    }
}
