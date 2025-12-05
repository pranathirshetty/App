import 'dart:async';
import 'package:flutter/material.dart';
import 'package:kuudere/utils/app_fonts.dart';

/// Subtitle event data containing timing and text
class SubtitleEvent {
  final double startTime; // in seconds
  final double endTime; // in seconds
  final List<String> text;

  SubtitleEvent({
    required this.startTime,
    required this.endTime,
    required this.text,
  });

  /// Check if subtitle should be visible at given time (with delay applied)
  bool isVisibleAt(double currentTime, double delay) {
    // delay > 0 means subtitles appear later (shifted forward in time)
    // So we need to compare against currentTime + delay
    final adjustedStart = startTime + delay;
    final adjustedEnd = endTime + delay;
    return currentTime >= adjustedStart && currentTime < adjustedEnd;
  }
}

/// Custom subtitle overlay widget with delay support
class SubtitleOverlay extends StatefulWidget {
  /// Stream of subtitle events from the video player
  final Stream<SubtitleEvent>? subtitleStream;

  /// Current video position stream (in seconds)
  final Stream<double>? positionStream;

  /// Subtitle delay in seconds (positive = later, negative = earlier)
  final double delay;

  /// Subtitle font size
  final double fontSize;

  /// Subtitle text color
  final Color textColor;

  /// Subtitle background color
  final Color backgroundColor;

  /// Subtitle position (0-100, 100 = bottom)
  final double position;

  const SubtitleOverlay({
    super.key,
    this.subtitleStream,
    this.positionStream,
    this.delay = 0.0,
    this.fontSize = 28.0,
    this.textColor = Colors.white,
    this.backgroundColor = const Color(0x80000000),
    this.position = 90.0,
  });

  @override
  State<SubtitleOverlay> createState() => _SubtitleOverlayState();
}

class _SubtitleOverlayState extends State<SubtitleOverlay> {
  final List<SubtitleEvent> _subtitleQueue = [];
  List<String> _currentText = [];
  double _currentPosition = 0.0;
  StreamSubscription? _subtitleSubscription;
  StreamSubscription? _positionSubscription;
  Timer? _updateTimer;

  @override
  void initState() {
    super.initState();
    _subscribeToStreams();
    // Update subtitles frequently to ensure smooth display
    _updateTimer = Timer.periodic(const Duration(milliseconds: 50), (_) {
      _updateCurrentSubtitle();
    });
  }

  @override
  void didUpdateWidget(SubtitleOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.subtitleStream != widget.subtitleStream ||
        oldWidget.positionStream != widget.positionStream) {
      _unsubscribeFromStreams();
      _subscribeToStreams();
    }
  }

  void _subscribeToStreams() {
    _subtitleSubscription = widget.subtitleStream?.listen((event) {
      // Add new subtitle to queue
      _subtitleQueue.add(event);
      // Keep only recent subtitles (last 50)
      if (_subtitleQueue.length > 50) {
        _subtitleQueue.removeAt(0);
      }
      _updateCurrentSubtitle();
    });

    _positionSubscription = widget.positionStream?.listen((position) {
      _currentPosition = position;
      _updateCurrentSubtitle();
    });
  }

  void _unsubscribeFromStreams() {
    _subtitleSubscription?.cancel();
    _positionSubscription?.cancel();
  }

  void _updateCurrentSubtitle() {
    if (!mounted) return;

    List<String> newText = [];

    // Find all subtitles that should be visible at current time with delay
    for (final sub in _subtitleQueue) {
      if (sub.isVisibleAt(_currentPosition, widget.delay)) {
        newText.addAll(sub.text);
      }
    }

    if (newText.join('\n') != _currentText.join('\n')) {
      setState(() {
        _currentText = newText;
      });
    }
  }

  @override
  void dispose() {
    _updateTimer?.cancel();
    _unsubscribeFromStreams();
    super.dispose();
  }

  /// Clear all subtitles (useful when changing subtitle track)
  void clearSubtitles() {
    _subtitleQueue.clear();
    setState(() {
      _currentText = [];
    });
  }

  @override
  Widget build(BuildContext context) {
    if (_currentText.isEmpty) {
      return const SizedBox.shrink();
    }

    // Calculate vertical position
    // position 0 = top, 50 = center, 100 = bottom
    final alignment = Alignment(
      0.0, // centered horizontally
      (widget.position / 50.0) - 1.0, // -1 to 1 range
    );

    return Positioned.fill(
      child: IgnorePointer(
        child: Align(
          alignment: alignment,
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: widget.backgroundColor,
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              _currentText.join('\n'),
              textAlign: TextAlign.center,
              style: AppFonts.poppins(
                fontSize: widget.fontSize,
                color: widget.textColor,
                fontWeight: FontWeight.w500,
                shadows: [
                  Shadow(
                    offset: const Offset(1, 1),
                    blurRadius: 3,
                    color: Colors.black.withValues(alpha: 0.8),
                  ),
                  Shadow(
                    offset: const Offset(-1, -1),
                    blurRadius: 3,
                    color: Colors.black.withValues(alpha: 0.8),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// Controller for managing subtitles with the video player
class SubtitleController {
  final _subtitleStreamController = StreamController<SubtitleEvent>.broadcast();
  final _positionStreamController = StreamController<double>.broadcast();

  Stream<SubtitleEvent> get subtitleStream => _subtitleStreamController.stream;
  Stream<double> get positionStream => _positionStreamController.stream;

  /// Call this when a subtitle event is received from the player
  void onSubtitleText(double start, double end, List<String> text) {
    _subtitleStreamController.add(SubtitleEvent(
      startTime: start,
      endTime: end,
      text: text,
    ));
  }

  /// Call this to update the current video position
  void updatePosition(double positionSeconds) {
    _positionStreamController.add(positionSeconds);
  }

  /// Clear all subtitle data (useful when changing video/subtitle track)
  void clear() {
    // Broadcast an empty subtitle to clear display
    _subtitleStreamController.add(SubtitleEvent(
      startTime: 0,
      endTime: 0,
      text: [],
    ));
  }

  void dispose() {
    _subtitleStreamController.close();
    _positionStreamController.close();
  }
}
