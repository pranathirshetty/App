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

  // Subtitle settings
  final ValueNotifier<double> subtitleSize = ValueNotifier<double>(22.0);
  final ValueNotifier<double> subtitleDelay = ValueNotifier<double>(0.0);
  final ValueNotifier<double> subtitlePos = ValueNotifier<double>(90.0);

  // Keys
  static const String _keyAutoPlay = 'settings_auto_play';
  static const String _keyAutoNext = 'settings_auto_next';
  static const String _keyDefaultAudio = 'settings_default_audio';
  static const String _keySubtitleSize = 'settings_subtitle_size';
  static const String _keySubtitleDelay = 'settings_subtitle_delay';
  static const String _keySubtitlePos = 'settings_subtitle_pos';

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

      // Load subtitle settings
      String? subtitleSizeStr = await _storage.read(key: _keySubtitleSize);
      if (subtitleSizeStr != null) {
        subtitleSize.value = double.tryParse(subtitleSizeStr) ?? 22.0;
      }

      String? subtitleDelayStr = await _storage.read(key: _keySubtitleDelay);
      if (subtitleDelayStr != null) {
        subtitleDelay.value = double.tryParse(subtitleDelayStr) ?? 0.0;
      }

      String? subtitlePosStr = await _storage.read(key: _keySubtitlePos);
      if (subtitlePosStr != null) {
        subtitlePos.value = double.tryParse(subtitlePosStr) ?? 90.0;
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

  Future<void> setSubtitleSize(double value) async {
    subtitleSize.value = value;
    await _storage.write(key: _keySubtitleSize, value: value.toString());
  }

  Future<void> setSubtitleDelay(double value) async {
    subtitleDelay.value = value;
    await _storage.write(key: _keySubtitleDelay, value: value.toString());
  }

  Future<void> setSubtitlePos(double value) async {
    subtitlePos.value = value;
    await _storage.write(key: _keySubtitlePos, value: value.toString());
  }
}
