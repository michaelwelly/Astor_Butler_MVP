/**
 * Point records in data/videos.json at the public Yandex.Disk archive.
 *
 *   node scripts/yadisk-enable.mjs            # every record
 *   node scripts/yadisk-enable.mjs --max-mb 60
 *   node scripts/yadisk-enable.mjs --direction reels
 *   node scripts/yadisk-enable.mjs --off      # clear src/poster again
 *
 * It writes the archive *path*, not a URL: Yandex only issues short-lived
 * signed links, so /api/yadisk resolves one per request. Records that already
 * have a hand-set src (a real CDN rendition) are left alone — those are better
 * than the master and must win.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { fileURLToPath } from "node:url";
import { dirname, join } from "node:path";

const here = dirname(fileURLToPath(import.meta.url));
const DB = join(here, "..", "data", "videos.json");

const args = process.argv.slice(2);
const flag = (name) => {
  const i = args.indexOf(name);
  return i === -1 ? null : args[i + 1] ?? true;
};
const off = args.includes("--off");
const maxMb = Number(flag("--max-mb")) || Infinity;
const direction = flag("--direction");

const db = JSON.parse(readFileSync(DB, "utf8"));
let touched = 0;
let skipped = 0;

for (const rec of db) {
  if (direction && rec.direction !== direction) continue;
  if (Number(rec.sizeBytes) / 1e6 > maxMb) continue;

  if (off) {
    if (String(rec.src).startsWith("yadisk")) {
      rec.src = "";
      rec.poster = "";
      touched++;
    }
    continue;
  }

  // Never clobber a hosted rendition someone deliberately set.
  if (rec.src && !String(rec.src).startsWith("yadisk")) {
    skipped++;
    continue;
  }
  rec.src = `yadisk:${rec.sourcePath}`;
  rec.poster = `yadisk-poster:${rec.sourcePath}`;
  touched++;
}

writeFileSync(DB, JSON.stringify(db, null, 2) + "\n", "utf8");

const live = db.filter((r) => String(r.src).trim()).length;
console.log(
  `${off ? "cleared" : "linked"} ${touched} record(s)` +
    (skipped ? `, kept ${skipped} with a hosted URL` : "") +
    ` — ${live}/${db.length} now live`,
);
