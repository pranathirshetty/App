import 'package:flutter/material.dart';

class ErrorView extends StatelessWidget {
  final String title;
  final String message;
  final VoidCallback? onRetry;
  final IconData icon;

  const ErrorView({
    super.key,
    this.title = 'Oops! Something went wrong',
    this.message =
        'Unable to load content. Please check your connection and try again.',
    this.onRetry,
    this.icon = Icons.cloud_off_rounded,
  });

  // Factory constructors for common error types
  factory ErrorView.network({
    VoidCallback? onRetry,
  }) {
    return ErrorView(
      title: 'No Internet Connection',
      message: 'Please check your internet connection and try again.',
      icon: Icons.wifi_off_rounded,
      onRetry: onRetry,
    );
  }

  factory ErrorView.notFound({
    VoidCallback? onRetry,
  }) {
    return ErrorView(
      title: 'Not Found',
      message: 'The content you\'re looking for doesn\'t exist.',
      icon: Icons.search_off_rounded,
      onRetry: onRetry,
    );
  }

  factory ErrorView.serverError({
    VoidCallback? onRetry,
  }) {
    return ErrorView(
      title: 'Server Error',
      message: 'Our servers are having trouble. Please try again later.',
      icon: Icons.dns_rounded,
      onRetry: onRetry,
    );
  }

  factory ErrorView.timeout({
    VoidCallback? onRetry,
  }) {
    return ErrorView(
      title: 'Request Timeout',
      message: 'The request took too long to complete. Please try again.',
      icon: Icons.timer_off_rounded,
      onRetry: onRetry,
    );
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(32.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // Error Icon with gradient background
            Container(
              width: 120,
              height: 120,
              decoration: BoxDecoration(
                shape: BoxShape.circle,
                gradient: LinearGradient(
                  colors: [
                    Colors.red.withValues(alpha: 0.2),
                    Colors.red.withValues(alpha: 0.05),
                  ],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
              ),
              child: Icon(
                icon,
                size: 60,
                color: Colors.red.withValues(alpha: 0.8),
              ),
            ),
            const SizedBox(height: 32),

            // Title
            Text(
              title,
              style: const TextStyle(
                color: Colors.white,
                fontSize: 24,
                fontWeight: FontWeight.bold,
              ),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 12),

            // Message
            Text(
              message,
              style: TextStyle(
                color: Colors.grey[400],
                fontSize: 16,
                height: 1.5,
              ),
              textAlign: TextAlign.center,
            ),

            if (onRetry != null) ...[
              const SizedBox(height: 32),

              // Retry Button
              ElevatedButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh_rounded, color: Colors.white),
                label: const Text(
                  'Try Again',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 16,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.red,
                  padding: const EdgeInsets.symmetric(
                    horizontal: 32,
                    vertical: 16,
                  ),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  elevation: 0,
                ),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
