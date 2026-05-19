# Mini-spec — [F-246 / SF-246-06] Pré-remplissage IA — Lot Successions / libéralités (Famille FR)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Découpage de référence : `docs/features/F-246/cadrage-decoupage.md` (§3.1 ligne SF-246-06, vague 2).
> **SF de lot** : 8 outils du sous-domaine successions/libéralités (F-FA-24), tous adossés au
> **même record** `FamilleExtractedData` et au **même prompt** `FAMILLE_INSTRUCTION`.

---

## Identifiant

`F-246 / SF-246-06`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-19

## Branche Git

`feat/SF-246-06-lot-successions-prefill`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA des 8 outils décisionnels successions/libéralités (`partage-successoral`, `reserve-heriditaire`, `rapport-succession`, `acceptation-renonciation`, `indivision-successorale`, `devolution-legale`, `donation`, `testament-validite` — F-FA-24) en faisant extraire par le pipeline IA les dates, montants et dénombrements de succession aujourd'hui absents de `FamilleExtractedData`, afin que leurs `prefillFromAi()` cessent d'être des no-op structurels.

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit de la famille FR comportant une succession ouverte (acte de notoriété, déclaration de succession, testament, actes de donation).
2. Le pipeline IA (prompt `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION`) extrait, dans le bloc `famille_extracted_data`, un sous-objet `succession_detection` regroupant les dates / montants / dénombrements factuels de la succession.
3. L'extracteur `extractFamilleData()` parse ce sous-objet en champs typés du record `FamilleExtractedData`.
4. Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose les champs ; les entrées `TOOL_REGISTRY` des 8 outils passent déjà `aiData: ctx.synthesis?.familleExtractedData`.
5. À l'ouverture de l'un des 8 outils, `prefillFromAi()` renseigne les champs détectables du formulaire ; un badge `auto_awesome` « Pré-rempli depuis l'analyse » s'affiche à côté de chaque champ pré-rempli.
6. L'avocat peut modifier toute valeur : le handler `onXxxChange()` correspondant remet `provenance<Field>` à `null` (le badge disparaît) et déclenche la revérification F-IA-03.
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète `getPrefillCount()` pour chacun des 8 outils.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Le LLM ne détecte aucune succession | Sous-objet `succession_detection` à `null` ; tous les `prefillFromAi()` no-op gracieux ; `getPrefillCount()` = 0 sur les 8 outils ; aucun badge | n/a (pipeline async) |
| Une date présente mais ambiguë / non lisible | Le prompt impose `null` plutôt qu'une date approximative ; champ `null` | n/a |
| Le LLM renvoie une date hors ISO `YYYY-MM-DD` | `isoDateOrNull()` côté extracteur rejette → `null` ; pas de pré-fill du champ | n/a |
| Montant de succession ou de libéralités ≤ 0 ou aberrant | `positiveDoubleOrNull()` → `null` (jamais `0` — invariant cadrage §5.2) | n/a |
| Nombre de cohéritiers / descendants négatif ou aberrant (> 50) | `boundedIntOrNull()` avec garde de plage `[0, 50]` → `null` | n/a |
| Mode de partage hors énumération | `null` (fail-open) ; pas de pré-fill du champ concerné | n/a |
| Dossier de famille belge | Tous les champs FR successions restent `null` (le prompt impose null hors FR) ; outils non affichés pour la BE | n/a |
| `famille_extracted_data` absent du JSON IA | `extractFamilleData()` retourne `null` ; outils affichés formulaire vierge | n/a |
| `aiData` arrive après le premier rendu d'un composant | `prefillFromAi()` réinvoqué dans `ngOnChanges()` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : les 8 outils F-FA-24 partagent le sous-domaine succession et certains champs (`dateOuvertureSuccessionDetectee` est lue par 4 outils, `dateDonationDetectee` par 2). C'est précisément la raison de la **SF de lot** : un seul sous-objet `succession_detection`, un seul ajout au record, un seul ajout au prompt — pas de duplication.
- [x] **Autres pays** : France uniquement. Les successions BE relèvent d'autres outils (`pacte_successoral_envisage` F-202) — hors périmètre. Les champs restent `null` pour la BE.
- [x] **Autres domaines** : non applicable — concept propre au droit des successions FR.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique `immigration-title-decision-section`), badges de provenance `auto_awesome`, alertes F-IA-03.
- [x] **Autres flows transversaux** : aucun (pas d'auth, pas de workspace context modifié, pas de plan).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** : `FamilleExtractedData` dans `divorce-accepte.model.ts` — extension de l'interface ; **réalignement** : retrait des champs aspirationnels déclarés sans source backend que cette SF couvre (`dateDecesDetectee`, `nombreCoheritiersDetecte`, etc. — cf. cadrage §5.3).
- [x] **Record / DTO backend** : `CaseAnalysisResponse.FamilleExtractedData` — extension du record + builder F-234.
- [x] **Service / logique métier** : `extractFamilleData()` — extension du parsing JSON.
- [x] **Entité JPA + schéma DB** : non applicable — `familleExtractedData` est sérialisé dans la synthèse IA (`analysis_result` JSON), pas de colonne dédiée. Les inputs des 8 outils restent persistés par leurs endpoints existants (inchangés).
- [x] **Tests existants** : helpers `*-section-prefill-rules.spec.ts` des 8 outils (lectures de champs aspirationnels), tests d'extraction `extractFamilleData()`. Tous mis à jour par cette SF.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : oui — les dates / montants de succession sont croisables. La SF étend `coherenceAlerts` de chaque outil pour intégrer les nouvelles sources IA, sans nouvelle SF jumelle.
- [x] **Refresh dashboard (F-IA-02)** : non modifié — les 8 outils déclenchent déjà `triggerRefresh()` dans le `next:` de leur POST.
- [x] **Pré-remplissage IA** : c'est l'objet de la SF — `prefillFromAi()` réel remplace les no-op structurels.
- [x] **Persistance des inputs** : inchangée — les inputs validés des 8 outils sont déjà persistés via leurs endpoints existants ; le pré-fill ne pré-remplit que le formulaire avant soumission.
- [x] **Masquage conditionnel selon type** : inchangé — la visibilité F-IA-04 des 8 outils est déjà gérée (FR + flags F-200 `succession_envisagee`, `testament_envisage`, `donation_envisagee`, etc.).
- [x] **Alertes actives après calcul** : le gate `coherenceAlerts` ne doit gater que `!this.showForm()` — vérifié sur les 8 composants.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint. La SF réutilise le pattern de pré-fill canonique et étend un record / un prompt existants.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `partage-successoral`, `reserve-heriditaire`, `rapport-succession`, `acceptation-renonciation`, `indivision-successorale`, `devolution-legale`, `donation`, `testament-validite` (F-FA-24) | Oui | Intégrés dans cette SF de lot |
| Outils Famille BE (pacte successoral) | Non | Régime BE distinct — champs `null` en BE |
| `partage-judiciaire` (F-FA-17) | Non | Indivision **hors succession** — traité par SF-246-07 (record partagé, ordre séquentiel) |
| Outils Travail / Immigration | Non | Concept propre au droit des successions |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF livre 8 parties frontend décisionnelles (`*-section` du lot F-FA-24).

### 1. Cohérence visuelle

- [x] **Palette statut** : navy/or info, vert OK, rouge réservé aux alertes critiques — conservé sur les 8 outils.
- [x] **Datepicker** : `<input type="date">` natif pour toutes les dates (décès, ouverture, donation, rédaction testament) — pas de `MatDatepicker`.
- [x] **Typographie** : `JetBrains Mono` pour `baseJuridique` / `formule`, `Inter` pour le reste — conservé.
- [x] **Gate `workspaceCountry`** : outils FR-only — bannière info en cas de mismatch BE (existant).
- [x] **Erreurs** : `MatSnackBar` — pas d'`alert()` / `confirm()`.
- [x] **Refresh dashboard** : `triggerRefresh()` dans le `next:` du POST — existant, inchangé sur les 8 outils.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: FamilleExtractedData | null` — déjà typé strictement sur les 8 composants.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` **ET** `ngOnChanges()` — la SF rend les méthodes effectives.
- [x] Un signal `provenance<Field>` par champ pré-rempli sur chaque composant (`provenanceDateDeces`, `provenanceModePartage`, `provenanceNombreCoheritiers`, `provenanceMontantSuccession`, `provenanceMontantLibs`, `provenanceNombreEnfants`, `provenanceDateDonation`, `provenanceMontantDonations`, `provenanceValeurDonationPartage`, `provenanceActifBrut`, `provenancePassif`, `provenanceDateOuverture`, `provenanceTypeIndivision`, `provenanceNbDescendants`, `provenanceNbFreresSoeurs`, `provenanceDateRedactionTestament`).
- [x] Badge `auto_awesome` « Pré-rempli depuis l'analyse » à côté de chaque champ pré-rempli.
- [x] Handler `onXxxChange()` par champ qui remet `provenance<Field>` à `null`.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` étendu sur les 8 outils pour les champs date / montant pré-remplis.
- [x] Hiérarchie F-96 > Question IA > IA détection > Pièce manquante respectée ; convergence → `'MULTI'`.
- [x] `<app-coherence-popover-trigger>` câblé sur les champs concernés.
- [x] Helper partagé `CoherenceAlertBuilder` (`frontend/src/app/shared/coherence-popover/coherence-alert-builder.ts`).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Les 8 entrées F-FA-24 sont déjà présentes dans `TOOL_REGISTRY` ; `inputs(ctx)` passe déjà `aiData`, `procedureChecks`, `aiQuestions`, `piecesManquantes` — aucune modification du binding requise.
- [x] Static `getPrefillCount(input)` de chaque composant : refactorisé pour appeler le `computePrefillCount()` du helper enrichi.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` sur les 8 outils : mêmes guards, mêmes mappings, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest par outil : (a) 0 champ (aiData vide / BE), (b) M champs partiels, (c) N champs cas nominal.
- [x] Les 8 `tool_id` F-FA-24 sont déjà présents dans `KNOWN_FRONTEND_TOOL_IDS` du test `DecisionToolVisibilityIntegrityIT` — pas de migration `decision_tool_visibility_rules`.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau des outils : majorité **5** (scoring / analyse — réserve héréditaire, validité testament, acceptation/renonciation) ; certains niveau **3-4** (calculateur de dévolution, checklist partage).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Non | Le droit des successions n'a pas de transposition en droit du travail — concept non pertinent |
| Immigration | Non | Concept non pertinent en droit des étrangers |
| Famille | Oui (F-FA-24) | C'est le sous-domaine de cette SF |

> La SF complète le pré-fill d'outils existants — la parité de domaine de F-FA-24 a été tranchée à leur création.

---

## Champs IA à extraire (pré-remplissage)

- [ ] **Aucun pré-remplissage** — non, la SF crée le pré-remplissage des 8 outils.

> Les 8 outils F-FA-24 partagent un **unique sous-objet** `succession_detection`. Un champ source n'est ajouté qu'une fois, même s'il est lu par plusieurs outils (ex. `dateOuvertureSuccessionDetectee` : 4 outils).

| Champ du formulaire | Outil(s) consommateur(s) | Type | Champ source du record `FamilleExtractedData` | Extension requise |
|---------------------|--------------------------|------|------------------------------------------------|-------------------|
| date de décès du de cujus | `partage-successoral` | date (ISO YYYY-MM-DD) | `dateDecesDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date d'ouverture de la succession | `partage-successoral`, `acceptation-renonciation`, `indivision-successorale`, `rapport-succession` | date (ISO YYYY-MM-DD) | `dateOuvertureSuccessionDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| mode de partage demandé | `partage-successoral` | texte (énum `AMIABLE` / `JUDICIAIRE` / `null`) | `modePartageDemandeDetecte` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| nombre de cohéritiers | `partage-successoral` | nombre (entier `[0, 50]`) | `nombreCoheritiersDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| montant de la succession (€) | `reserve-heriditaire` | nombre (€) | `montantSuccessionEurDetecte` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| montant total des libéralités (€) | `reserve-heriditaire` | nombre (€) | `montantLibsTotalEurDetecte` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| nombre d'enfants du défunt | `reserve-heriditaire` | nombre (entier `[0, 50]`) | `nombreEnfantsSuccessionDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de la donation | `rapport-succession`, `donation` | date (ISO YYYY-MM-DD) | `dateDonationDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| montant des donations reçues (€) | `rapport-succession` | nombre (€) | `montantDonationsRecuesEurDetecte` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| valeur de la donation au jour du partage (€) | `rapport-succession` | nombre (€) | `valeurDonationAuJourPartageEurDetectee` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| actif brut de la succession (€) | `acceptation-renonciation` | nombre (€) | `actifBrutSuccessionEurDetecte` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| passif de la succession (€) | `acceptation-renonciation` | nombre (€) | `passifSuccessionEurDetecte` (`Double`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| type d'indivision successorale | `indivision-successorale` | texte (énum `LEGALE` / `CONVENTIONNELLE` / `null`) | `typeIndivisionSuccessoraleDetecte` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| nombre de descendants | `devolution-legale` | nombre (entier `[0, 50]`) | `nbDescendantsDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| nombre de frères / sœurs | `devolution-legale` | nombre (entier `[0, 50]`) | `nbFreresSoeursDetecte` (`Integer`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |
| date de rédaction du testament | `testament-validite` | date (ISO YYYY-MM-DD) | `dateRedactionTestamentDetectee` (`String`, nullable) | [x] record + [x] prompt + [x] extracteur + [x] DTO frontend |

- [x] Pour chaque champ date / valeur à pré-remplir non encore présent, l'extension du record `FamilleExtractedData` **et** du prompt `LegalDomainPromptBuilder` (`FAMILLE_INSTRUCTION`) est explicitement dans le périmètre de cette SF.

> **Note couplage record partagé** : voir « Notes et décisions » — SF-246-06 est la **première SF de la série Famille** à étendre `FamilleExtractedData` ; les SF-246-07 à 12 sont séquentielles après elle.

> **Note de design IA** : 16 champs source ajoutés au record (toutes nullables). Aucun champ booléen redondant : les flags F-200 existants (`succession_envisagee`, `testament_envisage`, `donation_envisagee`…) continuent de piloter la **visibilité** ; les 16 nouveaux champs pilotent le **pré-fill**. `dateDecesDetectee` ≠ `dateOuvertureSuccessionDetectee` : le prompt nomme explicitement les deux concepts (souvent identiques mais distincts juridiquement — invariant cadrage §5.1.1). Les champs non factualisables de façon fiable (libellés de biens, qualité de chaque héritier, attribution préférentielle) restent en saisie manuelle — documenté, pas une dette masquée (cadrage §5.6).

---

## Critères d'acceptation

- [ ] Le record `FamilleExtractedData` contient les 16 nouveaux champs (cf. tableau), tous nullables, propagés par le builder F-234 (champ privé + setter + ajout dans `build()`).
- [ ] Le prompt `FAMILLE_INSTRUCTION` décrit un sous-objet `succession_detection` avec une définition juridique sans ambiguïté par champ + l'instruction `null` hors FR / hors certitude + la distinction explicite `date du décès` / `date d'ouverture de la succession` / `date de donation` / `date de rédaction du testament`.
- [ ] `extractFamilleData()` parse `succession_detection` : dates via `isoDateOrNull()`, montants via `positiveDoubleOrNull()`, dénombrements via `boundedIntOrNull(node, field, 0, 50)`, énumérations via `stringOrNull()` + whitelist.
- [ ] Le DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`) expose les 16 champs avec les bons types TS, et **ne déclare plus** de champ aspirationnel équivalent sans source backend.
- [ ] Les 8 helpers `*-section-prefill-rules.ts` lisent des champs réels ; chaque `computePrefillCount()` retourne le nombre exact de champs pré-remplissables de l'outil.
- [ ] Les 8 `prefillFromAi()` renseignent les champs de leur tableau respectif quand `workspaceCountry === 'FRANCE'`, et restent no-op si BE.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` ; la modification manuelle remet `provenance<Field>` à `null` et masque le badge.
- [ ] Sur chaque outil, `getPrefillCount()` statique et `prefillFromAi()` runtime en parité stricte (test Jest cas 0 / partiel / nominal).
- [ ] Une fixture IA multi-dates (date de décès `2025-03-01` ≠ date d'ouverture `2025-03-01` ≠ date de donation antérieure `2018-06-12` ≠ date de testament `2020-09-30`) remplit chaque champ avec la bonne date, aucune confusion (test backend — invariant cadrage §5.1.6).
- [ ] `coherenceAlerts` lève une alerte F-IA-03 si une date / un montant pré-rempli diverge de la saisie de l'avocat.
- [ ] Isolation workspace : non applicable côté pré-fill (donnée portée par la synthèse du dossier) — voir section dédiée.

---

## Périmètre

### Hors scope (explicite)

- Toute modification du scoring, des verdicts, des formules ou des bases juridiques des 8 outils F-FA-24 (logique métier inchangée).
- Le pré-remplissage des champs non factualisables de façon fiable par le LLM en V1 (libellés de biens, qualité individuelle de chaque héritier, attribution préférentielle, valeur de chaque lot) — restent en saisie manuelle (documenté, cadrage §5.6).
- L'outil `partage-judiciaire` (F-FA-17, indivision hors succession) — traité par SF-246-07.
- Tout outil Famille BE — hors périmètre vague 2 FR.
- Toute migration Liquibase — aucun nouvel outil, aucune table.

---

## Valeurs initiales

| Champ | Valeur initiale | Règle |
|-------|----------------|-------|
| `dateDecesDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `dateOuvertureSuccessionDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `modePartageDemandeDetecte` | `null` | `AMIABLE` / `JUDICIAIRE` ou `null` |
| `nombreCoheritiersDetecte` | `null` | entier `[0, 50]` ou `null` |
| `montantSuccessionEurDetecte` | `null` | montant `> 0` ou `null` |
| `montantLibsTotalEurDetecte` | `null` | montant `> 0` ou `null` |
| `nombreEnfantsSuccessionDetecte` | `null` | entier `[0, 50]` ou `null` |
| `dateDonationDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |
| `montantDonationsRecuesEurDetecte` | `null` | montant `> 0` ou `null` |
| `valeurDonationAuJourPartageEurDetectee` | `null` | montant `> 0` ou `null` |
| `actifBrutSuccessionEurDetecte` | `null` | montant `> 0` ou `null` |
| `passifSuccessionEurDetecte` | `null` | montant `> 0` ou `null` |
| `typeIndivisionSuccessoraleDetecte` | `null` | `LEGALE` / `CONVENTIONNELLE` ou `null` |
| `nbDescendantsDetecte` | `null` | entier `[0, 50]` ou `null` |
| `nbFreresSoeursDetecte` | `null` | entier `[0, 50]` ou `null` |
| `dateRedactionTestamentDetectee` | `null` | ISO `YYYY-MM-DD` strict ou `null` |

Comportements à la création : aucun — la SF étend un record de réponse IA.

---

## Contraintes de validation

| Champ | Obligatoire | Longueur max | Format / Valeurs autorisées | Unicité | Normalisation |
|-------|-------------|-------------|----------------------------|---------|---------------|
| dates `*Detectee` | Non | — | ISO `YYYY-MM-DD` ; rejet sinon → `null` | Non | `isoDateOrNull()` |
| montants `*Eur*` | Non | — | nombre `> 0` ; `≤ 0` ou aberrant → `null` | Non | `positiveDoubleOrNull()` |
| dénombrements `nombre*` / `nb*` | Non | — | entier `[0, 50]` ; hors plage → `null` | Non | `boundedIntOrNull(_, _, 0, 50)` |
| `modePartageDemandeDetecte` | Non | — | `AMIABLE` / `JUDICIAIRE` ; sinon → `null` | Non | `stringOrNull()` + whitelist |
| `typeIndivisionSuccessoraleDetecte` | Non | — | `LEGALE` / `CONVENTIONNELLE` ; sinon → `null` | Non | `stringOrNull()` + whitelist |

Notes :
- Tous les champs nullables — invariant cadrage §5.1.2 / §5.2.
- Format ISO strict cohérent avec `dateAcceptationPV` (F-239) et `dateConvocationEntretienDetectee` (SF-246-01).
- Montants en euros — invariant cadrage §5.2.

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/{tool}` (8 outils F-FA-24) | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/{tool}` (8 outils F-FA-24) | Oui | MEMBER |

> Endpoints **inchangés** (existants F-FA-24). La SF n'ajoute aucun endpoint : les champs IA transitent par la synthèse d'analyse (`familleExtractedData`).

### Contrat API figé (parallélisation back / front)

**Bloc JSON produit par le pipeline IA** (sous `analysis_result.famille_extracted_data`) :

```json
"succession_detection": {
  "date_deces": "2025-03-01",
  "date_ouverture_succession": "2025-03-01",
  "mode_partage_demande": "JUDICIAIRE",
  "nombre_coheritiers": 3,
  "montant_succession_eur": 420000.0,
  "montant_liberalites_total_eur": 60000.0,
  "nombre_enfants_succession": 2,
  "date_donation": "2018-06-12",
  "montant_donations_recues_eur": 30000.0,
  "valeur_donation_au_jour_partage_eur": 45000.0,
  "actif_brut_succession_eur": 480000.0,
  "passif_succession_eur": 60000.0,
  "type_indivision_successorale": "LEGALE",
  "nb_descendants": 2,
  "nb_freres_soeurs": 0,
  "date_redaction_testament": "2020-09-30"
}
```

**Record backend `FamilleExtractedData`** — 16 champs ajoutés (en fin de record, après `dateAcceptationPV`) :

```java
// SF-246-06 : 16 champs IA successions/libéralités pour pré-fill des 8 outils F-FA-24
// (Famille FR uniquement, nullables).
String dateDecesDetectee,
String dateOuvertureSuccessionDetectee,
String modePartageDemandeDetecte,
Integer nombreCoheritiersDetecte,
Double montantSuccessionEurDetecte,
Double montantLibsTotalEurDetecte,
Integer nombreEnfantsSuccessionDetecte,
String dateDonationDetectee,
Double montantDonationsRecuesEurDetecte,
Double valeurDonationAuJourPartageEurDetectee,
Double actifBrutSuccessionEurDetecte,
Double passifSuccessionEurDetecte,
String typeIndivisionSuccessoraleDetecte,
Integer nbDescendantsDetecte,
Integer nbFreresSoeursDetecte,
String dateRedactionTestamentDetectee
```

**DTO frontend `FamilleExtractedData`** (`divorce-accepte.model.ts`) — 16 champs ajoutés (types TS `string | null` / `number | null`).

**Helpers `*PrefillInput`** — chaque helper d'outil expose un `Pick<FamilleExtractedData, ...>` restreint aux champs qu'il consomme + `workspaceCountry`. `computePrefillCount(input)` retourne 0 si `workspaceCountry !== 'FRANCE'`.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `familleExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle `decision_tool_visibility_rules`.

### Composants Angular (si applicable)

- `PartageSuccessoralSectionComponent`, `ReserveHeriditaireSectionComponent`, `RapportSuccessionSectionComponent`, `AcceptationRenonciationSectionComponent`, `IndivisionSuccessoraleSectionComponent`, `DevolutionLegaleSectionComponent`, `DonationSectionComponent`, `TestamentValiditeSectionComponent` — `prefillFromAi()` rendu effectif, signaux `provenance<Field>`, handlers `onXxxChange()`, badges `auto_awesome`, extension `coherenceAlerts`.
- Les 8 helpers `*-section-prefill-rules.ts` correspondants — lecture de champs réels, `computePrefillCount()` recalculé.

---

## Plan de test

### Tests unitaires

- [ ] `extractFamilleData()` — cas nominal : `succession_detection` complet → 16 champs renseignés.
- [ ] `extractFamilleData()` — sous-objet absent → 16 champs `null`, pas d'exception.
- [ ] `extractFamilleData()` — date non ISO → champ `null` (fail-open).
- [ ] `extractFamilleData()` — montant ≤ 0 → `null` ; dénombrement hors `[0, 50]` → `null`.
- [ ] `extractFamilleData()` — `mode_partage_demande` / `type_indivision_successorale` hors whitelist → `null`.
- [ ] `LegalDomainPromptBuilderTest` — `FAMILLE_INSTRUCTION` contient les 16 clés `succession_detection` + la distinction date décès / ouverture / donation / testament.
- [ ] Par outil : `computePrefillCount()` cas (a) `aiData` vide → 0 ; (b) M champs partiels ; (c) N champs nominal.
- [ ] Par outil : `computePrefillCount()` `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] Par outil : `prefillFromAi()` cas nominal → champs renseignés, badges présents.
- [ ] Par outil : `prefillFromAi()` parité stricte avec `getPrefillCount()`.
- [ ] Par outil : `onXxxChange()` — modification manuelle remet `provenance<Field>` à `null`.
- [ ] Par outil : `coherenceAlerts` — alerte levée si valeur saisie diverge de la détection IA.

### Tests d'intégration

- [ ] Analyse IA d'un dossier famille FR fixture avec succession → la synthèse expose `succession_detection` peuplé.
- [ ] **Fixture multi-dates** (invariant cadrage §5.1.6) : dossier contenant date de décès, date d'ouverture, date de donation antérieure et date de testament distinctes → chaque champ rempli avec la bonne date, aucune confusion.
- [ ] **Fixture multi-montants** : actif brut, passif, montant succession, montant libéralités distincts → chaque montant dans le bon champ.
- [ ] Dossier famille BE → les 16 champs FR restent `null`.
- [ ] `GET /api/v1/case-files/{caseFileId}/{tool}` → 403 si workspace différent (non-régression sur les 8 endpoints).

### Isolation workspace

- [x] Applicable — vérifiée au niveau des 8 endpoints F-FA-24 existants (tests de non-régression conservés). Les champs IA n'introduisent aucun nouvel accès : ils transitent par la synthèse du dossier, déjà isolée par `caseFileId` + workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune préoccupation transversale structurelle** — la SF coche le déclencheur **« Outil décisionnel métier »** (modification de 8 outils décisionnels). Composants impactés ci-dessous.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Les 8 `*SectionComponent` F-FA-24 | `prefillFromAi()` devient effectif — risque de pré-remplir un champ à tort | Tests Jest pré-fill cas 0 / partiel / nominal par outil |
| `extractFamilleData()` | Tout consommateur de `FamilleExtractedData` reçoit 16 champs supplémentaires (additif, nullable — non cassant via builder F-234) | Tests d'extraction existants conservés verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » des 8 outils passe de 0 à N | Tests Jest `getPrefillCount` |
| Autres outils Famille FR consommant `familleExtractedData` (divorce, régimes…) | Aucun — champs additifs ignorés | Compilation TS + tests existants |

### Smoke tests E2E concernés

- [x] `e2e/smoke/case-analysis-flow.spec.ts` — flux d'analyse de dossier reste vert.
- [x] `e2e/smoke/happy-path.spec.ts` — parcours nominal inchangé.
- [x] `cd e2e && npm test` avant push (préoccupation transversale « outil décisionnel »).

---

## Dépendances

### Subfeatures bloquantes

- Aucune SF bloquante en amont. **Couplage de fichier** : SF-246-06 est la **première** SF à étendre `FamilleExtractedData` / `FAMILLE_INSTRUCTION` / `extractFamilleData()`. Les SF-246-07, 08, 09, 10, 11, 12 sont **séquentielles après elle** — voir « Notes et décisions ».

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Couplage de fichiers partagés — ordre de dev de la série Famille FR

Les SF de lot Famille FR (SF-246-06 à 11) et la SF Famille BE (SF-246-12) modifient toutes les **mêmes fichiers backend partagés** : record `FamilleExtractedData` (+ builder F-234), prompt `FAMILLE_INSTRUCTION`, méthode `extractFamilleData()`, DTO frontend `FamilleExtractedData` (`divorce-accepte.model.ts`).

**Décision** : les SF Famille sont **strictement séquentielles** sur ces 4 fichiers. Ordre de dev imposé :

```
SF-246-06 → SF-246-07 → SF-246-08 → SF-246-09 → SF-246-10 → SF-246-11 → SF-246-12
```

Chaque SF ajoute ses champs **en fin de record / de prompt / de builder**, après ceux de la SF précédente, et rebase sur master à jour. Ne **jamais** développer deux SF Famille sur deux branches simultanées modifiant `FamilleExtractedData` — conflit de rebase systématique.

**SF-246-03** (`divorce-faute`, codes faute) touche aussi `FamilleExtractedData` : elle s'insère dans la séquence en vague 4, après SF-246-12 (ou avant SF-246-12 — ordre interne vague 4 à figer dans sa propre mini-spec).

La parallélisation backend / frontend **intra-SF** reste autorisée (contrat API figé ci-dessus ; `divorce-accepte.model.ts` est la seule zone de contact, figée par le contrat).

### Décision SF de lot vs SF par outil

Découper outil par outil imposerait 8 modifications successives du même record et autant de conflits de rebase. La SF de lot livre les 16 champs source en une fois et tous les outils consommateurs ensemble — conforme à la décision de découpage du cadrage §3.1.
