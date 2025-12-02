import 'package:flutter/material.dart';
import 'dart:convert';

import 'package:kuudere/services/auth_service.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import 'package:kuudere/services/http_service.dart';

class CommentBottomSheet extends StatefulWidget {
  final int commentCount;
  final Map<String, dynamic> episodeData;
  final dynamic epNumber;
  final dynamic animeId;
  final List<Comment> comments;
  final Function(List<Comment>) updateComments;

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
  State<CommentBottomSheet> createState() => _CommentBottomSheetState();
}

class _CommentBottomSheetState extends State<CommentBottomSheet> {
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
        final response =
            await httpService.get('/api/user/current', requireAuth: true);

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

  Future<void> _fetchComments(int page, String sort, {bool isInitialLoad = false}) async {
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

      final url = '/api/anime/comments/${widget.animeId}/${widget.epNumber}?page=$page&sort=$sort';
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
            final newComments = fetchedComments.where((c) => !existingIds.contains(c.id)).toList();
            comments.addAll(newComments);
          }

          _hasMore = data['has_more'] ?? false;
          _currentPage = page;
          _totalComments = data['total_comments'] ?? widget.commentCount;
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

        widget.updateComments(comments);
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
          const SnackBar(content: Text('Please log in to like/dislike comments')),
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

    return Container(
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: Colors.grey[900]?.withOpacity(0.5),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Colors.grey[800]!),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (!isReply) ...[
            Container(
              padding: const EdgeInsets.all(12),
              margin: const EdgeInsets.only(bottom: 12),
              decoration: BoxDecoration(
                color: Colors.grey[800]?.withOpacity(0.3),
                borderRadius: BorderRadius.circular(8),
              ),
              child: const Text(
                "If you don't mind, please leave a comment and share your thoughts with everyone—it will make the website even more lively! Many people are eager to read your comments! 😊",
                style: TextStyle(
                  color: Colors.white70,
                  fontSize: 12,
                  height: 1.4,
                ),
              ),
            ),
          ],
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              _userAvatarUrl != null &&
                      _userAvatarUrl!.isNotEmpty &&
                      _userAvatarUrl != '/placeholder.svg?height=32&width=32'
                  ? ClipRRect(
                      borderRadius: BorderRadius.circular(20),
                      child: Image.network(
                        _userAvatarUrl!.startsWith('http')
                            ? _userAvatarUrl!
                            : 'https://kuudere.to$_userAvatarUrl',
                        width: 40,
                        height: 40,
                        fit: BoxFit.cover,
                        errorBuilder: (context, error, stackTrace) =>
                            Container(
                          width: 40,
                          height: 40,
                          decoration: BoxDecoration(
                            color: Colors.grey[800],
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.person,
                              color: Colors.white70, size: 20),
                        ),
                      ),
                    )
                  : Container(
                      width: 40,
                      height: 40,
                      decoration: BoxDecoration(
                        color: Colors.grey[800],
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.person,
                          color: Colors.white70, size: 20),
                    ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    TextField(
                      controller: controller,
                      maxLines: isReply ? 3 : 5,
                      style: const TextStyle(color: Colors.white),
                      onChanged: (value) {
                        setState(() {
                          // Trigger rebuild to enable/disable button
                        });
                      },
                      decoration: InputDecoration(
                        hintText: isReply ? 'Write a reply...' : 'Write your comment...',
                        hintStyle: TextStyle(color: Colors.grey[600]),
                        filled: true,
                        fillColor: Colors.grey[800]?.withOpacity(0.5),
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide.none,
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: BorderSide(color: Colors.grey[700]!),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(8),
                          borderSide: const BorderSide(color: Colors.red, width: 1),
                        ),
                        contentPadding: const EdgeInsets.all(12),
                      ),
                    ),
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Row(
                          children: [
                            Checkbox(
                              value: isSpoiler,
                              onChanged: (value) => setState(() => isSpoiler = value!),
                              fillColor: WidgetStateProperty.all(Colors.red),
                              checkColor: Colors.white,
                            ),
                            Row(
                              children: [
                                const Icon(Icons.warning_amber_rounded,
                                    color: Colors.amber, size: 16),
                                const SizedBox(width: 4),
                                Text(
                                  'Mark as spoiler',
                                  style: TextStyle(
                                    color: Colors.grey[300],
                                    fontSize: 12,
                                  ),
                                ),
                              ],
                            ),
                          ],
                        ),
                        Row(
                          children: [
                            if (!isReply)
                              TextButton(
                                onPressed: () {
                                  controller.clear();
                                  setState(() => isSpoiler = false);
                                },
                                child: const Text(
                                  'Cancel',
                                  style: TextStyle(color: Colors.white70),
                                ),
                              ),
                            const SizedBox(width: 8),
                            ElevatedButton(
                              onPressed: _isSubmitting || controller.text.trim().isEmpty
                                  ? null
                                  : () async {
                                      if (controller.text.trim().isNotEmpty) {
                                        setState(() {
                                          _isSubmitting = true;
                                        });
                                        if (isReply) {
                                          await _handleReplySubmit(commentId!);
                                        } else {
                                          await _handleCommentSubmit();
                                        }
                                        setState(() {
                                          _isSubmitting = false;
                                        });
                                      }
                                    },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: Colors.red,
                                foregroundColor: Colors.white,
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 16, vertical: 8),
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8),
                                ),
                              ),
                              child: _isSubmitting
                                  ? SizedBox(
                                      width: 20,
                                      height: 20,
                                      child: LoadingAnimationWidget.threeArchedCircle(
                                        color: Colors.white,
                                        size: 20,
                                      ),
                                    )
                                  : Text(
                                      isReply ? 'Reply' : 'Comment',
                                      style: const TextStyle(
                                        color: Colors.white,
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ],
      ),
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
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
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
        body: {
          'commentId': commentId,
          'content': replyContent,
        },
        requireAuth: true,
      );

      if (response.statusCode == 200 || response.statusCode == 201) {
        final responseData = json.decode(response.body);
        setState(() {
          final commentIndex =
              comments.indexWhere((comment) => comment.id == commentId);
          if (commentIndex != -1) {
            // Create a Reply object from the response
            final newReply = Reply(
              id: responseData['id'] ?? responseData['episodeCommentReplyId'] ?? '',
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
        widget.updateComments(comments);

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
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error: ${e.toString()}')),
        );
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

  Widget _buildCommentContent(Comment comment) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        // Profile picture
        ClipRRect(
          borderRadius: BorderRadius.circular(20),
          child: comment.authorPfp != null && comment.authorPfp!.isNotEmpty
              ? Image.network(
                  comment.authorPfp!,
                  width: 40,
                  height: 40,
                  fit: BoxFit.cover,
                  errorBuilder: (context, error, stackTrace) =>
                      _buildDefaultAvatar(comment.author),
                )
              : _buildDefaultAvatar(comment.author),
        ),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Text(
                    comment.author,
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 14,
                      fontWeight: FontWeight.w500,
                    ),
                  ),
                  if (comment.authorVerified == true) ...[
                    const SizedBox(width: 4),
                    const Icon(Icons.verified, color: Colors.blue, size: 16),
                  ],
                  const SizedBox(width: 8),
                  Text(
                    '• ${comment.time}',
                    style: TextStyle(
                      color: Colors.grey[600],
                      fontSize: 12,
                    ),
                  ),
                  if (comment.isSpoiler)
                    Container(
                      margin: const EdgeInsets.only(left: 8),
                      padding: const EdgeInsets.symmetric(
                          horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: Colors.red,
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: const Text(
                        'SPOILER',
                        style: TextStyle(
                          color: Colors.white,
                          fontSize: 10,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 8),
              if (comment.isSpoiler)
                GestureDetector(
                  onTap: () {
                    setState(() {
                      comment.isSpoiler = false;
                    });
                  },
                  child: Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.grey[900],
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: Colors.grey[800]!),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.warning_amber_rounded,
                            color: Colors.amber, size: 20),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text(
                            'This comment contains spoilers. Tap to reveal.',
                            style: TextStyle(color: Colors.white70),
                          ),
                        ),
                      ],
                    ),
                  ),
                )
              else
                Text(
                  comment.content,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 14,
                    height: 1.4,
                  ),
                ),
              const SizedBox(height: 12),
              Row(
                children: [
                  _buildInteractionButton(
                    icon: Icons.thumb_up_outlined,
                    count: comment.likes,
                    isActive: comment.isLiked,
                    onTap: () => _handleInteraction(comment.id, 'like'),
                  ),
                  const SizedBox(width: 16),
                  _buildInteractionButton(
                    icon: Icons.thumb_down_outlined,
                    count: comment.dislikes,
                    isActive: comment.isDisliked,
                    onTap: () => _handleInteraction(comment.id, 'dislike'),
                  ),
                  const SizedBox(width: 16),
                  TextButton.icon(
                    onPressed: () => toggleReplyForm(comment.id),
                    icon: const Icon(Icons.reply, size: 16),
                    label: const Text('Reply'),
                    style: TextButton.styleFrom(
                      foregroundColor: Colors.white70,
                      padding: const EdgeInsets.symmetric(horizontal: 8),
                    ),
                  ),
                  if (comment.replies.isNotEmpty)
                    TextButton(
                      onPressed: () => toggleReplies(comment.id),
                      child: Text(
                        (expandedReplies[comment.id] ?? false)
                            ? 'Hide ${comment.replies.length} replies'
                            : 'Show ${comment.replies.length} replies',
                        style: const TextStyle(color: Colors.white70),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildDefaultAvatar(String author) {
    return Container(
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: Colors.grey[800],
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Text(
          author.isNotEmpty ? author[0].toUpperCase() : '?',
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 16,
          ),
        ),
      ),
    );
  }

  Widget _buildCommentItem(Comment comment) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _buildCommentContent(comment),
          if (showReplyForms[comment.id] ?? false)
            Padding(
              padding: const EdgeInsets.only(left: 40, top: 12),
              child: _buildCommentForm(isReply: true, commentId: comment.id),
            ),
          if (comment.replies.isNotEmpty &&
              (expandedReplies[comment.id] ?? false))
            Padding(
              padding: const EdgeInsets.only(left: 40, top: 12),
              child: Column(
                children: comment.replies
                    .map((reply) => _buildReplyContent(reply))
                    .toList(),
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildReplyContent(Reply reply) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12, left: 16),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(16),
            child: reply.authorPfp != null && reply.authorPfp!.isNotEmpty
                ? Image.network(
                    reply.authorPfp!,
                    width: 32,
                    height: 32,
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) =>
                        _buildDefaultReplyAvatar(reply.author),
                  )
                : _buildDefaultReplyAvatar(reply.author),
          ),
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
                    if (reply.authorVerified == true) ...[
                      const SizedBox(width: 4),
                      const Icon(Icons.verified, color: Colors.blue, size: 14),
                    ],
                    const SizedBox(width: 8),
                    Text(
                      '• ${reply.time}',
                      style: TextStyle(
                        color: Colors.grey[600],
                        fontSize: 12,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 6),
                Text(
                  reply.content,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 13,
                    height: 1.4,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildDefaultReplyAvatar(String author) {
    return Container(
      width: 32,
      height: 32,
      decoration: BoxDecoration(
        color: Colors.grey[800],
        shape: BoxShape.circle,
      ),
      child: Center(
        child: Text(
          author.isNotEmpty ? author[0].toUpperCase() : '?',
          style: const TextStyle(
            color: Colors.white,
            fontWeight: FontWeight.bold,
            fontSize: 12,
          ),
        ),
      ),
    );
  }

  Widget _buildInteractionButton({
    required IconData icon,
    required int count,
    required bool isActive,
    required VoidCallback onTap,
  }) {
    return GestureDetector(
      onTap: onTap,
      child: Row(
        children: [
          Icon(
            icon,
            color: isActive ? Colors.blue : Colors.white70,
            size: 20,
          ),
          if (count > 0) ...[
            const SizedBox(width: 4),
            Text(
              count.toString(),
              style: TextStyle(
                color: isActive ? Colors.blue : Colors.white70,
                fontSize: 14,
              ),
            ),
          ],
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return DraggableScrollableSheet(
      initialChildSize: 0.9,
      minChildSize: 0.5,
      maxChildSize: 0.95,
      builder: (context, scrollController) {
        return Container(
          decoration: const BoxDecoration(
            color: Color(0xFF0A0A0A),
            borderRadius: BorderRadius.vertical(top: Radius.circular(16)),
          ),
          child: Column(
            children: [
              Container(
                margin: const EdgeInsets.symmetric(vertical: 12),
                width: 40,
                height: 4,
                decoration: BoxDecoration(
                  color: Colors.grey[800],
                  borderRadius: BorderRadius.circular(2),
                ),
              ),
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16),
                child: Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      children: [
                        const Text(
                          'COMMENTS',
                          style: TextStyle(
                            color: Colors.white,
                            fontSize: 20,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                        const SizedBox(width: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 4),
                          decoration: BoxDecoration(
                            color: Colors.red,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            '$_totalComments',
                            style: const TextStyle(
                              color: Colors.white,
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, color: Colors.white),
                      onPressed: () => Navigator.pop(context),
                    ),
                  ],
                ),
              ),
              // Sort buttons
              Padding(
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                child: Row(
                  children: [
                    _buildSortButton('best', 'Best'),
                    const SizedBox(width: 8),
                    _buildSortButton('new', 'Newest'),
                    const SizedBox(width: 8),
                    _buildSortButton('oldest', 'Oldest'),
                  ],
                ),
              ),
              const Divider(color: Color(0xFF2A2A2A)),
              Padding(
                padding: const EdgeInsets.all(16),
                child: _buildCommentForm(),
              ),
              const Divider(color: Color(0xFF2A2A2A)),
              Expanded(
                child: _isLoading
                    ? const Center(
                        child: CircularProgressIndicator(color: Colors.red),
                      )
                    : _error != null
                        ? Center(
                            child: Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  _error!,
                                  style: const TextStyle(color: Colors.white70),
                                ),
                                const SizedBox(height: 16),
                                ElevatedButton(
                                  onPressed: () => _fetchComments(
                                      1, _currentSort,
                                      isInitialLoad: true),
                                  child: const Text('Retry'),
                                ),
                              ],
                            ),
                          )
                        : comments.isEmpty
                            ? const Center(
                                child: Text(
                                  'No comments yet. Be the first to comment!',
                                  style: TextStyle(color: Colors.white70),
                                ),
                              )
                            : ListView.separated(
                                controller: scrollController,
                                padding: const EdgeInsets.symmetric(
                                    horizontal: 16, vertical: 8),
                                itemCount: comments.length +
                                    (_hasMore ? 1 : 0) +
                                    (_isLoadingMore ? 1 : 0),
                                separatorBuilder: (context, index) {
                                  if (index >= comments.length) {
                                    return const SizedBox.shrink();
                                  }
                                  return const Divider(
                                    color: Color(0xFF2A2A2A),
                                    height: 1,
                                  );
                                },
                                itemBuilder: (context, index) {
                                  if (index == comments.length && _isLoadingMore) {
                                    return const Padding(
                                      padding: EdgeInsets.all(16),
                                      child: Center(
                                        child: CircularProgressIndicator(
                                            color: Colors.red),
                                      ),
                                    );
                                  }
                                  if (index == comments.length && _hasMore) {
                                    return Padding(
                                      padding: const EdgeInsets.all(16),
                                      child: Center(
                                        child: ElevatedButton(
                                          onPressed: _loadMoreComments,
                                          child: const Text('Load more comments'),
                                        ),
                                      ),
                                    );
                                  }
                                  return _buildCommentItem(comments[index]);
                                },
                              ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSortButton(String sort, String label) {
    final isSelected = _currentSort == sort;
    return Expanded(
      child: ElevatedButton(
        onPressed: _isLoading ? null : () => _handleSortChange(sort),
        style: ElevatedButton.styleFrom(
          backgroundColor:
              isSelected ? Colors.red : Colors.grey[800]?.withOpacity(0.5),
          foregroundColor: isSelected ? Colors.white : Colors.grey[400],
          padding: const EdgeInsets.symmetric(vertical: 8),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(8),
          ),
        ),
        child: Text(
          label,
          style: TextStyle(
            fontSize: 12,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
          ),
        ),
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
