import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:kuudere/utils/app_fonts.dart';

import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/settings_service.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import 'comment_sheet.dart';
import 'package:kuudere/services/http_service.dart';
import 'package:socket_io_client/socket_io_client.dart' as socket_io;
import 'package:kuudere/widgets/app_header.dart';
import 'package:kuudere/theme/app_theme.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';
import 'widgets/video_settings_overlay.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:path_provider/path_provider.dart';
import 'package:http/http.dart' as http;

import 'package:kuudere/widgets/custom_dropdown.dart';
import 'package:window_manager/window_manager.dart';
import 'package:video_player/video_player.dart' as vp;
import 'package:kuudere/widgets/fvp_video_controls.dart';
import 'package:fvp/fvp.dart'; // Import without alias for VideoPlayerController extensions
import 'package:fvp/mdk.dart'
    as mdk; // For setGlobalOption (subtitle fonts dir)
import 'package:kuudere/utils/subtitle_utils.dart';

class WatchAnimeScreen extends StatefulWidget {
  final String id;
  final int? episodeNumber; // Now nullable
  final String? nid;
  final String? lang;
  final String? ntype;

  const WatchAnimeScreen({
    super.key,
    required this.id,
    this.episodeNumber, // Nullable now
    this.nid,
    this.lang,
    this.ntype,
  });

  @override
  State<WatchAnimeScreen> createState() => _WatchAnimeScreenState();
}

class _WatchAnimeScreenState extends State<WatchAnimeScreen> {
  bool isLoading = true;
  bool isLoadingVideo = false;
  int _currentPageStart = 1;
  final int _episodesPerPage = 100;
  final TextEditingController _searchController = TextEditingController();
  final ScrollController _episodeScrollController = ScrollController();
  final FlutterSecureStorage _secureStorage = const FlutterSecureStorage();
  String? _preferredLang;
  String _searchQuery = '';
  Map<String, dynamic> animeData = {};
  bool isLoadingEpisodes = true;
  Map<String, dynamic> episodeData = {};
  Map<String, dynamic>? currentSelectedServer;
  late socket_io.Socket socket;
  String currentRoom = '';
  int roomCount = 0;
  int previousRoomCount = 0;
  bool isCountIncreasing = false;
  bool isCountDecreasing = false;
  Timer? countAnimationTimer;
  List<Comment> comments = [];
  Timer? _progressTimer;
  String? _selectedEpisodeId;
  int? _currentEpisodeNumber; // Introduced _currentEpisodeNumber
  Map<String, String> _thumbnails = {}; // Store thumbnails
  List<dynamic> _availableSubtitles = [];
  Map<String, dynamic>? _currentSubtitle;
  String _currentServer = 'hiya'; // Default server
  List<Map<String, dynamic>> _availableQualities = [];
  String? _currentQuality;
  double _playbackSpeed = 1.0;

  // Subtitle Settings
  double _subtitleSize = 58.0;
  Color _subtitleColor = Colors.white;
  Color _subtitleBackgroundColor = Colors.black.withValues(alpha: 0.5);
  double _subtitleDelay = 0.0;
  double _subtitlePos = 100.0;

  // Settings Overlay State
  bool _showSettingsOverlay = false;
  Offset? _settingsAnchor;
  SettingsView _settingsInitialView = SettingsView.main;

  // Media Kit player (kept for potential fallback, may be removed in future)
  late final Player _player;
  late final VideoController _videoController;

  // Universal video player using video_player + fvp backend (all platforms)
  vp.VideoPlayerController? _fvpVideoController;
  bool _fvpVideoInitialized = false;

  final authService = AuthService();

  // Side Panel & Fullscreen State
  String? _activeSidePanel;
  Timer? _saveProgressTimer;
  bool _isCustomFullscreen = false;

  @override
  void dispose() {
    _progressTimer?.cancel();
    _saveProgressTimer?.cancel();
    _searchController.dispose();

    // Restore system UI overlays if we were in fullscreen
    SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
    SystemChrome.setPreferredOrientations([
      DeviceOrientation.portraitUp,
      DeviceOrientation.portraitDown,
      DeviceOrientation.landscapeLeft,
      DeviceOrientation.landscapeRight,
    ]);

    // Dispose Linux video controller if exists
    _fvpVideoController?.dispose();
    _player.dispose();
    super.dispose();
    // socket.dispose();
  }

  void _toggleSidePanel(String? panel) {
    setState(() {
      _activeSidePanel = panel;
    });
  }

  void _toggleFullscreen() async {
    setState(() {
      _isCustomFullscreen = !_isCustomFullscreen;
    });

    if (_isCustomFullscreen) {
      // Enter fullscreen
      if (Platform.isLinux || Platform.isWindows || Platform.isMacOS) {
        // Desktop: Use window_manager for true fullscreen
        await windowManager.setFullScreen(true);
      } else {
        // Mobile: Use system UI mode
        SystemChrome.setEnabledSystemUIMode(SystemUiMode.immersiveSticky);
        SystemChrome.setPreferredOrientations([
          DeviceOrientation.landscapeLeft,
          DeviceOrientation.landscapeRight,
        ]);
      }
    } else {
      // Exit fullscreen
      if (Platform.isLinux || Platform.isWindows || Platform.isMacOS) {
        // Desktop: Exit window fullscreen
        await windowManager.setFullScreen(false);
      } else {
        // Mobile: Restore system UI
        SystemChrome.setEnabledSystemUIMode(SystemUiMode.edgeToEdge);
        SystemChrome.setPreferredOrientations([
          DeviceOrientation.portraitUp,
          DeviceOrientation.portraitDown,
        ]);
      }
      // Also close side panel when exiting fullscreen
      _activeSidePanel = null;
    }
  }

  @override
  void initState() {
    super.initState();
    _currentEpisodeNumber =
        widget.episodeNumber; // Initialize _currentEpisodeNumber

    // Load persistent subtitle settings
    _loadSubtitleSettings();

    _initializePlayer(); // Extracted player initialization
    _loadPreferredLang().then((_) {
      if (_currentEpisodeNumber == null || _currentEpisodeNumber == 0) {
        fetchDefaultEpisodeNumber();
      } else {
        initializeScreen(_currentEpisodeNumber!);
      }
    });
  }

  void _loadSubtitleSettings() {
    final settings = SettingsService();
    _subtitleSize = settings.subtitleSize.value;
    _subtitleDelay = settings.subtitleDelay.value;
    _subtitlePos = settings.subtitlePos.value;
  }

  void _initializePlayer() {
    final bool isLinux = Platform.isLinux;
    _player = Player();
    _videoController = VideoController(
      _player,
      configuration: VideoControllerConfiguration(
        // Disable hardware acceleration on Linux (causes blue screen)
        enableHardwareAcceleration: !isLinux,
        androidAttachSurfaceAfterVideoParameters:
            Platform.isAndroid ? false : true,
      ),
    );
  }

  Future<String?> _getUserId() async {
    final sessionInfo = await authService.getStoredSession();
    return sessionInfo
        ?.userId; // Adjust this if your session object has a different structure
  }

  Future<void> _shareAnime() async {
    try {
      String? userId = await _getUserId();
      userId ??= "guest"; // Fallback if user ID is unavailable

      String animeTitle = animeData['anime_info']['english'] ?? "Unknown Anime";
      String episodeNumber =
          _currentEpisodeNumber?.toString() ?? "1"; // Use _currentEpisodeNumber
      String shareUrl =
          "https://kuudere.to/watch/${widget.id}/$episodeNumber?ref=$userId";

      String message = "Watch $animeTitle - Episode $episodeNumber\n$shareUrl";

      await Share.share(message);
    } catch (e) {
      // print("Error sharing anime: $e");
    }
  }

  Future<void> _loadPreferredLang() async {
    String? storedLang = await _secureStorage.read(key: "preferredLang");
    setState(() {
      _preferredLang = storedLang;
    });
  }

  void fetchDefaultEpisodeNumber() async {
    final String url = 'https://kuudere.to/watch/${widget.id}';
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    try {
      if (sessionInfo == null) {
        throw Exception('No session information found.');
      }

      final httpService = HttpService();
      final response = await httpService
          .get(url.replaceFirst('https://kuudere.to', ''), requireAuth: true);

      if (response.statusCode == 200) {
        final Map<String, dynamic> data = json.decode(response.body);
        if (data.containsKey('anime_info') &&
            data['anime_info'].containsKey('ep')) {
          int defaultEpisode = data['anime_info']['ep'];

          setState(() {
            _currentEpisodeNumber = defaultEpisode;
          });

          initializeScreen(defaultEpisode);
        }
      } else {
        throw Exception('Failed to fetch default episode number');
      }
    } catch (e) {
      // print('Error fetching default episode: $e');
    }
  }

  void initializeScreen(int episodeNumber) {
    fetchAnimeData(widget.nid);
    // _connectSocket();
    fetchEpisodeData(episodeNumber).then((_) async {
      if (episodeData['episode_links'] != null) {
        final selectedServer = _selectServer(episodeData['episode_links']);
        if (selectedServer != null) {
          setState(() {
            currentSelectedServer = selectedServer;
          });
          // print('Initial selected server dataLink: ${selectedServer['dataLink']}');
          await _loadVideo(selectedServer['dataLink']);
        }
      }

      // Ensure scrolling happens after the UI is built
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _scrollToCurrentEpisode(episodeNumber);
      });
    });

    _startSaveProgressTimer();
  }

  void _startSaveProgressTimer() {
    _saveProgressTimer?.cancel();
    _saveProgressTimer = Timer.periodic(const Duration(seconds: 5), (timer) {
      // Check if video is playing (using fvp controller on all platforms)
      bool isPlaying = false;
      if (_fvpVideoController != null) {
        isPlaying = _fvpVideoController!.value.isPlaying;
      }

      if (isPlaying) {
        _saveProgress();
      }
    });
  }

  Future<void> _saveProgress() async {
    if (_selectedEpisodeId == null || episodeData.isEmpty) return;

    try {
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo == null) return;

      double position;
      double duration;

      // Using fvp controller on all platforms
      if (_fvpVideoController != null) {
        position = _fvpVideoController!.value.position.inSeconds.toDouble();
        duration = _fvpVideoController!.value.duration.inSeconds.toDouble();
      } else {
        return; // No controller available
      }

      if (duration <= 0) return;

      final httpService = HttpService();
      print(
          'Saving progress: Anime: ${widget.id}, Ep: $_selectedEpisodeId, Time: $position / $duration');

      await httpService.post(
        '/api/save-progress',
        body: {
          'animeId': widget.id,
          'episodeId': _selectedEpisodeId,
          'currentTime': position,
          'duration': duration,
          'server': _currentServer,
          'language':
              'sub', // Defaulting to sub for now, can be dynamic if needed
        },
        requireAuth: true,
      );
    } catch (e) {
      // print('Error saving progress: $e');
    }
  }

  /*
  void _connectSocket() {
    socket = socket_io.io('https://api.kuudere.to', <String, dynamic>{
      'transports': ['websocket'],
      'autoConnect': true,
      'reconnection': true,
      'reconnectionDelay': 1000,
      'reconnectionDelayMax': 5000,
      'reconnectionAttempts': 5
    });

    socket.onConnect((_) {
      // print('Connected to socket server');
      _joinRoom();
    });

    socket.onDisconnect((_) {
      // print('Disconnected from socket server');
    });

    // Debug socket connection status
    socket.onConnectError((error) => null); // print('Connect Error: $error')
    socket.onError((error) => null); // print('Socket Error: $error')

    socket.on('current_room_count', (data) {
      // print('Received room count: $data'); // Debug print
      if (mounted) {
        setState(() {
          if (data is Map &&
              data.containsKey('count') &&
              data['room'] == currentRoom) {
            previousRoomCount = roomCount;
            roomCount = data['count'] ?? 0;

            if (roomCount > previousRoomCount) {
              _animateCountChange('up');
            } else if (roomCount < previousRoomCount) {
              _animateCountChange('down');
            }
            // print('Updated room count: $roomCount'); // Debug print
          }
        });
      }
    });
  }
  */

  /*
  void _joinRoom() {
    if (currentRoom.isNotEmpty) {
      // print('Leaving room: $currentRoom'); // Debug print
      socket.emit('leave', {'room': currentRoom});
    }

    setState(() {
      currentRoom = '/watch/${widget.id}/';
    });

    // print('Joining room: $currentRoom'); // Debug print
    socket.emit('join', {'other_id': currentRoom});
    socket.emit('get_current_room_count', {'room': currentRoom});
  }
  */

  // Add these helper methods:
  String _formatCount(int count) {
    if (count > 999) {
      return '${(count / 1000).toStringAsFixed(1)}k';
    }
    return count.toString();
  }

  /*
  void _animateCountChange(String direction) {
    setState(() {
      isCountIncreasing = direction == 'up';
      isCountDecreasing = direction == 'down';
    });

    countAnimationTimer?.cancel();
    countAnimationTimer = Timer(const Duration(milliseconds: 300), () {
      if (mounted) {
        setState(() {
          isCountIncreasing = false;
          isCountDecreasing = false;
        });
      }
    });
  }
  */

  Future<void> _handleAnimeResponse(String type) async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    try {
      if (sessionInfo == null) {
        throw Exception('No session information found.');
      }

      final httpService = HttpService();
      // Backend endpoint is /api/anime/like, expects animeId and type in body
      final response = await httpService.post(
        '/api/anime/like',
        body: {
          "animeId": widget.id, // Backend expects animeId in body, not in URL
          "type": type, // 'like' or 'dislike'
        },
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        // Update the UI state based on response
        setState(() {
          final animeInfo = animeData['anime_info'];
          if (type == 'like') {
            if (animeInfo['userLiked']) {
              // If already liked, remove like
              animeInfo['likes']--;
              animeInfo['userLiked'] = false;
            } else {
              // Add like and remove dislike if exists
              animeInfo['likes']++;
              if (animeInfo['userUnliked']) {
                animeInfo['dislikes']--;
                animeInfo['userUnliked'] = false;
              }
              animeInfo['userLiked'] = true;
            }
          } else if (type == 'dislike') {
            if (animeInfo['userUnliked']) {
              // If already disliked, remove dislike
              animeInfo['dislikes']--;
              animeInfo['userUnliked'] = false;
            } else {
              // Add dislike and remove like if exists
              animeInfo['dislikes']++;
              if (animeInfo['userLiked']) {
                animeInfo['likes']--;
                animeInfo['userLiked'] = false;
              }
              animeInfo['userUnliked'] = true;
            }
          }
        });
      } else {
        throw Exception('Failed to update response');
      }
    } catch (e) {
      // print('Error updating response: $e');
      // You might want to show an error message to the user here
    }
  }

  void _showWatchlistBottomSheet() {
    showModalBottomSheet(
      context: context,
      backgroundColor: Color(0xFF1E1E1E),
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (context) {
        return StatefulBuilder(
          builder: (context, setStateBottomSheet) {
            String? selectedStatus = animeData['anime_info']['folder'];
            String? updatingStatus; // Track which item is updating

            Future<void> handleSelection(String status) async {
              setStateBottomSheet(() {
                updatingStatus = status; // Show animation on selected status
              });

              await _updateWatchlistStatus(status);

              setStateBottomSheet(() {
                selectedStatus = status != 'Remove' ? status : null;
                updatingStatus = null; // Reset after request is complete
              });
            }

            return Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Center(
                    child: Container(
                      width: 40,
                      height: 4,
                      decoration: BoxDecoration(
                        color: Colors.grey[600],
                        borderRadius: BorderRadius.circular(2),
                      ),
                    ),
                  ),
                  const SizedBox(height: 20),
                  Text(
                    "Add to Watchlist",
                    style: AppFonts.poppins(
                      color: Colors.white,
                      fontSize: 24,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                  const SizedBox(height: 16),
                  Column(
                    children: [
                      _buildWatchlistOption("Watching", selectedStatus,
                          updatingStatus, handleSelection, setStateBottomSheet),
                      _buildWatchlistOption("On Hold", selectedStatus,
                          updatingStatus, handleSelection, setStateBottomSheet),
                      _buildWatchlistOption("Plan To Watch", selectedStatus,
                          updatingStatus, handleSelection, setStateBottomSheet),
                      _buildWatchlistOption("Dropped", selectedStatus,
                          updatingStatus, handleSelection, setStateBottomSheet),
                      _buildWatchlistOption("Completed", selectedStatus,
                          updatingStatus, handleSelection, setStateBottomSheet),
                      Divider(color: Colors.grey[800], height: 32),
                      _buildWatchlistOption("Remove", selectedStatus,
                          updatingStatus, handleSelection, setStateBottomSheet),
                    ],
                  ),
                ],
              ),
            );
          },
        );
      },
    );
  }

  Widget _buildWatchlistOption(
    String status,
    String? selectedStatus,
    String? updatingStatus,
    Function(String) onTap,
    StateSetter setStateBottomSheet,
  ) {
    final isSelected = selectedStatus == status;
    final isUpdating = updatingStatus == status;

    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: InkWell(
        onTap: () => onTap(status),
        borderRadius: BorderRadius.circular(12),
        child: Ink(
          decoration: BoxDecoration(
            gradient: isSelected
                ? LinearGradient(
                    colors: [Color(0xFF6A3093), Color(0xFFA044FF)],
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                  )
                : null,
            color: isSelected ? null : Color(0xFF2A2A2A),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Padding(
            padding: EdgeInsets.symmetric(horizontal: 16, vertical: 12),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Text(
                  status,
                  style: AppFonts.poppins(
                    color: isSelected ? Colors.white : Colors.grey[300],
                    fontSize: 16,
                    fontWeight:
                        isSelected ? FontWeight.bold : FontWeight.normal,
                  ),
                ),
                if (isUpdating)
                  LoadingAnimationWidget.threeArchedCircle(
                    color: isSelected ? Colors.white : Color(0xFFA044FF),
                    size: 24,
                  )
                else if (isSelected)
                  Icon(Icons.check, color: Colors.white),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Future<void> _updateWatchlistStatus(String newStatus) async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    if (sessionInfo == null) return;

    try {
      final httpService = HttpService();
      // Use the correct endpoint and field names that match SvelteKit backend
      final response = await httpService.post(
        '/api/anime/watchlist',
        body: {
          'animeId': widget.id, // Backend expects camelCase
          'folder': newStatus, // Backend expects 'folder' not 'status'
        },
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        setState(() {
          animeData['anime_info']['inWatchlist'] = newStatus != 'Remove';
          animeData['anime_info']['folder'] =
              newStatus != 'Remove' ? newStatus : null;
        });
      } else {
        // print('Failed to update watchlist. Status code: ${response.statusCode}');
      }
    } catch (e) {
      // print('Error updating watchlist: $e');
    }
  }

  Widget _buildViewCount() {
    return AnimatedDefaultTextStyle(
      duration: const Duration(milliseconds: 300),
      style: TextStyle(
        color: isCountIncreasing
            ? Colors.green
            : isCountDecreasing
                ? AppTheme.primary
                : Colors.white.withValues(alpha: 0.7),
        fontSize: 14,
      ),
      child: Text(
        roomCount > 0 ? '${_formatCount(roomCount)} watching' : 'Connecting...',
      ),
    );
  }

  Future<void> fetchEpisodeData(int episodeNumber) async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();
    setState(() {
      isLoadingEpisodes = true;
    });

    try {
      final httpService = HttpService();
      final response = await httpService.get(
        '/api/watch/${widget.id}/$episodeNumber',
        requireAuth: sessionInfo != null,
      );

      if (response.statusCode == 200) {
        setState(() {
          episodeData = json.decode(response.body);
          isLoadingEpisodes = false;
          _currentEpisodeNumber =
              episodeNumber; // Update _currentEpisodeNumber here

          if (episodeData['anime_info'] != null &&
              episodeData['anime_info']['anilist'] != null) {
            fetchThumbnails(episodeData['anime_info']['anilist']);
          }

          if (episodeData['all_episodes'] != null) {
            // Store episode ID for tracking
            final episodes = episodeData['all_episodes'] as List<dynamic>;
            final currentEpisode = episodes.firstWhere(
              (episode) => episode['number'] == episodeNumber,
              orElse: () => null,
            );

            _selectedEpisodeId =
                currentEpisode?['id'] ?? episodeData['episode_id'];
          }

          if (episodeData['current'] != null && episodeData['current'] != 0) {
            final savedPosition = episodeData['current'] as int;
            // Seek to saved position once video is loaded
            _player.stream.duration.listen((duration) {
              if (duration != Duration.zero && savedPosition > 0) {
                _player.seek(Duration(seconds: savedPosition));
              }
            });
          }
        });

        // Scroll to the selected episode
        WidgetsBinding.instance.addPostFrameCallback((_) {
          _scrollToCurrentEpisode(episodeNumber);
        });
      } else {
        throw Exception('Failed to load episode data');
      }
    } catch (e) {
      // print('Error loading episode data: $e');
      setState(() {
        isLoadingEpisodes = false;
      });
    }
  }

  void _scrollToCurrentEpisode(int episodeNumber) {
    if (!mounted || _episodeScrollController.positions.isEmpty) {
      return; // Prevent crashes
    }

    final index = episodeData['all_episodes']
        ?.indexWhere((episode) => episode['number'] == episodeNumber);

    if (index != null && index >= 0 && _episodeScrollController.hasClients) {
      _episodeScrollController.animateTo(
        index * 80.0, // Adjust based on item height
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
      );
    }
  }

  Future<void> fetchAnimeData(String? nid) async {
    final String url = nid != null
        ? 'https://kuudere.to/watch/${widget.id}/${_currentEpisodeNumber ?? widget.episodeNumber}?nid=$nid' // Use _currentEpisodeNumber
        : 'https://kuudere.to/watch/${widget.id}/${_currentEpisodeNumber ?? widget.episodeNumber}'; // Use _currentEpisodeNumber

    final httpService = HttpService();
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();
    try {
      final endpoint = url.replaceFirst('https://kuudere.to', '');
      final response = await httpService.get(
        endpoint,
        requireAuth: sessionInfo != null,
      );

      if (response.statusCode == 200) {
        setState(() {
          animeData = json.decode(response.body);
          isLoading = false;
        });
      } else {
        throw Exception('Failed to load anime data');
      }
    } catch (e) {
      // print('Error: $e');
      setState(() {
        isLoading = false;
      });
    }
  }

  Future<void> fetchThumbnails(int anilistId) async {
    final httpService = HttpService();
    try {
      final response = await httpService.get('/api/thumbnails/$anilistId');

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        if (data['success'] == true && data['thumbnails'] != null) {
          setState(() {
            _thumbnails = Map<String, String>.from(data['thumbnails']);
          });
        }
      }
    } catch (e) {
      // print('Error fetching thumbnails: $e');
    }
  }

  void _showReportBottomSheet() {
    String? selectedIssue;
    TextEditingController feedbackController = TextEditingController();
    bool isSubmitting = false;

    showModalBottomSheet(
      context: context,
      backgroundColor: Colors.transparent,
      isScrollControlled: true,
      builder: (sheetContext) {
        return StatefulBuilder(
          builder: (sbContext, setStateBottomSheet) {
            return DraggableScrollableSheet(
              initialChildSize: 0.6, // Start at 60% of screen height
              minChildSize: 0.3, // Allow to be dragged down to 30%
              maxChildSize: 0.9, // Allow to expand up to 90%
              builder: (_, controller) {
                return Container(
                  decoration: BoxDecoration(
                    color: Colors.grey[900],
                    borderRadius:
                        const BorderRadius.vertical(top: Radius.circular(20)),
                  ),
                  child: Column(
                    children: [
                      // Drag handle and close button
                      Container(
                        padding: const EdgeInsets.symmetric(
                            horizontal: 16, vertical: 8),
                        child: Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            IconButton(
                              icon:
                                  const Icon(Icons.close, color: Colors.white),
                              onPressed: () => Navigator.pop(sheetContext),
                            ),
                            Container(
                              width: 50,
                              height: 5,
                              decoration: BoxDecoration(
                                color: Colors.grey[700],
                                borderRadius: BorderRadius.circular(2.5),
                              ),
                            ),
                            const SizedBox(
                                width: 48), // Balance the close button
                          ],
                        ),
                      ),
                      // Content
                      Expanded(
                        child: ListView(
                          controller: controller,
                          padding: const EdgeInsets.all(24),
                          children: [
                            // Title Section
                            const Text(
                              "Report an Issue",
                              style: TextStyle(
                                color: Colors.white,
                                fontSize: 24,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 16),
                            const Divider(color: Colors.grey),
                            const SizedBox(height: 16),

                            // Issue Options (Radio Buttons)
                            ...[
                              "Buffering",
                              "Request Sub",
                              "Request Dub",
                              "Fail To Fetch Streaming Link",
                              "Streaming Servers Are Missing",
                              "Other"
                            ].map((issue) {
                              return Padding(
                                padding: const EdgeInsets.only(bottom: 8),
                                child: AnimatedContainer(
                                  duration: const Duration(milliseconds: 200),
                                  decoration: BoxDecoration(
                                    color: selectedIssue == issue
                                        ? Colors.white.withValues(alpha: 0.5)
                                        : Colors.transparent,
                                    borderRadius: BorderRadius.circular(8),
                                  ),
                                  child: RadioListTile<String>(
                                    title: Text(issue,
                                        style: const TextStyle(
                                            color: Colors.white)),
                                    value: issue,
                                    // ignore: deprecated_member_use
                                    groupValue: selectedIssue,
                                    activeColor: AppTheme.primary,
                                    // ignore: deprecated_member_use
                                    onChanged: (value) {
                                      setStateBottomSheet(() {
                                        selectedIssue = value;
                                      });
                                    },
                                  ),
                                ),
                              );
                            }),

                            // If "Other" is selected, show text field for feedback
                            if (selectedIssue == "Other") ...[
                              const SizedBox(height: 16),
                              AnimatedOpacity(
                                opacity: selectedIssue == "Other" ? 1.0 : 0.0,
                                duration: const Duration(milliseconds: 300),
                                child: TextField(
                                  controller: feedbackController,
                                  maxLength: 250,
                                  maxLines: 3,
                                  style: const TextStyle(color: Colors.white),
                                  decoration: InputDecoration(
                                    hintText: "Describe your issue...",
                                    hintStyle:
                                        const TextStyle(color: Colors.white70),
                                    filled: true,
                                    fillColor: Colors.grey[800],
                                    border: OutlineInputBorder(
                                      borderRadius: BorderRadius.circular(8),
                                      borderSide: BorderSide.none,
                                    ),
                                    focusedBorder: OutlineInputBorder(
                                      borderRadius: BorderRadius.circular(8),
                                      borderSide: const BorderSide(
                                          color: AppTheme.primary),
                                    ),
                                  ),
                                ),
                              ),
                            ],

                            // Show the notice if episode released less than 1 hour ago
                            if (_currentEpisodeNumber != null &&
                                _getEpisodeTimeInMinutes(
                                        _currentEpisodeNumber!) <
                                    60) ...[
                              // Use _currentEpisodeNumber
                              const SizedBox(height: 24),
                              Container(
                                padding: const EdgeInsets.all(16),
                                decoration: BoxDecoration(
                                  color: Colors.amber.withValues(alpha: 0.1),
                                  borderRadius: BorderRadius.circular(8),
                                ),
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    const Text(
                                      "New Episode Notice",
                                      style: TextStyle(
                                          color: Colors.amber,
                                          fontWeight: FontWeight.bold,
                                          fontSize: 16),
                                    ),
                                    const SizedBox(height: 8),
                                    const Text(
                                      "This episode was recently released. It may take some time to become available here. In the meantime, you can watch it on our website.",
                                      style: TextStyle(
                                          color: Colors.white, fontSize: 14),
                                    ),
                                    const SizedBox(height: 16),
                                    ElevatedButton.icon(
                                      icon: const Icon(Icons.open_in_new,
                                          color: Colors.black),
                                      label: const Text("Watch on our site",
                                          style:
                                              TextStyle(color: Colors.black)),
                                      style: ElevatedButton.styleFrom(
                                        backgroundColor: Colors.amber,
                                        shape: RoundedRectangleBorder(
                                            borderRadius:
                                                BorderRadius.circular(8)),
                                      ),
                                      onPressed: () {
                                        final url =
                                            "https://kuudere.to/watch/${widget.id}/$_currentEpisodeNumber";
                                        launchUrl(Uri.parse(url));
                                      },
                                    ),
                                  ],
                                ),
                              ),
                            ],

                            const SizedBox(height: 24),

                            // Submit Button
                            SizedBox(
                              width: double.infinity,
                              child: AnimatedContainer(
                                duration: const Duration(milliseconds: 300),
                                height: 50,
                                decoration: BoxDecoration(
                                  gradient: LinearGradient(
                                    colors: isSubmitting
                                        ? [Colors.grey, Colors.grey]
                                        : [Colors.red, Colors.redAccent],
                                  ),
                                  borderRadius: BorderRadius.circular(25),
                                ),
                                child: ElevatedButton(
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: Colors.transparent,
                                    shadowColor: Colors.transparent,
                                    shape: RoundedRectangleBorder(
                                        borderRadius:
                                            BorderRadius.circular(25)),
                                  ),
                                  onPressed: isSubmitting
                                      ? null
                                      : () async {
                                          setStateBottomSheet(() {
                                            isSubmitting = true;
                                          });

                                          await _submitReport(selectedIssue,
                                              feedbackController.text);

                                          if (sheetContext.mounted) {
                                            Navigator.pop(
                                                sheetContext); // Close the bottom sheet
                                          }
                                        },
                                  child: isSubmitting
                                      ? LoadingAnimationWidget
                                          .threeArchedCircle(
                                          color: Colors.white,
                                          size: 30,
                                        )
                                      : const Text(
                                          "Submit Report",
                                          style: TextStyle(
                                              color: Colors.white,
                                              fontWeight: FontWeight.bold,
                                              fontSize: 16),
                                        ),
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                );
              },
            );
          },
        );
      },
    );
  }

  int _getEpisodeTimeInMinutes(int episodeNumber) {
    // Get the 'ago' string for the episode
    String ago = _getEpisodeAgo(
        episodeNumber); // _getEpisodeAgo() returns the time in string format

    int minutesAgo = 0;

    // Try to parse it as an integer if it is a number
    try {
      minutesAgo = int.parse(ago);
    } catch (e) {
      // If parsing fails (e.g., 'Unknown time ago'), we assume it's 0
      minutesAgo = 0;
    }

    return minutesAgo;
  }

  Future<void> _submitReport(String? category, String feedback) async {
    if (category == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text("Please select an issue category")),
        );
      }
      return;
    }

    try {
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo == null) {
        throw Exception("User session not found.");
      }

      final httpService = HttpService();
      // Backend expects: animeId, epId, type, explain
      final response = await httpService.post(
        '/api/report',
        body: {
          "animeId": widget.id, // Backend expects camelCase
          "epId": _selectedEpisodeId ??
              '', // Backend expects epId (episode ID) not episode number
          "type": category, // Backend expects 'type' not 'category'
          "explain": category == "Other"
              ? feedback
              : null, // Backend expects 'explain' not 'feedback'
        },
        requireAuth: true,
      );

      final data = jsonDecode(response.body);

      if (response.statusCode == 200 && data['success']) {
        if (!mounted) return;
        Navigator.pop(context);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Report submitted successfully')),
        );
      } else {
        throw Exception(data['message'] ?? "Unknown error");
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text("Error submitting report: $e")),
        );
      }
    }
  }

  @override
  @override
  Widget build(BuildContext context) {
    if (_isCustomFullscreen) {
      return Scaffold(
        backgroundColor: Colors.black,
        body: Platform.isLinux
            ? SizedBox.expand(
                child: _buildVideoPlayer(isFullscreen: true),
              )
            : SafeArea(
                child: _buildVideoPlayer(isFullscreen: true),
              ),
      );
    }

    if (isLoading) {
      return Scaffold(
        backgroundColor: Colors.black,
        body: Center(
          child: LoadingAnimationWidget.threeArchedCircle(
            color: Colors.red,
            size: 50,
          ),
        ),
      );
    }

    final isDesktop = MediaQuery.of(context).size.width > 1000;

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppHeader(
        style: HeaderStyle.transparent,
        showBackButton: true,
      ),
      body: isDesktop ? _buildDesktopLayout() : _buildMobileLayout(),
    );
  }

  Widget _buildMobileLayout() {
    // On Linux with software rendering, keeping video inside ScrollView causes
    // black screen issues. Use a fixed layout instead.
    if (Platform.isLinux) {
      return Column(
        children: [
          _buildVideoPlayer(),
          Expanded(
            child: SingleChildScrollView(
              child: Column(
                children: [
                  _buildMainContent(isDesktop: false),
                  _buildEpisodeListMobile(),
                  _buildAnimeDetailsCard(),
                ],
              ),
            ),
          ),
        ],
      );
    }

    return Stack(
      children: [
        SingleChildScrollView(
          child: Column(
            children: [
              _buildVideoPlayer(),
              _buildMainContent(isDesktop: false),
              _buildEpisodeListMobile(),
              _buildAnimeDetailsCard(),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildDesktopLayout() {
    // On Linux with software rendering, keeping video inside ScrollView causes
    // black screen issues. Use a fixed layout instead.
    if (Platform.isLinux) {
      return Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 3,
            child: Column(
              children: [
                _buildVideoPlayer(),
                Expanded(
                  child: SingleChildScrollView(
                    padding: const EdgeInsets.only(right: 24),
                    child: Column(
                      children: [
                        _buildMainContent(isDesktop: true),
                        _buildAnimeDetailsCard(),
                      ],
                    ),
                  ),
                ),
              ],
            ),
          ),
          Container(
            width: 400,
            decoration: BoxDecoration(
              border: Border(
                left: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
              ),
              color: const Color(0xFF0F0F0F),
            ),
            child: _buildSidebar(),
          ),
        ],
      );
    }

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Expanded(
          flex: 3,
          child: SingleChildScrollView(
            padding: const EdgeInsets.only(right: 24),
            child: Column(
              children: [
                _buildVideoPlayer(),
                _buildMainContent(isDesktop: true),
                _buildAnimeDetailsCard(),
              ],
            ),
          ),
        ),
        Container(
          width: 400,
          decoration: BoxDecoration(
            border: Border(
              left: BorderSide(color: Colors.white.withValues(alpha: 0.1)),
            ),
            color: const Color(0xFF0F0F0F),
          ),
          child: _buildSidebar(),
        ),
      ],
    );
  }

  Widget _buildSidebar() {
    return DefaultTabController(
      length: 2,
      child: Column(
        children: [
          TabBar(
            indicatorColor: Colors.red,
            indicatorSize: TabBarIndicatorSize.tab,
            labelColor: Colors.white,
            unselectedLabelColor: Colors.grey,
            dividerColor: Colors.transparent,
            tabs: const [
              Tab(text: 'Episodes'),
              Tab(text: 'Comments'),
            ],
          ),
          Expanded(
            child: TabBarView(
              children: [
                _buildEpisodeListSidebar(),
                CommentsContent(
                  commentCount: episodeData['total_comments'] ?? 0,
                  episodeData: episodeData,
                  epNumber: _currentEpisodeNumber,
                  animeId: widget.id,
                  comments: comments,
                  updateComments: updateComments,
                  isDesktop: true,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildEpisodeListSidebar() {
    if (isLoadingEpisodes) {
      return Center(
        child: LoadingAnimationWidget.fourRotatingDots(
          color: Colors.red,
          size: 50,
        ),
      );
    }

    final episodes = episodeData['all_episodes'] ?? [];
    final totalEpisodes = episodes.length;
    final pageGroups = _generatePageGroups(totalEpisodes);

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              DropdownButton<int>(
                dropdownColor: Colors.black,
                value: _currentPageStart,
                isExpanded: true,
                items: pageGroups.map((start) {
                  final end = start + _episodesPerPage - 1;
                  return DropdownMenuItem(
                    value: start,
                    child: Text(
                      '$start - $end',
                      style: const TextStyle(color: Colors.white),
                    ),
                  );
                }).toList(),
                onChanged: (value) {
                  setState(() {
                    _currentPageStart = value!;
                  });
                },
                underline: Container(height: 1, color: Colors.white24),
                icon: const Icon(Icons.arrow_drop_down, color: Colors.white),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: _searchController,
                decoration: InputDecoration(
                  hintText: 'Search episode...',
                  hintStyle: const TextStyle(color: Colors.white38),
                  fillColor: Colors.white.withValues(alpha: 0.05),
                  filled: true,
                  isDense: true,
                  contentPadding:
                      const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(8),
                    borderSide: BorderSide.none,
                  ),
                  prefixIcon:
                      const Icon(Icons.search, color: Colors.white38, size: 20),
                ),
                style: const TextStyle(color: Colors.white),
                onChanged: (value) {
                  setState(() {
                    _searchQuery = value.toLowerCase();
                  });
                },
              ),
            ],
          ),
        ),
        Expanded(
          child: ListView.builder(
            controller: _episodeScrollController,
            padding: const EdgeInsets.symmetric(horizontal: 16),
            itemCount: _getFilteredEpisodes(episodes).length,
            itemBuilder: (context, index) {
              final filteredEpisodes = _getFilteredEpisodes(episodes);
              final episode = filteredEpisodes[index];
              final isCurrentEpisode =
                  episode['number'] == _currentEpisodeNumber;

              return Container(
                margin: const EdgeInsets.only(bottom: 8),
                decoration: BoxDecoration(
                  color: isCurrentEpisode
                      ? Colors.red.withValues(alpha: 0.1)
                      : Colors.transparent,
                  border: Border.all(
                    color: isCurrentEpisode
                        ? Colors.red
                        : Colors.white.withValues(alpha: 0.1),
                    width: 1,
                  ),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: ListTile(
                  contentPadding:
                      const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
                  leading: Container(
                    width: 100,
                    height: 60,
                    decoration: BoxDecoration(
                      color: Colors.grey[800],
                      borderRadius: BorderRadius.circular(4),
                      image: _thumbnails
                              .containsKey(episode['number'].toString())
                          ? DecorationImage(
                              image: NetworkImage(
                                  _thumbnails[episode['number'].toString()]!),
                              fit: BoxFit.cover,
                            )
                          : null,
                    ),
                    child: _thumbnails.containsKey(episode['number'].toString())
                        ? Container(
                            color: Colors.black.withValues(alpha: 0.3),
                            child: const Center(
                              child: Icon(
                                Icons.play_circle_outline,
                                color: Colors.white,
                                size: 24,
                              ),
                            ),
                          )
                        : const Center(
                            child: Icon(
                              Icons.play_circle_outline,
                              color: Colors.white70,
                              size: 24,
                            ),
                          ),
                  ),
                  title: Text(
                    '${episode['number']}. ${episode['titles'][0]}',
                    style: TextStyle(
                      color: isCurrentEpisode ? Colors.red : Colors.white,
                      fontWeight: FontWeight.w500,
                      fontSize: 14,
                    ),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                  onTap: () => onEpisodeSelected(episode['number']),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Future<void> _loadVideo(String url, {String? server}) async {
    // Save current position before switching (for server switches)
    Duration? resumePosition;
    if (_fvpVideoController != null && _fvpVideoInitialized) {
      resumePosition = _fvpVideoController!.value.position;
    }

    setState(() {
      isLoadingVideo = true;
    });

    try {
      final httpService = HttpService();
      final anilistId = episodeData['anime_info']?['anilist'];

      if (anilistId == null) {
        throw Exception('Anilist ID not found');
      }

      final serverParam = server ?? _currentServer;
      final endpoint = '/$anilistId/$_currentEpisodeNumber/$serverParam';

      final response = await httpService.get(endpoint);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);

        if (data['success'] == true) {
          // Handle different server response structures
          Map<String, dynamic>? directLinkData;

          if (data.containsKey('directLink') && data['directLink'] != null) {
            // zen/zen2 format
            final directLink = data['directLink'];
            if (directLink is Map && directLink.containsKey('data')) {
              directLinkData = directLink['data'];
            }
          } else if (data.containsKey('data') && data['data'] != null) {
            // pahe/hiya/allmanga format
            directLinkData = data['data'];
          }

          if (directLinkData != null && directLinkData['m3u8_url'] != null) {
            String m3u8Url = directLinkData['m3u8_url'];
            final subtitles = directLinkData['subtitles'] as List<dynamic>?;
            final fonts = directLinkData['fonts'] as List<dynamic>?;
            final sources = directLinkData['sources'] as List<dynamic>?;

            // Clear current subtitle track when changing servers
            try {
              await _player.setSubtitleTrack(SubtitleTrack.no());
            } catch (e) {
              // Ignore errors when clearing
            }

            setState(() {
              _currentServer = serverParam;
              _availableSubtitles = subtitles ?? [];
              _currentSubtitle = null;
              _availableQualities = [];

              if (sources != null && sources.isNotEmpty) {
                // pahe format with multiple quality options
                for (var source in sources) {
                  _availableQualities.add({
                    'url': source['url'],
                    'quality': source['quality'],
                  });
                }
                // Use first quality by default
                m3u8Url = sources[0]['url'];
                _currentQuality = sources[0]['quality'];
              } else {
                _currentQuality = 'Auto';
              }
            });

            // Set mpv properties before loading media
            if (Platform.isLinux ||
                Platform.isWindows ||
                Platform.isMacOS ||
                Platform.isAndroid ||
                Platform.isIOS) {
              try {
                await (_player.platform as dynamic)
                    .setProperty('sub-ass', 'yes');
                await (_player.platform as dynamic)
                    .setProperty('embeddedfonts', 'yes');
                await (_player.platform as dynamic)
                    .setProperty('sub-ass-override', 'scale');
              } catch (e) {
                // print('Error setting mpv properties: $e');
              }
            }

            if (fonts != null && fonts.isNotEmpty) {
              await _loadFonts(fonts);
            }

            // Fetch saved position before playing
            Duration? savedDuration;
            try {
              final httpService = HttpService();
              final historyResponse = await httpService.post(
                '/api/watch/${widget.id}/$_selectedEpisodeId',
                body: {}, // Empty body as per curl request
                requireAuth: true,
              );

              if (historyResponse.statusCode == 200) {
                final historyData = json.decode(historyResponse.body);
                if (historyData['success'] == true &&
                    historyData['data'] != null) {
                  final savedTime = historyData['data']['currentTime'];
                  if (savedTime != null) {
                    savedDuration =
                        Duration(seconds: (savedTime as num).toInt());
                  }
                }
              }
            } catch (e) {
              // print('Error fetching watch history: $e');
            }

            // Use video_player with fvp backend (universal for all platforms)
            // Dispose previous controller if exists
            _fvpVideoController?.dispose();

            _fvpVideoController = vp.VideoPlayerController.networkUrl(
              Uri.parse(m3u8Url),
            );

            await _fvpVideoController!.initialize();

            // Enable ASS subtitle rendering with libass
            try {
              _fvpVideoController!.setProperty('subtitle', '1');
            } catch (e) {
              debugPrint('Error setting subtitle properties: $e');
            }

            _fvpVideoController!.play();

            // Seek to position: prioritize resumePosition (server switch) over savedDuration (watch history)
            final seekPosition = resumePosition ?? savedDuration;
            if (seekPosition != null && seekPosition.inSeconds > 0) {
              await _fvpVideoController!.seekTo(seekPosition);
            }

            setState(() {
              _fvpVideoInitialized = true;
            });

            // Load default subtitle
            if (subtitles != null) {
              for (final sub in subtitles) {
                if (sub['is_default'] == true) {
                  try {
                    final subUrl = sub['url'];

                    // Apply subtitle delay if needed
                    if (_fvpVideoController != null) {
                      final effectiveUrl = await _getDelayedSubtitleUrl(subUrl);
                      _fvpVideoController!.setExternalSubtitle(effectiveUrl);
                      setState(() {
                        _currentSubtitle = sub;
                      });
                    }
                  } catch (e) {
                    debugPrint('Error setting subtitle: $e');
                  }
                  break; // Set only the default one
                }
              }
            }
          } else {
            throw Exception('Failed to get direct link data');
          }
        } else {
          throw Exception('Server returned success=false');
        }
      } else {
        throw Exception('Failed to fetch video data: ${response.statusCode}');
      }

      // Apply subtitle settings
      await _applySubtitleSettings();

      setState(() {
        isLoadingVideo = false;
      });
    } catch (e) {
      // print('Error loading video: $e');
      setState(() {
        isLoadingVideo = false;
      });
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error loading video: $e')),
        );
      }
    }
  }

  void _changeSubtitle(Map<String, dynamic>? subtitle) async {
    if (subtitle == null) {
      await _player.setSubtitleTrack(SubtitleTrack.no());
      setState(() {
        _currentSubtitle = null;
      });
    } else {
      try {
        final subUrl = subtitle['url'];
        final format = subtitle['format'] ?? 'srt';

        if (format == 'ass') {
          // Download ASS file to preserve formatting
          final tempDir = await getTemporaryDirectory();
          final subFile = File(
              '${tempDir.path}/subtitle_${DateTime.now().millisecondsSinceEpoch}.ass');

          final response = await http.get(Uri.parse(subUrl));
          if (response.statusCode == 200) {
            await subFile.writeAsBytes(response.bodyBytes);

            await _player.setSubtitleTrack(
              SubtitleTrack.uri(
                subFile.path,
                title: subtitle['title'],
                language: subtitle['language'],
              ),
            );
          }
        } else {
          await _player.setSubtitleTrack(
            SubtitleTrack.uri(
              subUrl,
              title: subtitle['title'],
              language: subtitle['language'],
            ),
          );
        }

        setState(() {
          _currentSubtitle = subtitle;
        });

        // Apply current settings
        _applySubtitleSettings();
      } catch (e) {
        // print('Error changing subtitle: $e');
      }
    }
  }

  Future<void> _applySubtitleSettings() async {
    // Apply settings using fvp controller (universal for all platforms)
    // Note: mdk-sdk uses different property names than mpv
    if (_fvpVideoController == null) return;

    try {
      // Scale (default is 1.0)
      // We map 28.0 (our default UI size) to 1.0 scale roughly
      double scale = _subtitleSize / 28.0;
      _fvpVideoController!.setProperty('subtitle.scale', scale.toString());

      // Subtitle colors use RGBA hex format in mdk-sdk
      // Convert our Color to mdk format (0xRRGGBBAA as decimal string)
      int colorValue = _subtitleColor.toARGB32();
      _fvpVideoController!.setProperty('subtitle.color', colorValue.toString());

      int bgColorValue = _subtitleBackgroundColor.toARGB32();
      _fvpVideoController!
          .setProperty('subtitle.color.background', bgColorValue.toString());

      // Subtitle position via alignment
      // Convert _subtitlePos (0-100, 100=bottom) to alignment
      // mdk uses subtitle.alignment.y: -1=top, 0=center, 1=bottom
      // and subtitle.margin.y for fine-tuning
      if (_subtitlePos <= 33) {
        _fvpVideoController!.setProperty('subtitle.alignment.y', '-1'); // top
      } else if (_subtitlePos <= 66) {
        _fvpVideoController!.setProperty('subtitle.alignment.y', '0'); // center
      } else {
        _fvpVideoController!.setProperty('subtitle.alignment.y', '1'); // bottom
      }

      // Subtitle delay is now handled by modifying the subtitle file
      // See _getDelayedSubtitleUrl() method
    } catch (e) {
      debugPrint('Error applying subtitle settings: $e');
    }
  }

  /// Get subtitle URL with delay applied by modifying the subtitle file
  Future<String> _getDelayedSubtitleUrl(String originalUrl) async {
    if (_subtitleDelay == 0) {
      return originalUrl;
    }

    try {
      final modifiedPath =
          await SubtitleUtils.applyDelay(originalUrl, _subtitleDelay);
      if (modifiedPath != null) {
        return modifiedPath;
      }
    } catch (e) {
      debugPrint('Error applying subtitle delay: $e');
    }

    // Fall back to original URL if delay application fails
    return originalUrl;
  }

  /// Reload current subtitle with updated delay
  Future<void> _reloadSubtitleWithDelay() async {
    if (_currentSubtitle == null || _currentSubtitle!['url'] == null) return;

    try {
      final effectiveUrl =
          await _getDelayedSubtitleUrl(_currentSubtitle!['url']);
      _fvpVideoController?.setExternalSubtitle(effectiveUrl);
    } catch (e) {
      debugPrint('Error reloading subtitle with delay: $e');
    }
  }

  Future<void> _loadFonts(List<dynamic> fonts) async {
    try {
      final tempDir = await getTemporaryDirectory();
      final fontsDir = Directory('${tempDir.path}/subtitle_fonts');
      if (!await fontsDir.exists()) {
        await fontsDir.create(recursive: true);
      }

      // Download all fonts to the fonts directory
      for (final font in fonts) {
        final fontUrl = font['url'];
        final fontName = font['name'];
        if (fontUrl != null && fontName != null) {
          // Extract extension from URL
          String extension = '.ttf'; // Default
          try {
            final uri = Uri.parse(fontUrl);
            final pathSegments = uri.pathSegments;
            if (pathSegments.isNotEmpty) {
              final filename = pathSegments.last;
              if (filename.contains('.')) {
                // Get extension without query params
                final pathPart = filename.split('?').first;
                if (pathPart.contains('.')) {
                  extension = pathPart.substring(pathPart.lastIndexOf('.'));
                }
              }
            }
          } catch (_) {}

          String fileNameWithExt = fontName;
          if (!fontName.toLowerCase().endsWith(extension.toLowerCase())) {
            fileNameWithExt = '$fontName$extension';
          }

          final fontFile = File('${fontsDir.path}/$fileNameWithExt');
          if (!await fontFile.exists()) {
            // Download font
            debugPrint('Downloading font: $fontName from $fontUrl');
            final response = await http.get(Uri.parse(fontUrl));
            if (response.statusCode == 200) {
              await fontFile.writeAsBytes(response.bodyBytes);
              debugPrint('Font saved: ${fontFile.path}');
            } else {
              debugPrint(
                  'Failed to download font $fontName: ${response.statusCode}');
            }
          } else {
            debugPrint('Font already exists: ${fontFile.path}');
          }
        }
      }

      // Configure player to use fonts directory and enable ASS
      // For fvp/mdk on all desktop platforms, configure fonts for libass
      if (Platform.isLinux || Platform.isWindows || Platform.isMacOS) {
        // libass uses fontconfig to find fonts, so we need to make fonts available

        // Approach 1: Copy fonts to user's local fonts directory (Linux-specific)
        // ~/.local/share/fonts is automatically scanned by fontconfig on Linux
        if (Platform.isLinux) {
          try {
            final homeDir = Platform.environment['HOME'] ?? '/tmp';
            final userFontsDir =
                Directory('$homeDir/.local/share/fonts/kuudere-subs');
            if (!await userFontsDir.exists()) {
              await userFontsDir.create(recursive: true);
            }

            // Copy all fonts to user fonts directory
            final fontsList = await fontsDir.list().toList();
            for (final file in fontsList) {
              if (file is File) {
                final fileName = file.path.split('/').last;
                final destFile = File('${userFontsDir.path}/$fileName');
                if (!await destFile.exists()) {
                  await file.copy(destFile.path);
                  debugPrint('Copied font to user fonts: ${destFile.path}');
                }
              }
            }

            // Update font cache (run in background, don't wait)
            Process.run('fc-cache', ['-f', userFontsDir.path]).then((_) {
              debugPrint('Font cache updated');
            }).catchError((e) {
              debugPrint('Error updating font cache: $e');
            });
          } catch (e) {
            debugPrint('Error installing fonts to user directory: $e');
          }
        }

        // Set MDK global option for fonts directory (works on all platforms with fvp)
        debugPrint('Setting subtitle.fonts.dir to: ${fontsDir.path}');
        mdk.setGlobalOption('subtitle.fonts.dir', fontsDir.path);

        // Set controller properties if available
        if (_fvpVideoController != null) {
          try {
            _fvpVideoController!
                .setProperty('subtitle.fonts.dir', fontsDir.path);
          } catch (e) {
            debugPrint('Error setting controller property: $e');
          }
        }
      }
    } catch (e) {
      debugPrint('Error loading fonts: $e');
    }
  }

  bool _hasNextEpisode() {
    if (episodeData['all_episodes'] == null) return false;
    final episodes = episodeData['all_episodes'] as List<dynamic>;
    return episodes.any((e) => e['number'] == (_currentEpisodeNumber ?? 0) + 1);
  }

  bool _hasPrevEpisode() {
    if (episodeData['all_episodes'] == null) return false;
    final episodes = episodeData['all_episodes'] as List<dynamic>;
    return episodes.any((e) => e['number'] == (_currentEpisodeNumber ?? 0) - 1);
  }

  void _playNextEpisode() {
    if (_hasNextEpisode()) {
      onEpisodeSelected((_currentEpisodeNumber ?? 0) + 1);
    }
  }

  void _playPrevEpisode() {
    if (_hasPrevEpisode()) {
      onEpisodeSelected((_currentEpisodeNumber ?? 0) - 1);
    }
  }

  Widget _buildVideoPlayer({bool isFullscreen = false}) {
    if (currentSelectedServer == null || isLoadingVideo) {
      return AspectRatio(
        aspectRatio: 16 / 9,
        child: Container(
          color: Colors.black,
          child: Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                LoadingAnimationWidget.threeArchedCircle(
                  color: Colors.red,
                  size: 50,
                ),
                const SizedBox(height: 20),
                Text(
                  "Fetching Episode...",
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7)),
                ),
              ],
            ),
          ),
        ),
      );
    }

    final servers = [
      'hiya',
      'hiya-dub',
      'zen',
      'zen2',
      'pahe',
      'allmanga',
      'allmanga-dub'
    ];
    final speeds = [0.5, 0.75, 1.0, 1.25, 1.5, 2.0];

    // Use video_player with fvp backend (universal for all platforms)
    Widget fvpVideoWidget;

    if (_fvpVideoInitialized && _fvpVideoController != null) {
      // Build the settings overlay for Linux
      Widget? fvpSettingsOverlay;
      if (_showSettingsOverlay) {
        fvpSettingsOverlay = VideoSettingsOverlay(
          onClose: () {
            setState(() {
              _showSettingsOverlay = false;
            });
          },
          servers: servers,
          currentServer: _currentServer,
          onServerChanged: (server) async {
            setState(() {
              _showSettingsOverlay = false;
            });
            await _loadVideo('', server: server);
          },
          speeds: speeds,
          currentSpeed: _playbackSpeed,
          onSpeedChanged: (speed) {
            setState(() {
              _playbackSpeed = speed;
              _showSettingsOverlay = false;
            });
            _fvpVideoController?.setPlaybackSpeed(speed);
          },
          subtitles: _availableSubtitles.cast<Map<String, dynamic>>(),
          currentSubtitle: _currentSubtitle,
          onSubtitleChanged: (subtitle) async {
            setState(() {
              _currentSubtitle = subtitle;
              _showSettingsOverlay = false;
            });
            // Load subtitle using fvp extension with delay applied
            if (subtitle != null && subtitle['url'] != null) {
              try {
                final effectiveUrl =
                    await _getDelayedSubtitleUrl(subtitle['url']);
                _fvpVideoController?.setExternalSubtitle(effectiveUrl);
              } catch (e) {
                debugPrint('Error setting subtitle: $e');
              }
            }
          },
          initialView: _settingsInitialView,
          subtitleSize: _subtitleSize,
          subtitleDelay: _subtitleDelay,
          subtitlePos: _subtitlePos,
          onSubtitleSizeChanged: (size) {
            setState(() {
              _subtitleSize = size;
            });
            _applySubtitleSettings();
            SettingsService().setSubtitleSize(size);
          },
          onSubtitleDelayChanged: (delay) async {
            setState(() {
              _subtitleDelay = delay;
            });
            // Reload subtitle with new delay applied
            await _reloadSubtitleWithDelay();
            SettingsService().setSubtitleDelay(delay);
          },
          onSubtitlePosChanged: (pos) {
            setState(() {
              _subtitlePos = pos;
            });
            _applySubtitleSettings();
            SettingsService().setSubtitlePos(pos);
          },
          qualities:
              _availableQualities.map((q) => q['quality'].toString()).toList(),
          currentQuality: _currentQuality ?? 'Auto',
          onQualityChanged: (quality) async {
            final selected = _availableQualities.firstWhere(
              (q) => q['quality'] == quality,
              orElse: () => {'url': ''},
            );
            setState(() {
              _currentQuality = quality;
              _showSettingsOverlay = false;
            });
            if (selected['url'] != null && selected['url'].isNotEmpty) {
              // Dispose and reload with new quality
              final currentPosition = _fvpVideoController?.value.position;
              _fvpVideoController?.dispose();
              _fvpVideoController = vp.VideoPlayerController.networkUrl(
                Uri.parse(selected['url']),
              );
              await _fvpVideoController!.initialize();
              if (currentPosition != null) {
                await _fvpVideoController!.seekTo(currentPosition);
              }
              _fvpVideoController!.play();
              setState(() {});
            }
          },
          onSubtitleSettingsPressed: () {
            // Handled internally by VideoSettingsOverlay
          },
        );
      }

      // Build video container - different for fullscreen vs normal
      Widget videoContainer;
      if (isFullscreen) {
        // In fullscreen, fill the space and fit the video while maintaining aspect ratio
        videoContainer = SizedBox.expand(
          child: FittedBox(
            fit: BoxFit.contain,
            child: SizedBox(
              width: _fvpVideoController!.value.size.width > 0
                  ? _fvpVideoController!.value.size.width
                  : 1920,
              height: _fvpVideoController!.value.size.height > 0
                  ? _fvpVideoController!.value.size.height
                  : 1080,
              child: vp.VideoPlayer(_fvpVideoController!),
            ),
          ),
        );
      } else {
        // In normal mode, use fixed 16:9 aspect ratio
        videoContainer = AspectRatio(
          aspectRatio: 16 / 9,
          child: FittedBox(
            fit: BoxFit.contain,
            child: SizedBox(
              width: _fvpVideoController!.value.size.width > 0
                  ? _fvpVideoController!.value.size.width
                  : 1920,
              height: _fvpVideoController!.value.size.height > 0
                  ? _fvpVideoController!.value.size.height
                  : 1080,
              child: vp.VideoPlayer(_fvpVideoController!),
            ),
          ),
        );
      }

      fvpVideoWidget = Stack(
        children: [
          // Video - fills available space in fullscreen
          if (isFullscreen)
            Positioned.fill(
              child: Container(
                color: Colors.black,
                child: videoContainer,
              ),
            )
          else
            videoContainer,
          // Controls
          Positioned.fill(
            child: FvpVideoControls(
              controller: _fvpVideoController!,
              onSettingsPressed: (anchor) {
                setState(() {
                  _settingsAnchor = anchor;
                  _showSettingsOverlay = true;
                  _settingsInitialView = SettingsView.main;
                });
              },
              onSubtitlePressed: (anchor) {
                setState(() {
                  _settingsAnchor = anchor;
                  _showSettingsOverlay = true;
                  _settingsInitialView = SettingsView.subtitles;
                });
              },
              isFullscreen: isFullscreen,
              onFullscreenToggle: _toggleFullscreen,
              onEpisodesPressed: () => _toggleSidePanel('episodes'),
              onCommentsPressed: () => _toggleSidePanel('comments'),
              activeSidePanel: _activeSidePanel,
              settingsOverlay: fvpSettingsOverlay,
              title: animeData['anime_info']?['english'] ?? 'Unknown Anime',
              episodeTitle: 'Episode $_currentEpisodeNumber',
              onNextEpisode: _hasNextEpisode() ? _playNextEpisode : null,
              onPrevEpisode: _hasPrevEpisode() ? _playPrevEpisode : null,
            ),
          ),
        ],
      );
    } else {
      // Show loading while initializing
      fvpVideoWidget = Container(
        color: Colors.black,
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              LoadingAnimationWidget.threeArchedCircle(
                color: Colors.red,
                size: 50,
              ),
              const SizedBox(height: 20),
              Text(
                "Loading Video...",
                style: TextStyle(color: Colors.white.withValues(alpha: 0.7)),
              ),
            ],
          ),
        ),
      );
    }

    // Fullscreen layout for Linux
    if (isFullscreen) {
      return Row(
        children: [
          Expanded(child: fvpVideoWidget),
          if (_activeSidePanel != null)
            Container(
              width: 320,
              color: Colors.black.withValues(alpha: 0.9),
              child: DefaultTabController(
                length: 2,
                child: Column(
                  children: [
                    Padding(
                      padding: const EdgeInsets.fromLTRB(16, 8, 8, 8),
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              _activeSidePanel == 'episodes'
                                  ? 'Episodes'
                                  : 'Comments',
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ),
                          IconButton(
                            icon: const Icon(Icons.close, color: Colors.white),
                            onPressed: () => _toggleSidePanel(null),
                          ),
                        ],
                      ),
                    ),
                    Expanded(
                      child: _activeSidePanel == 'episodes'
                          ? _buildSidePanelEpisodeList()
                          : CommentsContent(
                              commentCount: episodeData['total_comments'] ?? 0,
                              episodeData: episodeData,
                              epNumber: _currentEpisodeNumber,
                              animeId: widget.id,
                              comments: comments,
                              updateComments: updateComments,
                              isDesktop: true,
                            ),
                    ),
                  ],
                ),
              ),
            ),
        ],
      );
    }

    return AspectRatio(
      aspectRatio: 16 / 9,
      child: fvpVideoWidget,
    );
  }

  Widget _buildSidePanelEpisodeList() {
    if (isLoadingEpisodes) {
      return Center(
        child: LoadingAnimationWidget.fourRotatingDots(
          color: Colors.red,
          size: 50,
        ),
      );
    }

    final episodes = episodeData['all_episodes'] ?? [];
    // Sort episodes by number
    episodes.sort((a, b) => (a['number'] as int).compareTo(b['number'] as int));

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.all(16),
          child: TextField(
            decoration: InputDecoration(
              hintText: 'Search episode...',
              hintStyle: const TextStyle(color: Colors.white38),
              fillColor: Colors.white.withValues(alpha: 0.1),
              filled: true,
              isDense: true,
              contentPadding:
                  const EdgeInsets.symmetric(vertical: 12, horizontal: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide.none,
              ),
              prefixIcon:
                  const Icon(Icons.search, color: Colors.white38, size: 20),
            ),
            style: const TextStyle(color: Colors.white),
            onChanged: (value) {
              // Simple local search filtering if needed, or just rely on scrolling
              // For now, let's just filter the list locally
              setState(() {
                // We might need a local state for this search if we want it to update
                // But setState here will rebuild the whole player which is fine
                _searchQuery = value.toLowerCase();
              });
            },
          ),
        ),
        Expanded(
          child: GridView.builder(
            padding: const EdgeInsets.all(16),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 5,
              childAspectRatio: 1,
              crossAxisSpacing: 8,
              mainAxisSpacing: 8,
            ),
            itemCount: episodes.length,
            itemBuilder: (context, index) {
              final episode = episodes[index];
              final epNumber = episode['number'];
              final isSelected = epNumber == _currentEpisodeNumber;

              // Filter based on search
              if (_searchQuery.isNotEmpty &&
                  !epNumber.toString().contains(_searchQuery)) {
                return const SizedBox.shrink();
              }

              return InkWell(
                onTap: () => onEpisodeSelected(epNumber),
                borderRadius: BorderRadius.circular(8),
                child: Container(
                  decoration: BoxDecoration(
                    color: isSelected
                        ? Colors.red
                        : Colors.white.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: isSelected
                          ? Colors.red
                          : Colors.white.withValues(alpha: 0.2),
                    ),
                  ),
                  child: Center(
                    child: Text(
                      '$epNumber',
                      style: TextStyle(
                        color: Colors.white,
                        fontWeight:
                            isSelected ? FontWeight.bold : FontWeight.normal,
                        fontSize: 16,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  String _getEpisodeAgo(int episodeNumber) {
    if (episodeData.isEmpty || episodeData['all_episodes'] == null) {
      return 'Unknown time ago';
    }

    final List<dynamic> episodes = episodeData['all_episodes'];
    final currentEpisode = episodes.firstWhere(
      (ep) => ep['number'] == episodeNumber,
      orElse: () => null,
    );

    if (currentEpisode != null && currentEpisode.containsKey('ago')) {
      return currentEpisode['ago'];
    }

    return 'Unknown time ago';
  }

  Widget _buildMainContent({bool isDesktop = false}) {
    final animeInfo = animeData['anime_info'];
    final currentEpisode = (episodeData['all_episodes'] as List<dynamic>?)
        ?.firstWhere((episode) => episode['number'] == _currentEpisodeNumber,
            orElse: () => null);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                animeInfo['english'],
                maxLines: 2,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 4),
              if (currentEpisode != null)
                Text(
                  'Episode $_currentEpisodeNumber - ${currentEpisode['titles'][0]}',
                  style: TextStyle(
                    color: Colors.white.withValues(alpha: 0.7),
                    fontSize: 16,
                  ),
                ),
            ],
          ),
        ),
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
          child: Row(
            children: [
              _buildViewCount(),
              const SizedBox(width: 8),
              Container(
                width: 4,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.white.withValues(alpha: 0.5),
                  shape: BoxShape.circle,
                ),
              ),
              const SizedBox(width: 8),
              Text(
                _currentEpisodeNumber != null
                    ? _getEpisodeAgo(_currentEpisodeNumber!)
                    : 'Unknown time ago',
                style: TextStyle(
                  color: Colors.white.withValues(alpha: 0.7),
                  fontSize: 14,
                ),
              ),
            ],
          ),
        ),
        Container(
          margin: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
          height: 56,
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
              decoration: BoxDecoration(
                color: Colors.grey[900]?.withValues(alpha: 0.7),
                borderRadius: BorderRadius.circular(24),
              ),
              child: Row(
                children: [
                  Container(
                    padding:
                        const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                    decoration: BoxDecoration(
                      color: Colors.grey[800]?.withValues(alpha: 0.5),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Row(
                      children: [
                        InkWell(
                          onTap: () => _handleAnimeResponse('like'),
                          child: Row(
                            children: [
                              Icon(
                                animeInfo['userLiked']
                                    ? Icons.thumb_up
                                    : Icons.thumb_up_outlined,
                                color: animeInfo['userLiked']
                                    ? Colors.blue
                                    : Colors.white,
                                size: 20,
                              ),
                              const SizedBox(width: 8),
                              Text(
                                animeInfo['likes'].toString(),
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.9),
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(width: 12),
                        InkWell(
                          onTap: () => _handleAnimeResponse('dislike'),
                          child: Row(
                            children: [
                              Icon(
                                animeInfo['userUnliked']
                                    ? Icons.thumb_down
                                    : Icons.thumb_down_outlined,
                                color: animeInfo['userUnliked']
                                    ? Colors.red
                                    : Colors.white,
                                size: 20,
                              ),
                              const SizedBox(width: 8),
                              Text(
                                animeInfo['dislikes'].toString(),
                                style: TextStyle(
                                  color: Colors.white.withValues(alpha: 0.9),
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 8),
                  _buildActionButton(
                    icon: Icons.share,
                    label: 'Share',
                    onPressed: _shareAnime, // Call our share function
                  ),
                  const SizedBox(width: 12),
                  _buildActionButton(
                    icon: animeData['anime_info']['inWatchlist'] == true &&
                            animeData['anime_info']['folder'] != null
                        ? Icons.bookmark
                        : Icons.bookmark_border,
                    label: animeData['anime_info']['inWatchlist'] == true &&
                            animeData['anime_info']['folder'] != null
                        ? 'Saved'
                        : 'Save',
                    onPressed: _showWatchlistBottomSheet,
                  ),
                  const SizedBox(width: 12),
                  _buildActionButton(
                    icon: Icons.flag_outlined,
                    label: 'Report',
                    onPressed: _showReportBottomSheet, // Opens the bottom sheet
                  ),
                  const SizedBox(width: 12),
                  _buildActionButton(
                    icon: Icons.repeat,
                    label: 'Remix',
                    onPressed: () {},
                  ),
                ],
              ),
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            children: [
              if (!isDesktop) _buildCommentButton(),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    required String label,
    required VoidCallback onPressed,
  }) {
    return TextButton.icon(
      onPressed: onPressed,
      style: TextButton.styleFrom(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        backgroundColor: Colors.grey[800]?.withValues(alpha: 0.5),
        shape: RoundedRectangleBorder(
          borderRadius: BorderRadius.circular(20),
        ),
      ),
      icon: Icon(icon, color: Colors.white, size: 20),
      label: Text(
        label,
        style: TextStyle(
          color: Colors.white.withValues(alpha: 0.9),
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }

  Widget _buildEpisodeListMobile() {
    if (isLoadingEpisodes) {
      return Center(
        child: LoadingAnimationWidget.threeArchedCircle(
          color: Colors.red,
          size: 50,
        ),
      );
    }

    final episodes = episodeData['all_episodes'] ?? [];
    if (episodes.isEmpty) {
      return const Center(
        child: Text(
          'No episodes found',
          style: TextStyle(color: Colors.white),
        ),
      );
    }

    final totalEpisodes = episodes.length;
    final pageGroups = _generatePageGroups(totalEpisodes);

    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              CustomDropdown<int>(
                value: _currentPageStart,
                items: pageGroups,
                itemBuilder: (start) {
                  final end = start + _episodesPerPage - 1;
                  return '$start - $end';
                },
                onChanged: (value) {
                  setState(() {
                    _currentPageStart = value;
                  });
                },
                width: 120,
                maxHeight: 200, // Make episode dropdown scrollable
                child: Container(
                  padding:
                      const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                  decoration: BoxDecoration(
                    color: Colors.grey[900],
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.1),
                      width: 1,
                    ),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '${_currentPageStart} - ${_currentPageStart + _episodesPerPage - 1}',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: _currentPageStart >= 1000 ? 10 : 14,
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Icon(
                        Icons.arrow_drop_down,
                        color: Colors.white,
                        size: 20,
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(width: 16),
              Expanded(
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    hintText: 'Search episode...',
                    hintStyle: const TextStyle(color: Colors.white70),
                    fillColor: Colors.grey[900],
                    filled: true,
                    contentPadding: const EdgeInsets.symmetric(
                        vertical: 12, horizontal: 16),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(8),
                      borderSide: BorderSide.none,
                    ),
                    prefixIcon: const Icon(Icons.search, color: Colors.white70),
                    suffixIcon: _searchController.text.isNotEmpty
                        ? IconButton(
                            icon:
                                const Icon(Icons.clear, color: Colors.white70),
                            onPressed: () {
                              setState(() {
                                _searchController.clear();
                                _searchQuery = '';
                              });
                            },
                          )
                        : null,
                  ),
                  style: const TextStyle(color: Colors.white),
                  onChanged: (value) {
                    setState(() {
                      _searchQuery = value.toLowerCase();
                    });
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          AnimatedSwitcher(
            duration: const Duration(milliseconds: 200),
            transitionBuilder: (child, animation) {
              return FadeTransition(
                opacity: animation,
                child: SlideTransition(
                  position: Tween<Offset>(
                    begin: const Offset(0, 0.05),
                    end: Offset.zero,
                  ).animate(CurvedAnimation(
                    parent: animation,
                    curve: Curves.easeOutCubic,
                  )),
                  child: child,
                ),
              );
            },
            child: ListView.builder(
              key: ValueKey(_searchQuery + _currentPageStart.toString()),
              padding: EdgeInsets.zero,
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              itemCount: _getFilteredEpisodes(episodes).length,
              itemBuilder: (context, index) {
                final filteredEpisodes = _getFilteredEpisodes(episodes);
                final episode = filteredEpisodes[index];
                final isCurrentEpisode =
                    episode['number'] == _currentEpisodeNumber;

                return Container(
                  key: ValueKey(episode['number']),
                  margin: const EdgeInsets.only(bottom: 8),
                  decoration: BoxDecoration(
                    color: isCurrentEpisode
                        ? Colors.red.withValues(alpha: 0.1)
                        : Colors.grey[900],
                    borderRadius: BorderRadius.circular(8),
                    border:
                        isCurrentEpisode ? Border.all(color: Colors.red) : null,
                  ),
                  child: ListTile(
                    contentPadding: const EdgeInsets.all(8),
                    leading: Container(
                      width: 90,
                      height: 50,
                      decoration: BoxDecoration(
                        color: Colors.black,
                        borderRadius: BorderRadius.circular(4),
                        image: _thumbnails
                                .containsKey(episode['number'].toString())
                            ? DecorationImage(
                                image: NetworkImage(
                                    _thumbnails[episode['number'].toString()]!),
                                fit: BoxFit.cover,
                              )
                            : null,
                      ),
                      child:
                          _thumbnails.containsKey(episode['number'].toString())
                              ? Container(
                                  color: Colors.black.withValues(alpha: 0.3),
                                  child: const Center(
                                    child: Icon(
                                      Icons.play_circle_outline,
                                      color: Colors.white,
                                      size: 24,
                                    ),
                                  ),
                                )
                              : const Center(
                                  child: Icon(
                                    Icons.play_circle_outline,
                                    color: Colors.white54,
                                    size: 24,
                                  ),
                                ),
                    ),
                    title: Text(
                      'Episode ${episode['number']}',
                      style: TextStyle(
                        color: isCurrentEpisode ? Colors.red : Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    subtitle: Text(
                      episode['titles'][0] ?? '',
                      style: TextStyle(color: Colors.grey[400], fontSize: 12),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    onTap: () => onEpisodeSelected(episode['number']),
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  List<int> _generatePageGroups(int totalEpisodes) {
    List<int> groups = [];
    for (int i = 1; i <= totalEpisodes; i += _episodesPerPage) {
      groups.add(i);
    }
    return groups;
  }

  List<dynamic> _getFilteredEpisodes(List<dynamic> episodes) {
    return episodes.where((episode) {
      final number = episode['number'] as int;
      final title = (episode['titles'][0] as String).toLowerCase();
      final numberInRange = number >= _currentPageStart &&
          number < _currentPageStart + _episodesPerPage;

      if (_searchQuery.isEmpty) return numberInRange;

      final matchesNumber = number.toString().contains(_searchQuery);
      final matchesTitle = title.contains(_searchQuery);

      return numberInRange && (matchesNumber || matchesTitle);
    }).toList()
      ..sort((a, b) => (a['number'] as int).compareTo(b['number'] as int));
  }

  void onEpisodeSelected(int episodeNumber) {
    _currentEpisodeNumber = episodeNumber;
    fetchEpisodeData(episodeNumber);

    fetchEpisodeData(episodeNumber).then((_) async {
      if (episodeData['episode_links'] != null) {
        final selectedServer = _selectServer(episodeData['episode_links']);
        if (selectedServer != null) {
          setState(() {
            currentSelectedServer = selectedServer;
          });
          await _loadVideo(selectedServer['dataLink']);
        }
      }
    });

    setState(() {});
  }

  void _showComments() {
    if (isLoadingEpisodes) {
      return;
    }

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) => CommentBottomSheet(
        commentCount: episodeData['total_comments'] ?? 0,
        episodeData: episodeData,
        epNumber: _currentEpisodeNumber,
        animeId: widget.id,
        comments: comments,
        updateComments: updateComments,
      ),
    );
  }

  Map<String, dynamic>? _selectServer(List<dynamic> episodeLinks) {
    if (episodeLinks.isEmpty) return null;

    // Use the widget.lang if provided
    if (widget.lang != null) {
      var serverWithLang = episodeLinks.firstWhere(
        (link) => link['dataType'] == widget.lang,
        orElse: () => {},
      );

      if (serverWithLang.isNotEmpty) {
        // _selectedCategory = serverWithLang['dataType'];
        // _selectedServerName = serverWithLang['serverName'];
        return serverWithLang;
      }
    }

    // Try to use stored preferred language
    if (_preferredLang != null) {
      var preferredServer = episodeLinks.firstWhere(
        (link) => link['dataType'] == _preferredLang,
        orElse: () => {},
      );

      if (preferredServer.isNotEmpty) {
        // _selectedCategory = preferredServer['dataType'];
        // _selectedServerName = preferredServer['serverName'];
        return preferredServer;
      }
    }

    // If no stored preference OR no matching servers, fallback to first available
    var fallbackServer = episodeLinks.firstWhere(
      (link) => link['dataType'] == 'dub', // Try dub first if available
      orElse: () => {},
    );

    if (fallbackServer.isEmpty) {
      fallbackServer = episodeLinks.firstWhere(
        (link) => link['dataType'] == 'sub', // If no dub, use sub
        orElse: () => {},
      );
    }

    if (fallbackServer.isNotEmpty) {
      // _selectedCategory = fallbackServer['dataType'];
      // _selectedServerName = fallbackServer['serverName'];

      // Also update _preferredLang to match the fallback
      _preferredLang = fallbackServer['dataType'];
      _secureStorage.write(key: "preferredLang", value: _preferredLang!);

      return fallbackServer;
    }

    return null;
  }

  Widget _buildCommentButton() {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 0),
      child: InkWell(
        onTap: _showComments,
        child: Container(
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: const Color(0xFF1A1A1A),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Row(
            children: [
              const Icon(Icons.comment_outlined, color: Colors.white),
              const SizedBox(width: 12),
              const Text(
                'Comments',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(width: 8),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: const Color(0xFF2A2A2A),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: Text(
                  '${episodeData['total_comments'] ?? 0}',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 12,
                  ),
                ),
              ),
              const Spacer(),
              const Icon(Icons.chevron_right, color: Colors.white),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildAnimeDetailsCard() {
    final animeInfo = animeData['anime_info'];
    bool showFullDescription = false;

    return StatefulBuilder(
      builder: (BuildContext context, StateSetter setState) {
        return Container(
          margin: const EdgeInsets.all(16),
          padding: const EdgeInsets.all(24),
          decoration: BoxDecoration(
            color: const Color(0xFF1A1A1A),
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ClipRRect(
                    borderRadius: BorderRadius.circular(8),
                    child: Image.network(
                      animeInfo['cover'],
                      width: 96,
                      height: 144,
                      fit: BoxFit.cover,
                      errorBuilder: (context, error, stackTrace) => Container(
                        width: 96,
                        height: 144,
                        color: Colors.grey[800],
                        child: const Icon(Icons.image, color: Colors.white54),
                      ),
                    ),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          animeInfo['english'],
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                          overflow: TextOverflow.visible,
                          maxLines: 2,
                        ),
                        const SizedBox(height: 8),
                        Row(
                          children: [
                            const Icon(Icons.star,
                                size: 20, color: Colors.amber),
                            const SizedBox(width: 4),
                            Text(
                              animeInfo['malScore'].toString(),
                              style: const TextStyle(
                                color: Colors.white,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(width: 4),
                            Text(
                              '(MAL Rating)',
                              style: TextStyle(
                                  color: Colors.grey[400], fontSize: 12),
                            ),
                          ],
                        ),
                        const SizedBox(height: 8),
                        RichText(
                          text: TextSpan(
                            style: TextStyle(
                                fontSize: 14, color: Colors.grey[400]),
                            children: [
                              const TextSpan(text: 'Genres: '),
                              TextSpan(
                                text: (animeInfo['genres'] as List<dynamic>)
                                    .join(', '),
                                style: const TextStyle(color: Colors.white),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 4),
                        RichText(
                          text: TextSpan(
                            style: TextStyle(
                                fontSize: 14, color: Colors.grey[400]),
                            children: [
                              const TextSpan(text: 'Studios: '),
                              TextSpan(
                                text: (animeInfo['studios'] as List<dynamic>)
                                    .join(', '),
                                style: const TextStyle(color: Colors.white),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 16),
              Text(
                'Synopsis',
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 16,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(height: 8),
              showFullDescription
                  ? HtmlWidget(
                      animeInfo['description'],
                      textStyle: TextStyle(
                        color: Colors.grey[300],
                        fontSize: 14,
                        height: 1.5,
                      ),
                    )
                  : Text(
                      _stripHtmlTags(animeInfo['description']),
                      style: TextStyle(
                        color: Colors.grey[300],
                        fontSize: 14,
                        height: 1.5,
                      ),
                      maxLines: 3,
                      overflow: TextOverflow.ellipsis,
                    ),
              GestureDetector(
                onTap: () {
                  setState(() {
                    showFullDescription = !showFullDescription;
                  });
                },
                child: Text(
                  showFullDescription ? 'Read less' : 'Read more',
                  style: const TextStyle(
                    color: Color(0xFFFF0000),
                    fontWeight: FontWeight.bold,
                  ),
                ),
              ),
              const SizedBox(height: 16),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _buildInfoItem('Type', animeInfo['type']),
                  _buildInfoItem('Status', animeInfo['status']),
                  _buildInfoItem('Year', animeInfo['year'].toString()),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  _buildInfoItem('Episodes', animeInfo['epCount'].toString()),
                  _buildInfoItem('Duration', '${animeInfo['duration']} min'),
                  animeInfo['season'] != null
                      ? _buildInfoItem('Season', animeInfo['season'])
                      : SizedBox(),
                ],
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildInfoItem(String label, String value) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          label,
          style: TextStyle(color: Colors.grey[400], fontSize: 12),
        ),
        const SizedBox(height: 4),
        Text(
          value,
          style:
              const TextStyle(color: Colors.white, fontWeight: FontWeight.bold),
        ),
      ],
    );
  }

  void updateComments(List<Comment> updatedComments, int totalCount) {
    setState(() {
      comments = updatedComments;
      if (episodeData.isNotEmpty) {
        episodeData['total_comments'] = totalCount;
      }
    });
  }

  String _stripHtmlTags(String htmlString) {
    String text = htmlString
        .replaceAll(RegExp(r'<br\s*/?>', caseSensitive: false), '\n')
        .replaceAll(RegExp(r'</p>', caseSensitive: false), '\n\n')
        .replaceAll(RegExp(r'</div>', caseSensitive: false), '\n');
    return text.replaceAll(RegExp(r'<[^>]*>'), '').trim();
  }
}
