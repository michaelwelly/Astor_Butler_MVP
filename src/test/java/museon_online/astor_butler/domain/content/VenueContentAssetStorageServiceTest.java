package museon_online.astor_butler.domain.content;

import museon_online.astor_butler.storage.ObjectStorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class VenueContentAssetStorageServiceTest {

    private final ObjectStorageService objectStorageService = mock(ObjectStorageService.class);
    private final VenueContentAssetStorageService service = new VenueContentAssetStorageService(objectStorageService);

    @Test
    void keepsSourceAssetWhenMirroringDisabled() {
        ReflectionTestUtils.setField(service, "assetMirroringEnabled", false);
        NormalizedVenueContentPost post = post(new VenueContentAsset(
                "PHOTO",
                "https://cdn.example/photo.jpg",
                null,
                null,
                "image/jpeg"
        ));

        NormalizedVenueContentPost result = service.mirrorAssets(post);

        assertThat(result.assets()).singleElement()
                .satisfies(asset -> {
                    assertThat(asset.sourceUrl()).isEqualTo("https://cdn.example/photo.jpg");
                    assertThat(asset.objectKey()).isNull();
                });
        verify(objectStorageService, never()).uploadMediaObject(any(), any(), any());
    }

    private NormalizedVenueContentPost post(VenueContentAsset asset) {
        return new NormalizedVenueContentPost(
                "AERIS",
                "TELEGRAM_PUBLIC_HTML",
                "aeris_gastrobar",
                "123",
                "https://t.me/aeris_gastrobar/123",
                "hash",
                Instant.parse("2026-06-15T12:00:00Z"),
                "Сегодня DJ set",
                List.of(asset),
                "{}"
        );
    }
}
