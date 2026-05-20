# SF-216-07 — ARIPA recouvrement pension alimentaire impayée — backend

## Objectif

Outil décisionnel `F-FA-ARIPA-RECOUVREMENT` : guide l'avocat dans la démarche de recouvrement de pension alimentaire impayée via l'ARIPA (Agence de Recouvrement des Impayés de Pensions Alimentaires), détermine la voie de recouvrement la plus rapide (SDR direct CAF / saisie sur salaire / saisie administrative à tiers détenteur) et calcule les droits à l'Allocation de Soutien Familial (ASF).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/aripa-recouvrement`.
- Body :
  - `montantPensionMensuelleEur` (int, requis)
  - `nombreMoisImpayes` (int, requis)
  - `situationCreancier` (SALARIE | CHOMEUR | INDEPENDANT | SANS_ACTIVITE)
  - `situationDebiteur` (SALARIE | CHOMEUR | INDEPENDANT | SANS_ACTIVITE | INCONNU)
  - `titreExecutoire` (boolean, requis) — décision de justice / convention CM / acte notarié
  - `debiteurEnFrance` (boolean, requis)
  - `nombreEnfantsACharge` (int, requis)
- Calculator `AripaRecouvrementCalculator` :
  - Si `!titreExecutoire` → verdict `TITRE_REQUIS` (obtenir d'abord un titre exécutoire).
  - Si `titreExecutoire` + `debiteurEnFrance` :
    - **SDR ARIPA direct** : créancier éligible si enfant mineur + pension fixée + impayé ≥ 1 mois.
    - **Montant ASF** : si pension < seuil ASF (art. L. 523-1 CSS, actualisé) → complément CAF.
    - **Saisie sur salaire** (si débiteur salarié) : procédure simplifiée art. L. 581-3 CSS.
    - **Saisie administrative à tiers détenteur** (SATD) : si débiteur salarié employeur public.
  - Si `debiteurEnFrance = false` → verdict `CONVENTION_INTERNATIONALE` (Convention La Haye 2007 ou Règlement UE 4/2009).
- Retourne : `voieRecommandee`, `montantASFEligible`, `delaiEstime`, `etapes`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400 (ARIPA = dispositif FR uniquement).
- `legalDomain ≠ DROIT_FAMILLE` → 400.
- `nombreMoisImpayes < 1` → 400.
- Workspace mismatch → 404.

## Source juridique

- **art. L. 581-1 à L. 581-14 CSS** — recouvrement pension alimentaire.
- **art. L. 582-1 CSS** — saisie sur rémunération.
- **art. L. 523-1 à L. 523-6 CSS** — Allocation de Soutien Familial (ASF).
- **Ordonnance n°2002-1358 du 20/11/2002** — ARIPA.
- **Convention La Haye 23/11/2007** — recouvrement international des aliments.
- **Règlement UE n°4/2009 du 18/12/2008** — obligations alimentaires transfrontières.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `vie_commune_detection.revenusAnnuelsEpoux1` → déterminer catégorie créancier
- `filiation_detection_v2.nombreEnfantsDetecte`
- `filiation_detection_v2.agesEnfantsDetectes`

**Nouveaux champs à ajouter** :
- `aripaRecouvrementEnvisage` (boolean | null) — détecté si mention « ARIPA », « pension impayée », « saisie pension », « L. 581 CSS ».
- `montantPensionMensuelleDueEur` (int | null) — extrait du titre exécutoire / jugement.
- `titreExecutoireDetecte` (boolean | null) — détecté si mention « jugement définitif », « convention CM notariée », « titre exécutoire ».

## Plan de test

- UT calculator : (a) titre + débiteur salarié → voie `SAISIE_SUR_SALAIRE` ; (b) sans titre → `TITRE_REQUIS` ; (c) débiteur à l'étranger → `CONVENTION_INTERNATIONALE` ; (d) pension < seuil ASF → `montantASFEligible > 0`.
- UT service : gates pays + domaine.
- IT controller : POST + GET.

## Composants impactés

- Migration Liquibase 277 : table `aripa_recouvrement_analyses`.
- Migration Liquibase 278 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `aripaRecouvrementEnvisage`, `DROIT_FAMILLE`, `FRANCE`, priority 101.
- Java : `AripaRecouvrementCalculator`, result, analysis, repository, service, controller, `VoieRecouvrementEnum`, `SituationAripa`.
- `CaseAnalysisResponse.java` — ajout `aripaRecouvrementEnvisage`, `montantPensionMensuelleDueEur`, `titreExecutoireDetecte`.
- `LegalDomainPromptBuilder` — section `FAMILLE_INSTRUCTION`.

## Critères d'acceptation

- AC1 : titre + débiteur salarié FR → voie `SAISIE_SUR_SALAIRE`, étapes listées.
- AC2 : sans titre exécutoire → verdict `TITRE_REQUIS`.
- AC3 : `country=BELGIQUE` → 400.
- AC4 : POST + GET idempotent.

## Hors périmètre

- Frontend (SF-216-08).
- Convention La Haye 2007 (recouvrement international — référencé mais non détaillé).
- Procédure pénale abandon de famille (art. 227-3 CP — FR-only pénal hors scope).
