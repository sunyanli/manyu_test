/**
 * src/scenes/SceneBark.tsx — S2 张嘴叫 (0.5–2.5s, 60 frames)
 *
 * brief.md §1 S2: 张嘴叫 2–3 声短促连叫，伴随轻微头部前探、耳朵向后抖动。
 * brief.md §2.3 S2: 面部特写，焦点锁定嘴部与眼部，景深极浅。
 * brief.md §4.2: 真实幼犬短促叫声，与嘴部开合同帧同步。
 * brief.md §7: 卡点容差 ≤ 1 帧 (33ms).
 *
 * Skeleton: branded placeholder with mouth-open/close animation synced to
 * the 2–3 bark pattern. Real footage + audio composited downstream.
 */
import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';
import { AUDIO, COLORS, FONTS, SCENES } from '../config';

/** Bark pattern: 3 short bursts within the 60-frame window.
 *  Each burst: mouth opens (frame 0→4), peaks (4→8), closes (8→12). */
const BARK_PULSES = [
  { onset: 4, peak: 8, offset: 12 },
  { onset: 24, peak: 28, offset: 32 },
  { onset: 44, peak: 48, offset: 52 },
];

export const SceneBark: React.FC = () => {
  const frame = useCurrentFrame();

  // Mouth openness 0–1 across the 3 pulses
  const mouthOpen = BARK_PULSES.reduce((max, p) => {
    const local = interpolate(frame, [p.onset, p.peak, p.offset], [0, 1, 0], {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    });
    return Math.max(max, local);
  }, 0);

  // Head forward probe (brief.md §1 S2)
  const headProbe = interpolate(frame, [0, 30, 60], [0, 8, 4], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' });

  return (
    <AbsoluteFill
      style={{
        backgroundColor: COLORS.sceneS2,
        justifyContent: 'center',
        alignItems: 'center',
        transform: `scale(${1 + mouthOpen * 0.01}) translateY(${-headProbe}px)`,
      }}
    >
      <div style={{ textAlign: 'center', fontFamily: FONTS.family, color: COLORS.ink }}>
        <div style={{ fontSize: 64, fontWeight: 700, marginBottom: 12 }}>
          {SCENES.S2.id} · {SCENES.S2.name}
        </div>
        {/* Mouth open/close indicator */}
        <div
          style={{
            fontSize: 80,
            lineHeight: 1,
            margin: '16px 0',
            transform: `scaleY(${0.3 + mouthOpen * 0.7})`,
            transition: 'none',
          }}
        >
          🐕
        </div>
        <div style={{ fontSize: 22, color: COLORS.sub }}>
          嘴部开合 · 同帧同步 · 容差 ≤{AUDIO.syncToleranceMs}ms
        </div>
        <div style={{ fontSize: 16, color: COLORS.sub, marginTop: 20, fontFamily: FONTS.mono }}>
          {SCENES.S2.startSeconds.toFixed(2)}s → {(SCENES.S2.startSeconds + SCENES.S2.durationSeconds).toFixed(2)}s
          {'  '}
          ({SCENES.S2.durationFrames}f · 3 pulses)
        </div>
      </div>
    </AbsoluteFill>
  );
};
