# Mini-spec — F-IM-21 / SF-IM-21-02 Backend extension prompt IA codes IM21_*

## Identifiant

`F-IM-21 / SF-IM-21-02`

## Feature parente

`F-IM-21` — Critères binaires de validité dossier immigration

## Statut

`ready`

## Date de création

2026-04-30

## Branche Git

`feat/SF-IM-21-02-backend-prompt`

---

## Objectif

Étendre les prompts système `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` et `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` pour lister explicitement les 18 codes binaires IM21_* (FR + BE) figés dans SF-IM-21-01. L'IA produit des entrées `points_procedure` avec `critere_code = "IM21_*"` et `expected_value = null` pour les dossiers immigration, exactement comme F-DT-08 le fait pour le droit du travail.

---

## Comportement attendu

### Cas nominal

1. **Prompt initial** : la règle de spécification du champ `points_procedure` (SYSTEM_PROMPT_TEMPLATE) est étendue avec une nouvelle ligne :
   ```
   - Critères F-IM-21 Validité dossier immigration (droit de l'immigration, binaires) :
     IM21_REGULARITE_SEJOUR_FR, IM21_DELAI_DEPOT_FR, IM21_PIECE_IDENTITE_FR,
     IM21_JUSTIF_DOMICILE_FR, IM21_ETAT_CIVIL_FR, IM21_PHOTO_FR, IM21_TIMBRE_FISCAL_FR,
     IM21_PIECES_MARIAGE_FR, IM21_COMMUNAUTE_VIE_FR, IM21_RESSOURCES_FR,
     IM21_CONVENTION_ACCUEIL_FR, IM21_REGULARITE_SEJOUR_BE, IM21_PIECE_IDENTITE_BE,
     IM21_PIECES_COHABITATION_BE, IM21_RESSOURCES_BE, IM21_LOGEMENT_BE,
     IM21_ASSURANCE_BE, IM21_EXTRAIT_CASIER_BE.
     Pour ces critères, "expected_value" doit rester null (statut VERIFIED/NON_COMPLIANT porte le signal).
   ```
2. **Prompt enrichi** : même règle ajoutée dans `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE`.
3. **Comportement attendu de Claude** : pour les dossiers immigration FR ou BE :
   - Les vérifications binaires (régularité séjour, pièce identité, état civil, etc.) sortent dans `points_procedure` avec `critere_code = "IM21_..."` et `expected_value = null`.
   - Les options stratégiques (Passeport talent envisageable, Carte de résident à 3 ans, etc.) sortent dans `pistes_strategiques` (F-176 SF-176-01).
   - Pas de mélange entre les deux blocs.
4. **Cohérence avec F-DT-08** : les codes IM21_* suivent la même règle d'écriture (préfixe domaine + nom court explicite + suffixe pays).
5. **F-96 frontend reste inchangé** : la checklist procédurale affiche les critères IM21_* exactement comme les FR_/BE_ de F-DT-08 (label affiché = `description` du critère + l'item du JSON IA).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Code IM21_* inconnu produit par l'IA | Comportement F-96 actuel — affiché comme un point sans `critere_code` (fail-open) |
| Champ `expected_value` rempli sur un IM21_* | Le prompt impose null ; si l'IA en produit un, F-96 ignore (pattern F-DT-08) |
| Dossier non-immigration mais l'IA produit IM21_* | Cas marginal — F-96 affiche la ligne, sans dommage |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-DT-08 (FR_*, BE_*) déjà dans le prompt — pattern miroir strict appliqué. F-FA-07 (FR_/BE_ étapes divorce binaires) — même logique. F-IM-05/06/07 (énumérés) — différents (expected_value rempli). F-DT-09 (DT09_TYPE_RUPTURE) — énuméré.
- [x] **Autres pays** : France + Belgique — couvre les deux explicitement.
- [x] **Autres domaines** : transversal (le prompt est le même pour les 3 domaines, les codes IM21_* ne sont pertinents qu'en immigration mais ne gênent pas les autres domaines).
- [x] **Autres flows** : pas d'auth, pas de routing, pas de quota.

### Niveaux de vérification

- [x] **Prompt initial** `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` étendu.
- [x] **Prompt enrichi** `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` étendu.
- [x] **Tests prompt** : assertions sur le contenu du prompt (présence de chaque code IM21_*).
- [x] **Pas de modification logique métier** — extension prompt seule.

### Nouveau pattern UI ou service partagé

- [x] **Pas de nouveau pattern** — extension d'un pattern existant (F-DT-08).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-08 (FR_/BE_ codes binaires) | Pattern de référence | Strict miroir |
| F-FA-07 | Pattern de référence | Strict miroir |
| F-IM-05/06/07 (énumérés) | Non concernés | Pas modifiés |
| F-IA-03 (cohérence IA) | À traiter en SF-IM-21-03 | Hors scope SF-IM-21-02 |
| F-96 frontend | Non | Affichage transparent (existant) |

### Décision

- [x] Extension prompt strict (2 lignes ajoutées dans 2 templates).
- [x] SF-IM-21-03 (F-IA-03) suivra.

---

## Impact par domaine métier

| Domaine | Effet |
|---------|-------|
| **Droit du travail (FR + BE)** | Aucun — le prompt liste les 14 codes F-DT-08 + autres domaines, l'IA continue de cibler les bons codes selon le contexte du dossier. |
| **Droit de la famille (FR + BE)** | Aucun. |
| **Droit de l'immigration (FR + BE)** | **Cible directe** — l'IA produit désormais des `points_procedure` avec `critere_code = IM21_*` et `expected_value = null` pour les dossiers immigration. La checklist F-96 affiche du binaire (et non plus des pistes stratégiques mal placées). |

---

## Parité des domaines métier

(N/A — pas un outil décisionnel niveau ≥ 5. Extension de pattern transversal.)

---

## Critères d'acceptation

- [ ] `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` enrichi avec une nouvelle ligne listant les 18 codes IM21_* (placée dans la section `points_procedure` après les codes F-DT-08 et F-FA-07).
- [ ] `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` enrichi de manière identique.
- [ ] L'instruction "expected_value doit rester null" répétée pour les codes IM21_*.
- [ ] Tests unitaires :
  - `CaseAnalysisServiceTest.systemPrompt_containsIm21BinaryCriteriaCodes()` — assertion présence des 18 codes
  - `EnrichedAnalysisServiceTest.systemPrompt_containsIm21BinaryCriteriaCodes()` — idem
  - 1 test par code (×18) trop verbeux → 1 test global vérifiant `containsIgnoringCase` sur chacun
- [ ] Pas de modification de la logique métier ni des entités.
- [ ] Tests existants passent toujours (non-régression sur F-DT-08, F-IA-03, etc.).

---

## Périmètre

### Hors scope

- F-IA-03 alignement cohérence IA (SF-IM-21-03)
- Frontend F-96 — affichage déjà transparent
- Validation runtime des codes IM21_* côté serveur
- Tests d'intégration end-to-end avec un dossier immigration réel — coût élevé pour valeur marginale en V1

---

## Technique

### Fichiers impactés

| Fichier | Modification |
|---------|--------------|
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisService.java` | Ajout d'une ligne dans `SYSTEM_PROMPT_TEMPLATE` |
| `backend/src/main/java/fr/ailegalcase/analysis/EnrichedAnalysisService.java` | Ajout d'une ligne identique dans `SYSTEM_PROMPT_TEMPLATE` |
| `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisServiceTest.java` | Nouveau test |
| `backend/src/test/java/fr/ailegalcase/analysis/EnrichedAnalysisServiceTest.java` | Nouveau test |

### Composants Angular

(N/A — couvert par F-96 frontend existant)

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisServiceTest.systemPrompt_containsIm21BinaryCriteriaCodes()` — assertion sur les 18 codes
- [ ] `EnrichedAnalysisServiceTest.systemPrompt_containsIm21BinaryCriteriaCodes()` — assertion sur les 18 codes
- [ ] Non-régression : tests existants passent (CaseAnalysisServiceTest, EnrichedAnalysisServiceTest, SentryJobReportingTest)

### Tests d'intégration

(N/A — extension de prompt sans modification de logique métier)

### Isolation workspace

- [x] N/A (extension prompt globale)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale**

### Smoke tests E2E concernés

- [x] Aucun

---

## Dépendances

### Subfeatures bloquantes

- **SF-IM-21-01** : mergée (PR #714). Les codes IM21_* sont figés.

### Subfeatures parallèles

- (N/A)

### Subfeatures débloquées

- **SF-IM-21-03** F-IA-03 alignement cohérence IA.

### Questions ouvertes impactées

- [x] Aucune

---

## Notes et décisions

- **Pourquoi ne pas générer dynamiquement la liste des codes depuis `ImmigrationValidationCriteriaReferentiel.all()` ?** Le prompt est statique pour des raisons de cohérence (l'IA doit voir les mêmes codes à chaque appel) et de testabilité. Si la liste évolue (V2), le prompt sera mis à jour explicitement. Pattern miroir F-DT-08.
- **Pourquoi placer les codes IM21_* à côté des codes F-DT-08 / F-FA-07 dans le prompt ?** Cohérence visuelle pour l'IA — toutes les listes binaires regroupées, puis les énumérés.
- **Pourquoi pas un seul test avec 18 assertions ?** 1 test global avec une boucle est plus lisible que 18 méthodes. Pattern de test miroir F-DT-08.
