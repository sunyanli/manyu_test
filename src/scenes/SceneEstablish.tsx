/**
 * src/scenes/SceneEstablish.tsx — S1 入画定位 (0.0–0.5s, 15 frames)
 *
 * brief.md §1 S1: 小狗已入画，正面/微侧脸面向镜头，眼神锁定镜头，耳朵微动。
 * brief.md §2.3 S1: 中近景（胸部以上），浅景深，背景虚化。
 * brief.md §4.2: 极轻环境底噪，尚未出声。
 *
 * Skeleton: branded placeholder block with scene label. Real puppy footage
 * will replace this; durations and specs are authoritative from config.ts.
 */
import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';
import { COLORS, FONTS, SCENES, SUBJECT } from '../config';

export const SceneEstablish: React.FC = () => {
  const frame = useCurrentFrame();
  // Subtle ear-twitch motion (brief.md §1 S1)
  const twitch = interpolate(frame, [0, 7, 15], [0, 0.5, 0], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' });

  return (
    <AbsoluteFill style={{ backgroundColor: COLORS.sceneS1, justifyContent: 'center', alignItems: 'center' }}>
      <div style={{ textAlign: 'center', fontFamily: FONTS.family, color: COLORS.ink }}>
        <div style={{ fontSize: 64, fontWeight: 700, marginBottom: 12 }}>
          {SCENES.S1.id} · {SCENES.S1.name}
        </div>
        <div style={{ fontSize: 28, color: COLORS.sub }}>
          {SUBJECT.breed} · 中近景 · 浅景深
        </div>
        <div style={{ fontSize: 20, color: COLORS.sub, marginTop: 8, transform: `translateY(${twitch * 4}px)` }}>
          🐶 耳朵微动
        </div>
        <div style={{ fontSize: 16, color: COLORS.sub, marginTop: 24, fontFamily: FONTS.mono }}>
          {SCENES.S1.startSeconds.toFixed(2)}s → {(SCENES.S1.startSeconds + SCENES.S1.durationSeconds).toFixed(2)}s
          {'  '}
          ({SCENES.S1.durationFrames}f @ 30fps)
        </div>
      </div>
    </AbsoluteFill>
  );
};
