import 'dart:async';
import 'package:flutter/material.dart';
import 'package:video_player/video_player.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';

class LinuxVideoControls extends StatefulWidget {
  final VideoPlayerController controller;
  final Function(Offset anchor) onSettingsPressed;
  final Function(Offset anchor) onSubtitlePressed;
  final VoidCallback? onFullscreenToggle;
  final bool isFullscreen;
  final VoidCallback? onEpisodesPressed;
  final VoidCallback? onCommentsPressed;
  final String? activeSidePanel;
  final Widget? settingsOverlay;

  const LinuxVideoControls({
    super.key,
    required this.controller,
    required this.onSettingsPressed,
    required this.onSubtitlePressed,
    this.onFullscreenToggle,
    this.isFullscreen = false,
    this.onEpisodesPressed,
    this.onCommentsPressed,
    this.activeSidePanel,
    this.settingsOverlay,
  });

  @override
  State<LinuxVideoControls> createState() => _LinuxVideoControlsState();
}

class _LinuxVideoControlsState extends State<LinuxVideoControls> {
  bool _visible = true;
  bool _showVolumeSliderInline = false;
  Timer? _hideTimer;
  final GlobalKey _settingsButtonKey = GlobalKey();
  final GlobalKey _subtitleButtonKey = GlobalKey();

  // Double tap animation state
  bool _showForwardAnimation = false;
  bool _showBackwardAnimation = false;
  Timer? _forwardAnimationTimer;
  Timer? _backwardAnimationTimer;

  @override
  void initState() {
    super.initState();
    _startHideTimer();
    widget.controller.addListener(_onControllerUpdate);
  }

  void _onControllerUpdate() {
    if (mounted) {
      setState(() {});
    }
  }

  @override
  void dispose() {
    _hideTimer?.cancel();
    _forwardAnimationTimer?.cancel();
    _backwardAnimationTimer?.cancel();
    widget.controller.removeListener(_onControllerUpdate);
    super.dispose();
  }

  void _startHideTimer() {
    _hideTimer?.cancel();
    if (widget.settingsOverlay != null || widget.activeSidePanel != null)
      return;

    _hideTimer = Timer(const Duration(seconds: 5), () {
      if (mounted) {
        setState(() {
          _visible = false;
        });
      }
    });
  }

  void _toggleVisibility() {
    setState(() {
      _visible = !_visible;
    });
    if (_visible) {
      _startHideTimer();
    } else {
      _hideTimer?.cancel();
    }
  }

  void _onUserInteraction() {
    if (!_visible) {
      setState(() {
        _visible = true;
      });
    }
    _startHideTimer();
  }

  bool get _controlsVisible =>
      _visible ||
      widget.settingsOverlay != null ||
      widget.activeSidePanel != null;

  String _formatDuration(Duration duration) {
    String twoDigits(int n) => n.toString().padLeft(2, '0');
    String twoDigitMinutes = twoDigits(duration.inMinutes.remainder(60));
    String twoDigitSeconds = twoDigits(duration.inSeconds.remainder(60));
    if (duration.inHours > 0) {
      return '${twoDigits(duration.inHours)}:$twoDigitMinutes:$twoDigitSeconds';
    } else {
      return '$twoDigitMinutes:$twoDigitSeconds';
    }
  }

  void _handleDoubleTapSeek(bool forward) {
    final currentPosition = widget.controller.value.position;
    final seekDuration = Duration(seconds: forward ? 10 : -10);
    final newPosition = currentPosition + seekDuration;

    widget.controller.seekTo(Duration(
      milliseconds: newPosition.inMilliseconds.clamp(
        0,
        widget.controller.value.duration.inMilliseconds,
      ),
    ));

    setState(() {
      if (forward) {
        _showForwardAnimation = true;
        _forwardAnimationTimer?.cancel();
        _forwardAnimationTimer = Timer(const Duration(milliseconds: 500), () {
          if (mounted) setState(() => _showForwardAnimation = false);
        });
      } else {
        _showBackwardAnimation = true;
        _backwardAnimationTimer?.cancel();
        _backwardAnimationTimer = Timer(const Duration(milliseconds: 500), () {
          if (mounted) setState(() => _showBackwardAnimation = false);
        });
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    final value = widget.controller.value;
    final isBuffering = value.isBuffering;
    final isPlaying = value.isPlaying;
    final position = value.position;
    final duration = value.duration;

    return GestureDetector(
      onTap: _toggleVisibility,
      behavior: HitTestBehavior.opaque,
      child: Stack(
        children: [
          // Double tap areas for seek
          Row(
            children: [
              // Left side - backward seek
              Expanded(
                child: GestureDetector(
                  onDoubleTap: () => _handleDoubleTapSeek(false),
                  behavior: HitTestBehavior.translucent,
                  child: Container(color: Colors.transparent),
                ),
              ),
              // Right side - forward seek
              Expanded(
                child: GestureDetector(
                  onDoubleTap: () => _handleDoubleTapSeek(true),
                  behavior: HitTestBehavior.translucent,
                  child: Container(color: Colors.transparent),
                ),
              ),
            ],
          ),

          // Forward animation
          if (_showForwardAnimation)
            Positioned(
              right: 50,
              top: 0,
              bottom: 0,
              child: Center(
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.5),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.fast_forward,
                    color: Colors.white,
                    size: 40,
                  ),
                ),
              ),
            ),

          // Backward animation
          if (_showBackwardAnimation)
            Positioned(
              left: 50,
              top: 0,
              bottom: 0,
              child: Center(
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.5),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(
                    Icons.fast_rewind,
                    color: Colors.white,
                    size: 40,
                  ),
                ),
              ),
            ),

          // Gradient Overlay with fade animation
          Positioned.fill(
            child: IgnorePointer(
              child: AnimatedOpacity(
                opacity: _controlsVisible ? 1.0 : 0.0,
                duration: const Duration(milliseconds: 300),
                curve: Curves.easeInOut,
                child: Container(
                  decoration: BoxDecoration(
                    gradient: LinearGradient(
                      begin: Alignment.topCenter,
                      end: Alignment.bottomCenter,
                      colors: [
                        Colors.black.withValues(alpha: 0.7),
                        Colors.transparent,
                        Colors.transparent,
                        Colors.black.withValues(alpha: 0.7),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),

          // Buffering Indicator
          if (isBuffering)
            Center(
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  LoadingAnimationWidget.threeArchedCircle(
                    color: Colors.red,
                    size: 50,
                  ),
                  const SizedBox(height: 20),
                  Text(
                    "Loading...",
                    style:
                        TextStyle(color: Colors.white.withValues(alpha: 0.7)),
                  ),
                ],
              ),
            ),

          // Center Play/Pause button
          if (!isBuffering)
            Center(
              child: AnimatedOpacity(
                opacity: _controlsVisible ? 1.0 : 0.0,
                duration: const Duration(milliseconds: 300),
                child: GestureDetector(
                  onTap: () {
                    _onUserInteraction();
                    if (isPlaying) {
                      widget.controller.pause();
                    } else {
                      widget.controller.play();
                    }
                  },
                  child: Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.black.withValues(alpha: 0.5),
                      shape: BoxShape.circle,
                    ),
                    child: Icon(
                      isPlaying ? Icons.pause : Icons.play_arrow,
                      color: Colors.white,
                      size: 48,
                    ),
                  ),
                ),
              ),
            ),

          // Controls with fade animation
          AnimatedOpacity(
            opacity: _controlsVisible ? 1.0 : 0.0,
            duration: const Duration(milliseconds: 300),
            curve: Curves.easeInOut,
            child: IgnorePointer(
              ignoring: !_controlsVisible,
              child: Stack(
                children: [
                  // Top Bar
                  Positioned(
                    top: 0,
                    left: 0,
                    right: 0,
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          if (widget.isFullscreen)
                            IconButton(
                              icon: const Icon(Icons.arrow_back,
                                  color: Colors.white),
                              onPressed: () {
                                _onUserInteraction();
                                widget.onFullscreenToggle?.call();
                              },
                            ),
                          const Spacer(),
                          if (widget.isFullscreen) ...[
                            IconButton(
                              icon: Icon(
                                Icons.list,
                                color: widget.activeSidePanel == 'episodes'
                                    ? Colors.red
                                    : Colors.white,
                              ),
                              onPressed: () {
                                _onUserInteraction();
                                widget.onEpisodesPressed?.call();
                              },
                            ),
                            IconButton(
                              icon: Icon(
                                Icons.chat_bubble_outline,
                                color: widget.activeSidePanel == 'comments'
                                    ? Colors.red
                                    : Colors.white,
                              ),
                              onPressed: () {
                                _onUserInteraction();
                                widget.onCommentsPressed?.call();
                              },
                            ),
                          ],
                        ],
                      ),
                    ),
                  ),

                  // Bottom Bar
                  Positioned(
                    bottom: 0,
                    left: 0,
                    right: 0,
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                          horizontal: 16, vertical: 12),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          // Progress bar
                          SliderTheme(
                            data: SliderThemeData(
                              trackHeight: 4,
                              thumbShape: const RoundSliderThumbShape(
                                  enabledThumbRadius: 6),
                              overlayShape: const RoundSliderOverlayShape(
                                  overlayRadius: 12),
                              activeTrackColor: Colors.red,
                              inactiveTrackColor:
                                  Colors.white.withValues(alpha: 0.3),
                              thumbColor: Colors.red,
                              overlayColor: Colors.red.withValues(alpha: 0.2),
                            ),
                            child: Slider(
                              value: duration.inMilliseconds > 0
                                  ? position.inMilliseconds.toDouble().clamp(
                                      0, duration.inMilliseconds.toDouble())
                                  : 0,
                              min: 0,
                              max: duration.inMilliseconds > 0
                                  ? duration.inMilliseconds.toDouble()
                                  : 1,
                              onChanged: (value) {
                                _onUserInteraction();
                                widget.controller.seekTo(
                                    Duration(milliseconds: value.toInt()));
                              },
                            ),
                          ),
                          // Control buttons row
                          Row(
                            children: [
                              // Play/Pause
                              IconButton(
                                icon: Icon(
                                  isPlaying ? Icons.pause : Icons.play_arrow,
                                  color: Colors.white,
                                ),
                                onPressed: () {
                                  _onUserInteraction();
                                  if (isPlaying) {
                                    widget.controller.pause();
                                  } else {
                                    widget.controller.play();
                                  }
                                },
                              ),
                              // Time display
                              Text(
                                '${_formatDuration(position)} / ${_formatDuration(duration)}',
                                style: GoogleFonts.poppins(
                                  color: Colors.white,
                                  fontSize: 12,
                                ),
                              ),
                              const Spacer(),
                              // Volume
                              if (_showVolumeSliderInline)
                                SizedBox(
                                  width: 100,
                                  child: SliderTheme(
                                    data: SliderThemeData(
                                      trackHeight: 2,
                                      thumbShape: const RoundSliderThumbShape(
                                          enabledThumbRadius: 6),
                                      activeTrackColor: Colors.white,
                                      inactiveTrackColor:
                                          Colors.white.withValues(alpha: 0.3),
                                      thumbColor: Colors.white,
                                    ),
                                    child: Slider(
                                      value: value.volume,
                                      min: 0,
                                      max: 1,
                                      onChanged: (v) {
                                        _onUserInteraction();
                                        widget.controller.setVolume(v);
                                      },
                                    ),
                                  ),
                                ),
                              IconButton(
                                icon: Icon(
                                  value.volume == 0
                                      ? Icons.volume_off
                                      : Icons.volume_up,
                                  color: Colors.white,
                                ),
                                onPressed: () {
                                  _onUserInteraction();
                                  setState(() {
                                    _showVolumeSliderInline =
                                        !_showVolumeSliderInline;
                                  });
                                },
                              ),
                              // Subtitles
                              IconButton(
                                key: _subtitleButtonKey,
                                icon: const Icon(
                                  Icons.closed_caption_outlined,
                                  color: Colors.white,
                                ),
                                onPressed: () {
                                  _onUserInteraction();
                                  final RenderBox renderBox = _subtitleButtonKey
                                      .currentContext!
                                      .findRenderObject() as RenderBox;
                                  final Offset position =
                                      renderBox.localToGlobal(Offset.zero);
                                  widget.onSubtitlePressed(position);
                                },
                              ),
                              // Settings
                              IconButton(
                                key: _settingsButtonKey,
                                icon: const Icon(
                                  Icons.settings,
                                  color: Colors.white,
                                ),
                                onPressed: () {
                                  _onUserInteraction();
                                  final RenderBox renderBox = _settingsButtonKey
                                      .currentContext!
                                      .findRenderObject() as RenderBox;
                                  final Offset position =
                                      renderBox.localToGlobal(Offset.zero);
                                  widget.onSettingsPressed(position);
                                },
                              ),
                              // Fullscreen
                              IconButton(
                                icon: Icon(
                                  widget.isFullscreen
                                      ? Icons.fullscreen_exit
                                      : Icons.fullscreen,
                                  color: Colors.white,
                                ),
                                onPressed: () {
                                  _onUserInteraction();
                                  widget.onFullscreenToggle?.call();
                                },
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),

          // Settings Overlay
          if (widget.settingsOverlay != null) widget.settingsOverlay!,
        ],
      ),
    );
  }
}
