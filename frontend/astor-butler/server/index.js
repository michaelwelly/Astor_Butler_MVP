const ASSET_MANIFEST = {
  "/": "index.html",
  "/index.html": "index.html",
  "/css/style.css": "css/style.css",
  "/js/main.js": "js/main.js",
  "/js/widget.js": "js/widget.js",
  "/assets/favicon.svg": "assets/favicon.svg",
  "/assets/og-image.png": "assets/og-image.png",
  "/docs/offer.html": "docs/offer.html",
  "/docs/comparison.html": "docs/comparison.html",
  "/docs/brand.html": "docs/brand.html",
  "/docs/docs.css": "docs/docs.css"
};

const MIME_TYPES = {
  ".css": "text/css; charset=utf-8",
  ".html": "text/html; charset=utf-8",
  ".js": "application/javascript; charset=utf-8",
  ".png": "image/png",
  ".svg": "image/svg+xml; charset=utf-8"
};

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const pathname = normalizePath(url.pathname);
    const assetPath = ASSET_MANIFEST[pathname];

    if (!assetPath) {
      return new Response("Not found", { status: 404 });
    }

    const asset = await env.ASSETS.fetch(new URL(assetPath, "https://assets.local/"));
    const headers = new Headers(asset.headers);
    headers.set("Content-Type", contentType(assetPath));
    headers.set("Cache-Control", cacheControl(assetPath));

    return new Response(asset.body, {
      status: asset.status,
      headers
    });
  }
};

function normalizePath(pathname) {
  if (pathname !== "/" && pathname.endsWith("/")) {
    return pathname.slice(0, -1) + ".html";
  }
  return pathname;
}

function contentType(assetPath) {
  const extension = assetPath.slice(assetPath.lastIndexOf("."));
  return MIME_TYPES[extension] || "application/octet-stream";
}

function cacheControl(assetPath) {
  if (assetPath.endsWith(".html")) {
    return "public, max-age=60";
  }
  return "public, max-age=31536000, immutable";
}
