# Mini-spec — F-125 / SF-125-01 Bouton d'analyse contextuel (préserver le contexte avocat)

## Identifiant
`F-125 / SF-125-01`

## Feature parente
`F-125` — Bouton d'analyse contextuel — préserver le travail avocat entre analyses

## Statut
`draft`

## Date de création
`2026-04-18`

## Branche Git
`feat/SF-125-01-bouton-analyse-contextuel`

---

## Objectif

Empêcher la perte involontaire du contexte avocat (réponses aux questions IA, checks procéduraux validés, messages chat, cohérence des outils décisionnels) lorsqu'un avocat relance une analyse sur un dossier déjà travaillé. Rendre le bouton "Analyser" **contextuel** : lancer automatiquement une analyse ENRICHED (qui préserve tout) quand c'est possible, avec une option explicite "Nouvelle analyse complète depuis zéro" masquée derrière un menu secondaire.

---

## Comportement attendu

### Cas nominal

Sur la page `case-file-detail`, le bouton principal d'analyse change de libellé et d'action selon l'état du dossier :

| État du dossier | Label du bouton principal | Action au clic |
|---|---|---|
| Aucune analyse DONE | **"Analyser le dossier"** | STANDARD — `caseAnalysisCommandService.triggerAnalysis()` |
| Analyse STANDARD DONE, aucune réponse Q&A ni nouveau message chat depuis cette analyse | **"Analyser le dossier"** | STANDARD (pas d'enrichissement possible — condition backend `ReAnalysisCommandService` L83-93) |
| Analyse STANDARD DONE + ≥ 1 réponse Q&A récente OU ≥ 1 message chat récent | **"Enrichir la synthèse"** | ENRICHED — `reAnalysisService.reAnalyze()` |
| Analyse ENRICHED DONE, encore de nouvelles réponses/chat depuis | **"Enrichir à nouveau"** | ENRICHED |

Dans tous les cas sauf "aucune analyse", un menu secondaire `⋮` à côté du bouton principal propose :
- **"Nouvelle analyse complète depuis zéro"** → ouvre une `MatDialog` de confirmation → si confirmé, déclenche STANDARD

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Une analyse est déjà en cours (PENDING/PROCESSING) | Le bouton principal est désactivé (comportement actuel, inchangé) |
| L'avocat clique "Enrichir la synthèse" mais les nouvelles réponses / messages chat ont été ajoutés à l'instant et pas encore visibles côté backend (timing) | Le backend retourne `409 CONFLICT` → snackbar "Aucune nouvelle réponse depuis la dernière analyse" (déjà géré par `ReAnalysisService`) |
| L'avocat clique "Nouvelle analyse complète" sans confirmer la dialog | No-op, la dialog se ferme |
| Limite du plan atteinte (quota analyses ou budget tokens) | `402 PAYMENT_REQUIRED` → snackbar upgrade (comportement existant préservé) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable. Le bouton d'analyse n'est pas un outil décisionnel par dossier, c'est le déclencheur global de l'analyse IA.
- [x] **Autres pays** — Non applicable. Le comportement est identique FR/BE (la logique STANDARD vs ENRICHED est la même côté backend pour les deux).
- [x] **Autres domaines** — Non applicable. Le bouton est dans `case-file-detail`, commun aux 3 domaines V1 (travail, immigration, famille).
- [x] **Autres UI patterns** — `MatMenuModule` pour le menu secondaire `⋮`, `MatDialog` pour la confirmation. Patterns déjà utilisés ailleurs dans l'app (export ZIP, clôture dossier).
- [x] **Autres flows transversaux** — Aucun. Pas d'auth nouvelle, pas de changement workspace context.

### Niveaux de vérification couverts

- [x] **Modèle TypeScript / API exposée** — Pas de changement DTO. On ne change que l'appel frontend (`triggerAnalysis` vs `reAnalyze`).
- [x] **Record / DTO backend** — Inchangé.
- [x] **Service / logique métier** — Inchangé backend. Seule la décision d'appel change côté frontend.
- [x] **Entité JPA + schéma DB** — Inchangé.
- [x] **Tests existants** — `case-file-detail.component.spec.ts` à étendre avec les nouveaux cas. Les tests backend sur `CaseAnalysisCommandService` et `ReAnalysisCommandService` restent verts.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] Pas de nouveau service partagé — on réutilise `CaseAnalysisCommandService` et `ReAnalysisService` existants.
- [x] Pas de nouveau pattern UI — `MatMenu` + `MatDialog` sont déjà utilisés ailleurs dans l'app.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| Comportement STANDARD vs ENRICHED côté backend | Oui | Non modifié — on consomme différemment depuis le frontend |
| Logique de détection "peut-on enrichir ?" | Oui | Dupliquée côté frontend (condition `hasNewAnswers \|\| hasNewChatMessages`) — alignée avec `ReAnalysisCommandService` L83-93 backend |
| Bouton similaire dans synthesis.component | Oui | À inspecter : le bouton `reAnalyze()` dans `synthesis.component.html` L534 existe déjà pour ENRICHED. À voir si on le garde ou si on centralise dans `case-file-detail` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Non applicable aux autres cibles (justifiée)

---

## Critères d'acceptation

- [ ] Sur un dossier sans analyse, le bouton principal affiche **"Analyser le dossier"** et lance STANDARD au clic
- [ ] Sur un dossier avec analyse DONE mais aucune nouvelle réponse/chat depuis, le bouton principal reste **"Analyser le dossier"** et lance STANDARD
- [ ] Sur un dossier avec analyse STANDARD DONE + ≥ 1 nouvelle réponse Q&A ou message chat, le bouton principal affiche **"Enrichir la synthèse"** et lance ENRICHED au clic
- [ ] Sur un dossier avec analyse ENRICHED DONE + nouvelles réponses depuis, le bouton principal affiche **"Enrichir à nouveau"** et lance ENRICHED
- [ ] Un menu secondaire `⋮` à côté du bouton principal propose **"Nouvelle analyse complète depuis zéro"** (uniquement si au moins une analyse DONE existe)
- [ ] Cliquer cette option ouvre une `MatDialog` de confirmation avec le titre "Analyse complète depuis zéro" et un message d'avertissement : *"Cette action crée une nouvelle analyse qui ne prendra pas en compte vos réponses aux questions IA, votre validation des points procéduraux, ni vos échanges avec l'assistant chat. Ces données restent accessibles en base mais ne seront plus utilisées par l'analyse. Préférez-vous [Enrichir la synthèse actuelle / Nouvelle analyse complète] ?"*
- [ ] Confirmer la dialog déclenche STANDARD (`caseAnalysisCommandService.triggerAnalysis()`)
- [ ] Annuler la dialog → no-op (aucune analyse déclenchée)
- [ ] Le bouton principal est désactivé si une analyse est déjà en cours (comportement actuel préservé)
- [ ] Si `reAnalyze()` retourne 409 (aucune nouvelle réponse/chat détectée par le backend) → snackbar clair, pas d'erreur brute dans la console
- [ ] Si limite plan atteinte (402) → snackbar upgrade (comportement existant préservé)
- [ ] Le bouton `reAnalyze()` existant dans `synthesis.component` reste fonctionnel (rétrocompatibilité ; on ne le supprime pas dans cette SF pour éviter un scope creep, mais on documente qu'il devient redondant)
- [ ] Tests unitaires `case-file-detail.component.spec.ts` : 6 nouveaux cas couvrent les 4 états du bouton principal + le menu secondaire + la dialog de confirmation
- [ ] Aucune régression sur les 999 tests frontend existants

---

## Périmètre

### Hors scope (explicite)

- Modification backend — `CaseAnalysisCommandService` et `ReAnalysisCommandService` restent inchangés
- Suppression du bouton `reAnalyze()` existant dans `synthesis.component` — à évaluer dans une SF de cleanup ultérieure (risque de casser un habit acquis chez les utilisateurs existants)
- Propagation des checks VERIFIED/NON_COMPLIANT en STANDARD (piste C évoquée en analyse) — à évaluer séparément si retour terrain négatif
- Pagination / affichage des anciennes versions de questions et checks — hors scope (point 1 de l'investigation, volontairement laissé pour plus tard)

---

## Valeurs initiales

Pas d'entité créée par cette SF.

---

## Contraintes de validation

Pas de nouveau champ utilisateur.

**Règles de détection frontend** (alignées sur backend `ReAnalysisCommandService` L83-93) :

- `canEnrich = analysisExists(DONE) && (hasNewAnswersSince(lastEnrichedAt) || hasNewChatMessagesSince(lastEnrichedAt))`
- `hasAnyAnalysis = analysisExists(DONE)`
- `canTriggerStandardFromMenu = hasAnyAnalysis && !analysisInProgress`

Ces conditions sont évaluées côté frontend via les données déjà chargées (`synthesis`, `questions`, `chatMessages`). Aucun nouvel appel API nécessaire.

---

## Technique

### Endpoint(s)

Aucun nouvel endpoint. On consomme les existants :
- `POST /api/v1/case-files/{id}/trigger-analysis` (STANDARD)
- `POST /api/v1/case-files/{id}/re-analysis` (ENRICHED)

### Tables impactées

Aucune.

### Migration Liquibase

Aucune.

### Composants Angular modifiés

| Composant | Modification |
|---|---|
| `case-file-detail.component.ts` | Ajout de getters `canEnrichSynthesis`, `hasAnyAnalysis`, `analysisButtonLabel`. Ajout méthode `onAnalysisButtonClick()` qui dispatch vers STANDARD ou ENRICHED selon l'état. Ajout méthode `onFullReanalysisClick()` qui ouvre la dialog de confirmation. |
| `case-file-detail.component.html` | Remplacer le bouton unique actuel par un `mat-button` + `mat-menu` kebab. Le menu contient "Nouvelle analyse complète depuis zéro" (conditionnel sur `hasAnyAnalysis`). |
| `case-file-detail.component.scss` | Styles mineurs pour aligner le menu avec le bouton principal. |
| `full-reanalysis-confirm-dialog.component.ts` (NEW) | Composant `MatDialog` simple avec titre, message, 2 boutons ("Enrichir la synthèse" recommandé + "Nouvelle analyse complète"). |
| `full-reanalysis-confirm-dialog.component.html` (NEW) | Template. |
| `case-file-detail.component.spec.ts` | 6 tests unitaires ajoutés. |

### Composants frontend (imports Material)

- `MatMenuModule` (si pas déjà importé — à vérifier)
- `MatDialogModule` (déjà utilisé)

---

## Plan de test

### Tests unitaires (case-file-detail.component.spec.ts)

- [ ] `U-CFD-B01` : aucune analyse → `analysisButtonLabel` = "Analyser le dossier", clic → STANDARD appelé
- [ ] `U-CFD-B02` : analyse STANDARD DONE sans réponses Q&A ni chat → label "Analyser le dossier", clic → STANDARD appelé (pas ENRICHED car `canEnrichSynthesis=false`)
- [ ] `U-CFD-B03` : analyse STANDARD DONE + 1 réponse Q&A → label "Enrichir la synthèse", clic → ENRICHED (`reAnalysisService.reAnalyze`) appelé
- [ ] `U-CFD-B04` : analyse STANDARD DONE + 1 message chat récent → label "Enrichir la synthèse", clic → ENRICHED appelé
- [ ] `U-CFD-B05` : menu secondaire "Nouvelle analyse complète" → ouvre la dialog. Confirmation → STANDARD appelé. Annulation → aucun appel.
- [ ] `U-CFD-B06` : analyse en cours (`analysisJob.status === 'PROCESSING'`) → bouton principal désactivé

### Tests d'intégration

- [ ] Aucun — pas de changement backend. Les IT existants sur `/trigger-analysis` et `/re-analysis` restent verts.

### Isolation workspace

- [x] **Non applicable** — l'isolation est gérée côté backend (existante), inchangée.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non touché
- [ ] Workspace context — non touché
- [ ] Plans / limites — non touché (gates PaymentRequired inchangés)
- [x] **Navigation / routing frontend** — le bouton change de comportement mais pas de route. Pas d'impact sur les guards.
- [ ] Aucune préoccupation transversale majeure

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `synthesis.component.html` L534 bouton `reAnalyze()` | Devient redondant avec le nouveau bouton contextuel du parent. Maintenu pour rétrocompat cette SF. | Tests existants `synthesis.component.spec.ts` restent verts |
| Backend `CaseAnalysisCommandService` et `ReAnalysisCommandService` | Aucun changement d'API | IT existants |
| Polling `loadAnalysisJobs` + transitions d'état | Même mécanisme, juste un appel différent | Tests existants `case-file-detail.component.spec.ts` |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — le parcours critique "login → créer dossier → upload → analyser → synthèse" utilise le bouton "Analyser le dossier" dans l'état initial (aucune analyse). Le comportement inchangé pour ce cas → **le test doit rester vert sans adaptation**. À vérifier après dev.

---

## Dépendances

### Subfeatures bloquantes

- F-39 SSE notifications temps réel — **done**
- F-56 Garde relance enrichie — **done** (fournit la condition `hasNewAnswers || hasNewChatMessages`)
- F-90 Chat comme contexte enrichi — **done**

### Questions ouvertes impactées

- [x] Aucune question bloquante de `docs/OPEN_QUESTIONS.md` impactée.

---

## Notes et décisions

### Pourquoi ne pas supprimer le bouton `reAnalyze()` dans `synthesis.component`

Il est déjà documenté et utilisé depuis SF-56-05. Le supprimer dans cette SF créerait un risque de habit utilisateur cassé + scope creep. On le laisse en place en parallèle. Une SF ultérieure (ex. F-125 suite) pourra le supprimer après une période d'observation.

### Pourquoi un menu secondaire `⋮` plutôt qu'un bouton séparé

- Le cas "nouvelle analyse complète depuis zéro" est **rare** (re-upload de documents majeurs invalidant l'ancien contexte)
- Le mettre au même niveau visuel que "Enrichir la synthèse" créerait un **faux choix égal** qui pousserait l'utilisateur à choisir mal (biais de l'UX neutre)
- Le menu secondaire signale "voie alternative, rare, à justifier" — bon contrat UX
- Pattern cohérent avec d'autres actions secondaires dans l'app (kebab menu sur les listes de dossiers, sur la fiche)

### Pourquoi ne pas bloquer l'accès STANDARD quand enrichissement est possible

Un avocat peut vouloir légitimement repartir à neuf (ex. changement de documents fondamentaux, clarification post-information client qui rend l'ancien contexte obsolète). Le bloquer serait autoritaire. L'option reste accessible mais explicite.

### Texte de la dialog de confirmation (version de travail)

> **Analyse complète depuis zéro**
>
> Cette action crée une nouvelle analyse qui **ne prendra pas en compte** :
> - Vos réponses aux questions complémentaires
> - Votre validation des points procéduraux (cochés vérifié / non-respecté)
> - Vos échanges avec l'assistant chat
>
> Ces données restent accessibles dans votre historique mais ne seront plus utilisées par l'analyse suivante.
>
> **Si vous souhaitez préserver votre travail**, préférez "Enrichir la synthèse actuelle".
>
> [Enrichir la synthèse]    [Nouvelle analyse complète]

### Estimations

- Dev : ~1,5-2h (TS + HTML + SCSS + nouveau dialog component)
- Tests : ~45 min (6 cas unitaires + adaptation des tests existants si label change)
- Review + PR : ~15 min

Total : **~2,5-3h**. Largement sous les 2 jours CLAUDE.md.
