# Mini-spec — F-IA-03 / SF-IA-03-03 Sources pièces manquantes + questions IA

## Identifiant

`F-IA-03 / SF-IA-03-03`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-13

## Branche Git

`feat/SF-IA-03-03-sources-pieces-questions`

---

## Objectif

Ajouter deux nouvelles sources au moteur de cohérence F-DT-08 : les pièces manquantes (signal faible, `warning` uniquement) et les questions IA répondues par l'avocat (signal fort, `blocker`), chacune taggée par Claude avec un `critere_code` optionnel.

---

## Comportement attendu

### Cas nominal

1. **Pièces manquantes** : le prompt `pieces_manquantes` passe du format `["Lettre de convocation"]` au format `[{ texte, critere_code? }]`. Rétrocompat string conservée. Exposition étendue dans `CaseAnalysisResponse` (nouveau type) + legacy texts préservés.
2. **Questions IA** : le prompt du générateur de questions demande à Claude de retourner `[{ texte, critere_code? }]` pour les questions portant sur un critère F-DT-08. **Convention impérative** : ces questions doivent être formulées pour qu'une réponse "oui" signifie "critère respecté" (ex: "La lettre de convocation a-t-elle été envoyée par LRAR ?"). Le `critere_code` est persisté en colonne dédiée sur `ai_questions` (migration 069).
3. Le frontend charge les questions IA répondues (via l'endpoint existant `GET /ai-questions`) et les passe au `LicenciementSectionComponent` avec les pièces manquantes déjà disponibles dans `synthesis()`.
4. Le computed `coherenceAlerts` applique la nouvelle hiérarchie.

### Hiérarchie finale des sources (ordre strict)

Pour chaque critère F-DT-08, en parcourant dans l'ordre :

| Étape | Condition | Niveau si divergence |
|---|---|---|
| A | avocat `INCONNU` | aucune alerte |
| B | point F-96 `VERIFIED` ou `NON_COMPLIANT` sur le critère | `blocker` (source `F96`) |
| C | ≥ 1 question IA répondue `oui`/`non` taggée sur le critère | `blocker` (source `QUESTION_IA`) |
| D | détection IA `OUI`/`NON` sur le critère | `warning`/`blocker` selon criticité (source `IA`, SF-IA-03-01) |
| E | ≥ 1 pièce manquante taggée sur le critère | `warning` (source `PIECE_MANQUANTE`) |
| F | sinon | aucune alerte |

Si plusieurs sources contredisent l'avocat simultanément, une seule alerte est émise à la priorité la plus haute, et le tooltip enrichi liste toutes les preuves (source `MULTI`).

### Interprétation des réponses de questions IA

- L'`answer_text` est normalisé en lower-case, trim.
- Si l'answer est exactement `"oui"` ou commence par `"oui"` (avec ponctuation) → réponse interprétée comme `OUI` → critère considéré comme respecté → `expectedReponse = OUI`.
- Si l'answer est exactement `"non"` ou commence par `"non"` → `NON` → critère considéré comme non respecté → `expectedReponse = NON`.
- Sinon (réponse libre non interprétable) → question ignorée pour le moteur de cohérence.
- S'il y a plusieurs questions sur le même critère avec des interprétations contradictoires, on ignore toutes les réponses (conflit interne, on ne tranche pas).

### Interprétation des pièces manquantes

- Une pièce manquante taggée avec un `critere_code` signifie que la preuve matérielle du critère fait défaut.
- Si l'avocat coche `OUI` sur ce critère alors qu'une pièce manquante le conteste → `warning` (source `PIECE_MANQUANTE`). Jamais `blocker` : l'avocat peut avoir la pièce sans l'avoir uploadée.
- Si l'avocat coche `NON` → concordance implicite, aucune alerte.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Pièces manquantes format string legacy | Parsé sans `critere_code`, ignoré par le moteur (fail-open) |
| Question IA non répondue (answer_text null) | Ignorée |
| Question IA réponse non interprétable | Ignorée silencieusement |
| Plusieurs questions IA contradictoires sur même critère | Toutes ignorées |
| Plusieurs pièces manquantes même critère | Déclenchent une seule alerte consolidée |
| `critere_code` invalide | Ignoré |

---

## Critères d'acceptation

- [ ] Le prompt `pieces_manquantes` accepte le nouveau format objet, rétrocompat string conservée.
- [ ] Le prompt du générateur de questions IA demande `[{ texte, critere_code? }]` et impose la convention "oui = critère respecté".
- [ ] Migration Liquibase `069-add-critere-code-to-ai-questions.xml`.
- [ ] `AiQuestion` entity + `AiQuestionResponse` exposent `critereCode`.
- [ ] `CaseAnalysisResponse` expose `piecesManquantesDetails: List<PieceManquanteEntry>` (nouveau type) en plus du champ string legacy conservé.
- [ ] `LicenciementSectionComponent` reçoit `@Input() aiQuestions` et `@Input() piecesManquantes` (liste d'objets).
- [ ] La hiérarchie A-F est appliquée strictement par le computed `coherenceAlerts`.
- [ ] Source `QUESTION_IA` = blocker, source `PIECE_MANQUANTE` = warning uniquement, sources `MULTI` = niveau du plus fort.
- [ ] Réponse IA "oui"/"non" correctement interprétée (trim, lower-case, préfixe).
- [ ] Réponses libres ignorées silencieusement.
- [ ] Questions IA contradictoires sur même critère → toutes ignorées.
- [ ] Fallback SF-IA-03-01 et SF-IA-03-02 strictement préservés quand aucune nouvelle source n'est disponible.
- [ ] Tests backend (prompt accepté, parsing pièces legacy + objet, persistance `critere_code` question).
- [ ] Tests frontend (matrice complète : QUESTION_IA seul, PIECE_MANQUANTE seul, F-96 écrase QUESTION_IA, IA dégradée par PIECE_MANQUANTE, MULTI source).

---

## Périmètre

### Hors scope (explicite)

- Extension aux autres outils (F-DT-07, F-DT-09, F-FA-*, F-IM-*) → SF-IA-03-04 et suivantes.
- Niveau `info` + justification obligatoire → SF ultérieure.
- Interprétation NLP des réponses libres de question IA (si pas oui/non → ignoré).
- Association manuelle par l'avocat d'une pièce/question à un critère.
- Backfill des questions/pièces existantes (seules les nouvelles analyses seront taggées).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `ai_questions.critere_code` | `null` | rempli par parsing IA si présent |

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Normalisation |
|-------|-------------|-------------|----------------------------|---------------|
| `ai_questions.critere_code` | Non | 50 | upper-case, valeurs connues filtrées côté front | upper-case backend avant persistance |
| `pieces_manquantes[].critere_code` | Non | 50 | idem | idem |
| `question.answer_text` (interprétation) | N/A | N/A | préfixe "oui"/"non" insensible à la casse | trim + lower-case côté front |

---

## Technique

### Endpoint(s)

| Méthode | URL | Changement |
|---------|-----|------------|
| GET | `/api/v1/case-files/{id}/ai-questions` | ajoute `critereCode` dans `AiQuestionResponse` |
| GET | `/api/v1/case-files/{id}/case-analysis` | ajoute `piecesManquantesDetails: [{texte, critereCode?}]` (le champ `piecesManquantes: string[]` legacy est conservé) |

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `ai_questions` | ALTER — ajout colonne `critere_code VARCHAR(50) NULL` | migration 069 |

### Migration Liquibase

- [x] Oui — `069-add-critere-code-to-ai-questions.xml`
- [ ] Non applicable

Réversible : DROP COLUMN sur colonne nullable non backfillée.

### Composants Angular

- `AiQuestion` model : ajout de `critereCode?: string | null`.
- `CaseAnalysisResult` model : ajout de `piecesManquantesDetails?: PieceManquanteEntry[] | null`.
- Nouveau type `PieceManquanteEntry { texte: string; critereCode?: string | null }`.
- `CaseFileDetailComponent` :
  - charge les questions IA (déjà fait via `loadQuestions`)
  - expose `questions()` et passe à `LicenciementSectionComponent`
- `LicenciementSectionComponent` :
  - nouveaux `@Input() aiQuestions?: AiQuestion[] | null`, `@Input() piecesManquantes?: PieceManquanteEntry[] | null`
  - signaux miroirs + ngOnChanges
  - `CoherenceAlert.source` enrichi : `F96 | QUESTION_IA | IA | PIECE_MANQUANTE | MULTI`
  - `buildQuestionIndex()` + `buildPiecesIndex()`
  - computed `coherenceAlerts` réécrit selon la hiérarchie A-F
  - badge label et tooltip adaptés par source

---

## Plan de test

### Tests unitaires backend

- [ ] `CaseAnalysisResponse.extractPiecesManquantesDetails()` : parse nouveau format objet + code upper-case.
- [ ] Idem format string legacy → liste d'objets sans `critereCode`.
- [ ] Item malformé (sans `texte`) ignoré.
- [ ] `AiQuestionService.parseQuestions()` parse format objet `{texte, critere_code?}` et persiste le code upper-case.
- [ ] Format string legacy accepté pour les questions.
- [ ] `AiQuestionResponse.from()` expose `critereCode`.
- [ ] Suite complète verte.

### Tests unitaires frontend

- [ ] Question IA OUI sur FR_MOTIVATION + avocat NON → blocker `QUESTION_IA`.
- [ ] Question IA NON sur FR_CONVOCATION + avocat OUI → blocker `QUESTION_IA`.
- [ ] Question IA réponse "peut-être" → ignorée, fallback IA/PIECE.
- [ ] Questions IA contradictoires sur même critère → toutes ignorées.
- [ ] Pièce manquante sur FR_CONVOCATION + avocat OUI → warning `PIECE_MANQUANTE`.
- [ ] Pièce manquante + avocat NON → aucune alerte.
- [ ] F-96 VERIFIED + question IA OUI + avocat NON → source `F96` gagne (pas `QUESTION_IA`).
- [ ] Question IA OUI + détection IA OUI + avocat NON → `QUESTION_IA` gagne (avec tooltip multi).
- [ ] Pièce manquante seule (IA silencieuse) → warning.
- [ ] Code inconnu dans question/pièce → ignoré.
- [ ] Fallback SF-IA-03-01 intact si aucune nouvelle source.
- [ ] Compteur agrège toutes les sources.

### Tests d'intégration

- [ ] `GET /ai-questions` retourne `critereCode` pour une question taggée.
- [ ] `GET /case-analysis` retourne `piecesManquantesDetails` avec codes.

### Isolation workspace

- [x] Applicable — héritée des endpoints existants. Aucun accès nouveau.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — extension localisée.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `AiQuestionService.generateQuestions()` | prompt rallongé | vérifier tokens, tests existants |
| `SynthesisComponent` | lit `pieces_manquantes` (string) — doit continuer à fonctionner | tests existants inchangés |
| `CaseAnalysisResponse` | nouveau champ, constructeur record rallongé | vérifier toutes les invocations `new CaseAnalysisResponse(...)` |

### Smoke tests E2E concernés

- [ ] Aucun concerné.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IA-03-01` (Done) — moteur d'alerte.
- `SF-IA-03-02` (Done) — source F-96 et pattern tagging Claude.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi pièce manquante ≠ blocker** : absence de preuve ≠ preuve d'absence. L'avocat peut avoir la pièce sans l'avoir uploadée. Signal utile mais faux positif trop probable pour bloquer visuellement.
- **Pourquoi convention "oui = respecté" sur les questions** : sans cette contrainte, il faut stocker la polarité de chaque question (`conforme_si_reponse`), colonne supplémentaire + complexité parsing. La contrainte côté prompt est simple et fiable.
- **Pourquoi interpréter strictement `oui`/`non`** : toute heuristique NLP sur une réponse libre ouvre un champ d'erreurs. Mieux vaut rater des signaux que produire de faux positifs.
- **Pourquoi `piecesManquantes` legacy string conservé** : plusieurs composants existants le consomment (Synthesis, Dashboard). On ajoute `piecesManquantesDetails` en parallèle sans casser l'existant.
