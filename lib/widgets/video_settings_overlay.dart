import 'dart:math';

import 'package:flutter/material.dart';
import 'package:kuudere/utils/app_fonts.dart';

class VideoSettingsOverlay extends StatefulWidget {
  final List<String> qualities;
  final String currentQuality;
  final Function(String) onQualityChanged;

  final List<double> speeds;
  final double currentSpeed;
  final Function(double) onSpeedChanged;

  final List<Map<String, dynamic>> subtitles;
  final Map<String, dynamic>? currentSubtitle;
  final Function(Map<String, dynamic>?) onSubtitleChanged;

  final List<String> servers;
  final String currentServer;
  final Function(String) onServerChanged;

  final VoidCallback onSubtitleSettingsPressed;
  final SettingsView initialView;
  final Offset? anchorPosition;

  // Subtitle Settings
  final double subtitleSize;
  final Function(double) onSubtitleSizeChanged;
  final double subtitleDelay;
  final Function(double) onSubtitleDelayChanged;
  final double subtitlePos;
  final Function(double) onSubtitlePosChanged;

  final VoidCallback onClose;

  const VideoSettingsOverlay({
    super.key,
    required this.qualities,
    required this.currentQuality,
    required this.onQualityChanged,
    required this.speeds,
    required this.currentSpeed,
    required this.onSpeedChanged,
    required this.subtitles,
    required this.currentSubtitle,
    required this.onSubtitleChanged,
    required this.servers,
    required this.currentServer,
    required this.onServerChanged,
    required this.onSubtitleSettingsPressed,
    this.initialView = SettingsView.main,
    this.anchorPosition,
    required this.subtitleSize,
    required this.onSubtitleSizeChanged,
    required this.subtitleDelay,
    required this.onSubtitleDelayChanged,
    required this.subtitlePos,
    required this.onSubtitlePosChanged,
    required this.onClose,
  });

  @override
  State<VideoSettingsOverlay> createState() => _VideoSettingsOverlayState();
}

enum SettingsView { main, quality, speed, subtitles, server, subtitleSettings }

class _VideoSettingsOverlayState extends State<VideoSettingsOverlay>
    with SingleTickerProviderStateMixin {
  late SettingsView _currentView;
  late double _localSubtitleSize;
  late double _localSubtitleDelay;
  late double _localSubtitlePos;

  // Animation controllers
  late AnimationController _animationController;
  late Animation<double> _slideAnimation;
  late Animation<double> _fadeAnimation;

  @override
  void initState() {
    super.initState();
    _currentView = widget.initialView;
    _localSubtitleSize = widget.subtitleSize;
    _localSubtitleDelay = widget.subtitleDelay;
    _localSubtitlePos = widget.subtitlePos;

    // Initialize animation controller
    _animationController = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );

    _slideAnimation = Tween<double>(begin: 1.0, end: 0.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOutCubic),
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );

    // Start the animation
    _animationController.forward();
  }

  @override
  void dispose() {
    _animationController.dispose();
    super.dispose();
  }

  @override
  void didUpdateWidget(VideoSettingsOverlay oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.subtitleSize != widget.subtitleSize) {
      _localSubtitleSize = widget.subtitleSize;
    }
    if (oldWidget.subtitleDelay != widget.subtitleDelay) {
      _localSubtitleDelay = widget.subtitleDelay;
    }
    if (oldWidget.subtitlePos != widget.subtitlePos) {
      _localSubtitlePos = widget.subtitlePos;
    }
  }

  void _closeWithAnimation() {
    _animationController.reverse().then((_) {
      widget.onClose();
    });
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return AnimatedBuilder(
          animation: _animationController,
          builder: (context, child) {
            return Stack(
              children: [
                Positioned.fill(
                  child: GestureDetector(
                    onTap: _closeWithAnimation,
                    behavior: HitTestBehavior.translucent,
                    child: Container(
                      color: Colors.black
                          .withValues(alpha: 0.5 * _fadeAnimation.value),
                    ),
                  ),
                ),
                _buildBottomSheet(constraints.biggest),
              ],
            );
          },
        );
      },
    );
  }

  Widget _buildBottomSheet(Size size) {
    // Bottom sheet style: full width (with some margin maybe), bottom aligned
    final double width =
        min(300.0, size.width); // Max width for tablets/desktop
    final double maxHeight = size.height * 0.8; // Max 80% height

    return Align(
      alignment: Alignment.bottomCenter,
      child: Transform.translate(
        offset: Offset(0, maxHeight * _slideAnimation.value),
        child: Container(
          width: width,
          constraints: BoxConstraints(maxHeight: maxHeight),
          decoration: BoxDecoration(
            color: Colors.black.withValues(alpha: 0.80),
            borderRadius: const BorderRadius.vertical(top: Radius.circular(16)),
            border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              // Drag handle
              Center(
                child: Container(
                  margin: const EdgeInsets.only(top: 8, bottom: 8),
                  width: 40,
                  height: 4,
                  decoration: BoxDecoration(
                    color: Colors.grey[600],
                    borderRadius: BorderRadius.circular(2),
                  ),
                ),
              ),
              Flexible(
                child: ClipRRect(
                  borderRadius:
                      const BorderRadius.vertical(top: Radius.circular(16)),
                  child: _buildCurrentView(),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCurrentView() {
    Widget view;
    switch (_currentView) {
      case SettingsView.main:
        view = _buildMainView();
        break;
      case SettingsView.quality:
        view = _buildQualityView();
        break;
      case SettingsView.speed:
        view = _buildSpeedView();
        break;
      case SettingsView.subtitles:
        view = _buildSubtitlesView();
        break;
      case SettingsView.server:
        view = _buildServerView();
        break;
      case SettingsView.subtitleSettings:
        view = _buildSubtitleSettingsView();
        break;
    }

    return AnimatedSwitcher(
      duration: const Duration(milliseconds: 200),
      transitionBuilder: (Widget child, Animation<double> animation) {
        return FadeTransition(
          opacity: animation,
          child: child,
        );
      },
      child: KeyedSubtree(
        key: ValueKey<SettingsView>(_currentView),
        child: view,
      ),
    );
  }

  Widget _buildMainView() {
    return SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _buildMenuItem(
            icon: Icons.dns_outlined,
            title: 'Server',
            value: widget.currentServer.toUpperCase(),
            onTap: () => setState(() => _currentView = SettingsView.server),
          ),
          if (widget.qualities.isNotEmpty)
            _buildMenuItem(
              icon: Icons.tune, // Sliders icon for Quality
              title: 'Quality',
              value: widget.currentQuality,
              onTap: () => setState(() => _currentView = SettingsView.quality),
            ),
          _buildMenuItem(
            icon: Icons.play_circle_outline,
            title: 'Speed',
            value: widget.currentSpeed == 1.0
                ? 'Normal'
                : '${widget.currentSpeed}x',
            onTap: () => setState(() => _currentView = SettingsView.speed),
          ),
          if (widget.subtitles.isNotEmpty)
            _buildMenuItem(
              icon: Icons.closed_caption_outlined,
              title: 'Captions',
              value: widget.currentSubtitle?['title'] ??
                  widget.currentSubtitle?['language_name'] ??
                  'Off',
              onTap: () =>
                  setState(() => _currentView = SettingsView.subtitles),
            ),
          const SizedBox(height: 16),
        ],
      ),
    );
  }

  Widget _buildQualityView() {
    return _buildSubMenu(
      title: 'Quality',
      children: widget.qualities.map((q) {
        return _buildRadioItem(
          title: q,
          isSelected: q == widget.currentQuality,
          onTap: () {
            widget.onQualityChanged(q);
            widget.onClose();
          },
        );
      }).toList(),
    );
  }

  Widget _buildSpeedView() {
    return _buildSubMenu(
      title: 'Speed',
      children: widget.speeds.map((s) {
        return _buildRadioItem(
          title: s == 1.0 ? 'Normal' : '${s}x',
          isSelected: s == widget.currentSpeed,
          onTap: () {
            widget.onSpeedChanged(s);
            widget.onClose();
          },
        );
      }).toList(),
    );
  }

  Widget _buildSubtitlesView() {
    return _buildSubMenu(
      title: 'Captions',
      onCustomize: () =>
          setState(() => _currentView = SettingsView.subtitleSettings),
      children: [
        _buildRadioItem(
          title: 'Off',
          isSelected: widget.currentSubtitle == null,
          onTap: () {
            widget.onSubtitleChanged(null);
            widget.onClose();
          },
        ),
        ...widget.subtitles.map((s) {
          final title = s['title'] ?? s['language_name'] ?? 'Unknown';
          return _buildRadioItem(
            title: title,
            isSelected: s == widget.currentSubtitle,
            onTap: () {
              widget.onSubtitleChanged(s);
              widget.onClose();
            },
          );
        }),
      ],
    );
  }

  Widget _buildServerView() {
    return _buildSubMenu(
      title: 'Server',
      children: widget.servers.map((s) {
        return _buildRadioItem(
          title: s.toUpperCase(),
          isSelected: s == widget.currentServer,
          onTap: () {
            widget.onServerChanged(s);
            widget.onClose();
          },
        );
      }).toList(),
    );
  }

  Widget _buildSubtitleSettingsView() {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.white),
                onPressed: () =>
                    setState(() => _currentView = SettingsView.subtitles),
              ),
              Text(
                "Subtitle Settings",
                style: AppFonts.poppins(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
        ),
        const Divider(color: Colors.white24, height: 1),
        Flexible(
          child: SingleChildScrollView(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    "Size",
                    style:
                        AppFonts.poppins(color: Colors.white, fontSize: 14),
                  ),
                  Row(
                    children: [
                      const Text("Small",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                      Expanded(
                        child: SliderTheme(
                          data: SliderThemeData(
                            activeTrackColor: Colors.red,
                            inactiveTrackColor: Colors.white24,
                            thumbColor: Colors.red,
                            overlayColor: Colors.red.withValues(alpha: 0.2),
                          ),
                          child: Slider(
                            value: _localSubtitleSize,
                            min: 12.0,
                            max: 64.0,
                            onChanged: (value) {
                              setState(() {
                                _localSubtitleSize = value;
                              });
                              widget.onSubtitleSizeChanged(value);
                            },
                          ),
                        ),
                      ),
                      const Text("Large",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    "Delay (seconds)",
                    style:
                        AppFonts.poppins(color: Colors.white, fontSize: 14),
                  ),
                  Row(
                    children: [
                      const Text("-5s",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                      Expanded(
                        child: SliderTheme(
                          data: SliderThemeData(
                            activeTrackColor: Colors.red,
                            inactiveTrackColor: Colors.white24,
                            thumbColor: Colors.red,
                            overlayColor: Colors.red.withValues(alpha: 0.2),
                          ),
                          child: Slider(
                            value: _localSubtitleDelay,
                            min: -5.0,
                            max: 5.0,
                            divisions: 20,
                            label: "${_localSubtitleDelay.toStringAsFixed(1)}s",
                            onChanged: (value) {
                              setState(() {
                                _localSubtitleDelay = value;
                              });
                              widget.onSubtitleDelayChanged(value);
                            },
                          ),
                        ),
                      ),
                      const Text("+5s",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                    ],
                  ),
                  const SizedBox(height: 16),
                  Text(
                    "Position (Vertical)",
                    style:
                        AppFonts.poppins(color: Colors.white, fontSize: 14),
                  ),
                  Row(
                    children: [
                      const Text("Top",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                      Expanded(
                        child: SliderTheme(
                          data: SliderThemeData(
                            activeTrackColor: Colors.red,
                            inactiveTrackColor: Colors.white24,
                            thumbColor: Colors.red,
                            overlayColor: Colors.red.withValues(alpha: 0.2),
                          ),
                          child: Slider(
                            value: _localSubtitlePos,
                            min: 0.0,
                            max: 100.0,
                            onChanged: (value) {
                              setState(() {
                                _localSubtitlePos = value;
                              });
                              widget.onSubtitlePosChanged(value);
                            },
                          ),
                        ),
                      ),
                      const Text("Bottom",
                          style:
                              TextStyle(color: Colors.white70, fontSize: 10)),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildMenuItem({
    required IconData icon,
    required String title,
    required String value,
    required VoidCallback onTap,
    bool showChevron = true,
  }) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        child: Row(
          children: [
            Icon(icon, color: Colors.white, size: 24),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                title,
                style: AppFonts.poppins(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
            ),
            if (value.isNotEmpty)
              Text(
                value,
                style: AppFonts.poppins(
                  color: Colors.grey[400],
                  fontSize: 14,
                ),
              ),
            if (showChevron) ...[
              const SizedBox(width: 8),
              Icon(Icons.chevron_right, color: Colors.grey[400], size: 16),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildSubMenu({
    required String title,
    required List<Widget> children,
    VoidCallback? onCustomize,
  }) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
          child: Row(
            children: [
              IconButton(
                icon: const Icon(Icons.arrow_back, color: Colors.white),
                onPressed: () =>
                    setState(() => _currentView = SettingsView.main),
              ),
              Text(
                title,
                style: AppFonts.poppins(
                  color: Colors.white,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              if (onCustomize != null) ...[
                const Spacer(),
                TextButton(
                  onPressed: onCustomize,
                  child: Text(
                    "Customize",
                    style: AppFonts.poppins(
                        color: Colors.red, fontWeight: FontWeight.bold),
                  ),
                ),
              ]
            ],
          ),
        ),
        const Divider(color: Colors.white24, height: 1),
        Flexible(
          child: ListView(
            shrinkWrap: true,
            children: children,
          ),
        ),
      ],
    );
  }

  Widget _buildRadioItem({
    required String title,
    required bool isSelected,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
        child: Row(
          children: [
            if (isSelected)
              const Icon(Icons.check, color: Colors.white, size: 24)
            else
              const SizedBox(width: 24),
            const SizedBox(width: 16),
            Expanded(
              child: Text(
                title,
                style: AppFonts.poppins(
                  color: Colors.white,
                  fontSize: 16,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
