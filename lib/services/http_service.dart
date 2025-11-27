import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/session_model.dart';

class HttpService {
  static const String baseUrl = 'https://kuudere.to';
  final storage = const FlutterSecureStorage();

  // Get cookies string from stored session
  Future<String?> _getCookies() async {
    try {
      final storedData = await storage.read(key: 'session_info');
      if (storedData != null) {
        final sessionInfo = SessionInfo.fromJson(jsonDecode(storedData));
        // Format cookies as: session_id=xxx; session_secret=xxx; user_id=xxx
        return 'session_id=${sessionInfo.sessionId}; session_secret=${sessionInfo.session}; user_id=${sessionInfo.userId}';
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Make GET request with optional authentication
  Future<http.Response> get(
    String endpoint, {
    Map<String, String>? queryParams,
    bool requireAuth = false,
  }) async {
    final uri = Uri.parse('$baseUrl$endpoint');
    final finalUri = queryParams != null
        ? uri.replace(queryParameters: queryParams)
        : uri;

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };

    if (requireAuth) {
      final cookies = await _getCookies();
      if (cookies != null) {
        headers['Cookie'] = cookies;
      }
    }

    return await http.get(finalUri, headers: headers);
  }

  // Make POST request with optional authentication
  Future<http.Response> post(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requireAuth = false,
  }) async {
    final uri = Uri.parse('$baseUrl$endpoint');

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };

    if (requireAuth) {
      final cookies = await _getCookies();
      if (cookies != null) {
        headers['Cookie'] = cookies;
      }
    }

    final requestBody = body != null ? jsonEncode(body) : null;
    return await http.post(uri, headers: headers, body: requestBody);
  }

  // Make PUT request with optional authentication
  Future<http.Response> put(
    String endpoint, {
    Map<String, dynamic>? body,
    bool requireAuth = false,
  }) async {
    final uri = Uri.parse('$baseUrl$endpoint');

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };

    if (requireAuth) {
      final cookies = await _getCookies();
      if (cookies != null) {
        headers['Cookie'] = cookies;
      }
    }

    final requestBody = body != null ? jsonEncode(body) : null;
    return await http.put(uri, headers: headers, body: requestBody);
  }

  // Make DELETE request with optional authentication
  Future<http.Response> delete(
    String endpoint, {
    bool requireAuth = false,
  }) async {
    final uri = Uri.parse('$baseUrl$endpoint');

    final headers = <String, String>{
      'Content-Type': 'application/json',
    };

    if (requireAuth) {
      final cookies = await _getCookies();
      if (cookies != null) {
        headers['Cookie'] = cookies;
      }
    }

    return await http.delete(uri, headers: headers);
  }
}

