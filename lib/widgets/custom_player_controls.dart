import 'dart:async';
import 'package:flutter/material.dart';
import 'package:media_kit_video/media_kit_video.dart';
import 'package:kuudere/utils/app_fonts.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';

class CustomVideoControls extends StatefulWidget {
  final VideoState videoState;
  final VideoController controller;
  final Function(Offset anchor) onSettingsPressed;
  final Function(Offset anchor) onSubtitlePressed;
  final String title;
  final String episodeTitle;
  final Widget? settingsOverlay;
  final VoidCallback? onNextEpisode;
  final VoidCallback? onPrevEpisode;
  final WidgetBuilder? commentsBuilder;
  final WidgetBuilder? episodesBuilder;
  final Function(String?)? onSidePanelToggled;
  final String? activeSidePanel;
  final VoidCallback? onFullscreenToggle;
  final bool isFullscreen;

  const CustomVideoControls({
    super.key,
    required this.videoState,
    required this.controller,
    required this.onSettingsPressed,
    required this.onSubtitlePressed,
    required this.title,
    required this.episodeTitle,
    this.settingsOverlay,
    this.onNextEpisode,
    this.onPrevEpisode,
    this.commentsBuilder,
    this.episodesBuilder,
    this.onSidePanelToggled,
    this.activeSidePanel,
    this.onFullscreenToggle,
    this.isFullscreen = false,
  });

  @override
  State<CustomVideoControls> createState() => _CustomVideoControlsState();
}

class _CustomVideoControlsState extends State<CustomVideoControls> {
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
  }

  @override
  void dispose() {
    _hideTimer?.cancel();
    _forwardAnimationTimer?.cancel();
    _backwardAnimationTimer?.cancel();
    super.dispose();
  }

  void _startHideTimer() {
    _hideTimer?.cancel();
    if (widget.settingsOverlay != null || widget.activeSidePanel != null)
      return; // Don't hide if settings or side panel are open

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

  void _seekForward() {
    _onUserInteraction();

    setState(() {
      _showForwardAnimation = true;
    });

    _forwardAnimationTimer?.cancel();
    _forwardAnimationTimer = Timer(const Duration(milliseconds: 500), () {
      if (mounted) {
        setState(() {
          _showForwardAnimation = false;
        });
      }
    });

    final position = widget.controller.player.state.position;
    final duration = widget.controller.player.state.duration;
    final newPosition = position + const Duration(seconds: 10);
    widget.controller.player.seek(
      newPosition > duration ? duration : newPosition,
    );
  }

  void _seekBackward() {
    _onUserInteraction();

    setState(() {
      _showBackwardAnimation = true;
    });

    _backwardAnimationTimer?.cancel();
    _backwardAnimationTimer = Timer(const Duration(milliseconds: 500), () {
      if (mounted) {
        setState(() {
          _showBackwardAnimation = false;
        });
      }
    });

    final position = widget.controller.player.state.position;
    final newPosition = position - const Duration(seconds: 10);
    widget.controller.player.seek(
      newPosition < Duration.zero ? Duration.zero : newPosition,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        // Double Tap Detectors (Background Layer)
        Row(
          children: [
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: _toggleVisibility,
                onDoubleTap: _seekBackward,
                child: Container(
                  color: Colors.transparent,
                  child: _showBackwardAnimation
                      ? Center(
                          child: TweenAnimationBuilder<double>(
                            tween: Tween(begin: 0.0, end: 1.0),
                            duration: const Duration(milliseconds: 500),
                            builder: (context, value, child) {
                              return Opacity(
                                opacity: 1.0 - value,
                                child: Transform.scale(
                                  scale: 1.0 + value,
                                  child: Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      const Icon(Icons.replay_10,
                                          color: Colors.white, size: 48),
                                      Text(
                                        "-10s",
                                        style: AppFonts.poppins(
                                          color: Colors.white,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                        )
                      : null,
                ),
              ),
            ),
            Expanded(
              child: GestureDetector(
                behavior: HitTestBehavior.translucent,
                onTap: _toggleVisibility,
                onDoubleTap: _seekForward,
                child: Container(
                  color: Colors.transparent,
                  child: _showForwardAnimation
                      ? Center(
                          child: TweenAnimationBuilder<double>(
                            tween: Tween(begin: 0.0, end: 1.0),
                            duration: const Duration(milliseconds: 500),
                            builder: (context, value, child) {
                              return Opacity(
                                opacity: 1.0 - value,
                                child: Transform.scale(
                                  scale: 1.0 + value,
                                  child: Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      const Icon(Icons.forward_10,
                                          color: Colors.white, size: 48),
                                      Text(
                                        "+10s",
                                        style: AppFonts.poppins(
                                          color: Colors.white,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              );
                            },
                          ),
                        )
                      : null,
                ),
              ),
            ),
          ],
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

        // Buffering Indicator (Always visible if buffering)
        Center(
          child: StreamBuilder<bool>(
            stream: widget.controller.player.stream.buffering,
            initialData: widget.controller.player.state.buffering,
            builder: (context, bufferingSnapshot) {
              final isBuffering = bufferingSnapshot.data ?? false;
              if (isBuffering) {
                return Column(
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
                );
              }
              return const SizedBox.shrink();
            },
          ),
        ),

        // Controls with fade animation
        AnimatedOpacity(
          opacity: _controlsVisible ? 1.0 : 0.0,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeInOut,
          child: IgnorePointer(
            ignoring: !_controlsVisible,
            child: Row(
              children: [
                Expanded(
                  child: Stack(
                    children: [
                      // Top Bar
                      Positioned(
                        top: 0,
                        left: 0,
                        right: 0,
                        child: SafeArea(
                          child: Padding(
                            padding: const EdgeInsets.all(16.0),
                            child: Row(
                              children: [
                                const SizedBox(width: 8),
                                if (widget.videoState.isFullscreen())
                                  Expanded(
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Text(
                                          widget.title,
                                          style: AppFonts.poppins(
                                            color: Colors.white,
                                            fontSize: 16,
                                            fontWeight: FontWeight.bold,
                                          ),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                        Text(
                                          widget.episodeTitle,
                                          style: AppFonts.poppins(
                                            color: Colors.white
                                                .withValues(alpha: 0.7),
                                            fontSize: 12,
                                          ),
                                          maxLines: 1,
                                          overflow: TextOverflow.ellipsis,
                                        ),
                                      ],
                                    ),
                                  )
                                else
                                  const Spacer(),
                                if (widget.isFullscreen) ...[
                                  if (widget.episodesBuilder != null ||
                                      widget.commentsBuilder != null)
                                    IconButton(
                                      icon: Icon(Icons.view_sidebar,
                                          color: widget.activeSidePanel != null
                                              ? Colors.red
                                              : Colors.white),
                                      onPressed: () {
                                        _onUserInteraction();
                                        widget.onSidePanelToggled?.call(
                                            widget.activeSidePanel != null
                                                ? null
                                                : 'sidePanel');
                                      },
                                    ),
                                ],
                                IconButton(
                                  key: _settingsButtonKey,
                                  icon: const Icon(Icons.settings_outlined,
                                      color: Colors.white),
                                  onPressed: () {
                                    _onUserInteraction();
                                    final RenderBox renderBox =
                                        _settingsButtonKey.currentContext!
                                            .findRenderObject() as RenderBox;
                                    final RenderBox parentRenderBox =
                                        context.findRenderObject() as RenderBox;
                                    final offset = renderBox.localToGlobal(
                                        Offset.zero,
                                        ancestor: parentRenderBox);
                                    widget.onSettingsPressed(offset);
                                  },
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),

                      // Center Play/Pause & Seek Buttons
                      Align(
                        alignment: const Alignment(0, -0.2),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            // Previous Episode
                            IconButton(
                              iconSize: 32,
                              icon: Icon(
                                Icons.skip_previous,
                                color: widget.onPrevEpisode != null
                                    ? Colors.white
                                    : Colors.white.withValues(alpha: 0.3),
                              ),
                              onPressed: () {
                                if (widget.onPrevEpisode != null) {
                                  _onUserInteraction();
                                  widget.onPrevEpisode!();
                                }
                              },
                            ),
                            const SizedBox(width: 4),
                            IconButton(
                              iconSize: 32,
                              icon: const Icon(Icons.replay_10,
                                  color: Colors.white),
                              onPressed: () {
                                _onUserInteraction();
                                final position =
                                    widget.controller.player.state.position;
                                widget.controller.player.seek(
                                    position - const Duration(seconds: 10));
                              },
                            ),
                            const SizedBox(width: 12),
                            StreamBuilder<bool>(
                              stream: widget.controller.player.stream.playing,
                              initialData:
                                  widget.controller.player.state.playing,
                              builder: (context, snapshot) {
                                final playing = snapshot.data ?? false;
                                return IconButton(
                                  iconSize: 64,
                                  icon: Icon(
                                    playing
                                        ? Icons.pause_circle_filled
                                        : Icons.play_circle_filled,
                                    color: Colors.white.withValues(alpha: 0.8),
                                  ),
                                  onPressed: () {
                                    _onUserInteraction();
                                    widget.controller.player.playOrPause();
                                  },
                                );
                              },
                            ),
                            const SizedBox(width: 12),
                            IconButton(
                              iconSize: 32,
                              icon: const Icon(Icons.forward_10,
                                  color: Colors.white),
                              onPressed: () {
                                _onUserInteraction();
                                final position =
                                    widget.controller.player.state.position;
                                widget.controller.player.seek(
                                    position + const Duration(seconds: 10));
                              },
                            ),
                            const SizedBox(width: 4),
                            // Next Episode
                            IconButton(
                              iconSize: 32,
                              icon: Icon(
                                Icons.skip_next,
                                color: widget.onNextEpisode != null
                                    ? Colors.white
                                    : Colors.white.withValues(alpha: 0.3),
                              ),
                              onPressed: () {
                                if (widget.onNextEpisode != null) {
                                  _onUserInteraction();
                                  widget.onNextEpisode!();
                                }
                              },
                            ),
                          ],
                        ),
                      ),

                      // Bottom Bar
                      Positioned(
                        bottom: 0,
                        left: 0,
                        right: 0,
                        child: SafeArea(
                          top: false,
                          child: Padding(
                            padding: const EdgeInsets.all(16.0),
                            child: Column(
                              mainAxisSize: MainAxisSize.min,
                              children: [
                                // Progress Bar
                                StreamBuilder<Duration>(
                                  stream:
                                      widget.controller.player.stream.position,
                                  builder: (context, snapshot) {
                                    final position =
                                        snapshot.data ?? Duration.zero;
                                    final duration =
                                        widget.controller.player.state.duration;
                                    return Row(
                                      children: [
                                        Text(
                                          _formatDuration(position),
                                          style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 12),
                                        ),
                                        Expanded(
                                          child: SliderTheme(
                                            data: SliderThemeData(
                                              trackHeight: 2,
                                              thumbShape:
                                                  const RoundSliderThumbShape(
                                                      enabledThumbRadius: 6),
                                              overlayShape:
                                                  const RoundSliderOverlayShape(
                                                      overlayRadius: 12),
                                              activeTrackColor: Colors.red,
                                              inactiveTrackColor: Colors.white
                                                  .withValues(alpha: 0.3),
                                              thumbColor: Colors.red,
                                              overlayColor: Colors.red
                                                  .withValues(alpha: 0.3),
                                            ),
                                            child: Slider(
                                              value: position.inSeconds
                                                  .toDouble()
                                                  .clamp(
                                                      0,
                                                      duration.inSeconds
                                                          .toDouble()),
                                              min: 0,
                                              max:
                                                  duration.inSeconds.toDouble(),
                                              onChanged: (value) {
                                                _onUserInteraction();
                                                widget.controller.player.seek(
                                                    Duration(
                                                        seconds:
                                                            value.toInt()));
                                              },
                                            ),
                                          ),
                                        ),
                                        Text(
                                          _formatDuration(duration),
                                          style: const TextStyle(
                                              color: Colors.white,
                                              fontSize: 12),
                                        ),
                                      ],
                                    );
                                  },
                                ),

                                // Bottom Controls Row
                                Row(
                                  mainAxisAlignment:
                                      MainAxisAlignment.spaceBetween,
                                  children: [
                                    Row(
                                      children: [
                                        StreamBuilder<double>(
                                          stream: widget
                                              .controller.player.stream.volume,
                                          initialData: widget
                                              .controller.player.state.volume,
                                          builder: (context, snapshot) {
                                            final volume =
                                                snapshot.data ?? 100.0;
                                            return Row(
                                              children: [
                                                IconButton(
                                                  icon: Icon(
                                                    volume == 0
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
                                                if (_showVolumeSliderInline)
                                                  SizedBox(
                                                    width: 100,
                                                    child: SliderTheme(
                                                      data: SliderThemeData(
                                                        activeTrackColor:
                                                            Colors.red,
                                                        inactiveTrackColor:
                                                            Colors.white
                                                                .withValues(
                                                                    alpha: 0.3),
                                                        thumbColor: Colors.red,
                                                        overlayColor: Colors.red
                                                            .withValues(
                                                                alpha: 0.3),
                                                        thumbShape:
                                                            const RoundSliderThumbShape(
                                                                enabledThumbRadius:
                                                                    6),
                                                        trackHeight: 2,
                                                      ),
                                                      child: Slider(
                                                        value: volume.clamp(
                                                            0.0, 100.0),
                                                        min: 0,
                                                        max: 100,
                                                        onChanged: (value) {
                                                          _onUserInteraction();
                                                          widget
                                                              .controller.player
                                                              .setVolume(value);
                                                        },
                                                      ),
                                                    ),
                                                  ),
                                              ],
                                            );
                                          },
                                        ),
                                      ],
                                    ),
                                    Row(
                                      children: [
                                        IconButton(
                                          key: _subtitleButtonKey,
                                          icon: const Icon(Icons.subtitles,
                                              color: Colors.white),
                                          onPressed: () {
                                            _onUserInteraction();
                                            final RenderBox renderBox =
                                                _subtitleButtonKey
                                                        .currentContext!
                                                        .findRenderObject()
                                                    as RenderBox;
                                            final RenderBox parentRenderBox =
                                                context.findRenderObject()
                                                    as RenderBox;
                                            final offset = renderBox
                                                .localToGlobal(Offset.zero,
                                                    ancestor: parentRenderBox);
                                            widget.onSubtitlePressed(offset);
                                          },
                                        ),
                                        IconButton(
                                          icon: Icon(
                                            widget.isFullscreen
                                                ? Icons.fullscreen_exit
                                                : Icons.fullscreen,
                                            color: Colors.white,
                                          ),
                                          onPressed: () {
                                            _onUserInteraction();
                                            if (widget.onFullscreenToggle !=
                                                null) {
                                              widget.onFullscreenToggle!();
                                            } else {
                                              if (widget.videoState
                                                  .isFullscreen()) {
                                                widget.videoState
                                                    .exitFullscreen();
                                              } else {
                                                widget.videoState
                                                    .enterFullscreen();
                                              }
                                            }
                                          },
                                        ),
                                      ],
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),

        // Settings Overlay
        if (widget.settingsOverlay != null) widget.settingsOverlay!,
      ],
    );
  }
}
