---
name: android-live-updates
description: Ghid pentru implementarea notificărilor Live Update pe Android (comenzi rapide, design și reguli).
---

# Android Live Update Notifications

Acest skill oferă ghidare procedurală pentru designul și implementarea notificărilor de tip „Live Update” (ex. Status Bar chips, extinderea notificărilor).

## Când să utilizezi acest skill
- Când dezvolți funcționalități de urmărire în timp real (livrare, rideshare, cronometre).
- Când proiectezi interfețe de notificare care trebuie să afișeze progrese pe ecranul principal sau în bara de stare.

## Directive de Design (Do's & Don'ts)
- **Do:** Folosește actualizările live pentru experiențe cu final clar (ex: livrare finalizată).
- **Don't:** Nu folosi acest tip de notificări pentru recomandări sau promoții.

## Reguli Procedurale
1. **Alerte Selective:**
   - Nu alerta utilizatorul pentru schimbări minore de ETA (ex: +/- 1 minut).
   - Alertează doar pentru schimbări de stare critică (ex: "Driver has arrived").
2. **Componenta de Progres:**
   - Dacă folosești pași discreți, asigură-te că fiecare pas este etichetat clar.
   - Bara de progres trebuie să fie vizibilă dintr-o singură privire.
3. **Timestamp și Formatare:**
   - Formatele de timp trebuie să fie identice între Status Bar și cardul de notificare.
   - Păstrează titlurile scurte și concentrate pe informația cea mai critică.

## Resurse Adiționale
Pentru detalii tehnice aprofundate, consultă `docs/android_live_updates.md` din workspace.