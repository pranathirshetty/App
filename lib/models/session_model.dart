class SessionInfo {
  /// Project-R session token (project_r_ prefix) — from api.md
  final String projectRToken;

  /// Anisurge BFF JWT for versioned API routes
  final String? anisurgeToken;

  /// User profile from BFF
  final String? externalUserId;
  final String? username;
  final String? email;
  final String? avatarUrl;
  final String? customPfpUrl;
  final String? displayName;
  final int coins;
  final bool isStaff;
  final bool reanimeConnected;
  final String? reanimeUsername;
  final bool isPremium;
  final String? premiumPlan;
  final String? premiumExpiresAt;

  SessionInfo({
    required this.projectRToken,
    this.anisurgeToken,
    this.externalUserId,
    this.username,
    this.email,
    this.avatarUrl,
    this.customPfpUrl,
    this.displayName,
    this.coins = 0,
    this.isStaff = false,
    this.reanimeConnected = false,
    this.reanimeUsername,
    this.isPremium = false,
    this.premiumPlan,
    this.premiumExpiresAt,
  });

  String get effectiveAvatar => customPfpUrl ?? avatarUrl ?? '';

  bool get hasAnisurgeToken =>
      anisurgeToken != null && anisurgeToken!.isNotEmpty;

  factory SessionInfo.fromJson(Map<String, dynamic> json) {
    return SessionInfo(
      projectRToken: json['projectRToken'] ?? json['token'] ?? '',
      anisurgeToken: json['anisurgeToken'],
      externalUserId: json['externalUserId'],
      username: json['username'],
      email: json['email'],
      avatarUrl: json['avatarUrl'],
      customPfpUrl: json['customPfpUrl'],
      displayName: json['displayName'],
      coins: json['coins'] ?? 0,
      isStaff: json['isStaff'] ?? false,
      reanimeConnected: json['reanimeConnected'] ?? false,
      reanimeUsername: json['reanimeUsername'],
      isPremium: json['isPremium'] ?? false,
      premiumPlan: json['premiumPlan'],
      premiumExpiresAt: json['premiumExpiresAt'],
    );
  }

  Map<String, dynamic> toJson() => {
        'projectRToken': projectRToken,
        'anisurgeToken': anisurgeToken,
        'externalUserId': externalUserId,
        'username': username,
        'email': email,
        'avatarUrl': avatarUrl,
        'customPfpUrl': customPfpUrl,
        'displayName': displayName,
        'coins': coins,
        'isStaff': isStaff,
        'reanimeConnected': reanimeConnected,
        'reanimeUsername': reanimeUsername,
        'isPremium': isPremium,
        'premiumPlan': premiumPlan,
        'premiumExpiresAt': premiumExpiresAt,
      };

  /// Parse BFF auth response: { projectRToken, anisurgeToken, anisurgeUserId, user: { ... } }
  factory SessionInfo.fromBffAuth(Map<String, dynamic> json) {
    final user = json['user'] as Map<String, dynamic>?;
    return SessionInfo(
      projectRToken: json['projectRToken'] ?? json['token'] ?? '',
      anisurgeToken: json['anisurgeToken'],
      externalUserId: json['anisurgeUserId'] ?? user?['externalUserId'],
      username: user?['username'],
      email: user?['email'],
      avatarUrl: user?['avatarUrl'],
      customPfpUrl: user?['customPfpUrl'],
      displayName: user?['displayName'],
      coins: user?['coins'] ?? 0,
      isStaff: user?['isStaff'] ?? false,
      reanimeConnected: user?['reanimeConnected'] ?? false,
      reanimeUsername: user?['reanimeUsername'],
      isPremium: user?['isPremium'] ?? false,
      premiumPlan: user?['premiumPlan'],
      premiumExpiresAt: user?['premiumExpiresAt'],
    );
  }
}
