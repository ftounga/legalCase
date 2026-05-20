# SF-246-29 — Pré-remplissage IA exhaustif de l'outil F-DT-38 (rupture de période d'essai)

> Mini-spec produite à partir de `project-governance/templates/subfeature-template.md`.
> Pattern de référence : `docs/features/F-246/SF-246-13-prefill-non-concurrence.md`
> (clause de non-concurrence — sous-objet `clause_non_concurrence_detail`)
> et `docs/features/F-206/SF-206-05/06/07/08-*.md` (sous-objet `prise_acte_detail` /
> `resiliation_judiciaire_detail` — 11 / 12 champs miroir).

---

## Identifiant

`F-246 / SF-246-29`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`ready`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-29-prefill-dt38`

---

## Objectif

> En une phrase : que fait cette subfeature ?

Compléter le pré-remplissage IA de l'outil F-DT-38 (rupture de période d'essai, Travail FR)
en branchant les **14 champs spécifiques période d'essai** restés non pré-remplis par la
livraison SF-DT-38-02 — extension du record `TravailExtractedData` (sous-objet
`rupturePeriodeEssaiDetail`), du prompt `LegalDomainPromptBuilder.TRAVAIL_INSTRUCTION`
(sous-objet `rupture_periode_essai_detail`), de l'extracteur `extractTravailData()`, du
DTO frontend, du helper de pré-fill et du composant — afin que `prefillFromAi()` couvre
désormais **les 23 champs saisissables** du formulaire (invariant F-246 « tous les
champs »).

---

## Comportement attendu

### Cas nominal

1. L'avocat lance l'analyse IA d'un dossier de droit du travail FR contenant le contrat
   de travail + la lettre de rupture pendant période d'essai.
2. Le pipeline IA (prompt `TRAVAIL_INSTRUCTION`) émet le sous-objet
   `rupture_periode_essai_detail` avec 14 clés (cf. tableau d'audit § « Champs IA à
   extraire »).
3. L'extracteur `extractTravailData()` parse le sous-objet en 14 champs typés du record
   `TravailExtractedData` (préfixe `rpe…` ou `rupturePeriodeEssai…`).
4. Le DTO frontend `TravailExtractedData` expose les 14 champs ; le `TOOL_REGISTRY`
   passe déjà `aiData: ctx.synthesis?.travailExtractedData` — aucun changement de
   binding.
5. À l'ouverture du composant `rupture-periode-essai-section`, `prefillFromAi()` renseigne
   désormais les 14 nouveaux champs en plus des 9 existants — un badge `auto_awesome` par
   champ pré-rempli.
6. L'avocat peut modifier toute valeur : les handlers existants remettent les signaux
   `provenance<Field>` à `null` (badge masqué).
7. Le badge « Pré-rempli par l'IA (N champs) » du panel F-IA-04 reflète
   `getPrefillCount()` recalculé sur 23 champs.

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Sous-objet `rupture_periode_essai_detail` absent (dossier sans contrat ni lettre, dossier BE) | Les 14 champs → `null` ; pré-fill no-op gracieux sur ces 14 champs | n/a |
| Date non ISO (`14/03/2026`) | `isoDateOrNull()` → `null` (fail-open) | n/a |
| Enum hors whitelist (ex. `categorie_socio_professionnelle = "STAGIAIRE"`) | `normalizeEnumCode()` → `null` ; pas de pré-fill du select | n/a |
| Entier hors plage (ex. `duree_periode_essai_contractuelle_mois = 99`) | `boundedIntOrNull()` → `null` | n/a |
| Dossier travail BE | Prompt impose `null` ; les 14 champs FR restent `null` ; section non rendue (visibility FR-only) | n/a |
| LLM renvoie une chaîne pour un booléen | `booleanOrNull()` → `null` | n/a |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : la rupture de période d'essai est **propre à F-DT-38**.
  Les 14 champs sont des concepts spécifiques (catégorie socio-pro L.1221-19, durée
  d'essai contractuelle, renouvellement L.1221-23, délai de prévenance L.1221-25,
  motif lié aux compétences, motifs avérés, CCN plus favorable, etc.). Aucun
  recouvrement avec F-DT-08 / F-DT-36 / F-DT-39 / F-DT-40 (rupture / nullité /
  prise d'acte / résiliation judiciaire — outils distincts au sens « un outil = une
  situation métier »).
- [x] **Autres pays** : France uniquement. La rupture de période d'essai BE n'existe pas
  (statut unique 2014 — pas de période d'essai en droit belge depuis la loi du
  26/12/2013). Champs `null` pour la BE — le prompt l'impose.
- [x] **Autres domaines** : non applicable — concept propre au droit du travail.
- [x] **Autres UI patterns** : pré-remplissage IA (pattern canonique), badges
  `auto_awesome`. Pas d'alerte F-IA-03 ajoutée (les 3 alertes existantes
  `DATE_RUPTURE` / `MOTIF_PROFESSIONNEL` / `DUREE_ESSAI` sont conservées, inchangées).
- [x] **Autres flows transversaux** : aucun (auth / workspace / navigation inchangés).

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript** : `TravailExtractedData` dans `case-analysis.model.ts`.
- [x] **Record / DTO backend** : `CaseAnalysisResponse.TravailExtractedData` + builder F-234.
- [x] **Service / logique métier** : `extractTravailData()`.
- [x] **Entité JPA + schéma DB** : non applicable — `travailExtractedData` est sérialisé
  dans la synthèse IA. La table `rupture_periode_essai_analyses` reste inchangée
  (inputs persistés par le calculator, indépendamment du pré-fill IA).
- [x] **Tests existants** :
  - `rupture-periode-essai-section-prefill-rules.spec.ts` — étendu (9 → 23 champs).
  - `rupture-periode-essai-section.component.spec.ts` — vérification badges + handlers.
  - `CaseAnalysisResponseTest` — sérialisation du nouveau sous-objet.
  - `LegalDomainPromptBuilderTest` — présence des 14 nouvelles clés dans le prompt.

### Cas spécifique : nouvelle feature d'outil décisionnel

- [x] **Cohérence IA (F-IA-03)** : **pas d'alerte ajoutée pour les 14 nouveaux champs**.
  Les 3 alertes F-IA-03 existantes (`DATE_RUPTURE`, `MOTIF_PROFESSIONNEL`, `DUREE_ESSAI`)
  couvrent déjà les axes critiques (date critique, motif, durée). Les 14 champs ajoutés
  sont soit des sous-éléments (catégorie, dérogation CCN, accord écrit), soit des
  identifiants booléens (présence lettre, motifs avérés), pour lesquels une notion
  d'« écart » n'a pas de sens. Le pré-fill IA + badge `auto_awesome` (provenance) couvre
  la traçabilité. Décision documentée — pas une dette masquée.
- [x] **Refresh dashboard (F-IA-02)** : inchangé — `triggerRefresh()` déjà dans le `next`
  du POST `RupturePeriodeEssaiService.calculate()`.
- [x] **Pré-remplissage IA** : objet de la SF — pré-fill étendu de 9 à 23 champs.
- [x] **Persistance des inputs** : inchangée — inputs persistés via l'endpoint F-DT-38
  existant.
- [x] **Masquage conditionnel selon type** : inchangé — visibilité F-IA-04 de F-DT-38
  déjà gérée (FR + workspaceCountry).
- [x] **Alertes actives après calcul** : `coherenceAlerts` inchangé.

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — aucun nouveau composant partagé, service ou endpoint. La SF étend un
record, un prompt, un extracteur et un helper existants (pattern miroir
SF-246-13 / SF-206-05 / SF-206-07).

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-38 (rupture période d'essai) | Oui | Intégré dans cette SF |
| Alertes F-IA-03 supplémentaires | Non | Pas de notion d'écart sur les 14 nouveaux champs (justifié ci-dessus) |
| Rupture d'essai BE | Non | Mécanisme inexistant en droit belge (statut unique 2014) |
| Autres outils Travail FR | Non | Concepts strictement propres à la période d'essai |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable aux autres cibles (justification explicite ci-dessus)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [ ] **Non applicable** — la SF étend la partie pré-fill d'un composant décisionnel
  existant (`rupture-periode-essai-section`).

### 1. Cohérence visuelle

- [x] Palette statut / Datepicker / Typographie / Gate `workspaceCountry` / Erreurs /
  Refresh dashboard — tous inchangés.

### 2. Pré-fill IA (OBLIGATOIRE)

- [x] `@Input() aiData?: TravailExtractedData | null` — déjà typé strictement.
- [x] `prefillFromAi()` invoqué dans `ngOnInit()` ET `ngOnChanges()` — déjà le cas.
- [x] Signaux `provenance<Field>` : ajout de 14 signaux côté composant (1 par champ
  pré-rempli — voir tableau d'audit).
- [x] Badge `auto_awesome` par champ pré-rempli (template HTML étendu).
- [x] Handlers de modification existants étendus pour reset des nouveaux signaux
  de provenance.

### 3. Validation F-IA-03 (OBLIGATOIRE)

- [x] `coherenceAlerts` inchangé (3 alertes existantes conservées).

### 4. TOOL_REGISTRY symétrique + `getPrefillCount(input)`

- [x] Entrée `F-DT-38-rupture-periode-essai` déjà présente dans `TOOL_REGISTRY` ;
  `inputs(ctx)` passe déjà `aiData` — aucun changement de binding.
- [x] Static `getPrefillCount(input)` du composant : délègue au helper —
  `RupturePeriodeEssaiSectionPrefillRules.computePrefillCount()` recalculé sur 23 champs.
- [x] Parité stricte `getPrefillCount()` ↔ `prefillFromAi()` : mêmes guards, mêmes
  mappings, même condition `workspaceCountry === 'FRANCE'`.
- [x] Tests Jest : (a) 0 champ, (b) M champs partiels, (c) 23 champs cas nominal.
- [x] `tool_id` `F-DT-38-rupture-periode-essai` déjà dans `KNOWN_FRONTEND_TOOL_IDS` —
  pas de migration `decision_tool_visibility_rules`.

### 5. Parité des domaines métier (niveau ≥ 5)

- [x] Niveau du tool : **5** (qualification 4 niveaux + indemnité fourchette).

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail FR | Oui (F-DT-38) | C'est l'outil de cette SF |
| Droit du travail BE | **Non applicable** — la période d'essai n'existe plus en droit belge depuis 2014 (statut unique loi 26/12/2013) | Sans objet |
| Immigration / Famille | Non | Concept propre au droit du travail |

> La SF complète le pré-fill d'un outil existant — la parité de domaine de F-DT-38 a été
> tranchée à sa création.

---

## Champs IA à extraire (pré-remplissage) — Tableau d'audit

> **Audit complet** des 23 champs du formulaire `RupturePeriodeEssaiRequest`
> (référence : `frontend/src/app/core/models/rupture-periode-essai.model.ts` lignes
> 59-83). 9 champs déjà pré-remplis par SF-DT-38-02 ; 14 champs à brancher par cette SF.

| # | Champ formulaire | Type | Source actuelle (SF-DT-38-02 helper) | Verdict | Champ IA cible (clé prompt → champ record) | Source documentaire attendue |
|---|---|---|---|---|---|---|
| 1 | `categorieSocioProfessionnelle` | enum (OUVRIER_EMPLOYE / AGENT_MAITRISE_TECHNICIEN / CADRE) | aucun | **à brancher** | `rupture_periode_essai_detail.categorie_socio_professionnelle` → `rpeCategorieSocioProfessionnelle` (String, whitelist) | Contrat de travail (qualification, classification CCN, coefficient) |
| 2 | `typeContrat` | enum (CDI / CDD / INTERIM) | `aiData.typeContrat` | **déjà OK** | (existant) | (existant) |
| 3 | `dureeCddMois` | Integer | aucun | **à brancher** | `rupture_periode_essai_detail.duree_cdd_mois` → `rpeDureeCddMois` (Integer borné [0, 36]) | Contrat CDD (durée à terme précis ou imprécis) |
| 4 | `dateDebutContrat` | LocalDate ISO | `aiData.dateEntree` | **déjà OK** | (existant) | (existant) |
| 5 | `dateRupture` | LocalDate ISO | `aiData.dateLicenciement` | **déjà OK** | (existant) | (existant) |
| 6 | `dureePeriodeEssaiContractuelleMois` | Integer | aucun | **à brancher** | `rupture_periode_essai_detail.duree_periode_essai_mois` → `rpeDureePeriodeEssaiMois` (Integer borné [0, 24]) | Contrat de travail (clause « période d'essai de X mois ») |
| 7 | `renouvellementInvoque` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.renouvellement_invoque` → `rpeRenouvellementInvoque` (Boolean) | Lettre / avenant de renouvellement de la période d'essai |
| 8 | `accordBrancheRenouvellement` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.accord_branche_renouvellement` → `rpeAccordBrancheRenouvellement` (Boolean) | CCN applicable / accord de branche |
| 9 | `accordEcritSalarieRenouvellement` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.accord_ecrit_salarie_renouvellement` → `rpeAccordEcritSalarieRenouvellement` (Boolean) | Avenant signé par le salarié (jamais tacite — L.1221-23) |
| 10 | `auteurRupture` | enum (EMPLOYEUR / SALARIE) | aucun | **à brancher** | `rupture_periode_essai_detail.auteur_rupture` → `rpeAuteurRupture` (String, whitelist) | Lettre de rupture (en-tête : qui notifie ?) |
| 11 | `delaiPrevenanceJoursAppliques` | Integer | aucun | **à brancher** | `rupture_periode_essai_detail.delai_prevenance_jours_appliques` → `rpeDelaiPrevenanceJours` (Integer borné [0, 30]) | Lettre de rupture (date envoi vs date d'effet) |
| 12 | `motifInvoque` | String | `aiData.motifLicenciement` | **déjà OK** | (existant) | (existant) |
| 13 | `motifLieAuxCompetencesProfessionnelles` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.motif_lie_competences_professionnelles` → `rpeMotifLieCompetences` (Boolean) | Lettre de rupture (le motif est-il rattaché à l'évaluation des aptitudes professionnelles ?) |
| 14 | `motifEconomiqueOuOrganisationnel` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.motif_economique_ou_organisationnel` → `rpeMotifEconomique` (Boolean) | Lettre de rupture (motif détourné — économique / réorganisation) |
| 15 | `discriminationInvoquee` | enum L.1132-1 | mapping `motifNullitePressenti` | **déjà OK** | (existant) | (existant) |
| 16 | `grossesseAuMomentRupture` | Boolean | mapping `motifNullitePressenti = MATERNITE_PATERNITE` | **déjà OK** | (existant) | (existant) |
| 17 | `arretAccidentTravailEnCours` | Boolean | `aiData.atMpDetecte` | **déjà OK** | (existant) | (existant) |
| 18 | `atteinteLiberteFondamentale` | String (texte libre) | aucun | **à brancher** | `rupture_periode_essai_detail.atteinte_liberte_fondamentale` → `rpeAtteinteLiberteFondamentale` (String tronqué ≤ 500 car.) | Lettre de rupture ou pièces du dossier (atteinte à une liberté fondamentale documentée) |
| 19 | `lettreRuptureMotivee` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.lettre_rupture_motivee` → `rpeLettreRuptureMotivee` (Boolean) | Lettre de rupture (présence et contenu motivé) |
| 20 | `motifsAveresParPieces` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.motifs_averes_par_pieces` → `rpeMotifsAveresParPieces` (Boolean) | Évaluations, rapports d'incidents, courriers (motifs adossés à des pièces datées) |
| 21 | `conventionCollectiveApplicable` | Boolean | `aiData.conventionCollective != null` | **déjà OK** | (existant) | (existant) |
| 22 | `conventionCollectivePlusFavorableRespectee` | Boolean | aucun | **à brancher** | `rupture_periode_essai_detail.ccn_plus_favorable_respectee` → `rpeCcnPlusFavorableRespectee` (Boolean) | CCN applicable + lettre de rupture (le préavis ou la durée d'essai conventionnelle est-elle respectée ?) |
| 23 | `salaireMensuelBrut` | Double | `aiData.salaireBrutMensuel` | **déjà OK** | (existant) | (existant) |

**Bilan** : 9 champs déjà pré-remplis, **14 champs à brancher** par cette SF. Aucun
champ exclu — l'invariant F-246 « tous les champs » est atteint.

> Note : la mini-spec préliminaire (au lendemain de F-DT-38) mentionnait « 22 champs / 13
> à brancher ». L'audit définitif retient **23 champs / 14 à brancher** — le
> `motifEconomiqueOuOrganisationnel` (#14) et l'`atteinteLiberteFondamentale` (#18)
> étaient comptés ensemble dans l'estimation initiale.

---

## Critères d'acceptation

- [ ] Le record `TravailExtractedData` contient **14 nouveaux champs nullables** préfixés
  `rpe…`, tous propagés par le builder F-234 (déclaration, champ Builder, setter,
  `toBuilder()`, `build()`).
- [ ] Le prompt `TRAVAIL_INSTRUCTION` (split en PART1/PART2) enrichit le sous-objet
  `rupture_periode_essai_detail` avec **14 clés** : `categorie_socio_professionnelle`,
  `duree_cdd_mois`, `duree_periode_essai_mois`, `renouvellement_invoque`,
  `accord_branche_renouvellement`, `accord_ecrit_salarie_renouvellement`,
  `auteur_rupture`, `delai_prevenance_jours_appliques`,
  `motif_lie_competences_professionnelles`, `motif_economique_ou_organisationnel`,
  `atteinte_liberte_fondamentale`, `lettre_rupture_motivee`,
  `motifs_averes_par_pieces`, `ccn_plus_favorable_respectee`. Chaque clé inclut une
  définition juridique sans ambiguïté + instruction `null` hors FR / hors certitude.
- [ ] Le sous-objet est placé en **PART2** du prompt (vérifié sous la limite JVM
  65 535 octets — actuellement PART2 = 53 437, marge 12 KB).
- [ ] `extractTravailData()` parse ces 14 clés du sous-objet
  `rupture_periode_essai_detail` :
  - 2 enums via `normalizeEnumCode()` avec whitelists fermées
    (`CATEGORIE_SOCIO_PROFESSIONNELLE_CODES`, `AUTEUR_RUPTURE_CODES`).
  - 3 entiers via `boundedIntOrNull()` ([0, 36] pour CDD, [0, 24] pour essai, [0, 30]
    pour prévenance).
  - 8 booléens via `booleanOrNull()`.
  - 1 texte tronqué via `truncatedTextOrNull()` (≤ 500 car).
  - Les 14 champs sont `null` si le sous-objet est absent (`hasRpe == false`).
- [ ] Le DTO frontend `TravailExtractedData` (`case-analysis.model.ts`) expose les 14
  champs avec les bons types TS.
- [ ] Le helper `RupturePeriodeEssaiSectionPrefillRules` expose **14 nouvelles fonctions
  `compute…`** (1 par champ) + un `computePrefillCount()` qui compte jusqu'à 23.
- [ ] `prefillFromAi()` du composant `RupturePeriodeEssaiSectionComponent` renseigne les
  14 nouveaux champs (FR uniquement) avec leurs signaux `provenance` ; chaque handler
  de modification existant met le signal `provenance` à `null`.
- [ ] Chaque champ pré-rempli affiche un badge `auto_awesome` dans le template HTML.
- [ ] `getPrefillCount()` statique et `prefillFromAi()` runtime sont en parité stricte
  (test Jest cas 0 / partiel / nominal 23 champs).
- [ ] Fixtures backend :
  - sous-objet `rupture_periode_essai_detail` complet → 14 champs renseignés (test).
  - sous-objet absent → 14 champs `null` (test).
  - `categorie_socio_professionnelle = "STAGIAIRE"` (hors whitelist) → `null`.
  - `duree_periode_essai_mois = 50` (hors borne) → `null`.
- [ ] Builder propagation : le `toBuilder()` et le `build()` couvrent les 14 nouveaux
  champs (`BuilderPatternEnforcementIT` reste vert).
- [ ] Test `LegalDomainPromptBuilderTest` vérifie la présence des 14 clés dans le prompt.
- [ ] **Aucune régression** : `CritereCodeIntegrityIT`, `DecisionToolVisibilityIntegrityIT`,
  `DashboardTileToolIdIntegrityIT`, `BuilderPatternEnforcementIT` restent verts.
- [ ] Isolation workspace : non applicable côté pré-fill (donnée portée par la synthèse
  du dossier, déjà isolée).

---

## Périmètre

### Hors scope (explicite)

- Toute modification de la logique du Calculator `RupturePeriodeEssaiCalculator`,
  des 4 verdicts (REGULIERE / RISQUE_ABUSIVE / NULLE / ILLEGALE), des bases juridiques
  ou de la fourchette d'indemnité.
- Toute modification de la table `rupture_periode_essai_analyses` (inputs persistés
  via le DTO `RupturePeriodeEssaiRequest` inchangé).
- Toute alerte F-IA-03 supplémentaire (3 alertes existantes conservées, pas de notion
  d'écart sur les nouveaux champs).
- Le jumeau BE F-DT-39 (mécanisme inexistant en droit belge).
- Toute migration Liquibase — aucun nouvel outil, aucune table, aucune règle
  `decision_tool_visibility_rules`.
- Modification du seed F-DT-38 (déjà appliqué migration 256).

---

## Valeurs initiales

| Champ record | Valeur initiale | Règle |
|-------|----------------|-------|
| `rpeCategorieSocioProfessionnelle` | `null` | Code parmi `OUVRIER_EMPLOYE / AGENT_MAITRISE_TECHNICIEN / CADRE` ou `null` |
| `rpeDureeCddMois` | `null` | Entier ∈ [0, 36] ou `null` |
| `rpeDureePeriodeEssaiMois` | `null` | Entier ∈ [0, 24] ou `null` |
| `rpeRenouvellementInvoque` | `null` | Boolean ou `null` |
| `rpeAccordBrancheRenouvellement` | `null` | Boolean ou `null` |
| `rpeAccordEcritSalarieRenouvellement` | `null` | Boolean ou `null` |
| `rpeAuteurRupture` | `null` | Code parmi `EMPLOYEUR / SALARIE` ou `null` |
| `rpeDelaiPrevenanceJours` | `null` | Entier ∈ [0, 30] ou `null` |
| `rpeMotifLieCompetences` | `null` | Boolean ou `null` |
| `rpeMotifEconomique` | `null` | Boolean ou `null` |
| `rpeAtteinteLiberteFondamentale` | `null` | String ≤ 500 car. ou `null` |
| `rpeLettreRuptureMotivee` | `null` | Boolean ou `null` |
| `rpeMotifsAveresParPieces` | `null` | Boolean ou `null` |
| `rpeCcnPlusFavorableRespectee` | `null` | Boolean ou `null` |

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs autorisées | Normalisation |
|-------|-------------|----------------------------|---------------|
| `rpeCategorieSocioProfessionnelle` | Non | Whitelist 3 codes ; hors liste → `null` | `normalizeEnumCode()` |
| `rpeDureeCddMois` | Non | Entier ∈ [0, 36] ; hors borne → `null` | `boundedIntOrNull()` |
| `rpeDureePeriodeEssaiMois` | Non | Entier ∈ [0, 24] ; hors borne → `null` | `boundedIntOrNull()` |
| `rpeRenouvellementInvoque`, `rpeAccordBrancheRenouvellement`, `rpeAccordEcritSalarieRenouvellement`, `rpeMotifLieCompetences`, `rpeMotifEconomique`, `rpeLettreRuptureMotivee`, `rpeMotifsAveresParPieces`, `rpeCcnPlusFavorableRespectee` | Non | Boolean tri-état (true / false / null) | `booleanOrNull()` |
| `rpeAuteurRupture` | Non | Whitelist 2 codes ; hors liste → `null` | `normalizeEnumCode()` |
| `rpeDelaiPrevenanceJours` | Non | Entier ∈ [0, 30] ; hors borne → `null` | `boundedIntOrNull()` |
| `rpeAtteinteLiberteFondamentale` | Non | String ≤ 500 caractères | `truncatedTextOrNull()` |

Notes :
- L'invariant F-246 — une valeur non identifiée de façon fiable reste `null`, jamais une
  valeur par défaut — est appliqué partout.
- Les enums n'ont pas de valeur fallback : `AUTRE` n'est pas utilisé pour signifier
  « inconnu ».

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth | Rôle minimum |
|---------|-----|------|-------------|
| POST | `/api/v1/case-files/{caseFileId}/rupture-periode-essai` | Oui | LAWYER |
| GET | `/api/v1/case-files/{caseFileId}/rupture-periode-essai` | Oui | MEMBER |

> Endpoints **inchangés** (existants SF-DT-38-01). La SF n'ajoute aucun endpoint :
> les champs IA transitent par la synthèse d'analyse (`travailExtractedData`).

### Contrat API figé

**Bloc JSON produit par le pipeline IA** (sous
`analysis_result.travail_extracted_data.rupture_periode_essai_detail`) :

```json
"rupture_periode_essai_detail": {
  "categorie_socio_professionnelle": "CADRE",
  "duree_cdd_mois": null,
  "duree_periode_essai_mois": 4,
  "renouvellement_invoque": false,
  "accord_branche_renouvellement": null,
  "accord_ecrit_salarie_renouvellement": null,
  "auteur_rupture": "EMPLOYEUR",
  "delai_prevenance_jours_appliques": 14,
  "motif_lie_competences_professionnelles": true,
  "motif_economique_ou_organisationnel": false,
  "atteinte_liberte_fondamentale": null,
  "lettre_rupture_motivee": true,
  "motifs_averes_par_pieces": true,
  "ccn_plus_favorable_respectee": true
}
```

**Record backend `TravailExtractedData`** — 14 champs ajoutés en fin de record (après
`offreOutplacementMentionnee`), regroupés sous le commentaire `SF-246-29` :

```java
// SF-246-29 : 14 champs IA pour pré-fill exhaustif F-DT-38 (rupture de
// période d'essai, Travail FR uniquement, nullables). Sous-objet
// `rupture_periode_essai_detail`. La période d'essai est un mécanisme
// franco-français — ces champs restent null pour la BE (statut unique
// 2014, loi 26/12/2013 abolissant la clause d'essai BE).
String rpeCategorieSocioProfessionnelle,
Integer rpeDureeCddMois,
Integer rpeDureePeriodeEssaiMois,
Boolean rpeRenouvellementInvoque,
Boolean rpeAccordBrancheRenouvellement,
Boolean rpeAccordEcritSalarieRenouvellement,
String rpeAuteurRupture,
Integer rpeDelaiPrevenanceJours,
Boolean rpeMotifLieCompetences,
Boolean rpeMotifEconomique,
String rpeAtteinteLiberteFondamentale,
Boolean rpeLettreRuptureMotivee,
Boolean rpeMotifsAveresParPieces,
Boolean rpeCcnPlusFavorableRespectee
```

**DTO frontend `TravailExtractedData`** (`case-analysis.model.ts`) — 14 champs ajoutés
au record.

**Helper `RupturePeriodeEssaiPrefillInput`** — `Pick<TravailExtractedData, ...>` étendu
avec les 14 nouveaux champs.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| — | — | Aucune table impactée. `travailExtractedData` sérialisé dans le JSON de synthèse |

### Migration Liquibase

- [ ] Oui
- [x] Non applicable — aucun nouvel outil, aucune table, aucune règle
  `decision_tool_visibility_rules`.

### Composants Angular

- `RupturePeriodeEssaiSectionComponent` — `prefillFromAi()` étendu, 14 signaux
  `provenance*` ajoutés, 14 badges `auto_awesome` dans le template HTML, handlers
  étendus.
- `rupture-periode-essai-section-prefill-rules.ts` — 14 nouvelles fonctions
  `compute…` ; `computePrefillCount()` recalculé sur 23 champs.

---

## Plan de test

### Tests unitaires (backend)

- [ ] `extractTravailData()` cas nominal : sous-objet `rupture_periode_essai_detail`
  complet → 14 champs `rpe*` renseignés correctement.
- [ ] `extractTravailData()` sous-objet absent → 14 champs `null`, pas d'exception.
- [ ] `extractTravailData()` `categorie_socio_professionnelle = "STAGIAIRE"` (hors
  whitelist) → `rpeCategorieSocioProfessionnelle = null`.
- [ ] `extractTravailData()` `auteur_rupture = "INTERIM"` (hors whitelist) → `null`.
- [ ] `extractTravailData()` `duree_periode_essai_mois = 50` (hors borne) → `null`.
- [ ] `extractTravailData()` `delai_prevenance_jours_appliques = -1` (hors borne) →
  `null`.
- [ ] `extractTravailData()` `atteinte_liberte_fondamentale` trop long → tronqué à
  500 car.
- [ ] `LegalDomainPromptBuilderTest` : `TRAVAIL_INSTRUCTION` contient les 14 clés
  attendues dans le sous-objet `rupture_periode_essai_detail` + les whitelists des 2
  enums (3 codes + 2 codes).
- [ ] `CaseAnalysisResponseTest` : sérialisation / désérialisation des 14 champs en
  snake_case.

### Tests unitaires (frontend / Jest)

- [ ] `computePrefillCount()` cas (a) `aiData` vide → 0 ; cas (b) partiel → compte
  intermédiaire ; cas (c) nominal complet → 23.
- [ ] `computePrefillCount()` `workspaceCountry = 'BELGIQUE'` → 0.
- [ ] 14 nouvelles fonctions `compute*` : null hors FR, valeur OK en FR, null si champ
  source absent / hors whitelist.
- [ ] `prefillFromAi()` du composant : badges `auto_awesome` apparaissent pour les 14
  nouveaux champs ; modification manuelle → reset du signal `provenance` correspondant.

### Tests d'intégration

- [ ] Builder F-234 — `BuilderPatternEnforcementIT` reste vert (couvre les 14 nouveaux
  champs automatiquement via détection paramètre / setter / build).
- [ ] `CritereCodeIntegrityIT`, `DecisionToolVisibilityIntegrityIT`,
  `DashboardTileToolIdIntegrityIT` — verts (la SF n'ajoute aucun critereCode,
  aucun outil, aucune tile).

### Isolation workspace

- [x] Applicable — vérifiée au niveau de l'endpoint F-DT-38 existant (test de
  non-régression conservé). Les champs IA n'introduisent aucun nouvel accès : ils
  transitent par la synthèse du dossier, déjà isolée par `caseFileId` + workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Outil décisionnel métier** — F-DT-38 = outil décisionnel existant, modification
  du pré-fill IA. Composants impactés listés ci-dessous.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| `RupturePeriodeEssaiSectionComponent` | `prefillFromAi()` étendu — risque de pré-remplir un champ à tort | Tests Jest 0 / partiel / nominal |
| `extractTravailData()` | Tout consommateur de `TravailExtractedData` reçoit 14 champs supplémentaires (additif, nullable, non cassant via builder F-234) | Tests d'extraction existants verts |
| `decisional-tools-panel` | Badge « Pré-rempli par l'IA (N) » de F-DT-38 passe de ≤ 9 à ≤ 23 | Test Jest `getPrefillCount` |
| Autres outils Travail FR consommant `travailExtractedData` | Aucun — champs additifs ignorés | Compilation TS + tests existants |
| Calculator `RupturePeriodeEssaiCalculator` | Inchangé | Tests UT existants conservés |
| `RupturePeriodeEssaiAnalysesRepository` / table | Inchangée | Tests IT existants |

### Smoke tests E2E concernés

- [x] La SF étend uniquement un record IA, un prompt, un extracteur, un helper de
  pré-fill et un composant. Aucune route, aucun guard, aucun endpoint modifié.
  Préoccupation transversale = « outil décisionnel » (pas auth / workspace /
  navigation) : les smoke tests E2E ne sont pas un blocage de push pour cette SF.

---

## Dépendances

### Subfeatures bloquantes

- **F-DT-38 (SF-DT-38-01 + SF-DT-38-02)** — mergée PR #1135 (2026-05-20). Pré-requis
  livré.
- Aucune autre SF F-246 ne modifie `TravailExtractedData` en parallèle (F-246 est
  rouverte spécifiquement pour cette SF unique).

### Questions ouvertes impactées

- [x] Aucune question de `docs/OPEN_QUESTIONS.md` touchée.

---

## Notes et décisions

### Décision product owner 2026-05-20 — réouverture F-246 (option 1)

Plutôt que créer une nouvelle feature F-25X, on rouvre F-246 (statut Terminée la veille)
pour absorber la SF de complément de pré-fill IA de F-DT-38. Cohérent avec le scope
F-246 « tous les outils décisionnels ». Statut F-246 : Terminée → En cours — 28/29 SF
(redeviendra Terminée à la fin de cette SF).

### Saturation prompt — sous-objet en PART2

Le prompt `TRAVAIL_INSTRUCTION` est split en PART1 (46 494 octets) + PART2 (53 437
octets) pour rester sous la limite JVM 65 535 octets par String literal. Le nouveau
sous-objet `rupture_periode_essai_detail` (≈ 4-5 KB) est ajouté **en PART2** (marge
disponible 12 098 octets). Aucune bascule entre PART1 et PART2 n'est nécessaire.

### Pas d'alerte F-IA-03 supplémentaire

Les 3 alertes F-IA-03 existantes de SF-DT-38-02 (`DATE_RUPTURE`, `MOTIF_PROFESSIONNEL`,
`DUREE_ESSAI`) couvrent déjà les axes critiques. Les 14 nouveaux champs sont soit
booléens (présence / qualité), soit enums fermés, soit entiers bornés — pour lesquels
une notion d'« écart » F-IA-03 n'a pas de sens. Le badge `auto_awesome` couvre la
traçabilité ; sa disparition au premier changement manuel signale à l'avocat qu'il
s'écarte de l'IA. Décision documentée — pas une omission.

### Préfixe `rpe…` côté record

Pour éviter toute collision avec les champs existants (`grossesseAuMomentRupture`,
`atMpDetecte`, etc.), les 14 nouveaux champs portent le préfixe `rpe…`
(`rupturePeriodeEssai…` aurait été trop long et alourdirait le builder). Convention
documentée dans la mini-spec.

### Pas de mapping vers les champs existants

Une approche alternative aurait été de tenter des heuristiques de pré-fill depuis les
champs existants (ex. `categorieSocioProfessionnelle` déduite de `conventionCollective`).
Rejetée : le prompt LLM est plus fiable que des règles de mapping fragiles (cf.
diagnostic 2026-05-18 §8). On laisse le LLM extraire directement les 14 champs et on
retombe sur `null` en cas de doute (invariant F-246).
