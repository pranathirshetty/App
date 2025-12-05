import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:kuudere/services/notification.dart';
import 'package:kuudere/splash_screen.dart';
import 'package:media_kit/media_kit.dart';
import 'package:fvp/fvp.dart' as fvp;

void main() async {
  WidgetsFlutterBinding.ensureInitialized(); // Add this line

  // Initialize media_kit (ensure bundled native libs are loaded on all platforms)
  MediaKit.ensureInitialized();

  // Register fvp (FFmpeg video player) for all platforms
  // This provides consistent video playback and subtitle rendering across all platforms
  if (!kIsWeb) {
    fvp.registerWith(options: {
      'platforms': ['linux', 'windows', 'macos', 'android', 'ios'],
      // Enable subtitle font for ASS/SSA subtitles
      'subtitleFontFile':
          'https://github.com/mpv-android/mpv-android/raw/master/app/src/main/assets/subfont.ttf',
    });
  }

  // Initialize notification service
  final notificationService = NotificationService();
  await notificationService.initialize();

  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      debugShowCheckedModeBanner: false,
      themeMode: ThemeMode.dark,
      darkTheme: ThemeData.dark().copyWith(
        scaffoldBackgroundColor: Color(0xFF141217),
        textSelectionTheme: TextSelectionThemeData(
          cursorColor: Colors.white,
          selectionColor: Colors.white.withValues(alpha: 0.3),
          selectionHandleColor: Colors.white,
        ),
      ),
      home: SplashScreen(),
    );
  }
}
