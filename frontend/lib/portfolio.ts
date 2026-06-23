export type DirectionId = "events" | "reels" | "commercials";

/**
 * Optional override fields let an editor pin contract-aligned metadata
 * (FRONTEND_BACKEND_CONTRACTS.md §3) per item. When omitted, the catalog
 * adapter in lib/video-catalog.ts derives deterministic defaults.
 */
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
  featured?: boolean;
  // ── Contract overrides (optional) ──────────────────────────────────────
  videoId?: string;
  slug?: string;
  shortDescription?: string;
  tags?: string[];
  orientation?: "portrait" | "landscape";
  status?: "READY" | "DRAFT" | "ARCHIVED";
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
    title: "Ивенты",
    shortTitle: "Ивенты",
    description: "Атмосфера с пульсом. Живые моменты, превращённые в истории, которые переживают ночь.",
  },
  {
    id: "reels",
    index: "02",
    title: "Рилсы и продукт",
    shortTitle: "Рилсы",
    description: "Быстро, тактильно, точно. Визуальный язык для продуктов, которые хочется потрогать.",
  },
  {
    id: "commercials",
    index: "03",
    title: "Реклама",
    shortTitle: "Реклама",
    description: "Кинематографичные кампании с редакционной точкой зрения и чёткой бизнес-задачей.",
  },
];

// Videos rotate across 3 available placeholders
const V1 = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4";
const V2 = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4";
const V3 = "https://storage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4";

export const portfolioCases: PortfolioCase[] = [
  // ─── Event Stories ───────────────────────────────────────────────────────
  {
    id: "segreto",
    direction: "events",
    category: "Ивенты",
    title: "Segreto",
    kicker: "Ресторан в кадре",
    year: "2025",
    duration: "01:40",
    accent: "#d76f49",
    image: "/portfolio/segreto.jpg",
    video: "/portfolio/segreto_hero.mp4",
    statement: "Атмосфера ресторана — живая, тёмная, кинематографичная.",
    featured: true,
  },
  {
    id: "wine-story",
    direction: "events",
    category: "Ивенты",
    title: "Wine Story",
    kicker: "Вино и свет",
    year: "2025",
    duration: "00:45",
    accent: "#c4845a",
    image: "/portfolio/food_5s.jpg",
    video: V2,
    statement: "Момент налитого бокала — гастрономический кинематограф.",
  },
  {
    id: "golden-hour",
    direction: "events",
    category: "Ивенты",
    title: "Golden Hour",
    kicker: "Свет перед закатом",
    year: "2025",
    duration: "00:54",
    accent: "#c9a84c",
    image: "/portfolio/food_8s.jpg",
    video: V3,
    statement: "Те сорок минут, когда всё становится золотым.",
  },
  {
    id: "the-crowd",
    direction: "events",
    category: "Ивенты",
    title: "The Crowd",
    kicker: "Тысяча лиц",
    year: "2025",
    duration: "00:37",
    accent: "#7a6fa8",
    image: "https://images.unsplash.com/photo-1429514513361-8a632ff3a549?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Один фестиваль, снятый глазами тех, кто был внутри.",
  },
  {
    id: "after-rain",
    direction: "events",
    category: "Ивенты",
    title: "After Rain",
    kicker: "Мокрый асфальт, неон",
    year: "2025",
    duration: "00:43",
    accent: "#5a8fa8",
    image: "https://images.unsplash.com/photo-1501281668745-f7f57925c3b4?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "Открытая сцена под ливнем. Никто не ушёл.",
  },
  {
    id: "opening-night",
    direction: "events",
    category: "Ивенты",
    title: "Opening Night",
    kicker: "Первый вечер",
    year: "2024",
    duration: "01:05",
    accent: "#d4a574",
    image: "https://images.unsplash.com/photo-1478147427282-58a87a433130?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Волнение за кулисами за секунду до начала.",
  },
  {
    id: "neon-night",
    direction: "events",
    category: "Ивенты",
    title: "Neon Night",
    kicker: "Свечение города",
    year: "2024",
    duration: "00:51",
    accent: "#e066a0",
    image: "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Ночь, когда город стал декорацией.",
  },
  {
    id: "studio-a",
    direction: "events",
    category: "Ивенты",
    title: "Studio A",
    kicker: "Живая запись",
    year: "2024",
    duration: "02:14",
    accent: "#8fa870",
    image: "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "Один дубль. Без монтажа. Только момент.",
  },
  {
    id: "final-dance",
    direction: "events",
    category: "Ивенты",
    title: "Final Dance",
    kicker: "Последний трек",
    year: "2024",
    duration: "01:28",
    accent: "#a87060",
    image: "https://images.unsplash.com/photo-1504609813442-a8924e83f76e?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Финал сета. Та секунда тишины до аплодисментов.",
  },
  {
    id: "the-room",
    direction: "events",
    category: "Ивенты",
    title: "The Room",
    kicker: "Пространство без стен",
    year: "2024",
    duration: "00:59",
    accent: "#6080a8",
    image: "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Архитектура как соучастник истории.",
  },

  // ─── Reels & Product Content ──────────────────────────────────────────────
  {
    id: "cristal-pour",
    direction: "reels",
    category: "Рилсы",
    title: "Cristal",
    kicker: "Шампанское во льду",
    year: "2025",
    duration: "00:31",
    accent: "#c8940a",
    image: "/portfolio/food_12s.jpg",
    video: V2,
    statement: "Louis Roederer. Продукт снятый как ювелирное изделие.",
    featured: true,
  },
  {
    id: "texture-01",
    direction: "reels",
    category: "Рилсы",
    title: "Texture 01",
    kicker: "Материал крупным планом",
    year: "2025",
    duration: "00:22",
    accent: "#a89070",
    image: "/portfolio/food_22s.jpg",
    video: V3,
    statement: "Кожа, дерево, металл — всё что можно почувствовать через экран.",
  },
  {
    id: "pour-it",
    direction: "reels",
    category: "Рилсы",
    title: "Pour It",
    kicker: "Жидкость в движении",
    year: "2025",
    duration: "00:18",
    accent: "#d4a030",
    image: "/portfolio/food_35s.jpg",
    video: V1,
    statement: "Один продукт, снятый так, будто это поэзия.",
  },
  {
    id: "drop-zone",
    direction: "reels",
    category: "Рилсы",
    title: "Drop Zone",
    kicker: "Падение и свет",
    year: "2025",
    duration: "00:25",
    accent: "#607090",
    image: "https://images.unsplash.com/photo-1491553895911-0055eca6402d?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "Высокоскоростная съёмка превращает физику в эстетику.",
  },
  {
    id: "unbox",
    direction: "reels",
    category: "Рилсы",
    title: "Unbox",
    kicker: "Первый раз",
    year: "2025",
    duration: "00:29",
    accent: "#c87050",
    image: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Момент открытия упаковки как ритуал.",
  },
  {
    id: "soft-hands",
    direction: "reels",
    category: "Рилсы",
    title: "Soft Hands",
    kicker: "Прикосновение",
    year: "2024",
    duration: "00:33",
    accent: "#d4b090",
    image: "https://images.unsplash.com/photo-1556228578-0d85b1a4d571?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Руки как инструмент рассказа о продукте.",
  },
  {
    id: "cold-brew",
    direction: "reels",
    category: "Рилсы",
    title: "Cold Brew",
    kicker: "Медленно и точно",
    year: "2024",
    duration: "00:27",
    accent: "#304050",
    image: "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "Процесс приготовления, снятый как документальное кино.",
  },
  {
    id: "reflect",
    direction: "reels",
    category: "Рилсы",
    title: "Reflect",
    kicker: "Игра отражений",
    year: "2024",
    duration: "00:21",
    accent: "#7090b0",
    image: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Один объект, бесконечное зеркало.",
  },
  {
    id: "in-frame",
    direction: "reels",
    category: "Рилсы",
    title: "In Frame",
    kicker: "Кадр внутри кадра",
    year: "2024",
    duration: "00:35",
    accent: "#907060",
    image: "https://images.unsplash.com/photo-1542038784456-1ea8e935640e?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Композиция как аргумент в пользу бренда.",
  },
  {
    id: "surface",
    direction: "reels",
    category: "Рилсы",
    title: "Surface",
    kicker: "Поверхность и свет",
    year: "2024",
    duration: "00:19",
    accent: "#b0a080",
    image: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "Свет касается продукта. Продукт становится желанием.",
  },

  // ─── Commercials ──────────────────────────────────────────────────────────
  {
    id: "night-drive",
    direction: "commercials",
    category: "Реклама",
    title: "Night Drive",
    kicker: "Город как персонаж",
    year: "2026",
    duration: "01:02",
    accent: "#8b94b4",
    image: "/portfolio/food_18s.jpg",
    video: V3,
    statement: "Рекламный фильм, где импульс говорит сам за себя.",
    featured: true,
  },
  {
    id: "momentum",
    direction: "commercials",
    category: "Реклама",
    title: "Momentum",
    kicker: "Скорость бренда",
    year: "2025",
    duration: "00:45",
    accent: "#6070a0",
    image: "/portfolio/food_28s.jpg",
    video: V1,
    statement: "Бренд движется вперёд. Камера едва успевает.",
  },
  {
    id: "quiet-power",
    direction: "commercials",
    category: "Реклама",
    title: "Quiet Power",
    kicker: "Тишина как сила",
    year: "2025",
    duration: "01:15",
    accent: "#404050",
    image: "/portfolio/food_42s.jpg",
    video: V2,
    statement: "Не каждый бренд кричит. Некоторые просто существуют.",
  },
  {
    id: "origin",
    direction: "commercials",
    category: "Реклама",
    title: "Origin",
    kicker: "История с начала",
    year: "2025",
    duration: "02:00",
    accent: "#806050",
    image: "https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Откуда берётся то, что ты покупаешь.",
  },
  {
    id: "the-pitch",
    direction: "commercials",
    category: "Реклама",
    title: "The Pitch",
    kicker: "Один шанс",
    year: "2025",
    duration: "00:50",
    accent: "#5080a0",
    image: "https://images.unsplash.com/photo-1497366216548-37526070297c?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Кампания, которую нужно было снять за один день.",
  },
  {
    id: "season",
    direction: "commercials",
    category: "Реклама",
    title: "Season",
    kicker: "Коллекция и время",
    year: "2024",
    duration: "01:30",
    accent: "#a07050",
    image: "https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "Новая коллекция рассказывает о сезоне лучше любого слогана.",
  },
  {
    id: "proof",
    direction: "commercials",
    category: "Реклама",
    title: "Proof",
    kicker: "Показать, не рассказать",
    year: "2024",
    duration: "00:40",
    accent: "#708090",
    image: "https://images.unsplash.com/photo-1556742049-0cfed4f6a45d?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Демонстрация продукта как кинематографический аргумент.",
  },
  {
    id: "last-mile",
    direction: "commercials",
    category: "Реклама",
    title: "Last Mile",
    kicker: "Финишная прямая",
    year: "2024",
    duration: "00:55",
    accent: "#c04040",
    image: "https://images.unsplash.com/photo-1530549387789-4c1017266635?auto=format&fit=crop&w=800&q=80",
    video: V1,
    statement: "Спортивный бренд. Всё решает последний шаг.",
  },
  {
    id: "blueprint",
    direction: "commercials",
    category: "Реклама",
    title: "Blueprint",
    kicker: "Архитектура идеи",
    year: "2024",
    duration: "01:10",
    accent: "#3060a0",
    image: "https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=800&q=80",
    video: V2,
    statement: "От чертежа до финального кадра — история о создании.",
  },
  {
    id: "chapter-one",
    direction: "commercials",
    category: "Реклама",
    title: "Chapter One",
    kicker: "Начало марки",
    year: "2024",
    duration: "01:45",
    accent: "#604080",
    image: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=800&q=80",
    video: V3,
    statement: "Лонч-фильм для бренда, который только начинается.",
  },
];

export function getByDirection(dir: DirectionId, limit?: number): PortfolioCase[] {
  const filtered = portfolioCases.filter((c) => c.direction === dir);
  return limit ? filtered.slice(0, limit) : filtered;
}

export function getFeatured(): PortfolioCase[] {
  return portfolioCases.filter((c) => c.featured === true);
}

export const servicePackages = [
  {
    number: "01",
    title: "Рилсы / Продукт A",
    price: "85 000 ₽",
    description: "10 рилсов: идея и структура, съёмка, монтаж, одна правка.",
  },
  {
    number: "02",
    title: "Рилсы / Продукт B",
    price: "65 000 ₽",
    description: "Сфокусированный съёмочный день: съёмка, монтаж, одна правка.",
  },
  {
    number: "03",
    title: "Event Stories",
    price: "от 22 000 ₽",
    description: "Съёмка атмосферы мероприятия от двух часов. Каждый следующий час — 11 000 ₽.",
  },
  {
    number: "04",
    title: "Подкаст",
    price: "68 000 ₽",
    description: "Полноценный выпуск подкаста до одного часа, готовый к релизу.",
  },
];
