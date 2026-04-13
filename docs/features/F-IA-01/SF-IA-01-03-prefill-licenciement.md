# Mini-spec — F-IA-01 / SF-IA-01-03 Pré-remplissage grille Validité licenciement

## Identifiant

`F-IA-01 / SF-IA-01-03`

## Feature parente

`F-IA-01` — Pré-remplissage automatique des outils métier depuis l'analyse IA

## Statut

`draft`

## Date de création

2026-04-13

## Branche Git

`feat/SF-IA-01-03-prefill-licenciement`

---

## Objectif

Étendre l'extraction IA et le pré-remplissage au bloc F-DT-08 (Validité licenciement) pour que les 7 critères FR et les 7 critères BE soient pré-cochés depuis la synthèse IA, au lieu de laisser l'avocat tout remplir à la main.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance une analyse IA sur un dossier DROIT_DU_TRAVAIL (FR ou BE).
2. Le prompt IA étendu demande à Claude d'évaluer 7 critères par pays à partir des documents et de renvoyer pour chacun : `reponse` (`OUI` / `NON` / `INCONNU`) + `justification` (citation du document source si disponible).
3. Les données sont persistées dans `case_analyses.licenciement_validity_detection_json` et exposées dans `CaseAnalysisResponse.licenciementValidityDetection`.
4. `LicenciementSectionComponent` reçoit `aiData` via `@Input` et appelle `prefillFromAi()` sur `ngOnChanges`.
5. Chaque critère dont la détection IA renvoie une valeur différente de `INCONNU` est pré-coché avec la valeur détectée.
6. L'avocat peut surcharger chaque réponse. Une fois analysé (POST `/licenciement`), c'est la réponse avocat qui est persistée — la détection IA reste lisible en lecture seule (source pour F-IA-03 à venir).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Prompt IA renvoie un JSON invalide sur le bloc détection | Fail-open : extraction ignorée, grille reste vide | 200 |
| Aucun document exploitable | Toutes les détections = `INCONNU`, aucune radio pré-cochée | 200 |
| Workspace pays différent des critères détectés | Seuls les critères du pays du workspace sont utilisés | 200 |
| Dossier hors DROIT_DU_TRAVAIL | Extraction non déclenchée, champ absent de la réponse | 200 |

---

## Critères d'acceptation

- [ ] Le prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` inclut l'extraction des 7 critères FR + 7 critères BE avec `reponse` et `justification`.
- [ ] `TravailExtractedData` (ou nouveau `LicenciementValidityDetection`) expose les 14 critères sous une forme Map<String, DetectedAnswer>.
- [ ] `LicenciementSectionComponent` pré-coche chaque critère quand `aiData` contient une détection ≠ `INCONNU`.
- [ ] L'avocat peut surcharger la réponse — le POST `/licenciement` persiste la valeur humaine, pas la détection IA.
- [ ] Fail-open : JSON malformé → grille vide, aucune erreur bloquante.
- [ ] Isolation workspace : un workspace FR ne reçoit que les critères FR, un BE ne reçoit que les BE.
- [ ] Tests unitaires backend (extraction, fail-open) verts.
- [ ] Tests unitaires frontend (prefill `ngOnChanges`, override) verts.
- [ ] Suite complète backend + frontend verte.

---

## Périmètre

### Hors scope (explicite)

- Le **contrôle de cohérence** (alerte quand l'avocat contredit la détection IA) — c'est F-IA-03 / SF-IA-03-01.
- L'extension du prefill aux autres outils déjà couverts par F-IA-01.
- La persistance de la détection IA dans une table dédiée (on la garde dans le JSON `case_analyses`).
- L'affichage de la justification IA à côté de chaque critère (prévu F-IA-03).

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `licenciement_validity_detection_json` | `null` | peuplé uniquement si analyse DROIT_DU_TRAVAIL et parsing OK |
| Réponses avocat dans `licenciement_analyses` | inchangé | toujours source de vérité pour le calcul de score |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|----------------------------|---------------|
| `reponse` | Oui (dans le JSON IA) | `OUI` / `NON` / `INCONNU` | upper-case, défaut `INCONNU` si absent |
| `justification` | Non | texte libre ≤ 500 caractères | trim, tronqué à 500 si plus long |
| Clé critère | Oui | doit matcher un `code` existant dans `LicenciementCritereReferentiel.ALL` | ignorée sinon (fail-open) |

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. Extension de la réponse existante :

| Méthode | URL | Changement |
|---------|-----|------------|
| GET | `/api/v1/case-files/{id}/case-analysis` | ajoute `licenciementValidityDetection` dans le payload |

### Tables impactées

Aucune. La détection IA est reparsée à la volée depuis `case_analyses.analysis_result` (raw JSON LLM), même pattern que `TravailExtractedData`. Pas de colonne dédiée.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable** — réutilisation du raw JSON `analysis_result` existant, pas de schéma à modifier.

### Composants Angular (si applicable)

- `LicenciementSectionComponent` — ajouter `@Input() aiData?: LicenciementValidityDetection | null` + logique `ngOnChanges` + `prefillFromAi()`.
- `case-file-detail.component.html` — passer `[aiData]="synthesis()?.licenciementValidityDetection"`.
- `case-analysis.model.ts` — ajouter le type `LicenciementValidityDetection`.

---

## Plan de test

### Tests unitaires backend

- [ ] `CaseAnalysisResponse.extractLicenciementValidityDetection()` — JSON nominal FR parsé.
- [ ] Idem avec JSON nominal BE.
- [ ] JSON malformé → fail-open, détection null.
- [ ] Clés inconnues ignorées silencieusement.
- [ ] `reponse` non normalisée (`oui`) → `OUI`.
- [ ] `reponse` absente → `INCONNU`.
- [ ] `justification` > 500 caractères tronquée.

### Tests unitaires frontend

- [ ] `LicenciementSectionComponent` pré-coche les radios quand `aiData` contient des détections FR.
- [ ] Idem BE.
- [ ] `INCONNU` → pas de pré-remplissage.
- [ ] L'avocat peut override la valeur pré-cochée.
- [ ] `ngOnChanges` ne réinitialise pas une saisie en cours si `aiData` ne change pas.

### Tests d'intégration

- [ ] `GET /case-analysis` retourne `licenciementValidityDetection` pour un dossier DROIT_DU_TRAVAIL FR.
- [ ] Idem BE (critères BE uniquement).
- [ ] Autre domaine (DROIT_IMMIGRATION) → champ absent.
- [ ] `POST /licenciement` ignore `licenciementValidityDetection` — persiste uniquement les réponses avocat.

### Isolation workspace

- [x] Applicable — la détection est lue depuis `case_analyses` qui filtre déjà par `workspace_id`. Vérifier qu'un dossier cross-workspace renvoie 403 sur `GET /case-analysis`.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension localisée du pipeline IA + d'un composant Angular.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseAnalysisResponse` | ajout d'un champ nullable | suite tests existants reste verte |
| `LegalDomainPromptBuilder` | prompt TRAVAIL rallongé → vigilance tokens | vérifier longueur prompt < limite |
| `LicenciementSectionComponent` | flux prefill ajouté | test override avocat |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — les smoke tests actuels (`auth`, `workspace`, `navigation`) ne traversent pas le bloc licenciement.

---

## Dépendances

### Subfeatures bloquantes

- Aucune.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi un champ JSON plutôt qu'une table dédiée** : la détection IA est éphémère (regénérée à chaque analyse) et sert de source pour F-IA-03. Pas besoin d'historique ni de CRUD — JSON dans `case_analyses` suffit et évite une migration lourde.
- **Pourquoi conserver la valeur avocat même quand la détection IA est présente** : principe F-IA-01 — l'IA suggère, l'avocat décide. Le calcul de score F-DT-08 reste strictement basé sur les réponses humaines.
- **Préparation F-IA-03** : la détection IA (reponse + justification) sera réutilisée telle quelle par le moteur de cohérence. Garder la structure simple et stable.
