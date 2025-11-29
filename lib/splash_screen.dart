import 'dart:convert';
import 'dart:async';
import 'package:flutter/material.dart';

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

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom("/");
    _startSplashSequence();
  }

  Future<void> _startSplashSequence() async {
    await Future.delayed(const Duration(seconds: 3));
    // Version check disabled
    // await _checkVersion();
    await _checkSession();
  }

  Future<void> _checkSession() async {
    try {
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        // Check session using new SvelteKit endpoint with cookie-based auth
        final response =
            await httpService.get('/api/user/current', requireAuth: true);

        if (response.statusCode == 200) {
          final data = jsonDecode(response.body);
          if (data['success'] != false) {
            _navigateToHome();
            return;
          }
        }

        if (authService.isSessionExpired(sessionInfo)) {
          _navigateToAuth();
          return;
        }
      }
      _navigateToAuth();
    } catch (e) {
      debugPrint('Session check error: $e');
      _navigateToAuth();
    }
  }

  void _navigateToAuth() {
    if (mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const AuthScreen()),
      );
    }
  }

  void _navigateToHome() {
    if (mounted) {
      Navigator.pushReplacement(
        context,
        MaterialPageRoute(builder: (context) => const HomeScreen()),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => FocusManager.instance.primaryFocus?.unfocus(),
      child: Scaffold(
        backgroundColor: const Color(0xFF0B0B0B),
        body: SafeArea(
          child: Column(
            mainAxisSize: MainAxisSize.max,
            children: [
              Flexible(
                child: Align(
                  alignment: AlignmentDirectional.center,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(16),
                    child: Image.asset(
                      'assets/splash.png',
                      width: 150,
                      height: 150,
                      fit: BoxFit.fill,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 24),
              const Text(
                'AniSurge powered by kuudere',
                style: TextStyle(
                  fontFamily: 'Inter',
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 0.5,
                ),
              ),
              const SizedBox(height: 30),
            ],
          ),
        ),
      ),
    );
  }
}
