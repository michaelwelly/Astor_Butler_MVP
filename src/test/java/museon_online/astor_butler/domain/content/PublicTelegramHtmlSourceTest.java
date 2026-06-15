package museon_online.astor_butler.domain.content;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PublicTelegramHtmlSourceTest {

    @Test
    void parsesTelegramPublicHtmlMessage() {
        PublicTelegramHtmlSource source = new PublicTelegramHtmlSource();
        ReflectionTestUtils.setField(source, "venueCode", "AERIS");
        ReflectionTestUtils.setField(source, "username", "aeris_gastrobar");
        String html = """
                <div class="tgme_widget_message" data-post="aeris_gastrobar/123">
                  <div class="tgme_widget_message_text js-message_text" dir="auto">Сегодня в 21:00<br/>DJ set &amp; ужин</div>
                  <a class="tgme_widget_message_photo_wrap" style="background-image:url('https://cdn.example/photo.jpg')"></a>
                  <time datetime="2026-06-15T16:00:00+00:00"></time>
                </div>
                """;

        List<NormalizedVenueContentPost> posts = source.parse(html);

        assertThat(posts).hasSize(1);
        NormalizedVenueContentPost post = posts.getFirst();
        assertThat(post.venueCode()).isEqualTo("AERIS");
        assertThat(post.sourceMessageId()).isEqualTo("123");
        assertThat(post.sourceUrl()).isEqualTo("https://t.me/aeris_gastrobar/123");
        assertThat(post.text()).contains("Сегодня в 21:00", "DJ set & ужин");
        assertThat(post.assets()).singleElement()
                .satisfies(asset -> {
                    assertThat(asset.assetKind()).isEqualTo("PHOTO");
                    assertThat(asset.sourceUrl()).isEqualTo("https://cdn.example/photo.jpg");
                });
    }
}
