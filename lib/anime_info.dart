import 'package:flutter/material.dart';
import 'dart:ui';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'dart:convert';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'watch_anime.dart';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/widgets/app_header.dart';
import 'package:flutter_widget_from_html/flutter_widget_from_html.dart';
import 'package:kuudere/widgets/custom_dropdown.dart';
import 'search_tab.dart';
import 'package:kuudere/models/anime_models.dart';

class AnimeDetails {
  final String id;
  final String english;
  final String romaji;
  final String native;
  final String ageRating;
  final double? malScore;
  final int averageScore;
  final int duration;
  final List<String> genres;
  final String cover;
  final String banner;
  final String season;
  final String startDate;
  final String status;
  final List<String> synonyms;
  final List<String> studios;
  final String type;
  final int year;
  final int epCount;
  final int subbedCount;
  final int dubbedCount;
  final String description;
  final bool inWatchlist;
  final String views;
  final String likes;
  final String? folder;
  final int? anilistId;

  AnimeDetails({
    required this.id,
    required this.english,
    required this.romaji,
    required this.native,
    required this.ageRating,
    this.malScore,
    required this.averageScore,
    required this.duration,
    required this.genres,
    required this.cover,
    required this.banner,
    required this.season,
    required this.startDate,
    required this.status,
    required this.synonyms,
    required this.studios,
    required this.type,
    required this.year,
    required this.epCount,
    required this.subbedCount,
    required this.dubbedCount,
    required this.description,
    required this.inWatchlist,
    required this.views,
    required this.likes,
    this.folder,
    this.anilistId,
  });

  factory AnimeDetails.fromJson(Map<String, dynamic> json) {
    final raw = json['data'] ?? json;
    final title = raw['title'] is Map ? raw['title'] : <String, dynamic>{};
    final cover = raw['cover_image'] is Map ? raw['cover_image'] : <String, dynamic>{};
    final startDate = raw['start_date'] is Map ? raw['start_date'] : null;

    return AnimeDetails(
      id: raw['anime_id'] ?? raw['id'] ?? '',
      english: title['english'] ?? raw['english'] ?? 'Unknown',
      romaji: title['romaji'] ?? raw['romaji'] ?? 'Unknown',
      native: title['native'] ?? raw['native'] ?? 'Unknown',
      ageRating: raw['ageRating'] ?? raw['age_rating'] ?? 'Unknown',
      malScore: (raw['mal_score'] ?? raw['malScore'])?.toDouble(),
      averageScore: raw['average_score'] ?? raw['averageScore'] ?? 0,
      duration: raw['duration'] ?? 0,
      genres: raw['genres'] != null ? List<String>.from(raw['genres']) : [],
      cover: cover['extra_large'] ?? cover['extraLarge'] ?? cover['large'] ?? raw['cover'] ?? '',
      banner: raw['banner_image'] ?? raw['banner'] ?? '',
      season: raw['season'] ?? 'Unknown',
      startDate: startDate != null ? '${startDate['year']}-${startDate['month']?.toString().padLeft(2, '0')}' : (raw['startDate'] ?? 'Unknown'),
      status: raw['status'] ?? 'Unknown',
      synonyms: raw['synonyms'] != null ? List<String>.from(raw['synonyms']) : [],
      studios: raw['studios'] != null ? (raw['studios'] as List).map<String>((s) => s is Map ? (s['name'] ?? '') as String : s.toString()).where((n) => n.isNotEmpty).toList() : [],
      type: raw['type'] ?? raw['format'] ?? 'Unknown',
      year: raw['season_year'] ?? raw['year'] ?? 0,
      epCount: raw['episodes_total'] ?? raw['epCount'] ?? 0,
      subbedCount: raw['subbed'] ?? raw['subbedCount'] ?? 0,
      dubbedCount: raw['dubbed'] ?? raw['dubbedCount'] ?? 0,
      description: raw['description'] ?? 'No description available.',
      inWatchlist: raw['watchlist'] != null,
      views: raw['views']?.toString() ?? '0',
      likes: raw['likes']?.toString() ?? '0',
      folder: raw['folder'],
      anilistId: raw['anilist_id'] ?? raw['anilistId'],
    );
  }
}

class AnimeInfoScreen extends StatefulWidget {
  final String animeId;

  const AnimeInfoScreen({super.key, required this.animeId});

  @override
  State<AnimeInfoScreen> createState() => _AnimeInfoScreenState();
}

class _AnimeInfoScreenState extends State<AnimeInfoScreen> {
  late Future<AnimeDetails> _animeDetails;
  bool isLoading = false;
  String notificationCount = '0';

  // Watchlist State - separate from Future to avoid page reload
  bool _inWatchlist = false;
  String? _folder;
  bool _isUpdatingWatchlist = false;

  // Episode List State
  bool _showEpisodes = false;
  bool isLoadingEpisodes = false;
  Map<String, dynamic> episodeData = {};
  Map<String, String> _thumbnails = {};
  int _currentPageStart = 1;
  final int _episodesPerPage = 100;
  final TextEditingController _searchController = TextEditingController();
  String _searchQuery = '';
  final ScrollController _episodeScrollController = ScrollController();

  final authService = AuthService();
  final RealtimeService _realtimeService = RealtimeService();

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom("/watch/${widget.animeId}/");
    fetchNotificationCount();
    _animeDetails = fetchAnimeDetails(widget.animeId);
  }

  @override
  void dispose() {
    _searchController.dispose();
    _episodeScrollController.dispose();
    super.dispose();
  }

  Future<AnimeDetails> fetchAnimeDetails(String id) async {
    final httpService = HttpService();
    final sessionInfo = await authService.getStoredSession();
    final slug = id; // Project-R uses slug from anime_id

    try {
      final response = await httpService.getProjectR(
        '/anime/$slug',
        session: sessionInfo,
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final details = AnimeDetails.fromJson({'data': data['data'] ?? data});

        setState(() {
          _inWatchlist = details.inWatchlist;
          _folder = details.folder;
        });

        _checkWatchlistStatus(id);

        return details;
      } else {
        throw Exception('Failed to load anime details. Status code: ${response.statusCode}');
      }
    } catch (e) {
      throw Exception('Failed to load anime details: $e');
    }
  }

  Future<void> fetchNotificationCount() async {
    // Notification count requires the old SvelteKit API — skip for now
    setState(() => notificationCount = '0');
  }

  Future<void> fetchEpisodeData() async {
    setState(() => isLoadingEpisodes = true);

    final sessionInfo = await authService.getStoredSession();
    final httpService = HttpService();

    try {
      // Fetch episodes from Project-R API: GET /anime/:slug/episodes
      final slug = widget.animeId;
      final response = await httpService.getProjectR(
        '/anime/$slug/episodes',
        session: sessionInfo,
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        // Response: { data: [episode_items], total, limit, offset }
        final episodes = data['data'] ?? [];
        setState(() {
          episodeData = data;
          isLoadingEpisodes = false;
        });
      } else {
        setState(() => isLoadingEpisodes = false);
      }
    } catch (e) {
      setState(() => isLoadingEpisodes = false);
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

  List<int> _generatePageGroups(int totalEpisodes) {
    List<int> groups = [];
    for (int i = 1; i <= totalEpisodes; i += _episodesPerPage) {
      groups.add(i);
    }
    return groups;
  }

  List<dynamic> _getFilteredEpisodes(List<dynamic> episodes) {
    return episodes.where((episode) {
      final number = episode['number'];
      if (number == null) return false;
      final n = number is int ? number : int.tryParse(number.toString()) ?? 0;
      final title = (episode['title'] ?? episode['titles']?[0] ?? '').toString().toLowerCase();
      final numberInRange = n >= _currentPageStart &&
          n < _currentPageStart + _episodesPerPage;

      if (_searchQuery.isEmpty) return numberInRange;

      final matchesNumber = n.toString().contains(_searchQuery);
      final matchesTitle = title.contains(_searchQuery);

      return numberInRange && (matchesNumber || matchesTitle);
    }).toList()
      ..sort((a, b) {
        final na = a['number'];
        final nb = b['number'];
        final naInt = na is int ? na : int.tryParse(na?.toString() ?? '0') ?? 0;
        final nbInt = nb is int ? nb : int.tryParse(nb?.toString() ?? '0') ?? 0;
        return naInt.compareTo(nbInt);
      });
  }

  Future<void> _updateWatchlistStatus(
      AnimeDetails anime, String newStatus) async {
    final sessionInfo = await authService.getStoredSession();
    if (sessionInfo?.anisurgeToken == null) return;
    final httpService = HttpService();

    setState(() => _isUpdatingWatchlist = true);
    final folderMap = {
      'Watching': 'WATCHING',
      'On Hold': 'PAUSED',
      'Plan To Watch': 'PLANNING',
      'Dropped': 'DROPPED',
      'Completed': 'COMPLETED',
    };

    try {
      // Update watchlist via BFF: POST /v1/watchlist { animeId, folder }
      final response = await httpService.postBff(
        '/watchlist',
        body: {
          'animeId': widget.animeId,
          'folder': newStatus,
        },
        session: sessionInfo!,
      );

      if (response.statusCode >= 200 && response.statusCode < 300) {
        setState(() {
          _inWatchlist = newStatus != 'Remove' && newStatus != 'REMOVE';
          _folder = _inWatchlist ? newStatus : null;
          _isUpdatingWatchlist = false;
        });
      } else {
        setState(() => _isUpdatingWatchlist = false);
      }
    } catch (e) {
      setState(() => _isUpdatingWatchlist = false);
    }
  }

  Future<void> _checkWatchlistStatus(String animeId) async {
    try {
      final session = await authService.getStoredSession();
      if (session?.anisurgeToken == null) return;
      final httpService = HttpService();
      final response = await httpService.get(
        '/v1/watchlist',
        queryParams: {'anime_id': animeId, 'limit': '1'},
        requireAuth: true,
        useBff: true,
      );
      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final items = data['data'] ?? [];
        if (items.isNotEmpty && items[0]['folder'] != null) {
          setState(() {
            _inWatchlist = true;
            _folder = items[0]['folder'];
          });
        }
      }
    } catch (_) {}
  }

  String? _mapFolderToDisplay(String folder) {
    return switch (folder.toUpperCase()) {
      'WATCHING' => 'Watching',
      'PAUSED' => 'On Hold',
      'PLANNING' => 'Plan To Watch',
      'DROPPED' => 'Dropped',
      'COMPLETED' => 'Completed',
      _ => folder,
    };
  }

  int? _anilistToMalId(int anilistId) {
    try {
      return null;
    } catch (_) {
      return null;
    }
  }

  Widget _buildWatchlistButton(AnimeDetails anime) {
    final options = [
      'Watching',
      'On Hold',
      'Plan To Watch',
      'Dropped',
      'Completed',
      'Remove'
    ];

    final displayFolder = _folder != null ? _mapFolderToDisplay(_folder!) : null;
    return CustomDropdown<String>(
      value: displayFolder,
      items: options,
      itemBuilder: (value) => value,
      onChanged: (value) {
        if (!_isUpdatingWatchlist) {
          _updateWatchlistStatus(anime, value);
        }
      },
      width: 180,
      child: AbsorbPointer(
        child: ElevatedButton(
          onPressed: null, // CustomDropdown handles the tap
          style: ElevatedButton.styleFrom(
            backgroundColor: Colors.white,
            disabledBackgroundColor: Colors.white,
            disabledForegroundColor: Colors.black,
            elevation: 0,
            padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 8),
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(8),
            ),
          ),
          child: SizedBox(
            width: 160, // Fixed width to maintain size
            child: Row(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                _isUpdatingWatchlist
                    ? LoadingAnimationWidget.threeArchedCircle(
                        color: Colors.black,
                        size: 16,
                      )
                    : Icon(
                        _inWatchlist ? Icons.bookmark : Icons.bookmark_border,
                        color: Colors.black,
                        size: 16,
                      ),
                const SizedBox(width: 8),
                Flexible(
                  child: Text(
                    _isUpdatingWatchlist
                        ? 'Updating...'
                        : _inWatchlist
                            ? (_folder ?? 'In Watchlist')
                            : 'Add to Watchlist',
                    style: const TextStyle(color: Colors.black),
                    textAlign: TextAlign.center,
                    overflow: TextOverflow.ellipsis,
                    maxLines: 1,
                    softWrap: false,
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildBannerImage(String bannerUrl, {required double height}) {
    if (bannerUrl.toLowerCase().endsWith('.svg')) {
      return SvgPicture.network(
        bannerUrl,
        height: height,
        width: double.infinity,
        fit: BoxFit.cover,
        placeholderBuilder: (BuildContext context) => _buildPlaceholder(height),
        errorBuilder:
            (BuildContext context, Object exception, StackTrace? stackTrace) {
          // print('Error loading SVG: $exception');
          return _buildPlaceholder(height);
        },
      );
    } else {
      return Image.network(
        bannerUrl,
        height: height,
        width: double.infinity,
        fit: BoxFit.cover,
        loadingBuilder: (BuildContext context, Widget child,
            ImageChunkEvent? loadingProgress) {
          if (loadingProgress == null) return child;
          return _buildPlaceholder(height);
        },
        errorBuilder:
            (BuildContext context, Object error, StackTrace? stackTrace) {
          // print('Error loading image: $error');
          return _buildPlaceholder(height);
        },
      );
    }
  }

  Widget _buildPlaceholder(double height) {
    return Container(
      height: height,
      color: Colors.grey[800],
      child: const Center(
        child: CircularProgressIndicator(),
      ),
    );
  }

  Widget _buildEpisodeList() {
    if (isLoadingEpisodes) {
      return Center(
        child: LoadingAnimationWidget.threeArchedCircle(
          color: Colors.red,
          size: 50,
        ),
      );
    }

    final rawEpisodes = episodeData['data'] ?? episodeData['episodes'] ?? episodeData['all_episodes'] ?? [];
    final episodes = rawEpisodes is List ? rawEpisodes : (rawEpisodes is Map ? rawEpisodes['data'] ?? [] : []);
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
      margin: const EdgeInsets.all(0),
      padding: const EdgeInsets.all(0),
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

                return Container(
                  key: ValueKey('${episode['number']}'),
                  margin: const EdgeInsets.only(bottom: 8),
                  decoration: BoxDecoration(
                    color: Colors.grey[900],
                    borderRadius: BorderRadius.circular(8),
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
                                .containsKey('${episode['number']}')
                            ? DecorationImage(
                                image: NetworkImage(
                                    _thumbnails['${episode['number']}']!),
                                fit: BoxFit.cover,
                              )
                              : null,
                      ),
                      child:
                          _thumbnails.containsKey('${episode['number']}')
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
                      style: const TextStyle(
                        color: Colors.white,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    subtitle: Text(
                      episode['title'] ?? episode['titles']?[0] ?? '',
                      style: TextStyle(color: Colors.grey[400], fontSize: 12),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                    ),
                    onTap: () {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => WatchAnimeScreen(
                            id: widget.animeId,
                            episodeNumber: episode['number'],
                          ),
                        ),
                      );
                    },
                  ),
                );
              },
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMobileLayout(AnimeDetails anime) {
    return Stack(
      children: [
        CustomScrollView(
          slivers: [
            SliverToBoxAdapter(
              child: Stack(
                children: [
                  _buildBannerImage(anime.banner, height: 300),
                  Container(
                    height: 300,
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.transparent,
                          Colors.black.withValues(alpha: 0.7),
                          Colors.black,
                        ],
                        stops: const [0.0, 0.7, 1.0],
                      ),
                    ),
                  ),
                  Padding(
                    padding: const EdgeInsets.fromLTRB(16, 200, 16, 16),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          crossAxisAlignment: CrossAxisAlignment.end,
                          children: [
                            ClipRRect(
                              borderRadius: BorderRadius.circular(8),
                              child: Image.network(
                                anime.cover,
                                height: 180,
                                width: 120,
                                fit: BoxFit.cover,
                              ),
                            ),
                            const SizedBox(width: 16),
                            Expanded(
                              child: Column(
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                  Text(
                                    anime.english,
                                    maxLines: 2,
                                    style: const TextStyle(
                                      color: Colors.white,
                                      fontSize: 24,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                  const SizedBox(height: 8),
                                  Row(
                                    children: [
                                      Icon(Icons.remove_red_eye,
                                          size: 16, color: Colors.grey[400]),
                                      const SizedBox(width: 4),
                                      Text(
                                        anime.views,
                                        style: TextStyle(
                                            color: Colors.grey[400],
                                            fontSize: 14),
                                      ),
                                      const SizedBox(width: 16),
                                      Icon(Icons.favorite,
                                          size: 16, color: Colors.grey[400]),
                                      const SizedBox(width: 4),
                                      Text(
                                        anime.likes,
                                        style: TextStyle(
                                            color: Colors.grey[400],
                                            fontSize: 14),
                                      ),
                                    ],
                                  ),
                                  const SizedBox(height: 8),
                                  Container(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 8, vertical: 4),
                                    decoration: BoxDecoration(
                                      color:
                                          Colors.green.withValues(alpha: 0.2),
                                      borderRadius: BorderRadius.circular(16),
                                    ),
                                    child: Text(
                                      anime.status,
                                      style: const TextStyle(
                                        color: Colors.green,
                                        fontSize: 12,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 24),

                        // Toggle Buttons
                        Container(
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.3),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: Row(
                            children: [
                              Expanded(
                                child: GestureDetector(
                                  onTap: () =>
                                      setState(() => _showEpisodes = false),
                                  child: AnimatedContainer(
                                    duration: const Duration(milliseconds: 300),
                                    curve: Curves.easeInOut,
                                    padding: const EdgeInsets.symmetric(
                                        vertical: 12),
                                    decoration: BoxDecoration(
                                      color: !_showEpisodes
                                          ? Colors.white
                                          : Colors.transparent,
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    alignment: Alignment.center,
                                    child: AnimatedDefaultTextStyle(
                                      duration:
                                          const Duration(milliseconds: 300),
                                      curve: Curves.easeInOut,
                                      style: TextStyle(
                                        color: !_showEpisodes
                                            ? Colors.black
                                            : Colors.white
                                                .withValues(alpha: 0.7),
                                        fontWeight: FontWeight.bold,
                                        fontSize: 15,
                                      ),
                                      child: const Text('Anime Info'),
                                    ),
                                  ),
                                ),
                              ),
                              Expanded(
                                child: GestureDetector(
                                  onTap: () {
                                    setState(() => _showEpisodes = true);
                                    if (episodeData.isEmpty) fetchEpisodeData();
                                  },
                                  child: AnimatedContainer(
                                    duration: const Duration(milliseconds: 300),
                                    curve: Curves.easeInOut,
                                    padding: const EdgeInsets.symmetric(
                                        vertical: 12),
                                    decoration: BoxDecoration(
                                      color: _showEpisodes
                                          ? Colors.white
                                          : Colors.transparent,
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    alignment: Alignment.center,
                                    child: AnimatedDefaultTextStyle(
                                      duration:
                                          const Duration(milliseconds: 300),
                                      curve: Curves.easeInOut,
                                      style: TextStyle(
                                        color: _showEpisodes
                                            ? Colors.black
                                            : Colors.white
                                                .withValues(alpha: 0.7),
                                        fontWeight: FontWeight.bold,
                                        fontSize: 15,
                                      ),
                                      child: const Text('Episodes'),
                                    ),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                        const SizedBox(height: 24),

                        if (_showEpisodes)
                          _buildEpisodeList()
                        else ...[
                          HtmlWidget(
                            anime.description,
                            textStyle: const TextStyle(
                              color: Colors.white,
                              fontSize: 14,
                              height: 1.5,
                            ),
                          ),
                          const SizedBox(height: 24),
                          const Text(
                            'Genres',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: anime.genres
                                .map(
                                  (genre) => GestureDetector(
                                    onTap: () {
                                      Navigator.push(
                                        context,
                                        MaterialPageRoute(
                                          builder: (context) =>
                                              SearchTab(initialGenre: genre),
                                        ),
                                      );
                                    },
                                    child: Container(
                                      padding: const EdgeInsets.symmetric(
                                          horizontal: 12, vertical: 6),
                                      decoration: BoxDecoration(
                                        color: Colors.grey[900],
                                        borderRadius: BorderRadius.circular(16),
                                      ),
                                      child: Text(
                                        genre,
                                        style: const TextStyle(
                                            color: Colors.white70),
                                      ),
                                    ),
                                  ),
                                )
                                .toList(),
                          ),
                          const SizedBox(height: 24),
                          const Text(
                            'Studios',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: 18,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 8),
                          Wrap(
                            spacing: 8,
                            runSpacing: 8,
                            children: anime.studios
                                .map(
                                  (studio) => Container(
                                    padding: const EdgeInsets.symmetric(
                                        horizontal: 12, vertical: 6),
                                    decoration: BoxDecoration(
                                      color: Colors.grey[900],
                                      borderRadius: BorderRadius.circular(16),
                                    ),
                                    child: Text(
                                      studio,
                                      style: const TextStyle(
                                          color: Colors.white70),
                                    ),
                                  ),
                                )
                                .toList(),
                          ),
                        ],
                        const SizedBox(
                            height: 100), // Space for floating buttons
                      ],
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),

        // Floating Bottom Bar
        Positioned(
          left: 0,
          right: 0,
          bottom: 0,
          child: Container(
            margin: const EdgeInsets.fromLTRB(40, 0, 40, 24),
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(16),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.4),
                  blurRadius: 20,
                  offset: const Offset(0, 10),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(16),
              child: BackdropFilter(
                filter: ImageFilter.blur(sigmaX: 10.0, sigmaY: 10.0),
                child: Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.5),
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(
                      color: Colors.white.withValues(alpha: 0.1),
                      width: 1,
                    ),
                    gradient: LinearGradient(
                      begin: Alignment.topLeft,
                      end: Alignment.bottomRight,
                      colors: [
                        Colors.white.withValues(alpha: 0.1),
                        Colors.white.withValues(alpha: 0.02),
                        Colors.transparent,
                      ],
                      stops: const [0.0, 0.4, 1.0],
                    ),
                  ),
                  child: Row(
                    children: [
                      if (anime.subbedCount > 0)
                        Expanded(
                          child: ElevatedButton.icon(
                            icon: const Icon(Icons.play_arrow),
                            label: const FittedBox(
                              fit: BoxFit.scaleDown,
                              child: Text('Watch Now'),
                            ),
                            onPressed: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) =>
                                      WatchAnimeScreen(id: anime.id),
                                ),
                              );
                            },
                            style: ElevatedButton.styleFrom(
                              backgroundColor: Colors.white,
                              foregroundColor: Colors.black,
                              elevation: 0,
                              shape: RoundedRectangleBorder(
                                borderRadius: BorderRadius.circular(8),
                              ),
                              padding: const EdgeInsets.symmetric(
                                  vertical: 14, horizontal: 8),
                            ),
                          ),
                        ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: _buildWatchlistButton(anime),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildDesktopLayout(AnimeDetails anime) {
    return SingleChildScrollView(
      child: Column(
        children: [
          Stack(
            clipBehavior: Clip.none,
            children: [
              _buildBannerImage(anime.banner, height: 500),
              Container(
                height: 500,
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withValues(alpha: 0.9),
                    ],
                    stops: const [0.0, 0.8],
                  ),
                ),
              ),
              Positioned(
                top: 300,
                left: 40,
                right: 40,
                child: Container(
                  padding: const EdgeInsets.fromLTRB(24, 48, 24, 24),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.7),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.network(
                          anime.cover,
                          height: 280,
                          width: 200,
                          fit: BoxFit.cover,
                        ),
                      ),
                      const SizedBox(width: 24),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              anime.english,
                              style: const TextStyle(
                                color: Colors.white,
                                fontSize: 28,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            const SizedBox(height: 12),
                            Row(
                              children: [
                                Row(
                                  children: [
                                    Icon(Icons.remove_red_eye,
                                        size: 16, color: Colors.grey[400]),
                                    const SizedBox(width: 4),
                                    Text(
                                      anime.views,
                                      style: TextStyle(
                                        color: Colors.grey[400],
                                        fontSize: 14,
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(width: 16),
                                Row(
                                  children: [
                                    Icon(Icons.favorite,
                                        size: 16, color: Colors.grey[400]),
                                    const SizedBox(width: 4),
                                    Text(
                                      anime.likes,
                                      style: TextStyle(
                                        color: Colors.grey[400],
                                        fontSize: 14,
                                      ),
                                    ),
                                  ],
                                ),
                                const SizedBox(width: 16),
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 2,
                                  ),
                                  decoration: BoxDecoration(
                                    color: Colors.green.withValues(alpha: 0.2),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: Text(
                                    anime.status,
                                    style: const TextStyle(
                                      color: Colors.green,
                                      fontSize: 12,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 24),
                            Row(
                              children: [
                                ElevatedButton.icon(
                                  icon: const Icon(Icons.play_arrow),
                                  label: const Text('Watch Now'),
                                  onPressed: () {
                                    Navigator.push(
                                      context,
                                      MaterialPageRoute(
                                        builder: (context) => WatchAnimeScreen(
                                            id: anime.id, episodeNumber: 1),
                                      ),
                                    );
                                  },
                                  style: ElevatedButton.styleFrom(
                                    backgroundColor: Colors.white,
                                    foregroundColor: Colors.black,
                                    elevation: 0,
                                    shape: RoundedRectangleBorder(
                                      borderRadius: BorderRadius.circular(8),
                                    ),
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 20,
                                      vertical: 12,
                                    ),
                                  ),
                                ),
                                const SizedBox(width: 12),
                                _buildWatchlistButton(anime),
                              ],
                            ),
                            const SizedBox(height: 24),
                            HtmlWidget(
                              anime.description,
                              textStyle: const TextStyle(
                                color: Colors.white70,
                                fontSize: 14,
                                height: 1.5,
                              ),
                            ),
                            const SizedBox(height: 24),
                            Row(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'Genres',
                                        style: TextStyle(
                                          color: Colors.white,
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      Wrap(
                                        spacing: 8,
                                        runSpacing: 8,
                                        children: anime.genres
                                            .map(
                                              (genre) => Container(
                                                padding:
                                                    const EdgeInsets.symmetric(
                                                  horizontal: 12,
                                                  vertical: 6,
                                                ),
                                                decoration: BoxDecoration(
                                                  color: Colors.grey[900],
                                                  borderRadius:
                                                      BorderRadius.circular(16),
                                                ),
                                                child: Text(
                                                  genre,
                                                  style: const TextStyle(
                                                    color: Colors.white70,
                                                    fontSize: 12,
                                                  ),
                                                ),
                                              ),
                                            )
                                            .toList(),
                                      ),
                                    ],
                                  ),
                                ),
                                const SizedBox(width: 24),
                                Expanded(
                                  child: Column(
                                    crossAxisAlignment:
                                        CrossAxisAlignment.start,
                                    children: [
                                      const Text(
                                        'Studios',
                                        style: TextStyle(
                                          color: Colors.white,
                                          fontSize: 18,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                      const SizedBox(height: 8),
                                      Wrap(
                                        spacing: 8,
                                        runSpacing: 8,
                                        children: anime.studios
                                            .map(
                                              (studio) => Container(
                                                padding:
                                                    const EdgeInsets.symmetric(
                                                  horizontal: 12,
                                                  vertical: 6,
                                                ),
                                                decoration: BoxDecoration(
                                                  color: Colors.grey[900],
                                                  borderRadius:
                                                      BorderRadius.circular(16),
                                                ),
                                                child: Text(
                                                  studio,
                                                  style: const TextStyle(
                                                    color: Colors.white70,
                                                    fontSize: 12,
                                                  ),
                                                ),
                                              ),
                                            )
                                            .toList(),
                                      ),
                                    ],
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final isDesktop = MediaQuery.of(context).size.width >= 1024;

    return Scaffold(
      backgroundColor: const Color(0xFF0B0B0B),
      extendBodyBehindAppBar: true,
      appBar: AppHeader(
        style: HeaderStyle.transparent,
        showBackButton: true,
      ),
      body: Stack(
        children: [
          FutureBuilder<AnimeDetails>(
            future: _animeDetails,
            builder: (context, snapshot) {
              if (snapshot.connectionState == ConnectionState.waiting) {
                return Center(
                  child: LoadingAnimationWidget.threeArchedCircle(
                    color: Colors.red,
                    size: 50,
                  ),
                );
              } else if (snapshot.hasError) {
                return Center(
                  child: Text(
                    'Error: ${snapshot.error}',
                    style: const TextStyle(color: Colors.white),
                  ),
                );
              } else if (snapshot.hasData) {
                return isDesktop
                    ? _buildDesktopLayout(snapshot.data!)
                    : _buildMobileLayout(snapshot.data!);
              } else {
                return const Center(
                  child: Text(
                    'No data available',
                    style: TextStyle(color: Colors.white),
                  ),
                );
              }
            },
          ),
          if (isLoading)
            Container(
              color: Colors.black.withValues(alpha: 0.5),
              child: Center(
                child: LoadingAnimationWidget.threeArchedCircle(
                  color: Colors.red,
                  size: 50,
                ),
              ),
            ),
        ],
      ),
    );
  }
}
