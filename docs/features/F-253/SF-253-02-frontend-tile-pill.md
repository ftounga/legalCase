# SF-253-02 — Frontend : routing tile dashboard + pill secondaire « À creuser » sur cards outils

## Objectif

Côté frontend, exposer le statut `À_CREUSER` des risques (F-195) à 2 endroits hors écran synthèse :
1. La nouvelle tile dashboard `F-253-risques-a-creuser` (livrée SF-253-01 backend) est cliquable et route vers `/synthesis/risques` via `BadgeNavigationService` — même cible canonique que F-195.
2. Une **pill secondaire** `🔍 N à creuser` apparaît sur chaque `<app-decision-tool-card>` quand au moins un risque mappé à l'outil est encore au statut implicite `A_CREUSER`. Palette gris navy subtil (DESIGN_SYSTEM.md), cohabite avec le pill F-195 `⚠️ Risques (V/E)` sans le remplacer.

## Comportement nominal

### Tile dashboard cliquable

- `case-dashboard.component.openGenericTool(toolId)` reconnaît `'F-253-risques-a-creuser'` et appelle `badgeNavigation.go('risques', caseFileId)` — l'utilisateur arrive sur `/case-files/:id/synthesis/risques` (cohérent avec le clic sur la tile F-195-risques-summary).

### Pill secondaire « N à creuser »

- Apparaît si `risquesACreuserCount > 0` pour l'outil.
- Format : `🔍 N à creuser` (picto `search`, label avec pluralisation implicite via "à creuser").
- Tooltip : `N risque(s) à creuser — arbitrage avocat en attente`.
- Aria-label : `N risque(s) à creuser pour cet outil`.
- Position dans la card : juste après le pill `⚠️ Risques (V/E)` existant.
- Palette : fond `rgba(26, 58, 92, 0.06)`, texte `#4A5C72`, border `rgba(26, 58, 92, 0.30)` — gris navy subtil cohérent DESIGN_SYSTEM.md.

### Helper côté panel

- Nouvelle fonction `getRisquesACreuserCountFor(alignment, toolId): number` dans `risque-badge.helper.ts`. Filtre par toolId et compte les statuts `A_CREUSER`. Retourne 0 si pas d'alignement.
- Nouvelle méthode `risquesACreuserCountFor(toolId): number` dans `decisional-tools-panel.component.ts` qui délègue au helper en passant le signal `risquesAlignment()`.

## Cas d'erreur

| Cas | Comportement |
|---|---|
| `risquesAlignment` vide / null | Le compteur = 0 → pill masquée silencieusement (`showRisquesACreuserPill` = false). |
| `risquesACreuserCount` = `null` côté input card | Pill masquée (composant card non instrumenté, forward-compat). |
| Tile F-253 sans handler `openGenericTool` | Console.warn générique du switch — mais le handler EST ajouté donc impossible en pratique. |

## Critères d'acceptation

- **CA-01** : clic sur la tile `F-253-risques-a-creuser` du dashboard route l'avocat vers `/case-files/:id/synthesis/risques`.
- **CA-02** : pill `🔍 N à creuser` visible sur une card outil si ≥ 1 risque mappé à l'outil est `A_CREUSER`.
- **CA-03** : pill masquée si compteur À_C = 0 pour l'outil (même si la card a d'autres risques V/É).
- **CA-04** : la pill F-195 existante (`⚠️ Risques (V/E)`) reste inchangée — aucune régression sur ses 5 kinds (`validated_critical` / `validated` / `mixed` / `discarded` / `to_explore`).
- **CA-05** : pluralisation : `1 à creuser` reste valide (le mot « creuser » ne se pluralise pas) ; tooltip / aria adaptent `risque` ↔ `risques` selon N.
- **CA-06** : palette gris navy (`rgba(26,58,92,*)`) — pas de rouge ni d'or. Le rouge reste réservé à `validated_critical`.
- **CA-07** : pill secondaire indépendante du kind du pill F-195. Cohabite avec n'importe lequel des 5 kinds.

## Plan de test minimal

### Jest — `decision-tool-card.component.spec.ts`

1. `pill À creuser visible si count > 0` — `risquesACreuserCount = 2` → pill DOM présente, label `2 à creuser`.
2. `pill À creuser masquée si count = 0` — `risquesACreuserCount = 0` → pill DOM absente.
3. `pill À creuser masquée si count null` — `risquesACreuserCount = null` → pill DOM absente.
4. `pill À creuser cohabite avec pill F-195` — `risquesACreuserCount = 1` + `risquesBadge = { kind: 'validated', counts: {...} }` → les 2 pills DOM présentes.
5. `tooltip pluriel/singulier` — `risquesACreuserCount = 1` → tooltip `1 risque à creuser…` ; `risquesACreuserCount = 3` → tooltip `3 risques à creuser…`.

### Jest — `risque-badge.helper.spec.ts`

1. `getRisquesACreuserCountFor — alignment null → 0`.
2. `getRisquesACreuserCountFor — toolId non mappé → 0`.
3. `getRisquesACreuserCountFor — compte uniquement les A_CREUSER`.

### Jest — `case-dashboard.component.spec.ts`

1. `clic tile F-253 route vers /synthesis/risques` — mock `BadgeNavigationService`, vérifier `go('risques', ...)` appelé.

### Tests de régression

Les tests F-195 existants restent verts (pill `⚠️ Risques (V/E)` inchangée).

## Tables / endpoints / composants impactés

| Élément | Modification |
|---|---|
| `case-dashboard.component.ts` | + branch `'F-253-risques-a-creuser'` dans `openGenericTool` |
| `risque-badge.helper.ts` | + export `getRisquesACreuserCountFor` |
| `decision-tool-card.component.ts` | + `@Input() risquesACreuserCount` + 4 getters (`show*Pill`, `*PillLabel`, `*PillAriaLabel`, `*PillTooltip`) |
| `decision-tool-card.component.html` | + bloc `@if (showRisquesACreuserPill)` après pill risques |
| `decision-tool-card.component.scss` | + classe `--risques-a-creuser` (gris navy subtil) |
| `decisional-tools-panel.component.ts` | + import `getRisquesACreuserCountFor` + méthode `risquesACreuserCountFor(toolId)` |
| `decisional-tools-panel.component.html` | + binding `[risquesACreuserCount]="risquesACreuserCountFor(item.toolId)"` |

**Aucune** modification de :
- Modèles TS (RisqueAlignment record déjà OK avec `statut: 'A_CREUSER' | ...`)
- Service Angular (`RisqueAlignmentService` lecture inchangée)
- Routes (`/synthesis/risques` existe déjà — F-195)
- Tile F-195 dans `case-dashboard` (CA-04)
- Pill F-195 dans `decision-tool-card` (CA-04)

## Hors périmètre SF-253-02

- **Backend** : tile dashboard `F-253-risques-a-creuser` → livré SF-253-01.
- **Export PDF** : section « Risques à creuser » → SF-253-03.
- **Modification du label compact V/E pour devenir V/E/À** : déjà disponible via tooltip et aria-label F-195. La pill secondaire est l'option choisie (cf. mini-spec) pour respecter l'invariant n°1 de l'étape 0 bis (titres harmonisés via composants distincts).
- **Notification toaster** : non — pas de side-effect (invariant étape 0).

## Notes et décisions

- **Pourquoi pill séparée et pas modification du label F-195** : préserve les tests existants `decision-tool-card.component.spec.ts` qui assertent le format `V/E` du label F-195. Pill séparée = forward-compat propre.
- **Pourquoi gris navy subtil** : DESIGN_SYSTEM.md réserve rouge à `validated_critical`, or à `validated`. Le gris navy est cohérent avec la classe `--risques-to-explore` existante (qui est navy mais plus saturée) — F-253 est encore moins saturée pour ne pas concurrencer visuellement.
- **Pourquoi label `N à creuser` et pas juste `N`** : explicite, évite l'ambiguïté avec d'autres compteurs visibles (V/E, prefill, etc.). Coût visuel raisonnable (~12 chars).

## Estimation

~30-45 min (tile routing + 4 fichiers card + helper + panel + tests Jest).
