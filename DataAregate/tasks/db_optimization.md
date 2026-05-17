# Task: Optimizare Database și Prevenire Duplicate

## 1. Problema
În scenarii de utilizare intensă sau cu mulți utilizatori/surse, pot apărea duplicate în baza de date locală dacă nu există o strategie clară de "Upsert". De asemenea, viteza de aducere a datelor din DB în UI poate fi îmbunătățită prin indexare.

## 2. Fișiere afectate
- `core/database/entity/ArticleEntity.kt`
- `core/database/entity/SavedArticleEntity.kt`
- `core/database/dao/ArticleDao.kt`
- `core/database/util/HashUtils.kt` [NEW]

## 3. Fix/Implementare
- Adăugare `indices` pe coloanele critice (`category`, `publishedAt`).
- Trecerea de la `@Insert` la `@Upsert` în DAO.
- Implementarea unei funcții de hash pentru generarea de ID-uri unice din conținut.
