import 'dart:io';
import 'dart:math';
import 'package:discord_rich_presence/discord_rich_presence.dart';
import 'package:flutter_dotenv/flutter_dotenv.dart';

/// Singleton service for managing Discord Rich Presence on desktop platforms.
/// Shows "Watching [Anime] - Episode X" or funny idle statuses.
class DiscordService {
  DiscordService._();
  static final DiscordService instance = DiscordService._();

  // Retrieved from .env file
  static String get _clientId => dotenv.env['DISCORD_APP_ID'] ?? '';

  Client? _client;
  bool _isInitialized = false;
  DateTime? _startTime;

  final List<String> _idleDetails = [
    "Touching Grace",
    "Searching for the One Piece",
    "Practicing Rasengan",
    "Hunting Curses",
    "In the Infinite Tsukuyomi",
    "Dodge rolling IRL",
    "Waiting for the next arc",
    "Running from Titans",
    "Collecting Dragon Balls",
    "Cooking Ramen",
    "Exploring the Abyss",
    "Awakening a Stand",
    "Training with Saitama",
  ];

  final List<String> _idleStates = [
    "Avoiding responsibilities",
    "Procrastinating",
    "Just chilling",
    "Thinking about anime",
    "Lost in the path of life",
    "Simping for 2D characters",
    "Vibing",
  ];

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
      _startTime = DateTime.now(); // Set application start time
      print('[DiscordService] Initialized successfully');

      // Set initial idle presence
      await setIdlePresence();
    } catch (e) {
      print('[DiscordService] Failed to initialize: $e');
    }
  }

  /// Update Discord presence to show the user is watching anime.
  Future<void> updatePresence({
    required String animeTitle,
    required int episodeNumber,
    String? coverUrl,
  }) async {
    if (!isSupported || !_isInitialized || _client == null) return;

    try {
      final activity = Activity(
        name: 'Anisurge',
        details: 'Watching $animeTitle',
        state: 'Episode $episodeNumber',
        timestamps: ActivityTimestamps(
          start: _startTime,
        ),
        assets: ActivityAssets(
          largeImage: 'large_image',
          largeText: 'Anisurge',
          smallImage: 'small_image',
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

  /// Set a random funny idle presence.
  Future<void> setIdlePresence() async {
    if (!isSupported || !_isInitialized || _client == null) return;

    final random = Random();
    final detail = _idleDetails[random.nextInt(_idleDetails.length)];
    final state = _idleStates[random.nextInt(_idleStates.length)];

    try {
      final activity = Activity(
        name: 'Anisurge',
        details: detail,
        state: state,
        timestamps: ActivityTimestamps(
          start: _startTime,
        ),
        assets: ActivityAssets(
          largeImage: 'large_image',
          largeText: 'Anisurge',
        ),
      );

      await _client!.setActivity(activity);
      print('[DiscordService] Set idle presence: $detail - $state');
    } catch (e) {
      print('[DiscordService] Failed to set idle presence: $e');
    }
  }

  /// Clear the Discord presence (when user stops watching).
  /// Now reverts to idle presence instead of clearing completely.
  Future<void> clearPresence() async {
    await setIdlePresence();
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
