import 'package:flutter/material.dart';
import 'package:flutter_svg/flutter_svg.dart';

import 'package:kuudere/anime_info.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'dart:convert';
import 'package:loading_animation_widget/loading_animation_widget.dart';

import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/theme/app_theme.dart';
import 'package:kuudere/models/anime_models.dart';

class WatchListTab extends StatefulWidget {
  const WatchListTab({super.key});

  @override
  State<WatchListTab> createState() => _WatchListTabState();
}

class _WatchListTabState extends State<WatchListTab> {
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

  List<WatchlistEntry> watchList = [];
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
        'limit': '18',
        'offset': ((page - 1) * 18).toString(),
      };
      if (selectedFilter != 'All') {
        queryParams['folder'] = selectedFilter.toUpperCase().replaceAll(' ', '_');
      }

      final response = await httpService.get(
        '/v1/watchlist',
        queryParams: queryParams,
        requireAuth: true,
        useBff: true,
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final resp = WatchlistResponse.fromJson(data);
        setState(() {
          if (page == 1) {
            watchList = resp.data;
          } else {
            watchList.addAll(resp.data);
          }
          totalPages = (resp.total / 18).ceil();
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
            padding: const EdgeInsets.fromLTRB(16, 16, 16, 100),
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
    setState(() => isLoading = true);

    _updateAnimeStatusAPI(animeId, newStatus).then((_) {
      if (mounted) {
        setState(() {
          if (newStatus == 'Remove') {
            final index = watchList.indexWhere((a) => a.id == animeId);
            if (index != -1) watchList.removeAt(index);
          } else {
            final folder = newStatus.toUpperCase().replaceAll(' ', '_');
            final index = watchList.indexWhere((a) => a.id == animeId);
            if (index != -1) {
              final entry = watchList[index];
              watchList[index] = WatchlistEntry(
                anime: entry.anime,
                folder: folder == 'PLAN_TO_WATCH' ? 'PLANNING' : folder,
              );
            }
            if (selectedFilter != 'All') {
              final filterFolder = selectedFilter.toUpperCase().replaceAll(' ', '_');
              if (filterFolder != folder) {
                watchList.removeAt(index);
              }
            }
          }
          isLoading = false;
        });
      }
    }).catchError((_) {
      if (mounted) setState(() => isLoading = false);
    });
  }

  Future<void> _updateAnimeStatusAPI(String animeId, String newStatus) async {
    try {
      final httpService = HttpService();
      final folder = newStatus == 'Remove'
          ? 'REMOVE'
          : newStatus.toUpperCase().replaceAll(' ', '_');
      final response = await httpService.post(
        '/v1/watchlist',
        body: {'animeId': animeId, 'folder': folder == 'PLAN_TO_WATCH' ? 'PLANNING' : folder},
        requireAuth: true,
        useBff: true,
      );
      if (response.statusCode != 200 && response.statusCode != 201) {
        throw Exception('Failed to update anime status');
      }
    } catch (e) {
      rethrow;
    }
  }

}

class AnimeCard extends StatelessWidget {
  final WatchlistEntry item;
  final Function(String, String) onStatusUpdate;

  const AnimeCard({
    super.key,
    required this.item,
    required this.onStatusUpdate,
  });

  @override
  Widget build(BuildContext context) {
    return _buildAnimeCard(context);
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
              Image.network(item.image, fit: BoxFit.cover),
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
                            icon: _buildSvgIcon(_episodesSvg, color: Colors.yellow),
                          ),
                          _buildTag(
                            '${item.dubbed}',
                            icon: _buildSvgIcon(_audioSvg, color: Colors.blue),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 4),
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
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 14,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 4),
                    Text(
                      '${item.status} • ${item.duration}',
                      style: TextStyle(color: Colors.grey[300], fontSize: 12),
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
      onTap: () => _showMoreOptionsMenu(context),
      child: Container(
        padding: const EdgeInsets.all(4),
        decoration: BoxDecoration(
          color: Colors.black.withValues(alpha: 0.5),
          shape: BoxShape.circle,
        ),
        child: const Icon(Icons.more_vert, color: Colors.white, size: 20),
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
        button.localToGlobal(button.size.bottomRight(Offset.zero), ancestor: overlay),
      ),
      Offset.zero & overlay.size,
    );

    showMenu<String>(
      context: context,
      position: position,
      items: [
        _buildPopupMenuItem('Watching', item.status == 'WATCHING'),
        _buildPopupMenuItem('On Hold', item.status == 'PAUSED'),
        _buildPopupMenuItem('Plan To Watch', item.status == 'PLANNING'),
        _buildPopupMenuItem('Dropped', item.status == 'DROPPED'),
        _buildPopupMenuItem('Completed', item.status == 'COMPLETED'),
        const PopupMenuItem<String>(value: 'Remove', child: Text('Remove')),
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
            const SizedBox(width: 8),
            const Icon(Icons.check, color: Colors.green, size: 18),
          ],
        ],
      ),
    );
  }

  Widget _buildTag(String text, {Widget? icon}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: Colors.black.withValues(alpha: 0.5),
        borderRadius: BorderRadius.circular(12),
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
