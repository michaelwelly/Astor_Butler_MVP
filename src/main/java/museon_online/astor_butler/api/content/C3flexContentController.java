package museon_online.astor_butler.api.content;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import museon_online.astor_butler.domain.content.C3flexVideoCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content/c3flex")
@RequiredArgsConstructor
@Tag(name = "Posts/Content API", description = "C3FLEX portfolio content and video catalog")
public class C3flexContentController {

    private final C3flexVideoCatalogService videoCatalogService;

    @GetMapping("/videos")
    @Operation(summary = "List C3FLEX portfolio video catalog")
    public C3flexVideoCatalogService.VideoCatalogView videos(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "tag", required = false) String tag,
            @RequestParam(value = "featured", required = false) Boolean featured,
            @RequestParam(value = "limit", required = false) Integer limit
    ) {
        return videoCatalogService.videos(category, tag, featured, limit);
    }
}
