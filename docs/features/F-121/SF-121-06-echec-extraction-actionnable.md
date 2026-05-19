# Mini-spec — F-121 / SF-121-06 Échec d'extraction actionnable côté écran

## Identifiant

`F-121 / SF-121-06`

## Feature parente

`F-121` — Gestion visible des extractions échouées

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-121-06-echec-extraction-actionnable`

---

## Objectif

Quand une pièce d'un dossier échoue à l'extraction, donner à l'avocat un message de récupération **visible et spécifique au motif** et un chemin direct vers l'action — au lieu d'une barre rouge sans issue.

---

## Comportement attendu

### Cas nominal

**A — Liste des documents (onglet Dossier).** Un document en `extractionStatus === 'FAILED'` affiche, sous son nom, un **message de récupération visible** (plus seulement un tooltip), **spécifique au `failureReason`** :

| `failureReason` | Message de récupération |
|---|---|
| `OCR_UNSUPPORTED_SIZE` | « Fichier trop volumineux pour l'analyse automatique (max 5 Mo / 11 pages). Divisez-le en fichiers plus légers et ré-uploadez-le. » |
| `EMPTY_TEXT`, `OCR_FAILED` | « Document scanné non reconnu. Utilisez « Relancer avec OCR » ci-dessus, ou remplacez le document. » |
| `CORRUPTED`, `UNSUPPORTED_FORMAT` | « Fichier illisible. Remplacez-le par une version valide puis ré-uploadez. » |
| `EXTRACTION_EXCEPTION` | « L'extraction a échoué. Ré-uploadez le document ; si l'erreur persiste, contactez le support. » |
| `OCR_QUOTA_EXCEEDED` | « Quota OCR atteint. Achetez des pages supplémentaires depuis Abonnement, puis relancez l'OCR. » |

L'action de récupération concrète (suppression) reste le bouton corbeille existant de la colonne actions ; SF-121-06 n'ajoute pas de bouton — elle rend le **message** explicite. Aucun bouton « réessayer » n'est ajouté pour un motif non-retryable.

**B — Pipeline (onglet Analyse).** Quand l'étape « Analyse des documents » est en état `failed`, l'étape affiche un élément cliquable de redirection (ex. « Gérer les documents non analysables »). Le clic émet un `@Output()` consommé par `case-file-detail`, qui **bascule sur l'onglet Dossier** (`TAB_DOSSIER`) et **scrolle vers la liste des documents** (`section-documents`) via le mécanisme `scrollAndHighlight` existant. Le signal rouge F-121-04 est **conservé** ; il devient directionnel.

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| `failureReason` absent / inconnu sur un doc FAILED | Message générique de repli : « Extraction impossible. Ré-uploadez le document ou remplacez-le. » |
| Aucune étape du pipeline en `failed` | Aucun élément de redirection affiché (rendu conditionnel strict). |
| Le clic de redirection survient alors que l'onglet Dossier est déjà actif | Bascule idempotente + scroll ; aucun effet de bord. |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** — non applicable : pipeline d'analyse et liste de documents, pas un outil décisionnel.
- [x] **Autres pays / domaines** — non applicable : aucune logique pays/domaine.
- [x] **Autres UI patterns** — scanné : le message de récupération est un texte d'aide contextuel attaché à une ligne ; pas de nouveau composant partagé. Le pattern « step de pipeline qui émet une intention de navigation » est nouveau mais local à `analysis-pipeline` (voir ci-dessous).
- [x] **Autres flows transversaux** — la bascule d'onglet utilise le signal d'état UI `selectedTabIndex` existant (onglets non routés depuis F-244) — **pas** de routing Angular, pas de guard. Aucune préoccupation transversale navigation au sens routes.

### Cas spécifique — nouveau pattern UI

`analysis-pipeline` émet une **intention de navigation** vers une zone d'un autre onglet via `@Output()`. Zones où ce pattern pourrait se réutiliser : les autres steps du pipeline pourraient à terme pointer vers leur zone d'action (synthèse, etc.). Classé **backlog** — SF-121-06 n'implémente le pattern que pour la step en échec ; pas de généralisation prématurée. Aucun pattern concurrent existant (le pipeline n'émettait aucun événement de navigation jusqu'ici).

### Décision

- [x] Étendu à la cible applicable de cette SF (step en échec). Généralisation du pattern « step → navigation » non traitée ici (pas de besoin avéré sur les autres steps) — pas de cible orpheline.

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — justification : ni `analysis-pipeline` ni la liste des documents ne sont des composants décisionnels (aucune entrée `TOOL_REGISTRY`, aucun endpoint POST/GET décisionnel, aucun verdict). SF purement UX d'un écran existant.

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage** — justification : SF-121-06 ne crée ni ne modifie aucun outil décisionnel à champs saisissables.

---

## Critères d'acceptation

- [ ] Un document `FAILED` affiche un message de récupération **visible** (hors tooltip) sous son nom dans la liste des documents.
- [ ] Le message est **spécifique au `failureReason`** selon le tableau ci-dessus.
- [ ] `OCR_UNSUPPORTED_SIZE` → message « divisez le fichier » ; **aucun** bouton « réessayer » affiché pour ce motif.
- [ ] `EMPTY_TEXT` / `OCR_FAILED` → message renvoyant vers « Relancer avec OCR » (F-122-05) sans dupliquer le bouton.
- [ ] `failureReason` absent/inconnu → message générique de repli.
- [ ] Quand la step « Analyse des documents » est `failed`, un élément cliquable de redirection est affiché.
- [ ] Le clic émet un `@Output()` ; `case-file-detail` bascule sur l'onglet Dossier et scrolle vers `section-documents`.
- [ ] Aucune step non-`failed` n'affiche l'élément de redirection.
- [ ] Aucun changement backend, aucun endpoint, aucun nouveau bloc primaire d'écran.

---

## Périmètre

### Hors scope (explicite)

- Aucun changement backend / pipeline / job (le déblocage est livré par SF-121-05).
- Aucune modification du bandeau retry OCR F-122-05 lui-même (on y renvoie, on ne le réécrit pas).
- Pas de découpe automatique des gros PDF (`OCR_UNSUPPORTED_SIZE`) — relève de F-122.
- **La couleur rouge de la step 2** (décision délibérée F-121-04 : signaler que la synthèse peut être incomplète) est **conservée** — SF-121-06 ajoute l'orientation, ne retire pas le signal.
- Pas de généralisation du pattern « step de pipeline → navigation » aux autres steps.

---

## Contraintes de validation

Aucun champ utilisateur saisissable. SF-121-06 n'ajoute que de l'affichage conditionnel et une intention de navigation. Sans objet.

---

## Technique

### Endpoint(s)

Aucun — aucun endpoint créé ni modifié. SF frontend pure.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `core/models/document.model.ts` — nouveau helper `extractionRecoveryHint(reason): string` (message actionnable par motif ; repli générique). `extractionFailureLabel` (libellé court existant) est conservé.
- `case-files/analysis-pipeline/analysis-pipeline.component.ts` / `.html` / `.scss` — `@Output() manageFailedDocuments`; rendu conditionnel d'un élément cliquable sur une step en état `failed`.
- `case-files/case-file-detail/case-file-detail.component.ts` / `.html` — handler de `manageFailedDocuments` : `selectedTabIndex.set(TAB_DOSSIER)` + `scrollAndHighlight('section-documents')` (mécanismes existants) ; affichage du message `extractionRecoveryHint` sous le nom du document `FAILED` dans la liste.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `document.model` — `extractionRecoveryHint` : un cas par `failureReason` du tableau + cas `null`/inconnu → repli.
- [ ] `AnalysisPipelineComponent` — step `failed` → l'élément de redirection est rendu ; step `done`/`active-*` → non rendu.
- [ ] `AnalysisPipelineComponent` — clic sur l'élément → `manageFailedDocuments` émis.
- [ ] `CaseFileDetailComponent` — réception de `manageFailedDocuments` → `selectedTabIndex` passe à `TAB_DOSSIER` et `scrollAndHighlight('section-documents')` est appelé.
- [ ] `CaseFileDetailComponent` — un document `FAILED` rend le message `extractionRecoveryHint` correspondant à son `failureReason`.

### Tests d'intégration

- [x] Non applicable — SF frontend pure, aucun endpoint. Couverture par tests Jest composant.

### Isolation workspace

- [x] Non applicable — aucun accès aux données ; affichage conditionnel sur des données déjà chargées et déjà filtrées par workspace en amont.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale.** La bascule d'onglet utilise le signal d'état UI `selectedTabIndex` (onglets non routés, F-244) — pas de route Angular, pas de guard, pas de redirection au sens routing. Auth / workspace / plans : non touchés.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `AnalysisPipelineComponent` | Ajout d'un `@Output()` + rendu conditionnel — les steps non-`failed` ne doivent pas changer | Spec : steps `done`/`active` inchangées, pas d'élément de redirection |
| `CaseFileDetailComponent` (liste docs) | Ajout d'un message sous le nom du doc `FAILED` — les docs `DONE` inchangés | Spec : doc `DONE` → aucun message de récupération |

### Smoke tests E2E concernés

- [x] Aucun smoke test E2E concerné — pas de route, pas d'auth, pas de workspace context modifié. Les onglets ne sont pas routés. Couverture par tests Jest composant.

---

## Dépendances

### Subfeatures bloquantes

- SF-121-05 (job `DOCUMENT_ANALYSIS` terminal) — `done`, mergée et déployée prod 2026-05-19. Sans elle, le bouton supprimer resterait grisé pendant le blocage ; avec elle, le parcours de récupération est fonctionnel et SF-121-06 le rend lisible.

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

- **Décision F-121-04 conservée explicitement** : la step « Analyse des documents » reste rouge en cas d'échec partiel (avertissement délibéré que la synthèse peut être incomplète). SF-121-06 n'altère pas cette couleur ; elle ajoute un élément d'orientation. Toute évolution future de la sémantique couleur serait une SF distincte actant le changement de la décision F-121-04.
- **Pas d'action mensongère** (invariant étape 0) : aucun bouton « réessayer » générique. Pour `EMPTY_TEXT`/`OCR_FAILED`, on renvoie vers le retry OCR **existant** (F-122-05) ; pour `OCR_UNSUPPORTED_SIZE`, la seule issue honnête est « diviser le fichier ».
- **Bridge inter-onglets** (ajustement étape 0 bis) : le signal d'échec de l'onglet Analyse renvoie vers l'onglet Dossier où vit l'action — réutilise `selectedTabIndex` + `scrollAndHighlight`, déjà en place.
