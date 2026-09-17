/**
 * src/Main.tsx — Main Video Composition (skeleton)
 *
 * Renders the 3-scene sequence:
 *   S1 入画定位 (0.0–0.5s) → S2 张嘴叫 (0.5–2.5s) → S3 收尾定格 (2.5–3.0s)
 *
 * This is a skeleton: scenes render as branded placeholder blocks with scene
 * labels and proportional timeline bars. Real puppy footage / audio will be
 * composited in downstream scenes; the structure, durations, and specs here
 * are authoritative (sourced from config.ts → brief.md / verified_data.json).
 *
 * brief.md §1: single continuous shot, no cuts — only framing progression.
 * No fade-in/out, no extra padding (brief.md §1 / §6.1).
 */
import React from 'react';
import { AbsoluteFill, Series, interpolate, useCurrentFrame } from 'remotion';
import { COLORS, FONTS, SCENES, VIDEO } from './config';
import { SceneEstablish } from './scenes/SceneEstablish';
import { SceneBark } from './scenes/SceneBark';
import { SceneFreeze } from './scenes/SceneFreeze';

export const Main: React.FC = () => {
  const frame = useCurrentFrame();

  // brief.md §1: single shot, no cuts — Series sequences the three framing stages
  return (
    <AbsoluteFill style={{ backgroundColor: COLORS.background, fontFamily: FONTS.family }}>
      <Series>
        <Series.Sequence durationInFrames={SCENES.S1.durationFrames}>
          <SceneEstablish />
        </Series.Sequence>
        <Series.Sequence durationInFrames={SCENES.S2.durationFrames}>
          <SceneBark />
        </Series.Sequence>
        <Series.Sequence durationInFrames={SCENES.S3.durationFrames}>
          <SceneFreeze />
        </Series.Sequence>
      </Series>

      {/* Debug timeline ruler — skeleton only; final video has no text/overlays */}
      <DebugTimeline frame={frame} />
    </AbsoluteFill>
  );
};

/**
 * Minimal scene-progress ruler overlay for the skeleton stage.
 * brief.md §2.4: no overlays in final deliverable — this will be removed
 * once real footage scenes are composited.
 */
const DebugTimeline: React.FC<{ frame: number }> = ({ frame }) => {
  const progress = frame / VIDEO.totalFrames;
  const opacity = interpolate(frame, [VIDEO.totalFrames - 3, VIDEO.totalFrames], [0.7, 0], {
    extrapolateLeft: 'clamp',
    extrapolateRight: 'clamp',
  });
  return (
    <div
      style={{
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        height: 4,
        opacity,
      }}
    >
      <div
        style={{
          width: `${progress * 100}%`,
          height: '100%',
          backgroundColor: COLORS.accent,
        }}
      />
    </div>
  );
};
