import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import '../models/session_model.dart';
import 'http_service.dart';

class AuthService {
  static const String v1Base = 'https://db.anisurge.qzz.io/v1';

  final HttpService httpService = HttpService();
  final storage = const FlutterSecureStorage();

  Future<SessionInfo> login(String identifier, String password) async {
    final response = await http.post(
      Uri.parse('$v1Base/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'identifier': identifier, 'password': password}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      final session = SessionInfo.fromBffAuth(data);
      await httpService.saveSession(session);
      return session;
    }
    throw Exception(_parseErrorBody(response));
  }

  Future<SessionInfo> register(
      String username, String email, String password) async {
    final response = await http.post(
      Uri.parse('$v1Base/auth/signup'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'username': username, 'email': email, 'password': password}),
    );

    if (response.statusCode == 200 || response.statusCode == 201) {
      final data = jsonDecode(response.body);
      final session = SessionInfo.fromBffAuth(data);
      await httpService.saveSession(session);
      return session;
    }
    throw Exception(_parseErrorBody(response));
  }

  Future<void> logout() async {
    await httpService.clearSession();
  }

  Future<SessionInfo?> getStoredSession() async {
    return await httpService.getStoredSession();
  }

  Future<SessionInfo?> refreshProfile(SessionInfo session) async {
    try {
      if (!session.hasAnisurgeToken) return session;
      final response = await httpService.getBff('/me', session: session);
      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        final user = data['user'] as Map<String, dynamic>? ?? {};
        final updated = SessionInfo(
          projectRToken: session.projectRToken,
          anisurgeToken: session.anisurgeToken,
          externalUserId: user['externalUserId'] ?? session.externalUserId,
          username: user['username'] ?? session.username,
          email: user['email'] ?? session.email,
          avatarUrl: user['avatarUrl'] ?? session.avatarUrl,
          customPfpUrl: user['customPfpUrl'] ?? session.customPfpUrl,
          displayName: user['displayName'] ?? session.displayName,
          coins: user['coins'] ?? session.coins,
          isStaff: user['isStaff'] ?? session.isStaff,
          reanimeConnected: user['reanimeConnected'] ?? session.reanimeConnected,
          reanimeUsername: user['reanimeUsername'] ?? session.reanimeUsername,
          isPremium: user['isPremium'] ?? session.isPremium,
          premiumPlan: user['premiumPlan'] ?? session.premiumPlan,
          premiumExpiresAt: user['premiumExpiresAt'] ?? session.premiumExpiresAt,
        );
        await httpService.saveSession(updated);
        return updated;
      }
      return session;
    } catch (_) {
      return session;
    }
  }

  String _parseErrorBody(http.Response response) {
    try {
      final data = jsonDecode(response.body);
      return data['error'] ?? data['message'] ?? 'Request failed (${response.statusCode})';
    } catch (_) {
      return 'Request failed (${response.statusCode})';
    }
  }
}
