/// AnimeItem — maps Project-R /api/v1 response
class AnimeItem {
  final String id;
  final String animeId;
  final int? anilistId;
  final int? malId;
  final String title;
  final String english;
  final String romaji;
  final String nativeTitle;
  final String cover;
  final String image;
  final String? banner;
  final int? episodes;
  final int? episodesTotal;
  final int subbed;
  final int dubbed;
  final double? malScore;
  final double? averageScore;
  final List<String> genres;
  final String status;
  final String format;
  final String? season;
  final int? year;
  final int? duration;
  final String description;
  final String? folder;
  final bool canWatch;
  final int? popularity;
  final Map<String, dynamic>? episode;

  AnimeItem({
    required this.id,
    required this.animeId,
    this.anilistId,
    this.malId,
    required this.title,
    required this.english,
    required this.romaji,
    this.nativeTitle = '',
    required this.cover,
    required this.image,
    this.banner,
    this.episodes,
    this.episodesTotal,
    this.subbed = 0,
    this.dubbed = 0,
    this.malScore,
    this.averageScore,
    this.genres = const [],
    this.status = '',
    this.format = '',
    this.season,
    this.year,
    this.duration,
    this.description = '',
    this.folder,
    this.canWatch = false,
    this.popularity,
    this.episode,
  });

  int get epCount => episodes ?? episodesTotal ?? 0;
  String get slug => animeId.isNotEmpty ? animeId : id;
  String get imageUrl => cover.isNotEmpty ? cover : image;

  factory AnimeItem.fromProjectRJson(Map<String, dynamic> json) {
    String english = '';
    String romaji = '';
    String nativeTitle = '';
    String displayTitle = '';
    final title = json['title'];
    if (title is Map<String, dynamic>) {
      english = title['english'] ?? '';
      romaji = title['romaji'] ?? '';
      nativeTitle = title['native'] ?? '';
      displayTitle = english.isNotEmpty ? english : romaji.isNotEmpty ? romaji : nativeTitle;
    } else if (title is String) {
      displayTitle = title;
    }

    String cover = '';
    String image = '';
    final coverImage = json['cover_image'];
    if (coverImage is Map<String, dynamic>) {
      cover = coverImage['extra_large'] ?? coverImage['large'] ?? coverImage['medium'] ?? '';
      image = cover;
    } else {
      cover = json['cover'] ?? json['image'] ?? '';
      image = cover;
    }

    String? banner;
    if (json['banner_image'] is String && (json['banner_image'] as String).isNotEmpty) {
      banner = json['banner_image'];
    } else {
      banner = json['banner'];
    }

    final animeId = json['anime_id'] ?? json['id'] ?? json['slug'] ?? '';

    return AnimeItem(
      id: animeId,
      animeId: animeId,
      anilistId: json['anilist_id'] ?? json['anilistId'],
      malId: json['mal_id'] ?? json['malId'],
      title: displayTitle,
      english: english,
      romaji: romaji,
      nativeTitle: nativeTitle,
      cover: cover,
      image: image,
      banner: banner,
      episodes: json['episodes'] ?? json['episodes_total'],
      episodesTotal: json['episodes_total'],
      subbed: json['subbed'] ?? 0,
      dubbed: json['dubbed'] ?? 0,
      malScore: (json['mal_score'] ?? json['malScore'])?.toDouble(),
      averageScore: (json['average_score'] ?? json['averageScore'])?.toDouble(),
      genres: List<String>.from(json['genres'] ?? []),
      status: json['status'] ?? '',
      format: json['format'] ?? '',
      season: json['season'],
      year: json['season_year'] ?? json['year'],
      duration: json['duration'],
      description: json['description'] ?? '',
      folder: json['folder'],
      canWatch: json['can_watch'] ?? json['canWatch'] ?? false,
      popularity: json['popularity'],
      episode: json['episode'] is Map<String, dynamic>
          ? Map<String, dynamic>.from(json['episode'])
          : null,
    );
  }

  factory AnimeItem.fromJson(Map<String, dynamic> json) =>
      AnimeItem.fromProjectRJson(json);
}

/// ContinueWatchingItem — BFF GET /v1/watch/continue
class ContinueWatchingItem {
  final String animeId;
  final String continueId;
  final String episodeId;
  final double currentTime;
  final double duration;
  final String language;
  final String server;
  final String? lastUpdated;
  final String slug;
  final String title;
  final String cover;

  ContinueWatchingItem({
    required this.animeId,
    required this.continueId,
    required this.episodeId,
    this.currentTime = 0,
    this.duration = 0,
    this.language = 'sub',
    this.server = 'suzu',
    this.lastUpdated,
    this.slug = '',
    this.title = '',
    this.cover = '',
  });

  int get episode {
    // Episode ID format: ep-4 => 4
    final match = RegExp(r'ep[=-]?(\d+)').firstMatch(episodeId);
    if (match != null) return int.tryParse(match.group(1)!) ?? 0;
    return 0;
  }

  String get progress {
    if (duration <= 0) return '0%';
    return '${((currentTime / duration) * 100).round()}%';
  }

  String get thumbnail => cover;

  /// BFF response: { data: [{ animeId, continueId, episodeId, currentTime, duration, language, server, lastUpdated, anime: { animeId, title } }] }
  factory ContinueWatchingItem.fromBffJson(Map<String, dynamic> json) {
    final animeObj = json['anime'] as Map<String, dynamic>?;
    final animeId = json['animeId'] ?? animeObj?['animeId'] ?? '';
    return ContinueWatchingItem(
      animeId: animeId,
      continueId: json['continueId'] ?? '',
      episodeId: json['episodeId'] ?? 'ep-1',
      currentTime: (json['currentTime'] ?? 0).toDouble(),
      duration: (json['duration'] ?? 0).toDouble(),
      language: json['language'] ?? 'sub',
      server: json['server'] ?? 'suzu',
      lastUpdated: json['lastUpdated'],
      slug: animeId,
      title: animeObj?['title'] ?? json['title'] ?? '',
      cover: json['cover'] ?? json['thumbnail'] ?? '',
    );
  }

  factory ContinueWatchingItem.fromJson(Map<String, dynamic> json) =>
      ContinueWatchingItem.fromBffJson(json);
}

/// AnimeDetails — Project-R GET /anime/:slug
class AnimeDetails {
  final String id;
  final String animeId;
  final int? anilistId;
  final int? malId;
  final String title;
  final String english;
  final String romaji;
  final String nativeTitle;
  final String cover;
  final String image;
  final String? banner;
  final String description;
  final String status;
  final String format;
  final String? type;
  final int episodesTotal;
  final int? seasonYear;
  final String? season;
  final List<String> genres;
  final List<String> tags;
  final List<String> studios;
  final int? averageScore;
  final double? malScore;
  final int? popularity;
  final int? duration;
  final int subbed;
  final int dubbed;
  final bool canWatch;
  final bool canRequest;
  final String? folder;
  final bool inWatchlist;
  final String? trailerUrl;
  final int? lastEpisode;
  final List<EpisodeItem> episodes;
  final Map<String, dynamic>? watchProgress;

  AnimeDetails({
    required this.id,
    required this.animeId,
    this.anilistId,
    this.malId,
    required this.title,
    required this.english,
    required this.romaji,
    this.nativeTitle = '',
    required this.cover,
    required this.image,
    this.banner,
    this.description = '',
    this.status = '',
    this.format = '',
    this.type,
    this.episodesTotal = 0,
    this.seasonYear,
    this.season,
    this.genres = const [],
    this.tags = const [],
    this.studios = const [],
    this.averageScore,
    this.malScore,
    this.popularity,
    this.duration,
    this.subbed = 0,
    this.dubbed = 0,
    this.canWatch = false,
    this.canRequest = false,
    this.folder,
    this.inWatchlist = false,
    this.trailerUrl,
    this.lastEpisode,
    this.episodes = const [],
    this.watchProgress,
  });

  String get slug => animeId.isNotEmpty ? animeId : id;
  int get epCount => episodesTotal;
  String get imageUrl => cover.isNotEmpty ? cover : image;

  factory AnimeDetails.fromProjectRJson(Map<String, dynamic> json) {
    String english = '';
    String romaji = '';
    String nativeTitle = '';
    String displayTitle = '';
    final title = json['title'];
    if (title is Map<String, dynamic>) {
      english = title['english'] ?? '';
      romaji = title['romaji'] ?? '';
      nativeTitle = title['native'] ?? '';
      displayTitle = english.isNotEmpty ? english : romaji.isNotEmpty ? romaji : nativeTitle;
    } else if (title is String) {
      displayTitle = title;
    }

    String cover = '';
    String image = '';
    final coverImage = json['cover_image'];
    if (coverImage is Map<String, dynamic>) {
      cover = coverImage['extra_large'] ?? coverImage['large'] ?? coverImage['medium'] ?? '';
      image = cover;
    } else {
      cover = json['cover'] ?? '';
      image = cover;
    }

    String? banner;
    if (json['banner_image'] is String && (json['banner_image'] as String).isNotEmpty) {
      banner = json['banner_image'];
    } else {
      banner = json['banner'];
    }

    final animeId = json['anime_id'] ?? json['id'] ?? json['slug'] ?? '';

    List<String> studios = [];
    final studiosRaw = json['studios'];
    if (studiosRaw is List) {
      studios = studiosRaw.map<String>((s) {
        if (s is Map<String, dynamic>) return s['name'] ?? s.toString();
        return s.toString();
      }).toList();
    }

    List<EpisodeItem> episodes = [];
    final rawEpisodes = json['episodes'];
    if (rawEpisodes is List) {
      episodes = rawEpisodes
          .map((e) => e is Map<String, dynamic> ? EpisodeItem.fromJson(e) : EpisodeItem(episodeNumber: 0, id: ''))
          .toList();
    }

    String? trailerUrl;
    final trailer = json['trailer'];
    if (trailer is Map<String, dynamic>) {
      if (trailer['site'] == 'youtube' && trailer['id'] != null) {
        trailerUrl = 'https://www.youtube.com/watch?v=${trailer['id']}';
      }
    }

    Map<String, dynamic>? watchProgress;
    final wp = json['watch_progress'];
    if (wp is Map<String, dynamic>) watchProgress = wp;

    bool inWatchlist = false;
    String? folder;
    final wl = json['watchlist'];
    if (wl is Map<String, dynamic>) {
      inWatchlist = true;
      folder = wl['folder'];
    }

    return AnimeDetails(
      id: animeId,
      animeId: animeId,
      anilistId: json['anilist_id'] ?? json['anilistId'],
      malId: json['mal_id'] ?? json['malId'],
      title: displayTitle,
      english: english,
      romaji: romaji,
      nativeTitle: nativeTitle,
      cover: cover,
      image: image,
      banner: banner,
      description: json['description'] ?? '',
      status: json['status'] ?? '',
      format: json['format'] ?? '',
      type: json['type'],
      episodesTotal: json['episodes_total'] ?? json['episodes'] ?? 0,
      seasonYear: json['season_year'] ?? json['year'],
      season: json['season'],
      genres: List<String>.from(json['genres'] ?? []),
      tags: (json['tags'] as List?)?.map<String>((t) => t is Map ? t['name']?.toString() ?? t.toString() : t.toString()).toList() ?? [],
      studios: studios,
      averageScore: json['average_score'] ?? json['mean_score'],
      malScore: (json['mal_score'] ?? json['malScore'])?.toDouble(),
      popularity: json['popularity'],
      duration: json['duration'],
      subbed: json['subbed'] ?? 0,
      dubbed: json['dubbed'] ?? 0,
      canWatch: json['can_watch'] ?? json['canWatch'] ?? false,
      canRequest: json['can_request'] ?? json['canRequest'] ?? false,
      folder: folder,
      inWatchlist: inWatchlist,
      trailerUrl: trailerUrl,
      lastEpisode: json['last_episode'] ?? json['lastEpisode'],
      episodes: episodes,
      watchProgress: watchProgress,
    );
  }

  factory AnimeDetails.fromJson(Map<String, dynamic> json) {
    if (json.containsKey('data') && json['data'] is Map<String, dynamic>) {
      return AnimeDetails.fromProjectRJson(json['data']);
    }
    return AnimeDetails.fromProjectRJson(json);
  }
}

/// EpisodeItem — Project-R GET /anime/:slug/episodes
class EpisodeItem {
  final int episodeNumber;
  final String id;
  final String? title;
  final String? titleJapanese;
  final String? titleRomanji;
  final String? aired;
  final int? duration;
  final String? thumbnail;
  final String? description;
  final bool filler;
  final bool recap;
  final bool subbed;
  final bool dubbed;
  final bool playable;

  EpisodeItem({
    required this.episodeNumber,
    required this.id,
    this.title,
    this.titleJapanese,
    this.titleRomanji,
    this.aired,
    this.duration,
    this.thumbnail,
    this.description,
    this.filler = false,
    this.recap = false,
    this.subbed = false,
    this.dubbed = false,
    this.playable = false,
  });

  factory EpisodeItem.fromJson(Map<String, dynamic> json) {
    return EpisodeItem(
      episodeNumber: json['episode_number'] ?? json['number'] ?? 0,
      id: json['episodeId'] ?? json['id'] ?? '',
      title: json['title'],
      titleJapanese: json['title_japanese'],
      titleRomanji: json['title_romanji'],
      aired: json['aired'],
      duration: json['duration'],
      thumbnail: json['thumbnail'],
      description: json['description'],
      filler: json['is_filler'] ?? json['filler'] ?? false,
      recap: json['is_recap'] ?? json['recap'] ?? false,
      subbed: json['subbed'] ?? false,
      dubbed: json['dubbed'] ?? false,
      playable: json['playable'] ?? false,
    );
  }
}

/// BatchScrapeResponse — GET ?action=batch_scrape
class BatchScrapeResponse {
  final BatchScrapeLangData? sub;
  final BatchScrapeLangData? dub;

  BatchScrapeResponse({this.sub, this.dub});

  factory BatchScrapeResponse.fromJson(Map<String, dynamic> json) {
    return BatchScrapeResponse(
      sub: json['sub'] != null ? BatchScrapeLangData.fromJson(json['sub']) : null,
      dub: json['dub'] != null ? BatchScrapeLangData.fromJson(json['dub']) : null,
    );
  }
}

class BatchScrapeLangData {
  final String? providerId;
  final String? episodeId;
  final List<StreamInfo> sources;
  final List<StreamSubtitle> subtitles;

  BatchScrapeLangData({
    this.providerId,
    this.episodeId,
    this.sources = const [],
    this.subtitles = const [],
  });

  factory BatchScrapeLangData.fromJson(Map<String, dynamic> json) {
    return BatchScrapeLangData(
      providerId: json['providerId'],
      episodeId: json['episodeId'],
      sources: (json['sources'] as List?)?.map((s) => StreamInfo.fromJson(s)).toList() ?? [],
      subtitles: (json['subtitles'] as List?)?.map((s) => StreamSubtitle.fromJson(s)).toList() ?? [],
    );
  }
}

class StreamInfo {
  final String url;
  final String? quality;
  final Map<String, String>? headers;

  StreamInfo({required this.url, this.quality, this.headers});

  factory StreamInfo.fromJson(Map<String, dynamic> json) {
    Map<String, String>? headers;
    if (json['headers'] is Map) {
      headers = Map<String, String>.from(json['headers']);
    }
    return StreamInfo(
      url: json['url'] ?? '',
      quality: json['quality'],
      headers: headers,
    );
  }
}

class StreamSubtitle {
  final String url;
  final String label;

  StreamSubtitle({required this.url, required this.label});

  factory StreamSubtitle.fromJson(Map<String, dynamic> json) {
    return StreamSubtitle(
      url: json['url'] ?? '',
      label: json['label'] ?? '',
    );
  }
}

class WatchlistResponse {
  final List<WatchlistEntry> results;
  final int total;

  WatchlistResponse({this.results = const [], this.total = 0});

  factory WatchlistResponse.fromJson(Map<String, dynamic> json) {
    return WatchlistResponse(
      results: (json['results'] as List?)?.map((e) => WatchlistEntry.fromJson(e)).toList() ?? [],
      total: json['total'] ?? 0,
    );
  }
}

class WatchlistEntry {
  final String animeId;
  final String folder;
  final String? notes;
  final WatchlistAnimeInfo? anime;

  WatchlistEntry({required this.animeId, required this.folder, this.notes, this.anime});

  factory WatchlistEntry.fromJson(Map<String, dynamic> json) {
    return WatchlistEntry(
      animeId: json['animeId'] ?? '',
      folder: json['folder'] ?? '',
      notes: json['notes'],
      anime: json['anime'] != null ? WatchlistAnimeInfo.fromJson(json['anime']) : null,
    );
  }
}

class WatchlistAnimeInfo {
  final String animeId;
  final String? title;
  final String? imageUrl;

  WatchlistAnimeInfo({required this.animeId, this.title, this.imageUrl});

  factory WatchlistAnimeInfo.fromJson(Map<String, dynamic> json) {
    return WatchlistAnimeInfo(
      animeId: json['animeId'] ?? '',
      title: json['title'],
      imageUrl: json['imageUrl'],
    );
  }
}
