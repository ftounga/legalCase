# SF-216-05 — Liquidation régime communauté légale FR — backend

## Objectif

Restaurer et compléter le calculateur backend de la liquidation du régime de communauté légale post-divorce (art. 1467-1517 Cciv) : calculer la masse commune, les récompenses, la soulte éventuelle et la quote-part de chaque époux.

> Outil `F-FA-04-liquidation-communaute` — DELETE migration 191, à restaurer. Rang 3 Top-10 audit F-191.

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/liquidation-communaute-legale`.
- Body :
  - `valeurImmeubleCommun1Eur` (int, optionnel)
  - `valeurImmeubleCommun2Eur` (int, optionnel)
  - `capitalRestantDuEur` (int, optionnel) — crédit immobilier commun
  - `valeurMobilierCommunEur` (int, optionnel)
  - `autresActifsCommunsEur` (int, optionnel)
  - `recompensesEpoux1Eur` (int, optionnel) — récompenses dues par l'époux 1 à la communauté
  - `recompensesEpoux2Eur` (int, optionnel)
  - `biensPropresEpoux1Eur` (int, optionnel)
  - `biensPropresEpoux2Eur` (int, optionnel)
  - `occupationLogementEpoux` (EPOUX1 | EPOUX2 | AUCUN, optionnel)
- Calculator `LiquidationCommunauteLegaleCalculator` :
  - **Masse commune** = somme actifs communs − passif commun (crédit restant dû).
  - **Récompenses** : intègre les créances réciproques entre époux et communauté (art. 1433+ Cciv).
  - **Quote-part nette** de chaque époux = (masse commune / 2) ± récompenses.
  - **Soulte** : si biens impossibles à diviser en nature, calcule la soulte due par l'époux qui reprend le bien.
  - **Alerte indivision** : si désaccord probable (ex. immobilier non partagé).
- Retourne : `masseCommuneEur`, `soulteEur`, `quotaPartEpoux1Eur`, `quotaPartEpoux2Eur`, `recompensesNettes`, `alerteIndivision`, `baseLegale`, `messages`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `legalDomain ≠ DROIT_FAMILLE` → 400.
- Montants négatifs → 400.
- `regimeMatrimonialDetecte` ≠ COMMUNAUTE_LEGALE → 400 avec message « outil applicable uniquement en communauté légale ».

## Source juridique

- **art. 1400-1469 Cciv** — composition et gestion de la communauté légale.
- **art. 1467-1517 Cciv** — dissolution et liquidation de la communauté.
- **art. 1433-1435 Cciv** — récompenses.
- **art. 815-832 Cciv** — indivision résiduelle.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `regimes_vie_commune_detection_v2.regimeMatrimonialDetecte`
- `regimes_vie_commune_detection_v2.valeurCommunauteEurDetectee`
- `regimes_vie_commune_detection_v2.valeurImmeubleEur`
- `regimes_vie_commune_detection_v2.capitalRestantDuEur`
- `regimes_vie_commune_detection_v2.contratNotarieDetected`
- `regimes_vie_commune_detection_v2.clauseAttributionIntegraleDetected`

**Nouveaux champs à ajouter** :
- `liquidationCommunauteEnvisagee` (boolean | null) — détecté si mention « liquidation communauté », « 1467 », « partage actif commun ».
- `recompensesEpoux1Eur` (int | null) — récompenses dues par l'époux 1 extraites des pièces.
- `recompensesEpoux2Eur` (int | null) — idem époux 2.

## Plan de test

- UT calculator : (a) immeuble 400 000 €, crédit 100 000 € → masse commune 300 000 €, quote-part 150 000 € chacun ; (b) récompenses → offset quote-part ; (c) régime ≠ COMMUNAUTE_LEGALE → erreur 400.
- UT service : gates pays/domaine/régime.
- IT controller : POST + GET.

## Composants impactés

- Migration Liquibase 275 : table `liquidation_communaute_legale_analyses`.
- Migration Liquibase 276 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `liquidationCommunauteEnvisagee`, priority 100.
- Java : `LiquidationCommunauteLegaleCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `liquidationCommunauteEnvisagee`, `recompensesEpoux1Eur`, `recompensesEpoux2Eur`.
- `LegalDomainPromptBuilder` — section `FAMILLE_INSTRUCTION`.

## Critères d'acceptation

- AC1 : immeuble 300 000 €, crédit 0 € → quote-parts = 150 000 € chacun.
- AC2 : régime ≠ COMMUNAUTE_LEGALE → 400 message explicite.
- AC3 : `country=BELGIQUE` → 400.
- AC4 : POST + GET → idempotent.

## Hors périmètre

- Frontend (SF-216-06).
- Récompenses complexes entre époux (outil F-FA-15-recompenses existant).
- Aspects fiscaux (plus-value immobilière, droits de mutation).
