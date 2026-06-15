package museon_online.astor_butler.api.content;

import museon_online.astor_butler.domain.content.AerisContentReadService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AerisContentControllerTest {

    private final AerisContentReadService contentReadService = mock(AerisContentReadService.class);
    private final AerisContentController controller = new AerisContentController(contentReadService);

    @Test
    void returnsMenuAssetsView() {
        AerisContentReadService.MenuAssetsView view = new AerisContentReadService.MenuAssetsView(
                "AERIS",
                List.of(),
                "media_assets:AERIS_MENU_*"
        );
        when(contentReadService.menuAssets()).thenReturn(view);

        AerisContentReadService.MenuAssetsView response = controller.menuAssets();

        assertThat(response).isEqualTo(view);
        verify(contentReadService).menuAssets();
    }

    @Test
    void returnsQuietGuideViewForPrompt() {
        AerisContentReadService.QuietGuideView view = new AerisContentReadService.QuietGuideView(
                "AERIS",
                null,
                new AerisContentReadService.ConceptView("AERIS", "summary"),
                List.of()
        );
        when(contentReadService.quietGuide("афиша")).thenReturn(view);

        AerisContentReadService.QuietGuideView response = controller.quietGuide("афиша");

        assertThat(response).isEqualTo(view);
        verify(contentReadService).quietGuide("афиша");
    }
}
