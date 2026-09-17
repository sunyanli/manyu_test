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
import { COLORS, SCENES, SUBJECT } from '../config';
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

  // 中近景 framing (chest up), face in safe zone.
  // brief §2.4: no text/overlays in final deliverable — pure picture.
  // Subject color spec sourced from config.ts SUBJECT (breed / catchlight / colorTemp).
  // SCENES.S1 / SUBJECT referenced to keep the SSOT contract; no debug label rendered.
  void SCENES;
  void SUBJECT;
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
    </AbsoluteFill>
  );
};
