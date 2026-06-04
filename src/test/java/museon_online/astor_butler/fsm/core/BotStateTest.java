package museon_online.astor_butler.fsm.core;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BotStateTest {

    @Test
    void mapsLegacyContactToConsentRequired() {
        assertThat(BotState.fromStorageValue("CONTACT")).isEqualTo(BotState.CONSENT_REQUIRED);
    }

    @Test
    void mapsLegacyMenuToReadyForDialog() {
        assertThat(BotState.fromStorageValue("MENU")).isEqualTo(BotState.READY_FOR_DIALOG);
    }

    @Test
    void keepsNewStateAsIs() {
        assertThat(BotState.fromStorageValue("READY_FOR_DIALOG")).isEqualTo(BotState.READY_FOR_DIALOG);
    }
}
