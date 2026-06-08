package museon_online.astor_butler.domain.media;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AerisMediaCatalog {

    public static final String MENU_KITCHEN = "AERIS_MENU_KITCHEN";
    public static final String MENU_BAR = "AERIS_MENU_BAR";
    public static final String MENU_ELEMENTS = "AERIS_MENU_ELEMENTS";
    public static final String MENU_WINE = "AERIS_MENU_WINE";
    public static final String FLOOR_PLAN = "AERIS_FLOOR_PLAN";
    public static final String INTERIOR_TOUR = "AERIS_INTERIOR_TOUR";

    private final MediaAssetRepository mediaAssetRepository;

    @Value("${astor.storage.s3.media-bucket:astor-media}")
    private String mediaBucket;

    @Value("${astor.media.aeris.menu.kitchen-object-key:content/aeris/menu/kitchen/MENU_AERIS_A4_2026_DIGITAL.pdf}")
    private String kitchenMenuObjectKey;

    @Value("${astor.media.aeris.menu.bar-object-key:content/aeris/menu/bar/BAR_CARD.pdf}")
    private String barMenuObjectKey;

    @Value("${astor.media.aeris.menu.elements-object-key:content/aeris/menu/elements/ELEMENTS_CARD.pdf}")
    private String elementsMenuObjectKey;

    @Value("${astor.media.aeris.menu.wine-object-key:content/aeris/menu/wine/WINE_MENU_2026_FINAL.pdf}")
    private String wineMenuObjectKey;

    @Value("${astor.media.aeris.menu.rag-source:media_assets:AERIS_MENU_*}")
    private String menuRagSource;

    @Value("${astor.media.aeris.floor-plan-object-key:content/aeris/floor-plan/AERIS_PLAN.pdf}")
    private String floorPlanObjectKey;

    @Value("${astor.media.aeris.interior-video-object-key:content/aeris/interior/INTERIOR.mp4}")
    private String interiorVideoObjectKey;

    public MediaAsset kitchenMenu() {
        return asset(MENU_KITCHEN)
                .orElseGet(() -> fallback(MENU_KITCHEN, "QUIET_GUIDE", "PDF_MENU",
                        "Кухня / основное меню", kitchenMenuObjectKey, "MENU AERIS A4 2026 DIGITAL.pdf", "application/pdf"));
    }

    public MediaAsset barMenu() {
        return asset(MENU_BAR)
                .orElseGet(() -> fallback(MENU_BAR, "QUIET_GUIDE", "PDF_MENU",
                        "Барная карта", barMenuObjectKey, "BAR CARD.pdf", "application/pdf"));
    }

    public MediaAsset elementsMenu() {
        return asset(MENU_ELEMENTS)
                .orElseGet(() -> fallback(MENU_ELEMENTS, "QUIET_GUIDE", "PDF_MENU",
                        "Коктейли / Elements", elementsMenuObjectKey, "ELEMENTS CARD.pdf", "application/pdf"));
    }

    public MediaAsset wineMenu() {
        return asset(MENU_WINE)
                .orElseGet(() -> fallback(MENU_WINE, "QUIET_GUIDE", "PDF_MENU",
                        "Винная карта", wineMenuObjectKey, "WINE MENU 2026 FINAL.pdf", "application/pdf"));
    }

    public List<MediaAsset> allMenus() {
        return List.of(kitchenMenu(), barMenu(), elementsMenu(), wineMenu());
    }

    public MediaAsset floorPlan() {
        return asset(FLOOR_PLAN)
                .orElseGet(() -> fallback(FLOOR_PLAN, "TABLE_BOOKING", "FLOOR_PLAN",
                        "План зала AERIS", floorPlanObjectKey, "AERIS PLAN.pdf", "application/pdf"));
    }

    public MediaAsset interiorTour() {
        return asset(INTERIOR_TOUR)
                .orElseGet(() -> fallback(INTERIOR_TOUR, "QUIET_GUIDE", "VIDEO_TOUR",
                        "AERIS interior tour", interiorVideoObjectKey, "INTERIOR.mp4", "video/mp4"));
    }

    public String menuRagSource() {
        return menuRagSource;
    }

    private Optional<MediaAsset> asset(String assetCode) {
        return mediaAssetRepository.findActiveByCode(assetCode);
    }

    private MediaAsset fallback(String assetCode, String domain, String kind, String title,
                                String objectKey, String filename, String contentType) {
        return new MediaAsset(assetCode, "AERIS", domain, kind, title, mediaBucket,
                objectKey, filename, contentType, true);
    }
}
