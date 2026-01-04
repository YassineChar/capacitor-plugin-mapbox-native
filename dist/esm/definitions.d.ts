export interface WhisperSpotsMapboxNativePlugin {
    echo(options: {
        value: string;
    }): Promise<{
        value: string;
    }>;
    initMapbox(options: {
        topOffset?: number;
        heightOffset?: number;
    }): Promise<{
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
    setValuesMapbox(options: {
        dataPoints: any[];
        moreWhispersTranslation?: string;
        clusteringThreshold?: number;
    }): Promise<{
        status: string;
    }>;
    setCenterPoint(options: {
        latitude: number;
        longitude: number;
        animated?: boolean;
    }): Promise<{
        status: string;
    }>;
    setCenterAndZoom(options: {
        latitude: number;
        longitude: number;
        zoom: number;
        animated?: boolean;
    }): Promise<{
        status: string;
    }>;
    setZoomLevel(options: {
        zoom: number;
        animated?: boolean;
    }): Promise<{
        status: string;
    }>;
    addCircle(options: {
        latitude: number;
        longitude: number;
        radius: number;
    }): Promise<{
        status: string;
        circleId: string;
    }>;
    removeCircle(): Promise<{
        status: string;
    }>;
    clearMarkers(): Promise<{
        status: string;
    }>;
    setMockUserLocation(options: {
        latitude: number;
        longitude: number;
    }): Promise<{
        status: string;
    }>;
}
//# sourceMappingURL=definitions.d.ts.map