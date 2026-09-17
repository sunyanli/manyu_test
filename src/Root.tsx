/**
 * src/Root.tsx — Composition Registry
 *
 * Registers all Remotion compositions. The "Main" composition is the 3-second
 * vertical video defined in brief.md. The "DataOverlay" composition renders a
 * data-class scene that references verified_data.json for spec verification.
 *
 * All dimensions / durations / fps come from config.ts (SSOT).
 */
import React from 'react';
import { Composition } from 'remotion';
import { VIDEO, SCENES } from './config';
import { Main } from './Main';
import { DataOverlay } from './scenes/DataScene';

export const RemotionRoot: React.FC = () => {
  return (
    <>
      {/* Primary composition: 3 s vertical puppy-bark video */}
      <Composition
        id="Main"
        component={Main}
        durationInFrames={VIDEO.totalFrames}
        fps={VIDEO.fps}
        width={VIDEO.width}
        height={VIDEO.height}
      />

      {/* Data-class composition: references verified_data.json */}
      <Composition
        id="DataOverlay"
        component={DataOverlay}
        durationInFrames={VIDEO.totalFrames}
        fps={VIDEO.fps}
        width={VIDEO.width}
        height={VIDEO.height}
      />
    </>
  );
};
