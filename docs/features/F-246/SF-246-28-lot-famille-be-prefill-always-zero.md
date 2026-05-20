# Mini-spec — SF-246-28 / F-246 — Lot Famille BE — levée PREFILL_COUNT_ALWAYS_ZERO

> Template basé sur `project-governance/templates/subfeature-template.md`.
> Modèle de référence : SF-246-27 (protection & divorce, commit f262aff2).

---

## Identifiant

`F-246 / SF-246-28`

## Feature parente

`F-246` — Complétion du pré-remplissage IA des outils décisionnels

## Statut

`in-progress`

## Date de création

2026-05-20

## Branche Git

`feat/SF-246-28-lot-famille-be`

---

## Objectif

Lever la constante `PREFILL_COUNT_ALWAYS_ZERO = true` sur les 5 outils décisionnels
Famille BELGIQUE (`autorite-parentale-be`, `contribution-alimentaire-enfants-be`,
`contribution-conjoint-be`, `liquidation-partage-be`, `regime-communaute-legale-be`)
en branchant le pipeline IA complet (record backend + prompt + extracteur + DTO frontend +
`prefillFromAi()` réels) pour les champs date/valeur extractibles identifiés par l'audit
SF-246-14 §8 (15 champs au total).

Conformément à l'invariant F-246 (2026-05-19) : *tout champ saisissable d'un outil
décisionnel doit être pré-rempli par l'IA ; la seule exception admise est que
l'information soit absente des documents uploadés.* Le motif `PREFILL_COUNT_ALWAYS_ZERO`
n'est plus recevable dès qu'il existe des dates/valeurs extractibles côté pièces.

---

## Tableau outil → champs extractibles (audit SF-246-14 §8)

| Outil | Champs IA à brancher | Type | Sémantique juridique BE | Info absente (exception admise) |
|---|---|---|---|---|
| `autorite-parentale-be` | `modeHebergementPrincipal` | string (whitelist) | Mode d'hébergement principal des enfants — lisible d'une convention de garde ou d'un jugement BE (art. 374 CC BE) | filiation, accord parental, désintérêt, mise en danger, incapacité, décision judiciaire antérieure = appréciation de l'avocat |
| `contribution-alimentaire-enfants-be` | `nombreEnfants`, `revenuMensuelParent1`, `revenuMensuelParent2`, `allocationsFamilialesMensuelles`, `nuitsHebergementParent1`, `nuitsHebergementParent2` | int/double/double/double/int/int | Nombre d'enfants communs (jugement / convention) ; revenus nets mensuels des deux parents (fiches de paie BE) ; allocations familiales mensuelles (paiement FAMIFED) ; nuits d'hébergement par mois selon convention ou jugement | `trancheAgeEnfants`, `coutMensuelGlobal`, `fraisExtraordinaires`, `commentaire` = paramètre de simulation / saisie manuelle |
| `contribution-conjoint-be` | `dureeMariageAnnees`, `revenuMensuelCreancier`, `revenuMensuelDebiteur` | int/double/double | Durée du mariage en années entières (acte de mariage BE / jugement) ; revenus nets mensuels des ex-époux (fiches de paie BE, art. 301 § 3 CC BE) | type de divorce, renonciation conventionnelle, état de besoin, faute grave, dégradation économique = appréciation |
| `liquidation-partage-be` | `dateDesignationNotaire`, `dateOuvertureOperations`, `dateNotificationProjet`, `dateHomologation` | string ISO | Date de désignation du notaire (ordonnance / jugement — CJ art. 1207 BE) ; date d'ouverture des opérations de liquidation ; date de notification du projet de liquidation-partage (délai de contredits — CJ art. 1218 BE) ; date d'homologation par le tribunal | notaire désigné, opérations, inventaire, projet, contredits, PV, homologation = constat de procédure (vrai/faux renseigné par l'avocat) |
| `regime-communaute-legale-be` | `dateMariage`, `contratMariageSigne` | string ISO / boolean | Date du mariage (acte de mariage BE — CC art. 3.18+ ) ; contrat de mariage signé (notaire — CC art. 1.2.59+ : régime conventionnel vs légal) | listes biens/dettes = saisie ligne à ligne par l'avocat |

**Total : 15 champs extractibles → 14 nouveaux champs backend (+ 1 booléen `contratMariageSigne`).**

---

## Comportement attendu

### Cas nominal — Backend

1. `CaseAnalysisResponse.FamilleExtractedData` reçoit un nouveau sous-objet
   `famille_be_detection_v2` (via le Builder F-234) avec les 15 champs :
   - `modeHebergementPrincipalBeDetecte` (string, whitelist 3 valeurs — **BELGIQUE UNIQUEMENT**)
   - `nombreEnfantsBeDetecte` (Integer — **BELGIQUE UNIQUEMENT**)
   - `revenuMensuelParent1BeDetecte` (Double — **BELGIQUE UNIQUEMENT**)
   - `revenuMensuelParent2BeDetecte` (Double — **BELGIQUE UNIQUEMENT**)
   - `allocationsFamiliales MensuellesBeDetectees` (Double — **BELGIQUE UNIQUEMENT**)
   - `nuitsHebergementParent1BeDetectees` (Integer — **BELGIQUE UNIQUEMENT**)
   - `nuitsHebergementParent2BeDetectees` (Integer — **BELGIQUE UNIQUEMENT**)
   - `dureeMariageAnneesBeDetectee` (Integer — **BELGIQUE UNIQUEMENT**)
   - `revenuMensuelCreancierBeDetecte` (Double — **BELGIQUE UNIQUEMENT**)
   - `revenuMensuelDebiteurBeDetecte` (Double — **BELGIQUE UNIQUEMENT**)
   - `dateDesignationNotaireBeDetectee` (String ISO — **BELGIQUE UNIQUEMENT**)
   - `dateOuvertureOperationsBeDetectee` (String ISO — **BELGIQUE UNIQUEMENT**)
   - `dateNotificationProjetBeDetectee` (String ISO — **BELGIQUE UNIQUEMENT**)
   - `dateHomologationBeDetectee` (String ISO — **BELGIQUE UNIQUEMENT**)
   - `dateMariageBeDetectee` (String ISO — **BELGIQUE UNIQUEMENT**)
   - `contratMariageSigneBeDetecte` (Boolean — **BELGIQUE UNIQUEMENT**)

2. `LegalDomainPromptBuilder.FAMILLE_INSTRUCTION_P2` : ajout du sous-objet
   `famille_be_detection_v2` avec whitelists fermées, sémantique juridique BE nommée
   sans ambiguïté, annotation BELGIQUE UNIQUEMENT, dates ISO strict.

3. `extractFamilleData()` : parsing du sous-objet `famille_be_detection_v2` +
   guard null + injection via Builder + helpers `isoDateOrNull()` / `boundedIntOrNull()`
   / `nonNegativeDoubleOrNull()` / `whitelistedOrNull()` / `booleanOrNull()`.

### Cas nominal — Frontend

4. `divorce-accepte.model.ts` (`FamilleExtractedData`) : ajout des 16 champs avec
   JSDoc BELGIQUE UNIQUEMENT.
5. `autorite-parentale-be-section-prefill-rules.ts` : `computeModeHebergement()`
   lisant `aiData.modeHebergementPrincipalBeDetecte` ; suppression `PREFILL_COUNT_ALWAYS_ZERO`.
6. `autorite-parentale-be-section.component.ts` : `prefillFromAi()` branché +
   `PREFILL_COUNT_ALWAYS_ZERO` → supprimé (ou `false`) ; signal
   `provenanceModeHebergement` + badge `auto_awesome` + remise à null au changement
   manuel.
7. `contribution-alimentaire-enfants-be-section-prefill-rules.ts` : 6 `compute*()`
   lisant les 6 champs réels ; suppression `PREFILL_COUNT_ALWAYS_ZERO`.
8. `contribution-alimentaire-enfants-be-section.component.ts` : `prefillFromAi()`
   branché + 6 signaux de provenance ; `PREFILL_COUNT_ALWAYS_ZERO` supprimé.
9. `contribution-conjoint-be-section-prefill-rules.ts` : 3 `compute*()` ; suppression
   `PREFILL_COUNT_ALWAYS_ZERO`.
10. `contribution-conjoint-be-section.component.ts` : `prefillFromAi()` branché + 3
    signaux de provenance ; `PREFILL_COUNT_ALWAYS_ZERO` supprimé.
11. `liquidation-partage-be-section-prefill-rules.ts` : 4 `compute*()` ; suppression
    `PREFILL_COUNT_ALWAYS_ZERO`.
12. `liquidation-partage-be-section.component.ts` : `prefillFromAi()` branché pour les
    4 dates + 4 signaux de provenance ; `PREFILL_COUNT_ALWAYS_ZERO` supprimé.
13. `regime-communaute-legale-be-section-prefill-rules.ts` : 2 `compute*()` ; suppression
    `PREFILL_COUNT_ALWAYS_ZERO`.
14. `regime-communaute-legale-be-section.component.ts` : `prefillFromAi()` branché pour
    `dateMariage` + `contratMariageSigne` + 2 signaux de provenance ;
    `PREFILL_COUNT_ALWAYS_ZERO` supprimé.
15. Pour les 5 composants : reset de la provenance au changement manuel (signal → `null`)
    + alertes F-IA-03 si déjà existantes (liquidation-partage-be a déjà F-IA-03 sur
    `DATE_NOTIFICATION_PROJET` ; regime-communaute-legale-be sur `DATE_MARIAGE` — les
    alertes existantes consomment désormais la valeur pré-remplie au lieu d'être en
    no-op).

### Cas d'erreur

| Situation | Comportement attendu |
|---|---|
| Sous-objet `famille_be_detection_v2` absent du JSON IA | `extractFamilleData()` : guard null → tous les champs BE à null. No-op gracieux — aucune exception. |
| Date renvoyée dans un format non ISO (`jj/mm/aaaa`) | `isoDateOrNull()` retourne null — le champ n'est pas pré-rempli. |
| Montant négatif pour un revenu | `nonNegativeDoubleOrNull()` retourne null — le champ reste vide. |
| Code mode hébergement hors whitelist | `whitelistedOrNull()` retourne null — le select reste à la valeur par défaut. |
| `nombre d'enfants` > 12 (hors plage formulaire) | `boundedIntOrNull(0, 12)` retourne null — le champ reste à 1. |
| Sous-objet `famille_be_detection_v2` présent mais dossier FRANCE | Le prompt impose null pour tous les champs BELGIQUE UNIQUEMENT si le dossier n'est pas belge — extracteur garde le null. |
| `aiData` non fourni au composant | Guard null dans `prefillFromAi()` — no-op, aucune exception. |

---

## Critères d'acceptation

1. **Backend** : `CaseAnalysisResponseTest` inclut 5 nouveaux tests (CA-X) vérifiant :
   - parsing nominal d'un fixture BE multi-champs (contient au moins 2 dates concurrentes
     pour `liquidation-partage-be` — délibérément) ;
   - parsing avec sous-objet absent → tous les champs BE null ;
   - parsing avec date non ISO → champ null ;
   - parsing avec montant négatif → null ;
   - parsing avec code hors whitelist (modeHebergement) → null.

2. **Frontend** : chaque helper `*-prefill-rules.spec.ts` couvert :
   - nominal : aiData complet → `computePrefillCount()` > 0 ;
   - aiData null → count 0 ;
   - codes hors whitelist → 0.
   Total : ≥ 5 × 4 = 20 nouveaux tests Jest.

3. **Levée PREFILL_COUNT_ALWAYS_ZERO** : self-check grep confirme que la constante
   `PREFILL_COUNT_ALWAYS_ZERO = true` n'existe plus dans aucun des 5 helpers ni
   composants.

4. **Signaux de provenance** : pour chaque champ pré-rempli, l'UI affiche le badge
   `auto_awesome` ; le badge disparaît si l'avocat modifie la valeur manuellement.

5. **No-op gracieux** : si `aiData` est null ou si le sous-objet `famille_be_detection_v2`
   est absent, aucune erreur de runtime — le formulaire reste à ses valeurs par défaut.

6. **Smoke E2E** : `cd e2e && npm test` — les ~27 FAIL préexistants restent stables ;
   aucun FAIL nouveau introduit par SF-246-28.

---

## Plan de test

### Backend (JUnit)

- `CaseAnalysisResponseTest` — fixture JSON BE (`famille_be_detection_v2`) :
  - CA-1 : parsing nominal complet 16 champs.
  - CA-2 : sous-objet absent → tous null.
  - CA-3 : date non ISO → null.
  - CA-4 : montant négatif → null.
  - CA-5 : code hors whitelist (`modeHebergement`) → null.
  - CA-6 : fixture multi-dates pour `liquidation-partage-be` (dates chevauchantes) →
    bon champ rempli, autre null.

### Frontend (Jest)

Par helper (× 5 helpers) :
- Test A : `computePrefillCount()` nominal → > 0.
- Test B : `aiData` null → 0.
- Test C : sous-objet `famille_be_detection_v2` null → 0.
- Test D : code hors whitelist → 0 (helper modeHebergement).

### Isolation workspace

- Les champs `*BeDetecte` ne sont jamais exposés sur un dossier FRANCE — le prompt
  contraint le LLM à laisser `famille_be_detection_v2` à null pour un dossier FR.

---

## Champs IA à extraire (pré-remplissage)

| Outil | Champ formulaire | Champ `FamilleExtractedData` (backend) | Type | Sous-objet JSON IA |
|---|---|---|---|---|
| `autorite-parentale-be` | `modeHebergementPrincipal` | `modeHebergementPrincipalBeDetecte` | String whitelist | `famille_be_detection_v2.mode_hebergement_principal_be` |
| `contribution-alimentaire-enfants-be` | `nombreEnfants` | `nombreEnfantsBeDetecte` | Integer [1,12] | `famille_be_detection_v2.nombre_enfants_be` |
| `contribution-alimentaire-enfants-be` | `revenuMensuelParent1` | `revenuMensuelParent1BeDetecte` | Double ≥ 0 | `famille_be_detection_v2.revenu_mensuel_parent1_be` |
| `contribution-alimentaire-enfants-be` | `revenuMensuelParent2` | `revenuMensuelParent2BeDetecte` | Double ≥ 0 | `famille_be_detection_v2.revenu_mensuel_parent2_be` |
| `contribution-alimentaire-enfants-be` | `allocationsFamilialesMensuelles` | `allocationsFamiliales MensuellesBeDetectees` | Double ≥ 0 | `famille_be_detection_v2.allocations_familiales_be` |
| `contribution-alimentaire-enfants-be` | `nuitsHebergementParent1` | `nuitsHebergementParent1BeDetectees` | Integer [0,30] | `famille_be_detection_v2.nuits_hebergement_parent1_be` |
| `contribution-alimentaire-enfants-be` | `nuitsHebergementParent2` | `nuitsHebergementParent2BeDetectees` | Integer [0,30] | `famille_be_detection_v2.nuits_hebergement_parent2_be` |
| `contribution-conjoint-be` | `dureeMariageAnnees` | `dureeMariageAnneesBeDetectee` | Integer [0,80] | `famille_be_detection_v2.duree_mariage_annees_be` |
| `contribution-conjoint-be` | `revenuMensuelCreancier` | `revenuMensuelCreancierBeDetecte` | Double ≥ 0 | `famille_be_detection_v2.revenu_mensuel_creancier_be` |
| `contribution-conjoint-be` | `revenuMensuelDebiteur` | `revenuMensuelDebiteurBeDetecte` | Double ≥ 0 | `famille_be_detection_v2.revenu_mensuel_debiteur_be` |
| `liquidation-partage-be` | `dateDesignationNotaire` | `dateDesignationNotaireBeDetectee` | String ISO | `famille_be_detection_v2.date_designation_notaire_be` |
| `liquidation-partage-be` | `dateOuvertureOperations` | `dateOuvertureOperationsBeDetectee` | String ISO | `famille_be_detection_v2.date_ouverture_operations_be` |
| `liquidation-partage-be` | `dateNotificationProjet` | `dateNotificationProjetBeDetectee` | String ISO | `famille_be_detection_v2.date_notification_projet_be` |
| `liquidation-partage-be` | `dateHomologation` | `dateHomologationBeDetectee` | String ISO | `famille_be_detection_v2.date_homologation_be` |
| `regime-communaute-legale-be` | `dateMariage` | `dateMariageBeDetectee` | String ISO | `famille_be_detection_v2.date_mariage_be` |
| `regime-communaute-legale-be` | `contratMariageSigne` | `contratMariageSigneBeDetecte` | Boolean | `famille_be_detection_v2.contrat_mariage_signe_be` |

Extension backend (record + prompt) : **dans le périmètre de la SF-246-28** (unique SF full-stack).

---

## Tables / endpoints / composants impactés

### Backend
- `CaseAnalysisResponse.java` — `FamilleExtractedData` record + Builder : 16 nouveaux champs.
- `LegalDomainPromptBuilder.java` — `FAMILLE_INSTRUCTION_P2` : ajout section `famille_be_detection_v2`.
- `CaseAnalysisResponse.java` — `extractFamilleData()` : parsing du sous-objet BE.
- `CaseAnalysisResponseTest.java` — 6 nouveaux tests CA.

### Frontend (5 composants `*-section` + 5 helpers + 5 specs)
- `frontend/src/app/core/models/divorce-accepte.model.ts` — `FamilleExtractedData` : 16 nouveaux champs.
- `autorite-parentale-be-section/autorite-parentale-be-section-prefill-rules.ts` + `.spec.ts`
- `autorite-parentale-be-section/autorite-parentale-be-section.component.ts`
- `contribution-alimentaire-enfants-be-section/contribution-alimentaire-enfants-be-section-prefill-rules.ts` + `.spec.ts`
- `contribution-alimentaire-enfants-be-section/contribution-alimentaire-enfants-be-section.component.ts`
- `contribution-conjoint-be-section/contribution-conjoint-be-section-prefill-rules.ts` + `.spec.ts`
- `contribution-conjoint-be-section/contribution-conjoint-be-section.component.ts`
- `liquidation-partage-be-section/liquidation-partage-be-section-prefill-rules.ts` + `.spec.ts`
- `liquidation-partage-be-section/liquidation-partage-be-section.component.ts`
- `regime-communaute-legale-be-section/regime-communaute-legale-be-section-prefill-rules.ts` + `.spec.ts`
- `regime-communaute-legale-be-section/regime-communaute-legale-be-section.component.ts`

### Pas de migration Liquibase (champs purement IA — pas de table à modifier).

---

## Hors périmètre

- Les champs déclarés « info structurellement absente » dans l'audit (appréciation de l'avocat,
  paramètres de simulation) : `trancheAgeEnfants`, `coutMensuelGlobalEnfants`, `fraisExtraordinairesMensuels`,
  `commentaire`, `typeDivorce`, `renonciationPensionConvention`, `creancierEnEtatDeBesoin`, etc.
- Les listes de biens et de dettes de `regime-communaute-legale-be` (saisie ligne à ligne).
- Les flags booléens de visibilité F-202 (déjà en place).
- Toute refonte du pipeline IA, des formules de calcul ou des endpoints métier.
- Les outils déjà couverts listés en §2.2 du cadrage.
- L'ajout d'alertes F-IA-03 nouvelles (hors `dateNotificationProjet` et `dateMariage`
  déjà câblées dans les composants existants — les pré-remplissages viendront alimenter
  ces alertes existantes automatiquement).

---

## Analyse d'impact — préoccupations transversales

| Préoccupation | Applicable ? | Action |
|---|---|---|
| Auth / Principal | Non — aucun changement d'authentification | — |
| Workspace context | Oui (indirectement) — les champs BE portent le guard pays `BELGIQUE UNIQUEMENT` dans le prompt ; le composant vérifie déjà `workspaceCountry === 'BELGIQUE'` | Guard déjà en place dans les 5 composants — vérifier qu'il n'est pas court-circuité par le pré-fill |
| Plans / limites | Non | — |
| Navigation / routing | Non | — |
| Outil décisionnel métier | Oui — 5 outils décisionnels modifiés | Liste explicite : `autorite-parentale-be`, `contribution-alimentaire-enfants-be`, `contribution-conjoint-be`, `liquidation-partage-be`, `regime-communaute-legale-be` |

**Self-check grep pré-commit obligatoire** :
```
grep -r "PREFILL_COUNT_ALWAYS_ZERO" frontend/src/app/case-files/autorite-parentale-be-section/
grep -r "PREFILL_COUNT_ALWAYS_ZERO" frontend/src/app/case-files/contribution-alimentaire-enfants-be-section/
grep -r "PREFILL_COUNT_ALWAYS_ZERO" frontend/src/app/case-files/contribution-conjoint-be-section/
grep -r "PREFILL_COUNT_ALWAYS_ZERO" frontend/src/app/case-files/liquidation-partage-be-section/
grep -r "PREFILL_COUNT_ALWAYS_ZERO" frontend/src/app/case-files/regime-communaute-legale-be-section/
```
→ Ces 5 greps doivent tous retourner 0 lignes après le dev.

**Smoke tests E2E** : `cd e2e && npm test` — stabilité des ~27 FAIL préexistants.

---

## Justification invariant 2026-05-19

La mémoire projet `feedback_decision_tools_all_fields_prefilled.md` formalise la décision
PO du 2026-05-19 : *« Invariant : tout champ saisissable d'un outil décisionnel doit être
pré-rempli par l'IA ; seule exception = info absente des documents uploadés. »*

L'audit SF-246-14 §8 démontre que les 5 outils Famille BE marqués `PREFILL_COUNT_ALWAYS_ZERO`
ont chacun des dates/montants extractibles des pièces uploadées (actes de mariage, fiches de
paie, jugements, ordonnances) — ils ne peuvent donc pas invoquer l'exception « info absente ».
La levée de `PREFILL_COUNT_ALWAYS_ZERO` est donc obligatoire sous le bar actuel.
