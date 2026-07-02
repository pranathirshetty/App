import 'package:flutter/material.dart';
import 'dart:convert';
import 'package:flutter_svg/flutter_svg.dart';
import 'package:kuudere/anime_info.dart';

import 'package:kuudere/services/realtime_service.dart';
import 'package:kuudere/services/http_service.dart';
import 'dart:ui';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:kuudere/models/anime_models.dart';

class SearchTab extends StatefulWidget {
  final String? initialGenre;
  final String? initialStatus;
  final String? initialSort;

  const SearchTab({
    super.key,
    this.initialGenre,
    this.initialStatus,
    this.initialSort,
  });

  @override
  State<SearchTab> createState() => _SearchTabState();
}

class _SearchTabState extends State<SearchTab> {
  final TextEditingController _searchController = TextEditingController();
  List<AnimeItem> _searchResults = [];
  bool _isLoading = false;
  bool _isLoadingMore = false;
  int _currentOffset = 0;
  bool _hasMore = true;
  final ScrollController _scrollController = ScrollController();

  final List<String> _selectedGenres = [];
  final List<String> _selectedSeasons = [];
  final List<String> _selectedYears = [];
  final List<String> _selectedTypes = [];
  final List<String> _selectedStatuses = [];
  final List<String> _selectedLanguages = [];
  final List<String> _selectedRatings = [];
  final RealtimeService _realtimeService = RealtimeService();

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom("search");
    if (widget.initialGenre != null) {
      _selectedGenres.add(widget.initialGenre!);
    }
    if (widget.initialStatus != null) {
      _selectedStatuses.add(widget.initialStatus!);
    }
    // Load initial results (all anime or filtered) like SvelteKit does
    _searchAnime();
    _scrollController.addListener(_scrollListener);
  }

  @override
  void dispose() {
    _scrollController.removeListener(_scrollListener);
    _scrollController.dispose();
    super.dispose();
  }

  void _scrollListener() {
    if (_scrollController.position.pixels ==
        _scrollController.position.maxScrollExtent) {
      _searchAnime(loadMore: true);
    }
  }

  Future<void> _searchAnime({bool loadMore = false}) async {
    if (!loadMore) {
      setState(() {
        _isLoading = true;
        _currentOffset = 0;
        _hasMore = true;
      });
    } else {
      if (_isLoadingMore || !_hasMore) return;
      setState(() {
        _isLoadingMore = true;
      });
    }

    try {
      final searchQuery = _searchController.text.trim();
      final httpService = HttpService();
      final queryParams = <String, String>{
        'limit': '20',
        'offset': _currentOffset.toString(),
      };

      if (searchQuery.isNotEmpty) {
        queryParams['q'] = searchQuery;
      }
      if (_selectedGenres.isNotEmpty) {
        queryParams['genre'] = _selectedGenres.join(',');
      }
      if (_selectedSeasons.isNotEmpty) {
        queryParams['season'] = _selectedSeasons.first;
      }
      if (_selectedYears.isNotEmpty) {
        queryParams['year'] = _selectedYears.first;
      }
      if (_selectedTypes.isNotEmpty) {
        queryParams['format'] = _selectedTypes.first;
      }
      if (_selectedStatuses.isNotEmpty) {
        queryParams['status'] = _selectedStatuses.first;
      }
      if (widget.initialSort != null) {
        queryParams['sort'] = widget.initialSort!;
      }

      final response = await httpService.get('/search', queryParams: queryParams);

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final searchResponse = SearchResponse.fromJson(data);
        if (mounted) {
          setState(() {
            if (loadMore) {
              _searchResults.addAll(searchResponse.results);
            } else {
              _searchResults = searchResponse.results;
            }
            _currentOffset = searchResponse.offset + searchResponse.limit;
            _hasMore = searchResponse.hasMore;
            _isLoading = false;
            _isLoadingMore = false;
          });
        }
      } else {
        if (mounted) {
          setState(() {
            if (!loadMore) _searchResults = [];
            _isLoading = false;
            _isLoadingMore = false;
          });
        }
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _isLoading = false;
          _isLoadingMore = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      body: Center(
        child: ConstrainedBox(
          constraints: BoxConstraints(maxWidth: 1200),
          child: CustomScrollView(
            controller: _scrollController,
            slivers: [
              SliverToBoxAdapter(
                child: Padding(
                  padding: const EdgeInsets.all(24.0),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Filter',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 24,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 16),
                      _buildSearchInput(),
                      SizedBox(height: 16),
                      _buildFilterGrid(),
                      SizedBox(height: 24),
                      Text(
                        'Results: ${_searchResults.length}',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 20,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      SizedBox(height: 16),
                    ],
                  ),
                ),
              ),
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(24, 24, 24, 100),
                sliver: _isLoading
                    ? SliverToBoxAdapter(
                        child: Center(
                          child: LoadingAnimationWidget.threeArchedCircle(
                            color: Colors.white,
                            size: 50,
                          ),
                        ),
                      )
                    : SliverGrid(
                        gridDelegate: SliverGridDelegateWithMaxCrossAxisExtent(
                          maxCrossAxisExtent: 250,
                          childAspectRatio: 0.7,
                          crossAxisSpacing: 16,
                          mainAxisSpacing: 16,
                        ),
                        delegate: SliverChildBuilderDelegate(
                          (context, index) {
                            if (index < _searchResults.length) {
                              final anime = _searchResults[index];
                              return _buildAnimeCard(anime);
                            } else if (_isLoadingMore) {
                              return Center(
                                child: LoadingAnimationWidget.threeArchedCircle(
                                  color: Colors.white,
                                  size: 30,
                                ),
                              );
                            } else {
                              return null;
                            }
                          },
                          childCount:
                              _searchResults.length + (_isLoadingMore ? 1 : 0),
                        ),
                      ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildSearchInput() {
    return Container(
      decoration: BoxDecoration(
        color: Color(0xFF1A1B1E),
        borderRadius: BorderRadius.circular(8),
      ),
      child: TextField(
        controller: _searchController,
        style: TextStyle(color: Colors.white),
        decoration: InputDecoration(
          hintText: 'Search...',
          hintStyle: TextStyle(color: Colors.grey[400]),
          border: OutlineInputBorder(
            borderSide: BorderSide.none,
            borderRadius: BorderRadius.circular(8),
          ),
          contentPadding: EdgeInsets.symmetric(horizontal: 16, vertical: 14),
          prefixIcon: Icon(Icons.search, color: Colors.grey[400]),
        ),
        onSubmitted: (_) => _searchAnime(),
      ),
    );
  }

  Widget _buildFilterGrid() {
    final hasFilters = _selectedGenres.isNotEmpty ||
        _selectedSeasons.isNotEmpty ||
        _selectedYears.isNotEmpty ||
        _selectedTypes.isNotEmpty ||
        _selectedStatuses.isNotEmpty ||
        _selectedLanguages.isNotEmpty ||
        _selectedRatings.isNotEmpty;

    return Theme(
      data: Theme.of(context).copyWith(dividerColor: Colors.transparent),
      child: ExpansionTile(
        initiallyExpanded: hasFilters,
        title: Text(
          'Filters',
          style: TextStyle(color: Colors.white, fontSize: 18),
        ),
        collapsedIconColor: Colors.white,
        iconColor: Colors.white,
        backgroundColor: Colors.transparent,
        children: [
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Expanded(
                      child: _buildFilterDropdown(
                          'Select genre',
                          _selectedGenres,
                          [
                            'Action',
                            'Adventure',
                            'Cars',
                            'Comedy',
                            'Dementia',
                            'Demons',
                            'Drama',
                            'Ecchi',
                            'Fantasy',
                            'Game',
                            'Harem',
                            'Historical',
                            'Horror',
                            'Isekai',
                            'Josei',
                            'Kids',
                            'Magic',
                            'Martial Arts',
                            'Mecha',
                            'Military',
                            'Music',
                            'Mystery',
                            'Parody',
                            'Police',
                            'Psychological',
                            'Romance',
                            'Samurai',
                            'School',
                            'Sci-Fi',
                            'Seinen',
                            'Shoujo',
                            'Shoujo Ai',
                            'Shounen',
                            'Shounen Ai',
                            'Slice of Life',
                            'Space',
                            'Sports',
                            'Super Power',
                            'Supernatural',
                            'Thriller',
                            'unknown',
                            'Vampire'
                          ],
                          multiSelect: true)),
                  SizedBox(width: 16),
                  Expanded(
                      child: _buildFilterDropdown(
                          'Select seasons',
                          _selectedSeasons,
                          ['Winter', 'Spring', 'Summer', 'Fall'])),
                ],
              ),
              SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                      child: _buildFilterDropdown(
                          'Select years',
                          _selectedYears,
                          List.generate(
                              45, (index) => (2025 - index).toString()))),
                  SizedBox(width: 16),
                  Expanded(
                      child: _buildFilterDropdown('Select languages',
                          _selectedLanguages, ['Japanese', 'English'])),
                ],
              ),
              SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                      child: _buildFilterDropdown('Select ratings',
                          _selectedRatings, ['G', 'PG', 'PG-13', 'R', 'R+'])),
                  SizedBox(width: 16),
                  Expanded(
                      child: _buildFilterDropdown('Select types',
                          _selectedTypes, ['TV', 'Movie', 'OVA'])),
                ],
              ),
              SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                      child: _buildFilterDropdown(
                          'Select statuses', _selectedStatuses, [
                    'Finished',
                    'Releasing',
                    'Not Yet Released',
                    'Cancelled'
                  ])),
                  Expanded(
                      child:
                          SizedBox()), // Empty space to maintain grid alignment
                ],
              ),
              SizedBox(height: 16),
              Row(
                children: [
                  Expanded(
                    child: ElevatedButton.icon(
                      onPressed: () => _searchAnime(),
                      icon: Icon(Icons.filter_list),
                      label: Text('Apply Filters'),
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Color(0xFFE53935),
                        foregroundColor: Colors.white,
                        padding: EdgeInsets.symmetric(vertical: 12),
                      ),
                    ),
                  ),
                  SizedBox(width: 16),
                  Expanded(
                    child: OutlinedButton.icon(
                      onPressed: () {
                        setState(() {
                          _selectedGenres.clear();
                          _selectedSeasons.clear();
                          _selectedYears.clear();
                          _selectedTypes.clear();
                          _selectedStatuses.clear();
                          _selectedLanguages.clear();
                          _selectedRatings.clear();
                        });
                        _searchAnime();
                      },
                      icon: Icon(Icons.clear_all),
                      label: Text('Clear Filters'),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white,
                        side: BorderSide(color: Colors.grey[700]!),
                        padding: EdgeInsets.symmetric(vertical: 12),
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildFilterDropdown(
      String hint, List<String> selectedItems, List<String> items,
      {bool multiSelect = false}) {
    return Container(
      decoration: BoxDecoration(
        color: Color(0xFF1A1B1E),
        borderRadius: BorderRadius.circular(8),
      ),
      child: DropdownButtonFormField<String>(
        isExpanded: true,
        initialValue: multiSelect
            ? null
            : (selectedItems.isNotEmpty ? selectedItems.first : null),
        hint: Text(
          hint,
          style: TextStyle(color: Colors.grey[400]),
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
        ),
        style: TextStyle(color: Colors.white),
        dropdownColor: Color(0xFF1A1B1E),
        decoration: InputDecoration(
          border: OutlineInputBorder(
            borderSide: BorderSide.none,
            borderRadius: BorderRadius.circular(8),
          ),
          contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 14),
        ),
        icon: Icon(Icons.arrow_drop_down, color: Colors.grey[400]),
        onChanged: (String? newValue) {
          if (newValue != null) {
            setState(() {
              if (multiSelect) {
                if (selectedItems.contains(newValue)) {
                  selectedItems.remove(newValue);
                } else {
                  selectedItems.add(newValue);
                }
              } else {
                selectedItems.clear();
                selectedItems.add(newValue);
              }
            });
          }
        },
        items: items.map<DropdownMenuItem<String>>((String value) {
          return DropdownMenuItem<String>(
            value: value,
            child: Row(
              children: [
                if (multiSelect)
                  Checkbox(
                    value: selectedItems.contains(value),
                    onChanged: (_) {},
                    fillColor: WidgetStateProperty.resolveWith(
                        (states) => Colors.white),
                    checkColor: Color(0xFF1A1B1E),
                  ),
                Expanded(
                  child: Text(
                    value,
                    overflow: TextOverflow.ellipsis,
                    maxLines: 1,
                  ),
                ),
              ],
            ),
          );
        }).toList(),
        selectedItemBuilder: (BuildContext context) {
          return items.map<Widget>((String item) {
            return Text(
              multiSelect
                  ? (selectedItems.isNotEmpty
                      ? '${selectedItems.length} selected'
                      : hint)
                  : (selectedItems.isNotEmpty ? selectedItems.first : hint),
              style: TextStyle(color: Colors.white),
              overflow: TextOverflow.ellipsis,
              maxLines: 1,
            );
          }).toList();
        },
      ),
    );
  }

  Widget _buildAnimeCard(AnimeItem anime) {
    return AnimeCard(item: anime);
  }
}

class GlassContainer extends StatelessWidget {
  final Widget child;

  const GlassContainer({super.key, required this.child});

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.white.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(
          color: Colors.white.withValues(alpha: 0.2),
          width: 1.5,
        ),
      ),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: child,
          ),
        ),
      ),
    );
  }
}

class AnimeCard extends StatelessWidget {
  final AnimeItem item;
  final VoidCallback? onTap;

  const AnimeCard({super.key, required this.item, this.onTap});

  @override
  Widget build(BuildContext context) {
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
              Image.network(item.imageUrl, fit: BoxFit.cover),
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
                child: Wrap(
                  spacing: 4,
                  runSpacing: 4,
                  children: [
                    _buildTag(item.type ?? item.format),
                    _buildTag(
                      '${item.epCount ?? 0}',
                      icon: _buildSvgIcon(_episodesSvg, color: Colors.yellow),
                    ),
                    _buildTag(
                      '${item.dubbed}',
                      icon: _buildSvgIcon(_audioSvg, color: Colors.blue),
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
                      item.displayTitle,
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
                      'Episodes ${item.epCount ?? 0}',
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
