import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/models/anime_models.dart';

// Note: AnimeItem is defined in home_screen.dart, but also search_tab.dart.
// Ideally it should be in a shared model file, but for now I will try to use the one from home_screen.dart or define a compatible one.
// Actually, looking at imports in home_screen.dart: import 'package:kuudere/models/...';
// home_screen.dart defines its own AnimeItem class on line 22. search_tab.dart defines its own on line 781.
// They are slightly different. I should probably refactor this eventually, but to avoid breaking things,
// I will just use the structure expected by HomeScreen since that's where I'm adding the feature.

class AiService {
  final HttpService _httpService = HttpService();

  // Replace these with your actual LLM API details
  final String _llmApiUrl = 'https://ai.megallm.io/v1';
  final String _llmApiKey =
      'sk-mega-4e55d744ff6bf1acac8ee8a9722099abfd5ee67da7d5e89657b355e59a5bc782';

  Future<AiRecommendationResult> getRecommendations(
      Map<String, List<String>> userHistory) async {
    // Check if we have any data at all
    bool hasData = false;
    userHistory.forEach((key, value) {
      if (value.isNotEmpty) hasData = true;
    });

    if (!hasData) {
      return AiRecommendationResult(
          tagline: "Start watching to get recommendations!", items: []);
    }

    // 1. Construct Prompt from structured data
    final StringBuffer historyBuffer = StringBuffer();
    userHistory.forEach((category, titles) {
      if (titles.isNotEmpty) {
        historyBuffer.writeln("$category: ${titles.take(20).join(', ')}");
      }
    });

    final prompt = "My anime list is as follows:\n${historyBuffer.toString()}\n"
        "Recommend 10 similar anime titles that I might like based on this history. "
        "Pay attention to what I am currently watching versus what I dropped (avoid similar to dropped). "
        "Also generate a short, witty tagline (under 10 words) describing my specific taste based on these. "
        "Return the response as a valid JSON object with keys: "
        "'tagline' (string) and 'titles' (list of strings). "
        "Do not include any markdown formatting or explanations, just the raw JSON.";

    String tagline = "Because you watch anime"; // Default fallback
    List<String> recommendedTitles = [];

    // Ensure we hit the correct chat completion endpoint if not specified
    String apiUrl = _llmApiUrl;
    if (!apiUrl.endsWith('/chat/completions')) {
      if (apiUrl.endsWith('/')) {
        apiUrl += 'chat/completions';
      } else {
        apiUrl += '/chat/completions';
      }
    }

    print('Requesting AI recommendations from: $apiUrl');
    print('Sending AI Prompt: $prompt');

    try {
      // 2. Call LLM API
      final response = await http.post(
        Uri.parse(apiUrl),
        headers: {
          'Content-Type': 'application/json',
          'Authorization': 'Bearer $_llmApiKey', // Adjust auth as needed
        },
        body: jsonEncode({
          'model': 'claude-sonnet-4-5-20250929', // or your model
          'messages': [
            {
              'role': 'system',
              'content':
                  'You are an anime recommendation expert. You respond only in valid JSON.'
            },
            {'role': 'user', 'content': prompt}
          ],
          'temperature': 0.7,
        }),
      );

      if (response.statusCode == 200) {
        final data = jsonDecode(response.body);
        String content = data['choices']?[0]?['message']?['content'] ?? '';
        print('AI Response Raw: $content');

        // Clean up markdown if present (e.g., ```json ... ```)
        content = content
            .replaceAll(RegExp(r'^```json\s*'), '')
            .replaceAll(RegExp(r'\s*```$'), '');

        try {
          final jsonContent = jsonDecode(content);
          tagline = jsonContent['tagline'] ?? tagline;
          recommendedTitles = List<String>.from(jsonContent['titles'] ?? []);

          print('Parsed Tagline: $tagline');
          print('Parsed Recommendations: $recommendedTitles');
        } catch (e) {
          print('Error parsing AI JSON response: $e');
        }
      } else {
        print('LLM API Error: ${response.statusCode} ${response.body}');
        return AiRecommendationResult(
            tagline: "Error loading recommendations", items: []);
      }
    } catch (e) {
      print('Error calling LLM: $e');
      return AiRecommendationResult(
          tagline: "Error connecting to AI", items: []);
    }

    // 3. Search for each title in our app's backend to get details (Parallel)
    final results = await Future.wait(
      recommendedTitles.map((title) => _searchTitle(title)),
    );

    // Filter out nulls (failed searches)
    final items = results.whereType<AnimeItem>().toList();

    return AiRecommendationResult(items: items, tagline: tagline);
  }

  Future<AnimeItem?> _searchTitle(String title) async {
    try {
      final searchResponse = await _httpService.get(
        '/search',
        queryParams: {
          'keyword': title,
          'format': 'api',
          'limit': '1', // We only need the top match
        },
      );

      if (searchResponse.statusCode == 200) {
        final data = jsonDecode(searchResponse.body);
        if (data['success'] == true && data['animeData'] != null) {
          final list = data['animeData'] as List;
          if (list.isNotEmpty) {
            final itemData = list.first;
            return AnimeItem.fromJson(itemData);
          }
        }
      }
    } catch (e) {
      print('Error searching for recommendation "$title": $e');
    }
    return null;
  }
}

class AiRecommendationResult {
  final String tagline;
  final List<AnimeItem> items;

  AiRecommendationResult({required this.tagline, required this.items});
}
