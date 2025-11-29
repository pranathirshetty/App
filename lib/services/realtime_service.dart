import 'package:socket_io_client/socket_io_client.dart' as socket_io;

class RealtimeService {
  static final RealtimeService _instance = RealtimeService._internal();
  factory RealtimeService() => _instance;

  socket_io.Socket? socket;
  String? currentRoom;

  RealtimeService._internal() {
    // Socket disabled as per request
    // initializeSocket();
  }

  void initializeSocket() {
    /*
    socket = socket_io.io(
        'https://kuudere.to',
        socket_io.OptionBuilder()
            .setTransports(['websocket'])
            .disableAutoConnect() // Prevents auto connection
            .build());

    socket!.onConnect((_) {
      // print('Connected to server');
    });

    socket!.connect();
    */
  }

  void joinRoom(String newRoom) {
    /*
    if (currentRoom != null) {
      socket!.emit('leave', {'room': currentRoom});
    }

    currentRoom = newRoom;
    socket!.emit('join', {'other_id': currentRoom});
    socket!.emit('get_current_room_count', {'room': currentRoom});

    // print("Joined room: $currentRoom");
    */
  }
}
