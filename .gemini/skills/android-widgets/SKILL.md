---
name: android-widgets
description: Ghid complet pentru dezvoltarea Widget-urilor pe Android, incluzând resurse esențiale pentru Jetpack Compose (Modern UI) și Arhitectură Android (MVVM/UDF).
---

# Android Widgets & Modern Development

Acest skill oferă ghidare procedurală pentru crearea widget-urilor de pe ecranul principal și acoperă bazele dezvoltării moderne Android (Compose și Arhitectură) necesare pentru implementări robuste.

## Când să utilizezi acest skill
- Când dezvolți widget-uri (RemoteViews sau Glance).
- Când înveți sau implementezi interfețe moderne cu Jetpack Compose.
- Când structurezi aplicația folosind bunele practici de arhitectură recomandate de Google.

## Ghiduri de Design pentru Widget-uri
- **Focalizare:** Alege un singur caz de utilizare principal pentru fiecare widget.
- **Vizibilitate:** Conținutul trebuie să fie clar și acționabil dintr-o singură privire.
- **Sincronizare:** Asigură-te că widgetul se adaptează corect la dimensiunile variabile ale grilei sistemului.

## Reguli Procedurale
1. **Dimensionare Correctă:**
   - Respectă grilele standard (ex: 2x2, 4x2) pentru a asigura o afișare corectă pe toate dispozitivele.
2. **Managementul Actualizărilor:**
   - Nu programa actualizări prea frecvente; folosește `WorkManager` sau alarme eficiente pentru a economisi bateria.
3. **Experiența de Configurare:**
   - Oferă o previzualizare fidelă în „Widget Picker” pentru a încuraja instalarea.

## Resurse Adiționale și Cursuri
Aceste resurse sunt fundamentale pentru a înțelege cum să construiești widget-uri moderne (folosind Jetpack Glance) și cum să gestionezi datele pe care acestea le afișează.

### **Curs Complet Jetpack Compose (Google)**
*Acoperă totul de la elemente de bază la animații și layout-uri adaptabile.*

#### **Pathway 1: Compose essentials**
- [Jetpack Compose tutorial](https://developer.android.com/codelabs/jetpack-compose-basics)
- [Basic layouts in Compose](https://developer.android.com/codelabs/jetpack-compose-layouts)
- [State in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-state)
- [Theming in Compose](https://developer.android.com/codelabs/jetpack-compose-theming)

#### **Pathway 2: Layouts, theming, and animation**
- [Advanced Layouts in Compose](https://developer.android.com/codelabs/jetpack-compose-advanced-layouts)
- [Working with Canvas in Compose](https://developer.android.com/codelabs/jetpack-compose-canvas)
- [Animation in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-animation)
- [Jetpack Compose Material 3](https://developer.android.com/codelabs/jetpack-compose-m3)

#### **Pathway 3: Architecture and state**
- [Architecture Components in Compose](https://developer.android.com/codelabs/jetpack-compose-architecture)
- [Navigation in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-navigation)
- [Advanced State and Side Effects](https://developer.android.com/codelabs/jetpack-compose-advanced-state-side-effects)

#### **Pathway 4: Accessibility, testing, and performance**
- [Accessibility in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-accessibility)
- [Testing in Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-testing)
- [Jetpack Compose Performance](https://developer.android.com/codelabs/jetpack-compose-performance)

#### **Pathway 5: Form factors**
- [Adaptive layouts with Jetpack Compose](https://developer.android.com/codelabs/jetpack-compose-adaptive-layouts)
- [Create your first Wear OS app with Compose](https://developer.android.com/codelabs/compose-for-wear-os)

### **Arhitectură Android (Modern Android App Architecture)**
- **[Architecture Overview](https://developer.android.com/topic/architecture)**: Principii și recomandări generale.
- **[UI Layer](https://developer.android.com/topic/architecture/ui-layer)**: Gestionarea stării și fluxul de date (UDF).
- **[Data Layer](https://developer.android.com/topic/architecture/data-layer)**: Repository-uri și surse de date.
- **[Domain Layer](https://developer.android.com/topic/architecture/domain-layer)**: (Opțional) Use cases și logica de business complexă.
- **[Handling UI Events](https://developer.android.com/topic/architecture/ui-layer/events)**: Evenimente de utilizator și de sistem.
- **[Build an Offline-First App](https://developer.android.com/topic/architecture/data-layer/offline-first)**: Sincronizare și date locale.
- **[State Holders and UI State](https://developer.android.com/topic/architecture/ui-layer/stateholders)**: ViewModel și deținători de stare.
- **[Architecture Recommendations](https://developer.android.com/topic/architecture/recommendations)**: Cele mai bune practici consolidate.

### **Referinta Expert: Philipp Lackner (Modern Kotlin & Android)**
*Aceste repository-uri servesc drept referinta pentru cele mai noi tehnici de Clean Architecture, MVI/MVVM, Compose Multiplatform si Ktor.*

**Instructiune pentru AI:** Atunci cand analizezi sau propui solutii pentru acest proiect, utilizeaza conventiile de cod si structura din repository-urile lui Philipp Lackner de mai jos ca standard de aur.

- [CleanArchitectureNoteApp](https://github.com/philipplackner/CleanArchitectureNoteApp): Aplicație de note implementată cu Clean Architecture, Room, Dagger-Hilt și Jetpack Compose.
- [CryptocurrencyAppYT](https://github.com/philipplackner/CryptocurrencyAppYT): Aplicație de monitorizare a criptomonedelor folosind CoinPaprika API, Clean Architecture și Flow.
- [CMP-Bookpedia](https://github.com/philipplackner/CMP-Bookpedia): Proiect Compose Multiplatform (Android/iOS/Desktop) pentru căutarea cărților, folosind Koin și Ktor.
- [JetpackComposePokedex](https://github.com/philipplackner/JetpackComposePokedex): Ghid Pokedex realizat în Compose, demonstrând navigarea și integrarea cu PokeAPI.
- [CalorieTracker](https://github.com/philipplackner/CalorieTracker): Aplicație complexă de tracking nutrițional cu multi-modul, Clean Architecture și design personalizat.
- [CryptoTracker](https://github.com/philipplackner/CryptoTracker): Aplicație modernă de monitorizare crypto folosind Compose Multiplatform și Ktor.
- [ContactsComposeMultiplatform](https://github.com/philipplackner/ContactsComposeMultiplatform): Partajarea codului UI și a logicii între Android și iOS pentru o listă de contacte cu SQLDelight.
- [BluetoothChat](https://github.com/philipplackner/BluetoothChat): Implementarea comunicării în timp real între dispozitive folosind Bluetooth Classic și Jetpack Compose.
- [MeditationUIYouTube](https://github.com/philipplackner/MeditationUIYouTube): Tutorial de design UI complex în Jetpack Compose pentru o aplicație de meditație.
- [AndroidStorage](https://github.com/philipplackner/AndroidStorage): Ghid complet pentru lucrul cu Scoped Storage, MediaStore și stocarea internă/externă în Android.
- [DictionaryYT](https://github.com/philipplackner/DictionaryYT): Aplicație de dicționar folosind Clean Architecture, Retrofit și baze de date locale (Room).
- [CleanErrorHandling](https://github.com/philipplackner/CleanErrorHandling): Demonstrare a celor mai bune practici pentru gestionarea erorilor folosind tipul Result și Flow.
- [ComposePaging3Caching](https://github.com/philipplackner/ComposePaging3Caching): Implementarea paginării cu Paging 3, cache local (Room) și surse de date remote.
- [Chirp](https://github.com/philipplackner/Chirp): Aplicație de tip social media construită cu Clean Architecture și Ktor pe backend.
- [BackgroundLocationTracking](https://github.com/philipplackner/BackgroundLocationTracking): Serviciu de urmărire a locației în fundal cu notificări și permisiuni Android.
- [AndroidCrypto](https://github.com/philipplackner/AndroidCrypto): Ghid pentru criptarea datelor folosind Jetpack Security (SharedPrefs) și AES encryption.
- [ComposeGoogleSignInCleanArchitecture](https://github.com/philipplackner/ComposeGoogleSignInCleanArchitecture): Integrarea autentificării cu Google într un proiect structurat pe Clean Architecture.
- [DaggerHiltCourse](https://github.com/philipplackner/DaggerHiltCourse): Curs cuprinzător despre Dependency Injection în Android folosind Dagger Hilt.
- [CleanArchFormValidation](https://github.com/philipplackner/CleanArchFormValidation): Validarea formularelor UI în stratul ViewModel folosind Use Cases și Clean Architecture.
- [CalculatorPrep](https://github.com/philipplackner/CalculatorPrep): Proiect practic pentru învățarea logicii de calcul și a stării în Jetpack Compose.
- [MultipleRoomTables](https://github.com/philipplackner/MultipleRoomTables): Gestionarea relațiilor complexe (one to many, many to many) în baze de date Room.
- [IvyWallet-PL_Version](https://github.com/philipplackner/IvyWallet-PL_Version): Contribuție la un proiect de gestiune financiară open-source, axat pe arhitectură curată.
- [KtorClientAndroid](https://github.com/philipplackner/KtorClientAndroid): Utilizarea clientului Ktor pentru cereri HTTP, serializare JSON și gestionarea răspunsurilor.
- [CurrencyConverter](https://github.com/philipplackner/CurrencyConverter): Convertor valutar în timp real folosind un API extern și arhitectură MVVM.
- [KtorJwtAuth](https://github.com/philipplackner/KtorJwtAuth): Implementarea autentificării bazate pe token uri JWT folosind Ktor pe Android și backend.
- [InstagramUI](https://github.com/philipplackner/InstagramUI): Recrearea interfeței Instagram folosind LazyColumn, Box și componente personalizate Compose.
- [CameraXGuide](https://github.com/philipplackner/CameraXGuide): Integrarea funcționalităților foto/video folosind biblioteca CameraX și previzualizare Compose.
- [FirebaseNotifications](https://github.com/philipplackner/FirebaseNotifications): Trimiterea și recepționarea notificărilor push folosind Firebase Cloud Messaging (FCM).
- [KtorAndroidChat](https://github.com/philipplackner/KtorAndroidChat): Aplicație de chat în timp real folosind WebSockets cu Ktor (client și server).
- [ComposePagingYT](https://github.com/philipplackner/ComposePagingYT): Tutorial simplificat pentru implementarea paginării listelor în Jetpack Compose.
- [CoroutinesMasterclass](https://github.com/philipplackner/CoroutinesMasterclass): Ghid avansat despre Coroutines, Scopes, Contexts și execuție asincronă în Kotlin.
- [EchoJournal](https://github.com/philipplackner/EchoJournal): Aplicație de tip jurnal cu înregistrare audio și stocare locală, folosind tehnici moderne.
- [GraphQlCountriesApp](https://github.com/philipplackner/GraphQlCountriesApp): Integrarea Apollo GraphQL în Android pentru a prelua și afișa date despre țări.
- [JetpackComposeMasterclass](https://github.com/philipplackner/JetpackComposeMasterclass): Curs avansat despre optimizarea UI, layout uri personalizate și animații în Compose.
- [ChatApp-Server](https://github.com/philipplackner/ChatApp-Server): Implementarea serverului de chat în Kotlin folosind Ktor Framework și WebSockets.
- [chirp-api](https://github.com/philipplackner/chirp-api): Proiectul backend (API) pentru aplicația Chirp, demonstrând integrarea bazelor de date cu Ktor.
- [MultiModuleGradleManagement](https://github.com/philipplackner/MultiModuleGradleManagement): Structurarea proiectelor mari în multiple module Gradle și gestionarea dependențelor.
- [JWTAuthKtorAndroid](https://github.com/philipplackner/JWTAuthKtorAndroid): Flux complet de autentificare JWT (login, signup, auth requests) între Android și Ktor.
- [KotlinFlowsGuide](https://github.com/philipplackner/KotlinFlowsGuide): Ghid practic despre StateFlow, SharedFlow și operatori de transformare a fluxurilor.
- [HiltTutorial](https://github.com/philipplackner/HiltTutorial): Introducere rapidă în configurarea și utilizarea Dagger Hilt pentru DI.
- [Material3App](https://github.com/philipplackner/Material3App): Demonstrare a componentelor Material Design 3 și a culorilor dinamice (Dynamic Color).
- [MusicKnob](https://github.com/philipplackner/MusicKnob): Crearea unui control UI personalizat (potențiometru) folosind gesturi și Canvas în Compose.
- [AnimatedSplashScreen](https://github.com/philipplackner/AnimatedSplashScreen): Implementarea unui ecran de pornire animat folosind noile API uri Android 12+.
- [DecomposeNavigation](https://github.com/philipplackner/DecomposeNavigation): Navigare avansată în proiecte multiplatform folosind biblioteca Decompose.
- [CMP-Koin-DI](https://github.com/philipplackner/CMP-Koin-DI): Utilizarea Koin pentru Dependency Injection în proiecte Compose Multiplatform.
- [ComposeNavDestinationsDemo](https://github.com/philipplackner/ComposeNavDestinationsDemo): Utilizarea bibliotecii Compose Destinations pentru navigare simplificată și tipizată.
- [ComposeTimer](https://github.com/philipplackner/ComposeTimer): Aplicație de cronometru demonstrând gestionarea precisă a timpului și a stării UI.
- [ManualDependencyInjection](https://github.com/philipplackner/ManualDependencyInjection): Înțelegerea conceptului de DI prin implementarea manuală fără biblieteci externe.
- [MotionLayoutCompose](https://github.com/philipplackner/MotionLayoutCompose): Crearea de animații complexe coordonate folosind MotionLayout în Jetpack Compose.
- [BottomNavWithBadges](https://github.com/philipplackner/BottomNavWithBadges): Implementarea unei bare de navigare inferioară cu indicatori de notificare (badges).
- [CMP-Ktor](https://github.com/philipplackner/CMP-Ktor): Utilizarea Ktor pentru networking partajat între Android și iOS în Compose Multiplatform.
- [LandmarkRecognitionTensorflow](https://github.com/philipplackner/LandmarkRecognitionTensorflow): Integrarea modelelor TensorFlow Lite pentru recunoașterea obiectelor în timp real.
- [BiometricAuth](https://github.com/philipplackner/BiometricAuth): Implementarea autentificării prin amprentă sau recunoaștere facială folosind BiometricPrompt.
- [DownloadManagerGuide](https://github.com/philipplackner/DownloadManagerGuide): Descărcarea fișierelor mari în fundal folosind serviciul de sistem DownloadManager.
- [FcmPushNotificationsHttpV1](https://github.com/philipplackner/FcmPushNotificationsHttpV1): Trimiterea notificărilor push folosind noul protocol HTTP v1 al Firebase.
- [M3-BottomNavigation](https://github.com/philipplackner/M3-BottomNavigation): Implementarea barei de navigare folosind componentele și stilurile Material 3.
- [InternetConnectionObserver](https://github.com/philipplackner/InternetConnectionObserver): Monitorizarea stării conexiunii la internet folosind ConnectivityManager și Flow.
- [BottomNavigationWithFAB](https://github.com/philipplackner/BottomNavigationWithFAB): Design UI care combină bara de navigare cu un buton de acțiune plutitor (FAB).
- [DataStoreAndroid](https://github.com/philipplackner/DataStoreAndroid): Înlocuirea SharedPreferences cu Jetpack DataStore pentru stocarea tipizată a datelor.
- [ErrorHandlingCleanArch](https://github.com/philipplackner/ErrorHandlingCleanArch): Strategii avansate de raportare și afișare a erorilor în proiecte multi strat.
- [CMPMemeCreator](https://github.com/philipplackner/CMPMemeCreator): Aplicație multiplatformă pentru crearea de meme uri cu editare de text peste imagini.
- [Doodlekong](https://github.com/philipplackner/Doodlekong): Joc multiplayer de tip "ghicește desenul" folosind WebSockets și Canvas.
- [GlobalSnackbarsCompose](https://github.com/philipplackner/GlobalSnackbarsCompose): Gestionarea afișării mesajelor de tip Snackbar la nivel de aplicație (Scaffold).
- [KoinGuide](https://github.com/philipplackner/KoinGuide): Ghid complet de utilizare a Koin pentru gestionarea dependențelor în Kotlin.
- [ktor-rabbits](https://github.com/philipplackner/ktor-rabbits): Exemplu de API simplu în Ktor pentru servirea de imagini și date aleatorii.
- [ComposeNavigationCustomTypes](https://github.com/philipplackner/ComposeNavigationCustomTypes): Trecerea obiectelor complexe între ecrane folosind Type Safe Navigation în Compose.
- [KotlinDelegates](https://github.com/philipplackner/KotlinDelegates): Demonstrare a puterii delegatelor în Kotlin (by lazy, observable, vetoable).
- [AlarmManagerGuide](https://github.com/philipplackner/AlarmManagerGuide): Programarea sarcinilor la ore precise folosind AlarmManager și BroadcastReceivers.
- [ComposeSwipeToReveal](https://github.com/philipplackner/ComposeSwipeToReveal): Implementarea acțiunilor de tip swipe (ștergere, editare) pentru elementele din liste.
- [ComposeOTPInput](https://github.com/philipplackner/ComposeOTPInput): Crearea unui câmp de introducere pentru coduri de verificare (OTP) cu focus automat.
- [JetpackViewModel-CMP](https://github.com/philipplackner/JetpackViewModel-CMP): Utilizarea ViewModel urilor partajate între Android și iOS în Compose Multiplatform.
- [KtorNoteServer](https://github.com/philipplackner/KtorNoteServer): Server backend pentru o aplicație de note, construit cu Ktor și PostgreSQL.
- [AudioRecorder](https://github.com/philipplackner/AudioRecorder): Capturarea sunetului de la microfon și salvarea fișierelor folosind MediaRecorder.
- [ComposeSwipeablePages](https://github.com/philipplackner/ComposeSwipeablePages): Crearea unui onboarding sau a unei galerii folosind HorizontalPager în Compose.
- [ComposeDateTimePicker](https://github.com/philipplackner/ComposeDateTimePicker): Selectoare de dată și oră personalizate integrate în fluxul Jetpack Compose.
- [MaterialCalculator](https://github.com/philipplackner/MaterialCalculator): Aplicație de calculator modernă cu design Material 3 și animații de tranziție.
- [MultipleBackstacksCompose](https://github.com/philipplackner/MultipleBackstacksCompose): Gestionarea stărilor de navigare independente pentru tab uri diferite.
- [CredentialManagerGuideCompose](https://github.com/philipplackner/CredentialManagerGuideCompose): Utilizarea noului Credential Manager API pentru Passkeys și autentificare.
- [FirebaseFirestore](https://github.com/philipplackner/FirebaseFirestore): Operațiuni CRUD în timp real folosind baza de date NoSQL Cloud Firestore.
- [ImageCompression](https://github.com/philipplackner/ImageCompression): Tehnici de reducere a dimensiunii imaginilor înainte de încărcarea pe server.
- [M3-NavigationDrawer](https://github.com/philipplackner/M3-NavigationDrawer): Implementarea meniului lateral (drawer) conform specificațiilor Material 3.
- [ComposeContextDropDown](https://github.com/philipplackner/ComposeContextDropDown): Afișarea meniurilor contextuale (pop up) la interacțiunea cu elementele UI.
- [kotlin-crash-course](https://github.com/philipplackner/kotlin-crash-course): Resurse și exemple pentru învățarea rapidă a limbajului Kotlin.
- [CMP-DataStore](https://github.com/philipplackner/CMP-DataStore): Utilizarea Jetpack DataStore în proiecte Compose Multiplatform pentru persistență partajată.
- [ComposeParallaxScroll](https://github.com/philipplackner/ComposeParallaxScroll): Implementarea efectului de scroll paralax pentru liste și imagini în Compose.
- [EncryptedDataStore](https://github.com/philipplackner/EncryptedDataStore): Securizarea datelor stocate în DataStore folosind biblioteca Jetpack Security.
- [ktor-doodlekong](https://github.com/philipplackner/ktor-doodlekong): Backend ul pentru jocul Doodlekong, gestionând logica de joc și conexiunile WebSockets.
- [ListPaneScaffoldGuide](https://github.com/philipplackner/ListPaneScaffoldGuide): Crearea de layout uri adaptabile (list detail) pentru ecrane mari și pliabile.
- [AnnotationsGuide](https://github.com/philipplackner/AnnotationsGuide): Crearea și procesarea adnotărilor personalizate în Kotlin pentru generare de cod.
- [AppSearchGuide](https://github.com/philipplackner/AppSearchGuide): Integrarea motorului de căutare performant de pe dispozitiv (AppSearch) în aplicație.
- [CMP-SplashScreen](https://github.com/philipplackner/CMP-SplashScreen): Ecran de pornire unificat pentru Android și iOS în Compose Multiplatform.
- [ComposeAutoResizedText](https://github.com/philipplackner/ComposeAutoResizedText): Componentă de text care își ajustează automat dimensiunea fontului pentru a încăpea în container.
- [ComposeScreenshotTesting](https://github.com/philipplackner/ComposeScreenshotTesting): Configurarea testelor automate care compară capturile de ecran ale componentelor UI.
- [DeeplinkingCompose](https://github.com/philipplackner/DeeplinkingCompose): Gestionarea link urilor externe și navigarea către ecrane specifice din aplicație.
- [DeeplinkingGuideTypeSafeNavigation](https://github.com/philipplackner/DeeplinkingGuideTypeSafeNavigation): Implementarea link urilor profunde folosind noile API uri de navigare tipizate.
- [KtorNoteApp](https://github.com/philipplackner/KtorNoteApp): Aplicația client (Android) pentru sistemul de note cu sincronizare backend.
- [AgoraUIKit](https://github.com/philipplackner/AgoraUIKit): Integrarea apelurilor video și audio folosind SDK ul Agora și Compose.
- [AutoStartAndroid](https://github.com/philipplackner/AutoStartAndroid): Gestionarea execuției codului la pornirea dispozitivului (Boot Completed).
- [com.plcoding.tictactoe](https://github.com/philipplackner/com.plcoding.tictactoe): Joc de X și 0 demonstrând logica de joc și starea în Compose.
- [ComposeDragAndDrop](https://github.com/philipplackner/ComposeDragAndDrop): Implementarea interacțiunilor de tragere și plasare între componentele UI.
- [ListItemCompose](https://github.com/philipplackner/ListItemCompose): Utilizarea și personalizarea componentei standard ListItem din Material 3.
- [AndroidLibrary](https://github.com/philipplackner/AndroidLibrary): Ghid pentru crearea și publicarea propriilor biblieteci Android (AAR).
- [AnimatedCounterCompose](https://github.com/philipplackner/AnimatedCounterCompose): Efect de numărare animată pentru cifre și valori numerice în UI.
- [CMP-Testing](https://github.com/philipplackner/CMP-Testing): Strategii de testare unitară și UI pentru proiecte Compose Multiplatform.
- [ComposeCustomLayouts](https://github.com/philipplackner/ComposeCustomLayouts): Măsurarea și plasarea manuală a elementelor folosind API ul Layout în Compose.
- [KMM-SharingResources](https://github.com/philipplackner/KMM-SharingResources): Partajarea resurselor (imagini, string uri, fonturi) între platforme în KMP.
- [KtorFCM](https://github.com/philipplackner/KtorFCM): Backend Ktor pentru trimiterea notificărilor push către clienți prin Firebase.
- [KtorUploadFileWithProgressBar](https://github.com/philipplackner/KtorUploadFileWithProgressBar): Încărcarea fișierelor pe server cu indicarea progresului în timp real.
- [LazyColumnLagFix](https://github.com/philipplackner/LazyColumnLagFix): Optimizări pentru rezolvarea problemelor de performanță în liste lungi Compose.
- [AndroidRoomMigration](https://github.com/philipplackner/AndroidRoomMigration): Ghid pentru actualizarea schemei bazei de date Room fără pierderea datelor.
- [CategorizedLazyColumn](https://github.com/philipplackner/CategorizedLazyColumn): Crearea listelor cu secțiuni și antete (sticky headers) în Compose.
- [CMP-Pagination](https://github.com/philipplackner/CMP-Pagination): Implementarea paginării partajate între Android și iOS în Compose Multiplatform.
- [CMP-PermissionHandling](https://github.com/philipplackner/CMP-PermissionHandling): Gestionarea permisiunilor de sistem (cameră, locație) în mod unificat pentru Android/iOS.
- [DaggerScopes](https://github.com/philipplackner/DaggerScopes): Înțelegerea duratei de viață a dependențelor (Singleton, ActivityScoped etc.) în Hilt.
- [DrawingInJetpackCompose](https://github.com/philipplackner/DrawingInJetpackCompose): Utilizarea API ului DrawScope pentru a desena forme și grafice personalizate.
- [M3-BottomSheet](https://github.com/philipplackner/M3-BottomSheet): Implementarea panourilor glisante inferioare conform stilului Material 3.
- [CoilImageCachingGuide](https://github.com/philipplackner/CoilImageCachingGuide): Configurarea încărcării și cache ului imaginilor folosind biblioteca Coil.
- [AppShortcutGuide](https://github.com/philipplackner/AppShortcutGuide): Crearea scurtăturilor pe ecranul principal (Static & Dynamic Shortcuts).
- [CMP-KoinAnnotationsGuide](https://github.com/philipplackner/CMP-KoinAnnotationsGuide): Utilizarea adnotărilor Koin pentru o configurare mai simplă a DI în multiplatform.
- [ComposeCustomShapes](https://github.com/philipplackner/ComposeCustomShapes): Crearea de forme geometrice personalizate pentru componente și imagini.
- [ComposeLoadingAnimations](https://github.com/philipplackner/ComposeLoadingAnimations): Implementarea indicatorilor de încărcare personalizați și a animațiilor de tip Shimmer.
- [KMPGradle9Migration](https://github.com/philipplackner/KMPGradle9Migration): Ghid pentru actualizarea proiectelor KMP la noile versiuni de Gradle și Kotlin.
- [KotlinChannels](https://github.com/philipplackner/KotlinChannels): Utilizarea Channels pentru comunicarea sigură între Coroutines.
- [KtorPushNotifications](https://github.com/philipplackner/KtorPushNotifications): Implementarea suportului pentru notificări în cadrul unui server Ktor.
- [LazyStaggeredGridCompose](https://github.com/philipplackner/LazyStaggeredGridCompose): Afișarea elementelor într o grilă asimetrică (stil Pinterest) în Compose.
- [Material3ExpressiveGuide](https://github.com/philipplackner/Material3ExpressiveGuide): Explorarea noilor concepte de design "Expressive" din ecosistemul Material.
- [BaselineProfileMacrobenchmark](https://github.com/philipplackner/BaselineProfileMacrobenchmark): Îmbunătățirea timpului de pornire a aplicației folosind profile de bază.
- [CMP-DateTime](https://github.com/philipplackner/CMP-DateTime): Gestionarea datelor și a timpului în mod partajat folosind biblioteca kotlinx datetime.
- [CMP-Theming](https://github.com/philipplackner/CMP-Theming): Implementarea unei teme vizuale partajate pentru o experiență consistentă pe Android și iOS.
- [ComposeDropDown](https://github.com/philipplackner/ComposeDropDown): Meniu de tip drop down standard pentru selecția opțiunilor în formulare.
- [ComposePinchZoomRotate](https://github.com/philipplackner/ComposePinchZoomRotate): Implementarea gesturilor de zoom și rotație pentru imagini sau hărți.
- [ComposeUiOptimization](https://github.com/philipplackner/ComposeUiOptimization): Tehnici de reducere a recompozițiilor inutile pentru o interfață fluidă.
- [FileUpload](https://github.com/philipplackner/FileUpload): Trimiterea de fișiere către un API folosind Multipart în Retrofit sau Ktor.
- [GenderPicker](https://github.com/philipplackner/GenderPicker): Control UI personalizat pentru selecția genului cu animații și grafică Canvas.
- [InitialARSetup](https://github.com/philipplackner/InitialARSetup): Configurația de bază pentru proiecte de realitate augmentată (ARCore).
- [Kapt-KSP-Migration](https://github.com/philipplackner/Kapt-KSP-Migration): Trecerea de la procesarea adnotărilor cu Kapt la noul și mai rapidul KSP.
- [LocationTrackerJava](https://github.com/philipplackner/LocationTrackerJava): Exemplu de urmărire a locației implementat în limbajul Java (referință legacy).
- [AndroidInternals](https://github.com/philipplackner/AndroidInternals): Explorarea modului de funcționare a sistemului Android (Low level topics).
- [ComposeMedia3](https://github.com/philipplackner/ComposeMedia3): Integrarea noului API Media3 pentru redare audio și video în Compose.
- [FabExplosionAnimation](https://github.com/philipplackner/FabExplosionAnimation): Animație complexă de tranziție care pornește de la un buton FAB.
- [LazyVerticalGridCompose](https://github.com/philipplackner/LazyVerticalGridCompose): Organizarea elementelor într o grilă verticală standard cu număr variabil de coloane.
- [M3-TopAppBar](https://github.com/philipplackner/M3-TopAppBar): Implementarea barei superioare a aplicației cu suport pentru scroll și acțiuni.
- [ComposeFormKeyboardManagement](https://github.com/philipplackner/ComposeFormKeyboardManagement): Controlul tastaturii și al focusului în formulare complexe Compose.
- [ComposeMultiSelect](https://github.com/philipplackner/ComposeMultiSelect): Implementarea selecției multiple pentru elementele dintr o listă.
- [FirebaseAuth](https://github.com/philipplackner/FirebaseAuth): Autentificarea utilizatorilor cu email/parolă sau social login folosind Firebase Auth.
- [M3-SelectableComponents](https://github.com/philipplackner/M3-SelectableComponents): Utilizarea Checkbox, RadioButton și Switch conform stilului Material 3.
- [AugmentedImages](https://github.com/philipplackner/AugmentedImages): Aplicație AR care afișează modele 3D atunci când detectează anumite imagini din lumea reală.
- [CodeDocumentation](https://github.com/philipplackner/CodeDocumentation): Ghid pentru documentarea codului Kotlin folosind KDoc și generarea cu Dokka.
- [FirebaseStorage](https://github.com/philipplackner/FirebaseStorage): Încărcarea și descărcarea fișierelor media folosind Firebase Cloud Storage.
- [MappersGuide](https://github.com/philipplackner/MappersGuide): Utilizarea mapper elor pentru conversia între obiectele de rețea, baza de date și UI (Data Mapping).
