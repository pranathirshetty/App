import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/session_model.dart';

class AuthService {
  static const String bffBaseUrl = 'https://db.anisurge.qzz.io';
  final storage = const FlutterSecureStorage();

  Future<SessionInfo> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$bffBaseUrl/v1/auth/login'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'identifier': email, 'password': password}),
      );
      final data = jsonDecode(response.body);
      if (response.statusCode == 200) {
        final session = SessionInfo(
          token: data['projectRToken'] ?? '',
          anisurgeToken: data['anisurgeToken'],
        );
        await storage.write(
            key: 'session_info', value: jsonEncode(session.toJson()));
        return session;
      }
      throw Exception(data['error'] ?? data['message'] ?? 'Login failed');
    } catch (e) {
      rethrow;
    }
  }

  Future<SessionInfo> register(
      String email, String password, String username) async {
    try {
      final response = await http.post(
        Uri.parse('$bffBaseUrl/v1/auth/signup'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode(
            {'username': username, 'email': email, 'password': password}),
      );
      final data = jsonDecode(response.body);
      if (response.statusCode == 200 || response.statusCode == 201) {
        final session = SessionInfo(
          token: data['projectRToken'] ?? '',
          anisurgeToken: data['anisurgeToken'],
        );
        await storage.write(
            key: 'session_info', value: jsonEncode(session.toJson()));
        return session;
      }
      throw Exception(data['error'] ?? data['message'] ?? 'Registration failed');
    } catch (e) {
      rethrow;
    }
  }

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

  bool isSessionExpired(SessionInfo session) {
    return false;
  }

  Future<void> logout() async {
    try {
      final storedData = await storage.read(key: 'session_info');
      if (storedData != null) {
        final session = SessionInfo.fromJson(jsonDecode(storedData));
        if (session.anisurgeToken != null) {
          await http.post(
            Uri.parse('$bffBaseUrl/v1/auth/logout'),
            headers: {
              'Content-Type': 'application/json',
              'Authorization': 'Bearer ${session.anisurgeToken}',
            },
          );
        }
      }
    } catch (_) {}
    await storage.delete(key: 'session_info');
  }
}
