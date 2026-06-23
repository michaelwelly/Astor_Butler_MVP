package museon_online.astor_butler.domain.content;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class C3flexVideoCatalogService {

    private static final Instant UPDATED_AT = Instant.parse("2026-06-23T12:00:00Z");

    @Value("${astor.storage.s3.public-endpoint:http://localhost:9000}")
    private String publicEndpoint;

    @Value("${astor.storage.s3.media-bucket:astor-media}")
    private String mediaBucket;

    public VideoCatalogView videos(String category, String tag, Boolean featured, Integer limit) {
        List<VideoItemView> filtered = seed().stream()
                .filter(item -> category == null || category.isBlank() || item.category().equalsIgnoreCase(category.trim()))
                .filter(item -> tag == null || tag.isBlank() || hasTag(item, tag))
                .filter(item -> featured == null || item.featured() == featured)
                .toList();

        int safeLimit = limit == null ? 30 : Math.max(1, Math.min(limit, 100));
        List<VideoItemView> page = filtered.stream().limit(safeLimit).toList();
        return new VideoCatalogView(page, new PageView(safeLimit, 0, filtered.size()));
    }

    private boolean hasTag(VideoItemView item, String tag) {
        String normalized = normalize(tag);
        return item.tags().stream().map(this::normalize).anyMatch(normalized::equals);
    }

    private List<VideoItemView> seed() {
        List<VideoItemView> items = new ArrayList<>();
        items.add(video("segreto", "events", "Segreto", "Ресторан в кадре", "Атмосфера ресторана - живая, темная, кинематографичная.", "01:40", true));
        items.add(video("wine-story", "events", "Wine Story", "Вино и свет", "Момент налитого бокала - гастрономический кинематограф.", "00:45", false));
        items.add(video("golden-hour", "events", "Golden Hour", "Свет перед закатом", "Те сорок минут, когда все становится золотым.", "00:54", false));
        items.add(video("the-crowd", "events", "The Crowd", "Тысяча лиц", "Один фестиваль, снятый глазами тех, кто был внутри.", "00:37", false));
        items.add(video("after-rain", "events", "After Rain", "Мокрый асфальт, неон", "Открытая сцена под ливнем. Никто не ушел.", "00:43", false));
        items.add(video("opening-night", "events", "Opening Night", "Первый вечер", "Волнение за кулисами за секунду до начала.", "01:05", false));
        items.add(video("neon-night", "events", "Neon Night", "Свечение города", "Ночь, когда город стал декорацией.", "00:51", false));
        items.add(video("studio-a", "events", "Studio A", "Живая запись", "Один дубль. Без монтажа. Только момент.", "02:14", false));
        items.add(video("final-dance", "events", "Final Dance", "Последний трек", "Финал сета. Та секунда тишины до аплодисментов.", "01:28", false));
        items.add(video("the-room", "events", "The Room", "Пространство без стен", "Архитектура как соучастник истории.", "00:59", false));

        items.add(video("cristal-pour", "reels", "Cristal", "Шампанское во льду", "Louis Roederer. Продукт, снятый как ювелирное изделие.", "00:31", true));
        items.add(video("texture-01", "reels", "Texture 01", "Материал крупным планом", "Кожа, дерево, металл - все, что можно почувствовать через экран.", "00:22", false));
        items.add(video("pour-it", "reels", "Pour It", "Жидкость в движении", "Один продукт, снятый так, будто это поэзия.", "00:18", false));
        items.add(video("drop-zone", "reels", "Drop Zone", "Падение и свет", "Высокоскоростная съемка превращает физику в эстетику.", "00:25", false));
        items.add(video("unbox", "reels", "Unbox", "Первый раз", "Момент открытия упаковки как ритуал.", "00:29", false));
        items.add(video("soft-hands", "reels", "Soft Hands", "Руки в кадре", "Руки как инструмент рассказа о продукте.", "00:33", false));
        items.add(video("cold-brew", "reels", "Cold Brew", "Процесс приготовления", "Процесс приготовления, снятый как документальное кино.", "00:27", false));
        items.add(video("reflect", "reels", "Reflect", "Отражение продукта", "Один объект, бесконечное зеркало.", "00:21", false));
        items.add(video("in-frame", "reels", "In Frame", "Композиция бренда", "Композиция как аргумент в пользу бренда.", "00:35", false));
        items.add(video("surface", "reels", "Surface", "Свет на поверхности", "Свет касается продукта. Продукт становится желанием.", "00:19", false));

        items.add(video("night-drive", "commercials", "Night Drive", "Импульс в рекламе", "Рекламный фильм, где импульс говорит сам за себя.", "01:02", true));
        items.add(video("momentum", "commercials", "Momentum", "Движение бренда", "Бренд движется вперед. Камера едва успевает.", "00:45", false));
        items.add(video("quiet-power", "commercials", "Quiet Power", "Тихая сила", "Не каждый бренд кричит. Некоторые просто существуют.", "01:15", false));
        items.add(video("origin", "commercials", "Origin", "История происхождения", "Откуда берется то, что ты покупаешь.", "02:00", false));
        items.add(video("the-pitch", "commercials", "The Pitch", "Один съемочный день", "Кампания, которую нужно было снять за один день.", "00:50", false));
        items.add(video("season", "commercials", "Season", "Сезонная кампания", "Новая коллекция рассказывает о сезоне лучше любого слогана.", "01:30", false));
        items.add(video("proof", "commercials", "Proof", "Доказательство продуктом", "Демонстрация продукта как кинематографический аргумент.", "00:40", false));
        items.add(video("last-mile", "commercials", "Last Mile", "Последний шаг", "Спортивный бренд. Все решает последний шаг.", "00:55", false));
        items.add(video("blueprint", "commercials", "Blueprint", "От чертежа до кадра", "От чертежа до финального кадра - история о создании.", "01:10", false));
        items.add(video("chapter-one", "commercials", "Chapter One", "Лонч-фильм", "Лонч-фильм для бренда, который только начинается.", "01:45", false));
        return items;
    }

    private VideoItemView video(String slug, String category, String title, String shortDescription,
                                String description, String duration, boolean featured) {
        String orientation = category.equals("reels") ? "portrait" : "landscape";
        return new VideoItemView(
                deterministicId(slug),
                slug,
                title,
                description,
                shortDescription,
                tags(category),
                category,
                featured,
                seconds(duration),
                orientation,
                "READY",
                poster(slug, orientation),
                List.of(source(slug, "mobile", orientation), source(slug, "desktop", orientation)),
                new CtaView("Обсудить похожий проект", "PROJECT_REQUEST"),
                UPDATED_AT
        );
    }

    private PosterView poster(String slug, String orientation) {
        boolean portrait = orientation.equals("portrait");
        String objectKey = "content/c3flex/posters/%s.jpg".formatted(slug);
        return new PosterView(
                deterministicId(slug + "-poster"),
                publicUrl(objectKey),
                "image/jpeg",
                portrait ? 1080 : 1920,
                portrait ? 1920 : 1080
        );
    }

    private SourceView source(String slug, String variant, String orientation) {
        boolean portrait = orientation.equals("portrait");
        String objectKey = "content/c3flex/videos/%s/%s.mp4".formatted(slug, variant);
        int width = portrait ? (variant.equals("mobile") ? 720 : 1080) : (variant.equals("mobile") ? 1280 : 1920);
        int height = portrait ? (variant.equals("mobile") ? 1280 : 1920) : (variant.equals("mobile") ? 720 : 1080);
        int bitrate = variant.equals("mobile") ? 1800 : 4500;
        return new SourceView(variant, publicUrl(objectKey), "video/mp4", width, height, bitrate);
    }

    private List<String> tags(String category) {
        return switch (category) {
            case "events" -> List.of("events", "hospitality", "storytelling");
            case "reels" -> List.of("reels", "product", "social");
            case "commercials" -> List.of("commercials", "brand", "campaign");
            default -> List.of(category);
        };
    }

    private int seconds(String duration) {
        String[] parts = duration.split(":");
        return Integer.parseInt(parts[0]) * 60 + Integer.parseInt(parts[1]);
    }

    private String deterministicId(String slug) {
        return java.util.UUID.nameUUIDFromBytes(("c3flex-video:" + slug).getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private String publicUrl(String objectKey) {
        String endpoint = publicEndpoint == null || publicEndpoint.isBlank()
                ? "http://localhost:9000"
                : publicEndpoint;
        return endpoint.replaceAll("/+$", "") + "/" + mediaBucket + "/" + objectKey.replaceAll("^/+", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    public record VideoCatalogView(
            List<VideoItemView> items,
            PageView page
    ) {
    }

    public record VideoItemView(
            String videoId,
            String slug,
            String title,
            String description,
            String shortDescription,
            List<String> tags,
            String category,
            boolean featured,
            int durationSeconds,
            String orientation,
            String status,
            PosterView poster,
            List<SourceView> sources,
            CtaView cta,
            Instant updatedAt
    ) {
    }

    public record PosterView(
            String assetId,
            String publicUrl,
            String contentType,
            int width,
            int height
    ) {
    }

    public record SourceView(
            String variant,
            String publicUrl,
            String contentType,
            int width,
            int height,
            int bitrateKbps
    ) {
    }

    public record CtaView(
            String label,
            String intent
    ) {
    }

    public record PageView(
            int limit,
            int offset,
            int total
    ) {
    }
}
