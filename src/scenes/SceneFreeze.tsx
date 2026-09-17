/**
 * src/scenes/SceneFreeze.tsx — S3 收尾定格 (2.50–3.00s, 15 frames)
 *
 * brief.md §1 S3: 叫声收尾，嘴微张/微闭，眼神依旧盯镜头，定格在小狗表情。
 * brief.md §2.3 S3: 特写定格，焦平面稳定在面部，焦点回归眼部。
 * brief.md §3: 第3层 收束 — 给观众一个情绪落点，不戛然而止。
 *
 * Independent of S1/S2; imports only config.ts + shared PuppyFace.
 * No fade-out (brief §6.1 无淡入淡出): opacity stays 1.
 */
import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';
import { COLORS, FONTS, SCENES } from '../config';
import { PuppyFace } from '../components/PuppyFace';

export const SceneFreeze: React.FC = () => {
  const frame = useCurrentFrame();

  // Last-bark echo decay: mouth eases from slightly open to micro-closed
  // (brief §1 S3 嘴微张/微闭, 最后一声余音收尾). No fade-out of the frame.
  const mouthOpenness = interpolate(frame, [0, 6, 15], [0.18, 0.07, 0.05], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // Focus returns to eyes: tiny zoom toward the eye line (焦点回归眼部)
  const focusY = interpolate(frame, [0, 15], [0, -8], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  return (
    <AbsoluteFill style={{ backgroundColor: COLORS.sceneS3, opacity: 1 }}>
      <PuppyFace
        mouthOpenness={mouthOpenness}
        scale={1.18}
        focusY={focusY}
        showChest={false}
        vignette={0.62}
      />
      {/* Debug label — skeleton stage only; final deliverable has no text (brief §2.4) */}
      <div
        style={{
          position: 'absolute', top: 40, left: 0, right: 0, textAlign: 'center',
          fontFamily: FONTS.mono, color: COLORS.ink, opacity: 0.5, fontSize: 22,
        }}
      >
        {SCENES.S3.id} · {SCENES.S3.name} · 特写定格 · 自然结束
      </div>
    </AbsoluteFill>
  );
};
