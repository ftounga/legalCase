# Mini-spec — F-177 / SF-177-09 Dashboard agrégé en cards + modal (absorbe F-167)

## Identifiant

`F-177 / SF-177-09`

## Feature parente

`F-177` — Refonte panel F-IA-04. Cette SF **absorbe F-167** (dashboard agrégé étendu).

## Statut

`draft`

## Date de création

2026-05-01

## Branche Git

`feat/SF-177-09-dashboard-agrege`

---

## Objectif

Refactoriser `<app-case-dashboard>` pour rendre ses 9 tiles existantes via le composant partagé `<app-decision-tool-card>` (SF-177-01), avec clic → ouverture dans le `MatDialog` de `DecisionToolModalService` (SF-177-02). Cohérence visuelle parfaite avec le panel décisionnel + comportement uniforme (cards = clic = modal).

---

## Position dans le découpage F-177

Dernière SF utile (10/10 du plan initial). Après merge :

- F-177 statut **Terminée** (10 SF mergées : 01/02/03/03b/05/07/09/10/11)
- F-167 statut **Terminée** (absorbée par SF-177-09)
- F-168 bis **Terminée** (absorbée par SF-177-10 mergée)

---

## Scope (révisé vs F-167 ambitieux)

Le découpage F-167 d'origine prévoyait 5 SF (~4 h) pour étendre le dashboard à ~78 outils en introduisant un DTO backend `DashboardTile` générique. **Décision SF-177-09** : se contenter de **réutiliser le composant card** sur les 9 tiles existantes — pas de nouveau backend, pas d'extension à 78 outils. Si le besoin d'étendre apparaît post-MEP, ouvrir une feature successeur dédiée (F-178+) avec le DTO générique. La valeur immédiate est :

1. **Cohérence visuelle** entre dashboard et panel (même composant card)
2. **Comportement uniforme** : clic dashboard = clic panel = même modal
3. **Hygiène code** : retire le SCSS custom `.dash-card` redondant avec `<app-decision-tool-card>`

---

## Comportement attendu

### Cas nominal

1. `<app-case-dashboard>` charge `DashboardResponse` via `CaseDashboardService` (logique inchangée).
2. Pour chacun des 9 champs non-null (`licenciement`, `indemnites`, `anciennete`, `titleDecision`, `workRight`, `recours`, `partage`, `garde`, `divorce`), le composant produit un `DashboardTile` :
   ```typescript
   interface DashboardTile {
     toolId: string;                // mapping vers TOOL_REGISTRY
     theme: string;                 // pour le styling card (DIAGNOSTIC / INDEMNITES / VALIDITE / DELAIS / DOCUMENTS)
     title: string;                 // lu via TOOL_LABEL static du composant outil
     icon: string;                  // lu via TOOL_ICON static
     summary: DecisionToolSummary;  // verdict synthétique calculé depuis le champ DashboardResponse
     metierAlertLevel?: MetierAlertLevel;
     component: Type<unknown>;      // le composant outil à instancier dans le modal
     componentInputs: Record<string, unknown>;
   }
   ```
3. Le template rend chaque tile via `<app-decision-tool-card>` avec les inputs ci-dessus + handler `(open)="openTile(tile)"`.
4. Le riskScore tile (`riskScore` + `riskLevel`) reste un **tile spécial non-cliquable** rendu via `<app-decision-tool-card [disabled]="true">` (pas de tool correspondant en `TOOL_REGISTRY`, c'est une métrique agrégée).
5. Au clic sur une tile (autre que riskScore), `openTile(tile)` appelle `DecisionToolModalService.open()` avec `{ toolId, title, icon, component, inputs: { ...componentInputs, forceExpanded: true } }` — exact symétrique de `openTool()` dans le panel SF-177-11.
6. Le titre `<h3>"Tableau de bord décisionnel"</h3>` et le message vide sont conservés. Le SCSS custom `.dash-card` / `.card-alert` / `.card-ok` est retiré (porté par `<app-decision-tool-card>`).

### Mapping DashboardResponse → toolId

| Champ DashboardResponse | toolId | Composant outil | Theme |
|------------------------|--------|----------------|-------|
| `licenciement` | `F-DT-08-licenciement-validity` | `LicenciementSectionComponent` | VALIDITE |
| `indemnites` | `F-DT-09-comparateur-indemnites` | `IndemniteComparatifSectionComponent` | INDEMNITES |
| `anciennete` | `F-DT-07-anciennete-conges-prime` | `AncienneteSectionComponent` | DELAIS |
| `titleDecision` | `F-IM-05-arbre-decisionnel-titre` | `ImmigrationTitleDecisionSectionComponent` | DIAGNOSTIC |
| `workRight` | `F-IM-07-droit-au-travail` | `ImmigrationWorkRightSectionComponent` | DIAGNOSTIC |
| `recours` | `F-IM-06-recours` | `ImmigrationRecoursSectionComponent` | DELAIS |
| `partage` | `F-FA-05-partage-immobilier` | `PartageImmobilierSectionComponent` | DIAGNOSTIC |
| `garde` | `F-FA-06-calendrier-garde` | `CalendrierGardeSectionComponent` | DIAGNOSTIC |
| `divorce` | `F-FA-07-checklist-divorce` | `DivorceChecklistSectionComponent` | DOCUMENTS |
| `riskScore` | _(aucun)_ | _(aucun — tile non cliquable)_ | DIAGNOSTIC |

### Construction de `DecisionToolSummary` par tile

| Champ | label | primaryValue | secondaryValue | alertLevel |
|-------|-------|-------------|---------------|-----------|
| `licenciement` | "Validité" | `{verdict}` (ex. "INVALIDE") | `{criteresNonConformes}/{criteresTotal} non conformes` | `ALERT` si verdict ≠ "VALIDE", `OK` sinon |
| `indemnites` | "Indemnités" | `{fourchetteBasse} – {fourchetteHaute} €` | `{baremeSource}` | — |
| `anciennete` | "Ancienneté" | `{annees} an(s) {mois} mois` | `{congesTotalJours}j congés` | `WARNING` si `ecartsDetectes > 0` |
| `titleDecision` | "Titre" | `{nbRecommandations} recommandation(s)` | `{premierTitreLabel}` | — |
| `workRight` | "Droit au travail" | `{droitTravail}` | `{titreLabel}` | `OK` si "OUI", `ALERT` si "NON" |
| `recours` | "Recours" | `{recoursLabel}` | `{dateLimite}` | `ALERT` si `dateLimiteDepassee` |
| `partage` | "Partage" | `{soulte} €` | `Coût total : {coutTotal} €` | — |
| `garde` | "Garde" | `{gardeLabel}` | `{joursParentA}j / {joursParentB}j` | — |
| `divorce` | "Divorce" | `{progressionPct} %` | `{etapesCompletees}/{etapesTotal} étapes` | — |
| `riskScore` (special) | "Risque" | `{riskScore} %` | `{riskLevel}` | `OK` si <30, `WARNING` si <60, `ALERT` sinon |

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Dashboard data null / vide | Le composant ne rend rien (`hasAnyData()` reste false) — comportement actuel préservé |
| `getToolMetadata(component)` retourne null | Tile rendue avec fallback `{ label: toolId, icon: 'extension' }` (cohérent SF-177-11) — pas de crash |
| Composant outil sans `forceExpanded` | Idem SF-177-11 : géré par instrumentation pattern B (tous les composants en `TOOL_REGISTRY` l'ont) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 9 tiles existantes refactorisées ; le riskScore reste hors-tool (non cliquable). Pas d'extension à d'autres outils dans cette SF (hors-scope explicite).
- [x] **Autres pays** : DashboardResponse contient `country` pour indemnites BE/FR — préservé (le composant outil F-DT-09 gère lui-même la variation pays).
- [x] **Autres domaines** : 9 tiles couvrent Travail (4) + Immigration (3) + Famille (3 dont divorce) — pas de bias par domaine.
- [x] **Autres UI patterns** : voir §"nouveau pattern UI ou service partagé" ci-dessous.
- [ ] **Autres flows transversaux** : auth/workspace/plans/navigation — non concernés (le composant tourne dans la même page dossier).

### Niveaux de vérification

- [x] **Modèle TypeScript** : nouveau type local `DashboardTile` colocalisé dans `case-dashboard.component.ts`.
- [ ] **Record / DTO backend** : non touché (DashboardResponse inchangé).
- [x] **Service / logique métier** : `CaseDashboardService` inchangé, ajout d'injection `DecisionToolModalService` côté composant.
- [ ] **Entité JPA + schéma DB** : non applicable.
- [x] **Tests existants** : `case-dashboard.component.spec.ts` couvre actuellement les 9 tiles — assertions sur les valeurs affichées doivent être adaptées au nouveau template (selectors `<app-decision-tool-card>` au lieu de `.dash-card`).

### Cas spécifique : nouveau pattern UI ou service partagé

SF-177-09 ne crée **ni nouveau composant ni nouveau service**. Elle consomme `<app-decision-tool-card>` (SF-177-01) et `DecisionToolModalService` (SF-177-02). Le type `DashboardTile` est **local à `case-dashboard.component.ts`** et ne sera pas réutilisé ailleurs (sinon il faudra le promouvoir en partagé).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 9 tiles existantes | Oui | Refactorisées dans cette SF |
| ~70 autres outils non couverts par dashboard | Oui mais hors-scope | Reportés en feature successeur F-178+ si besoin |
| Composants outils déjà instrumentés (SF-177-03/03b/05/07) | Oui | Réutilisés via `TOOL_REGISTRY` |
| Tests `case-file-detail` | Oui | Vérifier que la balise `<app-case-dashboard>` reçoit bien les nouveaux inputs `[synthesis]/[workspaceCountry]/[procedureChecks]/[aiQuestions]` |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature : 9 tiles existantes refactorisées
- [x] Backlog : extension à ~70 outils restants reportée en F-178 si besoin émerge post-MEP
- [ ] Non applicable aux autres cibles

---

## Critères d'acceptation

- [ ] Template `case-dashboard.component.html` rend chaque tile via `<app-decision-tool-card>` (sauf riskScore qui utilise aussi la card mais avec `[disabled]="true"`)
- [ ] `<div class="dash-card">` retiré du template (et SCSS associé `.dash-card`, `.card-alert`, `.card-ok`, `.card-value`, `.card-label`, `.card-sub`, `.progress-mini*` retiré du SCSS)
- [ ] Méthode `tilesFromDashboard(d: DashboardResponse): DashboardTile[]` produit les 9 tiles + le riskScore (10 entrées max), filtre les nulls
- [ ] `tilesFromDashboard` calcule un `DecisionToolSummary` cohérent par tile (cf. tableau Construction)
- [ ] `tilesFromDashboard` lit `TOOL_LABEL` / `TOOL_ICON` via `getToolMetadata(component)` (pattern symétrique SF-177-11)
- [ ] Méthode `openTile(tile: DashboardTile)` appelle `DecisionToolModalService.open()` avec `{ toolId, title, icon, component, inputs: { ...tile.componentInputs, forceExpanded: true } }`
- [ ] Le tile riskScore n'a **pas** de handler open (disabled) — clic ignoré
- [ ] `<app-case-dashboard>` accepte 4 nouveaux inputs : `synthesis`, `workspaceCountry`, `procedureChecks`, `aiQuestions` — passés à `componentInputsFor` pour calculer les inputs des composants outils
- [ ] `case-file-detail.component.html` ligne 501 transmet les 4 inputs au dashboard (alignement avec le panel ligne 503-510)
- [ ] Tests Jest existants `case-dashboard.component.spec.ts` adaptés aux nouveaux selectors (`<app-decision-tool-card>` plutôt que `.dash-card`)
- [ ] 1 test neuf SF-177-09 : `openTile` appelle `DecisionToolModalService.open` avec le bon component + `forceExpanded: true`
- [ ] 1 test neuf SF-177-09 : `tilesFromDashboard` produit 9 entrées + 1 riskScore quand tous les champs sont peuplés
- [ ] Tests existants `case-file-detail.component.spec.ts` qui réfèrent au dashboard restent verts (pas d'assertion sur le markup interne dashboard)
- [ ] Build Angular vert
- [ ] Suite Jest complète verte (≥ 3961 tests)

---

## Périmètre

### Hors scope (explicite)

- **Extension à ~70 autres outils** : reportée en F-178 si besoin émerge post-MEP. Le DTO backend `DashboardTile` générique de F-167 n'est PAS introduit ici (pas justifié sur 9 tiles seulement).
- **Re-design des verdicts** : on conserve la sémantique actuelle (les valeurs affichées sont les mêmes, juste portées par la card).
- **Refresh service** : `CaseDashboardRefreshService` continue de déclencher un reload de DashboardResponse (logique existante préservée).
- **Mini-progress bar du divorce** : la `progress-mini-bar` actuelle disparaît (la card n'a pas d'emplacement pour) — remplacée par le `primaryValue` "X %" et le `secondaryValue` "X/Y étapes — A/B pièces". Le `riskScore` perd sa color custom (le `[style.color]="riskColor()"`) — remplacé par `metierAlertLevel`.
- **Backend** : aucun changement.
- **Smoke E2E** : aucun ajout (couverture par tests Jest).

---

## Valeurs initiales

Aucune entité créée.

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|-------|-------------|--------|-------|
| `tilesFromDashboard` retour | Oui | `DashboardTile[]` | Liste filtrée (champs nulls exclus). Ordre : licenciement, indemnites, anciennete, titleDecision, workRight, recours, partage, garde, divorce — riskScore préfixé en tête si non null. |
| `openTile` arg | Oui | `DashboardTile` | Provenance : `tilesFromDashboard` → garanti non-null si `disabled === false` |

---

## Technique

### Endpoint(s)

Aucun (réutilise `GET /api/v1/case-files/{id}/dashboard` existant).

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular

- `CaseDashboardComponent` (modifié) — ajoute 4 inputs (`synthesis`, `workspaceCountry`, `procedureChecks`, `aiQuestions`), méthodes `tilesFromDashboard()` et `openTile()`, importe `DecisionToolCardComponent` + `DecisionToolModalService` + `getToolMetadata` + types `DecisionToolSummary` / `MetierAlertLevel` + utilise `DecisionToolsPanelComponent.TOOL_REGISTRY` pour résoudre composant + componentInputs
- `CaseDashboardComponent` template (modifié) — remplace les `<div class="dash-card">` par `<app-decision-tool-card>`
- `CaseDashboardComponent` SCSS (allégé) — retire `.dash-card`, `.card-alert`, `.card-ok`, `.card-label`, `.card-value`, `.card-sub`, `.progress-mini*` (porté par card SF-177-01)
- `CaseFileDetailComponent` template (modifié) — passe les 4 nouveaux inputs au `<app-case-dashboard>`

---

## Plan de test

### Tests unitaires (Jest)

#### `case-dashboard.component.spec.ts`

- [ ] **SF-177-09 T-01** : `tilesFromDashboard` produit 9 entrées + riskScore quand DashboardResponse a tous les champs peuplés (10 tiles total, riskScore en tête).
- [ ] **SF-177-09 T-02** : `tilesFromDashboard` filtre les champs null (ex. workRight=null → tile workRight absente).
- [ ] **SF-177-09 T-03** : `tilesFromDashboard` calcule `DecisionToolSummary.alertLevel = 'ALERT'` quand `licenciement.verdict === 'INVALIDE'`.
- [ ] **SF-177-09 T-04** : `openTile(tile)` appelle `DecisionToolModalService.open()` avec `inputs.forceExpanded === true` et le bon component (verifier sur 1 tile, ex. licenciement → LicenciementSectionComponent).
- [ ] **SF-177-09 T-05** : tile riskScore a `disabled === true` et n'a pas de component résolu — clic ne déclenche pas le modal.
- [ ] **Test existant adapté** : test "rendu dashboard" cherche `<app-decision-tool-card>` au lieu de `.dash-card`.

#### `case-file-detail.component.spec.ts`

- [ ] **SF-177-09 T-06** : la balise `<app-case-dashboard>` reçoit bien `[synthesis]`, `[workspaceCountry]`, `[procedureChecks]`, `[aiQuestions]` (vérifier via input bindings).

### Tests d'intégration

Non applicable.

### Isolation workspace

Non applicable (rendu pur — l'isolation est au niveau de l'endpoint dashboard déjà testé).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing frontend — non
- [x] **Aucune préoccupation transversale** — bascule UI interne à un composant existant

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `CaseDashboardService` | Aucun (utilisé tel quel) | Tests existants verts |
| `CaseDashboardRefreshService` | Aucun (refresh logic préservée) | Test existant SF-IA-04-04 reste vert |
| `case-file-detail` | Ajout 4 inputs sur la balise dashboard | Tests case-file-detail existants restent verts |

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — UI interne à la page dossier

---

## Impact par domaine métier

Cette SF est **transversale par construction**. Elle préserve la couverture actuelle :

- Travail (4 tiles : licenciement, indemnites, anciennete + … non, anciennete couvre) — oui : licenciement / indemnites / anciennete
- Immigration (3 tiles : titleDecision, workRight, recours)
- Famille (3 tiles : partage, garde, divorce)
- Risque global (1 tile riskScore)

Aucune asymétrie introduite : on refactorise l'existant, on n'ajoute pas de tile par domaine.

---

## Dépendances

### Subfeatures bloquantes

- [x] **SF-177-01** — `<app-decision-tool-card>` — done
- [x] **SF-177-02** — `DecisionToolModalService` — done
- [x] **SF-177-03/03b/05/07** — instrumentation pattern B — done (les 9 composants outils consommés ont leurs statics + forceExpanded)
- [x] **SF-177-11** — bascule panel — done (pattern d'utilisation `openTool` à reproduire)

### Subfeatures débloquées

- F-177 sera **Terminée** au merge de SF-177-09 (10/10 SF mergées)
- F-167 sera **Terminée** (absorbée intégralement par SF-177-09)

### Questions ouvertes impactées

- [x] Aucune

---

## Notes et décisions

- **Pourquoi ne pas étendre à 70 outils ?** Le découpage F-167 prévoyait un DTO backend `DashboardTile` générique pour homogénéiser ~78 verdicts. Coût ~3 h backend + tests. Bénéfice incertain : la majorité des outils ont peu de "verdict" sans saisie avocat (le panel décisionnel reste l'usage primaire). Décision : valider la cohérence visuelle sur 9 tiles, puis itérer si la demande terrain émerge.
- **Pourquoi garder le riskScore non cliquable ?** C'est une métrique agrégée, pas un outil avec page propre. Cliquer pour "ouvrir le riskScore" n'a pas de sens. On le rend non-cliquable via `[disabled]="true"` du card — visuel cohérent, comportement adapté.
- **Pourquoi passer `synthesis`/`procedureChecks`/etc. au dashboard ?** Pour réutiliser `TOOL_REGISTRY[id].inputs(ctx)` directement et passer les bons inputs aux composants outils dans le modal. Sans ça, les composants ouverts dans le modal n'auraient pas leur `aiData`/`procedureChecks` et perdraient le pré-fill IA + alertes F-IA-03.
- **Pourquoi `openTile` symétrique de `openTool` ?** Cohérence : un clic dashboard et un clic panel doivent ouvrir le modal exactement de la même façon. Le user n'a pas à deviner ce qu'il va se passer selon où il clique.
- **Pas de DTO `DashboardTile` exposé** : le type est local au composant (`case-dashboard.component.ts`). Si plus tard F-178 introduit un DTO backend partagé, il pourra remplacer ce type local sans casser l'API publique du composant.
