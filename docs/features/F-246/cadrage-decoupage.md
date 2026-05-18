# F-246 — Complétion du pré-remplissage IA des outils décisionnels

## Cadrage et découpage en sous-features

> **Statut** : document de cadrage / découpage (en amont des mini-specs).
> **Source** : diagnostic 2026-05-18 (prépa démo Renversez) + addendum §8 de
> `docs/features/F-155/audit-prefill-ia-2026-04-24.md`.
> **Ce document ne produit aucune mini-spec individuelle ni aucun code.** Chaque
> SF listée ici passera par le cycle obligatoire complet (cadrage cohérence →
> cadrage écran → mini-spec → readiness → dev → review → push).

---

## 1. Constat et cause racine

Le pré-remplissage IA des outils décisionnels est **partiel à l'échelle du
produit**. Le pattern frontend canonique (`immigration-title-decision-section`,
helper `*-prefill-rules.ts`, signaux `provenance<Field>`, badges `auto_awesome`)
est en place sur ~103 outils, **mais il ne sert à rien quand le champ n'existe
ni dans le contrat de données backend ni dans le prompt LLM**.

Mécanique de la dette, vérifiée fichier par fichier :

1. Les records `*ExtractedData` de
   `backend/src/main/java/fr/ailegalcase/analysis/CaseAnalysisResponse.java` sont
   dominés par des **flags booléens de détection** (F-166/200/201/202/203/204/205
   — au total ~30 + 35 + 14 booléens) qui pilotent la **visibilité** des outils,
   pas leur **pré-remplissage**.
2. Très peu de champs **date / valeur** existent réellement :
   - `TravailExtractedData` : `dateEntree`, `dateLicenciement`,
     `salaireBrutMensuel`, `conventionCollective`, `motifLicenciement`,
     `typeContrat`, `poste`, `congesContractuels`,
     `primeAncienneteContractuelle`, + 5 champs SF-155-04
     (`motifNullitePressenti`, `origineInaptitudePressentie`,
     `avisMedecinTravailDate`, `reclassementRespecteDetected`,
     `heuresSupMentionneesDansDossier`).
   - `ImmigrationExtractedData` : `dateExpirationTitre`, `typeTitreSejour`,
     `dateDepotProcedure`, `dateNotificationDecisionContestee`, + champs
     SF-155-04 OQTF FR/BE.
   - `FamilleExtractedData` : **35 booléens + 1 seul champ string**
     (`dateAcceptationPV`, F-239). Aucun montant, aucune date hors PV
     d'acceptation, aucun âge.
3. Les helpers `*-prefill-rules.ts` du domaine Famille (et quelques-uns Travail /
   Immigration) **déclarent des types d'intersection** ajoutant des champs
   aspirationnels (`dateDecesDetectee`, `valeurCommunauteEurDetectee`,
   `nombreCoheritiersDetecte`, `dateSeparation`, `regimeMatrimonialDetecte`,
   `ageEnfants`, `dateRequeteOP`…) qui **n'existent dans aucun record backend**.
   `computePrefillCount()` lit donc des chemins toujours `undefined` →
   retourne `0` en pratique. Le `prefillFromAi()` du composant est un no-op
   structurel.
4. Deux outils l'assument explicitement par une constante
   `PREFILL_COUNT_ALWAYS_ZERO = true` (ex. `procedure-nullite-licenciement` —
   F-DT-36).

**Cause de fond** : dette de couplage backend ↔ frontend. Le garde-fou de
gouvernance réclamé par l'audit F-155 (§8.5) — section obligatoire « Champs IA à
extraire » dans `subfeature-template.md` + item bloquant readiness — n'avait
jamais été posé ; il l'est désormais (cf. ligne d'historique PRODUCT_SPEC
2026-05-18). **F-246 est la remédiation du déficit existant.**

### Périmètre F-246

- **DANS le périmètre** : les outils décisionnels qui ont des **champs date /
  valeur saisissables** et dont le pré-fill IA est **nul ou quasi-nul** parce
  que le champ source manque côté backend (record + prompt + extracteur).
- **HORS périmètre** :
  - les outils déjà correctement pré-remplis (cf. §2, tableau « Outils déjà
    couverts ») ;
  - les outils purement booléens / checklist dont le formulaire n'a pas de
    champ date / valeur saisissable (le pré-fill `0` y est un état nominal,
    pas une dette) ;
  - les flags de visibilité F-200/201/202/203/204/205 (infrastructure F-205,
    finalité distincte — pas de doublon) ;
  - toute refonte du pipeline IA, des formules de calcul ou des endpoints
    métier.

---

## 2. Liste exacte des outils concernés

Méthode : scan des 103 helpers `frontend/**/*-prefill-rules.ts` et des composants
`*-section`, croisé avec les champs réellement présents dans les records
`*ExtractedData` (backend + `case-analysis.model.ts` / `divorce-accepte.model.ts`).

Un outil est **« concerné »** s'il réunit les deux conditions :
(a) le formulaire a au moins un champ **date ou valeur** saisissable utile ;
(b) ce champ **n'a pas de source backend** (helper lisant un champ aspirationnel,
ou `PREFILL_COUNT_ALWAYS_ZERO`).

### 2.1 Outils concernés — **32 outils**

| # | Outil (`*-section`) | Feature | Domaine | Champs date/valeur non pré-remplis | Champs source manquants côté backend (record `*ExtractedData`) |
|---|---|---|---|---|---|
| 1 | `procedure-nullite-licenciement` | F-DT-36 | Travail FR | délai de convocation, motivation suffisante, autres flags procéduraux | `delaiConvocationNonRespecte`, `motivationLettreInsuffisante`, `entretienPrealableIrregulier`… (`TravailExtractedData`) |
| 2 | `non-concurrence` | F-DT-24 (**SF-DT-24-03**) | Travail FR | durée de la clause (mois), zone géographique, contrepartie financière (% ou €) | `nonConcurrenceDureeMois`, `nonConcurrenceZone`, `nonConcurrenceContrepartiePct` (`TravailExtractedData`) — `salaireBrutMensuel` déjà OK |
| 3 | `divorce-faute` | F-FA-09 | Famille FR | codes faute détectés | `fautesDetectees` (frontend stub `String[]`, **absent du record backend** `TravailExtractedData`/`FamilleExtractedData`) |
| 4 | `credit-temps-be` | F-DT-29 | Travail BE | âge du demandeur (années) | `ageDemandeurAnnees` (frontend stub, **absent du record backend**) |
| 5 | `partage-successoral` | F-FA-24 | Famille FR | mode de partage, nombre de cohéritiers, date de décès | `modePartageDemandeDetecte`, `nombreCoheritiersDetecte`, `dateDecesDetectee`, `dateOuvertureSuccessionDetectee` |
| 6 | `partage-judiciaire` | F-FA-17 | Famille FR | valeur des biens en indivision (€), nombre de coïndivisaires | `valeurBiensIndivisionEur`, `nombreCoindivisairesDetecte`, `pvDifficultesEtablisDetected`, `tentativeAmiableEpuiseueeDetected` |
| 7 | `reserve-heriditaire` | F-FA-24 | Famille FR | montant succession (€), montant libéralités (€), nombre d'enfants | `montantSuccessionEurDetecte`, `montantLibsTotalEurDetecte`, `nombreEnfantsSuccessionDetecte`, `dateOuvertureSuccessionDetectee`, `nbDescendantsDetecte` |
| 8 | `rapport-succession` | F-FA-24 | Famille FR | date donation, montant donations reçues (€), valeur au jour du partage (€) | `dateDonationDetectee`, `montantDonationsRecuesEurDetecte`, `valeurDonationAuJourPartageEurDetectee` |
| 9 | `acceptation-renonciation` | F-FA-24 | Famille FR | date d'ouverture succession, actif brut (€), passif (€) | `dateOuvertureSuccessionDetectee`, `actifBrutSuccessionEurDetecte`, `passifSuccessionEurDetecte` |
| 10 | `indivision-successorale` | F-FA-24 | Famille FR | date d'ouverture succession | `dateOuvertureSuccessionDetectee`, `typeIndivisionSuccessoraleDetecte` |
| 11 | `devolution-legale` | F-FA-24 | Famille FR | nombre de descendants, nombre de frères/sœurs | `nbDescendantsDetecte`, `nbFreresSoeursDetecte` |
| 12 | `donation` | F-FA-24 | Famille FR | date de la donation | `dateDonationDetectee` |
| 13 | `testament-validite` | F-FA-24 | Famille FR | date de rédaction du testament | `dateRedactionTestamentDetectee` |
| 14 | `communaute-universelle` | F-FA-16 | Famille FR | valeur de la communauté (€) | `valeurCommunauteEurDetectee` |
| 15 | `recompenses` | F-FA-15 | Famille FR | régime matrimonial | `regimeMatrimonialDetecte` |
| 16 | `pacs-dissolution` | F-FA-20 | Famille FR | date de conclusion du PACS | `dateConclusionPacs`, `regimeBiensPacsDetecte`, `modeDissolutionPacsDetecte` |
| 17 | `separation-corps` | F-FA-21 | Famille FR | date de séparation, patrimoine commun (€) | `dateSeparation`, `patrimoineCommun` |
| 18 | `indivision` | F-FA-22 | Famille FR | date de séparation | `dateSeparation` |
| 19 | `ordonnance-protection` | F-FA-14 | Famille FR | date de la requête OP | `dateRequeteOP` |
| 20 | `mesures-provisoires` | F-FA-12 | Famille FR | date d'audience AOMP, patrimoine commun | `dateAudienceAOMP`, `patrimoineCommunSignificatif` |
| 21 | `changement-etat-civil` | F-FA-26 | Famille FR | date de naissance du demandeur | `dateNaissanceDemandeurDetectee` |
| 22 | `autorite-parentale` | F-FA-19 | Famille FR | âge des enfants | `ageEnfants` |
| 23 | `changement-residence` | F-FA-19 | Famille FR | âge des enfants | `ageEnfants` |
| 24 | `desaccords-parentaux` | F-FA-19 | Famille FR | âge des enfants | `ageEnfants` |
| 25 | `calendrier-garde` | F-FA-19 | Famille FR | âge des enfants, dates de référence du calendrier | aucun champ aiData lu — `ageEnfants`, dates de garde absents |
| 26 | `revisions-post-divorce` | F-FA-13 | Famille FR | nombre d'enfants à charge, revenus annuels époux (€) | `nbEnfantsACharge`, `revenusAnnuelsEpoux` (record n'a que `dateAcceptationPV`) |
| 27 | `contestation-paternite` | F-FA-18 | Famille FR | date d'établissement filiation, date connaissance vérité, date majorité enfant | `dateEtablissementFiliationDetectee`, `dateConnaissanceVeriteDetectee`, `dateMajoriteEnfantDetectee` |
| 28 | `recherche-paternite` | F-FA-18 | Famille FR | date de naissance de l'enfant | `dateNaissanceEnfantRechercheDetectee` |
| 29 | `reconnaissance-paternelle` | F-FA-18 | Famille FR | date de naissance de l'enfant | `dateNaissanceEnfantDetectee` |
| 30 | `adoption` | F-FA-18 | Famille FR | âge de l'adoptant, âge de l'adopté | `ageAdoptantDetecte`, `ageAdopteDetecte` |
| 31 | `divorce-desunion-be` | F-FA (BE) | Famille BE | date de séparation | `dateSeparation` (BE) |
| 32 | `victime-violences-l4256` | F-DT (FR) | Travail FR | date de l'ordonnance de protection JAF | `dateOrdonnanceProtectionJaf` (`TravailExtractedData`) |

> **Note méthode** : `divorce-faute` (#3) et `credit-temps-be` (#4) ont leur
> champ source déclaré côté frontend (`fautesDetectees`, `ageDemandeurAnnees`)
> avec un commentaire explicite « no-op gracieux — pipeline IA branché
> ultérieurement ». Ils sont **dans le périmètre** : la SF doit brancher le
> backend.

### 2.2 Outils déjà couverts (HORS périmètre — pré-fill réel fonctionnel)

Pré-fill alimenté par des champs backend réels — **ne pas retoucher** :

- Travail FR : `anciennete`, `at-mp`, `conges-payes`, `contestation-are`,
  `documents-fin-contrat`, `harcelement-licenciement-nul`, `heures-sup`,
  `inaptitude`, `indemnite-comparatif`, `indemnite-preavis`,
  `indemnite-precarite-cdd`, `licenciement`, `licenciement-economique`,
  `licenciement-nul-detection`, `prudhome-fiche`, `pse`, `rappel-salaire`,
  `refere-prudhomal`, `requalification-cdd-cdi`, `requalification-interim-cdi`,
  `rupture-conv`, `rupture-conv-indemnite`, `transaction`, `travail-dissimule`,
  `protection-rp`, `tribunal-travail-fiche`, `discrimination`,
  `fin-mission-interim`.
- Travail BE : `motif-grave-be`, `avantages-conventionnels-be`.
- Immigration FR : `immigration-title-decision`, `immigration-work-right`,
  `changement-statut`, `aes-etudiant`, `aes-famille`, `aes-humanitaire`,
  `aes-metiers-tension`, `crrv-refus-visa`, `dublin-recours`, `jld-retention`,
  `oqtf-avec-delai`, `oqtf-sans-delai`, `belgian-9bis`, `belgian-40bis`.
- Immigration BE : `annexe13-be` (SF-155-04-C livrée).
- Famille FR : `divorce-accepte`, `divorce-alteration`, `divorce-checklist`
  (`dateAcceptationPV` F-239).
- Famille BE : `divorce-dc-be`.

### 2.3 Outils HORS périmètre car sans champ date/valeur saisissable

Pré-fill `0` nominal — formulaire à toggles / radios / checklist sans champ
date ou valeur libre (le `0` n'est **pas** une dette) :

- `autorite-parentale-be`, `contribution-alimentaire-enfants-be`,
  `contribution-conjoint-be`, `liquidation-partage-be`,
  `regime-communaute-legale-be` (outils BE Famille — checklist/critères) ;
- `naturalisation`, `mineurs-immigration`, `regime-algerien`,
  `referes-admin`, `asile-avance`, `mesures-eloignement`, `belgian-9ter`,
  `belgian-40ter`, `immigration-checklist`, `immigration-recours` ;
- `majeurs-proteges`, `mediation-familiale`, `ordonnance-requete`,
  `possession-etat`, `partage-immobilier` (`valeurImmeuble`/`capitalRestantDu`
  déjà branchés SF-155-20), `pma-gpa-bioethique`, `travail-procedure`.

> Ces outils peuvent **rejoindre F-246 ultérieurement** si une mini-spec
> identifie un champ date/valeur exploitable ; à ce stade ils ne sont pas
> retenus pour éviter le gadget.

---

## 3. Découpage en sous-features

**Principe** : 1 SF **full-stack par outil ou par lot homogène**. Chaque SF
livre l'ensemble vertical :

1. extension du record `*ExtractedData` concerné
   (`CaseAnalysisResponse.java`) — champs date / valeur ;
2. extension du prompt `LegalDomainPromptBuilder` (instruction du domaine +
   contrat JSON) ;
3. extension de l'extracteur / parsing (`extractTravailData()` /
   `extractImmigrationData()` / `extractFamilleData()`) + fixtures ;
4. extension du DTO frontend (`case-analysis.model.ts` /
   `divorce-accepte.model.ts`) — suppression du type d'intersection
   aspirationnel ;
5. implémentation réelle de `prefillFromAi()` dans le composant `*-section` ;
6. mise à niveau du helper `*-prefill-rules.ts` (lecture de champs réels) ;
7. signaux `provenance<Field>` + badges de provenance `auto_awesome` +
   remise à `null` au changement manuel + alertes `coherenceAlerts` (F-IA-03).

**Regroupement** : le domaine Famille concentre 27 des 32 outils, presque tous
adossés au **même record** `FamilleExtractedData` et au **même prompt** famille.
Les regrouper par **lot de sous-domaine** évite de modifier 27 fois le même
record/prompt et limite les conflits de merge. Chaque lot reste une SF
full-stack unique livrant tous ses outils ensemble.

### 3.1 Tableau des SF

| SF | Périmètre | Domaine | Outils couverts | Ampleur indicative |
|---|---|---|---|---|
| **SF-246-01** | Nullité de procédure de licenciement | Travail FR | `procedure-nullite-licenciement` (F-DT-36) | M (flags procéduraux nouveaux) |
| **SF-246-02** | Clause de non-concurrence (**= SF-DT-24-03**) | Travail FR | `non-concurrence` (F-DT-24) | M |
| **SF-246-03** | Divorce pour faute — codes faute | Famille FR | `divorce-faute` (F-FA-09) | S (champ déjà stubé frontend) |
| **SF-246-04** | Victime de violences L.1152-… (date OP JAF) | Travail FR | `victime-violences-l4256` | S |
| **SF-246-05** | Crédit-temps fin de carrière — âge demandeur | Travail BE | `credit-temps-be` (F-DT-29) | S |
| **SF-246-06** | Lot Successions / libéralités | Famille FR | `partage-successoral`, `reserve-heriditaire`, `rapport-succession`, `acceptation-renonciation`, `indivision-successorale`, `devolution-legale`, `donation`, `testament-validite` (F-FA-24) | L (8 outils, 1 record, 1 prompt) |
| **SF-246-07** | Lot Régimes matrimoniaux & liquidation FR | Famille FR | `communaute-universelle`, `recompenses`, `partage-judiciaire` (F-FA-15/16/17) | M (3 outils) |
| **SF-246-08** | Lot Séparation / indivision / PACS / protection | Famille FR | `pacs-dissolution`, `separation-corps`, `indivision`, `ordonnance-protection`, `mesures-provisoires`, `revisions-post-divorce` (F-FA-12/13/14/20/21/22) | L (6 outils) |
| **SF-246-09** | Lot Filiation / adoption | Famille FR | `contestation-paternite`, `recherche-paternite`, `reconnaissance-paternelle`, `adoption` (F-FA-18) | M (4 outils, champs date/âge) |
| **SF-246-10** | Lot Autorité parentale (âge des enfants) | Famille FR | `autorite-parentale`, `changement-residence`, `desaccords-parentaux`, `calendrier-garde` (F-FA-19) | M (4 outils, champ partagé `ageEnfants`) |
| **SF-246-11** | Changement d'état civil — date de naissance | Famille FR | `changement-etat-civil` (F-FA-26) | S |
| **SF-246-12** | Divorce pour désunion irrémédiable BE — date de séparation | Famille BE | `divorce-desunion-be` | S |

**Total : 12 SF couvrant les 32 outils.**

> `SF-246-02` **est** la SF anciennement nommée `SF-DT-24-03` (clause de
> non-concurrence). Elle est renumérotée dans le découpage F-246 pour la
> cohérence de la série ; la traçabilité avec F-DT-24 et l'origine prépa démo
> Renversez 2026-05-18 est conservée dans sa mini-spec.

> **Décision de découpage** : les lots Famille (06/07/08/09/10) regroupent
> plusieurs outils dans une SF unique car ils partagent le record
> `FamilleExtractedData` et le prompt famille — découper outil par outil
> imposerait 27 modifications successives du même record et autant de conflits
> de rebase. Chaque lot reste **une SF full-stack** : record + prompt +
> extracteur + DTO + tous les composants du lot livrés ensemble. Une SF de lot
> peut, si la mini-spec le justifie, être parallélisée backend/frontend (record
> figé d'abord).

---

## 4. Ordre des vagues

Priorisation : **droit du travail FR d'abord** (périmètre des démos en cours —
Renversez, Mengue), puis Famille FR (gros volume), puis BE.

| Vague | SF | Justification de priorité | Ampleur de la vague |
|---|---|---|---|
| **Vague 1 — Travail FR démos** | SF-246-01, SF-246-02, SF-246-04 | Outils cités dans les signaux démo 2026-05-18 (Renversez : conclusions + nullité procédure F-DT-36 ; clause de non-concurrence dossier Dupont). Périmètre des démos en cours. | 3 SF — ~M each, ~2-3 j |
| **Vague 2 — Famille FR successions** | SF-246-06, SF-246-07 | Successions/libéralités + régimes matrimoniaux : 11 outils, fort volume de dossiers Famille FR, gros gain de pré-fill par SF de lot. | 2 SF (lots) — ~L, ~3-4 j |
| **Vague 3 — Famille FR vie commune & filiation** | SF-246-08, SF-246-09, SF-246-10 | Séparation/PACS/protection + filiation + autorité parentale : 14 outils. | 3 SF (lots) — ~L, ~4-5 j |
| **Vague 4 — Reliquats FR + BE** | SF-246-03, SF-246-11, SF-246-05, SF-246-12 | Outils isolés à champ unique (faible ampleur), Travail BE et Famille BE en dernier (cohérent avec priorisation domaine). | 4 SF — ~S each, ~2 j |

**Ampleur globale F-246** : ~12 SF, ~11-14 jours-homme estimés, étalés sur 4
vagues. Avec parallélisation backend/frontend par SF (record figé d'abord), le
calendrier réel se compresse.

> **Audit de couverture intermédiaire** : conformément à la règle gouvernance
> « audit de couverture tous les 10 outils du bloc 2026-04-24 », refaire un
> point de couverture après la Vague 3 (≈ 25 outils traités) avant d'engager la
> Vague 4.

---

## 5. Points d'attention

### 5.1 Fiabilité d'extraction des dates par le LLM (point critique)

Plusieurs outils ont besoin de **dates précises** (date de décès, date
d'ouverture de succession, date de séparation, date de l'avis d'inaptitude…).
Risque structurel : **plusieurs dates coexistent dans des pièces distinctes**
d'un même dossier, et le LLM peut confondre.

Exemples de collision :
- succession : date du **décès** ≠ date d'**ouverture** de la succession (souvent
  identiques mais pas toujours) ≠ date de l'acte de notoriété ≠ date du
  testament ≠ date d'une donation antérieure.
- divorce : date du **mariage** ≠ date de **cessation de la vie commune** ≠ date
  de la **séparation effective** ≠ date de l'**ordonnance de non-conciliation**.
- filiation : date de **naissance de l'enfant** ≠ date d'**établissement de la
  filiation** ≠ date de **connaissance de la vérité** ≠ date de **majorité**.

Invariants à imposer dans chaque mini-spec concernée :

1. **Un champ = une définition juridique sans ambiguïté.** Le prompt nomme
   explicitement le concept (« date du décès du de cujus », pas « date »).
2. **Champ nullable + no-op gracieux.** Si le LLM n'identifie pas la date avec
   certitude, il renvoie `null` — jamais une date approximative. Mieux vaut un
   champ vide qu'un champ faux.
3. **Provenance + badge `auto_awesome` obligatoires.** L'avocat doit voir d'un
   coup d'œil que la valeur vient de l'analyse et garde la main pour corriger
   (remise `provenance = null` au changement manuel).
4. **Alerte de cohérence F-IA-03** si la date pré-remplie est incohérente avec
   une autre date du dossier (ex. date de séparation postérieure à la date de
   divorce) — exposée via `coherenceAlerts`.
5. **Format ISO `YYYY-MM-DD` strict** dans le contrat JSON ; rejet du parsing si
   format non conforme (cohérent avec le traitement existant
   `dateAcceptationPV` F-239 et `avisMedecinTravailDate` SF-155-04).
6. **Fixtures de test multi-dates.** Le plan de test de chaque SF doit inclure
   un dossier fixture contenant **au moins deux dates concurrentes** et vérifier
   que le bon champ est rempli (et l'autre laissé à `null`).

### 5.2 Montants et valeurs (€)

- Toujours préciser **brut / net** et la **devise** dans le prompt.
- Un montant non identifié de façon fiable → `null`, jamais `0`.
- Cohérence avec l'invariant « outils décisionnels = simulateurs indépendants »
  (mémoire projet) : le pré-fill n'impose pas une valeur, il propose ; pas
  d'override croisé entre outils.

### 5.3 Famille FR — record sous-dimensionné

`FamilleExtractedData` n'a aujourd'hui que **1 champ string** pour ~27 outils.
Les lots SF-246-06 à 10 vont l'élargir significativement. Points de vigilance :

- **Builder `FamilleExtractedData.Builder`** (F-234) : chaque nouveau champ doit
  passer par le builder, pas par un constructeur rétrocompat — l'ajout de champ
  reste donc non cassant.
- **Divergence frontend / backend déjà présente** : le modèle frontend
  `FamilleExtractedData` (`divorce-accepte.model.ts`) déclare déjà des champs
  (`dateSeparation`, `regimeMatrimonialDetecte`, `ageEnfants`, `dateRequeteOP`,
  `dureeMariageAnnees`, `revenusAnnuelsEpoux1Eur`…) **absents du record
  backend**. Chaque SF doit **réaligner** le DTO frontend sur le record backend
  réel (ne plus déclarer un champ tant que le backend ne le fournit pas) — sinon
  la dette se reproduit silencieusement.

### 5.4 Préoccupation transversale — outil décisionnel

Chaque SF coche le déclencheur transversal **« Outil décisionnel métier »**.
Obligations à reporter dans chaque mini-spec :

- liste explicite des composants impactés (le ou les `*-section` du lot) ;
- self-check grep pré-commit (mémoire projet — éviter régression silencieuse) ;
- vérification `TOOL_REGISTRY` frontend ↔ `decisional-tools-panel` ↔
  binding `inputs(ctx)` ;
- smoke tests E2E `cd e2e && npm test` avant push.

### 5.5 Garde-fou de gouvernance (déjà posé — à appliquer)

La section obligatoire **« Champs IA à extraire (pré-remplissage) »** de
`subfeature-template.md` et l'item bloquant readiness associé (mis en place le
2026-05-18) **doivent être renseignés** par chaque mini-spec SF-246-* : inventaire
des champs IA + confirmation explicite que l'extension du record `*ExtractedData`
**et** du prompt `LegalDomainPromptBuilder` est dans le périmètre de la SF.
F-246 est précisément la dette que ce garde-fou empêche de reproduire.

### 5.6 Pas de réduction de scope silencieuse

Les outils listés en §2.3 (sans champ date/valeur) sont **hors périmètre
assumé**, pas abandonnés silencieusement. Si une mini-spec de lot révèle un champ
date/valeur exploitable sur l'un d'eux, il est rattaché explicitement à F-246 par
une SF de rattrapage — jamais « déféré si besoin émerge ».

---

## 6. Synthèse

- **32 outils décisionnels** concernés (champs date/valeur saisissables + source
  backend manquante), sur ~103 outils au total — cohérent avec le diagnostic
  2026-05-18 (« ~32 sur ~103 »).
- **12 SF full-stack** : 5 SF mono-outil (Travail FR / BE, Famille BE) + 5 SF de
  lot Famille FR + 2 SF reliquats — couvrant les 32 outils.
- `SF-DT-24-03` (clause de non-concurrence) intégrée au découpage sous le numéro
  **SF-246-02**.
- **4 vagues** : Vague 1 Travail FR (démos) → Vague 2 Famille FR successions →
  Vague 3 Famille FR vie commune & filiation → Vague 4 reliquats FR + BE.
- Point d'attention dominant : **fiabilité d'extraction des dates** quand
  plusieurs dates coexistent — invariants nullable / no-op gracieux / provenance /
  alerte F-IA-03 / fixtures multi-dates à imposer dans chaque mini-spec.
