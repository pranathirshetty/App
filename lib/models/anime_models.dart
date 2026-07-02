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

class NextAiringEpisode {
  final String airingAt;
  final int episode;
  final int timeUntilAiring;

  NextAiringEpisode({
    this.airingAt = '',
    this.episode = 0,
    this.timeUntilAiring = 0,
  });

  factory NextAiringEpisode.fromJson(Map<String, dynamic> json) {
    return NextAiringEpisode(
      airingAt: json['airing_at'] ?? json['airingAt'] ?? '',
      episode: json['episode'] ?? 0,
      timeUntilAiring: json['time_until_airing'] ?? json['timeUntilAiring'] ?? 0,
    );
  }
}

class HomeData {
  final List<AnimeItem> latestAired;
  final List<AnimeItem> newOnSite;
  final List<AnimeItem> trending;
  final List<AnimeItem> upcoming;
  final bool hasContinueWatching;

  HomeData({
    this.latestAired = const [],
    this.newOnSite = const [],
    this.trending = const [],
    this.upcoming = const [],
    this.hasContinueWatching = false,
  });

  factory HomeData.fromJson(Map<String, dynamic> json) {
    return HomeData(
      latestAired: _parseAnimeList(json['latest_aired'] ?? json['latestAired']),
      newOnSite: _parseAnimeList(json['new_on_site'] ?? json['newOnSite']),
      trending: _parseAnimeList(json['trending']),
      upcoming: _parseAnimeList(json['upcoming']),
      hasContinueWatching: json['has_continue_watching'] ?? json['hasContinueWatching'] ?? false,
    );
  }

  static List<AnimeItem> _parseAnimeList(dynamic data) {
    if (data == null) return [];
    if (data is List) {
      return data.map((e) => AnimeItem.fromJson(e)).toList();
    }
    return [];
  }
}

class ContinueWatchingItem {
  final AnimeItem anime;
  final String animeIdStr;
  final String continueId;
  final String episodeId;
  final double progress;
  final double duration;
  final String? server;
  final String? language;
  final String? updatedAt;
  final int episode;
  final String episodeThumbnail;

  ContinueWatchingItem({
    AnimeItem? anime,
    this.animeIdStr = '',
    this.continueId = '',
    this.episodeId = '',
    this.progress = 0.0,
    this.duration = 0.0,
    this.server,
    this.language,
    this.updatedAt,
    this.episode = 0,
    this.episodeThumbnail = '',
  }) : anime = anime ?? AnimeItem();

  factory ContinueWatchingItem.fromJson(Map<String, dynamic> json) {
    return ContinueWatchingItem(
      anime: json['anime'] != null
          ? AnimeItem.fromJson(json['anime'])
          : AnimeItem(
              animeId: json['animeId'] ?? json['id'] ?? '',
              title: AnimeTitle(english: json['title'] ?? ''),
              coverImage: CoverImage(
                extraLarge: json['thumbnail'] ?? json['cover'] ?? '',
              ),
            ),
      animeIdStr: json['animeId'] ?? '',
      continueId: json['continueId'] ?? '',
      episodeId: json['episodeId'] ?? '',
      progress: (json['currentTime'] ?? json['progress'] ?? 0).toDouble(),
      duration: (json['duration'] ?? 0).toDouble(),
      server: json['server'],
      language: json['language'],
      updatedAt: json['lastUpdated'] ?? json['updatedAt'],
      episode: json['episode'] ?? _episodeNumberFromId(json['episodeId'] ?? ''),
      episodeThumbnail: json['episodeThumbnail'] ?? json['episode_thumbnail'] ?? '',
    );
  }

  static int _episodeNumberFromId(String id) {
    final s = id.trim().toLowerCase();
    if (s.startsWith('ep-')) {
      final tail = s.substring(3).trim();
      return int.tryParse(tail.split('-').first) ?? 0;
    }
    return int.tryParse(s) ?? 0;
  }

  String get displayTitle => anime.displayTitle;
  String get imageUrl =>
      episodeThumbnail.isNotEmpty ? episodeThumbnail : anime.imageUrl;
  String? get bannerUrl => anime.bannerUrl;
  String get animeId => animeIdStr.isNotEmpty ? animeIdStr : anime.animeId;
  int get displayEpisode => episode > 0 ? episode : _episodeNumberFromId(episodeId);
}

class ContinueWatchingResponse {
  final List<ContinueWatchingItem> data;
  final int limit;
  final int offset;
  final int total;

  ContinueWatchingResponse({
    this.data = const [],
    this.limit = 20,
    this.offset = 0,
    this.total = 0,
  });

  factory ContinueWatchingResponse.fromJson(Map<String, dynamic> json) {
    return ContinueWatchingResponse(
      data: json['data'] != null
          ? (json['data'] as List)
              .map((e) => ContinueWatchingItem.fromJson(e))
              .toList()
          : [],
      limit: json['limit'] ?? 20,
      offset: json['offset'] ?? 0,
      total: json['total'] ?? 0,
    );
  }
}

class EpisodeItem {
  final int number;
  final String id;
  final String? title;
  final String? titleJapanese;
  final String? titleRomanji;
  final String? aired;
  final int? duration;
  final String? thumbnail;
  final String? description;
  final bool? filler;
  final bool? recap;
  final bool subbed;
  final bool dubbed;
  final bool playable;
  final String? site;
  final String? updatedAt;
  final String? url;

  EpisodeItem({
    this.number = 0,
    this.id = '',
    this.title,
    this.titleJapanese,
    this.titleRomanji,
    this.aired,
    this.duration,
    this.thumbnail,
    this.description,
    this.filler,
    this.recap,
    this.subbed = false,
    this.dubbed = false,
    this.playable = false,
    this.site,
    this.updatedAt,
    this.url,
  });

  factory EpisodeItem.fromJson(Map<String, dynamic> json) {
    return EpisodeItem(
      number: json['episode_number'] ?? json['number'] ?? 0,
      id: json['episodeId'] ?? json['id'] ?? '',
      title: json['title'],
      titleJapanese: json['title_japanese'] ?? json['titleJapanese'],
      titleRomanji: json['title_romanji'] ?? json['titleRomanji'],
      aired: json['aired'],
      duration: json['duration'],
      thumbnail: json['thumbnail'],
      description: json['description'],
      filler: json['is_filler'] ?? json['filler'],
      recap: json['is_recap'] ?? json['recap'],
      subbed: json['subbed'] ?? false,
      dubbed: json['dubbed'] ?? false,
      playable: json['playable'] ?? false,
      site: json['site'],
      updatedAt: json['updated_at'] ?? json['updatedAt'],
      url: json['url'],
    );
  }
}

class EpisodeListResponse {
  final List<EpisodeItem> episodes;
  final int? total;
  final int? limit;
  final int? offset;
  final int? totalPages;

  EpisodeListResponse({
    this.episodes = const [],
    this.total,
    this.limit,
    this.offset,
    this.totalPages,
  });

  factory EpisodeListResponse.fromJson(Map<String, dynamic> json) {
    return EpisodeListResponse(
      episodes: json['data'] != null
          ? (json['data'] as List).map((e) => EpisodeItem.fromJson(e)).toList()
          : [],
      total: json['total'],
      limit: json['limit'],
      offset: json['offset'],
      totalPages: json['totalPages'] ?? json['total_pages'],
    );
  }
}

class ScheduleDay {
  final String date;
  final String day;
  final List<ScheduleAnime> episodes;

  ScheduleDay({
    this.date = '',
    this.day = '',
    this.episodes = const [],
  });

  factory ScheduleDay.fromJson(Map<String, dynamic> json) {
    return ScheduleDay(
      date: json['date'] ?? '',
      day: json['day'] ?? '',
      episodes: json['episodes'] != null
          ? (json['episodes'] as List)
              .map((e) => ScheduleAnime.fromJson(e))
              .toList()
          : [],
    );
  }
}

class ScheduleAnime {
  final String animeId;
  final AnimeTitle title;
  final CoverImage coverImage;
  final String bannerImage;
  final String description;
  final String format;
  final String status;
  final List<String> genres;
  final int seasonYear;
  final String? season;
  final int? averageScore;
  final int? popularity;
  final int subbed;
  final int dubbed;
  final String duration;
  final int episodeNumber;
  final int episodesTotal;
  final String airingStatus;
  final String airType;
  final String episodeDate;
  final String route;
  final int? anilistId;
  final int? malId;
  final String? delayedFrom;
  final String? delayedUntil;
  final String? airingAt;
  final String? subRelease;
  final int? epCount;
  final int? year;
  final String? type;
  final bool canWatch;
  final String id;
  final String time;
  final String cover;
  final String? banner;

  ScheduleAnime({
    this.animeId = '',
    AnimeTitle? title,
    CoverImage? coverImage,
    this.bannerImage = '',
    this.description = '',
    this.format = 'TV',
    this.status = '',
    this.genres = const [],
    this.seasonYear = 0,
    this.season,
    this.averageScore,
    this.popularity,
    this.subbed = 0,
    this.dubbed = 0,
    this.duration = '',
    this.episodeNumber = 1,
    this.episodesTotal = 0,
    this.airingStatus = '',
    this.airType = '',
    this.episodeDate = '',
    this.route = '',
    this.anilistId,
    this.malId,
    this.delayedFrom,
    this.delayedUntil,
    this.airingAt,
    this.subRelease,
    this.epCount,
    this.year,
    this.type,
    this.canWatch = false,
    this.id = '',
    this.time = '',
    this.cover = '',
    this.banner,
  })  : title = title ?? AnimeTitle(),
        coverImage = coverImage ?? CoverImage();

  factory ScheduleAnime.fromJson(Map<String, dynamic> json) {
    return ScheduleAnime(
      animeId: json['anime_id'] ?? json['id'] ?? '',
      title: json['title'] != null
          ? AnimeTitle.fromJson(json['title'])
          : AnimeTitle(english: json['title'] ?? ''),
      coverImage: json['cover_image'] != null
          ? CoverImage.fromJson(json['cover_image'])
          : CoverImage(extraLarge: json['cover'] ?? ''),
      bannerImage: json['banner_image'] ?? json['banner'] ?? '',
      description: json['description'] ?? '',
      format: json['format'] ?? 'TV',
      status: json['status'] ?? '',
      genres: json['genres'] != null ? List<String>.from(json['genres']) : [],
      seasonYear: json['season_year'] ?? json['year'] ?? 0,
      season: json['season'],
      averageScore: json['average_score'] ?? json['averageScore'],
      popularity: json['popularity'],
      subbed: json['subbed'] ?? 0,
      dubbed: json['dubbed'] ?? 0,
      duration: json['duration']?.toString() ?? '',
      episodeNumber: json['episode_number'] ?? json['episode'] ?? 1,
      episodesTotal: json['episodes_total'] ?? json['epCount'] ?? 0,
      airingStatus: json['airing_status'] ?? json['airingStatus'] ?? '',
      airType: json['air_type'] ?? json['airType'] ?? '',
      episodeDate: json['episode_date'] ?? json['episodeDate'] ?? '',
      route: json['route'] ?? '',
      anilistId: json['anilist_id'] ?? json['anilistId'],
      malId: json['mal_id'] ?? json['malId'],
      delayedFrom: json['delayed_from'] ?? json['delayedFrom'],
      delayedUntil: json['delayed_until'] ?? json['delayedUntil'],
      airingAt: json['airing_at'] ?? json['airingAt'],
      subRelease: json['sub_release'] ?? json['subRelease'],
      epCount: json['epCount'],
      year: json['year'],
      type: json['type'],
      canWatch: json['can_watch'] ?? json['canWatch'] ?? false,
      id: json['id'] ?? '',
      time: json['time'] ?? '',
      cover: json['cover'] ?? '',
      banner: json['banner'],
    );
  }

  String get displayTitle => title.displayTitle;
  String get imageUrl => coverImage.bestUrl.isNotEmpty
      ? coverImage.bestUrl
      : (cover.startsWith('http') ? cover : '');
  String? get bannerUrl =>
      bannerImage.isNotEmpty ? bannerImage : (banner?.startsWith('http') == true ? banner : null);
  String get activeSlug => animeId.isNotEmpty ? animeId : id;
}

class ScheduleApiResponse {
  final List<ScheduleDay> schedule;
  final String timezone;
  final int? year;
  final int? month;

  ScheduleApiResponse({
    this.schedule = const [],
    this.timezone = 'UTC',
    this.year,
    this.month,
  });

  factory ScheduleApiResponse.fromJson(Map<String, dynamic> json) {
    return ScheduleApiResponse(
      schedule: json['schedule'] != null
          ? (json['schedule'] as List)
              .map((e) => ScheduleDay.fromJson(e))
              .toList()
          : [],
      timezone: json['timezone'] ?? 'UTC',
      year: json['year'],
      month: json['month'],
    );
  }
}

class SearchResponse {
  final List<AnimeItem> results;
  final int total;
  final int limit;
  final int offset;

  SearchResponse({
    this.results = const [],
    this.total = 0,
    this.limit = 20,
    this.offset = 0,
  });

  factory SearchResponse.fromJson(Map<String, dynamic> json) {
    return SearchResponse(
      results: json['results'] != null
          ? (json['results'] as List).map((e) => AnimeItem.fromJson(e)).toList()
          : [],
      total: json['total'] ?? 0,
      limit: json['limit'] ?? 20,
      offset: json['offset'] ?? 0,
    );
  }

  bool get hasMore => offset + limit < total;
}

class WatchlistEntry {
  final AnimeItem anime;
  final String folder;
  final String? notes;
  final String? startedAt;
  final String? completedAt;

  WatchlistEntry({
    AnimeItem? anime,
    this.folder = 'PLANNING',
    this.notes,
    this.startedAt,
    this.completedAt,
  }) : anime = anime ?? AnimeItem();

  factory WatchlistEntry.fromJson(Map<String, dynamic> json) {
    final anime = json['anime'] != null
        ? AnimeItem.fromJson(json['anime'])
        : AnimeItem.fromJson(json);
    return WatchlistEntry(
      anime: anime,
      folder: json['folder'] ?? json['status'] ?? 'PLANNING',
      notes: json['notes'],
      startedAt: json['started_at'] ?? json['startedAt'],
      completedAt: json['completed_at'] ?? json['completedAt'],
    );
  }

  String get id => anime.animeId;
  String get title => anime.displayTitle;
  String get image => anime.imageUrl;
  String get type => anime.type ?? '';
  int get subbed => anime.subbed;
  int get dubbed => anime.dubbed;
  String get duration => anime.duration;
  String get status => folder;
}

class WatchlistResponse {
  final List<WatchlistEntry> data;
  final int total;
  final int limit;
  final int offset;

  WatchlistResponse({
    this.data = const [],
    this.total = 0,
    this.limit = 20,
    this.offset = 0,
  });

  factory WatchlistResponse.fromJson(Map<String, dynamic> json) {
    final items = json['data'] ?? json['watchlist'] ?? [];
    return WatchlistResponse(
      data: items is List
          ? items.map((e) => WatchlistEntry.fromJson(e)).toList()
          : [],
      total: json['total'] ?? 0,
      limit: json['limit'] ?? 20,
      offset: json['offset'] ?? 0,
    );
  }
}

class WatchProgressDetail {
  final String? episodeId;
  final double? currentTime;
  final double? duration;
  final String? server;
  final String? language;
  final String? lastUpdated;

  WatchProgressDetail({
    this.episodeId,
    this.currentTime,
    this.duration,
    this.server,
    this.language,
    this.lastUpdated,
  });

  factory WatchProgressDetail.fromJson(Map<String, dynamic> json) {
    return WatchProgressDetail(
      episodeId: json['episode_id'] ?? json['episodeId'],
      currentTime: (json['current_time'] ?? json['currentTime'])?.toDouble(),
      duration: (json['duration'])?.toDouble(),
      server: json['server'],
      language: json['language'],
      lastUpdated: json['last_updated'] ?? json['lastUpdated'],
    );
  }
}

class WatchInfoResponse {
  final AnimeItem? anime;
  final String? folder;
  final WatchProgressDetail? progress;
  final List<EpisodeItem>? episodes;

  WatchInfoResponse({this.anime, this.folder, this.progress, this.episodes});

  factory WatchInfoResponse.fromJson(Map<String, dynamic> json) {
    return WatchInfoResponse(
      anime: json['anime'] != null ? AnimeItem.fromJson(json['anime']) : null,
      folder: json['folder'],
      progress: json['progress'] != null
          ? WatchProgressDetail.fromJson(json['progress'])
          : null,
      episodes: json['episodes'] != null
          ? (json['episodes'] as List)
              .map((e) => EpisodeItem.fromJson(e))
              .toList()
          : null,
    );
  }

  String? get animeId => anime?.animeId;
  int? get anilistId => anime?.anilistId;
  int? get malId => anime?.malId;
  double? get currentTime => progress?.currentTime;
  String? get server => progress?.server;
}

class TopAnimeResponse {
  final List<AnimeItem> data;
  final String? period;
  final bool? cached;

  TopAnimeResponse({this.data = const [], this.period, this.cached});

  factory TopAnimeResponse.fromJson(Map<String, dynamic> json) {
    return TopAnimeResponse(
      data: json['data'] != null
          ? (json['data'] as List).map((e) => AnimeItem.fromJson(e)).toList()
          : [],
      period: json['period'],
      cached: json['cached'],
    );
  }
}

class LatestAiredResponse {
  final List<AnimeItem> episodes;
  final bool hasMore;
  final String? nextCursor;

  LatestAiredResponse({
    this.episodes = const [],
    this.hasMore = false,
    this.nextCursor,
  });

  factory LatestAiredResponse.fromJson(Map<String, dynamic> json) {
    return LatestAiredResponse(
      episodes: json['data'] != null
          ? (json['data'] as List).map((e) => AnimeItem.fromJson(e)).toList()
          : [],
      hasMore: json['has_more'] ?? json['hasMore'] ?? false,
      nextCursor: json['next_cursor'] ?? json['nextCursor'],
    );
  }
}
