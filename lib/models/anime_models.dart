class AnimeTitle {
  final String english;
  final String native_;
  final String romaji;
  final String userPreferred;

  AnimeTitle({
    this.english = '',
    this.native_ = '',
    this.romaji = '',
    this.userPreferred = '',
  });

  factory AnimeTitle.fromJson(Map<String, dynamic> json) {
    return AnimeTitle(
      english: json['english'] ?? '',
      native_: json['native'] ?? '',
      romaji: json['romaji'] ?? '',
      userPreferred: json['user_preferred'] ?? '',
    );
  }

  String get displayTitle =>
      english.isNotEmpty ? english : userPreferred.isNotEmpty ? userPreferred : romaji.isNotEmpty ? romaji : native_;

  String displayTitleWithRomaji(bool preferRomaji) =>
      preferRomaji
          ? (romaji.isNotEmpty ? romaji : userPreferred.isNotEmpty ? userPreferred : english.isNotEmpty ? english : native_)
          : displayTitle;
}

class CoverImage {
  final String color;
  final String extraLarge;
  final String large;
  final String medium;

  CoverImage({
    this.color = '',
    this.extraLarge = '',
    this.large = '',
    this.medium = '',
  });

  factory CoverImage.fromJson(Map<String, dynamic> json) {
    return CoverImage(
      color: json['color'] ?? '',
      extraLarge: json['extra_large'] ?? json['extraLarge'] ?? '',
      large: json['large'] ?? '',
      medium: json['medium'] ?? '',
    );
  }

  String get bestUrl =>
      extraLarge.isNotEmpty ? extraLarge : large.isNotEmpty ? large : medium;
}

class AnimeItem {
  final String animeId;
  final AnimeTitle title;
  final CoverImage coverImage;
  final String bannerImage;
  final String description;
  final String format;
  final String status;
  final int? episodes;
  final int? episodesTotal;
  final int? seasonYear;
  final String? season;
  final List<String> genres;
  final int? averageScore;
  final int? malScore;
  final int? meanScore;
  final int? popularity;
  final String duration;
  final int subbed;
  final int dubbed;
  final bool canWatch;
  final bool canRequest;
  final String? folder;
  final String? type;
  final EpisodeBrief? episode;
  final NextAiringEpisode? nextAiringEpisode;
  final int? anilistId;
  final int? malId;

  AnimeItem({
    this.animeId = '',
    AnimeTitle? title,
    CoverImage? coverImage,
    this.bannerImage = '',
    this.description = '',
    this.format = '',
    this.status = '',
    this.episodes,
    this.episodesTotal,
    this.seasonYear,
    this.season,
    this.genres = const [],
    this.averageScore,
    this.malScore,
    this.meanScore,
    this.popularity,
    this.duration = '',
    this.subbed = 0,
    this.dubbed = 0,
    this.canWatch = false,
    this.canRequest = false,
    this.folder,
    this.type,
    this.episode,
    this.nextAiringEpisode,
    this.anilistId,
    this.malId,
  })  : title = title ?? AnimeTitle(),
        coverImage = coverImage ?? CoverImage();

  factory AnimeItem.fromJson(Map<String, dynamic> json) {
    return AnimeItem(
      animeId: json['anime_id'] ?? json['id'] ?? json['mainId'] ?? '',
      title: json['title'] != null
          ? AnimeTitle.fromJson(json['title'])
          : AnimeTitle(
              english: json['english'] ?? '',
              romaji: json['romaji'] ?? '',
              native_: json['native'] ?? '',
            ),
      coverImage: json['cover_image'] != null
          ? CoverImage.fromJson(json['cover_image'])
          : CoverImage(
              extraLarge: json['cover'] ?? json['image'] ?? '',
              large: json['poster'] ?? '',
            ),
      bannerImage: json['banner_image'] ?? json['banner'] ?? json['carouselBanner'] ?? '',
      description: json['description'] ?? '',
      format: json['format'] ?? '',
      status: json['status'] ?? '',
      episodes: json['episodes'],
      episodesTotal: json['episodes_total'] ?? json['epCount'] ?? json['episodesTotal'],
      seasonYear: json['season_year'] ?? json['year'] ?? json['seasonYear'],
      season: json['season'],
      genres: json['genres'] != null ? List<String>.from(json['genres']) : [],
      averageScore: json['average_score'] ?? json['averageScore'],
      malScore: json['mal_score'] ?? json['malScore']?.toDouble()?.toInt(),
      meanScore: json['mean_score'] ?? json['meanScore'],
      popularity: json['popularity'],
      duration: json['duration']?.toString() ?? '',
      subbed: json['subbed'] ?? json['subbedCount'] ?? 0,
      dubbed: json['dubbed'] ?? json['dubbedCount'] ?? 0,
      canWatch: json['can_watch'] ?? json['canWatch'] ?? false,
      canRequest: json['can_request'] ?? json['canRequest'] ?? false,
      folder: json['folder'],
      type: json['type'],
      episode: json['episode'] != null
          ? EpisodeBrief.fromJson(json['episode'])
          : null,
      nextAiringEpisode: json['next_airing_episode'] != null
          ? NextAiringEpisode.fromJson(json['next_airing_episode'])
          : null,
      anilistId: json['anilist_id'] ?? json['anilistId'],
      malId: json['mal_id'] ?? json['malId'],
    );
  }

  String get displayTitle => title.displayTitle;
  String get imageUrl => coverImage.bestUrl;
  String? get bannerUrl => bannerImage.isNotEmpty ? bannerImage : null;
  String get id => animeId;
  String get english => title.english;
  String get romaji => title.romaji;
  String get cover => coverImage.bestUrl;
  String? get banner => bannerUrl;
  String get image => coverImage.extraLarge;
  String get poster => coverImage.large;
  String get slug => animeId;
  int? get year => seasonYear;
  int? get epCount => episodes ?? episodesTotal;
  int? get score => averageScore ?? meanScore ?? malScore;
}

class EpisodeBrief {
  final int episodeNumber;
  final String aired;
  final bool playable;
  final String thumbnail;
  final String title;

  EpisodeBrief({
    this.episodeNumber = 0,
    this.aired = '',
    this.playable = false,
    this.thumbnail = '',
    this.title = '',
  });

  factory EpisodeBrief.fromJson(Map<String, dynamic> json) {
    return EpisodeBrief(
      episodeNumber: json['episode_number'] ?? json['episodeNumber'] ?? 0,
      aired: json['aired'] ?? '',
      playable: json['playable'] ?? false,
      thumbnail: json['thumbnail'] ?? '',
      title: json['title'] ?? '',
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
