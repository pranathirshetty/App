import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'package:kuudere/anime_info.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'dart:convert';
import 'package:loading_animation_widget/loading_animation_widget.dart';

import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/theme/app_theme.dart';

class WatchListTab extends StatefulWidget {
  const WatchListTab({super.key});

  @override
  State<WatchListTab> createState() => _WatchListTabState();
}

class _WatchListTabState extends State<WatchListTab>
    with TickerProviderStateMixin {
  final authService = AuthService();
  String selectedFilter = 'All';
  bool isPublic = true;
  final List<String> filters = [
    'All',
    'Watching',
    'On Hold',
    'Plan To Watch',
    'Dropped',
    'Completed'
  ];

  List<AnimeItem> watchList = [];
  int currentPage = 1;
  int totalPages = 1;
  bool isLoading = false;

  final ScrollController _scrollController = ScrollController();
  final RealtimeService _realtimeService = RealtimeService();

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom("profile");
    fetchWatchList();
    _scrollController.addListener(_onScroll);
  }

  @override
  void dispose() {
    _scrollController.dispose();
    super.dispose();
  }

  void _onScroll() {
    if (_scrollController.position.pixels ==
        _scrollController.position.maxScrollExtent) {
      if (currentPage < totalPages && !isLoading) {
        fetchWatchList(page: currentPage + 1);
      }
    }
  }

  Future<void> fetchWatchList({int page = 1}) async {
    if (isLoading) return;

    setState(() {
      isLoading = true;
    });

    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();

      if (sessionInfo == null) {
        setState(() {
          isLoading = false;
        });
        return;
      }

      final queryParams = <String, String>{
        'page': page.toString(),
        'perPage': '18', // Match backend default
      };
      if (selectedFilter != 'All') {
        queryParams['folder'] =
            selectedFilter; // Backend expects 'folder' not 'status'
      }

      final response = await httpService.get(
        '/api/anime/watchlist', // Use correct endpoint that matches SvelteKit backend
        queryParams: queryParams,
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final items = data['data'] ?? data['watchlist'] ?? [];
        setState(() {
          if (page == 1) {
            watchList = (items as List)
                .map((item) => AnimeItem.fromJson(item))
                .toList();
          } else {
            watchList.addAll((items as List)
                .map((item) => AnimeItem.fromJson(item))
                .toList());
          }
          currentPage = data['current_page'] ?? data['currentPage'] ?? page;
          totalPages = data['total_pages'] ?? data['totalPages'] ?? 1;
          isLoading = false;
        });
      } else {
        throw Exception('Failed to load watch list');
      }
    } catch (e) {
      setState(() {
        isLoading = false;
      });
      // print('Error fetching watch list: $e');
    }
  }

  Widget _buildHeader() {
    return Padding(
      padding: const EdgeInsets.all(16.0),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Row(
            children: [
              Icon(Icons.favorite, color: AppTheme.primary, size: 28),
              SizedBox(width: 8),
              Text(
                'Watch List',
                style: TextStyle(
                  color: Colors.white,
                  fontSize: 24,
                  fontWeight: FontWeight.bold,
                ),
              ),
            ],
          ),
          Row(
            children: [
              Text(
                'Public',
                style: TextStyle(color: Colors.white70),
              ),
              SizedBox(width: 8),
              Switch(
                value: isPublic,
                onChanged: (value) => setState(() => isPublic = value),
                activeThumbColor: AppTheme.primary,
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFilterTabs() {
    return SizedBox(
      height: 40,
      child: ListView.builder(
        scrollDirection: Axis.horizontal,
        padding: const EdgeInsets.symmetric(horizontal: 16),
        itemCount: filters.length,
        itemBuilder: (context, index) {
          final filter = filters[index];
          final isSelected = selectedFilter == filter;
          return Padding(
            padding: const EdgeInsets.only(right: 8),
            child: FilterChip(
              selected: isSelected,
              label: Text(filter),
              onSelected: (selected) {
                setState(() {
                  selectedFilter = filter;
                  currentPage = 1;
                  watchList.clear();
                });
                fetchWatchList(page: 1);
              },
              backgroundColor: Colors.grey[900],
              selectedColor: AppTheme.primary,
              labelStyle: TextStyle(
                color: isSelected ? Colors.white : Colors.grey,
              ),
            ),
          );
        },
      ),
    );
  }

  Widget _buildAnimeGrid() {
    if (watchList.isEmpty && !isLoading) {
      return Expanded(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.sentiment_dissatisfied, size: 64, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'Nothing here',
                style: TextStyle(
                    fontSize: 24,
                    color: Colors.white,
                    fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text(
                'Your watchlist is empty',
                style: TextStyle(fontSize: 16, color: Colors.grey),
              ),
            ],
          ),
        ),
      );
    }

    return Expanded(
      child: LayoutBuilder(
        builder: (context, constraints) {
          final double itemWidth = 180;
          final int crossAxisCount = (constraints.maxWidth / itemWidth).floor();

          return GridView.builder(
            controller: _scrollController,
            padding: const EdgeInsets.all(16),
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: crossAxisCount > 0 ? crossAxisCount : 1,
              childAspectRatio: 0.7,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
            ),
            itemCount: watchList.length + (isLoading ? 1 : 0),
            itemBuilder: (context, index) {
              if (index < watchList.length) {
                return AnimeCard(
                  item: watchList[index],
                  onStatusUpdate: updateAnimeStatus,
                );
              } else {
                return Center(
                  child: LoadingAnimationWidget.threeArchedCircle(
                    color: AppTheme.primary,
                    size: 50,
                  ),
                );
              }
            },
          );
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: 1200),
          child: Column(
            children: [
              _buildHeader(),
              _buildFilterTabs(),
              _buildAnimeGrid(),
            ],
          ),
        ),
      ),
    );
  }

  void updateAnimeStatus(String animeId, String newStatus) {
    setState(() {
      isLoading = true;
    });

    // Make API call to update status
    _updateAnimeStatusAPI(animeId, newStatus).then((_) {
      if (mounted) {
        setState(() {
          if (newStatus == 'Remove') {
            // Remove the anime from the list with animation
            final index = watchList.indexWhere((anime) => anime.id == animeId);
            if (index != -1) {
              _removeAnimeWithAnimation(index);
            }
          } else {
            // Update the status of the anime in the list
            final index = watchList.indexWhere((anime) => anime.id == animeId);
            if (index != -1) {
              watchList[index] = watchList[index].copyWith(status: newStatus);
            }
            // If the current filter doesn't match the new status, remove the item from the list
            if (selectedFilter != 'All' && selectedFilter != newStatus) {
              _removeAnimeWithAnimation(index);
            }
          }
          isLoading = false;
        });
      }
    }).catchError((error) {
      // print('Error updating anime status: $error');
      if (mounted) {
        setState(() {
          isLoading = false;
        });
      }
    });
  }

  Future<void> _updateAnimeStatusAPI(String animeId, String newStatus) async {
    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        // Use the correct endpoint and field names that match SvelteKit backend
        final response = await httpService.post(
          '/api/anime/watchlist',
          body: {
            'animeId': animeId, // Backend expects camelCase
            'folder': selectedFilter, // Backend expects 'folder' not 'status'
          },
          requireAuth: true,
        );

        if (response.statusCode != 200) {
          throw Exception('Failed to update anime status');
        }
      }
    } catch (e) {
      // print('Error updating anime status: $e');
      rethrow;
    }
  }

  void _removeAnimeWithAnimation(int index) {
    AnimationController controller = AnimationController(
      duration: const Duration(milliseconds: 300),
      vsync: this,
    );

    Animation<double> animation = CurvedAnimation(
      parent: controller,
      curve: Curves.easeInOut,
    );

    animation.addStatusListener((status) {
      if (status == AnimationStatus.completed) {
        setState(() {
          watchList.removeAt(index);
          // Trigger a rebuild of the grid to fill the space
          _rebuildGrid();
        });
        controller.dispose();
      }
    });

    setState(() {
      watchList[index] =
          watchList[index].copyWith(animationController: controller);
    });

    controller.forward();
  }

  void _rebuildGrid() {
    // This empty setState will trigger a rebuild of the grid
    setState(() {});
  }
}

class AnimeCard extends StatelessWidget {
  final AnimeItem item;
  final Function(String, String) onStatusUpdate;

  const AnimeCard({
    super.key,
    required this.item,
    required this.onStatusUpdate,
  });

  @override
  Widget build(BuildContext context) {
    return item.animationController != null
        ? SizeTransition(
            sizeFactor: CurvedAnimation(
              parent: item.animationController!,
              curve: Curves.easeInOut,
            ),
            child: FadeTransition(
              opacity: CurvedAnimation(
                parent: item.animationController!,
                curve: Curves.easeInOut,
              ),
              child: _buildAnimeCard(context),
            ),
          )
        : _buildAnimeCard(context);
  }

  Widget _buildAnimeCard(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => AnimeInfoScreen(animeId: item.id),
          ),
        );
      },
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
                item.image,
                fit: BoxFit.cover,
              ),
              Container(
                decoration: BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topCenter,
                    end: Alignment.bottomCenter,
                    colors: [
                      Colors.transparent,
                      Colors.black.withValues(alpha: 0.7),
                    ],
                    stops: [0.6, 1.0],
                  ),
                ),
              ),
              Positioned(
                left: 8,
                top: 8,
                right: 8,
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Wrap(
                        spacing: 4,
                        runSpacing: 4,
                        children: [
                          _buildTag(item.type),
                          _buildTag(
                            '${item.subbed}',
                            icon: _buildSvgIcon(_episodesSvg,
                                color: Colors.yellow),
                          ),
                          _buildTag(
                            '${item.dubbed}',
                            icon: _buildSvgIcon(_audioSvg, color: Colors.blue),
                          ),
                        ],
                      ),
                    ),
                    SizedBox(width: 4),
                    _buildMoreOptionsButton(context),
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
                      item.title,
                      style: TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    SizedBox(height: 4),
                    Text(
                      '${item.status} • ${item.duration}',
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
    );
  }

  Widget _buildMoreOptionsButton(BuildContext context) {
    return GestureDetector(
      onTap: () {
        _showMoreOptionsMenu(context);
      },
      child: Container(
        padding: EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: Colors.black.withValues(alpha: 0.5),
          shape: BoxShape.circle,
        ),
        child: Icon(
          Icons.more_vert,
          color: Colors.white,
          size: 20,
        ),
      ),
    );
  }

  void _showMoreOptionsMenu(BuildContext context) {
    final RenderBox button = context.findRenderObject() as RenderBox;
    final RenderBox overlay =
        Overlay.of(context).context.findRenderObject() as RenderBox;
    final RelativeRect position = RelativeRect.fromRect(
      Rect.fromPoints(
        button.localToGlobal(Offset.zero, ancestor: overlay),
        button.localToGlobal(button.size.bottomRight(Offset.zero),
            ancestor: overlay),
      ),
      Offset.zero & overlay.size,
    );

    showMenu<String>(
      context: context,
      position: position,
      items: [
        _buildPopupMenuItem('Watching', item.status == 'Watching'),
        _buildPopupMenuItem('On Hold', item.status == 'On Hold'),
        _buildPopupMenuItem('Plan To Watch', item.status == 'Plan To Watch'),
        _buildPopupMenuItem('Dropped', item.status == 'Dropped'),
        _buildPopupMenuItem('Completed', item.status == 'Completed'),
        _buildPopupMenuItem('Remove', false),
      ],
    ).then((String? selectedValue) {
      if (selectedValue != null) {
        onStatusUpdate(item.id, selectedValue);
      }
    });
  }

  PopupMenuItem<String> _buildPopupMenuItem(String value, bool isSelected) {
    return PopupMenuItem<String>(
      value: value,
      child: Row(
        children: [
          Text(value),
          if (isSelected) ...[
            SizedBox(width: 8),
            Icon(Icons.check, color: Colors.green, size: 18),
          ],
        ],
      ),
    );
  }

  Widget _buildTag(String text, {Widget? icon}) {
    return Container(
      padding: EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          if (icon != null) ...[
            icon,
            SizedBox(width: 4),
          ],
          Text(
            text,
            style: TextStyle(
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

class AnimeItem {
  final String id;
  final String title;
  final int total;
  final int dubbed;
  final int subbed;
  final String image;
  final String type;
  final String status;
  final String duration;
  final String url;
  final AnimationController? animationController;

  AnimeItem({
    required this.id,
    required this.title,
    required this.total,
    required this.dubbed,
    required this.subbed,
    required this.image,
    required this.type,
    required this.status,
    required this.duration,
    required this.url,
    this.animationController,
  });

  factory AnimeItem.fromJson(Map<String, dynamic> json) {
    // Handle null values from backend - use folder as status, map backend fields to model
    return AnimeItem(
      id: json['id'] ?? json['mainId'] ?? '',
      title: json['english'] ?? json['romaji'] ?? json['title'] ?? 'Unknown',
      total: json['epCount'] ?? json['subbed'] ?? json['total'] ?? 0,
      dubbed: json['dubbedCount'] ?? json['dubbed'] ?? 0,
      subbed: json['subbedCount'] ?? json['subbed'] ?? 0,
      image: json['cover'] ?? json['image'] ?? '',
      type: json['type'] ?? 'Unknown',
      status: json['folder'] ??
          json['status'] ??
          'Plan To Watch', // Use folder from backend
      duration: json['duration']?.toString() ?? '0',
      url: '/watch/${json['id'] ?? json['mainId'] ?? ''}',
    );
  }

  AnimeItem copyWith({
    String? status,
    AnimationController? animationController,
  }) {
    return AnimeItem(
      id: id,
      title: title,
      total: total,
      dubbed: dubbed,
      subbed: subbed,
      image: image,
      type: type,
      status: status ?? this.status,
      duration: duration,
      url: url,
      animationController: animationController ?? this.animationController,
    );
  }
}

// SVG strings for icons
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
