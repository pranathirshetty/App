import 'package:flutter/material.dart';
import 'package:kuudere/services/settings_service.dart';
import 'package:kuudere/theme/app_theme.dart';
import 'package:path_provider/path_provider.dart';

class GeneralSettingsPage extends StatefulWidget {
  const GeneralSettingsPage({super.key});

  @override
  State<GeneralSettingsPage> createState() => _GeneralSettingsPageState();
}

class _GeneralSettingsPageState extends State<GeneralSettingsPage> {
  final SettingsService _settingsService = SettingsService();
  bool _isClearingCache = false;

  @override
  void initState() {
    super.initState();
    _settingsService.loadSettings();
  }

  Future<void> _clearCache() async {
    setState(() {
      _isClearingCache = true;
    });

    try {
      final cacheDir = await getTemporaryDirectory();
      if (cacheDir.existsSync()) {
        await cacheDir.delete(recursive: true);
      }
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Cache cleared successfully')),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to clear cache: $e')),
        );
      }
    } finally {
      if (mounted) {
        setState(() {
          _isClearingCache = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        backgroundColor: Colors.black,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back, color: Colors.white),
          onPressed: () => Navigator.of(context).pop(),
        ),
        title: const Text(
          'General Settings',
          style: TextStyle(
            color: Colors.white,
            fontSize: 20,
            fontWeight: FontWeight.bold,
          ),
        ),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          _buildSectionHeader('Player Preferences'),
          _buildSwitchTile(
            title: 'Auto-play Video',
            subtitle: 'Automatically play video when page loads',
            notifier: _settingsService.autoPlay,
            onChanged: _settingsService.setAutoPlay,
          ),
          _buildSwitchTile(
            title: 'Auto-next Episode',
            subtitle: 'Automatically play next episode when current ends',
            notifier: _settingsService.autoNext,
            onChanged: _settingsService.setAutoNext,
          ),
          const SizedBox(height: 24),
          _buildSectionHeader('Content'),
          _buildDropdownTile(
            title: 'Default Audio Language',
            subtitle: 'Preferred audio language for anime',
            notifier: _settingsService.defaultAudio,
            items: const [
              DropdownMenuItem(
                  value: 'sub', child: Text('Subtitled (Japanese)')),
              DropdownMenuItem(value: 'dub', child: Text('Dubbed (English)')),
            ],
            onChanged: (value) {
              if (value != null) {
                _settingsService.setDefaultAudio(value);
              }
            },
          ),
          const SizedBox(height: 24),
          _buildSectionHeader('Storage'),
          _buildActionTile(
            title: 'Clear Image Cache',
            subtitle: 'Free up space by clearing temporary images',
            icon: Icons.delete_outline,
            onTap: _isClearingCache ? null : _clearCache,
            isLoading: _isClearingCache,
          ),
        ],
      ),
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12, left: 4),
      child: Text(
        title,
        style: TextStyle(
          color: AppTheme.primary,
          fontSize: 14,
          fontWeight: FontWeight.bold,
          letterSpacing: 1.0,
        ),
      ),
    );
  }

  Widget _buildSwitchTile({
    required String title,
    required String subtitle,
    required ValueNotifier<bool> notifier,
    required Function(bool) onChanged,
  }) {
    return ValueListenableBuilder<bool>(
      valueListenable: notifier,
      builder: (context, value, child) {
        return Container(
          margin: const EdgeInsets.only(bottom: 12),
          decoration: BoxDecoration(
            color: const Color(0xFF2A2A2A),
            borderRadius: BorderRadius.circular(12),
          ),
          child: SwitchListTile(
            title: Text(title, style: const TextStyle(color: Colors.white)),
            subtitle: Text(subtitle,
                style: TextStyle(color: Colors.grey[400], fontSize: 12)),
            value: value,
            onChanged: onChanged,
            activeThumbColor: AppTheme.primary,
            trackColor: WidgetStateProperty.resolveWith((states) {
              if (states.contains(WidgetState.selected)) {
                return AppTheme.primary.withValues(alpha: 0.5);
              }
              return Colors.grey[700];
            }),
          ),
        );
      },
    );
  }

  Widget _buildDropdownTile({
    required String title,
    required String subtitle,
    required ValueNotifier<String> notifier,
    required List<DropdownMenuItem<String>> items,
    required Function(String?) onChanged,
  }) {
    return ValueListenableBuilder<String>(
      valueListenable: notifier,
      builder: (context, value, child) {
        return Container(
          margin: const EdgeInsets.only(bottom: 12),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: const Color(0xFF2A2A2A),
            borderRadius: BorderRadius.circular(12),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(title,
                        style:
                            const TextStyle(color: Colors.white, fontSize: 16)),
                    const SizedBox(height: 4),
                    Text(subtitle,
                        style:
                            TextStyle(color: Colors.grey[400], fontSize: 12)),
                  ],
                ),
              ),
              DropdownButtonHideUnderline(
                child: DropdownButton<String>(
                  value: value,
                  items: items.map((item) {
                    return DropdownMenuItem<String>(
                      value: item.value,
                      child: Text(
                        (item.child as Text).data!,
                        style: const TextStyle(color: Colors.white),
                      ),
                    );
                  }).toList(),
                  onChanged: onChanged,
                  dropdownColor: const Color(0xFF333333),
                  icon: const Icon(Icons.arrow_drop_down, color: Colors.white),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildActionTile({
    required String title,
    required String subtitle,
    required IconData icon,
    required VoidCallback? onTap,
    bool isLoading = false,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: const Color(0xFF2A2A2A),
        borderRadius: BorderRadius.circular(12),
      ),
      child: ListTile(
        onTap: onTap,
        title: Text(title, style: const TextStyle(color: Colors.white)),
        subtitle: Text(subtitle,
            style: TextStyle(color: Colors.grey[400], fontSize: 12)),
        trailing: isLoading
            ? const SizedBox(
                width: 24,
                height: 24,
                child: CircularProgressIndicator(
                    strokeWidth: 2, color: Colors.white),
              )
            : Icon(icon, color: Colors.white),
      ),
    );
  }
}
