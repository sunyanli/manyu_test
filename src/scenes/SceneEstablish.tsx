/**
 * src/scenes/SceneEstablish.tsx — S1 入画定位 (0.00–0.50s, 15 frames)
 *
 * brief.md §1 S1: 小狗已入画，正面/微侧脸面向镜头，眼神锁定镜头，耳朵微动。
 * brief.md §2.3 S1: 中近景（胸部以上），浅景深，背景虚化。
 * brief.md §4.2: 极轻环境底噪，尚未出声。
 *
 * Independent of S2/S3; imports only config.ts + shared PuppyFace.
 */
import React from 'react';
import { AbsoluteFill, useCurrentFrame, interpolate } from 'remotion';
import { COLORS, FONTS, SCENES, SUBJECT } from '../config';
import { PuppyFace } from '../components/PuppyFace';

export const SceneEstablish: React.FC = () => {
  const frame = useCurrentFrame();

  // Subtle ear-twitch: small oscillation (brief §1 S1 耳朵微动)
  const earTwitch = interpolate(
    frame,
    [0, 4, 8, 12, 15],
    [0, 0.04, -0.03, 0.02, 0],
    { extrapolateLeft: 'clamp', extrapolateRight: 'clamp' }
  );

  // Slight settle-in: subject eases in from a tiny scale (入画)
  const settle = interpolate(frame, [0, 15], [0.97, 1.0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });

  // 中近景 framing (chest up), face in safe zone
  return (
    <AbsoluteFill style={{ backgroundColor: COLORS.sceneS1 }}>
      <PuppyFace
        mouthOpenness={0}
        earTwitch={earTwitch}
        scale={settle * 0.82}
        focusY={120}
        showChest
        vignette={0.4}
      />
      {/* Debug label — skeleton stage only; final deliverable has no text (brief §2.4) */}
      <div
        style={{
          position: 'absolute', top: 40, left: 0, right: 0, textAlign: 'center',
          fontFamily: FONTS.mono, color: COLORS.ink, opacity: 0.5, fontSize: 22,
        }}
      >
        {SCENES.S1.id} · {SCENES.S1.name} · {SUBJECT.breed} · 中近景
      </div>
    </AbsoluteFill>
  );
};
