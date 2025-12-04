import 'package:flutter/material.dart';
import 'dart:convert';

import 'package:kuudere/services/auth_service.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:kuudere/services/http_service.dart';
import 'package:kuudere/widgets/custom_dropdown.dart';

class CommentBottomSheet extends StatelessWidget {
  final int commentCount;
  final Map<String, dynamic> episodeData;
  final dynamic epNumber;
  final dynamic animeId;
  final List<Comment> comments;
  final Function(List<Comment>, int) updateComments;

  const CommentBottomSheet({
    super.key,
    required this.commentCount,
    required this.episodeData,
    required this.epNumber,
    required this.animeId,
    required this.comments,
    required this.updateComments,
  });

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.9,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      builder: (context, scrollController) {
        return CommentsContent(
          commentCount: commentCount,
          episodeData: episodeData,
          epNumber: epNumber,
          animeId: animeId,
          comments: comments,
          updateComments: updateComments,
          scrollController: scrollController,
          isDesktop: false,
        );
      },
    );
  }
}

class CommentsContent extends StatefulWidget {
  final int commentCount;
  final Map<String, dynamic> episodeData;
  final dynamic epNumber;
  final dynamic animeId;
  final List<Comment> comments;
  final Function(List<Comment>, int) updateComments;
  final ScrollController? scrollController;
  final bool isDesktop;

  const CommentsContent({
    super.key,
    required this.commentCount,
    required this.episodeData,
    required this.epNumber,
    required this.animeId,
    required this.comments,
    required this.updateComments,
    this.scrollController,
    this.isDesktop = false,
  });

  @override
  State<CommentsContent> createState() => _CommentsContentState();
}

class _CommentsContentState extends State<CommentsContent> {
  List<Comment> comments = [];
  final TextEditingController _commentController = TextEditingController();
  final Map<String, TextEditingController> _replyControllers = {};
  Map<String, bool> showReplyForms = {};
  Map<String, bool> expandedReplies = {};
  bool isSpoiler = false;
  bool _isSubmitting = false;
  bool _isLoading = true;
  bool _isLoadingMore = false;
  bool _hasMore = false;
  int _currentPage = 1;
  String _currentSort = 'new'; // 'new', 'oldest', 'best'
  String? _error;
  int _totalComments = 0;
  String? _userAvatarUrl;
  final authService = AuthService();

  @override
  void initState() {
    super.initState();
    _totalComments = widget.commentCount;
    _fetchUserAvatar();
    // Initialize with existing comments if any, otherwise fetch from API
    if (widget.comments.isNotEmpty) {
      comments = widget.comments;
      for (var comment in comments) {
        showReplyForms[comment.id] = false;
        expandedReplies[comment.id] = false;
        _replyControllers[comment.id] = TextEditingController();
      }
      _isLoading = false;
    } else {
      _fetchComments(1, _currentSort, isInitialLoad: true);
    }
  }

  Future<void> _fetchUserAvatar() async {
    try {
      final httpService = HttpService();
      final sessionInfo = await authService.getStoredSession();
      if (sessionInfo != null) {
        final response = await httpService.get(
          '/api/user/current',
          requireAuth: true,
        );

        if (response.statusCode == 200) {
          final data = json.decode(response.body);
          if (data['success'] == true && data['user'] != null) {
            final userData = data['user'];
            if (mounted) {
              setState(() {
                _userAvatarUrl = userData['avatar'];
              });
            }
          }
        }
      }
    } catch (e) {
      // print('Error fetching user avatar: $e');
    }
  }

  Future<void> _fetchComments(
    int page,
    String sort, {
    bool isInitialLoad = false,
  }) async {
    if (isInitialLoad) {
      setState(() {
        _isLoading = true;
        _error = null;
      });
    } else {
      setState(() {
        _isLoadingMore = true;
      });
    }

    try {
      final httpService = HttpService();
      final authService = AuthService();
      final sessionInfo = await authService.getStoredSession();

      final url =
          '/api/anime/comments/${widget.animeId}/${widget.epNumber}?page=$page&sort=$sort';
      final response = await httpService.get(
        url,
        requireAuth: sessionInfo != null,
      );

      if (response.statusCode == 200) {
        final data = json.decode(response.body);
        final fetchedComments = (data['comments'] as List<dynamic>?)
                ?.map((comment) => Comment.fromJson(comment))
                .toList() ??
            [];

        setState(() {
          if (page == 1) {
            comments = fetchedComments;
          } else {
            // Avoid duplicates when loading more
            final existingIds = comments.map((c) => c.id).toSet();
            final newComments = fetchedComments
                .where((c) => !existingIds.contains(c.id))
                .toList();
            comments.addAll(newComments);
          }

          _hasMore = data['has_more'] ?? false;
          _currentPage = page;
          _totalComments = data['total_comments'] ?? widget.commentCount;

          // Fallback: if API says 0 but we have comments, use the list length
          if (_totalComments == 0 && comments.isNotEmpty) {
            _totalComments = comments.length;
          }

          _isLoading = false;
          _isLoadingMore = false;

          // Initialize controllers for new comments
          for (var comment in comments) {
            if (!_replyControllers.containsKey(comment.id)) {
              showReplyForms[comment.id] = false;
              expandedReplies[comment.id] = false;
              _replyControllers[comment.id] = TextEditingController();
            }
          }
        });

        widget.updateComments(comments, _totalComments);
      } else {
        throw Exception('Failed to fetch comments: ${response.statusCode}');
      }
    } catch (e) {
      setState(() {
        _error = 'Failed to load comments. Please try again.';
        _isLoading = false;
        _isLoadingMore = false;
      });
    }
  }

  void _handleSortChange(String newSort) {
    if (newSort == _currentSort || _isLoading) return;

    setState(() {
      _currentSort = newSort;
      _currentPage = 1;
    });

    _fetchComments(1, newSort, isInitialLoad: true);
  }

  void _loadMoreComments() {
    if (!_isLoadingMore && _hasMore) {
      _fetchComments(_currentPage + 1, _currentSort);
    }
  }

  @override
  void dispose() {
    _commentController.dispose();
    for (var controller in _replyControllers.values) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _handleInteraction(String commentId, String type) async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    if (sessionInfo == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Please log in to like/dislike comments'),
          ),
        );
      }
      return;
    }

    try {
      final httpService = HttpService();
      final response = await httpService.post(
        '/api/anime/comment/respond/$commentId',
        body: {
          'type': type, // 'like' or 'dislike'
        },
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        setState(() {
          final comment = comments.firstWhere((c) => c.id == commentId);
          if (type == 'like') {
            if (comment.isLiked) {
              comment.likes--;
              comment.isLiked = false;
            } else {
              comment.likes++;
              comment.isLiked = true;
              if (comment.isDisliked) {
                comment.dislikes--;
                comment.isDisliked = false;
              }
            }
          } else if (type == 'dislike') {
            if (comment.isDisliked) {
              comment.dislikes--;
              comment.isDisliked = false;
            } else {
              comment.dislikes++;
              comment.isDisliked = true;
              if (comment.isLiked) {
                comment.likes--;
                comment.isLiked = false;
              }
            }
          }
        });
      } else {
        // print('Failed to $type comment: ${response.statusCode}');
      }
    } catch (e) {
      // print('Error ${type}ing comment: $e');
    }
  }

  Widget _buildCommentForm({bool isReply = false, String? commentId}) {
    final controller =
        isReply ? _replyControllers[commentId]! : _commentController;

    // We can use a FocusNode to detect focus if we want to show buttons only on focus
    // For simplicity, we'll show buttons if text is not empty or if it's a reply form
    final showButtons = isReply || controller.text.isNotEmpty;

    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        _buildAvatar(_userAvatarUrl, size: isReply ? 24 : 40),
        const SizedBox(width: 16),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              TextField(
                controller: controller,
                style: const TextStyle(color: Colors.white, fontSize: 14),
                maxLines: null,
                minLines: 1,
                onChanged: (val) => setState(() {}),
                decoration: InputDecoration(
                  hintText: isReply ? 'Add a reply...' : 'Add a comment...',
                  hintStyle: TextStyle(color: Colors.grey[600], fontSize: 14),
                  contentPadding: const EdgeInsets.only(bottom: 8),
                  isDense: true,
                  border: const UnderlineInputBorder(
                    borderSide: BorderSide(color: Colors.grey),
                  ),
                  enabledBorder: UnderlineInputBorder(
                    borderSide: BorderSide(color: Colors.grey[800]!),
                  ),
                  focusedBorder: const UnderlineInputBorder(
                    borderSide: BorderSide(color: Colors.white),
                  ),
                ),
              ),
              if (showButtons || isReply) ...[
                const SizedBox(height: 8),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    // Spoiler Checkbox
                    Row(
                      children: [
                        SizedBox(
                          height: 24,
                          width: 24,
                          child: Checkbox(
                            value: isSpoiler,
                            onChanged: (v) => setState(() => isSpoiler = v!),
                            fillColor: WidgetStateProperty.resolveWith(
                              (states) => states.contains(WidgetState.selected)
                                  ? Colors.red
                                  : Colors.transparent,
                            ),
                            side: const BorderSide(color: Colors.grey),
                          ),
                        ),
                        const SizedBox(width: 8),
                        const Text(
                          'Spoiler',
                          style: TextStyle(color: Colors.grey, fontSize: 12),
                        ),
                      ],
                    ),
                    Row(
                      children: [
                        TextButton(
                          onPressed: () {
                            controller.clear();
                            if (isReply) toggleReplyForm(commentId!);
                            setState(() => isSpoiler = false);
                          },
                          style: TextButton.styleFrom(
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 16),
                          ),
                          child: const Text('Cancel'),
                        ),
                        const SizedBox(width: 8),
                        ElevatedButton(
                          onPressed:
                              _isSubmitting || controller.text.trim().isEmpty
                                  ? null
                                  : () async {
                                      if (isReply) {
                                        await _handleReplySubmit(commentId!);
                                      } else {
                                        await _handleCommentSubmit();
                                      }
                                    },
                          style: ElevatedButton.styleFrom(
                            backgroundColor: Colors.blueAccent,
                            disabledBackgroundColor: Colors.grey[800],
                            foregroundColor: Colors.black,
                            disabledForegroundColor: Colors.grey[500],
                            padding: const EdgeInsets.symmetric(
                              horizontal: 16,
                              vertical: 8,
                            ),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(18),
                            ),
                          ),
                          child: _isSubmitting
                              ? const SizedBox(
                                  width: 16,
                                  height: 16,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                    color: Colors.black,
                                  ),
                                )
                              : Text(isReply ? 'Reply' : 'Comment'),
                        ),
                      ],
                    ),
                  ],
                ),
              ],
            ],
          ),
        ),
      ],
    );
  }

  Future<void> _handleCommentSubmit() async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    if (sessionInfo == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please log in to post comments')),
        );
      }
      return;
    }

    if (_commentController.text.trim().isEmpty) {
      return;
    }

    setState(() {
      _isSubmitting = true;
    });

    final httpService = HttpService();

    try {
      // Backend expects: anime, ep, content, spoiller
      final response = await httpService.post(
        '/api/anime/comment',
        body: {
          'anime': widget.animeId,
          'ep': widget.epNumber.toString(),
          'content': _commentController.text.trim(),
          'spoiller': isSpoiler,
        },
        requireAuth: true,
      );

      if (response.statusCode == 200) {
        final responseData = json.decode(response.body);
        if (responseData['success'] == true) {
          // Refresh comments to get the new one with proper formatting
          await _fetchComments(1, _currentSort, isInitialLoad: true);

          setState(() {
            _commentController.clear();
            isSpoiler = false;
            _totalComments++;
          });

          if (mounted) {
            ScaffoldMessenger.of(context).showSnackBar(
              const SnackBar(content: Text('Comment posted successfully!')),
            );
          }
        } else {
          throw Exception(responseData['message'] ?? 'Failed to post comment');
        }
      } else {
        throw Exception('Failed to post comment: ${response.statusCode}');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error: ${e.toString()}')));
      }
    } finally {
      if (mounted) {
        setState(() {
          _isSubmitting = false;
        });
      }
    }
  }

  Future<void> _handleReplySubmit(String commentId) async {
    final authService = AuthService();
    final sessionInfo = await authService.getStoredSession();

    if (sessionInfo == null) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Please log in to post replies')),
        );
      }
      return;
    }

    final replyContent = _replyControllers[commentId]!.text.trim();
    if (replyContent.isEmpty) {
      return;
    }

    try {
      final httpService = HttpService();
      final response = await httpService.post(
        '/api/anime/comments/reply',
        body: {'commentId': commentId, 'content': replyContent},
        requireAuth: true,
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final responseData = json.decode(response.body);
        setState(() {
          final commentIndex = comments.indexWhere(
            (comment) => comment.id == commentId,
          );
          if (commentIndex != -1) {
            // Create a Reply object from the response
            final newReply = Reply(
              id: responseData['id'] ??
                  responseData['episodeCommentReplyId'] ??
                  '',
              author: responseData['author'] ?? 'You',
              content: replyContent, // Use the original content, not formatted
              time: responseData['time'] ?? 'Just now',
            );

            comments[commentIndex].replies.add(newReply);
            _replyControllers[commentId]!.clear();
            showReplyForms[commentId] = false;
            expandedReplies[commentId] = true;
          }
        });
        widget.updateComments(comments, _totalComments);

        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Reply posted successfully!')),
          );
        }
      } else {
        throw Exception('Failed to post reply: ${response.statusCode}');
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text('Error: ${e.toString()}')));
      }
    }
  }

  void toggleReplyForm(String commentId) {
    setState(() {
      showReplyForms[commentId] = !(showReplyForms[commentId] ?? false);
    });
  }

  void toggleReplies(String commentId) {
    setState(() {
      expandedReplies[commentId] = !(expandedReplies[commentId] ?? false);
    });
  }

  Widget _buildCommentItem(Comment comment) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildAvatar(comment.authorPfp, size: 40, fallback: comment.author),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                // Author & Time
                Row(
                  children: [
                    Text(
                      comment.author,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    if (comment.authorVerified == true) ...[
                      const SizedBox(width: 4),
                      const Icon(
                        Icons.check_circle,
                        color: Colors.grey,
                        size: 12,
                      ),
                    ],
                    const SizedBox(width: 8),
                    Text(
                      comment.time,
                      style: TextStyle(color: Colors.grey[500], fontSize: 12),
                    ),
                  ],
                ),
                const SizedBox(height: 4),

                // Content
                if (comment.isSpoiler)
                  InkWell(
                    onTap: () => setState(() => comment.isSpoiler = false),
                    child: Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 4,
                      ),
                      decoration: BoxDecoration(
                        color: Colors.grey[900],
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'Spoiler (Tap to reveal)',
                        style: TextStyle(
                          color: Colors.redAccent,
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                  )
                else
                  Text(
                    comment.content,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      height: 1.3,
                    ),
                  ),

                const SizedBox(height: 8),

                // Actions
                Row(
                  children: [
                    _buildActionButton(
                      icon: comment.isLiked
                          ? Icons.thumb_up
                          : Icons.thumb_up_outlined,
                      label: comment.likes > 0 ? '${comment.likes}' : null,
                      isActive: comment.isLiked,
                      onTap: () => _handleInteraction(comment.id, 'like'),
                    ),
                    const SizedBox(width: 16),
                    _buildActionButton(
                      icon: comment.isDisliked
                          ? Icons.thumb_down
                          : Icons.thumb_down_outlined,
                      isActive: comment.isDisliked,
                      onTap: () => _handleInteraction(comment.id, 'dislike'),
                    ),
                    const SizedBox(width: 16),
                    InkWell(
                      onTap: () => toggleReplyForm(comment.id),
                      borderRadius: BorderRadius.circular(16),
                      child: Padding(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        child: const Text(
                          'Reply',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 12,
                            fontWeight: FontWeight.w500,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),

                // Reply Form
                if (showReplyForms[comment.id] ?? false)
                  Padding(
                    padding: const EdgeInsets.only(top: 12),
                    child: _buildCommentForm(
                      isReply: true,
                      commentId: comment.id,
                    ),
                  ),

                // Replies List
                if (comment.replies.isNotEmpty) ...[
                  const SizedBox(height: 8),
                  AnimatedSize(
                    duration: const Duration(milliseconds: 300),
                    curve: Curves.easeInOut,
                    child: Column(
                      children: [
                        if (!(expandedReplies[comment.id] ?? false))
                          InkWell(
                            onTap: () => toggleReplies(comment.id),
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 8),
                              child: Row(
                                children: [
                                  const Icon(
                                    Icons.arrow_drop_down,
                                    color: Colors.blue,
                                    size: 24,
                                  ),
                                  Text(
                                    '${comment.replies.length} replies',
                                    style: const TextStyle(
                                      color: Colors.blue,
                                      fontWeight: FontWeight.bold,
                                      fontSize: 13,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          )
                        else
                          Column(
                            children: [
                              ...comment.replies.map(
                                (reply) => _buildReplyItem(reply, comment.id),
                              ),
                              // Hide replies button
                              InkWell(
                                onTap: () => toggleReplies(comment.id),
                                child: Container(
                                  padding: const EdgeInsets.symmetric(
                                    vertical: 8,
                                  ),
                                  child: Row(
                                    children: [
                                      const Icon(
                                        Icons.arrow_drop_up,
                                        color: Colors.blue,
                                        size: 24,
                                      ),
                                      const Text(
                                        'Hide replies',
                                        style: TextStyle(
                                          color: Colors.blue,
                                          fontWeight: FontWeight.bold,
                                          fontSize: 13,
                                        ),
                                      ),
                                    ],
                                  ),
                                ),
                              ),
                            ],
                          ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildReplyItem(Reply reply, String commentId) {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildAvatar(reply.authorPfp, size: 24, fallback: reply.author),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Text(
                      reply.author,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 13,
                        fontWeight: FontWeight.w500,
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(
                      reply.time,
                      style: TextStyle(color: Colors.grey[500], fontSize: 12),
                    ),
                  ],
                ),
                const SizedBox(height: 2),
                Text(
                  reply.content,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    height: 1.3,
                  ),
                ),
                const SizedBox(height: 4),
                // Reply actions (simplified)
                Row(
                  children: [
                    Icon(
                      Icons.thumb_up_outlined,
                      color: Colors.grey[400],
                      size: 14,
                    ),
                    const SizedBox(width: 16),
                    Icon(
                      Icons.thumb_down_outlined,
                      color: Colors.grey[400],
                      size: 14,
                    ),
                    const SizedBox(width: 16),
                    InkWell(
                      onTap: () {
                        if (!(showReplyForms[commentId] ?? false)) {
                          toggleReplyForm(commentId);
                        }
                        // Pre-fill @username
                        final controller = _replyControllers[commentId];
                        if (controller != null) {
                          controller.text = '@${reply.author} ';
                          controller.selection = TextSelection.fromPosition(
                            TextPosition(offset: controller.text.length),
                          );
                        }
                      },
                      child: const Padding(
                        padding: EdgeInsets.symmetric(vertical: 4),
                        child: Text(
                          'Reply',
                          style: TextStyle(color: Colors.white, fontSize: 11),
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildAvatar(String? url, {double size = 40, String fallback = '?'}) {
    if (url != null && url.isNotEmpty) {
      return ClipOval(
        child: Image.network(
          url.startsWith('http') ? url : 'https://kuudere.to$url',
          width: size,
          height: size,
          fit: BoxFit.cover,
          errorBuilder: (_, __, ___) => _buildFallbackAvatar(size, fallback),
        ),
      );
    }
    return _buildFallbackAvatar(size, fallback);
  }

  Widget _buildFallbackAvatar(double size, String name) {
    return Container(
      width: size,
      height: size,
      decoration: BoxDecoration(
        color: Colors.primaries[name.length % Colors.primaries.length],
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: Text(
        name.isNotEmpty ? name[0].toUpperCase() : '?',
        style: TextStyle(
          color: Colors.white,
          fontSize: size * 0.5,
          fontWeight: FontWeight.bold,
        ),
      ),
    );
  }

  Widget _buildActionButton({
    required IconData icon,
    String? label,
    required bool isActive,
    required VoidCallback onTap,
  }) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(20),
      child: Padding(
        padding: const EdgeInsets.all(4),
        child: Row(
          children: [
            AnimatedSwitcher(
              duration: const Duration(milliseconds: 300),
              transitionBuilder: (child, anim) =>
                  ScaleTransition(scale: anim, child: child),
              child: Icon(
                icon,
                key: ValueKey(icon),
                size: 16,
                color: isActive ? Colors.white : Colors.grey[400],
              ),
            ),
            if (label != null) ...[
              const SizedBox(width: 6),
              Text(
                label,
                style: const TextStyle(color: Colors.grey, fontSize: 12),
              ),
            ],
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: const Color(0xFF0F0F0F),
        borderRadius: widget.isDesktop
            ? BorderRadius.circular(16)
            : const BorderRadius.vertical(top: Radius.circular(16)),
      ),
      child: Column(
        children: [
          if (!widget.isDesktop)
            Center(
              child: Container(
                margin: const EdgeInsets.symmetric(vertical: 12),
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey[800],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
            ),

          // Header: Count & Sort
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 4),
            child: Row(
              children: [
                Text(
                  '$_totalComments',
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const SizedBox(width: 8),
                const Text(
                  'Comments',
                  style: TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.bold,
                  ),
                ),
                const Spacer(),
                _buildSortDropdown(),
                if (!widget.isDesktop)
                  IconButton(
                    icon: const Icon(Icons.close, color: Colors.white),
                    onPressed: () => Navigator.pop(context),
                  ),
              ],
            ),
          ),

          const Divider(color: Colors.white10, height: 1),

          Expanded(
            child: _isLoading
                ? Center(
                    child: LoadingAnimationWidget.threeArchedCircle(
                      color: Colors.white,
                      size: 30,
                    ),
                  )
                : _error != null
                    ? Center(
                        child: Column(
                          mainAxisAlignment: MainAxisAlignment.center,
                          children: [
                            const Icon(
                              Icons.error_outline,
                              color: Colors.redAccent,
                              size: 32,
                            ),
                            const SizedBox(height: 8),
                            Text(
                              _error!,
                              style: const TextStyle(color: Colors.white70),
                            ),
                            TextButton(
                              onPressed: () => _fetchComments(
                                1,
                                _currentSort,
                                isInitialLoad: true,
                              ),
                              child: const Text(
                                'Retry',
                                style: TextStyle(color: Colors.blue),
                              ),
                            ),
                          ],
                        ),
                      )
                    : ListView.builder(
                        controller: widget.scrollController,
                        padding: const EdgeInsets.only(bottom: 24),
                        itemCount: comments.length +
                            (_hasMore ? 1 : 0) +
                            (_isLoadingMore ? 1 : 0) +
                            1, // +1 for the input form at top
                        itemBuilder: (context, index) {
                          if (index == 0) {
                            return Padding(
                              padding: const EdgeInsets.symmetric(
                                horizontal: 16,
                                vertical: 16,
                              ),
                              child: _buildCommentForm(),
                            );
                          }

                          final commentIndex = index - 1;

                          if (commentIndex == comments.length &&
                              _isLoadingMore) {
                            return Padding(
                              padding: const EdgeInsets.all(16),
                              child: Center(
                                child: LoadingAnimationWidget.staggeredDotsWave(
                                  color: Colors.white,
                                  size: 24,
                                ),
                              ),
                            );
                          }
                          if (commentIndex == comments.length && _hasMore) {
                            return TextButton(
                              onPressed: _loadMoreComments,
                              child: const Text(
                                'Load more comments',
                                style: TextStyle(color: Colors.blue),
                              ),
                            );
                          }
                          if (commentIndex >= comments.length) {
                            return const SizedBox.shrink();
                          }
                          return TweenAnimationBuilder<double>(
                            tween: Tween(begin: 0.0, end: 1.0),
                            duration: Duration(
                              milliseconds: 400 + (index * 50).clamp(0, 1000),
                            ),
                            curve: Curves.easeOutQuart,
                            builder: (context, value, child) {
                              return Transform.translate(
                                offset: Offset(0, 20 * (1 - value)),
                                child: Opacity(opacity: value, child: child),
                              );
                            },
                            child: _buildCommentItem(comments[commentIndex]),
                          );
                        },
                      ),
          ),
        ],
      ),
    );
  }

  Widget _buildSortDropdown() {
    return CustomDropdown<String>(
      value: _currentSort,
      items: const ['best', 'new', 'oldest'],
      itemBuilder: (value) {
        switch (value) {
          case 'best':
            return 'Top comments';
          case 'new':
            return 'Newest first';
          case 'oldest':
            return 'Oldest first';
          default:
            return value;
        }
      },
      onChanged: (value) => _handleSortChange(value),
      width: 160,
      child: Row(
        children: [
          const Icon(Icons.sort, color: Colors.white, size: 20),
          const SizedBox(width: 8),
          const Text(
            'Sort by',
            style: TextStyle(color: Colors.white, fontWeight: FontWeight.w500),
          ),
        ],
      ),
    );
  }
}

class Comment {
  final String id;
  final String author;
  final String? authorPfp;
  final bool? authorVerified;
  final List<String>? authorLabels;
  final String content;
  final String time;
  int likes;
  int dislikes;
  bool isLiked;
  bool isDisliked;
  final List<Reply> replies;
  bool isSpoiler;

  Comment({
    required this.id,
    required this.author,
    this.authorPfp,
    this.authorVerified,
    this.authorLabels,
    required this.content,
    required this.time,
    this.likes = 0,
    this.dislikes = 0,
    this.isLiked = false,
    this.isDisliked = false,
    this.replies = const [],
    this.isSpoiler = false,
  });

  factory Comment.fromJson(Map<String, dynamic> json) {
    final replies = (json['replies'] as List<dynamic>?)
            ?.map((reply) => Reply.fromJson(reply))
            .toList() ??
        [];

    return Comment(
      id: json['id'] ?? '',
      author: json['author'] ?? 'Unknown',
      authorPfp: json['authorPfp'],
      authorVerified: json['authorVerified'] ?? false,
      authorLabels: json['authorLabels'] != null
          ? List<String>.from(json['authorLabels'])
          : null,
      content: json['content'] ?? '',
      time: json['time'] ?? 'Unknown',
      likes: json['likes'] ?? 0,
      dislikes: 0, // Backend doesn't return dislikes count separately
      isLiked: json['isLiked'] ?? false,
      isDisliked: json['isUnliked'] ?? false,
      replies: replies,
      isSpoiler: json['isSpoiller'] ?? false,
    );
  }
}

class Reply {
  final String id;
  final String author;
  final String? authorPfp;
  final bool? authorVerified;
  final String content;
  final String time;

  Reply({
    required this.id,
    required this.author,
    this.authorPfp,
    this.authorVerified,
    required this.content,
    required this.time,
  });

  factory Reply.fromJson(Map<String, dynamic> json) {
    return Reply(
      id: json['id'] ?? '',
      author: json['author'] ?? 'Unknown',
      authorPfp: json['authorPfp'],
      authorVerified: json['authorVerified'] ?? false,
      content: json['content'] ?? '',
      time: json['time'] ?? 'Unknown',
    );
  }
}
