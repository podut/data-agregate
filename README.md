# DataAgregate PRO 🚀
### Ecosystem de Agregare Știri bazat pe AI (Gemini & DeepSeek)

DataAgregate este o platformă completă de agregare și procesare a știrilor, formată dintr-o aplicație mobilă Android modernă și un backend Dockerizat de înaltă performanță.

<p align="center">
  <a href="https://github.com/podut/data-agregate/releases/download/v1.0.5/DataAgregate-v1.0.5.apk">
    <img src="https://img.shields.io/badge/⬇️%20Download%20APK-v1.0.5-28b485?style=for-the-badge&logo=android&logoColor=white" alt="Download APK"/>
  </a>
</p>

> **Instalare:** Setări → Securitate → Permite surse necunoscute

---

## 🏗️ Arhitectură Sistem

Sistemul este construit pe o infrastructură **Client-Server** robustă:

1.  **Android Client**: Aplicație modulară (Kotlin/Compose) care consumă date procesate.
2.  **Backend API**: Microserviciu FastAPI care orchestrează colectarea și ranking-ul.
3.  **Data Layer**:
    *   **PostgreSQL**: Stocare persistentă pentru utilizatori, surse și articole.
    *   **Redis**: Caching, Deduplicare și Locking distribuit.
    *   **Docker Compose**: Orchestrarea întregului stack.

---

## ✨ Funcționalități Cheie

### 🤖 Algoritm AI în Cascadă (Fallback Cascade)
Motorul de analiză folosește o ierarhie de modele pentru a garanta disponibilitatea:
*   **Gemini 2.0 Flash**: Modelul principal pentru viteză și precizie.
*   **Gemini 1.5 Flash/Pro**: Fallback în caz de saturație.
*   **DeepSeek Chat**: Alternativă externă pentru reziliență totală.
*   **Local Processor**: Algoritm de rezervă fără costuri API.

### 📱 Android - Arhitectură Modulară Granulară
Proiectul este împărțit în module de tip `feature` pentru scalabilitate:
*   `:feature:news_feed`: Feed-ul principal rankat prin AI.
*   `:feature:explore`: Descoperire de noi interese.
*   `:feature:saved`: Gestionarea articolelor offline/favorite.
*   `:feature:categories`: Managementul surselor RSS direct de pe server.
*   **Optimizare Foldables**: Layout specializat cu *Navigation Rail* pentru Z Fold 4.

### 🔌 Integrare Server-Side Sync
*   **Single Source of Truth**: Sursele și interesele sunt salvate în PostgreSQL și sincronizate automat pe orice dispozitiv.
*   **Background Sync**: Algoritmul caută știri noi chiar și când aplicația este închisă.
*   **Global Sources**: Acces instant la surse de încredere din România (Digi24, Biziday, StartupCafe).

---

## 🛠️ Stack Tehnologic

| Componentă | Tehnologii |
| :--- | :--- |
| **Backend** | Python 3.11, FastAPI, SQLAlchemy, APScheduler |
| **Bază de Date** | PostgreSQL 16, Redis 7 (Alpine) |
| **Android** | Kotlin, Jetpack Compose, Hilt, Ktor Client, Room, Coil |
| **DevOps** | Docker, Docker Compose, Port Forwarding (8085) |
| **AI Providers** | Google Generative AI, DeepSeek API |

---

## 🚀 Instalare și Pornire

### 1. Backend (Docker)
1.  Asigură-te că ai un fișier `.env` în root cu cheile tale API.
2.  Rulează comanda:
    ```bash
    docker-compose up --build -d
    ```
3.  API-ul va fi disponibil la: `http://localhost:8085`

### 2. Android
1.  Configurează `local.properties` cu URL-ul serverului:
    ```properties
    API_BASE_URL=http://pixcode.go.ro:8085
    ```
2.  Build & Install:
    ```bash
    ./gradlew :app:installDevDebug
    ```

---

## 📊 Structura Bazei de Date
*   `user_profile`: Preferințe și interese per dispozitiv.
*   `rss_sources`: Lista globală și per-user a surselor RSS.
*   `pending_articles`: Rezervorul de știri procesate de AI, cu scoruri și tag-uri.
*   `device_seen`: Istoricul de citire pentru deduplicare inteligentă.

---

## 🔒 Securitate și Performanță
*   **Distributed Lock**: Previne procesarea dublă a feed-urilor în Docker.
*   **Rate Limiting**: Protejează API-ul de abuzuri prin Redis.
*   **Multi-stage Build**: Imaginile Docker sunt optimizate pentru viteză și amprentă redusă pe disc.

---
*Creat și optimizat de Gemini CLI în YOLO mode.*
