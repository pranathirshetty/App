import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/session_model.dart';

class AuthService {
  static const String baseUrl = 'https://kuudere.to';
  final storage = const FlutterSecureStorage();

  Future<SessionInfo?> login(String email, String password) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/login'),
        headers: {
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'email': email,
          'password': password,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success']) {
          // Map SvelteKit session format to Flutter format
          final sessionData = data['session'];
          final sessionInfo = SessionInfo(
            userId: sessionData['userId'],
            session: sessionData['sessionSecret'] ?? sessionData['session'], // Support both formats
            expire: sessionData['expire'],
            sessionId: sessionData['sessionId'],
          );
          await storage.write(key: 'session_info', value: jsonEncode(sessionInfo.toJson()));
          return sessionInfo;
        }
      }
      throw Exception(jsonDecode(response.body)['message']);
    } catch (e) {
      rethrow;
    }
  }

  Future<SessionInfo?> register(String email, String password, String username) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/api/auth/register'),
        headers: {
          'Content-Type': 'application/json',
        },
        body: jsonEncode({
          'email': email,
          'password': password,
          'username': username
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        if (data['success']) {
          // Map SvelteKit session format to Flutter format
          final sessionData = data['session'];
          final sessionInfo = SessionInfo(
            userId: sessionData['userId'],
            session: sessionData['sessionSecret'] ?? sessionData['session'], // Support both formats
            expire: sessionData['expire'],
            sessionId: sessionData['sessionId'],
          );
          await storage.write(key: 'session_info', value: jsonEncode(sessionInfo.toJson()));
          return sessionInfo;
        }
      }
      throw Exception(jsonDecode(response.body)['message']);
    } catch (e) {
      rethrow;
    }
  }

  // Add method to get stored session info
  Future<SessionInfo?> getStoredSession() async {
    try {
      final storedData = await storage.read(key: 'session_info');
      if (storedData != null) {
        return SessionInfo.fromJson(jsonDecode(storedData));
      }
      return null;
    } catch (e) {
      return null;
    }
  }

  // Add method to check if session is expired
  bool isSessionExpired(SessionInfo session) {
    final expireDate = DateTime.parse(session.expire);
    return DateTime.now().isAfter(expireDate);
  }
}