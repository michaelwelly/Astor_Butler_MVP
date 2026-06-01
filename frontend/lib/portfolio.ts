export type DirectionId = "events" | "reels" | "commercials";

export type PortfolioCase = {
  id: string;
  direction: DirectionId;
  category: string;
  title: string;
  kicker: string;
  year: string;
  duration: string;
  accent: string;
  image: string;
  video?: string;
  statement: string;
};

export type Direction = {
  id: DirectionId;
  index: string;
  title: string;
  shortTitle: string;
  description: string;
};

export const directions: Direction[] = [
  {
    id: "events",
    index: "01",
    title: "Event Stories",
    shortTitle: "Events",
    description: "Atmosphere with a pulse. Live moments shaped into stories that outlast the night.",
  },
  {
    id: "reels",
    index: "02",
    title: "Reels & Product Content",
    shortTitle: "Reels",
    description: "Fast, tactile, precise. A visual language built for products people want to touch.",
  },
  {
    id: "commercials",
    index: "03",
    title: "Commercials",
    shortTitle: "Films",
    description: "Cinematic campaigns with an editorial point of view and a clear business purpose.",
  },
];

// Replace these preview sources with MinIO public URLs when the three curated
// local samples are uploaded to astor-media/raw/.
export const portfolioCases: PortfolioCase[] = [
  {
    id: "afterglow",
    direction: "events",
    category: "Event Stories",
    title: "Afterglow",
    kicker: "A night in motion",
    year: "2026",
    duration: "00:48",
    accent: "#d76f49",
    image:
      "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?auto=format&fit=crop&w=1600&q=85",
    video:
      "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4",
    statement: "An electric portrait of the seconds people wish they could hold onto.",
  },
  {
    id: "still-life",
    direction: "reels",
    category: "Reels & Product Content",
    title: "Still / Alive",
    kicker: "Objects with appetite",
    year: "2026",
    duration: "00:31",
    accent: "#bf9b77",
    image:
      "https://images.unsplash.com/photo-1615634260167-c8cdede054de?auto=format&fit=crop&w=1600&q=85",
    video:
      "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4",
    statement: "A small object, treated like a lead character.",
  },
  {
    id: "night-drive",
    direction: "commercials",
    category: "Commercials",
    title: "Night Drive",
    kicker: "City as a character",
    year: "2026",
    duration: "01:02",
    accent: "#8b94b4",
    image:
      "https://images.unsplash.com/photo-1519608487953-e999c86e7455?auto=format&fit=crop&w=1600&q=85",
    video:
      "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4",
    statement: "A campaign film where momentum does most of the talking.",
  },
];

export const servicePackages = [
  {
    number: "01",
    title: "Reels / Product A",
    price: "85 000 RUB",
    description: "10 reels, idea and structure, shooting, editing and one revision.",
  },
  {
    number: "02",
    title: "Reels / Product B",
    price: "65 000 RUB",
    description: "A focused production day with shooting, editing and one revision.",
  },
  {
    number: "03",
    title: "Event Stories",
    price: "from 22 000 RUB",
    description: "Live atmosphere coverage from two hours. Every next hour is 11 000 RUB.",
  },
  {
    number: "04",
    title: "Podcast",
    price: "68 000 RUB",
    description: "A complete podcast episode up to one hour, shaped for a polished release.",
  },
];
