import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: true,
  output: "standalone",
  async redirects() {
    return [
      { source: "/films", destination: "/film", permanent: true },
      { source: "/podcasts", destination: "/podcast", permanent: true },
      { source: "/advertising", destination: "/reclama", permanent: true },
      { source: "/reportage", destination: "/events", permanent: true },
    ];
  },
};

export default nextConfig;
