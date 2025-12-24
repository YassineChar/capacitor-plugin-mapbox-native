# @whisperspots/capacitor-mapbox-native

Mapbox Native SDK plugin for Capacitor (Android only).

## Install

```bash
npm install @whisperspots/capacitor-mapbox-native
npx cap sync
```

## Configuration

Add your Mapbox token to `gradle.properties`:

```properties
MAPBOX_DOWNLOADS_TOKEN=your_secret_token_here
```

## API

### initMapbox(options)

Initialize the Mapbox map.

### showMapbox()

Show the map.

### hideMapbox()

Hide the map.

### setValuesMapbox(options)

Add markers to the map.

### setCenterPoint(options)

Set map center.

### addCircle(options)

Add a circle overlay.

## License

MIT
