import 'dart:io';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';

/// Utility class for parsing and modifying subtitle files
class SubtitleUtils {
  /// Apply a delay offset to a subtitle file and return the URI to the modified file
  /// [subtitleUrl] - URL of the original subtitle file
  /// [delaySeconds] - Delay in seconds (positive = subtitles appear later, negative = earlier)
  /// Returns URI to the modified subtitle file (file:// URI for local files), or null if failed
  static Future<String?> applyDelay(
      String subtitleUrl, double delaySeconds) async {
    if (delaySeconds == 0) {
      // No delay needed, return original URL
      return subtitleUrl;
    }

    try {
      // Download subtitle file
      final response = await http.get(Uri.parse(subtitleUrl));
      if (response.statusCode != 200) {
        debugPrint('Failed to download subtitle: ${response.statusCode}');
        return null;
      }

      String content = response.body;
      final format = _detectFormat(subtitleUrl, content);

      String modifiedContent;
      switch (format) {
        case SubtitleFormat.srt:
          modifiedContent = _applySrtDelay(content, delaySeconds);
          break;
        case SubtitleFormat.vtt:
          modifiedContent = _applyVttDelay(content, delaySeconds);
          break;
        case SubtitleFormat.ass:
          modifiedContent = _applyAssDelay(content, delaySeconds);
          break;
        default:
          debugPrint('Unknown subtitle format, cannot apply delay');
          return null;
      }

      // Save to temp file
      final tempDir = await getTemporaryDirectory();
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final ext = format == SubtitleFormat.ass
          ? 'ass'
          : format == SubtitleFormat.vtt
              ? 'vtt'
              : 'srt';
      final tempFile = File('${tempDir.path}/subtitle_delayed_$timestamp.$ext');
      await tempFile.writeAsString(modifiedContent);

      // Return as file:// URI for cross-platform compatibility
      final fileUri = Uri.file(tempFile.path).toString();
      debugPrint('Created delayed subtitle at: $fileUri');
      return fileUri;
    } catch (e) {
      debugPrint('Error applying subtitle delay: $e');
      return null;
    }
  }

  static SubtitleFormat _detectFormat(String url, String content) {
    final lowerUrl = url.toLowerCase();
    if (lowerUrl.contains('.ass') || lowerUrl.contains('format=ass')) {
      return SubtitleFormat.ass;
    }
    if (lowerUrl.contains('.vtt') || lowerUrl.contains('format=vtt')) {
      return SubtitleFormat.vtt;
    }
    if (lowerUrl.contains('.srt') || lowerUrl.contains('format=srt')) {
      return SubtitleFormat.srt;
    }

    // Auto-detect from content
    if (content.contains('[Script Info]') || content.contains('Format:')) {
      return SubtitleFormat.ass;
    }
    if (content.startsWith('WEBVTT')) {
      return SubtitleFormat.vtt;
    }
    // Default to SRT
    return SubtitleFormat.srt;
  }

  /// Apply delay to SRT format subtitles
  static String _applySrtDelay(String content, double delaySeconds) {
    final lines = content.split('\n');
    final result = <String>[];
    final timeRegex = RegExp(
        r'(\d{2}):(\d{2}):(\d{2}),(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2}),(\d{3})');

    for (final line in lines) {
      final match = timeRegex.firstMatch(line);
      if (match != null) {
        final startMs = _parseTimeToMs(
          int.parse(match.group(1)!),
          int.parse(match.group(2)!),
          int.parse(match.group(3)!),
          int.parse(match.group(4)!),
        );
        final endMs = _parseTimeToMs(
          int.parse(match.group(5)!),
          int.parse(match.group(6)!),
          int.parse(match.group(7)!),
          int.parse(match.group(8)!),
        );

        final delayMs = (delaySeconds * 1000).round();
        final newStartMs = (startMs + delayMs).clamp(0, 999999999);
        final newEndMs = (endMs + delayMs).clamp(0, 999999999);

        final newLine =
            '${_msToSrtTime(newStartMs)} --> ${_msToSrtTime(newEndMs)}';
        result.add(newLine);
      } else {
        result.add(line);
      }
    }

    return result.join('\n');
  }

  /// Apply delay to VTT format subtitles
  static String _applyVttDelay(String content, double delaySeconds) {
    final lines = content.split('\n');
    final result = <String>[];
    final timeRegex = RegExp(
        r'(\d{2}):(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2}):(\d{2})\.(\d{3})');
    // Also handle short format without hours
    final shortTimeRegex =
        RegExp(r'(\d{2}):(\d{2})\.(\d{3})\s*-->\s*(\d{2}):(\d{2})\.(\d{3})');

    for (final line in lines) {
      var match = timeRegex.firstMatch(line);
      if (match != null) {
        final startMs = _parseTimeToMs(
          int.parse(match.group(1)!),
          int.parse(match.group(2)!),
          int.parse(match.group(3)!),
          int.parse(match.group(4)!),
        );
        final endMs = _parseTimeToMs(
          int.parse(match.group(5)!),
          int.parse(match.group(6)!),
          int.parse(match.group(7)!),
          int.parse(match.group(8)!),
        );

        final delayMs = (delaySeconds * 1000).round();
        final newStartMs = (startMs + delayMs).clamp(0, 999999999);
        final newEndMs = (endMs + delayMs).clamp(0, 999999999);

        final newLine =
            '${_msToVttTime(newStartMs)} --> ${_msToVttTime(newEndMs)}';
        result.add(newLine);
      } else {
        match = shortTimeRegex.firstMatch(line);
        if (match != null) {
          final startMs = _parseTimeToMs(
            0,
            int.parse(match.group(1)!),
            int.parse(match.group(2)!),
            int.parse(match.group(3)!),
          );
          final endMs = _parseTimeToMs(
            0,
            int.parse(match.group(4)!),
            int.parse(match.group(5)!),
            int.parse(match.group(6)!),
          );

          final delayMs = (delaySeconds * 1000).round();
          final newStartMs = (startMs + delayMs).clamp(0, 999999999);
          final newEndMs = (endMs + delayMs).clamp(0, 999999999);

          final newLine =
              '${_msToVttTime(newStartMs)} --> ${_msToVttTime(newEndMs)}';
          result.add(newLine);
        } else {
          result.add(line);
        }
      }
    }

    return result.join('\n');
  }

  /// Apply delay to ASS/SSA format subtitles
  static String _applyAssDelay(String content, double delaySeconds) {
    final lines = content.split('\n');
    final result = <String>[];

    // ASS dialogue format: Dialogue: Layer,Start,End,Style,Name,MarginL,MarginR,MarginV,Effect,Text
    // Time format: H:MM:SS.cc (centiseconds)
    final dialogueRegex = RegExp(
        r'^(Dialogue:\s*\d+,)(\d+):(\d{2}):(\d{2})\.(\d{2}),(\d+):(\d{2}):(\d{2})\.(\d{2}),(.*)$');

    for (final line in lines) {
      final match = dialogueRegex.firstMatch(line);
      if (match != null) {
        final prefix = match.group(1)!;
        final startCs = _parseAssTimeToCs(
          int.parse(match.group(2)!),
          int.parse(match.group(3)!),
          int.parse(match.group(4)!),
          int.parse(match.group(5)!),
        );
        final endCs = _parseAssTimeToCs(
          int.parse(match.group(6)!),
          int.parse(match.group(7)!),
          int.parse(match.group(8)!),
          int.parse(match.group(9)!),
        );
        final suffix = match.group(10)!;

        final delayCs = (delaySeconds * 100).round();
        final newStartCs = (startCs + delayCs).clamp(0, 999999999);
        final newEndCs = (endCs + delayCs).clamp(0, 999999999);

        final newLine =
            '$prefix${_csToAssTime(newStartCs)},${_csToAssTime(newEndCs)},$suffix';
        result.add(newLine);
      } else {
        result.add(line);
      }
    }

    return result.join('\n');
  }

  static int _parseTimeToMs(int hours, int minutes, int seconds, int ms) {
    return hours * 3600000 + minutes * 60000 + seconds * 1000 + ms;
  }

  static int _parseAssTimeToCs(int hours, int minutes, int seconds, int cs) {
    return hours * 360000 + minutes * 6000 + seconds * 100 + cs;
  }

  static String _msToSrtTime(int ms) {
    final hours = ms ~/ 3600000;
    ms %= 3600000;
    final minutes = ms ~/ 60000;
    ms %= 60000;
    final seconds = ms ~/ 1000;
    final milliseconds = ms % 1000;
    return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')},${milliseconds.toString().padLeft(3, '0')}';
  }

  static String _msToVttTime(int ms) {
    final hours = ms ~/ 3600000;
    ms %= 3600000;
    final minutes = ms ~/ 60000;
    ms %= 60000;
    final seconds = ms ~/ 1000;
    final milliseconds = ms % 1000;
    return '${hours.toString().padLeft(2, '0')}:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}.${milliseconds.toString().padLeft(3, '0')}';
  }

  static String _csToAssTime(int cs) {
    final hours = cs ~/ 360000;
    cs %= 360000;
    final minutes = cs ~/ 6000;
    cs %= 6000;
    final seconds = cs ~/ 100;
    final centiseconds = cs % 100;
    return '$hours:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}.${centiseconds.toString().padLeft(2, '0')}';
  }
}

enum SubtitleFormat {
  srt,
  vtt,
  ass,
  unknown,
}
