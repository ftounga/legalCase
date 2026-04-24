# Mini-spec — F-155 / SF-155-05 Factorisation `CoherenceAlert` + helper `CoherenceAlertBuilder`

---

## Identifiant

`F-155 / SF-155-05`

## Feature parente

`F-155` — Cohérence frontend des composants décisionnels (harmonisation post-audit 2026-04-24).

## Statut

`in-progress`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-05-coherence-alert-factorisation`

---

## Objectif

Factoriser les **7 interfaces `CoherenceAlert` locales** (aujourd'hui redéfinies dans chaque composant décisionnel avec des champs divergents) en une **interface générique `CoherenceAlert<F>`** + un **helper `CoherenceAlertBuilder`** partagés, pour supprimer la dette de convergence identifiée DIV-5 dans l'audit F-155 (2026-04-24).

---

## Comportement attendu

### Cas nominal

1. Les 7 composants décisionnels (`harcelement-licenciement-nul`, `inaptitude`, `heures-sup`, `oqtf-avec-delai`, `oqtf-sans-delai`, `annexe13-be`, `immigration-title-decision`) utilisent **la même interface `CoherenceAlert<FieldEnum>`** importée de `shared/coherence-popover/coherence-alert.model.ts`.
2. Chaque composant conserve son enum `FieldEnum` local (ex. `HLNAlertField = 'SALAIRE' | 'MOTIF_NULLITE'`) — c'est le paramètre générique de l'interface.
3. Chaque composant utilise le helper `CoherenceAlertBuilder<F>` pour construire des alertes multi-sources (fusion `contributors` + résolution `source='MULTI'` + aggregation des `reason`).
4. Le template HTML continue à appeler `alertBadgeLabel(alert)` et `alertTooltip(alert)` sans modification (rétrocompat stricte).
5. L'API publique des composants (inputs TOOL_REGISTRY) reste inchangée.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucune source contributeur (IA / F96 / QUESTION_IA / PIECE_MANQUANTE) ne produit de divergence | Le builder retourne `null` — pas d'alerte affichée |
| Plusieurs sources convergent sur la même `expectedValue` | `source='MULTI'`, `contributors` liste les sources (ordre d'insertion), `reason` concaténé par " ET " |
| Une source contribue mais une `expectedValue` différente | Seule la première `expectedValue` est retenue (aligné sur pattern `buildMotifAlert` canonique) |
| `severity` non précisée | Défaut `WARNING` (configurable via paramètre builder) |
| Un composant n'expose qu'une source (ex. IA only pour `inaptitude`) | Le builder fonctionne en dégradé — `source='IA'`, `contributors=['IA']` |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : scan ci-dessous
- [x] **Autres pays** : non applicable (factorisation transverse TS, pas de logique par pays)
- [x] **Autres domaines** : non applicable (factorisation applicable aux 3 domaines `DROIT_DU_TRAVAIL` / `DROIT_FAMILLE` / `DROIT_IMMIGRATION`)
- [x] **Autres UI patterns** : aucun autre pattern d'alerte inline utilisé dans le codebase (seul `CoherencePopoverTriggerDirective` existe)
- [x] **Autres flows transversaux** : non impactés (pas de changement d'auth / workspace / plans / routing)

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** — interface `CoherenceAlert<F>` unique dans `shared/`
- [x] **Record / DTO backend** — aucun impact backend (frontend-only)
- [x] **Service / logique métier** — `CoherenceAlertBuilder` helper TS
- [x] **Entité JPA + schéma DB** — aucun impact DB
- [x] **Tests existants** — chaque composant a `.spec.ts`, migration vers nouvelle interface sans régression

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern UI pourrait-il être réutilisé ?** — sur TOUS les composants décisionnels présents ET futurs. Le helper est conçu pour être générique sur le type `F` (enum de fields).
- [x] **Y a-t-il des patterns concurrents ?** — oui : les 7 interfaces locales `HLNCoherenceAlert`, `InaptitudeCoherenceAlert`, `HsCoherenceAlert`, `OqtfCoherenceAlert`, `OqtfSdCoherenceAlert`, `IM08AnnexeBeCoherenceAlert`, `IM05CoherenceAlert`. Classement : **harmonisation immédiate dans cette SF** (remplacées par `CoherenceAlert<F>`).
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** — futurs composants décisionnels (F-DT-16/17/18 famille, F-FA-08+, F-IM-09+ etc.) adopteront ce helper par défaut.
- [x] **Le nouveau composant a-t-il un équivalent design que ce design remplace ?** — non, c'est une factorisation d'un pattern déjà établi dans `immigration-title-decision-section`.

### Scan des autres composants décisionnels non touchés par SF-155

Composants décisionnels existants hors des 7 du scope :

| Composant | Utilise `coherenceAlerts` ? | Classement |
|-----------|----------------------------|-----------|
| `anciennete-section` | Oui (F-IA-03-07) | **Backlog** — SF future d'adoption (non bloquant : l'ancien pattern local reste valide) |
| `calendrier-garde-section` | À vérifier | **Backlog** — pas dans scope DIV-5 |
| `divorce-checklist-section` | À vérifier | **Backlog** |
| `immigration-work-right-section` | À vérifier | **Backlog** |
| `immigration-recours-section` | À vérifier | **Backlog** |
| `licenciement-section` | Oui | **Backlog** — adoption progressive |
| `indemnite-comparatif-section` | Oui | **Backlog** |
| `rupture-conv-section` | Oui | **Backlog** |
| `partage-immobilier-section` | Oui | **Backlog** |

**Décision** : cette SF se limite aux 7 composants identifiés dans DIV-5 de l'audit F-155. Les autres composants adopteront `CoherenceAlert<F>` par SF dédiée ou lors de la prochaine modification. Pas de bloquant immédiat — l'interface générique est rétro-compatible (les interfaces locales continuent à compiler).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 7 composants DIV-5 | Oui | Migration intégrée à cette SF |
| Autres composants décisionnels | Oui (partiellement) | Backlog (F-155 SF-155-06 futur) |
| Backend DTOs | Non | TS-only |
| E2E / smoke tests | Non | Pas d'impact sur routing / auth |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature — 7 composants DIV-5 migrés ici.
- [x] Backlog pour les cibles restantes — autres composants décisionnels adopteront au gré des prochaines modifications.

---

## Impact par domaine métier

Cette SF est **transversale / infrastructure** : elle ne touche pas de logique métier (droit du travail, immigration, famille) mais uniquement la **forme d'un type de données partagé**. Aucune adaptation par domaine ou par pays. Le type générique `F extends string` permet à chaque domaine de garder son enum local.

---

## Critères d'acceptation

- [x] **Nouvelle interface** `CoherenceAlert<F extends string>` existe dans `frontend/src/app/shared/coherence-popover/coherence-alert.model.ts` avec les champs `field`, `source`, `contributors`, `severity`, `expectedDisplay`, `reason`, `pieceTexte?`.
- [x] **Nouveau helper** `CoherenceAlertBuilder<F>` dans `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts` avec méthodes fluides `.addSource()` + `.build()` + static `.forField()`.
- [x] Les 7 composants utilisent `CoherenceAlert<FieldEnum>` importé du shared — aucune interface locale `*CoherenceAlert` subsistante dans ces 7 fichiers.
- [x] Les 7 composants compilent (`npx tsc --noEmit`) sans erreur.
- [x] Les tests existants des 7 composants passent (`npx jest`).
- [x] Le helper a son propre fichier spec `coherence-alert-builder.spec.ts` avec **10+ tests** couvrant les combinaisons de sources.
- [x] **Rétrocompat HTML** : `alertBadgeLabel()` et `alertTooltip()` des composants conservent leur signature (template.html inchangé).
- [x] **API publique préservée** : les `@Input()` des composants sont inchangés (consumers TOOL_REGISTRY non impactés).

---

## Périmètre

### Hors scope (explicite)

- Enrichir les `coherenceAlerts` des 6 composants F-155 avec les sources `F96` / `QUESTION_IA` / `PIECE_MANQUANTE` non exploitées aujourd'hui (DIV-2 de l'audit). **Cette SF est structure only** — elle ne change pas la logique métier des alertes. L'enrichissement des sources est tracké séparément (SF-155-06 backlog).
- Migration des composants décisionnels **hors des 7 cibles DIV-5** (`anciennete`, `licenciement`, `rupture-conv`, `calendrier-garde`, `divorce-checklist`, `indemnite-comparatif`, `partage-immobilier`, `immigration-work-right`, `immigration-recours`).
- Changement de palette, datepicker, ou autres règles cohérence visuelles (DIV-1/3/4/6 déjà traitées SF-155-04-C).
- Modifications backend.

---

## Contraintes de validation

Aucune validation métier nouvelle. Le helper expose une API strictement typée :

| Champ | Obligatoire | Format / Valeurs autorisées |
|-------|-------------|----------------------------|
| `field` | Oui | `F extends string` (enum fourni par le composant) |
| `source` | Oui | `'F96' \| 'QUESTION_IA' \| 'IA' \| 'PIECE_MANQUANTE' \| 'MULTI'` |
| `contributors` | Oui | `CoherenceAlertSource[]` (non vide, ordre d'insertion) |
| `severity` | Oui | `'CRITICAL' \| 'WARNING' \| 'INFO'` (défaut `WARNING`) |
| `expectedDisplay` | Oui | string non vide |
| `reason` | Oui | string (concat " ET " si multi-sources) |
| `pieceTexte` | Non | string \| null |

---

## Technique

### Fichiers créés

- `frontend/src/app/shared/coherence-popover/coherence-alert.model.ts` (interface générique + types source/severity)
- `frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts` (helper fluide)
- `frontend/src/app/shared/coherence-popover/coherence-alert-builder.spec.ts` (tests unitaires du helper)

### Fichiers modifiés

- `frontend/src/app/case-files/harcelement-licenciement-nul-section/harcelement-licenciement-nul-section.component.ts`
- `frontend/src/app/case-files/inaptitude-section/inaptitude-section.component.ts`
- `frontend/src/app/case-files/heures-sup-section/heures-sup-section.component.ts`
- `frontend/src/app/case-files/oqtf-avec-delai-section/oqtf-avec-delai-section.component.ts`
- `frontend/src/app/case-files/oqtf-sans-delai-section/oqtf-sans-delai-section.component.ts`
- `frontend/src/app/case-files/annexe13-be-section/annexe13-be-section.component.ts`
- `frontend/src/app/case-files/immigration-title-decision-section/immigration-title-decision-section.component.ts`

### Migration DB / backend

Aucune.

---

## Plan de test

### Tests unitaires du helper (nouveaux)

- [x] `coherence-alert-builder.spec.ts` — `forField('X').build()` sans contributor → `null`
- [x] `coherence-alert-builder.spec.ts` — une source `IA` seule → `source='IA'`, `contributors=['IA']`
- [x] `coherence-alert-builder.spec.ts` — deux sources convergentes sur même expected → `source='MULTI'`, `contributors=['IA','F96']`
- [x] `coherence-alert-builder.spec.ts` — trois sources (IA + F96 + QUESTION_IA) → `source='MULTI'`, 3 contributors
- [x] `coherence-alert-builder.spec.ts` — deuxième source avec `expected` différent → ignorée silencieusement
- [x] `coherence-alert-builder.spec.ts` — `pieceTexte` transmis via `.addPieceManquante()` → `contributors` contient `PIECE_MANQUANTE`, `pieceTexte` rempli
- [x] `coherence-alert-builder.spec.ts` — `severity` par défaut → `WARNING`
- [x] `coherence-alert-builder.spec.ts` — `severity='CRITICAL'` override → champ alert.severity === `CRITICAL`
- [x] `coherence-alert-builder.spec.ts` — `reason` concaténé par ` ET ` quand multi-sources
- [x] `coherence-alert-builder.spec.ts` — ordre d'insertion des contributors préservé (insertion order)

### Tests composants (existants à préserver)

- Tous les tests `.spec.ts` existants des 7 composants doivent passer sans modification de leur logique métier (les imports peuvent changer si l'interface locale était exportée).

### Isolation workspace

Non applicable — changement de type TypeScript.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — refactor TS frontend interne

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné (pattern TS uniquement, pas de modification comportementale observable end-to-end).

---

## Dépendances

### Subfeatures bloquantes

- SF-155-04 (Done, mergée dans master — 10 PRs #512/#516/#517/#521-#526 + fix #529)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- Le type générique `F extends string` permet à chaque composant de conserver son enum de fields local (pas de godlike enum central). C'est le pattern canonique F-IM-05 (qui a son propre `IM05AlertField`).
- Le helper **ne déduit pas** la logique métier (pas de CODE_TO_MOTIF, pas de seuils). Il se limite à la **consolidation multi-sources** : chaque composant appelle `builder.addSource()` N fois avec ses propres conditions métier.
- Les composants conservent leurs méthodes privées `buildXxxAlert()` — elles **utilisent** désormais le builder pour retourner un `CoherenceAlert<F>` uniforme plutôt que de construire l'objet à la main.
- `severity` par défaut `WARNING` (cas le plus fréquent). `CRITICAL` réservé aux cas métier urgents (48h OQTF, placement CRA, transfert imminent). `INFO` pour les notes discrètes (ex. `SALAIRE_DEDUIT` heures-sup).
- Le champ `level` legacy de `heures-sup` (`'warning' | 'info'`) est mappé vers `severity` (`'WARNING' | 'INFO'`).
- Le champ `blocker` legacy d'`annexe13-be` est dérivé de `severity === 'CRITICAL'` — on aligne sur la nouvelle convention.
- Le champ `iaValue` legacy de `heures-sup` (descriptif verbeux) est mappé vers `expectedDisplay` (équivalent sémantique).
