# Ghid Picture-in-Picture (PiP)

Modul PiP permite utilizatorilor să vizualizeze conținut video într-o fereastră mică peste alte aplicații.

## Ghiduri de Design
- **Minimalism UI:** Ascunde toate butoanele și meniurile la trecerea în PiP.
- **Interacțiuni Standard:**
    - Single-tap: Afișează controalele (Play/Pause, Expand, Close).
    - Double-tap: Comută între dimensiunile ferestrei.
    - Pinch-to-zoom: Redimensionare manuală.

## Reguli de Implementare (Android 12+)
1. **Manifest:** PiP trebuie declarat explicit în manifestul aplicației.
2. **Tranziții:** Folosește `setAutoEnterEnabled(true)` pentru o tranziție lină la swipe up.
3. **sourceRectHint:** Indică sistemului zona video pentru a evita artefactele vizuale.
4. **Acțiuni:** Poți adăuga controale personalizate (ex: "Next") folosind `RemoteAction`.

---
*Sursa: [Android Developers - Picture-in-picture](https://developer.android.com/design/ui/mobile/guides/home-screen/picture-in-picture?hl=en)*