/**
 * src/components/PuppyFace.tsx — Shared SVG puppy subject (presentational)
 *
 * A single source of visual truth for the 田园幼犬 subject across S1/S2/S3.
 * Pure presentational component: every animation value is passed in by the
 * calling scene, so scenes stay independent and non-interfering (per workbench
 * parallel-development requirement).
 *
 * Visual spec sources:
 *  - brief.md §2.2  色彩与质感 (warm ~5200K, 高饱和不过曝, 毛绒根根分明, catchlight 瞳孔上方偏左)
 *  - brief.md §2.3  景别与焦点 (中近景→面部特写→定格特写, 浅景深, 背景虚化)
 *  - brief.md §2.3  背景 (室内浅色柔光 米白/浅木, 无杂物)
 *  - brief.md §4.1  田园幼犬 ~2–3 月龄, 毛发蓬松, 眼神圆亮
 *  - config.ts SUBJECT (breed / catchlight position / colorTempK)
 */
import React from 'react';
import { COLORS, SUBJECT, VIDEO } from '../config';

export interface PuppyFaceProps {
  /** 0..1 — mouth vertical openness (S2 bark sync) */
  mouthOpenness?: number;
  /** radians — ear twitch offset (S1 微动 / S2 向后抖动) */
  earTwitch?: number;
  /** 0..1 — ears pinned back during bark (brief §1 S2 耳朵向后抖动) */
  earBack?: number;
  /** px — head forward probe translateY (brief §1 S2 轻微头部前探) */
  headProbe?: number;
  /** overall scale for framing progression (S1 中近景 < S2/S3 特写) */
  scale?: number;
  /** vertical translate for framing (keep face in safe zone 上1/3~2/3) */
  focusY?: number;
  /** show chest/shoulders (wider S1 shot); hidden on face closeup */
  showChest?: boolean;
  /** depth-of-field vignette strength (浅景深 / 极浅) */
  vignette?: number;
}

const FUR = '#caa15a';        // 田园幼犬 warm tan
const FUR_DARK = '#a87f3f';   // shadow / fur root tone
const FUR_LIGHT = '#e4c98c';  // highlight, 高光通透
const MUZZLE = '#f1e0c0';     // cream muzzle
const NOSE = '#3a2a25';
const EYE = '#221a16';
const CATCHLIGHT = '#ffffff';
const BG_TOP = '#f7efe0';      // 室内浅色柔光 米白
const BG_BOTTOM = '#e6d4b0';   // 浅木 warm wood
const BOKEH = '#fff7e6';

export const PuppyFace: React.FC<PuppyFaceProps> = ({
  mouthOpenness = 0,
  earTwitch = 0,
  earBack = 0,
  headProbe = 0,
  scale = 1,
  focusY = 0,
  showChest = true,
  vignette = 0.45,
}) => {
  const cx = 540;
  const cy = 680; // upper-third face placement (safe zone)
  // catchlight upper-left of pupil (SUBJECT.catchlight)
  const cl = SUBJECT.catchlight;

  // Mouth grows from a thin line to an open dark ellipse
  const mouthH = 6 + mouthOpenness * 46;
  const mouthW = 70 - mouthOpenness * 6;

  // Ear rotation: base twitch + pinned-back offset
  const earRotL = -8 + (earTwitch * 180) / Math.PI - earBack * 14;
  const earRotR = 8 - (earTwitch * 180) / Math.PI + earBack * 14;

  return (
    <svg
      width={VIDEO.width}
      height={VIDEO.height}
      viewBox={`0 0 ${VIDEO.width} ${VIDEO.height}`}
      style={{ display: 'block' }}
    >
      <defs>
        <linearGradient id="bg" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={BG_TOP} />
          <stop offset="100%" stopColor={BG_BOTTOM} />
        </linearGradient>
        <radialGradient id="furGrad" cx="50%" cy="38%" r="65%">
          <stop offset="0%" stopColor={FUR_LIGHT} />
          <stop offset="55%" stopColor={FUR} />
          <stop offset="100%" stopColor={FUR_DARK} />
        </radialGradient>
        <radialGradient id="vignette" cx="50%" cy="46%" r="62%">
          <stop offset="60%" stopColor="#000" stopOpacity="0" />
          <stop offset="100%" stopColor="#1a1208" stopOpacity={vignette} />
        </radialGradient>
        <filter id="softBlur" x="-20%" y="-20%" width="140%" height="140%">
          <feGaussianBlur stdDeviation="14" />
        </filter>
      </defs>

      {/* Background: indoor soft light, cream→soft wood, no clutter */}
      <rect width={VIDEO.width} height={VIDEO.height} fill="url(#bg)" />

      {/* Soft bokeh light blobs (浅景深 背景虚化) */}
      <g opacity="0.5" filter="url(#softBlur)">
        <circle cx="210" cy="300" r="90" fill={BOKEH} />
        <circle cx="880" cy="240" r="120" fill={BOKEH} />
        <circle cx="150" cy="1100" r="140" fill={BOKEH} />
        <circle cx="930" cy="1200" r="100" fill={BOKEH} />
        <circle cx="540" cy="1450" r="160" fill={BOKEH} opacity="0.7" />
      </g>

      {/* Subject group: framing progression via scale + focusY + headProbe */}
      <g transform={`translate(${cx} ${cy + focusY}) scale(${scale}) translate(${-cx} ${-(cy + headProbe)})`}>
        {/* Chest / shoulders — only in wider S1 shot */}
        {showChest && (
          <g>
            <path
              d={`M${cx - 360} ${cy + 520}
                C ${cx - 360} ${cy + 360}, ${cx - 120} ${cy + 300}, ${cx} ${cy + 300}
                C ${cx + 120} ${cy + 300}, ${cx + 360} ${cy + 360}, ${cx + 360} ${cy + 520}
                L ${cx + 360} 1920 L ${cx - 360} 1920 Z`}
              fill="url(#furGrad)"
            />
            <path
              d={`M${cx - 360} ${cy + 520} C ${cx - 360} ${cy + 360}, ${cx - 120} ${cy + 300}, ${cx} ${cy + 300}`}
              fill="none" stroke={FUR_LIGHT} strokeWidth="3" opacity="0.5"
            />
          </g>
        )}

        {/* Ears (floppy) */}
        <g transform={`translate(${cx - 250} ${cy - 110}) rotate(${earRotL})`}>
          <path d="M0 0 C -40 60, -70 180, -10 250 C 40 270, 90 200, 70 120 C 60 50, 30 10, 0 0 Z" fill={FUR_DARK} />
          <path d="M10 20 C -20 70, -45 170, 0 230 C 35 245, 70 185, 55 120 Z" fill={FUR} />
        </g>
        <g transform={`translate(${cx + 250} ${cy - 110}) rotate(${earRotR})`}>
          <path d="M0 0 C 40 60, 70 180, 10 250 C -40 270, -90 200, -70 120 C -60 50, -30 10, 0 0 Z" fill={FUR_DARK} />
          <path d="M-10 20 C 20 70, 45 170, 0 230 C -35 245, -70 185, -55 120 Z" fill={FUR} />
        </g>

        {/* Head */}
        <ellipse cx={cx} cy={cy} rx="270" ry="245" fill="url(#furGrad)" />
        {/* Fur texture: subtle root strokes (毛绒根根分明) */}
        <g stroke={FUR_DARK} strokeWidth="2.5" opacity="0.28" fill="none" strokeLinecap="round">
          <path d={`M${cx - 200} ${cy - 160} q -10 30 5 55`} />
          <path d={`M${cx - 120} ${cy - 200} q -6 30 8 50`} />
          <path d={`M${cx} ${cy - 220} q 0 30 0 50`} />
          <path d={`M${cx + 120} ${cy - 200} q 6 30 -8 50`} />
          <path d={`M${cx + 200} ${cy - 160} q 10 30 -5 55`} />
          <path d={`M${cx - 230} ${cy} q -14 20 -2 42`} />
          <path d={`M${cx + 230} ${cy} q 14 20 2 42`} />
        </g>

        {/* Muzzle (cream) */}
        <ellipse cx={cx} cy={cy + 80} rx="150" ry="120" fill={MUZZLE} />

        {/* Cheek fluff */}
        <ellipse cx={cx - 170} cy={cy + 110} rx="70" ry="60" fill={FUR_LIGHT} opacity="0.8" />
        <ellipse cx={cx + 170} cy={cy + 110} rx="70" ry="60" fill={FUR_LIGHT} opacity="0.8" />

        {/* Eyes — round bright, locked on camera */}
        <g>
          {/* left eye */}
          <ellipse cx={cx - 95} cy={cy - 30} rx="46" ry="52" fill={EYE} />
          <circle cx={cx - 95 + (cl.x - 0.5) * 60} cy={cy - 30 + (cl.y - 0.5) * 60} r="14" fill={CATCHLIGHT} />
          <circle cx={cx - 95 - 10} cy={cy - 30 + 14} r="5" fill={CATCHLIGHT} opacity="0.55" />
          {/* right eye */}
          <ellipse cx={cx + 95} cy={cy - 30} rx="46" ry="52" fill={EYE} />
          <circle cx={cx + 95 + (cl.x - 0.5) * 60} cy={cy - 30 + (cl.y - 0.5) * 60} r="14" fill={CATCHLIGHT} />
          <circle cx={cx + 95 - 10} cy={cy - 30 + 14} r="5" fill={CATCHLIGHT} opacity="0.55" />
        </g>

        {/* Brows fluff */}
        <path d={`M${cx - 150} ${cy - 95} q 55 -28 110 0`} stroke={FUR_DARK} strokeWidth="6" fill="none" opacity="0.35" strokeLinecap="round" />
        <path d={`M${cx + 40} ${cy - 95} q 55 -28 110 0`} stroke={FUR_DARK} strokeWidth="6" fill="none" opacity="0.35" strokeLinecap="round" />

        {/* Nose */}
        <ellipse cx={cx} cy={cy + 35} rx="34" ry="24" fill={NOSE} />
        <ellipse cx={cx - 8} cy={cy + 28} rx="9" ry="6" fill="#6b4f44" opacity="0.7" />

        {/* Mouth — opens with mouthOpenness (S2 嘴部开合) */}
        <g transform={`translate(${cx} ${cy + 60})`}>
          {/* upper lip */}
          <path d={`M${-mouthW / 2} 0 Q 0 8 ${mouthW / 2} 0`} stroke={NOSE} strokeWidth="7" fill="none" strokeLinecap="round" />
          {/* open mouth interior */}
          {mouthOpenness > 0.02 && (
            <>
              <ellipse cx="0" cy={mouthH / 2 - 4} rx={mouthW / 2} ry={mouthH / 2} fill="#7a2a25" />
              <ellipse cx="0" cy={mouthH / 2 - 4} rx={mouthW / 2 - 8} ry={mouthH / 2 - 6} fill="#c85a4a" opacity="0.85" />
              {/* tongue hint */}
              <ellipse cx="0" cy={mouthH - 6} rx={mouthW / 2 - 16} ry="10" fill="#e88a7a" opacity="0.9" />
            </>
          )}
          {/* lower jaw when closed */}
          {mouthOpenness <= 0.02 && (
            <path d={`M${-mouthW / 2} 0 Q 0 18 ${mouthW / 2} 0`} stroke={NOSE} strokeWidth="6" fill="none" strokeLinecap="round" />
          )}
        </g>
      </g>

      {/* Depth-of-field vignette overlay (浅景深) */}
      <rect width={VIDEO.width} height={VIDEO.height} fill="url(#vignette)" pointerEvents="none" />
    </svg>
  );
};
