package museon_online.astor_butler.api.content;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.content.AerisContentReadService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/aeris")
@RequiredArgsConstructor
@Tag(name = "Posts/Content API", description = "AERIS menu assets, Quiet Guide content and venue materials")
public class AerisContentController {

    private final AerisContentReadService contentReadService;

    @GetMapping("/menu-assets")
    @Operation(summary = "List active AERIS menu assets")
    public AerisContentReadService.MenuAssetsView menuAssets() {
        return contentReadService.menuAssets();
    }

    @GetMapping("/quiet-guide")
    @Operation(summary = "Get active AERIS Quiet Guide materials")
    public AerisContentReadService.QuietGuideView quietGuide(
            @RequestParam(value = "prompt", required = false) String prompt
    ) {
        return contentReadService.quietGuide(prompt);
    }
}
