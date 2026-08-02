package museon_online.astor_butler.integration.saby;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "astor.integrations.saby")
@Getter
@Setter
public class SabyReservationProperties {

    private boolean enabled = false;
    private String baseUrl = "";
    private String authMethod = "";
    private String apiToken = "";
    private String clientId = "";
    private String clientSecret = "";
    private String refreshToken = "";
    private String organizationId = "";
    private String restaurantId = "";
    private String availabilityPath = "";
    private String reservationPath = "";
    private long timeoutMs = 3000;
    private int maxRetries = 1;

    public boolean configured() {
        return missingConfiguration().isEmpty();
    }

    public List<String> missingConfiguration() {
        List<String> missing = new ArrayList<>();
        require(missing, "SABY_API_BASE_URL", baseUrl);
        require(missing, "SABY_AUTH_METHOD", authMethod);
        require(missing, "SABY_ORGANIZATION_ID", organizationId);
        require(missing, "SABY_RESTAURANT_ID", restaurantId);
        require(missing, "SABY_AVAILABILITY_PATH", availabilityPath);
        require(missing, "SABY_RESERVATION_PATH", reservationPath);
        if (noTokenAuthConfigured() && noOAuthConfigured()) {
            missing.add("SABY_API_TOKEN or SABY_CLIENT_ID + SABY_CLIENT_SECRET + SABY_REFRESH_TOKEN");
        }
        return List.copyOf(missing);
    }

    private boolean noTokenAuthConfigured() {
        return apiToken == null || apiToken.isBlank();
    }

    private boolean noOAuthConfigured() {
        return isBlank(clientId) || isBlank(clientSecret) || isBlank(refreshToken);
    }

    private void require(List<String> missing, String envName, String value) {
        if (isBlank(value)) {
            missing.add(envName);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
