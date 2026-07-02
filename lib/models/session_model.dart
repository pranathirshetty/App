class SessionInfo {
  final String token;
  final String? anisurgeToken;

  SessionInfo({
    required this.token,
    this.anisurgeToken,
  });

  factory SessionInfo.fromJson(Map<String, dynamic> json) {
    return SessionInfo(
      token: json['token'] ?? json['projectRToken'] ?? '',
      anisurgeToken: json['anisurgeToken'],
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'token': token,
      'anisurgeToken': anisurgeToken,
    };
  }
}
