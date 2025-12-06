import 'package:fvp/fvp.dart' as fvp_lib;
import 'package:fvp/mdk.dart' as mdk_lib;

export 'package:fvp/fvp.dart';

class FvpBridge {
  static void registerWith({Map<String, dynamic>? options}) {
    fvp_lib.registerWith(options: options);
  }

  static void setGlobalOption(String key, String value) {
    mdk_lib.setGlobalOption(key, value);
  }
}
