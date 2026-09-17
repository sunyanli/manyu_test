/**
 * src/scenes/SceneBark.tsx — S2 张嘴叫 (0.50–2.50s, 60 frames)
 *
 * brief.md §1 S2: 张嘴叫 2–3 声短促连叫，伴随轻微头部前探、耳朵向后抖动。
 * brief.md §2.3 S2: 面部特写，焦点锁定嘴部与眼部，景深极浅。
 * brief.md §4.2: 真实幼犬短促叫声，与嘴部开合同帧同步。
 * brief.md §7: 卡点容差 ≤ 1 帧 (33ms).
 *
 * Independent of S1/S3; imports only config.ts + shared PuppyFace.
 * Bark pulses drive mouthOpenness on a per-frame basis so mouth and (future)
 * audio share the exact same frame grid (≤1 frame sync).
 */
import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';
import { COLORS } from '../config';
import { PuppyFace } from '../components/PuppyFace';

/**
 * Bark pattern: 3 short bursts within the 60-frame window.
 * Each burst: mouth opens (onset→peak), closes (peak→offset).
 * These frame indices are the single sync grid for mouth + audio.
 */
const BARK_PULSES = [
  { onset: 4, peak: 8, offset: 13 },
  { onset: 24, peak: 28, offset: 33 },
  { onset: 44, peak: 48, offset: 53 },
];

export const SceneBark: React.FC = () => {
  const frame = useCurrentFrame();

  // Mouth openness 0..1 across the 3 pulses (peak-aligned, ≤1-frame grid)
  const mouthOpenness = BARK_PULSES.reduce((max, p) => {
    const local = interpolate(frame, [p.onset, p.peak, p.offset], [0, 1, 0], {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    });
    return Math.max(max, local);
  }, 0);

  // Ears pinned back in phase with each bark (brief §1 S2 耳朵向后抖动)
  const earBack = BARK_PULSES.reduce((max, p) => {
    const local = interpolate(frame, [p.onset, p.peak, p.offset], [0, 1, 0.2], {
      extrapolateLeft: 'clamp',
      extrapolateRight: 'clamp',
    });
    return Math.max(max, local);
  }, 0);

  // Head forward probe across the whole scene (brief §1 S2 轻微头部前探)
  const headProbe = interpolate(frame, [0, 30, 60], [0, 10, 4], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // 面部特写 framing progression: ease toward closeup
  const zoom = interpolate(frame, [0, 30], [1.0, 1.18], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill style={{ backgroundColor: COLORS.sceneS2 }}>
      <PuppyFace
        mouthOpenness={mouthOpenness}
        earBack={earBack}
        headProbe={headProbe}
        scale={zoom}
        focusY={0}
        showChest={false}
        vignette={0.6}
      />
    </AbsoluteFill>
  );
};
