# Mini-spec — F-139 / SF-139-01 Alignement DB-first des référentiels métier Java

## Identifiant · `F-139 / SF-139-01`
## Date · `2026-04-20` · Branche · `feat/SF-139-01-db-first-alignment`

## Objectif
Généraliser le pattern F-129-03 ("DB seule source de vérité") aux 3 référentiels métier qui étaient encore hardcodés en Java : barème Macron + CCT 109, critères rupture conventionnelle, critères licenciement.

## Suppressions
- `IndemniteJurisprudentielReferentiel.java` (+ test)
- `RuptureConvCritereReferentiel.java` (+ test)
- `LicenciementCritereReferentiel.java` (+ test)

## Nouveau pattern dans `LegalReferentialService`
- `getBaremeMacron(int ancienneteAnnees)` — lit `INDEMNITE_BAREMES/MACRON`
- `Cct109Range getCct109Range()` — lit `INDEMNITE_BAREMES/CCT109` (record inner class)
- `List<RuptureConvCritere> getRuptureConvCriteres(String country)` — lit `RUPTURE_CONV_CRITERES`
- `getLicenciementCritere(code)` et `getLicenciementCriteres(country)` : fallback Java retiré

Toutes **DB only** (plus de fallback Java). Retournent `null`/`List.of()` si DB muette (fail-safe, loggé).

## Refactors consumers
- `IndemniteComparatifCalculator.calculate()` : signature enrichie (ajoute `IndemniteBareme macronBareme` + `Cct109Range cctRange`). Pure fonction, validation inchangée.
- `IndemniteComparatifService` : inject `LegalReferentialService`, lookup en amont, passe baremes au calculator.
- `RuptureConvAnalyzer.analyze(country, reponses, criteres)` : overload 2-args supprimée. Nouvelle statique `isCountryValid(country)` remplace le helper de la classe supprimée.
- `RuptureConvService` : inject `LegalReferentialService`, lookup criteres DB, passe à l'analyzer. 500 Internal Server Error si critères absents (ne devrait jamais arriver post-migration 092).
- `LicenciementAnalyzer.analyze(country, reponses, criteres)` : overload 2-args supprimée. Nouvelle `isCountryValid(country)`.
- `LicenciementService` : utilise `isCountryValid` du nouvel analyzer + garde 500 si critères absents.

## Tests adaptés
- `TestRuptureConvCriteres` (fixture FR 6 critères)
- `TestLicenciementCriteres` (fixture FR 7 + BE 6 critères)
- `TestIndemniteBaremes` (fixture barème Macron 0-29 ans + CCT 109 range)
- `RuptureConvAnalyzerTest`, `LicenciementAnalyzerTest`, `IndemniteComparatifCalculatorTest` : migrés pour injecter fixtures

## Critères d'acceptation
- [x] Les 3 classes Java référentiels supprimées + leurs tests
- [x] Compilation PASS (0 erreur)
- [x] 961/961 backend verts (vs 984 avant : -23 tests Java hardcodés + suite de tests analyzer/calculator migrée)
- [x] Context load PASS (migrations toutes jouées sur H2 + récupération des référentiels depuis DB fonctionnelle)
- [x] Pattern `LegalReferentialService` aligné avec F-129-03 (DB only, no fallback)

## Hors scope
- Optimisation caching des lookups DB (pas nécessaire — volume faible, Hibernate L1 suffit)
- Bénéfices "override admin workspace" : déjà disponibles via F-110 (endpoint PUT /referentials/{id})
