# AliExpress Media Downloader

This project is a full Android starter app built with Kotlin and Jetpack Compose for downloading images and videos from AliExpress product pages.

## Features included
- Modern dark-mode Compose interface
- Paste product URL and fetch media
- Circular loading indicator during scraping
- Media grid with selection, select-all, and multiple download actions
- Scoped storage + MediaStore image/video saving
- Notification on completion
- MVVM + StateFlow architecture
- Retrofit + Jsoup parsing for AliExpress product pages
- Coil for image caching and rendering

## Structure
- `app/src/main/java/com/fawaz/aliexpressdownloader/MainActivity.kt`
- `app/src/main/java/com/fawaz/aliexpressdownloader/ui/ProductMediaScreen.kt`
- `app/src/main/java/com/fawaz/aliexpressdownloader/ui/viewmodel/ProductMediaViewModel.kt`
- `app/src/main/java/com/fawaz/aliexpressdownloader/data/api/AliExpressApiService.kt`
- `app/src/main/java/com/fawaz/aliexpressdownloader/data/repository/AliExpressRepository.kt`

## How to run
1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the app on a device or emulator.

## Important note
AliExpress page HTML and media structures change often. This parser uses best-effort HTML and script extraction, and the app is designed to work as a robust starting point for production scraping.

## Next upgrade ideas
- Add a real product URL validation layer
- Add a dedicated download worker
- Add background downloads with progress callbacks
- Add support for review gallery extraction and explicit video parsing
- Improve URL normalization for AliExpress CDN and image quality selection
