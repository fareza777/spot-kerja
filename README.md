# Spotkerja

Aplikasi Android untuk menemukan **posisi meja kerja terbaik** di dalam ruangan berdasarkan sensor HP. 100% lokal — tanpa backend, tanpa LLM, tanpa internet wajib (ping diarahkan ke gateway/router lokal).

## Fitur

- **Scan multi-spot**: ukur Spot A/B/C (bisa ditambah sampai F) masing-masing 30–60 detik.
- **Work Spot Score 0–100** dari kombinasi metrik:
  - Kualitas Wi-Fi (RSSI, link speed, band)
  - Ping / jitter / packet loss ke gateway lokal
  - Cahaya (lux) + stabilitasnya
  - Kebisingan (dB relatif via mikrofon)
  - Orientasi hadap vs arah matahari (estimasi risiko silau, offline — algoritme posisi matahari)
  - Sinyal seluler (opsional, bila tersedia)
- **Mode dengan bobot berbeda**: Work, Study, Gaming, Video Call
- **Perbandingan antar-spot** + rekomendasi Best Spot
- **History** hasil scan (JSON lokal) + **share/export** (teks + CSV via FileProvider)
- **Material 3 dark**, ringan, hemat baterai (sampling interval longgar, sensor NORMAL delay)
- Semua angka = **estimasi praktis**, bukan pengukuran ilmiah (disclaimer di UI & export)

## Stack

Kotlin · Jetpack Compose (Material 3) · Navigation-Compose · coroutines/StateFlow · kotlinx.serialization (history lokal) · ViewModel. Tidak ada library chart — grafik digambar via Canvas agar AAB tetap kecil.

## Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # unit test scoring engine
./gradlew bundleRelease        # AAB release
```

### Signing release

Salin `keystore.properties.example` → `keystore.properties`, letakkan `release.jks` di root project, lalu `./gradlew bundleRelease`. Tanpa file itu, release build tetap jalan namun AAB unsigned (perlu `apksigner`/`jarsigner` atau upload key di Play Console).

### Generate keystore baru (contoh)

```bash
keytool -genkey -v -keystore release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias spotkerja
```

## Izin yang diminta

| Izin | Untuk |
|---|---|
| ACCESS_FINE/COARSE_LOCATION | Wi-Fi scan info, cell info, posisi matahari |
| RECORD_AUDIO | Pengukuran noise (opsional — ditolak pun app tetap jalan) |
| INTERNET, NETWORK/WIFI_STATE | Status koneksi; ping ke gateway lokal |

## Catatan akurasi

- dB noise adalah level relatif (dBFS + offset), bukan SPL terkalibrasi.
- Ping mengukur LAN ke router — proxy bagus untuk kualitas Wi-Fi tanpa internet.
- Risiko silau memakai azimuth HP vs posisi matahari aproksimasi; hanya saat matahari di atas horizon.
