import 'dart:ui' show ImageFilter, lerpDouble;
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:kuudere/notification_page.dart';
import 'package:kuudere/profile.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:kuudere/services/http_service.dart';
import 'dart:convert';

enum HeaderStyle {
  transparent, // Transparent with blur effect
  solid, // Solid background
  gradient, // Gradient background (for home screen)
}

class AppHeader extends StatefulWidget implements PreferredSizeWidget {
  final HeaderStyle style;
  final bool showBackButton;
  final String? title;
  final Widget? customTitle;
  final List<Widget>? customActions;
  final double? scrollProgress; // For gradient/transparent style animations
  final Color? backgroundColor;
  final double? elevation;

  const AppHeader({
    super.key,
    this.style = HeaderStyle.solid,
    this.showBackButton = false,
    this.title,
    this.customTitle,
    this.customActions,
    this.scrollProgress,
    this.backgroundColor,
    this.elevation,
  });

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  State<AppHeader> createState() => _AppHeaderState();
}

class _AppHeaderState extends State<AppHeader> {
  final authService = AuthService();
  String notificationCount = '0';
  String? userAvatarUrl;

  @override
  void initState() {
    super.initState();
    fetchNotificationCount();
    fetchUserAvatar();
  }

  Future<void> fetchNotificationCount() async {
    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        final response = await httpService.get('/api/notifications/count',
            requireAuth: true);

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          if (data['success'] ?? true) {
            if (mounted) {
              setState(() {
                notificationCount = data['total']?.toString() ??
                    data['count']?.toString() ??
                    '0';
              });
            }
          }
        }
      }
    } catch (e) {
      // print('Error fetching notification count: $e');
    }
  }

  Future<void> fetchUserAvatar() async {
    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        final response =
            await httpService.get('/api/user/current', requireAuth: true);

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          if (data['success'] == true && data['user'] != null) {
            final userData = data['user'];
            if (mounted) {
              setState(() {
                userAvatarUrl = userData['avatar'];
              });
            }
          }
        }
      }
    } catch (e) {
      // print('Error fetching user avatar: $e');
    }
  }

  Color get _appBarColor {
    if (widget.backgroundColor != null) {
      return widget.backgroundColor!;
    }

    switch (widget.style) {
      case HeaderStyle.transparent:
        return Colors.black.withValues(alpha: 0.2);
      case HeaderStyle.gradient:
        final progress = widget.scrollProgress ?? 0.0;
        return Color.lerp(
          Colors.transparent,
          const Color(0xFF0B0B0B),
          progress,
        )!;
      case HeaderStyle.solid:
        return const Color(0xFF0B0B0B);
    }
  }

  double get _appBarElevation {
    if (widget.elevation != null) {
      return widget.elevation!;
    }

    switch (widget.style) {
      case HeaderStyle.gradient:
        final progress = widget.scrollProgress ?? 0.0;
        return lerpDouble(0, 4, progress)!;
      case HeaderStyle.transparent:
        return 0;
      case HeaderStyle.solid:
        return 4;
    }
  }

  Widget? _buildFlexibleSpace() {
    if (widget.style == HeaderStyle.gradient && widget.scrollProgress != null) {
      final progress = widget.scrollProgress!;
      return AnimatedOpacity(
        duration: const Duration(milliseconds: 200),
        opacity: 1 - progress,
        child: Container(
          decoration: BoxDecoration(
            gradient: LinearGradient(
              begin: Alignment.topCenter,
              end: Alignment.bottomCenter,
              colors: [
                Colors.black.withValues(alpha: 0.8 * (1 - progress)),
                Colors.black.withValues(alpha: 0.7 * (1 - progress)),
                Colors.black.withValues(alpha: 0.5 * (1 - progress)),
                Colors.black.withValues(alpha: 0.4 * (1 - progress)),
                Colors.black.withValues(alpha: 0.2 * (1 - progress)),
                Colors.transparent,
              ],
              stops: const [0.0, 0.2, 0.4, 0.6, 0.8, 1.0],
            ),
          ),
        ),
      );
    }
    return null;
  }

  Widget _buildTitle() {
    if (widget.customTitle != null) {
      return widget.customTitle!;
    }

    if (widget.title != null) {
      return Text(
        widget.title!,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 20,
          fontWeight: FontWeight.bold,
        ),
      );
    }

    // Default: Logo
    return Image.asset(
      'assets/logo-txt.png',
      height: 100,
      fit: BoxFit.contain,
    );
  }

  List<Widget> _buildActions() {
    final actions = <Widget>[];

    // Add custom actions first
    if (widget.customActions != null) {
      actions.addAll(widget.customActions!);
    }

    // Notification bell
    actions.add(
      Padding(
        padding: EdgeInsets.zero,
        child: Stack(
          alignment: Alignment.center,
          children: [
            IconButton(
              icon: const Icon(Icons.notifications_outlined,
                  color: Colors.white, size: 30),
              onPressed: () {
                Navigator.push(
                  context,
                  MaterialPageRoute(
                    builder: (context) => NotificationPage(),
                  ),
                ).then((_) {
                  // Refresh notification count when returning
                  fetchNotificationCount();
                });
              },
            ),
            if (notificationCount != '0')
              Positioned(
                right: 8,
                top: 8,
                child: Container(
                  padding: const EdgeInsets.all(2),
                  decoration: BoxDecoration(
                    color: Colors.red,
                    borderRadius: BorderRadius.circular(10),
                  ),
                  constraints: const BoxConstraints(
                    minWidth: 16,
                    minHeight: 16,
                  ),
                  child: Text(
                    notificationCount,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 10,
                      fontWeight: FontWeight.bold,
                    ),
                    textAlign: TextAlign.center,
                  ),
                ),
              ),
          ],
        ),
      ),
    );

    // User avatar
    actions.add(
      GestureDetector(
        onTap: () async {
          // Navigate to profile page and refresh avatar when returning
          await Navigator.push(
            context,
            MaterialPageRoute(
              builder: (context) => ProfileEditPage(),
            ),
          );
          // Refresh avatar in case user updated their profile picture
          fetchUserAvatar();
          fetchNotificationCount();
        },
        child: Padding(
          padding: const EdgeInsets.all(8.0),
          child: userAvatarUrl != null &&
                  userAvatarUrl!.isNotEmpty &&
                  userAvatarUrl != '/placeholder.svg?height=32&width=32'
              ? CircleAvatar(
                  radius: 16,
                  backgroundImage: NetworkImage(
                    userAvatarUrl!.startsWith('http')
                        ? userAvatarUrl!
                        : 'https://kuudere.to$userAvatarUrl',
                  ),
                  onBackgroundImageError: (exception, stackTrace) {
                    // Fallback to default icon if image fails to load
                    if (mounted) {
                      setState(() {
                        userAvatarUrl = null;
                      });
                    }
                  },
                  child: userAvatarUrl == null
                      ? const Icon(Icons.account_circle,
                          color: Colors.white, size: 30)
                      : null,
                )
              : const Icon(Icons.account_circle, color: Colors.white, size: 30),
        ),
      ),
    );

    return actions;
  }

  @override
  Widget build(BuildContext context) {
    Widget appBar = AppBar(
      backgroundColor: _appBarColor,
      elevation: _appBarElevation,
      systemOverlayStyle: SystemUiOverlayStyle.light.copyWith(
        statusBarColor: widget.style == HeaderStyle.gradient &&
                widget.scrollProgress != null
            ? Color.lerp(
                Colors.transparent,
                const Color(0xFF0B0B0B),
                widget.scrollProgress!,
              )
            : _appBarColor,
      ),
      flexibleSpace: _buildFlexibleSpace(),
      leading: widget.showBackButton
          ? IconButton(
              icon: const Icon(Icons.arrow_back, color: Colors.white),
              onPressed: () => Navigator.of(context).pop(),
            )
          : null,
      title: _buildTitle(),
      actions: _buildActions(),
    );

    // Wrap with blur effect for transparent style
    if (widget.style == HeaderStyle.transparent) {
      return ClipRRect(
        child: BackdropFilter(
          filter: ImageFilter.blur(sigmaX: 10, sigmaY: 10),
          child: appBar,
        ),
      );
    }

    return appBar;
  }
}
