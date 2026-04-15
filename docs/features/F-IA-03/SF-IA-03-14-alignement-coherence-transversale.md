# Mini-spec — F-IA-03 / SF-IA-03-14 Alignement transversal post-audit

## Identifiant

`F-IA-03 / SF-IA-03-14`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur outils décisionnels

## Statut

`draft`

## Date de création

2026-04-15

## Branche Git

`feat/SF-IA-03-14-alignement-coherence-transversale`

---

## Objectif

Suite à l'audit de cohérence transversale du 2026-04-15 (`docs/audits/AUDIT-2026-04-15-coherence-transversale.md`), aligner 5 outils décisionnels sur le pattern F-IA-03 de référence (F-DT-08) :

1. **F-DT-08 Licenciement** : ajouter le gate `!showForm()` manquant dans `coherenceAlerts`.
2. **F-IM-05, F-IM-06, F-IM-07, F-FA-06** : ajouter l'input `@Input() piecesManquantes` et la source PIECE_MANQUANTE dans la hiérarchie ; détecter explicitement la convergence MULTI.

Résultat attendu : les 10 outils décisionnels auront un comportement F-IA-03 identique en structure (modulo les décisions explicites d'acceptation pour F-FA-07 design et F-DT-09 legacy, documentées dans l'audit).

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un outil (ex. F-IM-05). Ses champs sont pré-remplis depuis l'IA.
2. Une pièce manquante taggée avec `critere_code = IM05_MOTIF` existe dans `piecesManquantes`.
3. L'avocat saisit un motif contraire à la détection IA et à la pièce manquante.
4. Un badge de cohérence source `MULTI` apparaît, listant F96 + IA + PIECE_MANQUANTE dans le tooltip.
5. Quand le bloc résultat est affiché (`showForm=false`), aucune alerte n'est calculée (gate).

### Changements spécifiques par outil

| Outil | Changement |
|---|---|
| F-DT-08 Licenciement | Ajout `if (!this.showForm()) return {};` en début de `coherenceAlerts` |
| F-IM-05 Titre séjour | + `@Input() piecesManquantes`, + `buildPiecesIndex`, + source PIECE_MANQUANTE, + détection MULTI |
| F-IM-06 Recours | idem |
| F-IM-07 Droit au travail | idem |
| F-FA-06 Calendrier garde | idem (note : l'alerte est `coherenceAlert` singulier — un seul champ surveillé `FA06_MODE_GARDE`) |

### Outils non modifiés (justifiés dans audit)

- F-DT-07, F-DT-09, F-DT-10, F-FA-05, F-FA-07, F-IM-05/06/07 (pour leur pattern principal — seuls `piecesManquantes` + MULTI ajoutés où applicable).
- F-FA-07 : design 100% procédural acceptable.
- F-FA-05 : traitée dans SF-FA-05-05 (enrichissement complet séparé).

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| `piecesManquantes` input null ou vide | `buildPiecesIndex` retourne `{}` — aucune alerte PIECE_MANQUANTE |
| `pieces.critereCode` pas dans l'enum de l'outil | pièce ignorée silencieusement |
| Convergence MULTI avec avis contradictoires | priorité à la source la plus haute (F96 > QUESTION_IA > IA > PIECE_MANQUANTE) ; les autres sont extraContributors seulement si elles convergent vers la même valeur |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 10 outils sont dans le périmètre (déjà audités)
- [x] **Autres pays** : aligné automatiquement car les `*_CRITERE_CODES` sont cross-pays
- [x] **Autres domaines** : applicable aux 3 domaines (Travail / Famille / Immigration)
- [x] **Autres UI patterns** : pattern `coherenceAlerts` computed uniforme
- [x] **Autres flows transversaux** : aucun impact auth/workspace/plans

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `RuptureConvValidityDetection` et `LicenciementValidityDetection` déjà exposés ; aucune nouvelle API côté types.
- [x] **Record / DTO backend** : aucun changement — les pièces manquantes existent déjà dans `CaseAnalysisResponse.piecesManquantesDetails`.
- [x] **Service / logique métier** : `piecesManquantesSignal` ajouté côté frontend, consommé dans `coherenceAlerts`.
- [x] **Entité JPA + schéma DB** : aucun changement — les données sont déjà dans `ai_analyses.analysis_result` JSON.
- [x] **Tests existants** : les specs des 5 outils cibles sont augmentés (cf. plan de test).

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable directement (alignement d'outils existants). Les 6 questions checklist ont été passées pour chaque outil lors de leur création initiale et complétées par cette SF.

### Résultat du scan (audit source)

Cf. `docs/audits/AUDIT-2026-04-15-coherence-transversale.md`. 5 outils à aligner : F-DT-08 (gate) + F-IM-05/06/07 + F-FA-06 (piecesManquantes + MULTI).

### Décision

- [x] Étendu aux 5 cibles applicables dans cette subfeature
- [x] Subfeature parallèle créée pour F-FA-05 : SF-FA-05-05 (enrichissement complet)
- [ ] Backlog
- [x] Non applicable aux 3 autres outils (F-DT-07/09/10 déjà conformes, F-FA-07 design, audit doc)

---

## Critères d'acceptation

- [ ] `LicenciementSectionComponent.coherenceAlerts` : `if (!this.showForm()) return {};` ajouté en début de computed.
- [ ] `ImmigrationTitleDecisionSectionComponent` : `@Input() piecesManquantes`, signal miroir, `buildPiecesIndex`, source PIECE_MANQUANTE intégrée, détection MULTI via `collectSupportingSources` + `multiOrSingle`.
- [ ] `ImmigrationRecoursSectionComponent` : idem.
- [ ] `ImmigrationWorkRightSectionComponent` : idem.
- [ ] `CalendrierGardeSectionComponent` : idem (adapté au singulier `coherenceAlert`).
- [ ] `CaseFileDetailComponent` HTML : passage de `[piecesManquantes]="synthesis()?.piecesManquantesDetails"` aux 4 outils.
- [ ] Tests Jest ≥ 5 (1 par outil touché) : scénario pièce manquante + alerte PIECE_MANQUANTE + convergence MULTI.
- [ ] Non-régression : tests existants des 5 outils verts.
- [ ] 948+ tests frontend verts, build OK.

---

## Périmètre

### Hors scope (explicite)

- Enrichissement de F-FA-05 (→ SF-FA-05-05 séparée).
- Refactorisation des helpers F-IA-03 en classe abstraite partagée (tentant mais grand blast radius — à reconsidérer si un outil supplémentaire émerge).
- Ajout de nouveaux `critere_code` — les codes existants suffisent.
- Toucher F-DT-07/09/10, F-FA-07.
- Backend (aucun changement — les pièces manquantes sont déjà émises par les prompts existants avec `critere_code`).

---

## Valeurs initiales

Sans objet — alignement de signaux.

---

## Contraintes de validation

Sans objet.

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `licenciement-section.component.ts` : 1 ligne ajoutée.
- `immigration-title-decision-section.component.ts` : enrichissement ~50-80 lignes.
- `immigration-recours-section.component.ts` : idem.
- `immigration-work-right-section.component.ts` : idem.
- `calendrier-garde-section.component.ts` : idem.
- `case-file-detail.component.html` : 4 bindings ajoutés.
- 5 specs Jest enrichis.

### Backend

Aucun impact.

---

## Plan de test

### Tests unitaires Jest

- [ ] `LicenciementSectionComponent` : `coherenceAlerts` retourne `{}` quand `showForm=false`.
- [ ] `ImmigrationTitleDecisionSectionComponent` : pièce manquante avec `critere_code=IM05_MOTIF` + user set motif différent → alert warning source PIECE_MANQUANTE.
- [ ] `ImmigrationRecoursSectionComponent` : idem avec `IM06_RECOURS_TYPE`.
- [ ] `ImmigrationWorkRightSectionComponent` : idem avec `IM07_TITRE_TYPE`.
- [ ] `CalendrierGardeSectionComponent` : idem avec `FA06_MODE_GARDE`.
- [ ] Test convergence MULTI sur au moins un outil (F-IM-05) : F96 + IA + pièce manquante tous cohérents + user dissonant → source MULTI, contributors = 3.

### Tests d'intégration

- [x] N/A — frontend pur.

### Isolation workspace

- [x] N/A — garantie backend inchangée.

### Validation manuelle

- [ ] Staging : dossier avec pièce manquante taggée F-IM-05 → badge PIECE_MANQUANTE visible quand motif utilisateur différent.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — alignement de computed signals locaux.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `LicenciementSectionComponent` | 1 ligne ajoutée | Specs existants |
| 4 composants `*-section` | Inputs + computed enrichis | Specs existants + nouveaux |
| `CaseFileDetailComponent` | 4 bindings ajoutés | Navigation intacte |

### Smoke tests E2E concernés

- [ ] Aucun — logique client.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-03 Terminée`, `SF-IA-03-12 Done`, `SF-IA-03-13 Done`.
- Audit 2026-04-15 disponible.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi ne pas refactoriser les helpers F-IA-03 en classe abstraite** : grand blast radius, modification simultanée de 10 composants. L'alignement pattern-par-pattern via copie contrôlée est plus sûr pour cette étape. Un refacto ultérieur peut être envisagé si une 11ᵉ implémentation émerge.
- **Pourquoi F-FA-06 a `coherenceAlert` singulier** : un seul champ surveillé (`gardeCode`), donc au plus une alerte à la fois. Design acceptable, juste à aligner sur hiérarchie + MULTI.
- **Pourquoi aligner F-DT-08 alors qu'il servait de référence** : le pattern de référence était sa structure (pas de `|| result()` dans le gate). Le manque de `!showForm()` est un écart mineur non-découvert avant l'audit.
- **Pourquoi backend inchangé** : les pièces manquantes sont déjà émises par l'IA avec `critere_code` pour tous les codes supportés. Aucun prompt à modifier.
