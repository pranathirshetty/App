import 'dart:io';
import 'package:flutter/material.dart';
import 'package:http/http.dart' as http;
import 'package:path_provider/path_provider.dart';

/// Utility class for parsing and modifying subtitle files
class SubtitleUtils {
  /// Download and convert a subtitle to a styled ASS file.
  /// For SRT/VTT this produces a clean, visually polished result.
  /// For ASS this returns the original (already styled).
  /// [subtitleUrl] - URL or file:// URI of the subtitle
  /// [delaySeconds] - Optional delay to apply
  /// Returns a file:// URI to the local ASS file, or null on failure.
  static Future<String?> prepareSubtitle(
      String subtitleUrl, double delaySeconds) async {
    try {
      String content;
      if (subtitleUrl.startsWith('file://')) {
        final file = File(Uri.parse(subtitleUrl).toFilePath());
        content = await file.readAsString();
      } else {
        final response = await http.get(Uri.parse(subtitleUrl));
        if (response.statusCode != 200) {
          debugPrint('Failed to download subtitle: ${response.statusCode}');
          return null;
        }
        content = response.body;
      }

      final format = _detectFormat(subtitleUrl, content);

      String assContent;
      switch (format) {
        case SubtitleFormat.ass:
          // Already ASS — just apply delay if needed, return as-is otherwise
          assContent = delaySeconds != 0
              ? _applyAssDelay(content, delaySeconds)
              : content;
          break;
        case SubtitleFormat.srt:
          final delayed = delaySeconds != 0
              ? _applySrtDelay(content, delaySeconds)
              : content;
          assContent = _srtToAss(delayed);
          break;
        case SubtitleFormat.vtt:
          final delayed = delaySeconds != 0
              ? _applyVttDelay(content, delaySeconds)
              : content;
          assContent = _vttToAss(delayed);
          break;
        default:
          debugPrint('Unknown subtitle format, cannot prepare');
          return null;
      }

      final tempDir = await getTemporaryDirectory();
      final timestamp = DateTime.now().millisecondsSinceEpoch;
      final tempFile = File('${tempDir.path}/subtitle_$timestamp.ass');
      await tempFile.writeAsString(assContent);

      final fileUri = Uri.file(tempFile.path).toString();
      debugPrint('Prepared subtitle at: $fileUri (format: $format)');
      return fileUri;
    } catch (e) {
      debugPrint('Error preparing subtitle: $e');
      return null;
    }
  }

  /// Apply a delay offset to a subtitle file and return the URI to the modified file.
  /// Non-ASS formats are converted to styled ASS for consistent rendering.
  static Future<String?> applyDelay(
      String subtitleUrl, double delaySeconds) async {
    return prepareSubtitle(subtitleUrl, delaySeconds);
  }

  // ─── ASS header with clean anime-style styling ───────────────────────────

  static String _assHeader() {
    return '''[Script Info]
ScriptType: v4.00+
PlayResX: 1920
PlayResY: 1080
ScaledBorderAndShadow: yes

[V4+ Styles]
Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
Style: Default,Arial,52,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,2.5,1.5,2,80,80,40,1

[Events]
Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
''';
  }

  // ─── SRT → ASS converter ─────────────────────────────────────────────────

  static String _srtToAss(String srtContent) {
    final buffer = StringBuffer();
    buffer.write(_assHeader());

    final lines = srtContent.split('\n');
    int i = 0;
    while (i < lines.length) {
      final line = lines[i].trim();

      // Skip sequence numbers (lines that are just digits)
      if (RegExp(r'^\d+$').hasMatch(line)) {
        i++;
        // Next line should be the timing
        if (i < lines.length) {
          final timeLine = lines[i].trim();
          final timeMatch = RegExp(
                  r'(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})')
              .firstMatch(timeLine);
          if (timeMatch != null) {
            final start = _srtTimeToAss(timeMatch.group(1)!);
            final end = _srtTimeToAss(timeMatch.group(2)!);
            i++;

            // Collect text lines until empty line
            final textLines = <String>[];
            while (i < lines.length && lines[i].trim().isNotEmpty) {
              textLines.add(_stripHtmlTags(lines[i].trim()));
              i++;
            }

            if (textLines.isNotEmpty) {
              final text = textLines.join('\\N');
              buffer.writeln('Dialogue: 0,$start,$end,Default,,0,0,0,,${text}');
            }
          }
        }
      } else {
        i++;
      }
    }

    return buffer.toString();
  }

  // ─── VTT → ASS converter ─────────────────────────────────────────────────

  static String _vttToAss(String vttContent) {
    final buffer = StringBuffer();
    buffer.write(_assHeader());

    final lines = vttContent.split('\n');
    int i = 0;

    // Skip WEBVTT header and NOTE blocks
    while (i < lines.length) {
      final line = lines[i].trim();
      if (line.startsWith('WEBVTT') ||
          line.startsWith('NOTE') ||
          line.startsWith('STYLE') ||
          line.startsWith('REGION')) {
        // Skip until blank line
        while (i < lines.length && lines[i].trim().isNotEmpty) {
          i++;
        }
        i++;
        continue;
      }

      // Check for timing line (may be preceded by cue identifier)
      final timingRegex = RegExp(
          r'(\d{2}:\d{2}:\d{2}\.\d{3}|\d{2}:\d{2}\.\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2}\.\d{3}|\d{2}:\d{2}\.\d{3})');

      bool foundTiming = false;
      // Current line or next (skip optional cue id)
      for (int attempt = 0;
          attempt < 2 && i + attempt < lines.length;
          attempt++) {
        final candidate = lines[i + attempt].trim();
        final timeMatch = timingRegex.firstMatch(candidate);
        if (timeMatch != null) {
          i += attempt + 1; // skip to after timing line
          final start = _vttTimeToAss(timeMatch.group(1)!);
          final end = _vttTimeToAss(timeMatch.group(2)!);

          // Collect text
          final textLines = <String>[];
          while (i < lines.length && lines[i].trim().isNotEmpty) {
            textLines.add(_stripHtmlTags(lines[i].trim()));
            i++;
          }

          if (textLines.isNotEmpty) {
            final text = textLines.join('\\N');
            buffer.writeln('Dialogue: 0,$start,$end,Default,,0,0,0,,${text}');
          }
          foundTiming = true;
          break;
        }
      }

      if (!foundTiming) {
        i++;
      }
    }

    return buffer.toString();
  }

  // ─── Time format converters ───────────────────────────────────────────────

  /// SRT time "00:01:23,456" → ASS time "0:01:23.46"
  static String _srtTimeToAss(String srtTime) {
    // SRT: HH:MM:SS,mmm  →  ASS: H:MM:SS.cc
    final parts = srtTime.split(',');
    final hms = parts[0].split(':');
    final ms = int.parse(parts[1]);
    final cs = (ms / 10).floor();
    return '${int.parse(hms[0])}:${hms[1]}:${hms[2]}.${cs.toString().padLeft(2, '0')}';
  }

  /// VTT time "00:01:23.456" or "01:23.456" → ASS time "0:01:23.46"
  static String _vttTimeToAss(String vttTime) {
    final parts = vttTime.split('.');
    final hmsStr = parts[0];
    final ms = int.parse(parts[1]);
    final cs = (ms / 10).floor();

    final hmsParts = hmsStr.split(':');
    int hours, minutes, seconds;
    if (hmsParts.length == 3) {
      hours = int.parse(hmsParts[0]);
      minutes = int.parse(hmsParts[1]);
      seconds = int.parse(hmsParts[2]);
    } else {
      // MM:SS format
      hours = 0;
      minutes = int.parse(hmsParts[0]);
      seconds = int.parse(hmsParts[1]);
    }
    return '$hours:${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}.${cs.toString().padLeft(2, '0')}';
  }

  /// Strip basic HTML tags from subtitle text (e.g. <i>, <b>, <font ...>)
  static String _stripHtmlTags(String text) {
    // Convert <i>...</i> to ASS italic override tags
    text = text.replaceAllMapped(RegExp(r'<i>(.*?)</i>', dotAll: true),
        (m) => '{\\i1}${m.group(1)}{\\i0}');
    text = text.replaceAllMapped(RegExp(r'<b>(.*?)</b>', dotAll: true),
        (m) => '{\\b1}${m.group(1)}{\\b0}');
    // Strip remaining tags
    text = text.replaceAll(RegExp(r'<[^>]+>'), '');
    // Unescape HTML entities
    text = text
        .replaceAll('&amp;', '&')
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll('&nbsp;', ' ')
        .replaceAll('&#39;', "'")
        .replaceAll('&quot;', '"');
    return text;
  }

  static SubtitleFormat _detectFormat(String url, String content) {
    final lowerUrl = url.toLowerCase().split('?').first;
    if (lowerUrl.endsWith('.ass') ||
        lowerUrl.endsWith('.ssa') ||
        url.toLowerCase().contains('format=ass')) {
      return SubtitleFormat.ass;
    }
    if (lowerUrl.endsWith('.vtt') || url.toLowerCase().contains('format=vtt')) {
      return SubtitleFormat.vtt;
    }
    if (lowerUrl.endsWith('.srt') || url.toLowerCase().contains('format=srt')) {
      return SubtitleFormat.srt;
    }

    // Auto-detect from content
    if (content.contains('[Script Info]')) {
      return SubtitleFormat.ass;
    }
    if (content.trimLeft().startsWith('WEBVTT')) {
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

        result.add('${_msToVttTime(newStartMs)} --> ${_msToVttTime(newEndMs)}');
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

          result
              .add('${_msToVttTime(newStartMs)} --> ${_msToVttTime(newEndMs)}');
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

        result.add(
            '$prefix${_csToAssTime(newStartCs)},${_csToAssTime(newEndCs)},$suffix');
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
