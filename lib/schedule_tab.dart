import 'package:flutter/material.dart';

import 'dart:convert';
import 'package:intl/intl.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:flutter/foundation.dart';
import 'package:kuudere/widgets/anime_schedule_popup.dart';
import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'anime_info.dart';
import 'dart:ui';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:kuudere/widgets/custom_dropdown.dart';

class ScheduleTab extends StatefulWidget {
  const ScheduleTab({super.key});

  @override
  State<ScheduleTab> createState() => _ScheduleTabState();
}

class _ScheduleTabState extends State<ScheduleTab> {
  late Future<Map<String, List<AnimeSchedule>>> _scheduleData;
  final RealtimeService _realtimeService = RealtimeService();
  final authService = AuthService();

  // Static cache to persist data across tab switches
  static Map<String, List<AnimeSchedule>>? _cachedRawSchedule;
  static DateTime? _lastCacheTime;
  static String? _lastCacheTimezone;

  @override
  void initState() {
    super.initState();
    _scheduleData = fetchScheduleData();
    _realtimeService.joinRoom('countdowns');
  }

  Future<Map<String, List<AnimeSchedule>>> fetchScheduleData() async {
    final String currentTimeZone = await FlutterTimezone.getLocalTimezone();
    final httpService = HttpService();
    final sessionInfo = await authService.getStoredSession();

    Map<String, List<AnimeSchedule>> rawSchedule;

    // Check if cache is valid (same timezone, less than 24 hours old)
    if (_cachedRawSchedule != null &&
        _lastCacheTime != null &&
        _lastCacheTimezone == currentTimeZone &&
        DateTime.now().difference(_lastCacheTime!) <
            const Duration(hours: 24)) {
      rawSchedule = _cachedRawSchedule!;
    } else {
      try {
        // 1. Fetch Schedule from API
        final response = await httpService.get(
          '/api/schedule',
          queryParams: {'timezone': currentTimeZone},
          requireAuth: sessionInfo != null,
        );

        if (response.statusCode == 200) {
          final Map<String, dynamic> jsonData = json.decode(response.body);
          final Map<String, List<AnimeSchedule>> scheduleData = {};

          final data = jsonData['data'] ?? jsonData;
          if (data is Map) {
            data.forEach((date, animeList) {
              scheduleData[date] = (animeList as List)
                  .map((anime) => AnimeSchedule.fromJson(anime))
                  .toList();
            });
          }

          // Update Cache
          rawSchedule = scheduleData;
          _cachedRawSchedule = scheduleData;
          _lastCacheTime = DateTime.now();
          _lastCacheTimezone = currentTimeZone;
        } else {
          throw Exception(
              'Failed to load schedule data (Status: ${response.statusCode})');
        }
      } catch (e) {
        throw Exception('Error fetching data: $e');
      }
    }

    // 2. Fetch Watchlist Map (if logged in) & Merge
    // We always do this to ensure the "Add to List" buttons are up-to-date
    // even if the schedule itself is cached.
    if (sessionInfo != null) {
      try {
        final watchlistMap = await _fetchWatchlistMap();

        // Create a new map to avoid modifying the cached rawSchedule directly
        final Map<String, List<AnimeSchedule>> mergedSchedule = {};

        rawSchedule.forEach((date, animeList) {
          mergedSchedule[date] = animeList.map((anime) {
            if (watchlistMap.containsKey(anime.id)) {
              return anime.copyWith(
                inWatchlist: true,
                folder: watchlistMap[anime.id],
              );
            }
            return anime;
          }).toList();
        });

        return mergedSchedule;
      } catch (e) {
        // If watchlist fetch fails, just return raw schedule
        return rawSchedule;
      }
    }

    return rawSchedule;
  }

  Future<Map<String, String>> _fetchWatchlistMap() async {
    final httpService = HttpService();
    final Map<String, String> watchlistMap = {};
    int page = 1;
    bool hasMore = true;
    const int maxPages = 5; // Limit to 5 pages to prevent slow loading

    while (hasMore && page <= maxPages) {
      try {
        final response = await httpService.get(
          '/api/anime/watchlist',
          queryParams: {
            'page': page.toString(),
            'perPage': '50'
          }, // Fetch 50 at a time
          requireAuth: true,
        );

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          final items = data['data'] ?? data['watchlist'] ?? [];

          if (items.isEmpty) {
            hasMore = false;
            break;
          }

          for (var item in items) {
            final id = item['id'] ?? item['mainId'];
            final folder = item['folder'] ?? item['status'];
            if (id != null && folder != null) {
              watchlistMap[id.toString()] = folder.toString();
            }
          }

          final totalPages = data['total_pages'] ?? data['totalPages'] ?? 1;
          if (page >= totalPages) {
            hasMore = false;
          } else {
            page++;
          }
        } else {
          hasMore = false;
        }
      } catch (e) {
        hasMore = false;
      }
    }
    return watchlistMap;
  }

  void retryFetchingData() {
    setState(() {
      _scheduleData = fetchScheduleData();
    });
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<Map<String, List<AnimeSchedule>>>(
      future: _scheduleData,
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return const Center(child: CircularProgressIndicator());
        } else if (snapshot.hasError) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Text(
                  'Error: ${snapshot.error}',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.red, fontSize: 16),
                ),
                const SizedBox(height: 10),
                ElevatedButton(
                  onPressed: retryFetchingData,
                  child: const Text('Retry'),
                ),
              ],
            ),
          );
        } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
          return Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                const Text(
                  'No schedule data available.',
                  style: TextStyle(color: Colors.white),
                ),
                const SizedBox(height: 10),
                ElevatedButton(
                  onPressed: retryFetchingData,
                  child: const Text('Retry'),
                ),
              ],
            ),
          );
        }

        final scheduleData = snapshot.data!;
        final sortedDates = scheduleData.keys.toList()..sort();
        final today = DateFormat('yyyy-MM-dd').format(DateTime.now());

        return ListView.builder(
          padding: const EdgeInsets.only(bottom: 100),
          itemCount: sortedDates.length,
          itemBuilder: (context, index) {
            final date = sortedDates[index];
            final animeList = scheduleData[date]!;
            final isToday = date == today;

            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  color: isToday
                      ? Colors.transparent.withValues(alpha: 0.2)
                      : Colors.transparent,
                  child: Text(
                    '${DateFormat('EEEE, MMMM d, yyyy').format(DateTime.parse(date))}${isToday ? ' (Today)' : ''}',
                    style: TextStyle(
                      color: Colors.white, // You can adjust this if needed
                      fontSize: 18,
                      fontWeight: FontWeight.bold,
                    ),
                  ),
                ),
                ...animeList.asMap().entries.map((entry) {
                  final index = entry.key;
                  final anime = entry.value;
                  final uniqueId = '${date}_${anime.id}_$index';
                  return AnimeScheduleCard(
                    key: ValueKey(uniqueId),
                    anime: anime,
                    isToday: isToday,
                    heroTag: 'schedule-$uniqueId',
                  );
                }),
              ],
            );
          },
        );
      },
    );
  }
}

class AnimeScheduleCard extends StatefulWidget {
  final AnimeSchedule anime;
  final bool isToday;
  final String heroTag;

  const AnimeScheduleCard({
    super.key,
    required this.anime,
    required this.isToday,
    required this.heroTag,
  });

  @override
  State<AnimeScheduleCard> createState() => _AnimeScheduleCardState();
}

class _AnimeScheduleCardState extends State<AnimeScheduleCard> {
  late bool _inWatchlist;
  String? _folder;
  bool _isUpdatingWatchlist = false;
  OverlayEntry? _overlayEntry;
  final LayerLink _layerLink = LayerLink();

  @override
  void initState() {
    super.initState();
    _inWatchlist = widget.anime.inWatchlist;
    _folder = widget.anime.folder;
  }

  @override
  void dispose() {
    _removePopup();
    super.dispose();
  }

  void _showPopup({bool isMobile = false}) {
    if (_overlayEntry != null) return;

    final renderBox = context.findRenderObject() as RenderBox;
    final size = renderBox.size;
    final offset = renderBox.localToGlobal(Offset.zero);
    final screenHeight = MediaQuery.of(context).size.height;
    final spaceBelow = screenHeight - (offset.dy + size.height);
    final showAbove = spaceBelow < 250; // Popup height approx

    _overlayEntry = OverlayEntry(
      builder: (context) => Stack(
        children: [
          if (isMobile)
            Positioned.fill(
              child: GestureDetector(
                onTap: _removePopup,
                behavior: HitTestBehavior.translucent,
                child: Container(color: Colors.black54),
              ),
            ),
          Positioned(
            width: 300,
            child: CompositedTransformFollower(
              link: _layerLink,
              showWhenUnlinked: false,
              offset: isMobile
                  ? Offset(
                      (size.width - 300) /
                          2, // Center horizontally relative to card
                      showAbove ? -260 : size.height + 10)
                  : Offset(0, showAbove ? -260 : size.height + 10), // Desktop
              child: AnimeSchedulePopup(
                anime: widget.anime,
                onClose: isMobile ? _removePopup : null,
              ),
            ),
          ),
        ],
      ),
    );

    Overlay.of(context).insert(_overlayEntry!);
  }

  void _removePopup() {
    _overlayEntry?.remove();
    _overlayEntry = null;
  }

  Future<void> _updateWatchlistStatus(String newStatus) async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    if (sessionInfo == null) {
      return;
    }

    setState(() {
      _isUpdatingWatchlist = true;
    });

    final httpService = HttpService();

    try {
      final response = await httpService.post(
        '/api/anime/watchlist',
        body: {
          'animeId': widget.anime.id,
          'folder': newStatus,
        },
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        setState(() {
          _inWatchlist = newStatus != 'Remove';
          _folder = newStatus != 'Remove' ? newStatus : null;
          _isUpdatingWatchlist = false;
        });
      } else {
        setState(() {
          _isUpdatingWatchlist = false;
        });
      }
    } catch (e) {
      setState(() {
        _isUpdatingWatchlist = false;
      });
    }
  }

  Widget _buildWatchlistButton() {
    final options = [
      'Watching',
      'On Hold',
      'Plan To Watch',
      'Dropped',
      'Completed',
      'Remove'
    ];

    return CustomDropdown<String>(
      value: _folder,
      items: options,
      itemBuilder: (value) => value,
      onChanged: (value) {
        if (!_isUpdatingWatchlist) {
          _updateWatchlistStatus(value);
        }
      },
      width: 140, // Slightly smaller than anime_info
      child: AbsorbPointer(
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Row(
            mainAxisSize: MainAxisSize.min,
            children: [
              _isUpdatingWatchlist
                  ? SizedBox(
                      width: 14,
                      height: 14,
                      child: LoadingAnimationWidget.threeArchedCircle(
                        color: Colors.black,
                        size: 14,
                      ),
                    )
                  : Icon(
                      _inWatchlist ? Icons.bookmark : Icons.bookmark_border,
                      color: Colors.black,
                      size: 14,
                    ),
              const SizedBox(width: 6),
              Flexible(
                child: Text(
                  _isUpdatingWatchlist
                      ? '...'
                      : _inWatchlist
                          ? (_folder ?? 'Saved')
                          : 'Add to List',
                  style: const TextStyle(
                    color: Colors.black,
                    fontSize: 12,
                    fontWeight: FontWeight.bold,
                  ),
                  overflow: TextOverflow.ellipsis,
                  maxLines: 1,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDesktop = kIsWeb ||
        defaultTargetPlatform == TargetPlatform.windows ||
        defaultTargetPlatform == TargetPlatform.linux ||
        defaultTargetPlatform == TargetPlatform.macOS;

    return CompositedTransformTarget(
      link: _layerLink,
      child: MouseRegion(
        onEnter: isDesktop
            ? (_) {
                _showPopup(isMobile: false);
              }
            : null,
        onExit: isDesktop
            ? (_) {
                _removePopup();
              }
            : null,
        child: GestureDetector(
          onLongPress: !isDesktop
              ? () {
                  _showPopup(isMobile: true);
                }
              : null,
          child: Container(
            margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            decoration: BoxDecoration(
              color: const Color(0xFF121212),
              borderRadius: BorderRadius.circular(16),
              border: widget.isToday
                  ? Border.all(
                      color: Colors.redAccent.withValues(alpha: 0.5), width: 1)
                  : Border.all(color: Colors.white.withValues(alpha: 0.05)),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.3),
                  blurRadius: 12,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: Material(
              color: Colors.transparent,
              child: InkWell(
                borderRadius: BorderRadius.circular(16),
                onTap: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(
                      builder: (context) =>
                          AnimeInfoScreen(animeId: widget.anime.id),
                    ),
                  );
                },
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(16),
                  child: Stack(
                    children: [
                      // Background Banner (faded)
                      if (widget.anime.banner != null)
                        Positioned.fill(
                          child: Opacity(
                            opacity: 0.15,
                            child: ImageFiltered(
                              imageFilter:
                                  ImageFilter.blur(sigmaX: 3.0, sigmaY: 3.0),
                              child: Image.network(
                                widget.anime.banner!,
                                fit: BoxFit.cover,
                                errorBuilder: (context, error, stackTrace) =>
                                    const SizedBox(),
                              ),
                            ),
                          ),
                        ),

                      // Content
                      Padding(
                        padding: const EdgeInsets.all(12),
                        child: Row(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            // Cover Image
                            Hero(
                              tag: widget.heroTag,
                              child: ClipRRect(
                                borderRadius: BorderRadius.circular(8),
                                child: widget.anime.cover != null
                                    ? Image.network(
                                        widget.anime.cover!,
                                        width: 85,
                                        height: 125,
                                        fit: BoxFit.cover,
                                        errorBuilder:
                                            (context, error, stackTrace) =>
                                                Container(
                                          width: 85,
                                          height: 125,
                                          color: Colors.grey[900],
                                          child: const Icon(Icons.movie,
                                              color: Colors.white24),
                                        ),
                                      )
                                    : Container(
                                        width: 85,
                                        height: 125,
                                        color: Colors.grey[900],
                                        child: const Icon(Icons.movie,
                                            color: Colors.white24),
                                      ),
                              ),
                            ),
                            const SizedBox(width: 16),

                            // Details
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  // Title
                                  Text(
                                    widget.anime.title,
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 16,
                                      fontWeight: FontWeight.bold,
                                      height: 1.2,
                                    ),
                                    maxLines: 2,
                                    overflow: TextOverflow.ellipsis,
                                  ),
                                  const SizedBox(height: 8),

                                  // Time and Episode Badge
                                  Row(
                                    children: [
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                            horizontal: 8, vertical: 4),
                                        decoration: BoxDecoration(
                                          color: Colors.redAccent
                                              .withValues(alpha: 0.2),
                                          borderRadius:
                                              BorderRadius.circular(6),
                                          border: Border.all(
                                              color: Colors.redAccent
                                                  .withValues(alpha: 0.3)),
                                        ),
                                        child: Row(
                                          mainAxisSize: MainAxisSize.min,
                                          children: [
                                            const Icon(
                                                Icons.access_time_rounded,
                                                size: 12,
                                                color: Colors.redAccent),
                                            const SizedBox(width: 4),
                                            Text(
                                              widget.anime.time,
                                              style: const TextStyle(
                                                color: Colors.redAccent,
                                                fontWeight: FontWeight.bold,
                                                fontSize: 12,
                                              ),
                                            ),
                                          ],
                                        ),
                                      ),
                                      const SizedBox(width: 8),
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                            horizontal: 8, vertical: 4),
                                        decoration: BoxDecoration(
                                          color: Colors.white
                                              .withValues(alpha: 0.1),
                                          borderRadius:
                                              BorderRadius.circular(6),
                                        ),
                                        child: Text(
                                          'EP ${widget.anime.episode}',
                                          style: const TextStyle(
                                            color: Colors.white,
                                            fontSize: 12,
                                            fontWeight: FontWeight.w600,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ),

                                  const SizedBox(height: 10),

                                  // Genres
                                  if (widget.anime.genres != null &&
                                      widget.anime.genres!.isNotEmpty)
                                    Wrap(
                                      spacing: 6,
                                      runSpacing: 6,
                                      children: widget.anime.genres!
                                          .take(3)
                                          .map((genre) {
                                        return Text(
                                          genre,
                                          style: TextStyle(
                                            color: Colors.grey[400],
                                            fontSize: 11,
                                            fontWeight: FontWeight.w500,
                                          ),
                                        );
                                      }).toList(),
                                    ),

                                  const SizedBox(height: 12),

                                  // Watchlist Button
                                  _buildWatchlistButton(),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class AnimeSchedule {
  final String id;
  final String title;
  final int episode;
  final String time;
  final String? description;
  final String? cover;
  final String? banner;
  final int? epCount;
  final String? status;
  final List<String>? genres;
  final int? year;
  final String? type;
  final bool inWatchlist;
  final String? folder;

  AnimeSchedule({
    required this.id,
    required this.title,
    required this.episode,
    required this.time,
    this.description,
    this.cover,
    this.banner,
    this.epCount,
    this.status,
    this.genres,
    this.year,
    this.type,
    this.inWatchlist = false,
    this.folder,
  });

  factory AnimeSchedule.fromJson(Map<String, dynamic> json) {
    return AnimeSchedule(
      id: json['id'] ?? '',
      title: json['title'] ?? 'Unknown',
      episode: json['episode'] ?? 0,
      time: json['time'] ?? '00:00',
      description: json['description'],
      cover: json['cover'],
      banner: json['banner'],
      epCount: json['epCount'],
      status: json['status'],
      genres: json['genres'] != null ? List<String>.from(json['genres']) : null,
      year: json['year'],
      type: json['type'],
      inWatchlist: json['in_watchlist'] ?? false,
      folder: json['folder'],
    );
  }

  AnimeSchedule copyWith({
    String? id,
    String? title,
    int? episode,
    String? time,
    String? description,
    String? cover,
    String? banner,
    int? epCount,
    String? status,
    List<String>? genres,
    int? year,
    String? type,
    bool? inWatchlist,
    String? folder,
  }) {
    return AnimeSchedule(
      id: id ?? this.id,
      title: title ?? this.title,
      episode: episode ?? this.episode,
      time: time ?? this.time,
      description: description ?? this.description,
      cover: cover ?? this.cover,
      banner: banner ?? this.banner,
      epCount: epCount ?? this.epCount,
      status: status ?? this.status,
      genres: genres ?? this.genres,
      year: year ?? this.year,
      type: type ?? this.type,
      inWatchlist: inWatchlist ?? this.inWatchlist,
      folder: folder ?? this.folder,
    );
  }
}
