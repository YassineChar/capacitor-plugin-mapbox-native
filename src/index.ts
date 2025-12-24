import { registerPlugin } from '@capacitor/core';

import type { WhisperSpotsMapboxNativePlugin } from './definitions';

const WhisperSpotsMapboxNative = registerPlugin<WhisperSpotsMapboxNativePlugin>('WhisperSpotsMapboxNative', {
  web: () => import('./web').then(m => new m.WhisperSpotsMapboxNativeWeb()),
});

export * from './definitions';
export { WhisperSpotsMapboxNative };
