# Mini-spec — F-219 / SF-219-06b Frontend Outil licenciement BE fermeture entreprise

## Identifiant

`F-219 / SF-219-06b`

## Feature parente

`F-219` — P3 Travail BE — ~32 outils BE-only spécificité

## Statut

`ready`

## Date de création

2026-05-28

## Branche Git

`feat/SF-219-06b-frontend-licenciement-be-fermeture-entreprise`

---

## Objectif

Implémenter le composant frontend Angular OnPush de l'outil **licenciement BE fermeture d'entreprise** (Fonds Fermeture Entreprises FFE — Loi 26/06/2002 + AR 23/03/2007 + CCT n° 9bis) : formulaire de saisie + analyse + résultat avec verdict 6 états + base juridique. Câblage TOOL_REGISTRY + visibility seed ALWAYS_ON 124 BE / DROIT_DU_TRAVAIL.

---

## Source juridique BE

- **Loi du 26/06/2002** relative aux fermetures d'entreprises (cadre FFE)
- **AR du 23/03/2007** d'exécution (modalités indemnité forfaitaire + supplément âge)
- **CCT n° 9bis** (information / consultation préalable du Conseil d'entreprise)
- Indemnités prises en charge par le **Fonds de Fermeture des Entreprises** géré par l'ONEM
- BE uniquement — pas d'équivalent strict FR (FNGS couvre les salaires mais pas l'indemnité de fermeture forfaitaire)

---

## Comportement attendu

### Cas nominal

Affichage du composant dans le panel décisionnel F-IA-04 quand `workspaceCountry === 'BELGIQUE'` et `legalDomain === 'DROIT_DU_TRAVAIL'`. Visibility ALWAYS_ON priority 124 (au-dessus de outplacement-be-general-30sem = 123 SF-219-05b).

Form en 10 champs requis :
- `dateNaissance` (date) — calcul âge + supplément ≥ 45 ans
- `dateDebutContrat` (date) — calcul ancienneté (seuil 1 an)
- `dateFermeture` (date) — date cessation officielle ou jugement faillite
- `remunerationMensuelleBrute` (€) — base FFE
- `typeFermeture` (enum 6 valeurs) — CESSATION_DEFINITIVE / FAILLITE_LIQUIDATION_JUDICIAIRE / CESSATION_PARTIELLE / TRANSFERT_CCT_32BIS / FUSION_SANS_SUPPRESSION / FERMETURE_TEMPORAIRE
- `statutEmployeur` (enum 3 valeurs) — SOLVABLE / INSOLVABLE_AVERE / FAILLITE_DECLAREE
- `effectifEtp` (entier) — seuil 20 ETP (régime général V1)
- `salairesImpayes` (€)
- `peculeVacancesImpaye` (€)
- `indemniteRuptureImpayee` (€)

Submit → POST endpoint backend → résultat avec verdict 6 états :
- **ELIGIBLE_FFE_COMPLET** — fermeture qualifiée + insolvable (vert, full FFE + indemnité)
- **ELIGIBLE_FFE_REPRISE_CREANCES** — fermeture qualifiée + insolvable (vert, créances reprises)
- **ELIGIBLE_INDEMNITE_SEULE** — fermeture qualifiée + solvable (vert, indemnité seule)
- **INELIGIBLE_ANCIENNETE_INSUFFISANTE** — < 1 an ancienneté (rouge)
- **INELIGIBLE_TYPE_FERMETURE** — type non qualifiant (rouge)
- **INELIGIBLE_SEUIL_EFFECTIF** — < 20 ETP (rouge)

Affichage outputs : `ageADateFermeture`, `anneesAnciennete`, `indemniteFermeture`, `montantForfaitaireParAnnee`, `supplementAgeMensuel`, `montantTotalCreancesFfe`, `synthese`, `baseJuridique`, `avertissement`.

GET initial : si analyse existe, hydrate result + form pour édition.

### Cas d'erreur

| Situation | Code | Comportement |
|---|---|---|
| `workspaceCountry !== 'BELGIQUE'` | n/a | Bannière « réservé droit BE » + isAvailable=false |
| POST 404 | 404 | snackBar « Dossier introuvable » |
| POST 400 | 400 | snackBar avec message backend |
| POST 5xx | 5xx | snackBar générique |
| GET 404 | 404 | mode formulaire vierge |

---

## Critères d'acceptation

- [x] Composant Angular OnPush standalone créé.
- [x] Helper de prefill colocalisé (V1 = 0 champ).
- [x] Service HttpClient consommant l'endpoint backend.
- [x] Model TypeScript aligné sur le DTO backend.
- [x] TOOL_REGISTRY + THEME_BY_TOOL_ID enrichis.
- [x] Migration Liquibase visibility ALWAYS_ON 124 BE / DROIT_DU_TRAVAIL.
- [x] Tests Jest composant + prefill + service (~35-44).
- [x] `DashboardTileToolIdIntegrityIT` + `DecisionToolVisibilityIntegrityIT` verts.

---

## Périmètre

### Hors scope

- Backend (déjà livré SF-219-06 PR #1388).
- Autres outils F-219.

---

## Technique

### Composants frontend

- `frontend/src/app/case-files/licenciement-be-fermeture-entreprise-section/licenciement-be-fermeture-entreprise-section.component.{ts,html,scss,spec.ts}`
- `frontend/src/app/case-files/licenciement-be-fermeture-entreprise-section/licenciement-be-fermeture-entreprise-section-prefill-rules.{ts,spec.ts}`
- `frontend/src/app/core/models/licenciement-be-fermeture-entreprise.model.ts`
- `frontend/src/app/core/services/licenciement-be-fermeture-entreprise.service.ts`

### Modif `decisional-tools-panel.component.ts`

- import du composant
- entry TOOL_REGISTRY `licenciement-be-fermeture-entreprise`
- entry THEME_BY_TOOL_ID = `INDEMNITES` (calcul d'indemnité de fermeture + créances FFE = thème indemnités)

### Migration Liquibase

`386-seed-licenciement-be-fermeture-entreprise-visibility.xml` — changeset id `ac0dbd86-d610-4607-9ec5-1a1b486d6e00` author `claude` — INSERT visibility ALWAYS_ON priority 124 BE / DROIT_DU_TRAVAIL — reversible.

### Pré-fill IA V1

Aucun champ pré-rempli — alignement pattern uniforme F-213/F-219. Helper `getPrefillCount` retourne 0. Saisie avocat depuis attestation employeur / déclaration ONEM-FFE / jugement de faillite.

---

## Plan de test

### Unitaires composant (`*.component.spec.ts`)

- Gate pays BELGIQUE/FRANCE
- GET 200 / GET 404
- formValid : avec / sans champs
- calculate : POST OK + erreurs 400/404/500 + FR
- Verdict 6 états : label + classe + icône + badge
- Affichage outputs (indemnité, créances, supplément âge)
- Avertissement / synthèse
- editMode + toggleCollapse + forceExpanded
- handlers signals
- static getPrefillCount → 0
- static metadata TOOL_LABEL/ICON

### Unitaires helper (`*prefill-rules.spec.ts`)

- count = 0 quel que soit l'input (V1)
- contrat F-236 (nombre fini, non-NaN, ≥ 0)

### Service (`*.service.spec.ts`)

- POST endpoint correct
- GET endpoint correct

### Isolation workspace

- [x] Standard — gate BE strict côté composant + backend 404 FR.

---

## Analyse d'impact

### Préoccupations transversales

- [x] **Workspace context** — gate BE strict côté composant.
- [x] **Outil décisionnel métier** — création card frontend, miroir SF-219-04b.
- Auth / Plans / Navigation — non touchés.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `decisional-tools-panel.component.ts` | +import + entry TOOL_REGISTRY + THEME_BY_TOOL_ID | Tests existants |
| `DecisionToolVisibilityIntegrityIT` | seed visibility + frontend TOOL_REGISTRY alignés | Run vert |
| `DashboardTileToolIdIntegrityIT` | tool_id déjà ajouté à KNOWN_NO_DASHBOARD_TILE_IDS (PR #1388 préventif) | Run vert |

---

## Dépendances

- SF-219-06 backend mergée (PR #1388) — endpoint + entité disponibles.
- En parallèle avec SF-219-05b — conflits panel possibles, rebase + garder les 2 entries (mémoire `feedback_rebase_resolution_tool_registry`).
