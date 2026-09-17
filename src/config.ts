/**
 * src/config.ts — Single Source of Truth (SSOT)
 *
 * All brand colors, fonts, scene durations, frame specs, and asset paths
 * are declared here. Every scene/component must import from this file;
 * no magic numbers or hard-coded paths should appear elsewhere.
 *
 * Values are derived from:
 *   - brief.md (design Brief — downstream唯一输入)
 *   - workbench.html (visual spec / palette)
 *   - verified_data.json (verified arithmetic specs)
 */

// ─── Video / Canvas Spec (brief.md §2.1, verified_data D2–D3) ────────────────
export const VIDEO = {
  /** 9:16 vertical — width 1080, height 1920 (verified_data D2) */
  width: 1080,
  height: 1920,
  /** 30 fps (verified_data D3: 30 × 3 = 90 frames) */
  fps: 30,
  /** Total duration 3.00 s = 90 frames (verified_data D1) */
  totalDurationSeconds: 3.0,
  totalFrames: 90,
  /** MP4 H.264 + AAC (brief.md §2.1) */
  format: 'mp4' as const,
  codec: 'h264' as const,
} satisfies VideoSpec;

// ─── Scene Durations (brief.md §1, verified_data D1) ────────────────────────
/** S1 入画定位  0.00–0.50s = 15 frames */
/** S2 张嘴叫    0.50–2.50s = 60 frames */
/** S3 收尾定格  2.50–3.00s = 15 frames */
export const SCENES = {
  S1: { id: 'S1', name: '入画定位', startSeconds: 0.0, durationSeconds: 0.5, startFrame: 0, durationFrames: 15 },
  S2: { id: 'S2', name: '张嘴叫',  startSeconds: 0.5, durationSeconds: 2.0, startFrame: 15, durationFrames: 60 },
  S3: { id: 'S3', name: '收尾定格', startSeconds: 2.5, durationSeconds: 0.5, startFrame: 75, durationFrames: 15 },
} as const;

/** Array form for iteration; total must equal VIDEO.totalFrames (90). */
export const SCENE_SEQUENCE = Object.values(SCENES);

// ─── Brand Palette (workbench.html CSS variables) ───────────────────────────
export const COLORS = {
  // Scene accent colors (workbench --s1/--s2/--s3)
  sceneS1: '#4a9eff',   // 入画定位 — blue
  sceneS2: '#ff7a7a',   // 张嘴叫 — coral red
  sceneS3: '#b58cff',    // 收尾定格 — lavender
  // UI / structural
  accent: '#5ec8ff',
  accent2: '#ffd166',
  background: '#0f1115',
  panel: '#171a21',
  panelAlt: '#1d2129',
  ink: '#e8eaed',
  sub: '#9aa0a8',
  line: '#2a2f38',
  ok: '#7ee29a',
  warn: '#ffb454',
} as const;

// ─── Subject / Color-Temp (brief.md §2.2) ───────────────────────────────────
export const SUBJECT = {
  /** 田园幼犬 (中华田园犬幼崽), ~2–3 月龄 — brief.md §4.1 / §5 */
  breed: '田园幼犬',
  ageMonths: '2-3',
  /** Color temperature ~5200 K, warm-neutral (brief.md §2.2, verified_data D5) */
  colorTempK: 5200,
  /** catchlight position: pupil upper-left (brief.md §2.2) */
  catchlight: { x: 0.42, y: 0.38 } as const,
} as const;

// ─── Safe Zones (brief.md §2.1, verified_data D6 — design convention) ──────
export const SAFE_ZONE = {
  /** Top status-bar reserve 120 px (6.25 %) */
  topPx: 120,
  topPct: 6.25,
  /** Bottom social-UI reserve 240 px (12.5 %) */
  bottomPx: 240,
  bottomPct: 12.5,
} as const;

// ─── Audio (brief.md §4.2 / §2.1) ────────────────────────────────────────────
export const AUDIO = {
  /** Real puppy bark, 2–3 short bursts, 0.5–2.5 s window (brief.md §4.2) */
  barkAsset: 'public/audio/puppy-bark.mp3',
  /** Sync tolerance ≤ 1 frame = 33.33 ms (brief.md §7, verified_data D4) */
  syncToleranceMs: 33.33,
  /** No BGM, no ambient env sound (brief.md §4.2 / §5) */
  bgm: false,
  ambient: false,
} as const;

// ─── Asset Paths ─────────────────────────────────────────────────────────────
export const ASSETS = {
  /** Verified data source (verified_data.json) — imported by data-class scenes */
  verifiedData: 'verified_data.json',
  /** Visual subject (placeholder — puppy image to be supplied downstream) */
  puppyImage: 'public/images/puppy-subject.png',
  /** Background reference (indoor, cream/soft wood — brief.md §2.3) */
  backgroundRef: 'public/images/indoor-soft-light.jpg',
} as const;

// ─── Typography (skeleton — brief.md: no subtitles/brand text in final video) ──
export const FONTS = {
  /** Used only for internal/debug overlays; final video has no text (brief.md §2.4) */
  family: '-apple-system, BlinkMacSystemFont, "Segoe UI", "PingFang SC", "Microsoft YaHei", sans-serif',
  mono: 'ui-monospace, SFMono-Regular, Menlo, monospace',
} as const;

// ─── Type Definitions ────────────────────────────────────────────────────────
interface VideoSpec {
  width: number;
  height: number;
  fps: number;
  totalDurationSeconds: number;
  totalFrames: number;
  format: 'mp4';
  codec: 'h264';
}
