package museon_online.astor_butler.integration.saby;

import museon_online.astor_butler.domain.booking.external.ExternalReservationProvider;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SabyReservationProviderTest {

    @Test
    void reportsMissingConfigurationWithoutCallingExternalApi() {
        SabyReservationProperties properties = new SabyReservationProperties();
        properties.setEnabled(true);
        SabyReservationProvider provider = new SabyReservationProvider(properties);

        var status = provider.status();
        var availability = provider.checkAvailability(new ExternalReservationProvider.ExternalAvailabilityRequest(
                "AERIS",
                Instant.parse("2026-08-02T15:00:00Z"),
                Instant.parse("2026-08-02T17:00:00Z"),
                2,
                null,
                "WINE_ROOM"
        ));

        assertThat(status.configured()).isFalse();
        assertThat(status.missingConfiguration()).contains(
                "SABY_API_BASE_URL",
                "SABY_AUTH_METHOD",
                "SABY_ORGANIZATION_ID",
                "SABY_RESTAURANT_ID",
                "SABY_AVAILABILITY_PATH",
                "SABY_RESERVATION_PATH",
                "SABY_API_TOKEN or SABY_CLIENT_ID + SABY_CLIENT_SECRET + SABY_REFRESH_TOKEN"
        );
        assertThat(availability.status()).isEqualTo("PROVIDER_NOT_CONFIGURED");
        assertThat(availability.providerConfigured()).isFalse();
    }

    @Test
    void blocksWritesEvenWhenPlaceholderConfigIsCompleteUntilContractIsImplemented() {
        SabyReservationProperties properties = new SabyReservationProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("https://example.invalid");
        properties.setAuthMethod("token");
        properties.setApiToken("placeholder");
        properties.setOrganizationId("org");
        properties.setRestaurantId("restaurant");
        properties.setAvailabilityPath("/availability");
        properties.setReservationPath("/reservations");
        SabyReservationProvider provider = new SabyReservationProvider(properties);

        var result = provider.reserve(null, "idem-1");

        assertThat(provider.status().configured()).isTrue();
        assertThat(result.created()).isFalse();
        assertThat(result.status()).isEqualTo("PROVIDER_CONTRACT_NOT_IMPLEMENTED");
    }
}
