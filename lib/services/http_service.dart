import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/session_model.dart';

class HttpService {
  static const String projectRBaseUrl = 'https://api.reanime.to/api/v1';
  static const String bffBaseUrl = 'https://db.anisurge.qzz.io';
  final storage = const FlutterSecureStorage();

  Future<SessionInfo?> _getSession() async {
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

  String _resolveBaseUrl(String endpoint, {bool useBff = false}) {
    if (endpoint.startsWith('http://') || endpoint.startsWith('https://')) {
      return endpoint;
    }
    final base = useBff ? bffBaseUrl : projectRBaseUrl;
    return '$base$endpoint';
  }

  Future<Map<String, String>> _buildHeaders({
    bool requireAuth = false,
    bool anisurgeAuth = false,
  }) async {
    final headers = <String, String>{
      'Content-Type': 'application/json',
    };
    if (requireAuth) {
      final session = await _getSession();
      if (session != null) {
        final token = anisurgeAuth
            ? (session.anisurgeToken ?? session.token)
            : session.token;
        if (token.isNotEmpty) {
          headers['Authorization'] = 'Bearer $token';
        }
      }
    }
    return headers;
  }

  Future<http.Response> get(
    String endpoint, {
    Map<String, String>? queryParams,
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    final url = _resolveBaseUrl(endpoint, useBff: useBff);
    final uri = Uri.parse(url);
    final finalUri =
        queryParams != null ? uri.replace(queryParameters: queryParams) : uri;
    final headers = await _buildHeaders(
      requireAuth: requireAuth,
      anisurgeAuth: useBff,
    );
    return await http.get(finalUri, headers: headers);
  }

  Future<http.Response> post(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    final url = _resolveBaseUrl(endpoint, useBff: useBff);
    final uri = Uri.parse(url);
    final headers = await _buildHeaders(
      requireAuth: requireAuth,
      anisurgeAuth: useBff,
    );
    final requestBody = body != null ? jsonEncode(body) : null;
    return await http.post(uri, headers: headers, body: requestBody);
  }

  Future<http.Response> put(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    final url = _resolveBaseUrl(endpoint, useBff: useBff);
    final uri = Uri.parse(url);
    final headers = await _buildHeaders(
      requireAuth: requireAuth,
      anisurgeAuth: useBff,
    );
    final requestBody = body != null ? jsonEncode(body) : null;
    return await http.put(uri, headers: headers, body: requestBody);
  }

  Future<http.Response> delete(
    String endpoint, {
    bool requireAuth = false,
    bool useBff = false,
  }) async {
    final url = _resolveBaseUrl(endpoint, useBff: useBff);
    final uri = Uri.parse(url);
    final headers = await _buildHeaders(
      requireAuth: requireAuth,
      anisurgeAuth: useBff,
    );
    return await http.delete(uri, headers: headers);
  }
}
