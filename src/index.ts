/**
 * src/index.ts — Remotion entry point
 *
 * Calls registerRoot so the Remotion bundler / CLI can discover compositions.
 */
import { registerRoot } from 'remotion';
import { RemotionRoot } from './Root';

registerRoot(RemotionRoot);
