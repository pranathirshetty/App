import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

class SettingsService {
  static final SettingsService _instance = SettingsService._internal();
  factory SettingsService() => _instance;
  SettingsService._internal();

  final FlutterSecureStorage _storage = const FlutterSecureStorage();

  // ValueNotifiers for settings
  final ValueNotifier<bool> autoPlay = ValueNotifier<bool>(true);
  final ValueNotifier<bool> autoNext = ValueNotifier<bool>(true);
  final ValueNotifier<String> defaultAudio = ValueNotifier<String>('sub');

  // Keys
  static const String _keyAutoPlay = 'settings_auto_play';
  static const String _keyAutoNext = 'settings_auto_next';
  static const String _keyDefaultAudio = 'settings_default_audio';

  Future<void> loadSettings() async {
    try {
      String? autoPlayStr = await _storage.read(key: _keyAutoPlay);
      if (autoPlayStr != null) {
        autoPlay.value = autoPlayStr == 'true';
      }

      String? autoNextStr = await _storage.read(key: _keyAutoNext);
      if (autoNextStr != null) {
        autoNext.value = autoNextStr == 'true';
      }

      String? defaultAudioStr = await _storage.read(key: _keyDefaultAudio);
      if (defaultAudioStr != null) {
        defaultAudio.value = defaultAudioStr;
      }
    } catch (e) {
      debugPrint('Error loading settings: $e');
    }
  }

  Future<void> setAutoPlay(bool value) async {
    autoPlay.value = value;
    await _storage.write(key: _keyAutoPlay, value: value.toString());
  }

  Future<void> setAutoNext(bool value) async {
    autoNext.value = value;
    await _storage.write(key: _keyAutoNext, value: value.toString());
  }

  Future<void> setDefaultAudio(String value) async {
    defaultAudio.value = value;
    await _storage.write(key: _keyDefaultAudio, value: value);
  }
}
