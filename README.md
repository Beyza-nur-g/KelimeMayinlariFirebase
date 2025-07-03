# 🧠 Kelime Mayınları - Firebase Tabanlı Çift Oyunculu Kelime Oyunu  
### 🧩 Kelime stratejisi + mayınlar + ödüller + gerçek zamanlı eşleşme  

A real-time, multiplayer word game built with **Firebase**, **Jetpack Compose**, and **Kotlin**.  
Players compete in a Scrabble-style game enriched with **bonus** and **trap** tiles, matching based on chosen duration (2 min, 5 min, 12h, 24h). The game includes **word validation**, **score multipliers**, and **strategic game mechanics** such as **jokers**, **bombs**, and **live synchronization**.

---

## 📌 Genel Özellikler | Key Features

- ⚙️ Android & Firebase tabanlı mobil oyun  
- 🔁 Gerçek zamanlı Firestore senkronizasyonu  
- 👥 2 oyunculu çevrimiçi eşleşme sistemi  
- ⏳ Süre seçimine göre eşleşme (2dk, 5dk, 12sa, 24sa)  
- 🔡 Scrabble benzeri 15x15 oyun tahtası  
- 🧨 Mayın (ceza) sistemleri: puan transferi, kelime iptali, vb.  
- 🎁 Joker ödül sistemleri: ekstra hamle, bölge yasaklama, vb.  
- 📚 Türkçe kelime listesi ile kelime doğrulama  
- 🎨 Jetpack Compose ile modern UI

---

## 🛠 Kullanılan Teknolojiler | Tech Stack

| Katman         | Kullanılan Teknoloji              |
|----------------|-----------------------------------|
| Uygulama       | Kotlin, Jetpack Compose           |
| Backend        | Firebase Firestore, Authentication|
| Senkronizasyon | Snapshot Listeners (Firestore)    |
| Doğrulama      | Türkçe kelime listesi (HashSet)   |
| Yapılar        | GameViewModel, Room, MapScreen    |

---

## 🧠 Oyun Mekanikleri | Game Mechanics

- 🟩 **Oyun tahtası**: 15x15 matris, H2/H3 (harf çarpanı), K2/K3 (kelime çarpanı), STAR (başlangıç)
- 🔤 **Harf havuzu**: 100 harf, oyuncuya 7'şer harf
- ✅ **Kelime doğrulama**: temas kontrolü, hizalanma, yıldız teması, geçerlilik kontrolü
- 💣 **Mayınlar**: 
  - Puan transferi
  - Puan bölünmesi
  - Harf kaybı
  - Ekstra hamle engeli
  - Kelime iptali
- 🎁 **Ödüller**:
  - Bölge yasağı (rakip o alana yazamaz)
  - Harf yasağı (rakip harfleri dondurulur)
  - Ekstra hamle hakkı
- 🃏 **Joker harf** seçimi ve geçici tanımlama
- 🛑 **Oyun sonlandırma**: teslim, pas geçme, süre aşımı, harf kalmaması

---

## 🚀 Kurulum ve Çalıştırma | Setup & Run

### 1. Firebase Bağlantısı:
- Firestore ve Authentication yapılandırılmalı
- `google-services.json` dosyasını `/app` klasörüne yerleştir

### 2. Android Studio ile Aç:
```bash
File > Open > KelimeMayinlariFirebase/
