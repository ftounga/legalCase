# Mini-spec — F-IM-08 / SF-IM-08-08 — Frontend référés administratifs L.521-1 / L.521-2 (FR)

## Identifiant

`F-IM-08 / SF-IM-08-08`

## Feature parente

`F-IM-08` — Outil décisionnel OQTF / OQT (FR + BE)

## Statut

`in-progress`

## Date de création

2026-04-25

## Branche Git

`feat/SF-IM-08-08-frontend-referes-admin`

---

## Objectif

Exposer l'outil décisionnel "Référés administratifs L.521-1 / L.521-2" (FR uniquement) côté frontend Angular, en consommant l'API figée par SF-IM-08-07 (backend) — saisie du contexte d'urgence + 2 scoring parallèles (Suspension 30j vs Liberté 48h) + verdict de recommandation entre les deux référés.

> Contrat API importé de **SF-IM-08-07** (backend).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre le panel décisionnel sur un dossier d'immigration FR. La section "Référés administratifs L.521-1 / L.521-2" est affichée par F-IA-04 quand la règle backend l'autorise.
2. La section apparait **collapsed** par défaut. Au déploiement, un GET `/api/v1/case-files/{id}/referes-admin` est tenté :
   - 200 OK : on affiche le verdict + scores + délais déjà calculés (mode résultat) ;
   - 404 : on reste en mode formulaire.
3. Si `aiData.dateNotificationDecisionContestee`, `aiData.typeRecoursCode` ou `aiData.transfertImminentDetected` sont disponibles dans la synthèse, les champs correspondants sont **pré-remplis** avec un badge "Pré-rempli depuis l'analyse" (icône `auto_awesome`).
4. L'avocat saisit/ajuste 8 champs (typeRefere, decisionContestee, dateNotification, 3 toggles conditions, preuvesUrgence multi-select, demandeurDejaPrived) puis valide.
5. POST `/api/v1/case-files/{id}/referes-admin` → backend retourne 2 scores parallèles `scoreSuccessProbabiliteSuspension` (0..100) et `scoreSuccessProbabiliteLiberte` (0..100), un `verdictRecommandation`, les délais `delaiJugeTaJoursL521_1` (≈30j) et `delaiJugeTaHeuresL521_2` (48h), `baseJuridique`, `formule`, `messages`.
6. Affichage : bannière verdict (rouge dominant — urgence 48h documentée), 2 scores côte-à-côte (Suspension L.521-1 / Liberté L.521-2) + délai associé en JetBrains Mono, recommandation finale en grand, base juridique + formule + messages.
7. `CaseDashboardRefreshService.triggerRefresh()` est appelé après POST succès.
8. F-IA-03 : alertes de cohérence inline sur 3 champs clés (DATE_NOTIFICATION, DECISION_CONTESTEE, ATTEINTE_LIBERTE).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| `workspaceCountry === 'BELGIQUE'` | Bannière info "Référé administratif — procédure française uniquement. Pour la Belgique, voir le recours en suspension Conseil d'État (annexe 13)." | — |
| Date notification > aujourd'hui | Submit désactivé, `[max]="todayIso"` sur l'input | 400 (backend) |
| Aucune preuve d'urgence + `urgenceCaracterisee = false` | Submit autorisé (le backend retourne verdict `AUCUN_PROBABLE`) | 200 |
| Backend 4xx/5xx | MatSnackBar erreur avec message backend | — |
| GET 404 | Mode formulaire, pas d'erreur visible | — |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Autres outils métier** : F-IM-08-02 (OQTF avec délai FR), F-IM-08-04 (OQTF sans délai FR), F-IM-08-06 (Annexe 13 BE), F-IM-05/06/07 (immigration FR/BE)
- [x] **Autres pays** : FR uniquement (le contentieux du référé administratif L.521-1/L.521-2 est strictement français — la Belgique a un mécanisme distinct via Conseil d'État + extrême urgence). Gate `workspaceCountry === 'FRANCE'`.
- [x] **Autres domaines** : non applicable (immigration uniquement, pas de jumeau Travail/Famille — un référé liberté art. 521-2 CJA peut concerner d'autres droits fondamentaux mais ce SF cible le contexte OQTF/transfert/CRA hardcodé dans le contrat backend).
- [x] **Autres UI patterns** : pré-fill IA (pattern `oqtf-avec-delai-section`), validation F-IA-03 (pattern `oqtf-sans-delai-section`), `CoherencePopoverTriggerDirective`, palette rouge dominant (justification urgence 48h L.521-2), `<input type="date">` natif.
- [x] **Flows transversaux** : aucun (workspace context géré via `@Input() workspaceCountry`).

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — 3 alertes de cohérence (DATE_NOTIFICATION, DECISION_CONTESTEE, ATTEINTE_LIBERTE) via `CoherencePopoverTriggerDirective` + `CoherenceAlertBuilder`.
- [x] **Refresh dashboard (F-IA-02)** : oui — `CaseDashboardRefreshService.triggerRefresh()` après POST.
- [x] **Pré-remplissage IA** : oui — `prefillFromAi()` sur `dateNotificationDecisionContestee` (depuis `ImmigrationExtractedData.dateNotificationDecisionContestee`) et `decisionContestee` (depuis `typeRecoursCode` mapping) et `transfertImminentDetected` (preuvesUrgence pré-fill `TRANSFERT_IMMINENT`).
- [x] **Persistance des inputs** : oui (gérée par backend SF-IM-08-07).
- [x] **Masquage conditionnel** : gate `workspaceCountry === 'FRANCE'` avec bannière info si BELGIQUE.
- [x] **Alertes actives après calcul** : `coherenceAlerts` gated par `!this.showForm()` uniquement (pas de `|| this.result()` — bug SF-IA-03-12 évité).

### Cas spécifique : nouveau pattern UI ou service partagé

Aucun nouveau pattern partagé. Réutilise :
- `CoherencePopoverTriggerDirective` (existant)
- `CoherenceAlertBuilder` (existant, `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`)
- `LegalCitationsPipe` (existant)
- `DecisionalHeaderFlagComponent` (existant, SF-155-07 DIV-6)
- `SourceExplanationService` (existant, SF-155-07 DIV-7)

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| OQTF avec délai FR (SF-IM-08-02) | Oui (jumeau structure) | Pattern repris (gate FR + pré-fill IA + alertes F-IA-03) |
| OQTF sans délai FR (SF-IM-08-04) | Oui (urgence 48h) | Palette rouge dominant repris (justifié 48h L.521-2) |
| Annexe 13 BE (SF-IM-08-06) | Non (pas de référé admin équivalent en BE) | Bannière info "FR uniquement" |
| F-IA-03 cohérence | Oui | Intégrée dans cette SF (3 alertes) |
| F-IA-02 refresh | Oui | Intégrée dans cette SF |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [x] Subfeature(s) parallèle(s) créée(s) : non — backend déjà figé (SF-IM-08-07)
- [x] Backlog : non
- [x] Non applicable aux autres cibles (BE / Travail / Famille — référé administratif L.521-1/L.521-2 strictement FR)

---

## Critères d'acceptation

- [x] CA1 : composant `<app-referes-admin-section>` standalone avec selector `app-referes-admin-section`, intégré au `TOOL_REGISTRY` du panel F-IA-04 sous tool_id `F-IM-08-referes-admin-fr`.
- [x] CA2 : gate `workspaceCountry === 'FRANCE'` avec bannière info FR uniquement si BELGIQUE.
- [x] CA3 : formulaire à 8 champs : `typeRefere` (mat-select 3 options), `decisionContestee` (mat-select 5 options), `dateNotificationDecision` (input type=date), 3 slide-toggles (`urgenceCaracterisee`, `atteinteLiberteFondamentale`, `doutesSerieuxLegalite`), `preuvesUrgence` (mat-select multiple 6 options), `demandeurDejaPrived` (slide-toggle).
- [x] CA4 : POST/GET `/api/v1/case-files/{id}/referes-admin` consommés via `RefereAdminService`.
- [x] CA5 : affichage résultat = bannière verdict (palette rouge dominant justifiée 48h L.521-2), 2 scores parallèles côte-à-côte (Suspension/Liberté) avec délai en JetBrains Mono, recommandation, base juridique, formule, messages.
- [x] CA6 : pré-fill IA depuis `ImmigrationExtractedData` sur 3 champs minimum + badges `auto_awesome` "Pré-rempli depuis l'analyse" + handlers `onXxxChange()` qui effacent la provenance.
- [x] CA7 : F-IA-03 — `coherenceAlerts` computed avec 3 alertes (DATE_NOTIFICATION, DECISION_CONTESTEE, ATTEINTE_LIBERTE) + `CoherencePopoverTriggerDirective` câblé.
- [x] CA8 : `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [x] CA9 : MatSnackBar pour erreurs HTTP.
- [x] CA10 : >= 10 tests Jest couvrant mount, GET 200/404, form valid, POST succès/erreur, gate FR, pré-fill IA, F-IA-03, palette.

---

## Périmètre

### Hors scope (explicite)

- Pas de support BE (mécanisme juridiquement distinct).
- Pas de génération PDF requête référé (SF future).
- Pas de timer décompte heures restantes (le délai 48h est associé à L.521-2 uniquement, pas à l'introduction du référé lui-même).
- Pas de pré-fill `preuvesUrgence` exhaustif (uniquement `TRANSFERT_IMMINENT` si `transfertImminentDetected`).

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|----------------------------|---------------|
| typeRefere | Oui | `SUSPENSION` \| `LIBERTE` \| `LES_DEUX` | — |
| decisionContestee | Oui | `OQTF` \| `RETRAIT_TITRE` \| `REFUS_TITRE` \| `IRTF` \| `AUTRE_MESURE_ADMIN` | — |
| dateNotificationDecision | Oui | YYYY-MM-DD, ≤ today | — |
| urgenceCaracterisee | Oui | boolean | — |
| atteinteLiberteFondamentale | Oui | boolean | — |
| doutesSerieuxLegalite | Oui | boolean | — |
| preuvesUrgence | Oui | array de `MENACE_VIE_PRIVEE` \| `TRANSFERT_IMMINENT` \| `IMPACT_SCOLARITE_ENFANTS` \| `RUPTURE_SOINS_MEDICAUX` \| `RISQUE_VIOLENCES_PAYS_ORIGINE` \| `AUTRE` (peut être vide) | — |
| demandeurDejaPrived | Oui | boolean | — |

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/referes-admin` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/referes-admin` | Oui | MEMBER |

### Tables impactées

Aucune côté frontend.

### Migration Liquibase

Non applicable.

### Composants Angular

- `RefereAdminSectionComponent` — outil décisionnel section.
- `RefereAdminService` — wrapper HttpClient.
- `referes-admin.model.ts` — types TypeScript.

---

## Plan de test

### Tests unitaires (Jest)

- [x] mount — composant créé, formValid() = false par défaut.
- [x] FRANCE — `isFrance() === true`, GET appelé au ngOnInit.
- [x] BELGIQUE — `isFrance() === false`, pas d'appel HTTP, bannière info affichée.
- [x] GET 200 — résultat chargé, `showForm() === false`, scores hydratés.
- [x] GET 404 — mode formulaire conservé, pas d'erreur snack.
- [x] POST succès — résultat affiché, `triggerRefresh()` appelé.
- [x] POST erreur — snackBar affiché.
- [x] Pré-fill IA — `dateNotificationDecisionContestee` IA → champ rempli + provenance IA.
- [x] handler onChange — modification manuelle efface la provenance IA.
- [x] F-IA-03 — divergence date saisie vs IA → alerte WARNING émise dans `coherenceAlerts`.
- [x] F-IA-03 critique — `atteinteLiberteFondamentale=false` + `urgenceCaracterisee=true` + L.521-2 attendu → alerte CRITICAL.

### Tests d'intégration

Côté frontend : pas d'IT distinct (Jest mocks suffisent — l'IT réel est côté backend SF-IM-08-07).

### Isolation workspace

Non applicable côté frontend (gérée par backend).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — création nouveau composant décisionnel
- [ ] Aucune préoccupation transversale

### Composants existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `decisional-tools-panel.component.ts` | Ajout entrée TOOL_REGISTRY `F-IM-08-referes-admin-fr` | Tests existants du panel restent verts |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (composant isolé, pas de modification du shell ou de la navigation).

---

## Impact par domaine métier

- **Droit du travail (FR + BE)** : non applicable (référé admin = contentieux administratif, pas prud'homal).
- **Droit de la famille (FR + BE)** : non applicable.
- **Droit de l'immigration FR** : applicable directement — outil cible précisément les contestations OQTF/IRTF/RETRAIT_TITRE/REFUS_TITRE.
- **Droit de l'immigration BE** : non applicable — recours suspension Conseil d'État BE est juridiquement distinct (pas de symétrie L.521-1/L.521-2). Bannière info renvoie vers l'outil annexe 13 BE.

---

## Parité des domaines métier

L'outil livré est un **scoring** (niveau 5 sur les 7 niveaux de profondeur) :

- Travail : pas d'équivalent (pas de référé pour licenciement — la procédure prud'homale est intrinsèquement contradictoire).
- Famille : pas d'équivalent direct (les ordonnances de protection ne nécessitent pas un scoring de validité 521-1/521-2 — F-FA-14 traite déjà cet outil avec ses propres critères).
- Immigration BE : non — pas de symétrie procédurale.

**Justification** : le référé administratif L.521-1/L.521-2 est un mécanisme propre au contentieux administratif français, sans équivalent direct dans les 2 autres domaines ni en BE. Aucune feature jumelle au backlog requise.

---

## Dépendances

### Subfeatures bloquantes

- `SF-IM-08-07` — backend référés admin — statut : à figer (parallélisation décidée)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Palette rouge dominant** : autorisée par DESIGN_SYSTEM.md pour les urgences absolues (≤ 72h). Le L.521-2 a un délai de jugement de 48h (R.522-1 CJA), le L.521-1 typiquement 30 jours mais peut être urgent. Documentée dans le SCSS.
- **Pré-fill `decisionContestee`** : mapping `typeRecoursCode` → `decisionContestee` (ex: `OQTF` → `OQTF`, `REFUS_TITRE` → `REFUS_TITRE`). Si non mappable, no-op silencieux.
- **2 scores parallèles** : choix volontaire d'afficher les deux (suspension/liberté) plutôt que masquer celui qui ne correspond pas au `typeRefere` choisi — l'avocat doit voir l'arbitrage du verdict.
- **`demandeurDejaPrived`** : pré-fill possible depuis `placementCraDetected` (priver de liberté = privation au sens L.521-2) — mais champ sémantiquement différent, on choisit de **ne pas** pré-remplir (risque faux positif).
