import 'package:flutter/material.dart';

/// App font styles - uses bundled Poppins font
/// This replaces GoogleFonts.poppins() to avoid AssetManifest.json errors
class AppFonts {
  static const String fontFamily = 'Poppins';

  static TextStyle poppins({
    double? fontSize,
    FontWeight? fontWeight,
    Color? color,
    double? height,
    double? letterSpacing,
    TextDecoration? decoration,
    List<Shadow>? shadows,
  }) {
    return TextStyle(
      fontFamily: fontFamily,
      fontSize: fontSize,
      fontWeight: fontWeight,
      color: color,
      height: height,
      letterSpacing: letterSpacing,
      decoration: decoration,
      shadows: shadows,
    );
  }
}
