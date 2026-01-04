'use strict';

var core = require('@capacitor/core');

const WhisperSpotsMapboxNative = core.registerPlugin('WhisperSpotsMapboxNative', {
    web: () => Promise.resolve().then(function () { return web; }).then(m => new m.WhisperSpotsMapboxNativeWeb()),
});

class WhisperSpotsMapboxNativeWeb extends core.WebPlugin {
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

var web = /*#__PURE__*/Object.freeze({
    __proto__: null,
    WhisperSpotsMapboxNativeWeb: WhisperSpotsMapboxNativeWeb
});

exports.WhisperSpotsMapboxNative = WhisperSpotsMapboxNative;
//# sourceMappingURL=plugin.cjs.js.map
