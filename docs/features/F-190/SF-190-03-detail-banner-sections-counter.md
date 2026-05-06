# Mini-spec — F-190 / SF-190-03 — Compteur "X/7 sections reçues" dans le bandeau page détail

## Identifiant

`F-190 / SF-190-03`

## Feature parente

`F-190` — Progression granulaire de l'analyse + preview live des sections (extension F-185)

## Statut

`in-progress`

## Date de création

2026-05-06

## Branche Git

`feat/SF-190-03-detail-banner-sections-counter`

---

## Objectif

Afficher la progression numérique **"X/7 sections reçues"** dans le bandeau "Analyse du dossier en cours…" de la **page détail du dossier** (case-file-detail), pour donner à l'avocat le même signal de progression que sur la page synthèse — sans avoir à naviguer vers `/synthesis` pour voir où en est la génération.

---

## Comportement attendu

### Cas nominal

1. Avocat clique "Analyser le dossier" → bandeau "Analyse du dossier en cours…" apparaît
2. **Avant** : bandeau indéterminé, l'avocat ignore le degré d'avancement (« est-ce dans 5 s ou dans 1 min ? »)
3. **Après** : sous le label principal, sous-ligne fine "X/7 sections reçues" qui s'incrémente au fil des events SSE `CASE_ANALYSIS_PARTIAL` (1/7 → 2/7 → … → 7/7)
4. Bandeau et compteur disparaissent quand `CASE_ANALYSIS_DONE` arrive (cf. F-159 / SF-159-03 deja en place)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucun event PARTIAL reçu encore (analyse vient juste de démarrer) | Compteur affiche "0/7 sections reçues" |
| Endpoint `getPartial` répond 404 (pas d'analyse en flight) | Compteur masqué (la sous-ligne ne s'affiche pas) |
| `CASE_ANALYSIS` pas dans les jobs actifs (ex : seul `DOCUMENT_ANALYSIS` ou `ENRICHED_ANALYSIS` tourne) | Sous-ligne sections masquée — le compteur n'a de sens que pour la synthèse standard |
| `ENRICHED_ANALYSIS` en cours (re-synthèse) | Idem nominal — SF-190-02 a étendu le streaming PARTIAL à `ENRICHED_ANALYSIS`, donc le compteur s'applique aussi |

---

## Cause / contexte

F-190 SF-190-01 a livré la barre "X/7 sections" sur la page synthèse uniquement. Le scope explicite excluait la page détail :

> Hors scope : (a) progression granulaire dans le bandeau case-file-detail (F-159) — la grille F-162 sur la synthèse couvre déjà ce besoin

Constat utilisateur 2026-05-06 : le besoin existe bien sur la page détail, parce que c'est la page la plus consultée pendant qu'une analyse tourne (l'avocat reste sur le détail pour voir les pièces, les délais, etc., et regarde de temps en temps si la synthèse avance). Naviguer vers `/synthesis` juste pour voir le compteur est friction.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable (mécanisme de bannière progress, pas un outil décisionnel)
- [x] **Autres pays** — non applicable (transversal)
- [x] **Autres domaines** — non applicable (transversal)
- [x] **Autres UI patterns** — la `<mat-progress-bar>` granulaire existe déjà sur la page synthèse via SF-190-01. Cette SF la projette sur la page détail.
- [x] **Autres flows transversaux** — l'event SSE `CASE_ANALYSIS_PARTIAL` est déjà consommé par `synthesis.component.ts` ; cette SF ajoute un consommateur `case-file-detail`. Pas de nouveau type d'event.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] La constante `STREAMING_EXPECTED_SECTIONS` (déclarée en `synthesis.component.ts`) doit être **extraite dans un fichier partagé** pour réutilisation par `case-file-detail`. Évite la duplication. Pattern concurrent : aucun (la liste est unique). Cible : `frontend/src/app/case-files/streaming-sections.ts` (ou colocalisé sous `synthesis/`).
- [x] Le `decisional-tools-progress-banner.component.ts` reçoit deux nouveaux Inputs (`sectionsReceived`, `sectionsExpected`) — pattern reste local au composant, pas de service partagé créé.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Page synthèse (SF-190-01) | Oui | Refactor : import depuis le fichier partagé |
| Page détail (case-file-detail) | Oui | Cible principale : PARTIAL event → getPartial → streamingProgress → banner Input |
| `<app-decisional-tools-progress-banner>` | Oui | Inputs sectionsReceived / sectionsExpected ajoutés, sous-ligne conditionnelle |
| `<app-analysis-pipeline>` | Non | Niveau plus bas, donne déjà le statut PENDING/PROCESSING/DONE par job — pas le bon endroit pour le compteur de sections IA |
| Bandeau de re-synthèse enrichie (`reAnalyzing()` text) | Couvert par cette SF | Le banner reçoit aussi le compteur quand `ENRICHED_ANALYSIS` actif (SF-190-02 streaming étendu à enrichi) |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (synthesis refactor + case-file-detail wiring + banner Inputs)

---

## Critères d'acceptation

- [ ] **AC1** : `STREAMING_EXPECTED_SECTIONS` (+ interface `StreamingSection`) extrait dans `frontend/src/app/case-files/synthesis/streaming-sections.ts` et importé par `synthesis.component.ts` sans changement comportemental
- [ ] **AC2** : `case-file-detail.component.ts` souscrit aux events SSE `CASE_ANALYSIS_PARTIAL` (et `ENRICHED_ANALYSIS_PARTIAL` si SF-190-02 émet ce nom — sinon réutiliser le mécanisme existant). Au reçu, fetch `caseAnalysisService.getPartial(caseFileId)`, stocke le résultat dans signal `lastPartial`
- [ ] **AC3** : `case-file-detail.component.ts` expose un computed `streamingProgress` `{received, expected}` calculé à partir de `lastPartial()` et `STREAMING_EXPECTED_SECTIONS`
- [ ] **AC4** : `decisional-tools-progress-banner.component.ts` reçoit deux nouveaux Inputs : `@Input() sectionsReceived: number | null = null` et `@Input() sectionsExpected: number = 0`. Affiche une sous-ligne `"{received}/{expected} sections reçues"` **uniquement si** :
  - `sectionsReceived !== null && sectionsExpected > 0`
  - `CASE_ANALYSIS` ou `ENRICHED_ANALYSIS` est dans `activeJobTypes`
- [ ] **AC5** : Quand le bandeau est affiché pour `DOCUMENT_ANALYSIS` ou `QUESTION_GENERATION` seuls (pas de CASE_ANALYSIS / ENRICHED en cours), la sous-ligne sections est masquée — elle n'a de sens que pour les analyses qui produisent les 7 sections
- [ ] **AC6** : Tests Jest unitaires :
  - Banner : 5 nouveaux UT (sous-ligne affichée / masquée selon les 2 inputs et le job type)
  - case-file-detail : 3 nouveaux UT (PARTIAL event → getPartial appelé, streamingProgress cohérent avec lastPartial, computed = 0 si pas de partial)
- [ ] **AC7** : `synthesis.component.ts` continue à passer ses tests existants (refactor non-régressif)
- [ ] **AC8** : Aucune modification backend

---

## Périmètre

### Hors scope (explicite)

- Cliquabilité des sections (scroll vers la zone) — comportement spécifique à la page synthèse, sans intérêt sur le détail
- Animation/transition à l'incrémentation
- Refactor de `decisional-tools-progress-banner` au-delà des 2 nouveaux Inputs (pas de transition vers signals computed standalone)
- Affichage du nom des sections déjà arrivées (juste compteur, pattern minimal)

---

## Technique

### Endpoint(s) consommé(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| GET | `/api/v1/case-files/{id}/case-analysis/partial` | Oui | MEMBER |
| GET (SSE) | `/api/v1/case-files/{id}/analysis-status/stream` | Oui | MEMBER |

(endpoints existants, aucune modification backend)

### Composants Angular impactés

| Composant | Type de modification |
|-----------|---------------------|
| `synthesis/streaming-sections.ts` | **Nouveau fichier** — constante + interface partagées |
| `synthesis/synthesis.component.ts` | Import depuis fichier partagé (suppression de la déclaration locale) |
| `case-file-detail/case-file-detail.component.ts` | Souscription PARTIAL + signal `lastPartial` + computed `streamingProgress` |
| `case-file-detail/case-file-detail.component.html` | Passage des Inputs `sectionsReceived` / `sectionsExpected` au banner |
| `decisional-tools-panel/decisional-tools-progress-banner.component.ts` | 2 nouveaux Inputs + template sous-ligne |
| `decisional-tools-panel/decisional-tools-panel.component.html` | Forwarder les Inputs si le banner est dans le panel (à vérifier) |

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

---

## Plan de test

### Tests unitaires (frontend)

- [ ] `decisional-tools-progress-banner.component.spec.ts` :
  - Affiche "X/Y sections reçues" quand `sectionsReceived=3, sectionsExpected=7, activeJobTypes=['CASE_ANALYSIS']`
  - Affiche "X/Y sections reçues" quand `activeJobTypes=['ENRICHED_ANALYSIS']` (re-synthèse)
  - Masque la sous-ligne quand `sectionsReceived=null`
  - Masque la sous-ligne quand `sectionsExpected=0`
  - Masque la sous-ligne quand `activeJobTypes=['DOCUMENT_ANALYSIS']` seul (pas de CASE_ANALYSIS / ENRICHED)
- [ ] `case-file-detail.component.spec.ts` :
  - PARTIAL event → `caseAnalysisService.getPartial` est appelé
  - `streamingProgress()` retourne `{received: 0, expected: 7}` quand `lastPartial=null`
  - `streamingProgress()` retourne `{received: 3, expected: 7}` quand `lastPartial.sections` contient 3 clés présentes

### Tests d'intégration

- [x] Aucun (frontend pure)

### Isolation workspace

- [x] Non applicable (déjà couvert par les endpoints consommés, pas modifiés)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — extension UX d'un bandeau existant + abonnement à un event SSE déjà émis

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E ne couvre la bannière (justification : compteur informatif sans impact fonctionnel sur navigation, auth, workspace ; les tests Jest unitaires couvrent le comportement)

---

## Dépendances

### Subfeatures bloquantes

- `SF-190-01` — done (compteur sur synthesis)
- `SF-190-02` — done (streaming étendu à enriched)
- `SF-185-01` — done (event PARTIAL sur SSE)
- `SF-159-01` — done (banner existant)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Pourquoi extraire `STREAMING_EXPECTED_SECTIONS` dans un fichier partagé

Plutôt que dupliquer la constante dans `case-file-detail`, on l'extrait pour garantir l'invariant "même liste partout" — si demain on ajoute une 8e section, on modifie un seul endroit.

### Pourquoi pas un service partagé (`StreamingProgressService`)

Considéré, mais surdimensionné pour un compteur. La logique tient en un computed de 5 lignes ; en faire un service ajouterait de la cérémonie (DI, providers, tests d'isolation). Si demain un 3e consommateur émerge, on extraira à ce moment-là.

### Pourquoi le compteur disparaît pour DOCUMENT_ANALYSIS / QUESTION_GENERATION

Les 7 sections sont produites uniquement par `CaseAnalysisService` (synthèse standard) et `EnrichedAnalysisService` (re-synthèse, depuis SF-190-02). DOCUMENT_ANALYSIS extrait des données par document (pas un découpage en 7 sections) ; QUESTION_GENERATION produit une liste de questions (pas découpé non plus). Afficher "0/7 sections" ou "—/7 sections" pendant ces phases serait trompeur.
