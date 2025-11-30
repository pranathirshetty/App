// import 'package:flutter/material.dart';
import 'package:flutter_local_notifications/flutter_local_notifications.dart';
import 'package:kuudere/services/auth_service.dart';
import 'package:socket_io_client/socket_io_client.dart' as socket_io;
// import 'package:http/http.dart' as http;
// import 'package:path_provider/path_provider.dart';
import 'package:flutter/foundation.dart';
// import 'dart:io';

class NotificationService {
  static final NotificationService _instance = NotificationService._internal();
  factory NotificationService() => _instance;
  NotificationService._internal();

  final FlutterLocalNotificationsPlugin flutterLocalNotificationsPlugin =
      FlutterLocalNotificationsPlugin();
  late socket_io.Socket socket;

  final authService = AuthService();

  Future<void> initialize() async {
    await _initializeNotifications();
    _connectToSocket();
  }

  Future<void> _initializeNotifications() async {
    try {
      const AndroidInitializationSettings androidSettings =
          AndroidInitializationSettings('@mipmap/ic_launcher');
      final DarwinInitializationSettings iosSettings =
          DarwinInitializationSettings(
        requestAlertPermission: true,
        requestBadgePermission: true,
        requestSoundPermission: true,
      );
      final InitializationSettings settings = InitializationSettings(
        android: androidSettings,
        iOS: iosSettings,
      );
      await flutterLocalNotificationsPlugin.initialize(
        settings,
        onDidReceiveNotificationResponse: (NotificationResponse response) {
          debugPrint('Notification clicked: ${response.payload}');
        },
      );

      await flutterLocalNotificationsPlugin
          .resolvePlatformSpecificImplementation<
              AndroidFlutterLocalNotificationsPlugin>()
          ?.requestNotificationsPermission();
    } catch (e) {
      debugPrint('Error initializing notifications: $e');
    }
  }

  Future<String?> _downloadAndSaveImage(String imageUrl) async {
    if (kIsWeb) return null;
    /*
    try {
      final response = await http.get(Uri.parse(imageUrl));
      if (response.statusCode != 200) return null;

      final directory = await getTemporaryDirectory();
      final imagePath =
          '${directory.path}/notification_${DateTime.now().millisecondsSinceEpoch}.jpg';

      File imageFile = File(imagePath);
      await imageFile.writeAsBytes(response.bodyBytes);

      return imagePath;
    } catch (e) {
      debugPrint('Error downloading image: $e');
      return null;
    }
    */
    return null;
  }

  Future<void> _connectToSocket() async {
    /*
    try {
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        socket = socket_io.io('https://kuudere.to', <String, dynamic>{
          'transports': ['websocket'],
          'autoConnect': true,
          'query': {'user_id': sessionInfo.userId},
          'reconnection': true,
          'reconnectionDelay': 1000,
          'reconnectionAttempts': 5,
        });
      }

      socket.onConnect((_) {
        debugPrint('✅ Connected to WebSocket');
      });

      socket.on('new_notification', (data) async {
        debugPrint('🔔 New Notification: $data');
        await showNotification(
            data['message'], data['image_url'], data['title']);
      });

      socket.onDisconnect((_) => debugPrint('❌ Disconnected'));
      socket.onError((err) => debugPrint('❌ Error: $err'));
      socket.onConnectError((err) => debugPrint('❌ Connect Error: $err'));
      socket.connect();
    } catch (e) {
      debugPrint('Error connecting to socket: $e');
    }
    */
  }

  Future<void> showNotification(
      String message, String? imageUrl, String? title) async {
    try {
      String? bigPicturePath;
      if (imageUrl != null) {
        bigPicturePath = await _downloadAndSaveImage(imageUrl);
      }

      final AndroidNotificationDetails androidDetails =
          AndroidNotificationDetails(
        'channel_id',
        'Channel Name',
        channelDescription: 'Notification Channel',
        importance: Importance.high,
        priority: Priority.high,
        playSound: true,
        enableVibration: true,
        styleInformation: bigPicturePath != null
            ? BigPictureStyleInformation(
                FilePathAndroidBitmap(bigPicturePath),
                hideExpandedLargeIcon: true,
              )
            : null,
      );

      final NotificationDetails platformDetails = NotificationDetails(
        android: androidDetails,
        iOS: const DarwinNotificationDetails(
          presentAlert: true,
          presentBadge: true,
          presentSound: true,
        ),
      );

      await flutterLocalNotificationsPlugin.show(
        DateTime.now().millisecond,
        title ?? 'Notification',
        message,
        platformDetails,
        payload: message,
      );
    } catch (e) {
      debugPrint('Error showing notification: $e');
    }
  }

  void dispose() {
    socket.dispose();
  }
}
