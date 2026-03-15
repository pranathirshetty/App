# 🚀 Kuudere Flutter App

A cross-platform Flutter application for the Kuudere anime streaming platform.

## 📱 Features

- Browse and search anime
- Watch anime episodes
- Manage watchlist
- View schedule
- User authentication with session-based cookies
- Real-time notifications
- Comments and interactions

## 🔑 Authentication

The app uses session-based authentication. Users can:

- Register a new account
- Login with email/username and password
- Session is automatically stored and used for authenticated requests

**No API key required!** The app works with the SvelteKit backend using session cookies.

## 🚀 Getting Started

1. Clone the repository
2. Install dependencies: `flutter pub get`
3. Run the app: `flutter run`

## 📜 API Endpoints

The app connects to the SvelteKit backend at `https://anime.anisurge.qzz.io`. All endpoints use session cookies for authentication when required.

For any issues, join our **[Discord](https://discord.gg/h9v9Vfzp7B)**.
