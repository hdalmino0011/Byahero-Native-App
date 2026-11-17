# BYAHERO — by HDDEV

Generous mobile ride-hailing and location discovery application built with:
- **Native Android App (`/android`)**: Kotlin, Jetpack Compose, Gradle (Kotlin DSL), and a C++17 JNI Native Engine (`byahero_native.cpp`) for fast Haversine distance, Saturday 10% + 5% rider fare math, and polyline decoding.
- **GitHub Actions APK Pipeline (`/.github/workflows/build-apk.yml`)**: Automatically compiles the Kotlin + C++ Android project on push and publishes `byahero-debug-apk` as a downloadable GitHub Actions Artifact.
- **Interactive Web & Mobile Preview (`/src`)**: React + TypeScript + Tailwind CSS preview styled in Dark Green, White, and Black with Samsung Sans typography, vector-only graphics, live Google Maps Platform integration, and Supabase Auth/Realtime support.
- **Supabase Database Schema (`/supabase/schema.sql`)**: PostgreSQL + Realtime schema for profiles, flexible Passenger/Rider/Both role switching, and live ride synchronization across devices.

## Key Features
- **Motorcycle & Taxi Booking**: Instant pickup & destination entry via Google Places Autocomplete, GPS lock, or map tap.
- **Saturday Generosity**: Every Saturday gives passengers an automatic **10% discount** while riders earn **+5% extra incentive** on every successful ride.
- **Nearby Fast Food & Resorts Discovery**: Real-time Google Maps Places search for nearby fast food chains (McDonald's, Jollibee, KFC, etc.), resorts, beaches, or any custom location.
- **Google Account & Role Switching**: Register as Passenger, Rider, or Both and switch modes seamlessly.

## GitHub Actions & Secrets Setup
1. Push this repository to GitHub (`main` or `master` branch).
2. Optional repository secrets under **Settings -> Secrets and variables -> Actions**:
   - `MAPS_API_KEY`
   - `SUPABASE_URL`
   - `SUPABASE_ANON_KEY`
   - `GOOGLE_WEB_CLIENT_ID`
3. Open the **Actions** tab on GitHub, select **Build BYAHERO Android APK**, and download the **`byahero-debug-apk`** artifact.
