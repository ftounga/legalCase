# Mini-spec — F-FA-24 / SF-FA-24-12 Frontend indivision successorale

## Identifiant

`F-FA-24 / SF-FA-24-12`

## Feature parente

`F-FA-24` — Droit des successions FR (chantier successions, jumeau frontend de SF-FA-24-11 mergé PR #681).

## Statut

`in-progress`

## Date de création

2026-04-26

## Branche Git

`feat/SF-FA-24-12-frontend-indivision-successorale`

---

## Objectif

Composant Angular `<app-indivision-successorale-section>` exposant l'outil "Indivision successorale" (FR — art. 815 à 832-2 + 1873-1 et s. + 815-1 et s. Cciv) au sein du panel décisionnel F-IA-04, avec pré-fill IA + alertes F-IA-03 sur la date d'ouverture de succession et le type d'indivision, gate FR + bannière info BE, et bandeau verdict gestion (HARMONIEUSE / CONFLICTUELLE / BLOCAGE).

Contrat importé de `SF-FA-24-11-backend-indivision-successorale.md` (mergé PR #681).

---

## Comportement attendu

### Cas nominal

L'avocat ouvre le panel décisionnel d'un dossier FR Famille, déplie l'outil "Indivision successorale", saisit :

- type d'indivision : `INDIVISION_LEGALE` / `INDIVISION_CONVENTIONNELLE` / `MAINTIEN_FORCE`
- date d'ouverture de succession (= origine de l'indivision)
- nombre d'héritiers (2-50)
- valeur du patrimoine indivis en €
- valeur du bien occupé en € (optionnel, ≤ valeur patrimoine)
- consentements de tous les indivisaires (oui/non)
- occupation exclusive par un indivisaire (oui/non)
- actes d'administration contestés (oui/non)
- demande de partage (oui/non)

→ POST `/api/v1/case-files/{id}/indivision-successorale-analysis`. Le backend retourne :
- `verdictGestion` : HARMONIEUSE / CONFLICTUELLE / BLOCAGE
- `dispositifRecommande` : MAINTIEN_INDIVISION_LEGALE / CONVENTION_INDIVISION_5_ANS / MEDIATION_FAMILIALE / PARTAGE_AMIABLE / PARTAGE_JUDICIAIRE / MAINTIEN_FORCE_PRESERVE
- `indemniteOccupationDueEur`, `fraisGestionEstimesEur`, `dureeIndivisionMois`, `scoreConflictualite`, `baseJuridique`, `formule`, `messages`

Le composant affiche un bandeau verdict (palette navy/or — rouge réservé au BLOCAGE), un panneau de chiffres (indemnité occupation, frais gestion, durée), la liste des messages, la formule (JetBrains Mono), la base juridique, et un bouton "Modifier".

### Bannière info BE

Si workspace = BELGIQUE, le composant affiche une bannière `info` indiquant que l'outil est propre au droit français (art. 815+ Cciv), aucun appel HTTP n'est fait. Bannière jamais critique : palette navy. Pas de masquage silencieux.

### Pré-fill IA (RÈGLE FONDAMENTALE)

Au mount + sur `ngOnChanges(aiData)` post-initialisation tant que la résolution backend n'a pas eu lieu, le composant pré-remplit :

- `dateOuvertureSuccession` ← `aiData.dateOuvertureSuccessionDetectee` (existe déjà dans `FamilleExtractedData` — réutilisé de SF-FA-24-08).
- `typeIndivision` ← `aiData.typeIndivisionSuccessoraleDetecte` (NOUVEAU champ optionnel, parse `LEGALE` / `CONVENTIONNELLE` / `MAINTIEN_FORCE` ou alias canoniques).

Chaque champ pré-rempli affiche un badge `auto_awesome` "Pré-rempli depuis l'analyse" tant que l'avocat ne le modifie pas. Au 1er changement manuel, `provenance<Field>` repasse à `null` et le badge disparaît.

Pré-fill silencieux si `aiData` absent. N'écrase jamais une saisie avocat (provenance !== 'IA').

### Validation F-IA-03 au changement (RÈGLE FONDAMENTALE)

Sur les champs `DATE_OUVERTURE_SUCCESSION` et `TYPE_INDIVISION`, le composant exécute un `computed<coherenceAlerts>` qui consolide 4 sources :

1. **F-96** — `procedureChecks` matchant les `critereCode` :
   - `INDIVISION_DATE_OUVERTURE`, `INDIVISION_TYPE`.
2. **QUESTION_IA** — `aiQuestions` matchant les mêmes `critereCode`, `expectedValue` ou `answerText`.
3. **IA** — `aiData.dateOuvertureSuccessionDetectee` / `aiData.typeIndivisionSuccessoraleDetecte`.
4. **PIECE_MANQUANTE** — pieces matching `INDIVISION`, `INDIVISION_CONVENTION`, `ACTE_NOTORIETE`, `JUGEMENT_MAINTIEN`.

Le helper `CoherenceAlertBuilder.forField<F>(field)` est utilisé pour chaque alerte. La hiérarchie F96 > QUESTION_IA > IA > PIECE_MANQUANTE est respectée. Source `MULTI` si ≥ 2 contributors.

Chaque alerte est rendue via `<app-coherence-popover-trigger>` à côté du champ ; pas de blocage technique du POST.

### Cas d'erreur frontend

| Situation | Comportement |
|-----------|-------------|
| `dateOuvertureSuccession` future | bouton désactivé (formValid false) + 400 backend → snack rouge |
| `nbHeritiers` < 2 ou > 50 | bouton désactivé |
| `valeurPatrimoineIndivisEur` < 0 | bouton désactivé |
| `valeurBienOccupeEur` > patrimoine total | bouton désactivé |
| `typeIndivision` non sélectionné | bouton désactivé |
| 400 backend (validation supplémentaire) | snack rouge, mode form conservé |
| 404 GET au mount | mode formulaire (vide ou pré-rempli IA) |

---

## Critères d'acceptation

- [ ] Composant standalone Angular `<app-indivision-successorale-section>` créé sous `frontend/src/app/case-files/indivision-successorale-section/`.
- [ ] Modèle `indivision-successorale.model.ts` (TypeScript) aligné sur le record backend (request 9 champs, response 21 champs).
- [ ] Service `IndivisionSuccessoraleService` (POST + GET) sous `frontend/src/app/core/services/`.
- [ ] Entrée `TOOL_REGISTRY` `'F-FA-24-indivision-successorale'` dans `decisional-tools-panel.component.ts` avec inputs symétriques (caseFileId, workspaceCountry, aiData, procedureChecks, aiQuestions, piecesManquantes).
- [ ] Pré-fill IA fonctionnel sur `dateOuvertureSuccession` et `typeIndivision` (badge `auto_awesome`).
- [ ] Coherence alerts F-IA-03 : `coherenceAlerts` computed, builder partagé, popover câblé, source `MULTI` si convergence.
- [ ] Bannière info BE (jamais masquage).
- [ ] Bandeau verdict palette : HARMONIEUSE → info navy, CONFLICTUELLE → warn or, BLOCAGE → critical rouge.
- [ ] `MatSnackBar` pour erreurs (jamais alert/confirm).
- [ ] `CaseDashboardRefreshService.triggerRefresh()` après POST succès.
- [ ] JetBrains Mono pour `formule` + `baseJuridique`, Inter pour le reste.
- [ ] Champ `typeIndivisionSuccessoraleDetecte` ajouté à `FamilleExtractedData` (optionnel, string nullable).
- [ ] Tests Jest ≥ 12.
- [ ] Self-check grep 5/5 (voir Plan de test).

---

## Plan de test (≥ 12)

### Gate pays + init

1. FRANCE → `isFrance()` true, GET appelé au ngOnInit.
2. BELGIQUE → `isFrance()` false, aucun appel HTTP (gate pays).
3. GET 200 → mode résultat, provenance reset.
4. GET 404 → mode formulaire, pré-fill possible.

### Pré-fill IA

5. Pré-fill IA `dateOuvertureSuccession` + `typeIndivision` ← `aiData` + badges.
6. Pas de pré-fill si `aiData` absent.
7. Avocat change `typeIndivision` → provenance vidée.
8. `ngOnChanges(aiData)` post-mount rafraîchit le pré-fill si form vide.
9. `ngOnChanges(aiData)` post-saisie ne réécrase pas la saisie avocat.

### Form validation + POST

10. `formValid` false initialement.
11. `formValid` true avec champs minimaux requis.
12. `formValid` rejette `nbHeritiers` < 2 et > 50.
13. `formValid` rejette `valeurBienOccupeEur` > `valeurPatrimoineIndivisEur`.
14. `calculate()` POST envoie le body attendu + résultat + snackbar succès.
15. `calculate()` ignoré si invalide (pas d'appel HTTP).
16. `calculate()` erreur backend → snackbar rouge.

### F-IA-03 alertes

17. `coherenceAlerts.TYPE_INDIVISION` divergence IA visible si saisie diverge.
18. `coherenceAlerts.DATE_OUVERTURE_SUCCESSION` MULTI source F96 + IA → MULTI.
19. `coherenceAlerts` vides après calcul (showForm=false).

### Helpers UI

20. `verdictBannerClass`/`verdictChipClass` : BLOCAGE → critical, CONFLICTUELLE → warn, HARMONIEUSE → info.
21. `verdictIcon` : BLOCAGE=gpp_bad, CONFLICTUELLE=warning, HARMONIEUSE=verified.
22. `toggleCollapse` + `editMode` fonctionnels.

### Self-check grep pré-commit (anti-régression)

1. `grep -n "MatSnackBar" indivision-successorale-section/*.ts` → ≥ 1 import + ≥ 1 usage.
2. `grep -n "CaseDashboardRefreshService" indivision-successorale-section/*.ts` → 1 import + 1 triggerRefresh.
3. `grep -n "CoherenceAlertBuilder" indivision-successorale-section/*.ts` → ≥ 1 usage.
4. `grep -n "auto_awesome" indivision-successorale-section/*.html` → ≥ 1 (pré-fill badge).
5. `grep -n "isFrance\|workspaceCountry" indivision-successorale-section/*.ts` → ≥ 2 (gate pays).

---

## Tables / endpoints / composants impactés

- **Composant** : `frontend/src/app/case-files/indivision-successorale-section/` (4 fichiers : `.ts` / `.html` / `.scss` / `.spec.ts`).
- **Modèle** : `frontend/src/app/core/models/indivision-successorale.model.ts`.
- **Service** : `frontend/src/app/core/services/indivision-successorale.service.ts`.
- **Modèle étendu** : `FamilleExtractedData` (ajout `typeIndivisionSuccessoraleDetecte?`) — `divorce-accepte.model.ts`.
- **TOOL_REGISTRY** : ajout entrée `'F-FA-24-indivision-successorale'` dans `decisional-tools-panel.component.ts`.
- **Endpoints** : POST + GET `/api/v1/case-files/{id}/indivision-successorale-analysis` (déjà figés SF-FA-24-11).

## Hors périmètre

- Backend (mergé SF-FA-24-11 PR #681).
- Régime BE (CC BE art. 577-2 et s. — feature jumelle backlog).
- Calcul détaillé compte d'indivision (V1 forfait).
- Formulaire de génération de convention d'indivision (V1 oriente uniquement).

---

## Impact par domaine métier

Composant **strictement Droit de la famille FR**. Bannière BE, sortie 400 silencieuse côté Travail/Immigration (gate backend). Pas d'impact transversal.

## Parité des domaines métier

Outil de niveau 5. Pas d'équivalent dans Travail/Immigration. Famille BE → backlog (CC BE art. 577-2 +).

## Analyse de cohérence transversale

| Cible | Statut |
|-------|--------|
| `donation-section` (F-FA-24-06) — pattern de référence | **intégré** (palette, F-IA-03, pré-fill IA, banners) |
| `reserve-heriditaire-section` (F-FA-24-08) — sibling FR | **intégré** (gate FR, dateOuvertureSuccession reuse) |
| `indivision-section` (F-FA-22) — pattern jumeau divorce | différent (origine = divorce, pas succession) — **pas de partage code** |
| `partage-judiciaire-section` (F-FA-24-10) | dispositif renvoyé `PARTAGE_JUDICIAIRE` (orientation) — **pas de couplage UI** |
| Outils Travail/Immigration | non applicable |

## Nouveau pattern UI ou service partagé

Aucun nouveau pattern transversal — on réutilise `CoherenceAlertBuilder` (SF-155-05), `CaseDashboardRefreshService`, `LegalCitationsPipe`, `CoherencePopoverTriggerDirective`. SCSS : palette canonique.
