# SF-235-01 — Extension matching CONTEXTUAL champs texte (nationalite)

## Objectif

Étendre le mécanisme `extractDetectedSituations` de `DecisionToolVisibilityService` pour propager le champ texte `nationalite` (extrait par l'IA) comme `trigger_field` consommable par les règles `decision_tool_visibility_rules`. Permet aux outils décisionnels conditionnés à une nationalité (ex. F-IM-17 régime franco-algérien) de basculer ALWAYS_ON → CONTEXTUAL.

## Contexte

Aujourd'hui, le mécanisme accepte des `trigger_value` strings côté DB, mais `extractDetectedSituations` ne propage que des booleans + quelques strings hardcodées (type_rupture, type_titre_sejour_code, type_recours_code, etc.). Le champ texte libre `nationalite` n'est ni extrait par le prompt IA, ni propagé dans la map `detected`. Conséquence concrète : F-IM-17 (régime algérien) reste ALWAYS_ON malgré F-201 — la spec indiquait d'utiliser `nationalite='Algérienne'` mais le mécanisme actuel ne l'expose pas (cf. commentaire dans migration 213).

## Comportement nominal

1. Le prompt `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` demande à l'IA d'extraire un nouveau champ texte libre `nationalite` (ex. "Algérienne", "Tunisienne", "Marocaine"…) en plus du booléen `nationalite_ue` existant.
2. `CaseAnalysisResponse.extractImmigrationData` lit `root.path("nationalite")` et le stocke dans `ImmigrationExtractedData.nationalite` (nouveau champ string nullable, ajouté en queue du record).
3. `DecisionToolVisibilityService.extractDetectedSituations` lit `immigration_extracted_data.nationalite` (texte brut) et applique une normalisation titlecase :
   - `algerienne` / `ALGERIENNE` / `Algerienne` → `Algérienne`
   - `tunisienne` / `TUNISIENNE` → `Tunisienne`
   - tout texte non vide → `Capitalized` (1ʳᵉ majuscule, reste minuscule, accents préservés tels quels)
4. La valeur normalisée est ajoutée dans `detected['nationalite']`.
5. Une règle CONTEXTUAL `trigger_field='nationalite' trigger_value='Algérienne'` (livrée par SF-235-02) match sur cette propagation.

## Cas d'erreur

| Cas | Comportement attendu |
|-----|----------------------|
| Champ `nationalite` absent du JSON IA | Pas de propagation, pas d'erreur (skip silencieux). |
| Valeur null ou blank | Skip silencieux. |
| `immigration_extracted_data` absent (dossier non-immigration) | Skip silencieux. |
| Caractères non-ASCII (ex. accents Algérienne) | Préservés à l'identique, normalisation titlecase n'altère pas les accents. |
| Conflit potentiel avec F-234 (refactor Builder de `ImmigrationExtractedData`) | Le champ est ajouté en queue du record avec marqueur `// === F-235 nationalite ===`, et un constructeur de rétrocompat est ajouté pour limiter la zone de conflit. Si conflit au merge, rebaser et déplacer le champ dans le Builder F-234. |

## Critères d'acceptation

- [x] Le prompt IMMIGRATION_INSTRUCTION mentionne explicitement le champ `nationalite` (texte libre, optionnel, null si non détectable).
- [x] `ImmigrationExtractedData` contient un champ `String nationalite` (nullable).
- [x] `extractImmigrationData` lit `root.path("nationalite")` et le passe au constructeur.
- [x] `extractDetectedSituations` lit `immigration_extracted_data.nationalite` et propage la valeur normalisée titlecase dans `detected['nationalite']`.
- [x] Test unitaire dans `DecisionToolVisibilityServiceTest` qui vérifie : analyse JSON avec `nationalite='Algérienne'` → `detected['nationalite']` contient `'Algérienne'`.
- [x] Test unitaire qui vérifie la normalisation : `algerienne` (lowercase) → `Algérienne`.
- [x] Test unitaire qui vérifie `nationalite=null` → pas de propagation.
- [x] Aucun outil décisionnel existant n'est affecté (la propagation `nationalite` n'a aucun consommateur actuel — seul SF-235-02 ajoutera le 1ᵉʳ).
- [x] `./mvnw test -Dtest=DecisionToolVisibilityServiceTest` passe.

## Plan de test minimal

**Unitaires (`DecisionToolVisibilityServiceTest`)**
- Test : `immigration_extracted_data.nationalite='Algérienne'` → propagation `detected['nationalite'].contains('Algérienne')`.
- Test : `immigration_extracted_data.nationalite='algerienne'` (lowercase) → propagation `'Algérienne'` (normalisation).
- Test : `immigration_extracted_data` sans champ `nationalite` → pas de propagation.

**Pas de test d'intégration nécessaire pour cette SF** — l'intégration end-to-end est validée par la SF-235-02 (qui ajoute la règle CONTEXTUAL et un test IT).

**Isolation workspace** : non affectée — la propagation se fait dans la map locale `detected`, pas de requête DB cross-workspace.

## Tables / endpoints / composants impactés

- `backend/src/main/java/fr/ailegalcase/analysis/LegalDomainPromptBuilder.java` (ligne ~258 : ajout description du champ `nationalite`).
- `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` :
  - record `ImmigrationExtractedData` (ajout champ `String nationalite` en queue, constructeur de rétrocompat).
  - méthode `extractImmigrationData` (lecture `root.path("nationalite")` + propagation).
- `backend/src/main/java/fr/ailegalcase/casefile/DecisionToolVisibilityService.java` :
  - méthode `extractDetectedSituations` (ajout `addIfPresent(detected, "nationalite", normalizeTitleCase(nationalite))`).
  - helper privé `normalizeTitleCase(String)`.
- `backend/src/test/java/fr/ailegalcase/casefile/DecisionToolVisibilityServiceTest.java` (3 tests).

## Hors périmètre

- L'ajout effectif d'une règle CONTEXTUAL F-IM-17 (livré par SF-235-02).
- La généralisation à d'autres champs texte (Tunisienne, Marocaine, Sénégalaise — ouvertures futures via accords bilatéraux 1988/1983/2006 listés dans le backlog F-220).
- Le pré-fill IA frontend du champ nationalite (déjà couvert par les autres SF Immigration F-IM-17 en place).

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|-------|--------|---------------|
| Travail FR / BE | Non applicable | `nationalite` n'a pas de pertinence dans les outils Travail (ni FR ni BE). |
| Famille FR / BE | Non applicable | Pas de régime succession/divorce conditionné à la nationalité dans les outils livrés. F-FA-08 (DIP successions) pertinent à terme — backlog. |
| Immigration FR | Intégrée | Cible directe (F-IM-17 + ouverture vers F-220 accords bilatéraux). |
| Immigration BE | Backlog | Régime algérien spécifique au droit FR (Accord 27/12/1968). BE a son propre arrêt royal séparé. |
| UI patterns frontend | Non applicable | Pas de composant frontend touché (changement purement backend). |
| Pré-fill IA des outils existants | Non impacté | Les outils Immigration utilisent déjà `aiData.nationalite` côté frontend ; cette SF expose le champ aux règles de visibilité, pas à un pré-fill. |

## Nouveau pattern UI ou service partagé

Cette SF n'introduit **aucun composant UI partagé**. Elle étend un service backend existant (`DecisionToolVisibilityService.extractDetectedSituations`) avec un nouveau pattern de propagation (champ texte normalisé). Le pattern est strictement local — pas de risque de divergence avec d'autres patterns concurrents.

**Helper `normalizeTitleCase`** : privé, méthode statique de `DecisionToolVisibilityService`. Si réutilisé ailleurs (ex. F-220 accords bilatéraux), extraire vers un util commun à ce moment-là — pas avant (YAGNI).

## Impact par domaine métier

Cette feature est **sensible au domaine** :
- **Droit du travail (FR/BE)** : non sensible. `nationalite` n'est pas un trigger pertinent pour les outils Travail.
- **Droit de l'immigration FR** : sensible direct. Permet d'activer F-IM-17 (régime algérien). Ouverture future possible : régime tunisien (1988), marocain (1983), sénégalais (2006).
- **Droit de l'immigration BE** : non sensible. Les régimes de séjour BE sont gouvernés par les flags 9bis/9ter/40bis/40ter (F-203).
- **Droit de la famille (FR/BE)** : non sensible directement. Pourra l'être pour DIP successions (F-FA-08 / F-FA-12 backlog) — non dans cette SF.

## Audit "Impact F-166 cross-C×D"

Cette SF n'INSERT/UPDATE **aucune règle dans `decision_tool_visibility_rules`** (c'est SF-235-02 qui le fera). Elle étend uniquement le service Java en lecture. Pas de matrice C×D à auditer pour cette SF.

## Audit "exhaustivité droit national FR/BE"

Pas d'INSERT seed `legal_referentials` ni `decision_tool_visibility_rules` dans cette SF. Audit reporté à SF-235-02.
