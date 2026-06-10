# Mini-spec — F-262 / SF-262-01 — Référentiel des chefs de demande + détection (travail FR)

> Programme Conclusions V2 / F-262 (filet de complétude). Décision PO Option A (catalogue). Backend. Pré-requis de SF-262-02 (encart front).

## Identifiant
`F-262 / SF-262-01`

## Statut
`ready` (étape 0 GO + décision PO Option A)

## Branche
`feat/SF-262-01-referentiel-chefs-detection`

## Objectif
> Construire un **référentiel des chefs de demande** (vague travail FR) et un service de **détection des chefs applicables** à un dossier, exposé par un endpoint — base du filet « chefs applicables non plaidés ».

## Comportement attendu

### Cas nominal
1. `GET /api/v1/case-files/{caseFileId}/heads-of-claim` retourne les chefs de demande **applicables** au dossier, chacun : `{ code, label, fondement, category, tooled (toolId|null), addressed (bool) }`.
2. **Applicabilité** (anti-gadget — base réelle uniquement) :
   - Chef **outillé** (mappé à un outil F-DT) → applicable si l'outil est **visible/pertinent** pour le dossier (`DecisionToolVisibilityService` : alwaysOn ∪ contextual). `addressed = true` si l'outil est **calculé** (présent dans les `DashboardTile`), sinon `false` (réutilise la logique F-258, sans la ré-afficher).
   - Chef **transverse** (art. 700 CPC, dépens, intérêts au taux légal, capitalisation art. 1343-2, exécution provisoire) → applicable **toujours** au stade contentieux (jugement/fond, appel) ; `addressed` non déterminable (filet = rappel) → `addressed=false` (rappel de présence dans le dispositif).
3. Domaine ≠ travail FR → catalogue vide (vagues immigration/famille suivantes).

### Cas d'erreur
| Situation | Comportement | HTTP |
|---|---|---|
| Dossier d'un autre workspace | accès refusé | 404 |
| Domaine non couvert | liste vide (no-op) | 200 |

## Analyse de cohérence transversale
- **vs F-258** : F-262 **ne ré-affiche pas** « outils non calculés ». Il expose les **chefs de demande** (incluant les **transverses non outillés**). Pour les chefs outillés, il **réutilise** la même source d'applicabilité/calcul (pas de logique dupliquée divergente).
- **vs SF-98-55** : les chefs transverses (art. 700, intérêts, capitalisation) sont déjà **imposés** par la garde du dispositif → le filet les *signale* en cohérence, ne contredit pas.
- **Domaines** : framework uniforme ; **catalogue travail FR** cette SF ; immigration/famille = vagues.

### Résultat du scan
| Cible | Applicable | Traitement |
|---|---|---|
| `DecisionToolVisibilityService` (applicabilité outils) | Oui | Réutilisé (chefs outillés) |
| `CaseFileDashboardService` (outils calculés) | Oui | Réutilisé (`addressed`) |
| F-258 (encart outils) | Non (complémentaire) | Pas de re-affichage ; SF-262-02 = encart distinct « chefs » |
| Catalogue immigration/famille | Oui (futur) | Vagues |

### Décision
- [x] Framework + vague travail FR ; immigration/famille = vagues backlog.
- [x] Réutilise l'applicabilité/calcul des outils (pas de doublon divergent de F-258).

## Conformité F-IA-04
- [x] **Non applicable** — pas de composant décisionnel `TOOL_REGISTRY` ; service de détection + endpoint de lecture.

## Champs IA à extraire
- [x] **Aucun pré-remplissage** — la détection dérive de l'applicabilité des outils + règles de stade, pas d'une extraction IA.

## Critères d'acceptation
- [ ] Catalogue travail FR : chefs **outillés** (mappés aux F-DT pertinents) + chefs **transverses** (art. 700, dépens, intérêts, capitalisation, exéc. provisoire) avec `code/label/fondement/category`.
- [ ] `GET …/heads-of-claim` : chef outillé applicable ssi l'outil est visible ; `addressed=true` ssi calculé.
- [ ] Chefs transverses applicables au stade contentieux ; `addressed=false` (rappel).
- [ ] Domaine ≠ travail FR → liste vide.
- [ ] Isolation workspace (404 cross-workspace).
- [ ] **Anti-gadget** : aucun chef applicable sans base réelle (visibilité outil ou règle de stade).

## Périmètre
### Hors scope
- **Chefs métier fact-dépendants non outillés** (dommages-intérêts préjudice moral/vexatoire, rappels conditionnels) — raffinement ultérieur (applicabilité plus fine).
- **Vagues immigration / famille** (catalogues dédiés).
- **Encart frontend** (SF-262-02).

## Technique
### Endpoint
| Méthode | URL | Auth | Rôle |
|---|---|---|---|
| GET | `/api/v1/case-files/{caseFileId}/heads-of-claim` | Oui | MEMBER |

Réponse : `{ "heads": [{ "code", "label", "fondement", "category" (INDEMNITAIRE_OUTILLE / TRANSVERSE), "toolId" (nullable), "applicable" (true), "addressed" (bool) }] }` (on ne renvoie que les applicables ; `addressed` indique s'il est couvert).

### Backend
- Catalogue **code-based** (pas de table/migration) : `HeadOfClaimCatalog` — registre des `HeadOfClaim(code, label, fondement, category, domain, country, toolId|null, stageScope)` ; vague travail FR remplie.
- `HeadsOfClaimService.detect(caseFileId, …)` : résout outils visibles (`DecisionToolVisibilityService`) + calculés (`CaseFileDashboardService`) ; pour chaque chef du catalogue (domaine/pays du workspace) : applicable (outillé→visible ; transverse→stade contentieux) ; `addressed` (outillé→calculé ; transverse→false). Isolation workspace (pattern existant).
- `HeadsOfClaimController` : GET endpoint, délègue.

### Tables / migration
- [x] **Aucune** (catalogue code-based).

## Plan de test
### Unitaires
- [ ] `HeadsOfClaimService` : chef outillé visible → applicable ; calculé → addressed ; non calculé → non addressed ; transverse → applicable + addressed=false ; domaine ≠ travail FR → vide ; isolation 404.
### Intégration
- [ ] `GET …/heads-of-claim` 200 (structure) ; 404 cross-workspace.
### Isolation workspace
- [x] Applicable.

## Analyse d'impact
- [x] **Aucune préoccupation transversale** (lecture seule ; réutilise visibility/dashboard ; pas d'auth/plan/navigation nouveau).
### Smoke E2E
- [ ] Aucun.

## Dépendances
- F-258 / F-IA-04 (`DecisionToolVisibilityService`, `CaseFileDashboardService`) — `done` (réutilisés).
- **Bloque** SF-262-02 (encart front).

## Notes
- **Code-based catalog** (pas DB) : MVP versionnable, pas de migration ; passage en `legal_referentials` si besoin de maintenance opérateur = backlog.
- `addressed` pour les transverses = `false` par convention (filet = rappel ; on ne parse pas l'acte). Affinage possible (parser le dispositif) = backlog.
