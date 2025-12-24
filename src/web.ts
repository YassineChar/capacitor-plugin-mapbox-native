import { WebPlugin } from '@capacitor/core';

import type { WhisperSpotsMapboxNativePlugin } from './definitions';

export class WhisperSpotsMapboxNativeWeb extends WebPlugin implements WhisperSpotsMapboxNativePlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    return options;
  }

  async initMapbox(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async showMapbox(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async hideMapbox(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async closeMapbox(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async isMapboxVisible(): Promise<{ status: number }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setValuesMapbox(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setCenterPoint(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setCenterAndZoom(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setZoomLevel(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async addCircle(): Promise<{ status: string; circleId: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async removeCircle(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async clearMarkers(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }

  async setMockUserLocation(): Promise<{ status: string }> {
    throw this.unimplemented('Not implemented on web.');
  }
}
