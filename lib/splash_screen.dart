import 'dart:convert';
import 'package:flutter/foundation.dart';
import 'dart:async';
import 'dart:io';
import 'package:flutter/material.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';

import 'package:kuudere/services/realtime_service.dart';
import 'package:kuudere/auth_screen.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/http_service.dart';

import 'home_screen.dart';

class SplashScreen extends StatefulWidget {
  const SplashScreen({super.key});

  @override
  State<SplashScreen> createState() => _SplashScreenState();
}

class _SplashScreenState extends State<SplashScreen> {
  final authService = AuthService();
  final httpService = HttpService();
  final RealtimeService _realtimeService = RealtimeService();

  late final Player player;
  late final VideoController controller;

  // To store where we should go
  Widget? _nextScreen;
  bool _videoCompleted = false;

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom("/");

    // Initialize video player
    final bool isLinux = !kIsWeb && Platform.isLinux;
    player = Player();
    controller = VideoController(
      player,
      configuration: VideoControllerConfiguration(
        enableHardwareAcceleration: !isLinux,
      ),
    );

    _initVideo();
    _checkSession(); // Start checking immediately
  }

  Future<void> _initVideo() async {
    try {
      await player.open(Media('asset://assets/splash.mp4'));
      // Mute to allow autoplay on browsers
      await player.setVolume(0);
    } catch (e) {
      debugPrint('Error opening video: $e');
    }

    // Listen for completion
    player.stream.completed.listen((completed) {
      if (completed) {
        _videoCompleted = true;
        _tryNavigate();
      }
    });

    // Fallback timeout in case video fails or browser blocks it
    Future.delayed(const Duration(seconds: 4), () {
      if (!_videoCompleted) {
        debugPrint('Video timed out, forcing navigation');
        _videoCompleted = true;
        _tryNavigate();
      }
    });
  }

  Future<void> _checkSession() async {
    try {
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null && sessionInfo.anisurgeToken != null) {
        final response = await httpService.get(
          '/v1/me',
          requireAuth: true,
          useBff: true,
        );
        if (response.statusCode == 200) {
          _nextScreen = const HomeScreen();
        } else {
          _nextScreen = const AuthScreen();
        }
      } else {
        _nextScreen = const AuthScreen();
      }
    } catch (e) {
      debugPrint('Session check error: $e');
      _nextScreen = const AuthScreen();
    }
    _tryNavigate();
  }

  void _tryNavigate() {
    if (_videoCompleted && _nextScreen != null && mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => _nextScreen!),
      );
    }
  }

  @override
  void dispose() {
    player.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: const Color(0xFF0B0B0B),
      body: Center(
        child: Video(
          controller: controller,
          fit: BoxFit.contain, // Adjust to screen without stretching
          controls: NoVideoControls, // Hide controls for splash screen
        ),
      ),
    );
  }
}
