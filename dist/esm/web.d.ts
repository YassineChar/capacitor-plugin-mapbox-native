import { WebPlugin } from '@capacitor/core';
import type { WhisperSpotsMapboxNativePlugin } from './definitions';
export declare class WhisperSpotsMapboxNativeWeb extends WebPlugin implements WhisperSpotsMapboxNativePlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    initMapbox(): Promise<{
        status: string;
    }>;
    showMapbox(): Promise<{
        status: string;
    }>;
    hideMapbox(): Promise<{
        status: string;
    }>;
    closeMapbox(): Promise<{
        status: string;
    }>;
    isMapboxVisible(): Promise<{
        status: number;
    }>;
    setValuesMapbox(): Promise<{
        status: string;
    }>;
    setCenterPoint(): Promise<{
        status: string;
    }>;
    setCenterAndZoom(): Promise<{
        status: string;
    }>;
    setZoomLevel(): Promise<{
        status: string;
    }>;
    addCircle(): Promise<{
        status: string;
        circleId: string;
    }>;
    removeCircle(): Promise<{
        status: string;
    }>;
    clearMarkers(): Promise<{
        status: string;
    }>;
    setMockUserLocation(): Promise<{
        status: string;
    }>;
}
//# sourceMappingURL=web.d.ts.map