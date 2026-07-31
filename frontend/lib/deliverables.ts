/**
 * Deliverables validator for the video-render TZ (TZ_video_montazh).
 *
 * Pure, UI-agnostic logic used by the /studio editor tool. Two checks:
 *  - parseFilename(): validates one name against §8 (scheme + charset);
 *  - completeness(): groups valid files by slug and reports the missing
 *    format×resolution / poster combinations per §2–§3.
 *
 * Naming scheme (§8):  <project>-<name>-<type>-<resolution>.<ext>
 *   lowercase latin + digits + hyphens, no spaces / cyrillic / underscores.
 *   type ∈ hero | catalog | poster · resolution ∈ 720 | 1080 (hero: +1440)
 *   video ext ∈ mp4 | webm · poster ext ∈ webp | jpg
 */

export type FileKind = "hero" | "catalog" | "poster";
export const PROJECT_DEFAULT = "c3flex";

export type ParsedFile = {
  raw: string;
  ok: boolean;
  project?: string;
  name?: string;
  type?: FileKind;
  resolution?: number;
  ext?: string;
  issues: string[];
};

const TYPES = new Set<FileKind>(["hero", "catalog", "poster"]);
const VIDEO_EXT = new Set(["mp4", "webm"]);
const POSTER_EXT = new Set(["webp", "jpg"]);
const RESOLUTIONS = new Set([720, 1080, 1440]);
const REQUIRED_RES = [1080, 720]; // desktop + mobile (§3)

function isKind(v: string): v is FileKind {
  return TYPES.has(v as FileKind);
}

export function parseFilename(input: string, project = PROJECT_DEFAULT): ParsedFile {
  const raw = input.trim();
  const issues: string[] = [];
  if (!raw) return { raw: input, ok: false, issues: ["пустая строка"] };

  // Charset (§8): lowercase latin, digits, hyphens only.
  if (/[A-Z]/.test(raw)) issues.push("есть заглавные — только нижний регистр");
  if (/\s/.test(raw)) issues.push("есть пробелы");
  if (/[а-яё]/i.test(raw)) issues.push("есть кириллица");
  if (/_/.test(raw)) issues.push("подчёркивание — нужен дефис");

  const dot = raw.lastIndexOf(".");
  if (dot < 1) {
    issues.push("нет расширения");
    return { raw, ok: false, issues };
  }
  const base = raw.slice(0, dot);
  const ext = raw.slice(dot + 1).toLowerCase();
  const tokens = base.split("-").filter(Boolean);

  if (tokens.length < 4) {
    issues.push("схема <проект>-<название>-<тип>-<разрешение> — не хватает частей");
    return { raw, ok: false, ext, issues };
  }

  // Parse from the right so multi-word slugs (wine-story) still work.
  const proj = tokens[0];
  const resTok = tokens[tokens.length - 1];
  const typeTok = tokens[tokens.length - 2];
  const name = tokens.slice(1, -2).join("-");
  const resolution = /^\d+$/.test(resTok) ? parseInt(resTok, 10) : undefined;
  const type = isKind(typeTok) ? typeTok : undefined;

  if (proj !== project) issues.push(`проект «${proj}» ≠ ожидаемому «${project}»`);
  if (!name) issues.push("пустое <название>");
  if (!type) issues.push(`тип «${typeTok}» — ожидается hero / catalog / poster`);
  if (resolution === undefined || !RESOLUTIONS.has(resolution)) {
    issues.push(`разрешение «${resTok}» — ожидается 720 / 1080 (hero: +1440)`);
  }
  if (type === "poster" && !POSTER_EXT.has(ext)) {
    issues.push(`постер должен быть webp / jpg, а не ${ext}`);
  }
  if ((type === "hero" || type === "catalog") && !VIDEO_EXT.has(ext)) {
    issues.push(`видео должно быть mp4 / webm, а не ${ext}`);
  }

  return { raw, ok: issues.length === 0, project: proj, name, type, resolution, ext, issues };
}

export type SlugReport = {
  slug: string;
  present: string[];
  missing: string[];
  complete: boolean;
};

/** Group valid files by slug and report missing required combinations. */
export function completeness(files: ParsedFile[]): SlugReport[] {
  const bySlug = new Map<string, ParsedFile[]>();
  for (const f of files) {
    if (!f.ok || !f.name || !f.type || f.resolution === undefined || !f.ext) continue;
    const list = bySlug.get(f.name) ?? [];
    list.push(f);
    bySlug.set(f.name, list);
  }

  const reports: SlugReport[] = [];
  for (const [slug, list] of bySlug) {
    const have = new Set(list.map((f) => `${f.type}-${f.resolution}-${f.ext}`));
    const videoTypes = new Set(
      list.filter((f) => f.type === "hero" || f.type === "catalog").map((f) => f.type as FileKind),
    );
    const hasPoster = list.some((f) => f.type === "poster");
    const missing: string[] = [];

    for (const type of videoTypes) {
      for (const res of REQUIRED_RES) {
        for (const ext of ["webm", "mp4"]) {
          if (!have.has(`${type}-${res}-${ext}`)) missing.push(`${type} ${res} ${ext}`);
        }
      }
    }
    if (hasPoster) {
      for (const res of REQUIRED_RES) {
        for (const ext of ["webp", "jpg"]) {
          if (!have.has(`poster-${res}-${ext}`)) missing.push(`poster ${res} ${ext}`);
        }
      }
    }

    reports.push({
      slug,
      present: [...have].map((s) => s.replace(/-/g, " ")).sort(),
      missing,
      complete: missing.length === 0,
    });
  }
  return reports.sort((a, b) => a.slug.localeCompare(b.slug));
}

/** Auto-derivable subset of the §10 acceptance checklist (the rest is manual QA). */
export function autoChecklist(files: ParsedFile[], reports: SlugReport[]) {
  const anyFiles = files.length > 0;
  const allNamed = anyFiles && files.every((f) => f.ok);
  const anyComplete = reports.length > 0;
  const bothFormats = anyComplete && reports.every((r) => !r.missing.some((m) => /mp4|webm/.test(m)));
  const bothRes = anyComplete && reports.every((r) => !r.missing.some((m) => /1080|720/.test(m)));
  const posters = reports.some((r) => r.present.some((p) => p.startsWith("poster")));
  const posterPair =
    posters && reports.every((r) => !r.missing.some((m) => m.startsWith("poster")));
  return { allNamed, bothFormats, bothRes, posterPair };
}
