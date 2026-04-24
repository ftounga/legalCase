# Mini-spec — F-155 / SF-155-06 Enrichissement 4-sources des `coherenceAlerts` (ferme DIV-2)

---

## Identifiant

`F-155 / SF-155-06`

## Feature parente

`F-155` — Cohérence frontend des composants décisionnels (harmonisation post-audit 2026-04-24).

## Statut

`in-progress`

## Date de création

2026-04-24

## Branche Git

`feat/SF-155-06-coherence-alerts-multi-source`

---

## Objectif

Câbler **les 4 sources `F96` / `QUESTION_IA` / `IA` / `PIECE_MANQUANTE`** dans les 6 composants décisionnels F-155 (hors F-IM-05 déjà canonique), via le helper `CoherenceAlertBuilder` livré par SF-155-05. Ferme DIV-2 (MAJEURE) : les inputs `procedureChecks` / `aiQuestions` / `piecesManquantes` étaient déjà câblés via TOOL_REGISTRY mais non exploités par les `buildXxxAlert()`.

---

## Comportement attendu

### Cas nominal

1. Chaque `buildXxxAlert()` scanne les 4 sources disponibles (`aiData`, `procedureChecksSignal()`, `aiQuestionsSignal()`, `piecesManquantesSignal()`) pour un field donné et injecte chaque contributor via `builder.addSource(...)`.
2. Quand **≥ 2 sources** convergent sur la même `expectedDisplay`, le builder produit `source='MULTI'` + `contributors=[...]` + `reason` concaténé par `' ET '`.
3. La première `expectedDisplay` fixe la valeur canonique — sources divergentes ignorées silencieusement (pattern F-IM-05).
4. Si aucun matching sémantique clair F96/QUESTION_IA/PIECE_MANQUANTE pour un field donné, la source est **skip** plutôt que devinée (règle pragmatique — mieux vaut 2 sources solides que 4 flakey).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `procedureChecks` null/undefined | `procedureChecksSignal()` renvoie `[]`, les boucles no-op |
| `aiQuestions` null/undefined | `aiQuestionsSignal()` renvoie `[]`, les boucles no-op |
| `piecesManquantes` null/undefined | `piecesManquantesSignal()` renvoie `[]`, les boucles no-op |
| Une seule source contribue | `source='IA'` / `'F96'` / `'QUESTION_IA'`, `contributors` à 1 élément, rétrocompat garantie |
| Divergences inter-sources (IA dit A, F96 dit B) | Première source retenue (ordre d'insertion), les divergentes ignorées (pattern canonique) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : scan ci-dessous (6 composants F-155 + canonique F-IM-05).
- [x] **Autres pays** : non applicable — chaque composant garde sa propre gate pays (inchangée).
- [x] **Autres domaines** : non applicable — enrichissement TS par composant, transversal aux 3 domaines.
- [x] **Autres UI patterns** : `CoherencePopoverTriggerDirective` intact, template HTML inchangé.
- [x] **Autres flows transversaux** : aucun (pas d'auth / workspace / plans / routing).

### Niveaux de vérification

- [x] **Modèle TypeScript** : `CoherenceAlert<F>` inchangée (SF-155-05).
- [x] **Service / logique métier** : chaque composant garde ses règles de divergence IA — on ajoute F96 / QUESTION_IA / PIECE_MANQUANTE.
- [x] **Entité JPA / DB** : aucun impact.
- [x] **Tests existants** : 100 % préservés, tests SF-155-05 conservés intacts.

### Scan des 7 composants (6 F-155 + canonique F-IM-05)

| Composant | Statut actuel sources | Traitement SF-155-06 |
|-----------|-----------------------|----------------------|
| `harcelement-licenciement-nul-section` (F-DT-11) | IA seulement sur SALAIRE + MOTIF_NULLITE | Ajouter F96 + QUESTION_IA + PIECE_MANQUANTE sur MOTIF_NULLITE (mapping motif via crit `FR_MOTIVATION` / `HLN_MOTIF_NULLITE`) + PIECE_MANQUANTE sur SALAIRE |
| `inaptitude-section` (F-DT-15) | IA seulement sur 4 fields | Ajouter PIECE_MANQUANTE sur les 4 fields ; QUESTION_IA sur RECLASSEMENT (réponse "oui") ; pas de F96 matching évident — skip |
| `heures-sup-section` (F-DT-19) | IA seulement sur 3 fields | Ajouter PIECE_MANQUANTE sur TAUX_HORAIRE + HEURES_SUP ; pas de F96 / QUESTION_IA évident — skip |
| `oqtf-avec-delai-section` (F-IM-08-02) | IA seulement sur 3 fields | Ajouter F96 sur MOTIF_OQTF (crit `IM08_MOTIF_OQTF`) + QUESTION_IA sur RECOURS_FORME + PIECE_MANQUANTE sur DATE_NOTIFICATION + MOTIF_OQTF |
| `oqtf-sans-delai-section` (F-IM-08-04) | IA seulement sur 4 fields | Ajouter QUESTION_IA sur RECOURS_FORME (réponse "oui") + PIECE_MANQUANTE sur DATE_HEURE + MOTIF + PLACEMENT_CRA + RECOURS_FORME |
| `annexe13-be-section` (F-IM-08-06) | IA seulement sur 4 fields | Ajouter F96 sur MOTIF_OQT + PIECE_MANQUANTE sur les 4 fields |
| `immigration-title-decision-section` (F-IM-05) | **Canonique déjà 4 sources** sur MOTIF | Aucun changement |

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 6 composants F-155 | Oui | Enrichissement intégré à cette SF |
| F-IM-05 canonique | Déjà conforme | Aucun changement |
| Autres composants décisionnels (non F-155) | Oui | Backlog (adopteront au gré des modifications) |

### Décision

- [x] Étendu à toutes les cibles applicables (6 composants F-155).
- [x] Canonique F-IM-05 déjà conforme — non-régression à vérifier.
- [x] Autres composants décisionnels backlog — non urgent.

---

## Impact par domaine métier

SF **transversale / infrastructure frontend** — enrichissement de la logique d'alerte de cohérence déjà en place. Le comportement métier par domaine (droit du travail / immigration) reste inchangé ; cette SF augmente la **quantité** de divergences détectables (4 sources au lieu de 1) sans changer la sémantique. Aucun adaptation FR/BE spécifique — chaque composant garde sa gate pays existante.

---

## Critères d'acceptation

- [ ] Les 6 composants F-155 utilisent désormais `builder.addSource(...)` pour au moins 2 sources distinctes sur au moins 1 field chacun (source IA conservée + 1 source supplémentaire minimum).
- [ ] Là où 2 sources convergent, `coherenceAlert.source === 'MULTI'` et `contributors.length === 2`, et `reason` contient `' ET '`.
- [ ] Les 3 composants qui n'ont pas encore de signals miroirs (`inaptitude`, `heures-sup`, `oqtf-avec-delai`) en ont maintenant : `procedureChecksSignal`, `aiQuestionsSignal`, `piecesManquantesSignal`.
- [ ] L'API publique des composants est inchangée (inputs TOOL_REGISTRY identiques).
- [ ] Les templates HTML sont inchangés (signatures `alertBadgeLabel` / `alertTooltip` conservées).
- [ ] `tsc --noEmit -p tsconfig.app.json` passe sans erreur.
- [ ] Tous les tests existants des 6 composants restent verts (SF-155-04 / SF-155-05 non-régression).
- [ ] Au minimum **3 nouveaux tests spec par composant** valident : (a) F96 seul déclenche alerte, (b) QUESTION_IA seul, (c) IA + une autre source → MULTI avec 2 contributors. Quand une source n'est pas applicable à un composant, les tests sont substitués par d'autres combinaisons présentes (ex. PIECE_MANQUANTE + IA).
- [ ] Le helper `coherence-alert-builder` existant garde ses 10+ tests verts.

---

## Périmètre

### Hors scope (explicite)

- Modification de la logique métier IA (seuils divergence, mappings) — strictement enrichissement multi-sources.
- Ajout ou renommage de `field` enum — enum par composant inchangés.
- Enrichissement des composants décisionnels hors F-155 (`anciennete`, `licenciement`, `rupture-conv`, `partage-immobilier`, `calendrier-garde`, `divorce-checklist`, `indemnite-comparatif`, `immigration-work-right`, `immigration-recours`) — backlog.
- Introduction de nouveaux types d'alertes / champs sur `CoherenceAlert<F>` — l'interface reste identique (SF-155-05).
- Backend : aucun changement, aucun nouveau DTO, aucune migration Liquibase.

---

## Contraintes de validation

Aucune nouvelle validation métier. Pragmatisme pour le matching :

| Source | Règle pragmatique |
|--------|-------------------|
| `IA` | Conservée telle quelle (divergence entre `aiData.xxx` et signal form) |
| `F96` | Matching si `procedureCheck.critereCode` égale (case-insensitive) une constante documentée dans le composant ET `statut === 'NON_COMPLIANT'` avec `expectedValue` renseigné, OU si le `critereCode` désigne explicitement le field (listes closed-set) |
| `QUESTION_IA` | Matching si `aiQuestion.critereCode` identique à l'attendu ET `answerText` commence par `'oui'` (variantes `'oui'` / `'oui,'` / `'oui.'` / `'oui '`) ET `expectedValue` renseigné |
| `PIECE_MANQUANTE` | Matching si `pieceManquante.critereCode` matche le field — `addPieceManquante(texte)` appelé sur l'alerte si déjà initiée par une autre source |

Si aucune règle ne matche clairement → **skip** (la source ne contribue pas pour ce field — pas d'invention).

---

## Technique

### Fichiers modifiés

- `frontend/src/app/case-files/harcelement-licenciement-nul-section/harcelement-licenciement-nul-section.component.ts` (+ `.spec.ts`)
- `frontend/src/app/case-files/inaptitude-section/inaptitude-section.component.ts` (+ `.spec.ts`)
- `frontend/src/app/case-files/heures-sup-section/heures-sup-section.component.ts` (+ `.spec.ts`)
- `frontend/src/app/case-files/oqtf-avec-delai-section/oqtf-avec-delai-section.component.ts` (+ `.spec.ts`)
- `frontend/src/app/case-files/oqtf-sans-delai-section/oqtf-sans-delai-section.component.ts` (+ `.spec.ts`)
- `frontend/src/app/case-files/annexe13-be-section/annexe13-be-section.component.ts` (+ `.spec.ts`)

### Fichiers créés

Aucun (le helper + l'interface existent déjà — SF-155-05).

### Migration DB / backend

Aucune.

---

## Plan de test

### Tests composants (nouveaux par composant — minimum 3)

Pour chaque composant :

- [ ] **Test A** — F96 (ou une autre source non-IA) seul déclenche alerte → `source` correspondant, `contributors.length === 1`.
- [ ] **Test B** — QUESTION_IA (ou une autre source non-IA alternative si F96 non applicable) seul déclenche alerte → `source` correspondant, `contributors.length === 1`.
- [ ] **Test C** — IA + autre source convergentes sur le même `expectedDisplay` → `source === 'MULTI'`, `contributors.length === 2`, `reason.includes(' ET ')`.

### Tests existants à préserver

- Toutes les spec existantes des 6 composants (non-régression SF-155-04 et SF-155-05).
- Tests `coherence-alert-builder.spec.ts` (non-régression SF-155-05).

### Isolation workspace

Non applicable — factorisation TS frontend.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale** — enrichissement frontend interne sur données déjà fournies via inputs existants.

### Smoke tests E2E concernés

- [x] Aucun smoke test concerné — comportement interne des composants, pas de modification routing / auth / workspace.

---

## Dépendances

### Subfeatures bloquantes

- SF-155-05 (Done, mergée master commit `dc56d1a`) — fournit `CoherenceAlert<F>` + `CoherenceAlertBuilder`.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- **Règle pragmatique** : si la correspondance `critereCode` n'est pas identifiable avec certitude pour un field donné, **skip la source** plutôt que deviner. Mieux vaut 2 sources solides que 4 flakey.
- **Critères codes utilisés** : alignés sur la convention existante F-IM-05 (`IM05_MOTIF`) et les constants du backend (`CaseAnalysisService` — lignes 69) : `FR_MOTIVATION` pour HLN motif, `HLN_MOTIF_NULLITE` pour pièce manquante, `IM08_MOTIF_OQTF` pour motif OQTF FR avec délai, `IM08_MOTIF_OQT_BE` pour motif annexe 13 BE, `IM08_DATE_NOTIFICATION` / `IM08_DELAI_DEPART` / `IM08_TRANSFERT` / `IM08_PLACEMENT_CRA` / `IM08_RECOURS_FORME` pour les autres.
- **Signal miroirs manquants** : 3 composants (`inaptitude`, `heures-sup`, `oqtf-avec-delai`) consomment actuellement `this.aiData?.xxx` directement dans `buildXxxAlert` — ils doivent gagner des signals miroirs pour que les `computed` réagissent aux nouvelles sources `procedureChecks` / `aiQuestions` / `piecesManquantes`. Pattern canonique F-IM-05 (lignes 96-98).
- Le helper `.addPieceManquante()` n'accroche une pièce qu'à une alerte **déjà initiée** par une autre source — cohérent avec F-IM-05.
