# Blue Screen Issue - Troubleshooting Guide

## Problem

Video loads and plays (audio works, time progresses) but the screen shows only blue color instead of the video frames.

## Root Cause

This is a texture rendering issue on Linux with media_kit/MPV. The video is decoded and playing correctly, but the texture isn't being properly displayed in the Flutter widget.

## Solutions Attempted

### 1. Widget Configuration

Added proper Video widget configuration:

```dart
Video(
  key: ValueKey(_videoController),
  controller: _videoController,
  controls: MaterialVideoControls,
  fill: Colors.black,
  filterQuality: FilterQuality.high,
  wakelock: true,
  pauseUponEnteringBackgroundMode: false,
  resumeUponEnteringForegroundMode: true,
)
```

### 2. Alternative: Use Different Video Output

If the blue screen persists, try using `libmpv` video output directly:

```dart
_player = Player(
  configuration: PlayerConfiguration(
    title: 'Video Player',
    // Force specific video output
    vo: 'gpu',  // or 'x11', 'wayland', 'drm'
  ),
);
```

### 3. Check MPV Configuration

The issue might be related to MPV's video output backend. You can test MPV directly:

```bash
mpv --vo=help  # List available video outputs
mpv --vo=gpu <your-m3u8-url>  # Test with GPU output
mpv --vo=x11 <your-m3u8-url>  # Test with X11 output
```

### 4. System Dependencies

Ensure you have proper graphics drivers and libraries:

```bash
# Fedora
sudo dnf install mesa-libGL mesa-libEGL libva libvdpau

# Ubuntu/Debian
sudo apt-get install libgl1-mesa-dev libegl1-mesa-dev libva-dev libvdpau-dev
```

## Current Status

- ✅ Video loads successfully
- ✅ HLS stream parsing works
- ✅ Audio plays correctly
- ✅ Subtitles load (fonts downloading)
- ✅ Player controls work
- ❌ Video frames not rendering (blue screen)

## Next Steps

1. Hot reload the app to test the widget configuration changes
2. If still blue, try different MPV video output backends
3. Test on Android/Windows to verify it's Linux-specific
4. Consider using a different rendering approach for Linux

## Workaround

If the issue persists, you can:

1. Use the web player for Linux builds
2. Package the app as Flatpak/Snap with bundled graphics libraries
3. Use a different video player package specifically for Linux
