# Ghid Live Update Notifications (Android)

Acest ghid conține directivele oficiale de design și implementare pentru notificările de tip Live Update pe Android.

## Principii de Design
* **Când să folosești:** Pentru activități cu final clar, pornite de utilizator (ex. livrare, ridesharing).
* **De evitat:** Nu folosi pentru recomandări sau activități fără un punct final definit.

## Reguli de Implementare
1. **Alerte:** Doar pentru schimbări majore (ex. "A sosit curierul"). Nu alerta pentru modificări mici de timp (ETA).
2. **Bara de Progres:** Trebuie să fie clară. Pașii discreți trebuie etichetați.
3. **Consistență:** Timpul (timestamp) trebuie să aibă același format în Status Bar și în cardul extins.
4. **Șabloane:** Folosește titluri scurte care să permită scanarea rapidă a informației critice.

---
*Sursa: [Android Developers - Live update notifications](https://developer.android.com/design/ui/mobile/guides/home-screen/live-updates?hl=en)*