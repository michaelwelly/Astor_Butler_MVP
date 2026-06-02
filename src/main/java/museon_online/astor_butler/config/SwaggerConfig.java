package museon_online.astor_butler.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.tags.Tag;
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
                        .version("1.0.0"))
                .addTagsItem(tag("Auth API", "OAuth2/OIDC, JWT, roles and stateless access boundary."))
                .addTagsItem(tag("Consent Vault API", "Consent capture, privacy policy acceptance and export boundary."))
                .addTagsItem(tag("User API", "Memory Engine identity profile, roles and user lookup boundary."))
                .addTagsItem(tag("FSM API", "Safe Play, Panic Exit and normalized message gateway for Telegram, web chat and future messengers."))
                .addTagsItem(tag("Booking API", "Slot Keeper booking requests, statuses, drafts and manager notes."))
                .addTagsItem(tag("Timeline API", "User, booking, manager and system timelines."))
                .addTagsItem(tag("Posts/Content API", "Quiet Guide posts, afisha, promo blocks and SEO content."))
                .addTagsItem(tag("Media API", "Media metadata, upload contracts, S3 links and soft delete."))
                .addTagsItem(tag("Notifications API", "Smart Tip, Hidden Heart and operational delivery commands."))
                .addTagsItem(tag("Manager API", "Manager dashboard, tasks, escalations and staff workflow."))
                .addTagsItem(tag("Integrations API", "Telegram, CRM, analytics and external integration boundary."))
                .addTagsItem(tag("Observability API", "Readiness, liveness, Prometheus and local smoke boundaries."))
                .addTagsItem(tag("System API", "Read-only system checks for local and load-test scenarios."));
    }

    private Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }
}
