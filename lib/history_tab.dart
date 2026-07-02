import 'package:flutter/material.dart';

import 'package:kuudere/services/realtime_service.dart';
import 'package:kuudere/watch_anime.dart';
import 'dart:convert';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:kuudere/services/auth_service.dart';

import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/models/anime_models.dart';

class HistoryTab extends StatefulWidget {
  const HistoryTab({super.key});

  @override
  State<HistoryTab> createState() => _HistoryTabState();
}

class _HistoryTabState extends State<HistoryTab> {
  final authService = AuthService();
  List<ContinueWatchingItem> historyList = [];
  int currentPage = 1;
  int totalPages = 1;
  bool isLoading = false;
  final ScrollController _scrollController = ScrollController();
  final RealtimeService _realtimeService = RealtimeService();

  @override
  void initState() {
    _realtimeService.joinRoom("profile");
    super.initState();
    fetchHistory();
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
        fetchHistory(page: currentPage + 1);
      }
    }
  }

  Future<void> fetchHistory({int page = 1}) async {
    if (isLoading) return;

    setState(() {
      isLoading = true;
    });

    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo?.anisurgeToken != null) {
        final response = await httpService.get(
          '/v1/watch/continue',
          queryParams: {
            'limit': '18',
            'offset': ((page - 1) * 18).toString(),
          },
          requireAuth: true,
          useBff: true,
        );

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          final resp = ContinueWatchingResponse.fromJson(data);
          setState(() {
            if (page == 1) {
              historyList = resp.data;
            } else {
              historyList.addAll(resp.data);
            }
            currentPage = page;
            totalPages = (resp.total / 18).ceil();
            isLoading = false;
          });
        } else {
          throw Exception('Failed to load history');
        }
      }
    } catch (e) {
      setState(() {
        isLoading = false;
      });
      // print('Error fetching history: $e');
    }
  }

  Widget _buildHistoryList() {
    if (historyList.isEmpty && !isLoading) {
      return Expanded(
        child: Center(
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.movie_filter, size: 64, color: Colors.grey),
              SizedBox(height: 16),
              Text(
                'No history yet',
                style: TextStyle(
                    fontSize: 24,
                    color: Colors.white,
                    fontWeight: FontWeight.bold),
              ),
              SizedBox(height: 8),
              Text(
                'Start watching to see your history',
                style: TextStyle(fontSize: 16, color: Colors.grey),
              ),
            ],
          ),
        ),
      );
    }

    return Expanded(
      child: ListView.builder(
        controller: _scrollController,
        itemCount: historyList.length + (isLoading ? 1 : 0),
        itemBuilder: (context, index) {
          if (index < historyList.length) {
            return HistoryCard(item: historyList[index]);
          } else {
            return Center(
              child: LoadingAnimationWidget.threeArchedCircle(
                color: Colors.red,
                size: 50,
              ),
            );
          }
        },
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        leading: IconButton(
          icon: Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: Text(
          'Continue Watching',
          style: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
        actions: [
          IconButton(
            icon: Icon(Icons.search, color: Colors.white),
            onPressed: () {
              // Implement search functionality
            },
          ),
        ],
      ),
      body: SafeArea(
        child: Column(
          children: [
            _buildHistoryList(),
          ],
        ),
      ),
    );
  }
}

class HistoryCard extends StatelessWidget {
  final ContinueWatchingItem item;

  const HistoryCard({super.key, required this.item});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () {
        Navigator.push(
          context,
          MaterialPageRoute(
            builder: (context) => WatchAnimeScreen(
              id: item.animeId,
              episodeNumber: item.displayEpisode,
            ),
          ),
        );
      },
      child: Container(
        margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        decoration: BoxDecoration(
          color: Colors.grey[900],
          borderRadius: BorderRadius.circular(12),
        ),
        child: Row(
          children: [
            ClipRRect(
              borderRadius: const BorderRadius.only(
                topLeft: Radius.circular(12),
                bottomLeft: Radius.circular(12),
              ),
              child: Image.network(
                item.imageUrl,
                width: 100,
                height: 150,
                fit: BoxFit.cover,
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      item.displayTitle,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Episode ${item.displayEpisode}',
                      style: TextStyle(color: Colors.grey[400], fontSize: 14),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Progress: ${_formatDuration(item.progress)} / ${_formatDuration(item.duration)}',
                      style: TextStyle(color: Colors.grey[400], fontSize: 14),
                    ),
                    const SizedBox(height: 8),
                    LinearProgressIndicator(
                      value: item.duration > 0 ? item.progress / item.duration : 0,
                      backgroundColor: Colors.grey[700],
                      valueColor: const AlwaysStoppedAnimation<Color>(Colors.red),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _formatDuration(double seconds) {
    final d = Duration(milliseconds: (seconds * 1000).round());
    final hours = d.inHours;
    final minutes = d.inMinutes.remainder(60);
    final secs = d.inSeconds.remainder(60);
    if (hours > 0) {
      return '${hours}h ${minutes}m';
    }
    return '${minutes}m ${secs}s';
  }
}
