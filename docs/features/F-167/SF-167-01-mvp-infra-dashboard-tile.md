# Mini-spec — F-167 / SF-167-01 MVP infrastructure DashboardTile générique + 10 outils pilotes

> **Engagement de scope intégral** (mémoire `feedback_pas_de_reduction_scope_silencieuse`)
> Cette SF est la première de **5 SFs qui couvrent F-167 intégralement** :
> - SF-167-01 (cette SF) : infra DashboardTile + frontend tile + **10 outils pilotes mix domaines**
> - SF-167-02 : étendre aux 25 outils Travail FR+BE restants
> - SF-167-03 : étendre aux ~24 outils Famille FR+BE restants
> - SF-167-04 : étendre aux ~13 outils Immigration FR+BE restants
> - SF-167-05 : polish (groupement par thème, tri alertLevel, état vide)
>
> **Aucune des 5 SFs ne sera reportée**. Pas de "MVP avec extension si besoin émerge".
> Cas réel 2026-05-02 : F-IM-11 Changement de statut absent du dashboard sur dossier
> "Immigration Chen 5" malgré F-167 existante depuis 2026-04-27.

---

## Identifiant

`F-167 / SF-167-01`

## Feature parente

`F-167` — Compléter le dashboard agrégé `<app-case-dashboard>` avec tous les outils décisionnels calculés

## Statut

`draft`

## Date de création

2026-05-02

## Branche Git

`feat/SF-167-01-mvp-infra-dashboard-tile`

---

## Objectif

Poser l'infrastructure générique `DashboardTile` (backend record + frontend composant) qui permettra à toutes les `*Analysis.java` persistées d'apparaître dans le dashboard agrégé, et l'instancier sur **10 outils pilotes mix domaines** dont F-IM-11 Changement de statut (cas réel "Immigration Chen 5").

---

## Comportement attendu

### Cas nominal

1. Backend : nouveau record `DashboardTile` dans `fr.ailegalcase.casefile` :
   ```java
   public record DashboardTile(
       String toolId,           // F-DT-08-licenciement-validity
       String theme,            // VALIDITE | INDEMNITES | DELAIS | DOCUMENTS | DIAGNOSTIC
       String label,            // libellé court (ex. "Validité licenciement")
       String primaryValue,     // ex. "INVALIDE", "Score 70/100"
       String secondaryValue,   // optionnel, ex. "3/12 critères non conformes"
       String alertLevel        // OK | WARNING | ALERT | null
   ) {}
   ```
2. `CaseFileDashboardResponse` étendu avec un nouveau champ `List<DashboardTile> tiles` (rétro-compatible — les 9 champs typés actuels restent).
3. `CaseFileDashboardService` étendu avec une méthode `assembleTiles(caseFileId, workspaceId)` qui :
   - Charge chaque `*Analysis` du dossier (pour les 10 outils pilotes).
   - Mappe chaque `*Analysis` non null vers une `DashboardTile`.
   - Retourne la liste (ordre stable par `toolId`).
4. La méthode existante `getDashboard(caseFileId)` appelle `assembleTiles` et l'inclut dans la réponse.
5. Frontend : nouveau composant standalone `<app-dashboard-tile>` qui :
   - Reçoit `@Input() tile: DashboardTile` + `@Input() onClick?: () => void`.
   - Rend exactement la même mécanique visuelle que `<app-decision-tool-card>` (palette, layout, badges) — réutilise le composant si possible, sinon copie le pattern.
   - Au clic, ouvre le modal `DecisionToolModalService` correspondant au `toolId` (cohérent avec SF-177-09).
6. `case-dashboard.component` :
   - Lit `dashboard.tiles` et l'affiche en grid 3 colonnes responsive en haut de page dossier.
   - Coexiste avec les 9 tiles typées actuelles (les 10 nouvelles **complètent**, ne remplacent pas — pour ne pas perdre la backward compat avant SF-167-05 qui les fusionnera).

### Outils pilotes (10 mix domaines)

| toolId | Domaine | Pays | Source `*Analysis` |
|---|---|---|---|
| `F-DT-08-licenciement-validity` | Travail | FR | `LicenciementAnalysis` (déjà existant — peut servir de double tile pour valider la migration) |
| `F-DT-09-comparateur-indemnites` | Travail | FR | (déjà tile — valider la cohérence) |
| `F-DT-07-anciennete-conges-prime` | Travail | FR | `AncienneteAnalysis` (déjà tile) |
| `F-IM-05-arbre-decisionnel-titre` | Immigration | FR | (déjà tile, via `titleDecision`) |
| `F-IM-07-droit-au-travail` | Immigration | FR | (déjà tile, via `workRight`) |
| `F-IM-06-recours` | Immigration | FR | (déjà tile, via `recours`) |
| **`F-IM-11-changement-statut`** | Immigration | FR | `ChangementStatutAnalysis` — **NOUVEAU** (cas Chen 5) |
| `F-FA-05-partage-immobilier` | Famille | FR | (déjà tile, via `partage`) |
| `F-FA-06-calendrier-garde` | Famille | FR | (déjà tile, via `garde`) |
| `F-FA-07-checklist-divorce` | Famille | FR | (déjà tile, via `divorce`) |

**Important** : 9 des 10 outils pilotes sont **déjà** dans le dashboard (via les 9 records typés). Cette SF les fait passer dans la nouvelle structure générique pour valider que la migration n'introduit pas de régression visible. F-IM-11 est le **seul outil net nouveau** dans cette SF — sera visible immédiatement post-merge sur Chen 5.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|---|---|---|
| Aucune analyse persistée pour le dossier | `tiles = []` (liste vide, pas de null) | 200 |
| Une `*Analysis` repository échoue | Cette tile est absente, les autres sont retournées (fail-open par tile) | 200 |
| Workspace différent | L'endpoint `getDashboard` retourne déjà 403 — comportement préservé | 403 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] **Composant `<app-decision-tool-card>` (SF-177-01)** : déjà utilisé par les 9 tiles existantes (SF-177-09). Décision : `<app-dashboard-tile>` est **un alias visuel** qui consomme `DashboardTile` et délègue à `<app-decision-tool-card>` pour le rendu. Pas de duplication visuelle.
- [x] **Modal `DecisionToolModalService` (SF-177-02)** : déjà ouvert par les cards du panel et du dashboard existant. Réutilisé tel quel — la nouvelle tile passe `toolId` au modal.
- [x] **TOOL_REGISTRY** (frontend `decisional-tools-panel.component.ts`) : contient déjà 92 entrées dont F-IM-11. Le modal résout le composant outil depuis `toolId` — pas de modification nécessaire.
- [x] **Mapping thème** : `THEME_BY_TOOL_ID` existe déjà côté frontend (introduit par F-169). À réutiliser côté backend pour peupler le champ `theme` de la `DashboardTile`. Première option : extension d'un référentiel partagé. Seconde option (retenue pour rester minimal) : mapping en dur côté backend dans `CaseFileDashboardService` pour les 10 outils pilotes — étendu en SF-167-02/03/04. Convergence finale en SF-167-05 si nécessaire.
- [x] **Pattern miroir `getPrefillCount` (SF-177-12)** : la tile peut afficher un compteur de pré-fill IA. Hors scope cette SF (la SF-177-12 vient de livrer le pattern uniquement sur 4 composants Immigration FR — extension dashboard suit en SF-177-13+ ou SF-167-05).
- [x] **Belgique** : aucun outil BE dans les 10 pilotes (volontaire — utilisateur teste FR sur Chen 5). SF-167-02/03/04 incluent les outils BE de chaque domaine.

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Le pattern `<app-dashboard-tile>` peut-il être réutilisé ?** : oui, c'est explicitement son rôle (consommé par dashboard puis SF-167-02/03/04 par extension de backend mappers). Pas de duplication, pas de pattern concurrent.
- [x] **Patterns concurrents existants** : les 9 records typés (`LicenciementSummary`, `IndemniteSummary`, etc.) côté backend. Décision : les conserver pendant la transition (SF-167-01 à 04) pour ne pas casser le frontend existant qui les lit. SF-167-05 fusionne et supprime les records typés (refactoring final).

### Décision

- [x] Étendu aux 10 outils pilotes dans cette SF.
- [x] **SF parallèles à venir explicitement listées** : SF-167-02 Travail (25 outils), SF-167-03 Famille (~24 outils), SF-167-04 Immigration (~13 outils), SF-167-05 Polish (groupement thème + tri + état vide + suppression records typés legacy).
- [x] **Aucune réduction de scope silencieuse**. Si l'effort réel d'une SF dépasse 1 jour de dev, redécouper plutôt qu'abandonner.

---

## Critères d'acceptation

- [ ] **Backend record `DashboardTile`** créé dans `fr.ailegalcase.casefile` avec les 6 champs documentés.
- [ ] **`CaseFileDashboardResponse`** étendu avec `List<DashboardTile> tiles` rétro-compatible (les 9 records typés existants restent inchangés).
- [ ] **`CaseFileDashboardService.assembleTiles(caseFileId, workspaceId)`** retourne la liste pour les 10 outils pilotes — chaque tile contient un `primaryValue` lisible (ex. pour Changement de statut : `"ETUDIANT → VPF (ELEVEE)"`).
- [ ] **Endpoint `GET /api/v1/case-files/{id}/dashboard`** retourne le champ `tiles` peuplé (vérifier dans IT existant + 1 nouveau IT spécifique F-IM-11).
- [ ] **Frontend composant `<app-dashboard-tile>`** standalone, rendu identique à `<app-decision-tool-card>` (réutilise le composant), clic ouvre le modal du toolId.
- [ ] **`case-dashboard.component.html`** affiche un grid des nouvelles `tiles` en plus du grid des 9 tiles typées existantes (séparé temporairement, fusion en SF-167-05).
- [ ] **Test d'intégration** : créer un dossier avec une `ChangementStatutAnalysis` persistée → endpoint dashboard renvoie une tile `F-IM-11-changement-statut` avec verdict.
- [ ] **Test Jest** : `<app-dashboard-tile>` rend correctement, click déclenche `openTool(toolId)`.
- [ ] **Test Jest** : `case-dashboard.component` affiche les nouvelles tiles quand `dashboard.tiles` est non vide.
- [ ] **Aucune régression** : les 9 tiles typées existantes continuent de s'afficher.
- [ ] **Test manuel post-merge** : sur "Immigration Chen 5" (staging), une nouvelle tile "Changement de statut" apparaît dans le dashboard agrégé en haut de page, avec le verdict ELEVEE/MOYENNE/FAIBLE et le clic ouvre le modal.

---

## Périmètre

### Hors scope

- Pas d'extension aux outils Travail/Famille/Immigration au-delà des 10 pilotes (couvert par SF-167-02/03/04).
- Pas de groupement par thème ni de tri par alertLevel (couvert par SF-167-05).
- Pas de suppression des 9 records typés `LicenciementSummary` et al. (refactoring final en SF-167-05 quand les nouvelles tiles couvrent tout).
- Pas d'extension du pattern `getPrefillCount` au dashboard (suit en SF-177-13+ ou SF-167-05 selon priorité).

---

## Contraintes de validation

| Champ | Obligatoire | Format | Notes |
|---|---|---|---|
| `toolId` | Oui | string | Doit exister dans TOOL_REGISTRY frontend (sinon le clic crash le modal) |
| `theme` | Oui | enum string | INDEMNITES \| VALIDITE \| DELAIS \| DOCUMENTS \| DIAGNOSTIC |
| `label` | Oui | string ≤ 60 chars | libellé court UI |
| `primaryValue` | Oui | string ≤ 80 chars | verdict humain lisible |
| `secondaryValue` | Non | string ≤ 100 chars | détail subordonné |
| `alertLevel` | Non | enum nullable | OK \| WARNING \| ALERT |

---

## Technique

### Endpoints

| Méthode | URL | Auth | Rôle minimum |
|---|---|---|---|
| GET | `/api/v1/case-files/{id}/dashboard` | Oui | MEMBER |

(Endpoint existant — extension du DTO de réponse uniquement, pas de nouveau verbe.)

### Tables impactées

Aucune (lecture des `*_analyses` existantes).

### Migration Liquibase

- [ ] Non applicable.

### Composants / classes

**Backend**
- `backend/src/main/java/fr/ailegalcase/casefile/DashboardTile.java` — NOUVEAU record.
- `backend/src/main/java/fr/ailegalcase/casefile/CaseFileDashboardResponse.java` — ajout champ `List<DashboardTile> tiles`.
- `backend/src/main/java/fr/ailegalcase/casefile/CaseFileDashboardService.java` — méthode `assembleTiles` + helpers de mapping par outil pilote (10 mappers privés).
- `backend/src/test/java/fr/ailegalcase/casefile/CaseFileDashboardServiceTest.java` — UT.
- `backend/src/test/java/fr/ailegalcase/casefile/CaseFileDashboardControllerIT.java` — IT spécifique F-IM-11.

**Frontend**
- `frontend/src/app/case-files/case-dashboard/dashboard-tile/dashboard-tile.component.ts` — NOUVEAU composant.
- `frontend/src/app/case-files/case-dashboard/dashboard-tile/dashboard-tile.component.html` — délégation à `<app-decision-tool-card>`.
- `frontend/src/app/case-files/case-dashboard/dashboard-tile/dashboard-tile.component.spec.ts` — Tests Jest.
- `frontend/src/app/case-files/case-dashboard/case-dashboard.component.ts` — consomme `dashboard.tiles`.
- `frontend/src/app/case-files/case-dashboard/case-dashboard.component.html` — rend les nouvelles tiles.
- `frontend/src/app/core/models/case-dashboard.model.ts` — ajoute `DashboardTile` interface + champ `tiles?: DashboardTile[]` dans `DashboardResponse`.

---

## Plan de test

### Tests unitaires (JUnit)

- [ ] `CaseFileDashboardServiceTest` : `assembleTiles` retourne la tile F-IM-11 avec `primaryValue` correct quand `ChangementStatutAnalysis` est persistée.
- [ ] `assembleTiles` retourne liste vide si aucune analyse persistée.
- [ ] `assembleTiles` ignore les `*Analysis` corrompues (fail-open par tile).
- [ ] Les 10 mappers individuels produisent un `primaryValue` non vide pour leur outil.

### Tests d'intégration

- [ ] `CaseFileDashboardControllerIT` : `GET /api/v1/case-files/{id}/dashboard` après création d'une `ChangementStatutAnalysis` retourne `tiles` contenant `F-IM-11-changement-statut`.
- [ ] `GET /api/v1/case-files/{id}/dashboard` reste 200 sur dossier sans analyse (`tiles: []`).

### Tests Jest

- [ ] `<app-dashboard-tile>` rend les champs primary/secondary/alertLevel.
- [ ] Click sur la tile appelle `DecisionToolModalService.open(toolId)`.
- [ ] `case-dashboard.component` affiche les nouvelles tiles en plus des 9 typées.

### Isolation workspace

- [x] Couverte par les IT existants (le contrôleur applique le filtre `workspace_id` via le service standard).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension du DTO d'un endpoint existant + nouveau composant frontend visuellement aligné sur le pattern card existant.

### Composants impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|---|---|---|
| `case-dashboard.component` | Nouveau bloc `tiles` rendu en plus des 9 tiles typées | Tests Jest existants restent verts ; tests visuels confirmés post-merge |
| `CaseFileDashboardResponse` | Ajout d'un champ optionnel | Sérialisation Jackson rétro-compatible (champ `null` ignoré côté ancien client) |
| `CaseFileDashboardService.getDashboard` | Délégué à `assembleTiles` | IT existant doit rester vert |

### Smoke tests E2E

- [x] Aucun smoke E2E concerné — le dashboard est sur la page dossier, pas dans un flow critique d'auth/navigation.

---

## Impact par domaine métier

- **Travail** : 3 outils pilotes (F-DT-07/08/09) — déjà tile, valide la migration.
- **Immigration** : 4 outils pilotes (F-IM-05/06/07/11) — F-IM-11 nouveau (cible Chen 5).
- **Famille** : 3 outils pilotes (F-FA-05/06/07) — déjà tile, valide la migration.
- **France / Belgique** : pilotes FR uniquement. BE inclus dans SF-167-02/03/04.

---

## Parité des domaines métier

L'outil dashboard est de **niveau infrastructure transverse** (pas un outil décisionnel métier). Parité : SF-167-02/03/04 incluent explicitement les outils BE de chaque domaine. SF-167-04 Immigration inclut F-IM-08-annexe13-be, F-IM-14-9bis-humanitaire-be, etc.

---

## Dépendances

### Subfeatures bloquantes

- `SF-177-01` — `done` (composant `<app-decision-tool-card>` réutilisé).
- `SF-177-02` — `done` (modal `DecisionToolModalService`).
- `SF-IA-02-02` — `done` (endpoint dashboard initial).

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Pourquoi ne pas supprimer immédiatement les 9 records typés ?** : pour permettre à SF-167-01 de livrer SANS casser le rendu actuel — le frontend existant lit encore les 9 records. Le refactoring (suppression + fusion) est explicitement listé comme critère d'acceptation de SF-167-05.
- **Pourquoi un mapping toolId→theme en dur côté backend ?** : `THEME_BY_TOOL_ID` existe déjà côté frontend (F-169) mais n'est pas exposé via l'API. Pour rester minimal, dupliquer côté backend pour les 10 pilotes ; SF-167-05 évalue une convergence (référentiel partagé ou propagation depuis frontend).
- **Pourquoi `alertLevel` optionnel ?** : pour les outils sans verdict tranché (générateurs de document, checklists pures), la tile reste neutre (pas de badge couleur).
- **Pourquoi inclure F-IM-11 dans les pilotes alors qu'il n'a pas de tile aujourd'hui ?** : c'est précisément le cas réel "Immigration Chen 5" qui a déclenché la résurrection de F-167. Le valider dans SF-167-01 démontre que la nouvelle infra fonctionne end-to-end avant l'extension par domaine.
