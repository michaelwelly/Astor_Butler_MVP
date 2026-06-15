package museon_online.astor_butler.domain.content;

import museon_online.astor_butler.domain.media.AerisMediaCatalog;
import museon_online.astor_butler.domain.media.MediaAsset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AerisContentReadServiceTest {

    private final AerisMediaCatalog mediaCatalog = mock(AerisMediaCatalog.class);
    private final VenueContentQueryService venueContentQueryService = mock(VenueContentQueryService.class);
    private final VenueContentRepository venueContentRepository = mock(VenueContentRepository.class);
    private final AerisContentReadService service = new AerisContentReadService(
            mediaCatalog,
            venueContentQueryService,
            venueContentRepository
    );

    @Test
    void returnsMenuAssetsWithPublicLinksAndRagSource() {
        ReflectionTestUtils.setField(service, "publicEndpoint", "http://localhost:9000");
        when(mediaCatalog.allMenus()).thenReturn(List.of(asset("AERIS_MENU_WINE", "Винная карта", "content/aeris/menu/wine.pdf")));
        when(mediaCatalog.menuRagSource()).thenReturn("media_assets:AERIS_MENU_*");

        AerisContentReadService.MenuAssetsView view = service.menuAssets();

        assertThat(view.venueCode()).isEqualTo("AERIS");
        assertThat(view.ragSource()).isEqualTo("media_assets:AERIS_MENU_*");
        assertThat(view.menus()).singleElement()
                .satisfies(menu -> {
                    assertThat(menu.assetCode()).isEqualTo("AERIS_MENU_WINE");
                    assertThat(menu.publicUrl()).isEqualTo("http://localhost:9000/astor-media/content/aeris/menu/wine.pdf");
                });
    }

    @Test
    void returnsQuietGuideConceptInteriorAndActivePostsWithAssets() {
        ReflectionTestUtils.setField(service, "publicEndpoint", "http://localhost:9000/");
        UUID postId = UUID.fromString("f0b04ed5-3c10-44e3-a6cb-9d7e5d990db1");
        VenueContentPost post = new VenueContentPost(
                postId,
                "AERIS",
                "TELEGRAM_PUBLIC_HTML",
                "aeris_gastrobar",
                "123",
                "https://t.me/aeris_gastrobar/123",
                VenueContentType.AFISHA_EVENT,
                VenueContentStatus.ACTIVE,
                "Сегодня в AERIS",
                "DJ set в 21:00",
                Instant.parse("2026-06-15T16:00:00Z"),
                Instant.parse("2026-07-15T16:00:00Z"),
                0.9,
                Instant.parse("2026-06-15T12:00:00Z"),
                Instant.parse("2026-06-15T12:00:00Z"),
                Instant.parse("2026-06-15T12:00:00Z")
        );

        when(mediaCatalog.interiorTour()).thenReturn(asset("AERIS_INTERIOR_TOUR", "AERIS interior tour", "content/aeris/interior/INTERIOR.mp4"));
        when(venueContentQueryService.activeQuietGuidePosts("AERIS", "афиша")).thenReturn(List.of(post));
        when(venueContentRepository.findAssetsByPostId(postId)).thenReturn(List.of(new VenueContentAsset(
                "PHOTO",
                "https://cdn.example/photo.jpg",
                "astor-media",
                "content/aeris/channel/123/photo.jpg",
                "image/jpeg"
        )));

        AerisContentReadService.QuietGuideView view = service.quietGuide("афиша");

        assertThat(view.interiorTour().publicUrl()).isEqualTo("http://localhost:9000/astor-media/content/aeris/interior/INTERIOR.mp4");
        assertThat(view.concept().title()).contains("Георгия Матвеева");
        assertThat(view.activePosts()).singleElement()
                .satisfies(activePost -> {
                    assertThat(activePost.id()).isEqualTo(postId);
                    assertThat(activePost.assets()).singleElement()
                            .satisfies(asset -> assertThat(asset.publicUrl())
                                    .isEqualTo("http://localhost:9000/astor-media/content/aeris/channel/123/photo.jpg"));
                });
    }

    private MediaAsset asset(String code, String title, String objectKey) {
        return new MediaAsset(
                code,
                "AERIS",
                "QUIET_GUIDE",
                "PDF_MENU",
                title,
                "astor-media",
                objectKey,
                objectKey.substring(objectKey.lastIndexOf('/') + 1),
                objectKey.endsWith(".mp4") ? "video/mp4" : "application/pdf",
                true
        );
    }
}
