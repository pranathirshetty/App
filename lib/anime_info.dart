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
  });

  factory AnimeDetails.fromJson(Map<String, dynamic> json) {
    final data = json['data'];

    if (data == null) {
      throw Exception("Invalid data: 'data' field is null");
    }

    return AnimeDetails(
      id: data['id'] ?? '',
      english: data['english'] ?? 'Unknown',
      romaji: data['romaji'] ?? 'Unknown',
      native: data['native'] ?? 'Unknown',
      ageRating: data['ageRating'] ?? 'Unknown',
      malScore: (data['malScore'] != null) ? data['malScore'].toDouble() : null,
      averageScore: data['averageScore'] ?? 0,
      duration: data['duration'] ?? 0,
      genres: data['genres'] != null ? List<String>.from(data['genres']) : [],
      cover: data['cover'] ?? '',
      banner: data['banner'] ?? '',
      season: data['season'] ?? 'Unknown',
      startDate: data['startDate'] ?? 'Unknown',
      status: data['status'] ?? 'Unknown',
      synonyms:
          data['synonyms'] != null ? List<String>.from(data['synonyms']) : [],
      studios:
          data['studios'] != null ? List<String>.from(data['studios']) : [],
      type: data['type'] ?? 'Unknown',
      year: data['year'] ?? 0,
      epCount: data['epCount'] ?? 0,
      subbedCount: data['subbedCount'] ?? 0,
      dubbedCount: data['dubbedCount'] ?? 0,
      description: data['description'] ?? 'No description available.',
      inWatchlist: data['in_watchlist'] ?? false,
      views: data['views'] ?? '0',
      likes: data['likes'] ?? '0',
      folder: data['folder'],
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

    try {
      // Anime info can be fetched without auth, but user-specific data (like watchlist status) requires auth
      // Use /anime/[id]?type=api to get the same data as the SvelteKit frontend
      // Pass cookies when user is logged in to get user-specific data (watchlist status, likes, etc.)
      final response = await httpService.get(
        '/anime/$id',
        queryParams: {'type': 'api'},
        requireAuth: sessionInfo != null,
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        // SvelteKit might return data directly or wrapped
        if (data['data'] != null) {
          return AnimeDetails.fromJson(data);
        }
        return AnimeDetails.fromJson({'data': data});
      } else {
        throw Exception(
            'Failed to load anime details. Status code: ${response.statusCode}');
      }
    } catch (e) {
      // print('Error fetching anime details: $e');
      throw Exception('Failed to load anime details: $e');
    }
  }

  Future<void> fetchNotificationCount() async {
    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        final response = await httpService.get('/api/notifications/count',
            requireAuth: true);

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          if (data['success'] ?? true) {
            setState(() {
              notificationCount =
                  data['total']?.toString() ?? data['count']?.toString() ?? '0';
            });
          }
        }
      }
    } catch (e) {
      // print('Error fetching notification count: $e');
    }
  }

  Future<void> fetchEpisodeData() async {
    setState(() {
      isLoadingEpisodes = true;
    });

    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();
    final httpService = HttpService();

    try {
      // Fetch episode 1 to get the list of all episodes
      final response = await httpService.get(
        '/api/watch/${widget.animeId}/1',
        requireAuth: sessionInfo != null,
      );

      if (response.statusCode == 200) {
        setState(() {
          final data = json.decode(response.body);
          episodeData = data;
          // print("Episode Data Response: ${response.body}"); // Log the response
          isLoadingEpisodes = false;

          if (data['anime_info'] != null &&
              data['anime_info']['anilist'] != null) {
            fetchThumbnails(data['anime_info']['anilist']);
          }
        });
      } else {
        setState(() {
          isLoadingEpisodes = false;
        });
      }
    } catch (e) {
      setState(() {
        isLoadingEpisodes = false;
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

  Future<void> _updateWatchlistStatus(
      AnimeDetails anime, String newStatus) async {
    setState(() {
      isLoading = true;
    });

    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    if (sessionInfo == null) {
      setState(() {
        isLoading = false;
      });
      return;
    }

    final httpService = HttpService();

    try {
      // Use the correct endpoint and field names that match SvelteKit backend
      final response = await httpService.post(
        '/api/anime/watchlist',
        body: {
          'animeId': anime.id, // Backend expects camelCase
          'folder': newStatus, // Backend expects 'folder' not 'status'
        },
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        setState(() {
          _animeDetails = Future.value(AnimeDetails(
            id: anime.id,
            english: anime.english,
            romaji: anime.romaji,
            native: anime.native,
            ageRating: anime.ageRating,
            malScore: anime.malScore,
            averageScore: anime.averageScore,
            duration: anime.duration,
            genres: anime.genres,
            cover: anime.cover,
            banner: anime.banner,
            season: anime.season,
            startDate: anime.startDate,
            status: anime.status,
            synonyms: anime.synonyms,
            studios: anime.studios,
            type: anime.type,
            year: anime.year,
            epCount: anime.epCount,
            subbedCount: anime.subbedCount,
            dubbedCount: anime.dubbedCount,
            description: anime.description,
            inWatchlist: newStatus != 'Remove',
            views: anime.views,
            likes: anime.likes,
            folder: newStatus != 'Remove' ? newStatus : null,
          ));
          isLoading = false;
        });
      } else {
        // print(
        //     'Failed to update watchlist. Status code: ${response.statusCode}');
        setState(() {
          isLoading = false;
        });
      }
    } catch (e) {
      // print('Error updating watchlist: $e');
      setState(() {
        isLoading = false;
      });
    }
  }

  Widget _buildWatchlistButton(AnimeDetails anime) {
    return PopupMenuButton<String>(
      onSelected: (String result) {
        _updateWatchlistStatus(anime, result);
      },
      itemBuilder: (BuildContext context) => <PopupMenuEntry<String>>[
        ...<String>[
          'Watching',
          'On Hold',
          'Plan To Watch',
          'Dropped',
          'Completed'
        ].map(
          (String value) => PopupMenuItem<String>(
            value: value,
            child: Row(
              children: [
                Text(value),
                if (anime.folder == value) ...[
                  const SizedBox(width: 8),
                  const Icon(Icons.check, color: Colors.green, size: 16),
                ],
              ],
            ),
          ),
        ),
        const PopupMenuItem<String>(
          value: 'Remove',
          child: Text('Remove'),
        ),
      ],
      child: ElevatedButton.icon(
        icon: Icon(
          anime.inWatchlist ? Icons.bookmark : Icons.bookmark_border,
          color: Colors.black,
        ),
        label: FittedBox(
          fit: BoxFit.scaleDown,
          child: Text(
            anime.inWatchlist
                ? (anime.folder ?? 'In Watchlist')
                : 'Add to Watchlist',
            style: const TextStyle(color: Colors.black),
          ),
        ),
        onPressed: null,
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
        child: LoadingAnimationWidget.fourRotatingDots(
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
      margin: const EdgeInsets.all(0),
      padding: const EdgeInsets.all(0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
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
              hintText: 'Search episode...',
              hintStyle: const TextStyle(color: Colors.white70),
              fillColor: Colors.grey[900],
              filled: true,
              contentPadding:
                  const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(8),
                borderSide: BorderSide.none,
              ),
              prefixIcon: const Icon(Icons.search, color: Colors.white70),
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
          ListView.builder(
            padding: EdgeInsets.zero,
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: _getFilteredEpisodes(episodes).length,
            itemBuilder: (context, index) {
              final filteredEpisodes = _getFilteredEpisodes(episodes);
              final episode = filteredEpisodes[index];

              return Container(
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
                    episode['titles'][0] ?? '',
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
                                  (genre) => Container(
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
