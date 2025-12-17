import 'dart:io';
import 'package:discord_rich_presence/discord_rich_presence.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Singleton service for managing Discord Rich Presence on desktop platforms.
/// Shows "Watching [Anime] - Episode X" on the user's Discord profile.
class DiscordService {
  DiscordService._();
  static final DiscordService instance = DiscordService._();

  // Retrieved from .env file
  static String get _clientId => dotenv.env['DISCORD_APP_ID'] ?? '';

  Client? _client;
  bool _isInitialized = false;
  DateTime? _startTime;

  /// Check if we're on a supported desktop platform
  bool get isSupported => Platform.isWindows || Platform.isLinux;

  /// Initialize Discord RPC. Call this once in main() on desktop platforms.
  Future<void> initialize() async {
    if (!isSupported) return;
    if (_isInitialized) return;

    if (_clientId.isEmpty) {
      print('[DiscordService] DISCORD_APP_ID missing in .env');
      return;
    }

    try {
      _client = Client(clientId: _clientId);
      await _client!.connect();
      _isInitialized = true;
      print('[DiscordService] Initialized successfully');
    } catch (e) {
      print('[DiscordService] Failed to initialize: $e');
    }
  }

  /// Update Discord presence to show the user is watching anime.
  ///
  /// [animeTitle] - The title of the anime being watched
  /// [episodeNumber] - The episode number
  /// [coverUrl] - Optional cover image URL (not used directly, but could be for future features)
  Future<void> updatePresence({
    required String animeTitle,
    required int episodeNumber,
    String? coverUrl,
  }) async {
    if (!isSupported || !_isInitialized || _client == null) return;

    _startTime ??= DateTime.now();

    try {
      final activity = Activity(
        name: 'Anisurge',
        details: 'Watching $animeTitle',
        state: 'Episode $episodeNumber',
        timestamps: ActivityTimestamps(
          start: _startTime,
        ),
        assets: ActivityAssets(
          largeImage:
              'large_image', // Must match asset name in Discord Developer Portal
          largeText: 'Anisurge',
          smallImage: 'small_image', // Optional: secondary badge
          smallText: 'Episode $episodeNumber',
        ),
      );

      await _client!.setActivity(activity);
      print(
          '[DiscordService] Updated presence: $animeTitle - Episode $episodeNumber');
    } catch (e) {
      print('[DiscordService] Failed to update presence: $e');
    }
  }

  /// Clear the Discord presence (when user stops watching).
  Future<void> clearPresence() async {
    if (!isSupported || !_isInitialized || _client == null) return;

    try {
      // Set an empty/idle activity to clear presence
      await _client!.setActivity(Activity(name: 'Anisurge'));
      _startTime = null;
      print('[DiscordService] Cleared presence');
    } catch (e) {
      print('[DiscordService] Failed to clear presence: $e');
    }
  }

  /// Shutdown the Discord RPC connection. Call this when the app exits.
  Future<void> shutdown() async {
    if (!isSupported || !_isInitialized || _client == null) return;

    try {
      await _client!.disconnect();
      _isInitialized = false;
      _startTime = null;
      print('[DiscordService] Shutdown complete');
    } catch (e) {
      print('[DiscordService] Failed to shutdown: $e');
    }
  }
}
