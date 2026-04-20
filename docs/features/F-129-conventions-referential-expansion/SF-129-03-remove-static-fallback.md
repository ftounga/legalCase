# Mini-spec — F-129 / SF-129-03 Suppression du fallback static (DB only)

## Identifiant
`F-129 / SF-129-03`

## Feature parente
`F-129` — Référentiel conventions collectives

## Statut `draft`  · Date `2026-04-20`  · Branche `feat/SF-129-03-remove-static-convention-fallback`

---

## Objectif

Éliminer la double source de vérité (DB + fichier static) qui a causé les bugs PR #411 et #412 (consumers qui court-circuitaient le service DB-first). Après cette SF, **tous les barèmes CCN viennent uniquement de la table `legal_referentials`**.

---

## Comportement

### Suppression du static file

`ConventionBaremeReferentiel.java` est **supprimée** (plus aucun consumer après migration des tests). Les records DTOs (`ConventionBareme`, `CongesSupplementaire`, `PrimeAnciennete`) vivent dans `ConventionBareme.java` — inchangés.

### LegalReferentialService.getConventionBareme simplifié

Avant :
```java
public ConventionBareme getConventionBareme(String code) {
    List<LegalReferential> entries = repository.findSystemEntry(...);
    if (!entries.isEmpty()) return fromDb(entries.get(0));
    return ConventionBaremeReferentiel.getByCode(code);  // fallback
}
```

Après :
```java
public ConventionBareme getConventionBareme(String code) {
    List<LegalReferential> entries = repository.findSystemEntry(...);
    if (entries.isEmpty()) return null;
    return fromDb(entries.get(0));
}
```

### Normaliseur systématique

`ConventionCodeNormalizer.normalize()` appliqué **en amont** dans tous les consumers. Les anciens codes legacy (`METALLURGIE`, `COMMERCE`, etc.) passés par la normalisation deviennent `IDCC_3248`, `IDCC_2216`, etc. et trouvent leur entry DB.

Consumers à vérifier : `AncienneteService.calculate` + `validateRequest`, `BaremeController.get`, `CaseAnalysisResponse.extractTravailData` (déjà OK).

### Compatibilité ascendante

Les dossiers anciens (`anciennete_analyses`, `case_analyses`) ayant stocké un code legacy type `"METALLURGIE"` sont gérés via le normalizer côté read. Aucune migration de données nécessaire.

### Cas d'erreur

- Code inconnu (ni IDCC, ni legacy) → normalizer retourne la string telle quelle → DB lookup retourne null → caller se débrouille (404 / fallback UI)
- Erreur parsing JSON d'une entry DB malformée → log warn + retour null (try/catch dans `getConventionBareme`) — l'API caller reçoit null comme pour un code inconnu
- Code `null` ou vide → normalizer retourne null → retour immédiat null (pas d'accès DB)

---

## Critères d'acceptation

- [ ] La classe `ConventionBaremeReferentiel` n'existe plus dans `src/main/java` (supprimée après migration des consumers)
- [ ] `LegalReferentialService.getConventionBareme("IDCC_3248")` renvoie les barèmes complets Métallurgie
- [ ] `LegalReferentialService.getConventionBareme("METALLURGIE")` renvoie les barèmes Métallurgie (via normalizer interne METALLURGIE → IDCC_3248 → DB)
- [ ] `LegalReferentialService.getConventionBareme("INCONNU_XYZ")` renvoie null (plus de fallback statique)
- [ ] `BaremeController.get("METALLURGIE")` renvoie le barème Métallurgie (via normalizer)
- [ ] `AncienneteService.calculate` accepte l'un ou l'autre format via normalizer
- [ ] `LegalcaseBackendApplicationTests.contextLoads` PASS
- [ ] Tests existants qui référencent METALLURGIE etc. restent verts

---

## Plan de test

### Unitaires backend
- `LegalReferentialServiceTest` : nouveaux tests
  - `getConventionBareme_idccCode_returnsDbBareme` (IDCC_3248 direct)
  - `getConventionBareme_legacyCode_normalizesAndReturnsDbBareme` (METALLURGIE → IDCC_3248)
  - `getConventionBareme_unknownCode_returnsNull` (plus de fallback static)
  - `getConventionBareme_nullOrBlank_returnsNull`
- `AncienneteCalculatorTest` : migré pour utiliser la surcharge 6-args avec un helper `TestConventionBaremes` (fixtures locales aux tests) — le calculator testé comme fonction pure
- `ConventionBaremeReferentielTest` : **supprimé** (la classe est devenue un shell sans logique)
- `AncienneteCalculator.calculate(1-arg)` : **supprimée** (elle utilisait le static — plus possible)

### Context load
- `LegalcaseBackendApplicationTests.contextLoads` PASS avec migrations 086 + 087

### Smoke staging
- Dossier E28 : calculer ancienneté → 200 OK, prime correcte
- Dossier avec code legacy (si existant) : même résultat (normalizer rend transparent)

### Isolation workspace
- N/A (données system-wide)

---

## Tables / endpoints / composants impactés

### Backend
- `ConventionBaremeReferentiel.java` — **fichier supprimé** (plus aucun consumer après migration)
- `LegalReferentialService.java` — fallback static retiré, normalizer appliqué en amont
- `AncienneteCalculator.java` — surcharge 1-arg supprimée (dépendait du static)
- `AncienneteService.java` — déjà DB-first (PR #412)
- `BaremeController.java` — déjà DB-first (PR #411)
- Tests : `ConventionBaremeReferentielTest` supprimé, `AncienneteCalculatorTest` migré vers fixtures, `LegalReferentialServiceTest` enrichi

### DB / Migrations
- Aucun changement (données DB déjà en place depuis migrations 086 + 087)

### Frontend
- Aucun changement

---

## Hors périmètre

- Les records DTOs `ConventionBareme`, `CongesSupplementaire`, `PrimeAnciennete` restent (ils sont dans `ConventionBareme.java`, indépendants de la classe supprimée)
- Migration de schéma : aucune
- Ajout de nouvelles CCN : couvert par F-129 existant

---

## Analyse de cohérence transversale

| Cible | Applicable | Classement |
|---|---|---|
| Autres référentiels (immigration, famille) | Non applicable pour cette SF — mais **Backlog** : vérifier si ces référentiels suivent le même pattern à double source et dédupliquer si besoin |
| Autres consumers de `ConventionBareme` | **Intégrée** — grep exhaustif lancé dans la SF |

**Analyse d'impact cross-cutting** :
- [ ] Auth — non touché
- [ ] Workspace — non touché
- [ ] Plans — non touché
- [ ] Navigation — non touché

Aucun smoke E2E concerné.

---

## Nouveau pattern UI ou service partagé

- [x] Pas de nouveau pattern — **simplification architecturale** (suppression d'une branche de fallback). Le service `LegalReferentialService` devient la seule source de vérité pour les barèmes CCN.
