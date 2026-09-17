/**
 * src/scenes/SceneFreeze.tsx — S3 收尾定格 (2.5–3.0s, 15 frames)
 *
 * brief.md §1 S3: 叫声收尾，嘴微张/微闭，眼神依旧盯镜头，定格在小狗表情。
 * brief.md §2.3 S3: 特写定格，焦平面稳定在面部，焦点回归眼部。
 * brief.md §3: 第3层 收束 — 给观众一个情绪落点，不戛然而止。
 *
 * Skeleton: branded placeholder with freeze-frame indicator.
 */
import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';
import { COLORS, FONTS, SCENES } from '../config';

export const SceneFreeze: React.FC = () => {
  const frame = useCurrentFrame();
  // Last-bark echo decay (brief.md §1 S3: 最后一声余音收尾)
  const echo = interpolate(frame, [0, 15], [0.6, 0], { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' });

  return (
    <AbsoluteFill
      style={{
        backgroundColor: COLORS.sceneS3,
        justifyContent: 'center',
        alignItems: 'center',
        opacity: 1, // no fade-out (brief.md §6.1: 无淡入淡出)
      }}
    >
      <div style={{ textAlign: 'center', fontFamily: FONTS.family, color: COLORS.ink }}>
        <div style={{ fontSize: 64, fontWeight: 700, marginBottom: 12 }}>
          {SCENES.S3.id} · {SCENES.S3.name}
        </div>
        <div style={{ fontSize: 80, lineHeight: 1, margin: '16px 0' }}>🐶</div>
        <div style={{ fontSize: 22, color: COLORS.sub, opacity: 0.5 + echo * 0.5 }}>
          定格表情 · 焦点回归眼部 · 自然结束
        </div>
        <div style={{ fontSize: 16, color: COLORS.sub, marginTop: 20, fontFamily: FONTS.mono }}>
          {SCENES.S3.startSeconds.toFixed(2)}s → {(SCENES.S3.startSeconds + SCENES.S3.durationSeconds).toFixed(2)}s
          {'  '}
          ({SCENES.S3.durationFrames}f)
        </div>
      </div>
    </AbsoluteFill>
  );
};
