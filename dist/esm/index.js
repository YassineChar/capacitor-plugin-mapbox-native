import { registerPlugin } from '@capacitor/core';
const WhisperSpotsMapboxNative = registerPlugin('WhisperSpotsMapboxNative', {
    web: () => import('./web').then(m => new m.WhisperSpotsMapboxNativeWeb()),
});
export * from './definitions';
export { WhisperSpotsMapboxNative };
//# sourceMappingURL=index.js.map