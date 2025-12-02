import 'package:flutter/material.dart';

import 'dart:convert';
import 'package:intl/intl.dart';
import 'package:flutter_timezone/flutter_timezone.dart';
import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'anime_info.dart';
import 'dart:ui';

class ScheduleTab extends StatefulWidget {
  const ScheduleTab({super.key});

  @override
  State<ScheduleTab> createState() => _ScheduleTabState();
}

class _ScheduleTabState extends State<ScheduleTab> {
  late Future<Map<String, List<AnimeSchedule>>> _scheduleData;
  final RealtimeService _realtimeService = RealtimeService();
  final authService = AuthService();

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

    // Schedule can be viewed without auth, but user preferences might require it
    try {
      final response = await httpService.get(
        '/api/schedule',
        queryParams: {'timezone': currentTimeZone},
        requireAuth: sessionInfo != null,
      );

      if (response.statusCode == 200) {
        final Map<String, dynamic> jsonData = json.decode(response.body);
        final Map<String, List<AnimeSchedule>> scheduleData = {};

        // Handle both direct object and wrapped response
        final data = jsonData['data'] ?? jsonData;
        if (data is Map) {
          data.forEach((date, animeList) {
            scheduleData[date] = (animeList as List)
                .map((anime) => AnimeSchedule.fromJson(anime))
                .toList();
          });
        }

        return scheduleData;
      } else {
        throw Exception(
            'Failed to load schedule data (Status: ${response.statusCode})');
      }
    } catch (e) {
      throw Exception('Error fetching data: $e');
    }
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
                ...animeList.map((anime) =>
                    AnimeScheduleCard(anime: anime, isToday: isToday)),
              ],
            );
          },
        );
      },
    );
  }
}

class AnimeScheduleCard extends StatelessWidget {
  final AnimeSchedule anime;
  final bool isToday;

  const AnimeScheduleCard({
    super.key,
    required this.anime,
    required this.isToday,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: const Color(0xFF121212),
        borderRadius: BorderRadius.circular(16),
        border: isToday
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
                builder: (context) => AnimeInfoScreen(animeId: anime.id),
              ),
            );
          },
          child: ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: Stack(
              children: [
                // Background Banner (faded)
                if (anime.banner != null)
                  Positioned.fill(
                    child: Opacity(
                      opacity: 0.15,
                      child: ImageFiltered(
                        imageFilter: ImageFilter.blur(sigmaX: 3.0, sigmaY: 3.0),
                        child: Image.network(
                          anime.banner!,
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
                        tag: 'schedule-${anime.id}',
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(8),
                          child: anime.cover != null
                              ? Image.network(
                                  anime.cover!,
                                  width: 85,
                                  height: 125,
                                  fit: BoxFit.cover,
                                  errorBuilder: (context, error, stackTrace) =>
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
                              anime.title,
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
                                    color:
                                        Colors.redAccent.withValues(alpha: 0.2),
                                    borderRadius: BorderRadius.circular(6),
                                    border: Border.all(
                                        color: Colors.redAccent
                                            .withValues(alpha: 0.3)),
                                  ),
                                  child: Row(
                                    mainAxisSize: MainAxisSize.min,
                                    children: [
                                      const Icon(Icons.access_time_rounded,
                                          size: 12, color: Colors.redAccent),
                                      const SizedBox(width: 4),
                                      Text(
                                        anime.time,
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
                                    color: Colors.white.withValues(alpha: 0.1),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Text(
                                    'EP ${anime.episode}',
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
                            if (anime.genres != null &&
                                anime.genres!.isNotEmpty)
                              Wrap(
                                spacing: 6,
                                runSpacing: 6,
                                children: anime.genres!.take(3).map((genre) {
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
    );
  }
}
