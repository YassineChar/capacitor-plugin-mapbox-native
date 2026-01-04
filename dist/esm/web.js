import { WebPlugin } from '@capacitor/core';
export class WhisperSpotsMapboxNativeWeb extends WebPlugin {
    async echo(options) {
        return options;
    }
    async initMapbox() {
        throw this.unimplemented('Not implemented on web.');
    }
    async showMapbox() {
        throw this.unimplemented('Not implemented on web.');
    }
    async hideMapbox() {
        throw this.unimplemented('Not implemented on web.');
    }
    async closeMapbox() {
        throw this.unimplemented('Not implemented on web.');
    }
    async isMapboxVisible() {
        throw this.unimplemented('Not implemented on web.');
    }
    async setValuesMapbox() {
        throw this.unimplemented('Not implemented on web.');
    }
    async setCenterPoint() {
        throw this.unimplemented('Not implemented on web.');
    }
    async setCenterAndZoom() {
        throw this.unimplemented('Not implemented on web.');
    }
    async setZoomLevel() {
        throw this.unimplemented('Not implemented on web.');
    }
    async addCircle() {
        throw this.unimplemented('Not implemented on web.');
    }
    async removeCircle() {
        throw this.unimplemented('Not implemented on web.');
    }
    async clearMarkers() {
        throw this.unimplemented('Not implemented on web.');
    }
    async setMockUserLocation() {
        throw this.unimplemented('Not implemented on web.');
    }
}
//# sourceMappingURL=web.js.map