# Vision Download (V-Download) — Android App

YouTube, Facebook এবং TikTok-এর ভিডিও HD কোয়ালিটিতে (TikTok-এ ওয়াটারমার্ক ছাড়া) ডাউনলোড করার জন্য একটি সম্পূর্ণ Android Studio প্রজেক্ট। এটি [youtubedl-android](https://github.com/yausername/youtubedl-android) (`yt-dlp` + `FFmpeg`) লাইব্রেরি ব্যবহার করে।

## প্রধান ফিচারসমূহ
- **YouTube, Facebook ও TikTok সাপোর্ট**: সাধারণ ভিডিও, YouTube Shorts, Facebook Reels এবং TikTok লিংক পেস্ট করলেই প্ল্যাটফর্ম অটো-ডিটেক্ট হয়।
- **HD কোয়ালিটি ও ওয়াটারমার্ক-মুক্ত ডাউনলোড**: `yt-dlp` ও `FFmpeg`-এর মাধ্যমে YouTube ও Facebook-এর আলাদা HD ভিডিও+অডিও স্ট্রিম মার্জ করে এবং TikTok-এর ওয়াটারমার্ক-মুক্ত স্ট্রিম MP4 হিসেবে সেভ করে।
- **কোয়ালিটি অপশন**: Best HD (সর্বোচ্চ Available), Full HD 1080p, HD 720p (MP4) এবং শুধুমাত্র অডিও (MP3)।
- **সরাসরি শেয়ার (Share Intent)**: YouTube, Facebook বা TikTok অ্যাপ থেকে সরাসরি `Share -> V-Download` সিলেক্ট করে ডাউনলোড করা যাবে।
- **গ্যালারিতে অটো সেভ**: ভিডিও `Downloads/VisionDownload/` ফোল্ডারে সেভ হয় এবং সাথে সাথে ফোনের গ্যালারিতে দেখা যায়।
- **ইন-অ্যাপ ইঞ্জিন আপডেট**: কোনো প্ল্যাটফর্ম তাদের ফরম্যাট বদলালে অ্যাপের ভেতর থেকেই `yt-dlp` আপডেট করা যাবে।

## কীভাবে Android Studio দিয়ে APK বানাবেন
1. এই `.zip` ফাইলটি আপনার কম্পিউটারে Extract (Unzip) করুন।
2. [Android Studio](https://developer.android.com/studio) ওপেন করে **Open** বাটনে ক্লিক করুন এবং `VisionDownload` ফোল্ডারটি সিলেক্ট করুন।
3. Gradle Sync শেষ হওয়া পর্যন্ত অপেক্ষা করুন (ইন্টারনেট সংযোগ প্রয়োজন হবে)।
4. মেনু বার থেকে **Build -> Build Bundle(s) / APK(s) -> Build APK(s)** ক্লিক করুন।
5. বিল্ড শেষ হলে `app/build/outputs/apk/debug/app-debug.apk` ফাইলটি আপনার অ্যান্ড্রয়েড ফোনে নিয়ে ইনস্টল করুন।


## কীভাবে GitHub Actions দিয়ে (Android Studio ছাড়াই) APK বানাবেন
1. GitHub-এ একটি নতুন Repository তৈরি করুন (যেমন `VisionDownload`)।
2. এই `.zip` ফাইলটি আনজিপ করে ভেতরের সব ফাইল (`.github` ফোল্ডারসহ) আপনার Repository-র রুট ফোল্ডারে আপলোড/পুশ করুন।
3. Repository-র **Actions** ট্যাবে যান -> **Build Vision Download APK** সিলেক্ট করে **Run workflow** চাপুন (অথবা পুশ করলেই অটো শুরু হবে)।
4. বিল্ড সফল হলে নিচে **Artifacts** সেকশন থেকে `VisionDownload-APKs` ডাউনলোড করে আনজিপ করুন।
5. সাধারণ আধুনিক অ্যান্ড্রয়েড ফোনের জন্য `app-arm64-v8a-debug.apk` অথবা যেকোনো ফোনের জন্য `app-universal-debug.apk` ইনস্টল করুন।

## গুরুত্বপূর্ণ নোট
- ব্যক্তিগত ব্যবহার বা নিজের কন্টেন্ট ব্যাকআপের জন্য ব্যবহার করুন এবং সংশ্লিষ্ট প্ল্যাটফর্মের নীতিমালা ও কপিরাইট আইন মেনে চলুন।
