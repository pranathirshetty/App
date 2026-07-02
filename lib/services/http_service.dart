import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/session_model.dart';

class HttpService {
  /// Project-R catalog, home, search, anime info
  static const String projectRBaseUrl = 'https://api.reanime.to/api/v1';

  /// Anisurge BFF — auth, profile, watchlist, continue, progress, comments
  static const String bffBaseUrl = 'https://db.anisurge.qzz.io';

  /// v1 endpoint prefix on BFF
  static const String bffV1 = '$bffBaseUrl/v1';

  /// Streaming / batch_scrape endpoint
  static const String streamingUrl = 'https://fec.anisurge.lol/api';

  final storage = const FlutterSecureStorage();

  // ── Session management ──

  Future<SessionInfo?> getStoredSession() async {
    try {
      final storedData = await storage.read(key: 'session_info');
      if (storedData != null) {
        return SessionInfo.fromJson(jsonDecode(storedData));
      }
      return null;
    } catch (_) {
      return null;
    }
  }

  Future<void> saveSession(SessionInfo session) async {
    await storage.write(
        key: 'session_info', value: jsonEncode(session.toJson()));
  }

  Future<void> clearSession() async {
    await storage.delete(key: 'session_info');
  }

  // ── Auth headers ──

  /// Apply Project-R Bearer token
  Map<String, String> projectRAuthHeaders(SessionInfo session) {
    return {'Authorization': 'Bearer ${session.projectRToken}'};
  }

  /// Apply Anisurge BFF Bearer token
  /// Throws if no anisurgeToken is available.
  Map<String, String> bffAuthHeaders(SessionInfo session) {
    if (!session.hasAnisurgeToken) {
      throw Exception('Missing Anisurge session token');
    }
    return {'Authorization': 'Bearer ${session.anisurgeToken}'};
  }

  // ── Project-R requests (catalog, home, search, anime info) ──

  Future<http.Response> getProjectR(
    String endpoint, {
    Map<String, String>? queryParams,
    SessionInfo? session,
  }) async {
    final uri = Uri.parse('$projectRBaseUrl$endpoint');
    final finalUri =
        queryParams != null ? uri.replace(queryParameters: queryParams) : uri;

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    if (session != null) {
      headers.addAll(projectRAuthHeaders(session));
    }

    return await http.get(finalUri, headers: headers);
  }

  // ── BFF requests (auth, profile, watchlist, continue, progress, comments) ──

  Future<http.Response> getBff(
    String endpoint, {
    Map<String, String>? queryParams,
    required SessionInfo session,
  }) async {
    final uri = Uri.parse('$bffV1$endpoint');
    final finalUri =
        queryParams != null ? uri.replace(queryParameters: queryParams) : uri;

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    headers.addAll(bffAuthHeaders(session));

    return await http.get(finalUri, headers: headers);
  }

  Future<http.Response> postBff(
    String endpoint, {
    Map<String, dynamic>? body,
    required SessionInfo session,
  }) async {
    final uri = Uri.parse('$bffV1$endpoint');
    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    headers.addAll(bffAuthHeaders(session));

    return await http.post(uri, headers: headers, body: body != null ? jsonEncode(body) : null);
  }

  Future<http.Response> deleteBff(
    String endpoint, {
    required SessionInfo session,
  }) async {
    final uri = Uri.parse('$bffV1$endpoint');
    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    headers.addAll(bffAuthHeaders(session));

    return await http.delete(uri, headers: headers);
  }

  // ── Streaming / batch_scrape ──

  /// Fetch available stream sources for a given anilistId
  Future<http.Response> fetchStreamSources({
    required int anilistId,
    int? episodeNumber,
    String? source,
  }) async {
    final queryParams = <String, String>{
      'action': 'batch_scrape',
      'anilistId': anilistId.toString(),
    };
    if (episodeNumber != null) queryParams['episode'] = episodeNumber.toString();
    if (source != null) queryParams['source'] = source;

    final uri = Uri.parse(streamingUrl).replace(queryParameters: queryParams);
    return await http.get(uri, headers: {'Content-Type': 'application/json'});
  }

  // ── Legacy methods kept for backward compat ──

  @Deprecated('Use getProjectR or getBff instead')
  Future<http.Response> get(
    String endpoint, {
    Map<String, String>? queryParams,
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    const oldBaseUrl = 'https://anime.anisurge.qzz.io';
    final uri = Uri.parse('$oldBaseUrl$endpoint');
    final finalUri =
        queryParams != null ? uri.replace(queryParameters: queryParams) : uri;

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };

    if (requireAuth) {
      final cookies = await _getOldCookies();
      if (cookies != null) {
        headers['Cookie'] = cookies;
      }
    }

    return await http.get(finalUri, headers: headers);
  }

  @Deprecated('Use postBff instead')
  Future<http.Response> post(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    const oldBaseUrl = 'https://anime.anisurge.qzz.io';
    final uri = Uri.parse('$oldBaseUrl$endpoint');
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (requireAuth) {
      final cookies = await _getOldCookies();
      if (cookies != null) headers['Cookie'] = cookies;
    }
    return await http.post(uri, headers: headers, body: body != null ? jsonEncode(body) : null);
  }

  @Deprecated('Use postBff instead')
  Future<http.Response> put(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    const oldBaseUrl = 'https://anime.anisurge.qzz.io';
    final uri = Uri.parse('$oldBaseUrl$endpoint');
    final headers = <String, String>{'Content-Type': 'application/json'};
    if (requireAuth) {
      final cookies = await _getOldCookies();
      if (cookies != null) headers['Cookie'] = cookies;
    }
    return await http.put(uri, headers: headers, body: body != null ? jsonEncode(body) : null);
  }

  Future<String?> _getOldCookies() async {
    try {
      final storedData = await storage.read(key: 'session_info');
      if (storedData != null) {
        final sessionInfo = SessionInfo.fromJson(jsonDecode(storedData));
        return 'session_id=${sessionInfo.externalUserId}; session_secret=${sessionInfo.projectRToken}; user_id=${sessionInfo.externalUserId}';
      }
      return null;
    } catch (e) {
      return null;
    }
  }
}
