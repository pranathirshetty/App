class AnimeItem {
  final String id;
  final String title;
  final String english;
  final int epCount;
  final int subbedCount;
  final int dubbedCount;
  final String imageUrl;
  final String? bannerUrl;
  final String description;
  final double? malScore;
  final List<String> genres;
  final String type;

  AnimeItem({
    required this.id,
    required this.title,
    required this.english,
    required this.epCount,
    required this.subbedCount,
    required this.dubbedCount,
    required this.imageUrl,
    this.bannerUrl,
    required this.description,
    this.malScore,
    required this.genres,
    required this.type,
  });

  factory AnimeItem.fromJson(Map<String, dynamic> json) {
    return AnimeItem(
      id: json['id'] ?? '',
      title: json['english'] ?? json['romaji'] ?? '',
      english: json['english'] ?? '',
      epCount: json['epCount'] ?? 0,
      subbedCount: json['subbedCount'] ?? 0,
      dubbedCount: json['dubbedCount'] ?? 0,
      imageUrl: json['cover'] ?? '',
      bannerUrl: json['carouselBanner'] ??
          json['banner'], // Prioritize carouselBanner from carousel collection
      description: json['description'] ?? '',
      malScore: json['malScore']?.toDouble(),
      genres: List<String>.from(json['genres'] ?? []),
      type: json['type'] ?? '',
    );
  }
}

class ContinueWatchingItem {
  final String duration;
  final int episode;
  final String link;
  final String progress;
  final String thumbnail;
  final String title;

  ContinueWatchingItem({
    required this.duration,
    required this.episode,
    required this.link,
    required this.progress,
    required this.thumbnail,
    required this.title,
  });

  factory ContinueWatchingItem.fromJson(Map<String, dynamic> json) {
    return ContinueWatchingItem(
      duration: json['duration'] ?? '',
      episode: json['episode'] ?? 0,
      link: json['link'] ?? '',
      progress: json['progress'] ?? '',
      thumbnail: json['thumbnail'] ?? '',
      title: json['title'] ?? '',
    );
  }
}
