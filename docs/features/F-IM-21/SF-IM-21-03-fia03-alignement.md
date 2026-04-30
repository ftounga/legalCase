# Mini-spec — F-IM-21 / SF-IM-21-03 F-IA-03 alignement cohérence IA

## Identifiant

`F-IM-21 / SF-IM-21-03`

## Feature parente

`F-IM-21` — Critères binaires de validité dossier immigration

## Statut

`ready`

## Date de création

2026-04-30

## Branche Git

`feat/SF-IM-21-03-fia03-alignement`

---

## Objectif

Étendre le pipeline F-IA-03 (cohérence IA / popover sources) pour reconnaître les 18 codes IM21_* binaires comme `sourceKey` valides dans les `source_explanations`. L'avocat clique sur un critère IM21_* dans la checklist F-96 → un popover affiche les sources factuelles (DOCUMENT / QUESTION_AI / CHECKLIST_F96) qui ont conduit l'IA à statuer.

---

## Comportement attendu

### Cas nominal

1. **Prompt synthèse (initial + enrichi)** : la liste "Codes F96 additionnels possibles" pour `source_explanations.sourceKey` est étendue avec les 18 codes IM21_*.
2. **Prompt SourceExplanationGenerator** : la liste "Codes F96 outil" dans le prompt système de `SourceExplanationGenerator` est étendue avec les 18 codes IM21_*.
3. **Comportement attendu de l'IA** : quand l'analyse contient un point procédural avec `critere_code = "IM21_XXX"`, l'IA produit (si pertinent) une entrée `source_explanations` avec `sourceKey = "IM21_XXX"`, `sourceType` parmi DOCUMENT / QUESTION_AI / CHECKLIST_F96 / MISSING_PIECE / ANALYSIS_DETECTION, et les anchors correspondants.
4. **Frontend (transparent)** : le composant existant `SourceRefComponent` / popover F-IA-03 sait déjà gérer un `sourceKey` UPPER_CASE — pas de modification frontend.
5. **Multi-source MULTI** : si plusieurs documents corroborent le même critère IM21_*, l'IA produit plusieurs entrées avec le même sourceKey (pattern existant déjà documenté dans le prompt).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| L'IA produit un `sourceKey` IM21_* sans correspondance dans `points_procedure` | Pattern actuel — l'entrée est ignorée si pas de critère correspondant (fail-open) |
| L'IA omet une `source_explanation` pour un IM21_* présent | Pas grave V1 — popover juste pas affiché. Mieux vaut absent que faux. |
| Code IM21_* inconnu | Pas de risque — la liste est figée à 18 codes via SF-IM-21-01. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-DT-08 (FR_/BE_), F-DT-10 (RC_), F-FA-05/06/07, F-IM-05/06/07 — tous déjà dans la liste source_explanations. Pattern miroir.
- [x] **Autres pays** : FR + BE — couverts.
- [x] **Autres domaines** : transversal au prompt — uniquement IM21_* sont nouveaux.
- [x] **Autres flows** : pas d'auth, pas de routing, pas de quota.

### Niveaux de vérification

- [x] **Prompt synthèse `CaseAnalysisService`** : ligne `Codes F96 additionnels possibles` étendue.
- [x] **Prompt synthèse `EnrichedAnalysisService`** : idem.
- [x] **Prompt `SourceExplanationGenerator`** : ligne `Codes F96 outil` étendue.
- [x] **Tests unitaires prompts** : assertions présence des 18 codes dans les 3 prompts.

### Nouveau pattern UI ou service partagé

- [x] **Pas de nouveau pattern** — extension d'un pattern existant (F-DT-08, F-DT-10).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-08 / F-DT-10 / F-FA-05-07 / F-IM-05-07 source_explanations | Pattern de référence | Strict miroir |
| Frontend popover F-IA-03 | Non concerné | Transparent — accepte les sourceKey UPPER_CASE existants |
| F-176 pistes_strategiques | Non concerné | Volontairement exclu de F-IA-03 |

### Décision

- [x] Extension prompt strict (3 lignes ajoutées).

---

## Impact par domaine métier

| Domaine | Effet |
|---------|-------|
| **Droit du travail (FR + BE)** | Aucun |
| **Droit de la famille (FR + BE)** | Aucun |
| **Droit de l'immigration (FR + BE)** | **Cible directe** — l'avocat voit désormais le popover F-IA-03 sur les critères IM21_* dans la checklist F-96. |

---

## Parité des domaines métier

(N/A — extension de pattern transversal F-IA-03.)

---

## Critères d'acceptation

- [ ] `CaseAnalysisService.SYSTEM_PROMPT_TEMPLATE` ligne `Codes F96 additionnels possibles` enrichie avec les 18 codes IM21_*.
- [ ] `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` enrichi de manière identique.
- [ ] `SourceExplanationGenerator` ligne `Codes F96 outil` enrichie avec les 18 codes IM21_*.
- [ ] Tests unitaires prompt :
  - `CaseAnalysisServiceTest.systemPrompt_listsIm21CodesAsValidSourceKeys()` — assertion présence des 18 codes
  - `EnrichedAnalysisServiceTest.systemPrompt_listsIm21CodesAsValidSourceKeys()` — idem
  - `SourceExplanationGeneratorTest.systemPrompt_listsIm21CodesAsToolCodes()` — idem
- [ ] Pas de modification frontend.
- [ ] Pas de breaking change.
- [ ] Tests existants passent (non-régression).

---

## Périmètre

### Hors scope

- Frontend popover F-IA-03 — déjà transparent, pas de modification nécessaire
- Tests E2E avec un dossier immigration réel
- Helpers `CoherenceAlertBuilder` frontend — N/A (les composants décisionnels frontend immigration n'utilisent pas IM21_* directement, c'est la checklist F-96 qui les affiche)

---

## Technique

### Fichiers impactés

| Fichier | Modification |
|---------|--------------|
| `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisService.java` | Ajout codes IM21_* dans liste source_explanations |
| `backend/src/main/java/fr/ailegalcase/analysis/EnrichedAnalysisService.java` | Idem |
| `backend/src/main/java/fr/ailegalcase/analysis/SourceExplanationGenerator.java` | Ajout codes IM21_* dans liste "Codes F96 outil" |
| `backend/src/test/java/fr/ailegalcase/analysis/CaseAnalysisServiceTest.java` | Nouveau test |
| `backend/src/test/java/fr/ailegalcase/analysis/EnrichedAnalysisServiceTest.java` | Nouveau test |

(Note : `SourceExplanationGeneratorTest` à vérifier au moment du dev — si absent, créer ou ajouter à la suite existante.)

### Composants Angular

(N/A)

---

## Plan de test

### Tests unitaires

- [ ] `CaseAnalysisServiceTest.systemPrompt_listsIm21CodesAsValidSourceKeys()` — 18 assertions
- [ ] `EnrichedAnalysisServiceTest.systemPrompt_listsIm21CodesAsValidSourceKeys()` — 18 assertions
- [ ] `SourceExplanationGeneratorTest.systemPrompt_listsIm21CodesAsToolCodes()` (si test existe) — 18 assertions
- [ ] Non-régression : tests existants passent

### Tests d'intégration

(N/A)

### Isolation workspace

- [x] N/A

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

- **SF-IM-21-01** : mergée (PR #714)
- **SF-IM-21-02** : mergée (PR #715)

### Subfeatures parallèles

- (N/A)

### Subfeatures débloquées

- F-IM-21 globalement complet après cette SF.

### Questions ouvertes impactées

- [x] Aucune

---

## Notes et décisions

- **Pourquoi pas de modification frontend ?** Le `SourceRefComponent` accepte déjà n'importe quel `sourceKey` UPPER_CASE / snake_case (pattern existant pour F-DT-08, F-DT-10, etc.). Ajouter les 18 codes IM21_* au prompt suffit.
- **Pourquoi mentionner les 18 codes explicitement et pas un wildcard `IM21_*` ?** Cohérence avec le prompt existant (F-DT-08 et F-DT-10 sont aussi listés explicitement). L'IA est plus fiable avec une liste explicite qu'avec un pattern.
