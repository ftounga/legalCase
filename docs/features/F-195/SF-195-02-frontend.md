# Mini-spec — F-195 / SF-195-02 Frontend — UI tags risques + tile dashboard nuancée + propagation outils

## Identifiant

`F-195 / SF-195-02`

## Statut

`draft` — 2026-05-06

## Branche Git

`feat/SF-195-02-frontend-risques-markables`

## Pattern de référence

**SF-194-02-frontend.md** — pattern strictement aligné. F-195 réplique sur le bloc `risques`.

## Contrat API importé de SF-195-01-backend

- `PUT /api/v1/case-files/{id}/risques/status` body `{ risqueLibelleOriginal, statut: 'A_CREUSER' | 'VALIDE' | 'ECARTE', raisonEcarte? }`
- `GET /api/v1/case-files/{id}/risques-alignment` → `RisqueAlignment[]`
- `RisqueAlignment { libelle, statut, toolIdsCibles[], raisonEcarte? }`
- Tile `{ toolId: 'F-195-risques-summary', theme: 'DIAGNOSTIC', label, primaryValue, secondaryValue?, alertLevel? }`
- Tile `riskScore` F-IA-02 étendue avec `scoreAvocat?: number` (si statuts présents)

---

## Objectif

Côté frontend : (1) UI markable bloc Risques dans `SynthesisComponent` (3 boutons par risque + champ raison_ecarte) ; (2) tile dashboard `F-195-risques-summary` thème DIAGNOSTIC ; (3) tile `riskScore` F-IA-02 nuancée avec `Score IA brut : X · Score validé : Y` ; (4) flag visuel sur card panel F-IA-04 quand outil pré-flaggé par risque VALIDÉ.

---

## Comportement attendu

### Cas nominal

1. **UI markable dans `SynthesisComponent`** bloc Risques :
   - Chaque risque affiche 3 boutons : 🔍 `À_CREUSER` (default, navy/or) / ✅ `VALIDÉ` (vert/or) / ❌ `ÉCARTÉ` (gris discret, cohérent F-176)
   - Clic ÉCARTÉ → champ texte `raisonEcarte` (optionnel)
   - Optimistic update : UI change immédiat, rollback si erreur
   - **Cohérence F-176 stricte** : aucun `triggerRefresh()` au PUT
2. **Lecture alignement** : `RisqueAlignmentService.getForCaseFile(id)` au montage, signal cache
3. **Tile `F-195-risques-summary`** : `<app-dashboard-tile>` standard, clic → scroll vers bloc Risques de la synthèse
4. **Extension tile `riskScore` F-IA-02** : si `scoreAvocat` présent dans la response, afficher 2 lignes `Score IA brut : X / 100` + `Score validé avocat : Y / 100` (visuellement distinguées). Sinon comportement actuel (1 score).
5. **Card panel F-IA-04** : pour chaque outil dont l'ID apparaît dans `risquesAlignment[].toolIdsCibles` quand statut = VALIDÉ → badge `🚨 Risque validé` (rouge subtil) sur la card, taille identique aux autres pills (`auto_awesome`, `🎯`, `🔍`)
6. **Cycle rafraîchissement** : refresh `risquesAlignment` uniquement au SSE `ENRICHED_ANALYSIS DONE`

### Cas d'erreur

| Situation | Comportement |
|-----------|-------------|
| PUT 400 (statut invalide / raison fournie hors ECARTE) | Snackbar erreur, rollback optimistic |
| PUT 5xx ou timeout | Snackbar erreur, rollback |
| GET timeout | Fail-open silencieux, alignement = [], tile absente |

---

## Critères d'acceptation

- [ ] **CA-01 UI markable** : 3 boutons rendus par risque, statut persisté visible
- [ ] **CA-02 ÉCARTÉ raison** : clic ÉCARTÉ → champ raison se déplie
- [ ] **CA-03 PUT sans refresh** : aucun appel `triggerRefresh()` après PUT
- [ ] **CA-04 lecture alignement** : signal exposé au montage
- [ ] **CA-05 tile risques-summary** : thème DIAGNOSTIC, alertLevel correct selon mix
- [ ] **CA-06 riskScore nuancée** : tile affiche 2 scores quand `scoreAvocat` présent
- [ ] **CA-07 badge card outil pré-flaggé** : risque VALIDÉ "harcèlement" → badge `🚨 Risque validé` sur F-DT-12 card
- [ ] **CA-08 fail-open** : timeout → blocs vides
- [ ] **CA-09 OnPush + markForCheck**
- [ ] **CA-10 visuel charte** : ÉCARTÉ gris discret, VALIDÉ vert (palette navy/or principale, rouge réservé alerte critique badge `🚨`)

---

## Hors scope V1

- (a) Édition libre du libellé risque
- (b) Drag-and-drop entre statuts
- (c) Customisation seuils alertLevel
- (d) Notification push pour risque VALIDÉ critique

---

## Composants Angular impactés

- `RisqueAlignmentService` + `RisqueStatusService` (nouveaux)
- `risque-alignment.model.ts` (nouveau)
- `<app-synthesis>` extension — bloc Risques refondu (3 boutons + raison field)
- `<app-decision-tool-card>` extension — badge `🚨 Risque validé` (nouveau pill rouge subtil)
- Extension tile `riskScore` (composant existant `<app-dashboard-tile>` ou code F-IA-02 spécifique — à investiguer)
- `<app-decisional-tools-panel>` `TOOL_REGISTRY` étendu pour propager `risquesValidesToolFlag` aux cards
- `<app-case-dashboard>` mapping toolId `F-195-risques-summary`

---

## Tests Jest (~12)

- `RisqueStatusServiceTest` (3 : success / 400 / 500)
- `RisqueAlignmentServiceTest` (3)
- `SynthesisComponentTest` extension (4 : 3 boutons rendu, clic VALIDÉ → PUT, ÉCARTÉ → raison field, refresh SSE only)
- `DecisionToolCardComponentTest` (1 : badge 🚨 rendu si flag)
- `CaseDashboardComponentTest` (1 : tile F-195 + scoreAvocat)

---

## Dépendances

- F-192 SF-192-02 ✅
- F-193 SF-193-02, F-194 SF-194-02 (en cours)
- F-IA-02 ✅
- **SF-195-01 backend** — contrat figé

---

## Notes 2026-05-06

- Pattern strict F-176 trichotomie + F-176 design (gris discret pour ÉCARTÉ)
- Badge `🚨 Risque validé` est le **seul** usage de pill rouge plein dans F-19X — justifié car risque critique validé est l'info la plus saillante. Toujours subtil (rouge fond + texte blanc, taille pill standard, pas de bordure agressive)
- Optimistic update au PUT (UX fluide), rollback strict si erreur
