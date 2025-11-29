import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/material.dart';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:google_fonts/google_fonts.dart';

import 'package:kuudere/services/auth_service.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:share_plus/share_plus.dart';
import 'package:url_launcher/url_launcher.dart';
import 'comment_sheet.dart';
import 'package:kuudere/services/http_service.dart';
import 'package:socket_io_client/socket_io_client.dart' as socket_io;
import 'package:kuudere/widgets/app_header.dart';
import 'package:media_kit/media_kit.dart';
import 'package:media_kit_video/media_kit_video.dart';

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

  // Media Kit player
  late final Player _player;
  late final VideoController _videoController;

  final authService = AuthService();

  @override
  void dispose() {
    _progressTimer?.cancel();
    _searchController.dispose();
    _player.dispose();
    super.dispose();
    // socket.dispose();
  }

  @override
  void initState() {
    super.initState();
    _currentEpisodeNumber =
        widget.episodeNumber; // Initialize _currentEpisodeNumber

    _initializePlayer(); // Extracted player initialization
    _loadPreferredLang().then((_) {
      if (_currentEpisodeNumber == null || _currentEpisodeNumber == 0) {
        fetchDefaultEpisodeNumber();
      } else {
        initializeScreen(_currentEpisodeNumber!);
      }
    });
  }

  void _initializePlayer() {
    final bool isLinux = Platform.isLinux;
    _player = Player();
    _videoController = VideoController(
      _player,
      configuration: VideoControllerConfiguration(
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
                    style: GoogleFonts.poppins(
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
                  style: GoogleFonts.poppins(
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
                ? Colors.red
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
                                    activeColor: Colors.red,
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
                                      borderSide:
                                          const BorderSide(color: Colors.red),
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
  Widget build(BuildContext context) {
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

    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppHeader(
        style: HeaderStyle.transparent,
        showBackButton: true,
      ),
      body: Stack(
        children: [
          SingleChildScrollView(
            child: Column(
              children: [
                _buildVideoPlayer(),
                _buildMainContent(),
                _buildEpisodeListMobile(),
                _buildAnimeDetailsCard(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _loadVideo(String url) async {
    setState(() {
      isLoadingVideo = true;
    });

    try {
      // For testing, use the provided test URL
      // In production, you would extract the m3u8 URL from the server embed page
      final testUrl =
          'https://cc.sgsgsgsr.site/_v7/8065c6d1-28b3-40ff-bc66-4fefe09ad47c/master.m3u8?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ2aWRlb19pZCI6IjgwNjVjNmQxLTI4YjMtNDBmZi1iYzY2LTRmZWZlMDlhZDQ3YyIsImNsaWVudF9pcCI6IjIyMy4yMjQuMzAuMTgzIiwiZXhwIjoxNzY0MjI2NTYxLCJpYXQiOjE3NjQyMDQ5NjEsImlzcyI6InZpZGVvLWhvc3RpbmctcGxhdGZvcm0ifQ.1LJtbjXtUbC4QLs4eUo9U_X9iYtzaSbbX_itH9GWRhU';

      // print('Loading video from: $testUrl');

      await _player.open(
        Media(testUrl),
        play: true, // Auto-play to ensure video starts
      );

      // Listen for player state changes
      _player.stream.playing.listen((playing) {
        // print('Player playing state: $playing');
      });

      _player.stream.buffering.listen((buffering) {
        // print('Player buffering: $buffering');
      });

      _player.stream.error.listen((error) {
        // print('Player error: $error');
      });

      setState(() {
        isLoadingVideo = false;
      });

      // print('Video loaded successfully');
    } catch (e) {
      // print('Error loading video: $e');
      setState(() {
        isLoadingVideo = false;
      });
    }
  }

  Widget _buildVideoPlayer() {
    if (currentSelectedServer == null) {
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
                  "Loading Player...",
                  style: TextStyle(color: Colors.white.withValues(alpha: 0.7)),
                ),
              ],
            ),
          ),
        ),
      );
    }

    return AspectRatio(
      aspectRatio: 16 / 9,
      child: Video(
        key: ValueKey(_videoController),
        controller: _videoController,
        controls: MaterialVideoControls,
        fill: Colors.black,
        filterQuality: FilterQuality.high,
        wakelock: true,
        pauseUponEnteringBackgroundMode: false,
        resumeUponEnteringForegroundMode: true,
      ),
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

  Widget _buildMainContent() {
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
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: Colors.red[500],
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Column(
                  children: [
                    const Text('You are watching',
                        style: TextStyle(color: Colors.white)),
                    Text(
                      'Episode $_currentEpisodeNumber',
                      style: const TextStyle(
                          color: Colors.white, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    const Text(
                      'If the current server doesn\'t work, please try other servers below.',
                      style: TextStyle(color: Colors.white, fontSize: 12),
                      textAlign: TextAlign.center,
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: const Color(0xFF1A1A1A),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: isLoadingEpisodes
                    ? Center(
                        child: LoadingAnimationWidget.fourRotatingDots(
                          color: Colors.red,
                          size: 50,
                        ),
                      )
                    : Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Wrap(
                            crossAxisAlignment: WrapCrossAlignment.center,
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              const Text('SUB:',
                                  style: TextStyle(color: Colors.white)),
                              ...(episodeData['episode_links'] ?? [])
                                  .where((link) =>
                                      link['dataType'] == 'sub' &&
                                      (link['serverName'] == 'Streamwish' ||
                                          link['serverName'] == 'Vidhide' ||
                                          link['serverName'] == 'Hianime' ||
                                          link['serverName'] == 'StreamWish'))
                                  .map((server) => ElevatedButton(
                                        onPressed: () =>
                                            _onServerSelected(server),
                                        style: ElevatedButton.styleFrom(
                                          backgroundColor:
                                              server == currentSelectedServer
                                                  ? Colors.red
                                                  : const Color(0xFF1A1A1A),
                                          side: BorderSide(
                                            color:
                                                server == currentSelectedServer
                                                    ? Colors.red
                                                    : Colors.grey[700]!,
                                          ),
                                        ),
                                        child: Text(
                                          _getDisplayServerName(server),
                                          style: TextStyle(
                                            color:
                                                server == currentSelectedServer
                                                    ? Colors.white
                                                    : Colors.grey[400],
                                          ),
                                        ),
                                      )),
                            ],
                          ),
                          const SizedBox(height: 16),
                          Wrap(
                            crossAxisAlignment: WrapCrossAlignment.center,
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              const Text('DUB:',
                                  style: TextStyle(color: Colors.white)),
                              ...(episodeData['episode_links'] ?? [])
                                  .where((link) =>
                                      link['dataType'] == 'dub' &&
                                      (link['serverName'] == 'Streamwish' ||
                                          link['serverName'] == 'Vidhide' ||
                                          link['serverName'] == 'Hianime' ||
                                          link['serverName'] == 'StreamWish'))
                                  .map((server) => ElevatedButton(
                                        onPressed: () =>
                                            _onServerSelected(server),
                                        style: ElevatedButton.styleFrom(
                                          backgroundColor:
                                              server == currentSelectedServer
                                                  ? Colors.red
                                                  : const Color(0xFF1A1A1A),
                                          side: BorderSide(
                                            color:
                                                server == currentSelectedServer
                                                    ? Colors.red
                                                    : Colors.grey[700]!,
                                          ),
                                        ),
                                        child: Text(
                                          _getDisplayServerName(server),
                                          style: TextStyle(
                                            color:
                                                server == currentSelectedServer
                                                    ? Colors.white
                                                    : Colors.grey[400],
                                          ),
                                        ),
                                      )),
                            ],
                          ),
                        ],
                      ),
              ),
              const SizedBox(height: 16),
              _buildCommentButton(),
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
        child: LoadingAnimationWidget.fourRotatingDots(
          color: Colors.red,
          size: 50,
        ),
      );
    }

    final episodes = episodeData['all_episodes'] ?? [];
    final totalEpisodes = episodes.length;
    final pageGroups = _generatePageGroups(totalEpisodes);

    return Container(
      margin: const EdgeInsets.all(8),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: const Color.fromARGB(255, 0, 0, 0),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(
          color: Colors.grey[800]!,
          width: 1,
        ),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Text(
                'Episodes',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
              const SizedBox(width: 16),
              DropdownButton<int>(
                dropdownColor: Colors.black,
                value: _currentPageStart,
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
                underline: Container(
                  height: 2,
                  color: Colors.red,
                ),
                icon: const Icon(Icons.arrow_drop_down, color: Colors.white),
              ),
            ],
          ),
          const SizedBox(height: 16),
          TextField(
            controller: _searchController,
            decoration: InputDecoration(
              hintText: 'Search by episode number or title...',
              hintStyle: TextStyle(color: Colors.white70),
              fillColor: Colors.black,
              filled: true,
              contentPadding:
                  const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide(color: Colors.grey[700]!, width: 1),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide(color: Colors.grey[700]!, width: 1),
              ),
              focusedBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: const BorderSide(color: Colors.white, width: 1),
              ),
              prefixIcon: Icon(Icons.search, color: Colors.white70),
              suffixIcon: _searchController.text.isNotEmpty
                  ? IconButton(
                      icon: const Icon(Icons.clear, color: Colors.white70),
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
          const SizedBox(height: 16),
          SizedBox(
            height: 400,
            child: ListView.builder(
              controller: _episodeScrollController,
              itemCount: _getFilteredEpisodes(episodes).length,
              itemBuilder: (context, index) {
                final filteredEpisodes = _getFilteredEpisodes(episodes);
                final episode = filteredEpisodes[index];
                final isCurrentEpisode =
                    episode['number'] == _currentEpisodeNumber;

                return Container(
                  margin: const EdgeInsets.only(bottom: 8),
                  decoration: BoxDecoration(
                    border: Border.all(
                      color: isCurrentEpisode ? Colors.red : Colors.grey[800]!,
                      width: 1,
                    ),
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: ListTile(
                    contentPadding: const EdgeInsets.all(8),
                    leading: Container(
                      width: 100,
                      height: 60,
                      decoration: BoxDecoration(
                        color: Colors.grey[800],
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Center(
                        child: Icon(
                          Icons.play_circle_outline,
                          color: Colors.white54,
                          size: 32,
                        ),
                      ),
                    ),
                    title: Row(
                      children: [
                        Expanded(
                          child: Text(
                            '${episode['number']}. ${episode['titles'][0]}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.w500,
                            ),
                          ),
                        ),
                      ],
                    ),
                    subtitle: Text(
                      episode['aired'] ?? 'No air date',
                      style: TextStyle(color: Colors.grey[600]),
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

  String _getDisplayServerName(Map<String, dynamic> server) {
    String prefix;

    if (server['serverName'] == 'Streamwish') {
      prefix = 'HD-';
    } else if (server['serverName'] == 'StreamWish') {
      prefix = 'HD-';
    } else if (server['serverName'] == 'Hianime') {
      prefix = 'HD-';
    } else {
      prefix = 'HD-';
    }

    return '$prefix${server['serverId']}';
  }

  Future<void> _onServerSelected(Map<String, dynamic> server) async {
    setState(() {
      currentSelectedServer = server;
    });

    // Save new preferred language
    String selectedLang = server['dataType'];
    _preferredLang = selectedLang;
    await _secureStorage.write(key: "preferredLang", value: selectedLang);

    // _selectedCategory = selectedLang;
    // _selectedServerName = server['serverName'];

    await _loadVideo(server['dataLink']);
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
              Text(
                animeInfo['description'],
                style: TextStyle(
                  color: Colors.grey[300],
                  fontSize: 14,
                  height: 1.5,
                ),
                maxLines: showFullDescription ? null : 3,
                overflow: showFullDescription
                    ? TextOverflow.visible
                    : TextOverflow.ellipsis,
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

  void updateComments(List<Comment> updatedComments) {
    setState(() {
      comments = updatedComments;
    });
  }
}
