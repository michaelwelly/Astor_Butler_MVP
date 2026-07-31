#!/usr/bin/env node
/**
 * Publish selected C3AG archive videos to Yandex Object Storage.
 *
 * The source of truth remains frontend/data/videos.json. This script resolves
 * a Yandex.Disk archive path, downloads the master to a temp dir, produces:
 *   - web.mp4: site-ready H.264 rendition;
 *   - preview.mp4: short autoplay-safe card teaser;
 *   - poster.jpg: card/player still;
 * uploads those files to Object Storage and patches the JSON record with
 * objectKey, previewObjectKey and posterObjectKey.
 */

import { createWriteStream, readFileSync, writeFileSync } from "node:fs";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { dirname, extname, join, resolve } from "node:path";
import { fileURLToPath } from "node:url";
import { Readable } from "node:stream";
import { finished } from "node:stream/promises";
import { execFileSync } from "node:child_process";

const __dirname = dirname(fileURLToPath(import.meta.url));
const dbPath = resolve(__dirname, "..", "data", "videos.json");
const API = "https://cloud-api.yandex.net/v1/disk/public";

const args = new Map();
const flags = new Set();
for (let i = 2; i < process.argv.length; i += 1) {
  const arg = process.argv[i];
  if (!arg.startsWith("--")) continue;
  const key = arg.slice(2);
  const next = process.argv[i + 1];
  if (!next || next.startsWith("--")) flags.add(key);
  else {
    args.set(key, next);
    i += 1;
  }
}

const bucket = args.get("bucket") ?? process.env.C3_MEDIA_BUCKET ?? "c3ag-media";
const publicKey = args.get("public-key") ?? process.env.NEXT_PUBLIC_YADISK_PUBLIC_KEY ?? "https://disk.yandex.ru/d/oopTdDN0CTuIig";
const yc = args.get("yc") ?? process.env.YC_BIN ?? "/Users/michaelwelly/yandex-cloud/bin/yc";
const prefix = stripSlashes(args.get("prefix") ?? "content/c3ag/videos");
const posterPrefix = stripSlashes(args.get("poster-prefix") ?? "content/c3ag/posters");
const previewPrefix = stripSlashes(args.get("preview-prefix") ?? "content/c3ag/previews");
const limit = numberArg("limit", 0);
const maxMb = numberArg("max-mb", 0);
const dryRun = flags.has("dry-run");
const force = flags.has("force");
const ids = splitArg("ids");
const directions = splitArg("direction");

const records = JSON.parse(readFileSync(dbPath, "utf8"));
let selected = records
  .filter((r) => r.sourcePath?.startsWith("/"))
  .filter((r) => !ids.length || ids.includes(r.id))
  .filter((r) => !directions.length || directions.includes(r.direction))
  .filter((r) => !maxMb || Number(r.sizeBytes ?? 0) <= maxMb * 1024 * 1024)
  .filter((r) => force || !r.objectKey || !r.posterObjectKey || !r.previewObjectKey)
  .sort((a, b) => Number(a.sizeBytes ?? 0) - Number(b.sizeBytes ?? 0));

if (limit > 0) selected = selected.slice(0, limit);
if (!selected.length) {
  console.log("No matching records to publish.");
  process.exit(0);
}

console.log(JSON.stringify({
  dryRun,
  bucket,
  selected: selected.map((r) => ({
    id: r.id,
    title: r.title,
    direction: r.direction,
    sizeMb: Math.round(Number(r.sizeBytes ?? 0) / 1024 / 1024),
    sourcePath: r.sourcePath,
  })),
}, null, 2));

if (dryRun) process.exit(0);

const tempRoot = await mkdtemp(join(tmpdir(), "c3ag-media-"));
try {
  for (const record of selected) {
    await publishRecord(record);
    writeFileSync(dbPath, JSON.stringify(records, null, 2) + "\n");
  }
} finally {
  await rm(tempRoot, { recursive: true, force: true });
}

async function publishRecord(record) {
  const slug = safeKey(record.id);
  const sourceExt = extname(record.sourcePath).toLowerCase() || ".mp4";
  const input = join(tempRoot, `${slug}${sourceExt}`);
  const web = join(tempRoot, `${slug}-web.mp4`);
  const preview = join(tempRoot, `${slug}-preview.mp4`);
  const poster = join(tempRoot, `${slug}-poster.jpg`);

  console.log(`\n[${record.id}] resolving ${record.sourcePath}`);
  const href = await resolveYadiskDownload(record.sourcePath);
  console.log(`[${record.id}] downloading master`);
  await download(href, input);

  console.log(`[${record.id}] rendering web mp4`);
  run("ffmpeg", [
    "-hide_banner", "-loglevel", "error", "-y",
    "-i", input,
    "-vf", "scale=w='if(gte(iw,ih),1280,-2)':h='if(gte(iw,ih),-2,1280)':force_original_aspect_ratio=decrease,scale='trunc(iw/2)*2':'trunc(ih/2)*2',format=yuv420p",
    "-c:v", "libx264",
    "-preset", "veryfast",
    "-crf", "27",
    "-movflags", "+faststart",
    "-an",
    web,
  ]);

  console.log(`[${record.id}] rendering preview mp4`);
  run("ffmpeg", [
    "-hide_banner", "-loglevel", "error", "-y",
    "-i", input,
    "-t", "6",
    "-vf", "scale=w='if(gte(iw,ih),720,-2)':h='if(gte(iw,ih),-2,720)':force_original_aspect_ratio=decrease,scale='trunc(iw/2)*2':'trunc(ih/2)*2',format=yuv420p",
    "-c:v", "libx264",
    "-preset", "veryfast",
    "-crf", "32",
    "-movflags", "+faststart",
    "-an",
    preview,
  ]);

  console.log(`[${record.id}] rendering poster jpg`);
  run("ffmpeg", [
    "-hide_banner", "-loglevel", "error", "-y",
    "-ss", "1",
    "-i", input,
    "-frames:v", "1",
    "-vf", "scale=w='if(gte(iw,ih),960,-2)':h='if(gte(iw,ih),-2,960)':force_original_aspect_ratio=decrease",
    "-q:v", "3",
    poster,
  ]);

  const objectKey = `${prefix}/${slug}/web.mp4`;
  const previewObjectKey = `${previewPrefix}/${slug}.mp4`;
  const posterObjectKey = `${posterPrefix}/${slug}.jpg`;
  console.log(`[${record.id}] uploading to ${bucket}`);
  upload(objectKey, web, "video/mp4", "public, max-age=31536000, immutable");
  upload(previewObjectKey, preview, "video/mp4", "public, max-age=31536000, immutable");
  upload(posterObjectKey, poster, "image/jpeg", "public, max-age=31536000, immutable");

  record.objectKey = objectKey;
  record.previewObjectKey = previewObjectKey;
  record.posterObjectKey = posterObjectKey;
  record.adaptedUrl = `s3:${objectKey}`;
  record.previewUrl = `s3:${previewObjectKey}`;
  record.poster = `s3:${posterObjectKey}`;
  record.originalUrl = `yadisk:${record.sourcePath}`;
  record.featured = true;
  if (!record.duration?.trim()) record.duration = probeDuration(web);
}

async function resolveYadiskDownload(path) {
  const res = await fetch(`${API}/resources/download?${new URLSearchParams({ public_key: publicKey, path })}`);
  if (!res.ok) throw new Error(`Yandex.Disk resolve failed: ${res.status} ${await res.text()}`);
  const json = await res.json();
  if (!json.href) throw new Error(`Yandex.Disk resolve returned no href for ${path}`);
  return json.href;
}

async function download(url, path) {
  const res = await fetch(url);
  if (!res.ok || !res.body) throw new Error(`Download failed: ${res.status}`);
  await finished(Readable.fromWeb(res.body).pipe(createWriteStream(path)));
}

function upload(key, body, contentType, cacheControl) {
  run(yc, [
    "storage", "s3api", "put-object",
    "--bucket", bucket,
    "--key", key,
    "--body", body,
    "--content-type", contentType,
    "--cache-control", cacheControl,
    "--acl", "public-read",
  ]);
}

function probeDuration(file) {
  try {
    const out = execFileSync("ffprobe", [
      "-v", "error",
      "-show_entries", "format=duration",
      "-of", "default=noprint_wrappers=1:nokey=1",
      file,
    ], { encoding: "utf8" }).trim();
    const seconds = Math.max(0, Math.round(Number(out)));
    if (!seconds) return "";
    return `${Math.floor(seconds / 60).toString().padStart(2, "0")}:${(seconds % 60).toString().padStart(2, "0")}`;
  } catch {
    return "";
  }
}

function run(command, commandArgs) {
  execFileSync(command, commandArgs, { stdio: "inherit" });
}

function numberArg(name, fallback) {
  const value = Number(args.get(name) ?? fallback);
  return Number.isFinite(value) ? value : fallback;
}

function splitArg(name) {
  return (args.get(name) ?? "")
    .split(",")
    .map((s) => s.trim())
    .filter(Boolean);
}

function stripSlashes(value) {
  return value.replace(/^\/+|\/+$/g, "");
}

function safeKey(value) {
  return String(value)
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-zA-Z0-9._-]+/g, "-")
    .replace(/^-+|-+$/g, "")
    .toLowerCase();
}
