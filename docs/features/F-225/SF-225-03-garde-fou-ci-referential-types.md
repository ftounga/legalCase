# SF-225-03 — Garde-fou CI : intégrité types référentiels frontend ↔ DB

## Objectif
Empêcher l'arrivée en prod d'un nouveau `referential_type` (DB seed) sans intégration UX dédiée côté frontend (titre brut, JSON brut, icône générique). Pattern miroir de `DecisionToolVisibilityIntegrityIT` (F-164 SF-164-01) pour les outils décisionnels.

## Comportement nominal
- Test `tout_referential_type_systeme_a_une_integration_UX_frontend()` dans `LegalReferentialDescriptionIntegrityIT` :
  - Charge tous les `referential_type` distincts en DB avec `is_system=true`.
  - Compare à la liste hardcodée `KNOWN_FRONTEND_REFERENTIAL_TYPES`.
  - Échoue si un type DB n'est pas dans la liste (= UX manquante).
- Règle CLAUDE.md ajoutée dans la table "Blocages automatiques" : refuser tout merge backend qui INSERT un nouveau `referential_type` sans UX frontend dédiée.

## Cas d'erreur (test échoue)
- Migration ajoute `is_system=true` un nouveau `referential_type` sans mettre à jour `SECTION_LABELS`/`formatValue`/`sectionIcon` côté frontend ET sans ajouter le type dans `KNOWN_FRONTEND_REFERENTIAL_TYPES` → `assertThat(orphans).isEmpty()` échoue avec un message explicite listant les types orphelins.

## Critères d'acceptation
1. Nouveau test `tout_referential_type_systeme_a_une_integration_UX_frontend()` dans `LegalReferentialDescriptionIntegrityIT.java`.
2. Liste `KNOWN_FRONTEND_REFERENTIAL_TYPES` contient les 21 types actuels (16 historiques + 5 SF-225-01).
3. Règle CLAUDE.md de blocage automatique ajoutée dans la table "Blocages automatiques".
4. Test passe sur l'état actuel de master.
5. Test échoue (en mode démo, pas mergé) si on retire un type de la liste hardcodée.

## Plan de test
- Lancer `./mvnw test -Dtest='LegalReferentialDescriptionIntegrityIT'` → 3/3 verts.
- Vérifier (manuellement) que retirer un type de `KNOWN_FRONTEND_REFERENTIAL_TYPES` fait échouer le test avec le message attendu.

## Tables / endpoints / composants impactés
- `backend/src/test/java/fr/ailegalcase/referential/LegalReferentialDescriptionIntegrityIT.java` (extension)
- `CLAUDE.md` (ajout règle de blocage)

## Hors périmètre
- Refonte complète du dialog d'édition (V2 si besoin).
- Validation IA modif sur F-IA-03 (SF-225-02 parallèle).
- Auto-extraction de `SECTION_LABELS` via parsing du fichier TS frontend (V2 — la liste hardcodée est un meilleur compromis sécurité/maintenance pour l'instant).
