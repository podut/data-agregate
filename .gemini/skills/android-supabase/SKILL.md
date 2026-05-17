---
name: android-supabase
description: Ghid pentru integrarea Supabase în aplicații Android/Kotlin folosind Supabase-kt SDK.
---

# Supabase Kotlin Integration

Acest skill oferă instrucțiuni procedurale pentru configurarea și utilizarea Supabase în proiecte Android, respectând arhitectura Clean și bunele practici de securitate.

## Când să utilizezi acest skill
- Când implementezi autentificarea (Email, Google, etc.).
- Când lucrezi cu baze de date PostgreSQL via PostgREST.
- Când stochezi fișiere în Supabase Storage.
- Când ai nevoie de date în timp real (Realtime).

## Configurare Proiect (Gradle)
Pentru a activa Supabase, adaugă următoarele în `build.gradle.kts` (app):

```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.0"
}

dependencies {
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.ktor:ktor-client-android:3.0.0")
}
```

## Inițializarea Clientului (Dagger Hilt)
Recomandare: Injectează clientul ca Singleton.

```kotlin
@Provides
@Singleton
fun provideSupabaseClient(): SupabaseClient {
    return createSupabaseClient(
        supabaseUrl = "https://tau.supabase.co",
        supabaseKey = "ANON_KEY"
    ) {
        install(Postgrest)
        install(Auth)
    }
}
```

## Referințe Module SDK (Endpoints Specifice)
Utilizează aceste linkuri pentru a găsi sintaxa exactă și metodele disponibile pentru fiecare modul:

- **[Instalare și Configurare](https://supabase.com/docs/reference/kotlin/installing):** Pașii de bază și dependințe.
- **[Auth / GoTrue](https://supabase.com/docs/reference/kotlin/auth):** Gestionarea utilizatorilor, sesiunilor și autentificării sociale.
- **[Database / PostgREST](https://supabase.com/docs/reference/kotlin/database):** Interogări, filtrări, inserări și actualizări de date.
- **[Realtime](https://supabase.com/docs/reference/kotlin/realtime):** Ascultarea schimbărilor din baza de date în timp real via WebSockets.
- **[Edge Functions](https://supabase.com/docs/reference/kotlin/functions):** Apelarea funcțiilor serverless scrise în TypeScript.
- **[Storage](https://supabase.com/docs/reference/kotlin/storage):** Încărcarea, descărcarea și gestionarea fișierelor media.

## Reguli Procedurale și Securitate
1. **Row Level Security (RLS):** Activează întotdeauna RLS în dashboard-ul Supabase. Nu trimite niciodată `service_role` key în codul aplicației.
2. **Serializare:** Modelele de date trebuie adnotate cu `@Serializable` (Kotlin Serialization).
3. **Arhitectură:** Implementează apelurile Supabase în stratul de **Data (RepositoryImpl)**. Nu apela clientul direct din ViewModel sau UI.
4. **Error Handling:** Folosește blocuri `try-catch` pentru a captura excepțiile de rețea sau de autentificare specifice SDK-ului.

## Resurse Esențiale
- **[Supabase-kt GitHub](https://github.com/jan-tennert/supabase-kt):** SDK-ul oficial pentru Kotlin.
- **[Documentation](https://supabase.com/docs/reference/kotlin/start):** Ghidul oficial de referință.
- **[Auth Examples](https://supabase.com/docs/guides/auth/auth-helpers/kotlin):** Gestionarea sesiunilor și a utilizatorilor.

---

## Instrucțiuni pentru AI (Agent)
Atunci când acest skill este activat:
1. **Schema Mapping:** Verifică dacă tabelele din Supabase corespund cu modelele `@Serializable` din Kotlin.
2. **UDF Flow:** Asigură-te că datele primite de la Supabase sunt transformate (mapped) în modele de domain înainte de a ajunge în UI.
3. **Auth Checks:** Verifică starea sesiunii înainte de a efectua operațiuni protejate.
