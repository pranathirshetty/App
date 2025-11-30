import 'package:flutter/material.dart';
import 'dart:math' as math;
// import 'dart:ui';
import 'dart:async';

import 'package:kuudere/home_screen.dart';
import 'package:kuudere/models/session_model.dart';
import 'package:kuudere/services/realtime_service.dart';
import 'package:loading_animation_widget/loading_animation_widget.dart';
import '../services/auth_service.dart';
import 'package:carousel_slider/carousel_slider.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen>
    with SingleTickerProviderStateMixin {
  final _formKey = GlobalKey<FormState>();
  final _emailController = TextEditingController();
  final _passwordController = TextEditingController();
  final _displayNameController = TextEditingController();
  final _authService = AuthService();
  bool _isLoading = false;
  bool _isLogin = true;
  bool _obscurePassword = true;
  final RealtimeService _realtimeService = RealtimeService();

  late AnimationController _animationController;
  late Animation<double> _fadeAnimation;
  late Animation<Offset> _slideAnimation;
  final CarouselSliderController _carouselController =
      CarouselSliderController();
  Timer? _carouselTimer;

  final List<Map<String, String>> _animeList = [
    {
      'imageUrl':
          'https://artworks.thetvdb.com/banners/v4/series/424536/posters/68d6d5b36aa2f.jpg',
      'title': 'Frieren Season 2',
      'year': '2026',
      'eps': '28',
    },
    {
      'imageUrl':
          'https://artworks.thetvdb.com/banners/v4/series/377543/posters/655f6f3591801.jpg',
      'title': 'Jujutsu Kaisen Season 3',
      'year': '2026',
      'eps': '24',
    },
    {
      'imageUrl':
          'https://artworks.thetvdb.com/banners/v4/series/355480/posters/68aa38e36a087.jpg',
      'title': 'Fire Force Season 3',
      'year': '2026',
      'eps': '24',
    },
    {
      'imageUrl':
          'https://artworks.thetvdb.com/banners/v4/series/421069/posters/67026a480c6d1.jpg',
      'title': 'Oshi no Ko Season 3',
      'year': '2026',
      'eps': '13',
    },
    {
      'imageUrl':
          'https://artworks.thetvdb.com/banners/v4/series/414221/posters/639d9b966b354.jpg',
      'title': 'The Angel Next Door S2',
      'year': '2026',
      'eps': '12',
    },
  ];

  @override
  void initState() {
    super.initState();
    _realtimeService.joinRoom('home');

    _animationController = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 800),
    );

    _fadeAnimation = Tween<double>(begin: 0.0, end: 1.0).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );

    _slideAnimation =
        Tween<Offset>(begin: const Offset(0, 0.1), end: Offset.zero).animate(
      CurvedAnimation(parent: _animationController, curve: Curves.easeOut),
    );

    _animationController.forward();

    WidgetsBinding.instance.addPostFrameCallback((_) => _startCarousel());
  }

  void _startCarousel() {
    const duration = Duration(seconds: 4);
    // Start immediately
    _carouselController.nextPage(duration: duration, curve: Curves.linear);
    // Loop
    _carouselTimer = Timer.periodic(duration, (timer) {
      if (mounted) {
        _carouselController.nextPage(duration: duration, curve: Curves.linear);
      }
    });
  }

  @override
  void dispose() {
    _emailController.dispose();
    _passwordController.dispose();
    _displayNameController.dispose();
    _animationController.dispose();
    _carouselTimer?.cancel();
    super.dispose();
  }

  Future<void> _submitForm() async {
    if (_formKey.currentState!.validate()) {
      setState(() => _isLoading = true);
      try {
        SessionInfo? sessionInfo;
        if (_isLogin) {
          sessionInfo = await _authService.login(
            _emailController.text,
            _passwordController.text,
          );
        } else {
          sessionInfo = await _authService.register(
            _emailController.text,
            _passwordController.text,
            _displayNameController.text,
          );
        }

        if (sessionInfo != null && mounted) {
          // Navigate to home screen and remove all previous routes
          Navigator.of(context).pushAndRemoveUntil(
            MaterialPageRoute(builder: (context) => const HomeScreen()),
            (route) => false,
          );
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                e.toString(),
                style: const TextStyle(color: Colors.white),
              ),
              backgroundColor: Colors.red[900],
            ),
          );
        }
      } finally {
        setState(() => _isLoading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: LayoutBuilder(
        builder: (context, constraints) {
          final isDesktop = constraints.maxWidth > 900;
          final screenWidth = constraints.maxWidth;

          // Responsive font sizes
          final headingSize =
              isDesktop ? 42.0 : (screenWidth < 600 ? 28.0 : 36.0);
          final descriptionSize = isDesktop ? 16.0 : 14.0;
          final logoSize = isDesktop ? 32.0 : 28.0;

          return Stack(
            children: [
              // Background Image (Desktop Only)
              if (isDesktop)
                Positioned.fill(
                  child: Image.network(
                    'https://artworks.thetvdb.com/banners/v4/series/424536/posters/64e6a8b95dfad.jpg',
                    fit: BoxFit.cover,
                    errorBuilder: (context, error, stackTrace) {
                      return Container(color: const Color(0xFF000000));
                    },
                  ),
                ),
              // Dark Gradient Overlay (Desktop Only)
              if (isDesktop)
                Positioned.fill(
                  child: Container(
                    decoration: BoxDecoration(
                      gradient: LinearGradient(
                        begin: Alignment.topCenter,
                        end: Alignment.bottomCenter,
                        colors: [
                          Colors.black.withValues(alpha: 0.7),
                          Colors.black.withValues(alpha: 0.9),
                        ],
                      ),
                    ),
                  ),
                ),
              // Main Content
              isDesktop
                  ? _buildDesktopLayout(headingSize, descriptionSize, logoSize)
                  : _buildMobileLayout(headingSize, descriptionSize, logoSize),
            ],
          );
        },
      ),
    );
  }

  Widget _buildDesktopLayout(
      double headingSize, double descriptionSize, double logoSize) {
    return Stack(
      children: [
        // Angled Right Background
        Positioned(
          top: 0,
          bottom: 0,
          right: 0,
          width: MediaQuery.of(context).size.width * 0.45,
          child: ClipPath(
            clipper: _AngledClipper(),
            child: Container(
              color: const Color(0xFF0A0A0A),
            ),
          ),
        ),
        // Content
        Row(
          children: [
            // Left side - Promotional content
            Expanded(
              flex: 3,
              child: Container(
                padding: const EdgeInsets.symmetric(
                    horizontal: 60.0, vertical: 40.0),
                child: Row(
                  children: [
                    // Text Content
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          // Logo
                          ClipRRect(
                            borderRadius: BorderRadius.circular(6),
                            child: Image.network(
                              'https://kuudere.to/logo.png',
                              height: logoSize,
                              fit: BoxFit.contain,
                              errorBuilder: (context, error, stackTrace) {
                                return Container(
                                  width: logoSize,
                                  height: logoSize,
                                  decoration: BoxDecoration(
                                    color: Colors.white,
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  child: Icon(
                                    Icons.play_circle_outline,
                                    size: logoSize * 0.6,
                                    color: Colors.black,
                                  ),
                                );
                              },
                            ),
                          ),
                          const SizedBox(height: 40),
                          Text(
                            'Stream, Discover &\nDownload',
                            style: TextStyle(
                              color: Colors.white,
                              fontSize: headingSize,
                              fontWeight: FontWeight.w700,
                              height: 1.1,
                              letterSpacing: -1.5,
                            ),
                          ),
                          const SizedBox(height: 24),
                          Text(
                            'Find and download torrents, watch trailers, manage your list, search, browse and discover anime, watch together with friends and more, all in the same interface.',
                            style: TextStyle(
                              color: const Color(0xFF999999),
                              fontSize: descriptionSize,
                              height: 1.6,
                              fontWeight: FontWeight.w400,
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(width: 60),
                    // Anime Cards Carousel
                    Transform.rotate(
                      angle: 0.12,
                      child: SizedBox(
                        width: 150,
                        height: MediaQuery.of(context).size.height,
                        child: CarouselSlider.builder(
                          itemCount: _animeList.length,
                          carouselController: _carouselController,
                          options: CarouselOptions(
                            height: MediaQuery.of(context).size.height,
                            scrollDirection: Axis.vertical,
                            autoPlay: false,
                            viewportFraction: 0.35,
                            enableInfiniteScroll: true,
                            scrollPhysics: const NeverScrollableScrollPhysics(),
                          ),
                          itemBuilder: (context, index, realIndex) {
                            final anime = _animeList[index];
                            return Container(
                              margin: const EdgeInsets.symmetric(vertical: 8),
                              child: _AnimeCard(
                                imageUrl: anime['imageUrl']!,
                              ),
                            );
                          },
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            // Right side - Login form
            Expanded(
              flex: 2,
              child: Center(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.symmetric(
                      horizontal: 50.0, vertical: 32.0),
                  child: FadeTransition(
                    opacity: _fadeAnimation,
                    child: SlideTransition(
                      position: _slideAnimation,
                      child: ConstrainedBox(
                        constraints: const BoxConstraints(maxWidth: 400),
                        child: _buildLoginForm(centerText: true),
                      ),
                    ),
                  ),
                ),
              ),
            ),
          ],
        ),
      ],
    );
  }

  Widget _buildMobileLayout(
      double headingSize, double descriptionSize, double logoSize) {
    return Container(
      decoration: const BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topCenter,
          end: Alignment.bottomCenter,
          colors: [
            Color(0xFF0F0F0F),
            Color(0xFF000000),
          ],
        ),
      ),
      child: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(vertical: 32.0),
          child: FadeTransition(
            opacity: _fadeAnimation,
            child: SlideTransition(
              position: _slideAnimation,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Form Card Container
                  Padding(
                    padding: const EdgeInsets.symmetric(horizontal: 24.0),
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 500),
                      child: Container(
                        decoration: BoxDecoration(
                          color: const Color(0xFF0A0A0A),
                          borderRadius: BorderRadius.circular(16),
                          border: Border.all(
                            color: const Color(0xFF1A1A1A),
                            width: 1,
                          ),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.black.withValues(alpha: 0.3),
                              blurRadius: 20,
                              offset: const Offset(0, 10),
                            ),
                          ],
                        ),
                        padding: const EdgeInsets.all(24),
                        child: _buildLoginForm(centerText: false),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildLoginForm({bool centerText = true}) {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        centerText
            ? Center(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 300),
                  child: Text(
                    _isLogin ? 'Welcome back' : 'Create account',
                    key: ValueKey<bool>(_isLogin),
                    style: const TextStyle(
                      color: Colors.white,
                      fontSize: 26,
                      fontWeight: FontWeight.w700,
                      letterSpacing: -0.5,
                    ),
                  ),
                ),
              )
            : AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                child: Text(
                  _isLogin ? 'Welcome back' : 'Create account',
                  key: ValueKey<bool>(_isLogin),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 26,
                    fontWeight: FontWeight.w700,
                    letterSpacing: -0.5,
                  ),
                ),
              ),
        const SizedBox(height: 6),
        centerText
            ? Center(
                child: AnimatedSwitcher(
                  duration: const Duration(milliseconds: 300),
                  child: Text(
                    _isLogin
                        ? 'Sign in to continue watching'
                        : 'Join our streaming platform',
                    key: ValueKey<bool>(_isLogin),
                    style: const TextStyle(
                      color: Color(0xFF999999),
                      fontSize: 14,
                      fontWeight: FontWeight.w400,
                    ),
                  ),
                ),
              )
            : AnimatedSwitcher(
                duration: const Duration(milliseconds: 300),
                child: Text(
                  _isLogin
                      ? 'Sign in to continue watching'
                      : 'Join our streaming platform',
                  key: ValueKey<bool>(_isLogin),
                  style: const TextStyle(
                    color: Color(0xFF999999),
                    fontSize: 14,
                    fontWeight: FontWeight.w400,
                  ),
                ),
              ),
        const SizedBox(height: 28),
        Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              AnimatedSize(
                duration: const Duration(milliseconds: 300),
                curve: Curves.easeInOut,
                child: _isLogin
                    ? const SizedBox.shrink()
                    : Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          _CleanTextField(
                            controller: _displayNameController,
                            label: 'Full name',
                            hint: 'Enter your full name',
                            textInputAction: TextInputAction.next,
                          ),
                          const SizedBox(height: 16),
                        ],
                      ),
              ),
              _CleanTextField(
                controller: _emailController,
                label: 'Email',
                hint: 'Enter your email address',
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
              ),
              const SizedBox(height: 16),
              _CleanTextField(
                controller: _passwordController,
                label: 'Password',
                hint: 'Enter your password',
                isPassword: true,
                obscurePassword: _obscurePassword,
                onTogglePassword: () =>
                    setState(() => _obscurePassword = !_obscurePassword),
                textInputAction: TextInputAction.done,
                onSubmitted: (_) => _submitForm(),
              ),
              AnimatedSize(
                duration: const Duration(milliseconds: 300),
                curve: Curves.easeInOut,
                child: _isLogin
                    ? Column(
                        children: [
                          const SizedBox(height: 8),
                          Align(
                            alignment: Alignment.centerRight,
                            child: TextButton(
                              onPressed: () {
                                // Handle forgot password
                              },
                              style: TextButton.styleFrom(
                                padding:
                                    const EdgeInsets.symmetric(horizontal: 0),
                              ),
                              child: const Text(
                                'Forgot password?',
                                style: TextStyle(
                                  color: Colors.white,
                                  fontSize: 13,
                                  fontWeight: FontWeight.w500,
                                ),
                              ),
                            ),
                          ),
                        ],
                      )
                    : const SizedBox.shrink(),
              ),
              const SizedBox(height: 24),
              SizedBox(
                width: double.infinity,
                height: 48,
                child: ElevatedButton(
                  onPressed: _isLoading ? null : _submitForm,
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.white,
                    foregroundColor: Colors.black,
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(8),
                    ),
                    disabledBackgroundColor:
                        Colors.white.withValues(alpha: 0.6),
                  ),
                  child: _isLoading
                      ? LoadingAnimationWidget.threeArchedCircle(
                          color: Colors.black, size: 24)
                      : AnimatedSwitcher(
                          duration: const Duration(milliseconds: 300),
                          child: Text(
                            _isLogin ? 'Sign in' : 'Create account',
                            key: ValueKey<bool>(_isLogin),
                            style: const TextStyle(
                              fontSize: 15,
                              fontWeight: FontWeight.w700,
                              letterSpacing: 0.5,
                            ),
                          ),
                        ),
                ),
              ),
              const SizedBox(height: 20),
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  AnimatedSwitcher(
                    duration: const Duration(milliseconds: 300),
                    child: Text(
                      _isLogin
                          ? 'Don\'t have an account? '
                          : 'Already have an account? ',
                      key: ValueKey<bool>(_isLogin),
                      style: const TextStyle(
                        color: Color(0xFF999999),
                        fontSize: 13,
                      ),
                    ),
                  ),
                  TextButton(
                    onPressed: () {
                      setState(() => _isLogin = !_isLogin);
                    },
                    style: TextButton.styleFrom(
                      padding: const EdgeInsets.symmetric(horizontal: 4),
                      minimumSize: Size.zero,
                      tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                    ),
                    child: AnimatedSwitcher(
                      duration: const Duration(milliseconds: 300),
                      child: Text(
                        _isLogin ? 'Sign up' : 'Sign in',
                        key: ValueKey<bool>(_isLogin),
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.w600,
                          fontSize: 13,
                        ),
                      ),
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
}

class _CleanTextField extends StatefulWidget {
  final TextEditingController controller;
  final String label;
  final String hint;
  final bool isPassword;
  final TextInputType? keyboardType;
  final bool obscurePassword;
  final VoidCallback? onTogglePassword;
  final ValueChanged<String>? onSubmitted;
  final TextInputAction? textInputAction;

  const _CleanTextField({
    required this.controller,
    required this.label,
    required this.hint,
    this.isPassword = false,
    this.keyboardType,
    this.obscurePassword = false,
    this.onTogglePassword,
    this.onSubmitted,
    this.textInputAction,
  });

  @override
  State<_CleanTextField> createState() => _CleanTextFieldState();
}

class _CleanTextFieldState extends State<_CleanTextField> {
  final FocusNode _focusNode = FocusNode();
  bool _isFocused = false;

  @override
  void initState() {
    super.initState();
    _focusNode.addListener(() {
      setState(() {
        _isFocused = _focusNode.hasFocus;
      });
    });
  }

  @override
  void dispose() {
    _focusNode.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 14,
            fontWeight: FontWeight.w600,
            letterSpacing: 0.2,
          ),
        ),
        const SizedBox(height: 8),
        TextFormField(
          controller: widget.controller,
          focusNode: _focusNode,
          onFieldSubmitted: widget.onSubmitted,
          textInputAction: widget.textInputAction,
          obscureText: widget.isPassword ? widget.obscurePassword : false,
          keyboardType: widget.keyboardType,
          style: const TextStyle(
            color: Colors.white,
            fontSize: 15,
            fontWeight: FontWeight.w400,
          ),
          decoration: InputDecoration(
            hintText: widget.hint,
            hintStyle: const TextStyle(
              color: Color(0xFF666666),
              fontSize: 15,
              fontWeight: FontWeight.w400,
            ),
            suffixIcon: widget.isPassword
                ? IconButton(
                    icon: Icon(
                      widget.obscurePassword
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                      color:
                          _isFocused ? Colors.white : const Color(0xFF666666),
                      size: 20,
                    ),
                    onPressed: widget.onTogglePassword,
                  )
                : null,
            filled: true,
            fillColor: const Color(0xFF1A1A1A),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: const BorderSide(
                color: Color(0xFF333333),
                width: 1,
              ),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: const BorderSide(
                color: Color(0xFF333333),
                width: 1,
              ),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: const BorderSide(
                color: Colors.white,
                width: 1.5,
              ),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: const BorderSide(
                color: Color(0xFFE53935),
                width: 1,
              ),
            ),
            focusedErrorBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(6),
              borderSide: const BorderSide(
                color: Color(0xFFE53935),
                width: 1.5,
              ),
            ),
            contentPadding:
                const EdgeInsets.symmetric(vertical: 12, horizontal: 14),
          ),
          validator: (value) {
            if (value?.isEmpty ?? true) return 'This field is required';
            return null;
          },
        ),
      ],
    );
  }
}

class _AnimeCard extends StatelessWidget {
  final String imageUrl;

  const _AnimeCard({
    required this.imageUrl,
  });

  @override
  Widget build(BuildContext context) {
    return ClipRRect(
      borderRadius: BorderRadius.circular(12),
      child: Image.network(
        imageUrl,
        width: double.infinity,
        height: 200,
        fit: BoxFit.cover,
        errorBuilder: (context, error, stackTrace) {
          return Container(
            width: double.infinity,
            height: 200,
            color: const Color(0xFF2A2A2A),
            child: const Icon(Icons.movie, color: Color(0xFF666666)),
          );
        },
      ),
    );
  }
}

class _AngledClipper extends CustomClipper<Path> {
  @override
  Path getClip(Size size) {
    // Angle matches the card rotation (0.12 rad) - Clockwise
    final double angle = 0.12;
    final double tanAngle = math.tan(angle);
    final double slantWidth = size.height * tanAngle;

    Path path = Path();
    // Create a shape that slants from top-right to bottom-left ( / )
    // The container is positioned on the right side.
    // We want the left edge to be slanted clockwise.

    path.moveTo(slantWidth, 0); // Top-left of the container (shifted right)
    path.lineTo(size.width, 0); // Top-right
    path.lineTo(size.width, size.height); // Bottom-right
    path.lineTo(0, size.height); // Bottom-left (starts at edge)
    path.close();

    return path;
  }

  @override
  bool shouldReclip(CustomClipper<Path> oldClipper) => false;
}
