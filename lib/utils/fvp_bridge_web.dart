import 'package:video_player/video_player.dart';

class FvpBridge {
  static void registerWith({Map<String, dynamic>? options}) {
    // No-op for web
  }

  static void setGlobalOption(String key, String value) {
    // No-op for web
  }
}

extension FvpWebExtension on VideoPlayerController {
  Future<void> setProperty(String key, String value) async {
    // No-op for web
  }

  Future<void> setExternalSubtitle(String url) async {
    // No-op for web
  }
}
