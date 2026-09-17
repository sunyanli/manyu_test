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
import { AbsoluteFill, Series } from 'remotion';
import { COLORS, FONTS, SCENES } from './config';
import { SceneEstablish } from './scenes/SceneEstablish';
import { SceneBark } from './scenes/SceneBark';
import { SceneFreeze } from './scenes/SceneFreeze';

export const Main: React.FC = () => {
  // brief.md §1: single shot, no cuts — Series sequences the three framing stages.
  // brief.md §2.4: no text/overlays/brand elements in final deliverable — pure picture.
  void SCENES;
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
    </AbsoluteFill>
  );
};
