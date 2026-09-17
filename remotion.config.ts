import { Config } from '@remotion/cli/config';

Config.setVideoImageFormat('jpeg');
// This host exposes only 1 logical core; concurrency must not exceed it or
// Remotion aborts with "Maximum for --concurrency is 1".
Config.setConcurrency(1);
// Use the Playwright-cached Chrome Headless Shell so renders never trigger a
// remote 88 MB download (offline-friendly). Falls back gracefully if absent.
Config.setBrowserExecutable(
  '/root/.cache/ms-playwright/chromium_headless_shell-1217/chrome-headless-shell-linux64/chrome-headless-shell'
);
