---
name: android-pip
description: Ghid pentru implementarea modului Picture-in-picture pe Android (video multitasking).
---

# Android Picture-in-Picture (PiP)

Acest skill oferă ghidare procedurală pentru integrarea modului PiP în aplicațiile Android care redau conținut video.

## Când să utilizezi acest skill
- Când dezvolți aplicații de redare video (streaming, tutoriale, conferințe).
- Când vrei să permiți multitasking-ul în timp ce rulează conținut vizual.

## Ghiduri de Design
- **Minimalism:** Ascunde elementele de interfață în timpul tranzitului în PiP.
- **Interacțiune:** Implementează controale standard pentru redare și redimensionare.
- **Adaptabilitate:** Ferestrele PiP trebuie să poată fi mutate și ascunse parțial („Stash”).

## Reguli Procedurale (Android 12+)
1. **Configurare Manifest:** Activează explicit suportul PiP și modul de redimensionare în manifestul Android.
2. **Tranziții Fluide:**
   - Utilizează `setAutoEnterEnabled(true)` pentru a permite intrarea automată în PiP la swipe up (Home).
   - Indică `sourceRectHint` pentru a elimina artefactele de ecran negru la tranziție.
3. **Controale Media:**
   - Asigură-te că o sesiune media este activă pentru a beneficia de controalele de sistem implicite.
   - Adaugă acțiuni personalizate dacă este cazul folosind `RemoteAction`.

## Resurse Adiționale
Pentru detalii tehnice aprofundate, consultă `docs/android_pip.md` din workspace.