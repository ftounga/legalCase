# Mini-spec — F-IA-03 / SF-IA-03-13 Cohérence IA sur F-DT-10 Validité rupture conventionnelle

## Identifiant

`F-IA-03 / SF-IA-03-13`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur outils décisionnels

## Statut

`draft`

## Date de création

2026-04-15

## Branche Git

`feat/SF-IA-03-13-coherence-rupture-conventionnelle`

---

## Objectif

Combler le trou de cohérence IA sur F-DT-10 Validité de la rupture conventionnelle. F-IA-03 a été marquée Terminée hier après 11 sous-features couvrant 9 outils ; F-DT-10 a été créée juste après et a été livrée sans F-IA-03 par oubli (la règle de scan transversal n'existait pas encore). Cette subfeature corrige rétroactivement.

À l'issue : l'avocat qui coche manuellement un critère de F-DT-10 contrairement à ce que l'IA a détecté, répondu en question IA ou noté en checklist procédurale F-96 verra apparaître un badge de cohérence (warning ou blocker selon la criticité) — identique au comportement de F-DT-08.

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre F-DT-10 sur un dossier analysé.
2. Les 6 critères (RC_CONSENTEMENT, RC_DELAI_RETRACTATION, RC_HOMOLOGATION, RC_ASSISTANCE, RC_INDEMNITE, RC_ENTRETIENS) sont **pré-cochés** depuis les détections IA quand disponibles (OUI / NON / INCONNU).
3. L'avocat coche un critère contrairement à la détection IA / question IA répondue oui / F-96 VERIFIED → **badge de cohérence** apparaît immédiatement à côté du radio.
4. Les sources d'alertes sont hiérarchisées comme pour les autres outils : F-96 > Question IA > IA (détection directe) > PIECE_MANQUANTE.
5. Niveau d'alerte :
   - **blocker (rouge)** pour les critères bloquants de F-DT-10 (consentement, délai rétractation, homologation, indemnité) — ces critères entraînent la nullité de la rupture conventionnelle, une incohérence est critique.
   - **warning (orange)** pour les critères non-bloquants (assistance, entretiens) — moins critique mais signalé.

### Pré-remplissage IA

- Si l'IA a émis un `rupture_conv_validity_detection` dans son JSON avec une réponse `OUI` / `NON` / `INCONNU` pour chaque critère, les radios sont pré-sélectionnés à cette valeur à l'ouverture du formulaire.
- Si absent, les radios restent à `INCONNU` (comportement actuel).

### Nouveaux signaux backend

#### Extension du prompt IA (`TRAVAIL_INSTRUCTION` et `EnrichedAnalysisService`)

Ajouter l'émission d'un objet `rupture_conv_validity_detection` analogue à `licenciement_validity_detection`, émis uniquement si `compensation_data.type_rupture === "RUPTURE_CONVENTIONNELLE"` :

```json
"rupture_conv_validity_detection": {
  "RC_CONSENTEMENT": {"reponse": "OUI"|"NON"|"INCONNU", "justification": "..."},
  "RC_DELAI_RETRACTATION": {"reponse": ..., "justification": ...},
  "RC_HOMOLOGATION": {...},
  "RC_ASSISTANCE": {...},
  "RC_INDEMNITE": {...},
  "RC_ENTRETIENS": {...}
}
```

Guidelines IA (dans le prompt) :
- RC_CONSENTEMENT : `OUI` si aucun élément n'évoque pression / dol / menace / erreur. `NON` si vice du consentement évoqué dans les pièces. `INCONNU` à défaut.
- RC_DELAI_RETRACTATION : `OUI` si ≥ 15 jours calendaires entre signature et demande d'homologation. `NON` si < 15 jours. `INCONNU` si dates manquantes.
- RC_HOMOLOGATION : `OUI` si pièce d'homologation DREETS présente. `NON` si refus documenté. `INCONNU` à défaut.
- RC_ASSISTANCE : `OUI` si assistance documentée (avocat, conseiller, représentant). `NON` si l'employeur a refusé cette possibilité. `INCONNU` à défaut.
- RC_INDEMNITE : `OUI` si indemnité spécifique connue ≥ indemnité légale calculée. `NON` si strictement inférieure. `INCONNU` à défaut.
- RC_ENTRETIENS : `OUI` si au moins un entretien documenté (compte-rendu / correspondance). `NON` si aucune trace. `INCONNU` à défaut.

#### Extension `critere_code` dans procedure_checks et ai_questions

Les codes `RC_CONSENTEMENT`, `RC_DELAI_RETRACTATION`, `RC_HOMOLOGATION`, `RC_ASSISTANCE`, `RC_INDEMNITE`, `RC_ENTRETIENS` sont ajoutés à l'enum accepté dans le prompt enrichment (section `points_procedure` et `pieces_manquantes`). Pas de migration — c'est une valeur de chaîne, le prompt instruit simplement l'IA.

#### Extraction côté `CaseAnalysisResponse`

Ajouter un record `RuptureConvValidityDetection` (miroir `LicenciementValidityDetection`) et son extracteur depuis le JSON root. Exposé dans `CaseAnalysisResponse` via `ruptureConvValidityDetection`.

### Règle de cohérence côté frontend

`RuptureConvSectionComponent` :
- Nouveau `@Input() aiData?: RuptureConvValidityDetection | null` (propagé depuis `case-file-detail` via `[aiData]="synthesis()?.ruptureConvValidityDetection"`).
- Nouveau `@Input() procedureChecks?: ProcedureCheck[] | null`.
- Nouveau `@Input() aiQuestions?: AiQuestion[] | null`.
- Nouveau `@Input() piecesManquantes?: PieceManquanteEntry[] | null`.
- `coherenceAlerts` computed qui produit un `Record<code, CoherenceAlert>` miroir de `LicenciementSectionComponent`.
- Badges à côté de chaque radio dans le template.
- Pré-remplissage : si `aiData.detections[code].reponse` est `OUI` ou `NON` ou `INCONNU`, initialiser le signal correspondant.

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| Dossier non analysé / synthesis null | `aiData = null`, pas de pré-remplissage, pas d'alerte (inchangé) |
| `rupture_conv_validity_detection` absent du JSON IA | idem — `aiData = null` |
| Type de rupture ≠ RUPTURE_CONVENTIONNELLE | Le bloc F-DT-10 n'est pas affiché (SF-DT-10-04) — pas d'impact |
| IA renvoie une réponse invalide (autre que OUI/NON/INCONNU) | Normalisation fail-open côté backend : traitée comme INCONNU |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-DT-07/08/09, F-FA-05/06/07, F-IM-05/06/07 déjà couverts par F-IA-03 (SF-IA-03-01 à 11). F-DT-10 est le seul outil décisionnel sans couverture à ce jour.
- [x] **Autres pays** : non applicable — F-DT-10 est France-only.
- [x] **Autres domaines** : non applicable — F-DT-10 est DROIT_DU_TRAVAIL uniquement.
- [x] **Autres UI patterns** : le pattern "alertes de cohérence sur critères binaires OUI/NON/INCONNU" est celui de F-DT-08 Licenciement et F-FA-07 Checklist divorce. F-DT-10 doit s'aligner.
- [x] **Autres flows transversaux** : aucun impact auth / workspace / plans / navigation.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `CaseAnalysisResult` doit exposer `ruptureConvValidityDetection` — à ajouter.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.RuptureConvValidityDetection` à créer (miroir de `LicenciementValidityDetection`).
- [x] **Service / logique métier** : `CaseAnalysisResponse.extractRuptureConvValidityDetection` à créer pour parser le JSON IA.
- [x] **Entité JPA + schéma DB** : aucune persistance dédiée — les détections IA sont dans l'analyse IA (`analysis_result` JSON, déjà existant). Rien à migrer.
- [x] **Tests existants** : tests F-IA-03 sur F-DT-08 servent de modèle comportemental.

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable directement (c'est un complément à une feature existante), mais les 6 questions s'appliquent au niveau du comportement cible :

- [x] **Cohérence IA (F-IA-03)** : **c'est l'objet de cette SF**.
- [x] **Refresh dashboard (F-IA-02)** : déjà intégré lors de SF-DT-10-03 (`triggerRefresh` dans le `next:` de analyze).
- [x] **Pré-remplissage IA** : ajouté dans cette SF (radios pré-cochés depuis `rupture_conv_validity_detection`).
- [x] **Persistance des inputs** : déjà OK — `RuptureConvAnalysis.reponses_data` JSON persiste les 6 critères.
- [x] **Masquage conditionnel selon type** : déjà en place (SF-DT-10-04) — affiché uniquement si `type_rupture === RUPTURE_CONVENTIONNELLE` + FRANCE.
- [x] **Alertes actives après calcul** : le nouveau `coherenceAlerts` computed ne doit PAS inclure `|| this.result()`. Seulement `!this.showForm()`. Aligné SF-IA-03-12.

### Audit rétroactif des autres outils

Vérification exhaustive de F-IA-03 sur les 10 outils (après cette SF) :

| Outil | F-IA-03 couvert ? | Subfeature |
|-------|-------------------|------------|
| F-DT-07 Ancienneté | Oui | SF-IA-03-04 |
| F-DT-08 Validité licenciement | Oui | SF-IA-03-01/02/03 |
| F-DT-09 Comparateur indemnités | Oui | SF-IA-03-05 |
| F-DT-10 Validité rupture conventionnelle | **À couvrir** | **SF-IA-03-13 (cette SF)** |
| F-FA-05 Partage immobilier | Oui | SF-IA-03-08 |
| F-FA-06 Calendrier garde | Oui | SF-IA-03-07 |
| F-FA-07 Checklist divorce | Oui | SF-IA-03-06 |
| F-IM-05 Titre séjour | Oui | SF-IA-03-09 |
| F-IM-06 Recours | Oui | SF-IA-03-10 |
| F-IM-07 Droit au travail | Oui | SF-IA-03-11 |

**Conclusion de l'audit : F-DT-10 est le seul outil non couvert.** Après SF-IA-03-13 merge, les 10 outils auront tous F-IA-03.

### Décision

- [x] Étendu à la cible applicable (F-DT-10) dans cette subfeature
- [ ] Subfeature(s) parallèle(s)
- [ ] Backlog
- [x] Non applicable aux 9 autres outils (déjà couverts)

---

## Critères d'acceptation

- [ ] Prompt IA initial (`LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`) étendu avec l'émission de `rupture_conv_validity_detection` (conditionnel au type de rupture).
- [ ] Prompt IA enrichment (`EnrichedAnalysisService`) étendu similairement.
- [ ] Codes `RC_*` ajoutés à l'enum accepté pour `points_procedure.critere_code` et `ai_questions.critere_code` dans le prompt enrichment.
- [ ] Record `CaseAnalysisResponse.RuptureConvValidityDetection` créé (miroir `LicenciementValidityDetection`).
- [ ] Extracteur `extractRuptureConvValidityDetection` + exposition dans `CaseAnalysisResponse`.
- [ ] Frontend `CaseAnalysisResult` TS model étendu avec `ruptureConvValidityDetection`.
- [ ] `RuptureConvSectionComponent` : inputs `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes` ajoutés.
- [ ] `coherenceAlerts` computed produit des alertes hiérarchisées (F96 > QUESTION_IA > IA > PIECE_MANQUANTE).
- [ ] Pré-remplissage des radios depuis `aiData.detections`.
- [ ] Badges dans le template, niveaux blocker (4 critères) / warning (2 critères).
- [ ] Gate `coherenceAlerts` : uniquement `!this.showForm()`, pas de `|| this.result()` (règle SF-IA-03-12).
- [ ] Case-file-detail passe les inputs au composant.
- [ ] Tests Jest ≥ 5 : pré-remplissage, alerte F96, alerte question IA, alerte IA, hiérarchie MULTI.
- [ ] Tests backend : extracteur retourne détections valides, normalisation fail-open pour réponses invalides.
- [ ] 943+ tests frontend verts, build OK.

---

## Périmètre

### Hors scope (explicite)

- Toucher les 9 autres outils déjà couverts.
- Ajouter de nouveaux critères à F-DT-10 (les 6 existants sont figés par SF-DT-10-01).
- Extension F-IA-03 à d'autres outils non décisionnels (ex. fiche prud'homale).
- Calcul automatique backend de l'indemnité légale pour RC_INDEMNITE — l'IA compare les montants si les deux sont connus, sinon INCONNU.
- Migration de base (aucune nécessaire).

---

## Valeurs initiales

Sans objet — uniquement enrichissement de signaux existants.

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs | Normalisation |
|-------|-------------|---------|---------------|
| `rupture_conv_validity_detection.RC_*.reponse` | Non | `OUI` / `NON` / `INCONNU` | upper-case, fail-open vers INCONNU |
| `procedure_checks.critere_code` | Non | inclut `RC_*` | upper-case |
| `ai_questions.critere_code` | Non | inclut `RC_*` | upper-case |

---

## Technique

### Endpoints

Aucun nouveau endpoint.

### Tables impactées

Aucune (pas de migration).

### Migration Liquibase

Non applicable.

### Composants backend

- `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION` : enrichi.
- `EnrichedAnalysisService.SYSTEM_PROMPT_TEMPLATE` : enrichi.
- `CaseAnalysisResponse` : nouveau record `RuptureConvValidityDetection` + extracteur.

### Composants Angular

- `core/models/case-analysis.model.ts` : `CaseAnalysisResult` étendu avec `ruptureConvValidityDetection`.
- `core/models/rupture-conv.model.ts` : types d'alerte ajoutés.
- `RuptureConvSectionComponent` : ngOnChanges, computed `coherenceAlerts`, badges template.
- `CaseFileDetailComponent` : passe 4 nouveaux inputs à `<app-rupture-conv-section>`.

---

## Plan de test

### Tests unitaires backend

- [ ] `CaseAnalysisResponseTest` : `extractRuptureConvValidityDetection` retourne détections valides pour JSON complet.
- [ ] `CaseAnalysisResponseTest` : réponses invalides → normalisées en INCONNU.
- [ ] `CaseAnalysisResponseTest` : noeud absent → retourne record avec map vide.

### Tests unitaires frontend

- [ ] Pré-remplissage : `aiData.detections.RC_CONSENTEMENT.reponse = "OUI"` → `reponses().RC_CONSENTEMENT = "OUI"`.
- [ ] Alerte F96 : procedureCheck VERIFIED sur RC_HOMOLOGATION + reponse NON → alert blocker source F96.
- [ ] Alerte question IA : réponse "oui" sur question critere_code RC_ASSISTANCE + reponse NON → alert warning source QUESTION_IA.
- [ ] Alerte IA : `aiData.RC_INDEMNITE.reponse = "NON"` + reponse OUI → alert blocker source IA.
- [ ] Hiérarchie MULTI : F96 + question IA + IA convergent → source MULTI.
- [ ] Pas d'alerte quand la réponse user correspond à la détection IA.
- [ ] Pas d'alerte quand showForm=false (bloc résultat affiché).

### Isolation workspace

- [x] N/A — enrichissement frontend + extraction JSON backend.

### Validation manuelle

- [ ] Staging, dossier Martin : F-DT-10 affiche badges sur critères incohérents.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune structurelle** — enrichissement de signaux existants.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| `LegalDomainPromptBuilder` | Instruction supplémentaire dans prompt | Tests prompt existants |
| `CaseAnalysisResponse` | Nouveau champ exposé | Extraction existante inchangée |
| `RuptureConvSectionComponent` | Inputs + computed + template badges | Tests existants + nouveaux |
| Autres outils F-IA-03 | Aucun | Tests existants |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `F-DT-10 Terminée` (4/4) — composant + endpoints + orchestration en place.
- `F-IA-03 Terminée` (11 SF) — pattern établi, réutilisable.
- `SF-IA-03-12 Done` — gate `coherenceAlerts` sans `result()`.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi ajouter `rupture_conv_validity_detection` au prompt et pas réutiliser `licenciement_validity_detection`** : les critères sont différents (cadre art. L1237 vs art. L1232), les codes sont différents (RC_* vs FR_*). Les fusionner confondrait des domaines juridiques distincts.
- **Pourquoi émettre conditionnellement selon `type_rupture`** : inutile de demander à l'IA d'évaluer la rupture conventionnelle sur un dossier de licenciement sec. Économie de tokens et réponses plus fiables.
- **Pourquoi RC_CONSENTEMENT / RC_DELAI / RC_HOMOLOGATION / RC_INDEMNITE en blocker** : ce sont les 4 critères juridiquement bloquants de F-DT-10 (art. L1237-11 s. — leur défaut entraîne la nullité). Aligné sur le `bloquant: true` du référentiel.
- **Pourquoi RC_ASSISTANCE / RC_ENTRETIENS en warning** : non-bloquants juridiquement, mais divergence avec l'IA mérite d'être signalée comme information.
- **Pourquoi pas de migration** : tous les signaux sont dans du JSON (`analysis_result`, `reponses_data`) ou des champs existants (`procedure_checks.critere_code`, `ai_questions.critere_code`). Rien de structurel à changer.
- **Retour d'expérience gouvernance** : cette subfeature est le résultat d'un trou de cohérence transversale repéré par l'utilisateur — F-DT-10 shippée avant l'ajout de la règle de scan. La nouvelle checklist "nouvelle feature d'outil décisionnel" (commit `c872f5a`) force maintenant la question "faut-il F-IA-03 ?" pour toute future feature similaire.
