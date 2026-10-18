<p align="center">
  <img src="icon.svg" width="120" alt="Sentüp logo"/>
</p>

<h1 align="center">Sentüp</h1>

<p align="center">
  <b>YouTube video ve müzik, tek Android uygulamasında.</b><br/>
  <b>YouTube video and music in one Android app.</b>
</p>

<p align="center">
  <a href="#-features"><img alt="Platform" src="https://img.shields.io/badge/platform-Android%2011%2B-3DDC84?logo=android&logoColor=white"/></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/badge/license-GPL--3.0-blue"/></a>
  <a href="https://github.com/caca1403/sentup-android/releases"><img alt="Release" src="https://img.shields.io/github/v/release/caca1403/sentup-android?label=release"/></a>
  <a href="#-build-it--kurulum"><img alt="Build" src="https://img.shields.io/badge/build-Gradle-informational?logo=gradle"/></a>
</p>

---

Sentüp, tam bir video oynatıcı ile tam bir müzik çaları tek uygulamada toplar.
Ana ekrandan mod değiştirirsin; akış, arama, kitaplık ve oynatıcı seni takip
eder: aynı abonelikler, aynı geçmiş, aynı indirmeler — istediğin gibi okursun.

> **Türkçe** aşağıda; İngilizce için [scroll down](#english).

---

## ✨ Features / Özellikler

- 🎵 **Müzik + video tek uygulamada** — tek dokunuşla mod değiştir, her şey senkron.
- 🏠 **Kişiselleştirilmiş ana ekran** — sana özel raflar, trendler, "yeniden dinleyin",
  "unutulan favoriler" ve daha fazlası (Türkçe başlıklarla).
- 📊 **Dinleme analizi** — ana ekran ve kitaplıkta gerçek grafikler: sanatçı/şarkı
  dağılımı için **donut** grafik, sıralı çubuklar ve son 7 gün aktivitesi.
- 🧠 **Akıllı karışımlar** — YouTube Music'in üretmediği listeler; bir karışımı
  uzun basıp **gerçek bir çalma listesine** dönüştürebilirsin.
- 📚 **Yeniden tasarlanmış Kitaplık** — tek akış, bölüm başlıkları, üstte hızlı
  atlama çipleri ve tek dokunuşla karıştırma.
- 🔎 **Kural tabanlı Keşfet** — kaynak (abonelikler, geçmiş, beğeniler, cihaz
  listeleri), gruplama, uzunluk ve tazelik kurallarını sen yazarsın; liste
  donup kalmaz, kanallarını takip ederek kendini günceller.
- 🔔 **Arka plan oynatma**, **indirme/çevrimdışı**, **resim-in-resim (PiP)**,
  **şarkı sözleri** ve görselleştirici.
- 👤 **Hesap desteği ve hesap değiştirici** — giriş isteğe bağlıdır, uygulama
  asla API anahtarı istemez.
- 🎨 **Özelleştirme** — tema/renk ve davranış ayarları.

## 📥 Kurulum / Install

En hızlı yol: [Releases](https://github.com/caca1403/sentup-android/releases)
sayfasından APK'yı indir ve kur (Android 11 / API 30 ve üzeri).

> Dağıtılan APK **debug** imzalıdır; kendi cihazın için yeterlidir.

## 🔨 Build it / Kurulum (kaynaktan)

```bash
# Gereksinim: JDK 17+, Android SDK (API 30+)
git clone https://github.com/caca1403/sentup-android.git
cd sentup-android
./gradlew assembleDebug
# APK -> app/build/outputs/apk/debug/
```

Android Studio ile de açabilirsin: dizini proje olarak açıp **Run**'a bas.

## 🗂️ Proje yapısı

| Yol | İçerik |
|---|---|
| `app/src/main/java/com/sentup/android/data` | Ayrıştırma, depolama, oturumlar, indirmeler |
| `app/src/main/java/com/sentup/android/ui` | Özellik bazında tüm ekranlar |
| `app/src/main/java/com/sentup/android/service` | Oynatma ve indirme servisleri |
| `docs` | Parçaların nasıl çalıştığına dair notlar |

---

## English

Sentüp keeps a full video player and a full music player in one app. Switch
modes from Home, and the feed, search, library and player follow along: the same
subscriptions, history and downloads, read as either.

- **Personalised home** with YouTube-style shelves, localised to Turkish.
- **Interactive listening analytics** — a real donut chart of who you listen to,
  ranked bars and a 7-day activity chart, on both Home and the Library.
- **Smart mixes** YouTube Music does not build; long-press one to keep it as an
  ordinary, saveable playlist.
- **Single-flow Library** with section headers, quick jump chips and shuffle.
- **Rule-based Explore** — pick sources (subscriptions, history, liked videos,
  device playlists), grouping, length and freshness; a list rebuilds itself as
  your channels publish instead of freezing a page of results. Channels are
  now chosen from a picker, not typed as a comma list.
- Background playback, downloads/offline, picture-in-picture, synced lyrics and a
  visualiser. Optional sign-in; the app never asks for an API key.

**Requirements:** Android 11 (API 30) or newer, JDK 17+, Android SDK.

```bash
./gradlew assembleDebug   # APK -> app/build/outputs/apk/debug/
```

## ⚖️ Licence

This program is free software, licensed under the **GNU General Public License
v3.0** — see [LICENSE](LICENSE). Third-party work it builds on is credited in
[THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). The in-app **Settings → About
→ Open-source licences** screen lists every bundled library.

## ⚠️ Disclaimer / Sorumluluk reddi

Sentüp **YouTube ile bağlantılı veya YouTube tarafından onaylanmış değildir.**
YouTube adını, markasını ve içeriğini kullanma hakları YouTube LLC'ye aittir.

**Amaç nedir?** Sentüp'ün amacı YouTube'a **tamamen farklı bir arayüz ve
deneyimle** erişmektir — *veri, hesap veya şifre çalmak değildir*. Uygulama
YouTube'un kendi uygulamalarından farklı olarak reklamsız/kişiselleştirilmiş bir
okuma sunar; **hiçbir veriyi üçüncü taraflara göndermez**, hiçbir API anahtarı
istemek ve indirdiklerini bir yere yüklemek gibi bir davranışı yoktur.
Oturumun (cookies) varsa yalnızca senin cihazında kalır, oturum açmak
isteğe bağlıdır.

Bu yazılım eğitim ve kişisel kullanım amaçlıdır; kullanımı YouTube Hizmet
Kullanım Şartları ve yürürlükteki yerel yasalara uyumlu olmalıdır. Bu uygulamayı
kullanmaktan doğan sorumluluk kullanıcıya aittir.

*Sentüp is not affiliated with or endorsed by YouTube. Its purpose is to offer a
different way to experience YouTube — **not** to harvest data, accounts or
credentials. It sends nothing to third parties, asks for no API key and keeps
your session (if any) on your own device; signing in is optional. Use it in
accordance with YouTube's Terms of Service and your local laws.*

## 🤝 Katkı / Contributing

Katkıyı memnuniyetle karşılarım. [CONTRIBUTING.md](CONTRIBUTING.md) ve güvenlik
için [SECURITY.md](SECURITY.md) dosyalarına bak.
