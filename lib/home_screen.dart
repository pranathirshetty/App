import 'package:flutter/material.dart';
import 'package:carousel_slider/carousel_slider.dart';
import 'dart:ui';

import 'package:flutter_svg/flutter_svg.dart';
import 'dart:convert';
import 'package:kuudere/history_tab.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'package:kuudere/watch_anime.dart';
import 'schedule_tab.dart';
import 'search_tab.dart' hide AnimeItem;
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'anime_info.dart';
import 'package:kuudere/services/auth_service.dart';
import 'settings_tab.dart';
import 'watch_list_tab.dart' hide AnimeItem;
import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/widgets/app_header.dart';
import 'package:kuudere/services/ai_service.dart';
import 'package:kuudere/models/anime_models.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _currentIndex = 1;
  final ScrollController _scrollController = ScrollController();
  double _scrollProgress = 0.0;
  List<ContinueWatchingItem> continueWatching = [];
  List<AnimeItem> topAiring = [];
  List<AnimeItem> latestEpisodes = [];
  List<AnimeItem> newOnSite = [];
  List<AnimeItem> topUpcoming = [];
  bool isLoading = true;
  int ctotal = 0;
  final authService = AuthService();
  final RealtimeService _realtimeService = RealtimeService();
  final CarouselSliderController _carouselController =
      CarouselSliderController();
  final AiService _aiService = AiService();
  List<AnimeItem> aiRecommendations = [];
  bool isLoadingAi = false;
  String _aiTagline = "";

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom("home");
    fetchData();
    _scrollController.addListener(() {
      // Calculate scroll progress over first 100 pixels of scroll
      final progress = (_scrollController.offset / 100).clamp(0.0, 1.0);
      if (progress != _scrollProgress) {
        setState(() {
          _scrollProgress = progress;
        });
      }
    });
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  Future<void> fetchData() async {
    setState(() {
      isLoading = true;
    });

    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();

      // Fetch home data from Project-R API
      final homeResponse = await httpService.getProjectR(
        '/home',
        session: sessionInfo,
      );

      if (homeResponse.statusCode == 200) {
        final responseData = json.decode(homeResponse.body);
        // Project-R /home returns snake_case: latest_aired, new_on_site, trending, upcoming
        final latestAired = responseData['latest_aired'] ?? [];
        final newOnSiteRaw = responseData['new_on_site'] ?? [];
        final upcoming = responseData['upcoming'] ?? [];
        final trending = responseData['trending'] ?? [];

        // Fetch Continue Watching from BFF
        List<ContinueWatchingItem> continueWatchingList = [];
        try {
          final sessionInfo = await authService.getStoredSession();
          if (sessionInfo != null) {
            final continueResponse =
                await httpService.getBff('/watch/continue', session: sessionInfo);
            if (continueResponse.statusCode == 200) {
              final continueData = json.decode(continueResponse.body);
              final continueItems = continueData['data'] ?? [];
              continueWatchingList = (continueItems as List)
                  .map((item) =>
                      ContinueWatchingItem.fromBffJson(item))
                  .toList();
            }
          }
        } catch (e) {
          // Continue watching is optional
        }

        setState(() {
          topAiring = (trending as List)
              .map((item) => AnimeItem.fromProjectRJson(item))
              .toList();

          latestEpisodes = (latestAired as List)
              .map((item) => AnimeItem.fromProjectRJson(item))
              .toList();

          newOnSite = (newOnSite as List)
              .map((item) => AnimeItem.fromProjectRJson(item))
              .toList();

          topUpcoming = (upcoming as List)
              .map((item) => AnimeItem.fromProjectRJson(item))
              .toList();

          continueWatching = continueWatchingList;
          ctotal = continueWatchingList.length;
          isLoading = false;
        });

        _fetchAiRecommendations();
      } else {
        setState(() => isLoading = false);
      }
    } catch (e) {
      setState(() => isLoading = false);
    }
  }

  Future<void> _fetchAiRecommendations() async {
    setState(() {
      isLoadingAi = true;
    });

    try {
      final Map<String, List<String>> history = {
        'Continue Watching': [],
        'Watching': [],
        'On Hold': [],
        'Plan to Watch': [],
        'Dropped': [],
        'Completed': [],
      };

      // 1. Add Continue Watching titles
      for (var item in continueWatching.take(20)) {
        history['Continue Watching']!.add("${item.displayTitle} (Ep ${item.displayEpisode})");
      }

      // 2. Fetch Watchlist titles
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();

      if (sessionInfo?.anisurgeToken != null) {
        try {
          final response = await httpService.get(
            '/v1/watchlist',
            queryParams: {'limit': '50'},
            requireAuth: true,
            useBff: true,
          );

          if (response.statusCode == 200) {
            final data = json.decode(response.body);
            final items = data['data'] ?? data['watchlist'] ?? [];
            if (items is List) {
              for (var item in items) {
                // Extract title
                String title =
                    item['english'] ?? item['romaji'] ?? item['title'] ?? '';
                String status = item['status'] ?? 'Unknown';

                if (title.isNotEmpty) {
                  // Normalize status
                  // API mostly returns uppercase: WATCHING, COMPLETED, ON_HOLD, DROPPED, PLAN_TO_WATCH
                  // But let's check basic mapping
                  if (status == 'WATCHING' || status == 'Watching') {
                    history['Watching']!.add(title);
                  } else if (status == 'COMPLETED' || status == 'Completed') {
                    history['Completed']!.add(title);
                  } else if (status == 'ON_HOLD' || status == 'On Hold') {
                    history['On Hold']!.add(title);
                  } else if (status == 'DROPPED' || status == 'Dropped') {
                    history['Dropped']!.add(title);
                  } else if (status == 'PLAN_TO_WATCH' ||
                      status == 'Plan to Watch') {
                    history['Plan to Watch']!.add(title);
                  } else {
                    // Fallback if status is unknown? Maybe add to Plan to Watch or just ignore
                  }
                }
              }
            }
          }
        } catch (e) {
          // print('Error fetching watchlist for AI: $e');
        }
      }

      final result = await _aiService.getRecommendations(history);

      if (mounted) {
        setState(() {
          aiRecommendations = result.items;
          _aiTagline = result.tagline;
          isLoadingAi = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          isLoadingAi = false;
        });
      }
    }
  }

  void _navigateToWatchlist() {
    setState(() {
      _currentIndex = 3; // Index of the Watchlist tab
    });
  }

  Widget _buildContinueWatchingSection() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Continue Watching',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 20,
                  fontWeight: FontWeight.bold,
                ),
              ),
              TextButton(
                onPressed: () {
                  Navigator.push(
                    context,
                    MaterialPageRoute(builder: (context) => HistoryTab()),
                  );
                },
                child: Text(
                  'View more >',
                  style: TextStyle(color: Colors.grey, fontSize: 14),
                ),
              ),
            ],
          ),
        ),
        SizedBox(
          height: 220,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            itemCount: continueWatching.length,
            itemBuilder: (context, index) {
              final item = continueWatching[index];
              return Padding(
                padding: EdgeInsets.only(
                  left: index == 0 ? 16 : 8,
                  right: index == continueWatching.length - 1 ? 16 : 8,
                ),
                child: ContinueWatchingCard(
                  imageUrl: item.thumbnail,
                  title: item.title,
                  episodeNumber: item.episode,
                  currentTime: item.progress,
                  totalDuration: item.duration.toString(),
                  link: item.slug,
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final bool isHomeTab = _currentIndex == 1;
    return Scaffold(
      backgroundColor: const Color(0xFF0B0B0B),
      extendBodyBehindAppBar: isHomeTab,
      extendBody: true,
      appBar: AppHeader(
        style: isHomeTab ? HeaderStyle.gradient : HeaderStyle.solid,
        scrollProgress: isHomeTab ? _scrollProgress : null,
      ),
      body: IndexedStack(
        index: _currentIndex,
        children: [
          const ScheduleTab(),
          _buildHomeContent(),
          const SearchTab(),
          const WatchListTab(),
          SettingsTab(onWatchlistTap: _navigateToWatchlist),
        ],
      ),
      bottomNavigationBar: Container(
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
              child: BottomNavigationBar(
                backgroundColor: Colors.transparent,
                type: BottomNavigationBarType.fixed,
                selectedItemColor: Colors.redAccent,
                unselectedItemColor: Colors.white.withValues(alpha: 0.6),
                currentIndex: _currentIndex,
                elevation: 0,
                iconSize: 26,
                showSelectedLabels: false,
                showUnselectedLabels: false,
                items: const [
                  BottomNavigationBarItem(
                    icon: Icon(Icons.calendar_today),
                    label: 'Schedule',
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(Icons.home),
                    label: 'Home',
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(Icons.search),
                    label: 'Search',
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(Icons.bookmark_outline),
                    label: 'Watchlist',
                  ),
                  BottomNavigationBarItem(
                    icon: Icon(Icons.settings_outlined),
                    label: 'Settings',
                  ),
                ],
                onTap: (index) {
                  setState(() {
                    _currentIndex = index;
                  });
                },
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildAiRecommendationsSection() {
    if (isLoadingAi) {
      return Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'AI Recommendations',
              style: TextStyle(
                color: Colors.white,
                fontSize: 20,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 16),
            Center(
              child: Column(
                children: [
                  LoadingAnimationWidget.staggeredDotsWave(
                    color: Colors.redAccent,
                    size: 30,
                  ),
                  const SizedBox(height: 8),
                  Text(
                    "AI is cooking your recommendations...",
                    style: TextStyle(
                      color: Colors.white.withValues(alpha: 0.7),
                      fontSize: 14,
                      fontStyle: FontStyle.italic,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      );
    }

    if (aiRecommendations.isEmpty) return const SizedBox.shrink();

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.symmetric(horizontal: 16.0),
          child: Row(
            children: [
              const Icon(Icons.auto_awesome, color: Colors.amber, size: 24),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'AI Recommended for You',
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 20,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    if (_aiTagline.isNotEmpty)
                      Text(
                        _aiTagline,
                        style: TextStyle(
                          color: Colors.white.withValues(alpha: 0.7),
                          fontSize: 12,
                          fontStyle: FontStyle.italic,
                        ),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                      ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        SizedBox(
          height: 220, // Match typical card height
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            scrollDirection: Axis.horizontal,
            itemCount: aiRecommendations.length,
            separatorBuilder: (context, index) => const SizedBox(width: 12),
            itemBuilder: (context, index) {
              final item = aiRecommendations[index];
              return SizedBox(
                width: 140,
                child: GestureDetector(
                  onTap: () {
                    Navigator.push(
                      context,
                      MaterialPageRoute(
                        builder: (context) => AnimeInfoScreen(animeId: item.id),
                      ),
                    );
                  },
                  child: Column(
                    children: [
                      Expanded(
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(12),
                          child: Stack(
                            fit: StackFit.expand,
                            children: [
                              Image.network(
                                item.imageUrl,
                                fit: BoxFit.cover,
                                errorBuilder: (context, error, stackTrace) =>
                                    Container(color: Colors.grey[900]),
                              ),
                              Positioned(
                                top: 8,
                                right: 8,
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                      horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: Colors.black.withValues(alpha: 0.7),
                                    borderRadius: BorderRadius.circular(4),
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(Icons.star,
                                          size: 10, color: Colors.amber),
                                      const SizedBox(width: 2),
                                      Text(
                                        item.malScore?.toString() ?? 'N/A',
                                        style: const TextStyle(
                                          color: Colors.white,
                                          fontSize: 10,
                                          fontWeight: FontWeight.bold,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 8),
                      Text(
                        item.displayTitle,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          color: Colors.white,
                          fontSize: 12,
                          fontWeight: FontWeight.w500,
                        ),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildHomeContent() {
    if (isLoading) {
      return Center(
        child: LoadingAnimationWidget.threeArchedCircle(
          color: Colors.red,
          size: 50,
        ),
      );
    }

    return RefreshIndicator(
      onRefresh: fetchData,
      color: Colors.red,
      child: SingleChildScrollView(
        controller: _scrollController,
        child: Column(
          children: [
            // Hero Carousel
            if (topAiring.isNotEmpty)
              Stack(
                children: [
                  CarouselSlider(
                    carouselController: _carouselController,
                    options: CarouselOptions(
                      height: MediaQuery.of(context).size.height * 0.85,
                      viewportFraction: 1.0,
                      enlargeCenterPage: false,
                      autoPlay: true,
                      autoPlayInterval: const Duration(seconds: 6),
                      autoPlayAnimationDuration:
                          const Duration(milliseconds: 800),
                      autoPlayCurve: Curves.fastOutSlowIn,
                      padEnds: false,
                    ),
                    items: topAiring.asMap().entries.map((entry) {
                      final index = entry.key;
                      final item = entry.value;
                      return Builder(
                        builder: (BuildContext context) {
                          final screenWidth = MediaQuery.of(context).size.width;
                          final isDesktop = screenWidth > 800;

                          final imageUrl = isDesktop
                              ? (item.banner ??
                                  (item.image.isNotEmpty
                                      ? item.image
                                      : ''))
                              : (item.image.isNotEmpty
                                  ? item.image
                                  : (item.banner ?? ''));

                          return GestureDetector(
                            onTap: () {
                              Navigator.push(
                                context,
                                MaterialPageRoute(
                                  builder: (context) =>
                                      AnimeInfoScreen(animeId: item.id),
                                ),
                              );
                            },
                            child: Container(
                              width: MediaQuery.of(context).size.width,
                              decoration: BoxDecoration(
                                color: Colors.black,
                                image: DecorationImage(
                                  image: NetworkImage(imageUrl.isNotEmpty
                                      ? imageUrl
                                      : 'https://via.placeholder.com/800x400'),
                                  fit: BoxFit.cover,
                                  alignment: isDesktop
                                      ? Alignment.centerRight
                                      : Alignment.center,
                                ),
                              ),
                              child: Stack(
                                children: [
                                  // Desktop: Angled Glass/Gradient Overlay on Left
                                  if (isDesktop)
                                    Positioned.fill(
                                      child: ClipPath(
                                        clipper: _HeroContentClipper(),
                                        child: BackdropFilter(
                                          filter: ImageFilter.blur(
                                              sigmaX: 20.0, sigmaY: 20.0),
                                          child: Container(
                                            decoration: BoxDecoration(
                                              gradient: LinearGradient(
                                                begin: Alignment.centerLeft,
                                                end: Alignment.centerRight,
                                                colors: [
                                                  Colors.black
                                                      .withValues(alpha: 0.9),
                                                  Colors.black
                                                      .withValues(alpha: 0.8),
                                                  Colors.black
                                                      .withValues(alpha: 0.6),
                                                  Colors.black
                                                      .withValues(alpha: 0.2),
                                                ],
                                                stops: const [
                                                  0.0,
                                                  0.4,
                                                  0.8,
                                                  1.0
                                                ],
                                              ),
                                            ),
                                          ),
                                        ),
                                      ),
                                    )
                                  else
                                    // Mobile: Standard Bottom Gradient
                                    Container(
                                      decoration: BoxDecoration(
                                        gradient: LinearGradient(
                                          begin: Alignment.topCenter,
                                          end: Alignment.bottomCenter,
                                          colors: [
                                            Colors.black.withValues(alpha: 0.6),
                                            Colors.transparent,
                                            Colors.black.withValues(alpha: 0.3),
                                            Colors.black.withValues(alpha: 0.7),
                                            Colors.black.withValues(alpha: 0.9),
                                            Colors.black,
                                          ],
                                          stops: const [
                                            0.0,
                                            0.2,
                                            0.4,
                                            0.6,
                                            0.8,
                                            1.0
                                          ],
                                        ),
                                      ),
                                    ),

                                  // Content
                                  Padding(
                                    padding: EdgeInsets.fromLTRB(
                                      isDesktop ? 60 : 20,
                                      MediaQuery.of(context).padding.top +
                                          kToolbarHeight +
                                          (isDesktop ? 20 : 40),
                                      isDesktop ? 60 : 20,
                                      20,
                                    ),
                                    child: Column(
                                      mainAxisAlignment: isDesktop
                                          ? MainAxisAlignment.center
                                          : MainAxisAlignment.end,
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        // Spotlight Tag (Desktop Only)
                                        if (isDesktop) ...[
                                          Container(
                                            padding: const EdgeInsets.symmetric(
                                                horizontal: 16, vertical: 8),
                                            decoration: BoxDecoration(
                                              color: Colors.white
                                                  .withValues(alpha: 0.1),
                                              borderRadius:
                                                  BorderRadius.circular(30),
                                              border: Border.all(
                                                  color: Colors.white
                                                      .withValues(alpha: 0.2)),
                                            ),
                                            child: Row(
                                              mainAxisSize: MainAxisSize.min,
                                              children: [
                                                Container(
                                                  width: 8,
                                                  height: 8,
                                                  decoration: BoxDecoration(
                                                    color: Colors.redAccent,
                                                    shape: BoxShape.circle,
                                                    boxShadow: [
                                                      BoxShadow(
                                                        color: Colors.redAccent
                                                            .withValues(
                                                                alpha: 0.5),
                                                        blurRadius: 6,
                                                        spreadRadius: 2,
                                                      ),
                                                    ],
                                                  ),
                                                ),
                                                const SizedBox(width: 8),
                                                Text(
                                                  '#${index + 1} Spotlight',
                                                  style: const TextStyle(
                                                    color: Colors.white,
                                                    fontWeight: FontWeight.w600,
                                                    fontSize: 14,
                                                    letterSpacing: 0.5,
                                                  ),
                                                ),
                                              ],
                                            ),
                                          ),
                                          const SizedBox(height: 24),
                                        ] else ...[
                                          // Mobile Tags
                                          Row(
                                            children: [
                                              _buildTag('16+'),
                                              const SizedBox(width: 8),
                                              _buildTag(
                                                '${item.format}'
                                                '${item.subbed > 0 ? ' | SUB' : ''}'
                                                '${item.dubbed > 0 ? ' | DUB' : ''}',
                                              ),
                                            ],
                                          ),
                                          const SizedBox(height: 12),
                                        ],

                                        // Title
                                        SizedBox(
                                          width: isDesktop
                                              ? screenWidth * 0.45
                                              : null,
                                          child: Text(
                                            item.displayTitle,
                                            style: TextStyle(
                                              color: Colors.white,
                                              fontSize: isDesktop ? 56 : 28,
                                              fontWeight: isDesktop
                                                  ? FontWeight.w800
                                                  : FontWeight.bold,
                                              height: isDesktop ? 1.1 : 1.2,
                                              letterSpacing:
                                                  isDesktop ? -1.0 : 0.0,
                                            ),
                                            maxLines: isDesktop ? 3 : 2,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ),
                                        const SizedBox(height: 16),

                                        // Metadata (Desktop)
                                        if (isDesktop) ...[
                                          Row(
                                            children: [
                                              _buildMetaTag(
                                                  item.format, Icons.movie),
                                              const SizedBox(width: 12),
                                              _buildMetaTag(
                                                  '${item.epCount} Episodes',
                                                  Icons.layers),
                                              const SizedBox(width: 12),
                                              if (item.malScore != null)
                                                Container(
                                                  padding: const EdgeInsets
                                                      .symmetric(
                                                      horizontal: 8,
                                                      vertical: 4),
                                                  decoration: BoxDecoration(
                                                    color: const Color(
                                                        0xFF00C853), // MAL Green
                                                    borderRadius:
                                                        BorderRadius.circular(
                                                            4),
                                                  ),
                                                  child: Text(
                                                    'MAL ${item.malScore}',
                                                    style: const TextStyle(
                                                      color: Colors.white,
                                                      fontWeight:
                                                          FontWeight.bold,
                                                      fontSize: 13,
                                                    ),
                                                  ),
                                                ),
                                              const SizedBox(width: 12),
                                              Container(
                                                padding:
                                                    const EdgeInsets.symmetric(
                                                        horizontal: 6,
                                                        vertical: 2),
                                                decoration: BoxDecoration(
                                                  border: Border.all(
                                                      color: Colors.white70),
                                                  borderRadius:
                                                      BorderRadius.circular(4),
                                                ),
                                                child: const Text('HD',
                                                    style: TextStyle(
                                                        color: Colors.white,
                                                        fontSize: 12,
                                                        fontWeight:
                                                            FontWeight.w600)),
                                              ),
                                            ],
                                          ),
                                          const SizedBox(height: 24),
                                        ],

                                        // Description
                                        SizedBox(
                                          width: isDesktop
                                              ? screenWidth * 0.4
                                              : null,
                                          child: Text(
                                            _stripHtmlTags(item.description),
                                            style: TextStyle(
                                              color: Colors.white
                                                  .withValues(alpha: 0.9),
                                              fontSize: isDesktop ? 16 : 14,
                                              height: 1.5,
                                              fontWeight: FontWeight.w400,
                                            ),
                                            maxLines: isDesktop ? 4 : 3,
                                            overflow: TextOverflow.ellipsis,
                                          ),
                                        ),
                                        SizedBox(height: isDesktop ? 32 : 20),

                                        // Buttons
                                        Row(
                                          children: [
                                            if (isDesktop) ...[
                                              ElevatedButton.icon(
                                                onPressed: () {
                                                  Navigator.push(
                                                    context,
                                                    MaterialPageRoute(
                                                      builder: (context) =>
                                                          WatchAnimeScreen(
                                                              id: item.id),
                                                    ),
                                                  );
                                                },
                                                style: ElevatedButton.styleFrom(
                                                  backgroundColor: Colors.white,
                                                  foregroundColor: Colors.black,
                                                  padding: const EdgeInsets
                                                      .symmetric(
                                                    vertical: 22,
                                                    horizontal: 32,
                                                  ),
                                                  elevation: 0,
                                                  shape: RoundedRectangleBorder(
                                                    borderRadius:
                                                        BorderRadius.circular(
                                                            12),
                                                  ),
                                                ),
                                                icon: const Icon(
                                                    Icons.play_arrow,
                                                    size: 24),
                                                label: const Text(
                                                  'Start Watching',
                                                  style: TextStyle(
                                                    fontWeight: FontWeight.bold,
                                                    fontSize: 16,
                                                    letterSpacing: 0.5,
                                                  ),
                                                ),
                                              ),
                                              const SizedBox(width: 16),
                                              TextButton.icon(
                                                onPressed: () {
                                                  Navigator.push(
                                                    context,
                                                    MaterialPageRoute(
                                                      builder: (context) =>
                                                          AnimeInfoScreen(
                                                              animeId: item.id),
                                                    ),
                                                  );
                                                },
                                                style: TextButton.styleFrom(
                                                  foregroundColor: Colors.white,
                                                  padding: const EdgeInsets
                                                      .symmetric(
                                                      horizontal: 24,
                                                      vertical: 22),
                                                  shape: RoundedRectangleBorder(
                                                      borderRadius:
                                                          BorderRadius.circular(
                                                              12)),
                                                  backgroundColor: Colors.white
                                                      .withValues(alpha: 0.1),
                                                ),
                                                icon: const Icon(
                                                    Icons.info_outline,
                                                    size: 24),
                                                label: const Text('Details',
                                                    style: TextStyle(
                                                        fontWeight:
                                                            FontWeight.w600,
                                                        fontSize: 16)),
                                              ),
                                            ] else ...[
                                              // Mobile Buttons
                                              Expanded(
                                                child: ElevatedButton.icon(
                                                  onPressed: () {
                                                    Navigator.push(
                                                      context,
                                                      MaterialPageRoute(
                                                        builder: (context) =>
                                                            WatchAnimeScreen(
                                                                id: item.id),
                                                      ),
                                                    );
                                                  },
                                                  style:
                                                      ElevatedButton.styleFrom(
                                                    backgroundColor:
                                                        Colors.redAccent,
                                                    padding: const EdgeInsets
                                                        .symmetric(
                                                      vertical: 16,
                                                      horizontal: 24,
                                                    ),
                                                    shape:
                                                        RoundedRectangleBorder(
                                                      borderRadius:
                                                          BorderRadius.circular(
                                                              8),
                                                    ),
                                                  ),
                                                  icon: const Icon(
                                                      Icons.play_arrow,
                                                      color: Colors.white),
                                                  label: const Text(
                                                    'Watch Now',
                                                    style: TextStyle(
                                                      color: Colors.white,
                                                      fontSize: 16,
                                                      fontWeight:
                                                          FontWeight.bold,
                                                    ),
                                                  ),
                                                ),
                                              ),
                                              const SizedBox(width: 12),
                                              Container(
                                                decoration: BoxDecoration(
                                                  border: Border.all(
                                                      color: Colors.white),
                                                  borderRadius:
                                                      BorderRadius.circular(8),
                                                ),
                                                child: IconButton(
                                                  onPressed: () {
                                                    Navigator.push(
                                                      context,
                                                      MaterialPageRoute(
                                                        builder: (context) =>
                                                            AnimeInfoScreen(
                                                                animeId:
                                                                    item.id),
                                                      ),
                                                    );
                                                  },
                                                  icon: const Icon(
                                                    Icons.info_outline,
                                                    color: Colors.white,
                                                    size: 24,
                                                  ),
                                                  style: IconButton.styleFrom(
                                                    padding:
                                                        const EdgeInsets.all(
                                                            12),
                                                  ),
                                                ),
                                              ),
                                            ],
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          );
                        },
                      );
                    }).toList(),
                  ),
                  if (MediaQuery.of(context).size.width > 800)
                    Positioned(
                      bottom: 40,
                      right: 60,
                      child: Row(
                        children: [
                          _buildCarouselNavBtn(Icons.arrow_back,
                              () => _carouselController.previousPage()),
                          const SizedBox(width: 16),
                          _buildCarouselNavBtn(Icons.arrow_forward,
                              () => _carouselController.nextPage()),
                        ],
                      ),
                    ),
                ],
              ),

            // Continue Watching Section (show if we have continue watching items)
            if (continueWatching.isNotEmpty) ...[
              _buildContinueWatchingSection(),
              const SizedBox(height: 24),
            ],

            // AI Recommendations
            if (isLoadingAi || aiRecommendations.isNotEmpty) ...[
              _buildAiRecommendationsSection(),
              const SizedBox(height: 24),
            ],

            // Latest Episodes Section
            _buildResponsiveSection(
              context: context,
              title: 'Latest Episodes',
              items: latestEpisodes,
            ),

            // New On Site Section
            _buildResponsiveSection(
              context: context,
              title: 'New On App',
              items: newOnSite,
            ),

            // Top Upcoming Section
            _buildResponsiveSection(
              context: context,
              title: 'Top Upcoming',
              items: topUpcoming,
            ),
            const SizedBox(height: 100),
          ],
        ),
      ),
    );
  }

  Widget _buildResponsiveSection({
    required BuildContext context,
    required String title,
    required List<AnimeItem> items,
  }) {
    final isXlScreen = MediaQuery.of(context).size.width >= 1280;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.all(16.0),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                title,
                style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold),
              ),
              if (title != 'Top Upcoming')
                TextButton(
                  onPressed: () {
                    if (title == 'Latest Episodes') {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) =>
                              const SearchTab(initialSort: 'latest'),
                        ),
                      );
                    } else if (title == 'New On App') {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) =>
                              const SearchTab(initialSort: 'newest'),
                        ),
                      );
                    }
                  },
                  child: const Text('View more >',
                      style: TextStyle(color: Colors.grey, fontSize: 14)),
                ),
            ],
          ),
        ),
        SizedBox(
          height: isXlScreen ? 280 : 240,
          child: ListView.builder(
            scrollDirection: Axis.horizontal,
            itemCount: items.length,
            itemBuilder: (context, index) => Padding(
              padding: EdgeInsets.only(
                left: index == 0 ? 16 : 8,
                right: index == items.length - 1 ? 16 : 8,
              ),
              child: SizedBox(
                width: isXlScreen ? 200 : 160,
                child: GestureDetector(
                  onTap: () {
                    if (title == 'Latest Episodes') {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) => WatchAnimeScreen(
                            id: items[index].id,
                            episodeNumber: items[index].epCount,
                            lang:
                                'sub', // You may need to adjust this based on your data
                          ),
                        ),
                      );
                    } else {
                      Navigator.push(
                        context,
                        MaterialPageRoute(
                          builder: (context) =>
                              AnimeInfoScreen(animeId: items[index].id),
                        ),
                      );
                    }
                  },
                  child: AnimeCard(item: items[index]),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(height: 24),
      ],
    );
  }

  Widget _buildTag(String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.6),
        borderRadius: BorderRadius.circular(4),
        border: Border.all(
          color: Colors.white.withValues(alpha: 0.2),
          width: 0.5,
        ),
      ),
      child: Text(
        text,
        style: TextStyle(
          color: Colors.white,
          fontSize: 12,
          fontWeight: FontWeight.w500,
        ),
      ),
    );
  }

  String _stripHtmlTags(String htmlString) {
    String text = htmlString
        .replaceAll(RegExp(r'<br\s*/?>', caseSensitive: false), '\n')
        .replaceAll(RegExp(r'</p>', caseSensitive: false), '\n\n')
        .replaceAll(RegExp(r'</div>', caseSensitive: false), '\n');
    return text.replaceAll(RegExp(r'<[^>]*>'), '').trim();
  }

  Widget _buildMetaTag(String text, IconData icon) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 14, color: Colors.white70),
          const SizedBox(width: 6),
          Text(
            text,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 13,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildCarouselNavBtn(IconData icon, VoidCallback onPressed) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.1),
        shape: BoxShape.circle,
        border: Border.all(color: Colors.white.withValues(alpha: 0.1)),
      ),
      child: IconButton(
        onPressed: onPressed,
        icon: Icon(icon, color: Colors.white),
        padding: const EdgeInsets.all(12),
        constraints: const BoxConstraints(),
      ),
    );
  }
}

class _HeroContentClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    final path = Path();
    final w = size.width;
    final h = size.height;

    path.moveTo(0, 0);
    // Top edge goes to 60% width
    path.lineTo(w * 0.6, 0);
    // Angled line down to 45% width at bottom
    path.lineTo(w * 0.45, h);
    // Bottom edge back to start
    path.lineTo(0, h);
    path.close();

    return path;
  }

  @override
  bool shouldReclip(CustomClipper<Path> oldClipper) => false;
}

class ContinueWatchingCard extends StatefulWidget {
  final String imageUrl;
  final String title;
  final int episodeNumber;
  final String currentTime;
  final String totalDuration;
  final String link;

  const ContinueWatchingCard({
    super.key,
    required this.imageUrl,
    required this.title,
    required this.episodeNumber,
    required this.currentTime,
    required this.totalDuration,
    required this.link,
  });

  @override
  State<ContinueWatchingCard> createState() => _ContinueWatchingCardState();
}

class _ContinueWatchingCardState extends State<ContinueWatchingCard> {
  bool _isHovered = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: GestureDetector(
        onTap: () {
          final uri = Uri.parse(widget.link);
          final lang = uri.queryParameters['lang'];
          Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => WatchAnimeScreen(
                id: widget.link.split('/')[2],
                episodeNumber: widget.episodeNumber,
                lang: lang,
              ),
            ),
          );
        },
        child: SizedBox(
          width: 280,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                height: 157.5, // 16:9 aspect ratio
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                ),
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(8),
                  child: Stack(
                    children: [
                      // Image with scale animation
                      AnimatedContainer(
                        duration: const Duration(milliseconds: 300),
                        curve: Curves.easeInOut,
                        transform: Matrix4.diagonal3Values(
                          _isHovered ? 1.05 : 1.0,
                          _isHovered ? 1.05 : 1.0,
                          1.0,
                        ),
                        child: Image.network(
                          widget.imageUrl,
                          fit: BoxFit.cover,
                          width: double.infinity,
                          height: double.infinity,
                        ),
                      ),

                      // Play button overlay
                      AnimatedOpacity(
                        duration: const Duration(milliseconds: 300),
                        opacity: _isHovered ? 1.0 : 0.0,
                        child: Container(
                          color: Colors.black.withValues(alpha: 0.5),
                          child: Center(
                            child: Container(
                              padding: const EdgeInsets.all(12),
                              decoration: const BoxDecoration(
                                color: Colors.white,
                                shape: BoxShape.circle,
                              ),
                              child: const Icon(
                                Icons.play_arrow,
                                color: Colors.grey,
                                size: 24,
                              ),
                            ),
                          ),
                        ),
                      ),

                      // Delete button
                      Positioned(
                        top: 8,
                        right: 8,
                        child: AnimatedOpacity(
                          duration: const Duration(milliseconds: 300),
                          opacity: _isHovered ? 1.0 : 0.0,
                          child: Material(
                            color: Colors.black.withValues(alpha: 0.7),
                            borderRadius: BorderRadius.circular(8),
                            child: InkWell(
                              borderRadius: BorderRadius.circular(8),
                              onTap: () {
                                // Handle delete
                              },
                              child: Padding(
                                padding: const EdgeInsets.all(8.0),
                                child: Icon(
                                  Icons.close,
                                  color: Colors.white,
                                  size: 16,
                                ),
                              ),
                            ),
                          ),
                        ),
                      ),

                      // Episode number
                      Positioned(
                        top: 8,
                        left: 8,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.7),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            'EP ${widget.episodeNumber}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ),

                      // Progress bar
                      Positioned(
                        bottom: 0,
                        left: 0,
                        right: 0,
                        child: SizedBox(
                          height: 4,
                          child: ClipRRect(
                            borderRadius: const BorderRadius.only(
                              bottomLeft: Radius.circular(8),
                              bottomRight: Radius.circular(8),
                            ),
                            child: LinearProgressIndicator(
                              value: _calculateProgress(),
                              backgroundColor: Colors.grey[600],
                              valueColor: const AlwaysStoppedAnimation<Color>(
                                  Colors.red),
                            ),
                          ),
                        ),
                      ),

                      // Duration
                      Positioned(
                        bottom: 8,
                        right: 8,
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 4,
                          ),
                          decoration: BoxDecoration(
                            color: Colors.black.withValues(alpha: 0.7),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            '${widget.currentTime}/${widget.totalDuration}',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                            ),
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 8),
              Text(
                widget.title,
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }

  double _calculateProgress() {
    try {
      List<String> currentParts = widget.currentTime.split(':');
      List<String> totalParts = widget.totalDuration.split(':');

      double currentSeconds =
          double.parse(currentParts[0]) * 60 + double.parse(currentParts[1]);
      double totalSeconds =
          double.parse(totalParts[0]) * 60 + double.parse(totalParts[1]);

      return currentSeconds / totalSeconds;
    } catch (e) {
      // print('Error calculating progress: $e');
      return 0.0;
    }
  }
}

class AnimeCard extends StatefulWidget {
  final AnimeItem item;

  const AnimeCard({super.key, required this.item});

  @override
  State<AnimeCard> createState() => _AnimeCardState();
}

class _AnimeCardState extends State<AnimeCard> {
  bool _isHovered = false;

  @override
  Widget build(BuildContext context) {
    return MouseRegion(
      onEnter: (_) => setState(() => _isHovered = true),
      onExit: (_) => setState(() => _isHovered = false),
      child: AnimatedContainer(
        duration: const Duration(milliseconds: 300),
        curve: Curves.easeInOut,
        transform: Matrix4.diagonal3Values(
          _isHovered ? 1.05 : 1.0,
          _isHovered ? 1.05 : 1.0,
          1.0,
        ),
        child: AspectRatio(
          aspectRatio: 3 / 4,
          child: Container(
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withValues(alpha: 0.2),
                  blurRadius: 8,
                  offset: const Offset(0, 4),
                ),
              ],
            ),
            child: ClipRRect(
              borderRadius: BorderRadius.circular(8),
              child: Stack(
                fit: StackFit.expand,
                children: [
                  Image.network(
                    widget.item.imageUrl,
                    fit: BoxFit.cover,
                  ),
                  Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.transparent,
                          Colors.black.withValues(alpha: 0.5),
                          Colors.black.withValues(alpha: 0.8),
                        ],
                        stops: const [0.0, 0.5, 1.0],
                      ),
                    ),
                  ),
                  Positioned(
                    left: 8,
                    top: 8,
                    child: Wrap(
                      spacing: 4,
                      runSpacing: 4,
                      children: [
                        _buildTag(widget.item.format),
                        _buildTag(
                          '${widget.item.epCount}',
                          icon: _buildSvgIcon(_episodesSvg,
                              color: Colors.yellow[400]!),
                        ),
                        _buildTag(
                          '${widget.item.dubbed}',
                          icon: _buildSvgIcon(_audioSvg,
                              color: Colors.blue[400]!),
                        ),
                      ],
                    ),
                  ),
                  Positioned(
                    left: 8,
                    right: 8,
                    bottom: 8,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          widget.item.displayTitle,
                          style: const TextStyle(
                            color: Colors.white,
                            fontSize: 14,
                            fontWeight: FontWeight.w500,
                          ),
                          maxLines: 2,
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 4),
                        Text(
                          'Episodes ${widget.item.epCount}',
                          style: TextStyle(
                            color: Colors.grey[300],
                            fontSize: 12,
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
    );
  }

  Widget _buildTag(String text, {Widget? icon}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(16),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            icon,
            const SizedBox(width: 4),
          ],
          Text(
            text,
            style: const TextStyle(
              color: Colors.white,
              fontSize: 10,
              fontWeight: FontWeight.w500,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildSvgIcon(String svgString, {required Color color}) {
    return SvgPicture.string(
      svgString,
      width: 12,
      height: 12,
      colorFilter: ColorFilter.mode(color, BlendMode.srcIn),
    );
  }
}

// SVG strings remain unchanged
const String _episodesSvg = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect>
  <line x1="7" y1="2" x2="7" y2="22"></line>
  <line x1="17" y1="2" x2="17" y2="22"></line>
  <line x1="2" y1="12" x2="22" y2="12"></line>
  <line x1="2" y1="7" x2="7" y2="7"></line>
  <line x1="2" y1="17" x2="7" y2="17"></line>
  <line x1="17" y1="17" x2="22" y2="17"></line>
  <line x1="17" y1="7" x2="22" y2="7"></line>
</svg>
''';

const String _audioSvg = '''
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
  <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"></path>
  <path d="M19 10v2a7 7 0 0 1-14 0v-2"></path>
  <line x1="12" y1="19" x2="12" y2="23"></line>
  <line x1="8" y1="23" x2="16" y2="23"></line>
</svg>
''';
