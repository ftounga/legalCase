# SF-216-27 — Partage successoral notarié — backend

## Objectif

Outil décisionnel `F-FA-PARTAGE-NOTARIAL` : guide l'avocat dans la procédure de partage successoral amiable devant notaire (art. 870+ Cciv + 816+ Cciv) — calendrier des étapes, vérification des conditions, distinction avec le partage judiciaire (outil existant F-FA-17-partage-judiciaire).

> Outil distinct de `F-FA-24-partage-successoral` (qui ne distinguait pas notarié vs judiciaire) et de `F-FA-17-partage-judiciaire` (contentieux uniquement).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/partage-successoral-notarial`.
- Body :
  - `dateOuvertureSuccession` (LocalDate, requis)
  - `nombreCoheritiers` (int, requis)
  - `consentementsTousDetecte` (boolean) — tous les héritiers d'accord
  - `presenceImmeuble` (boolean) — succession comprend un immeuble (notaire obligatoire)
  - `desaccordPersistant` (boolean) — désaccord survenu → basculer vers judiciaire
  - `valeurMasseSuccessoraleEur` (int, optionnel)
  - `notaireDesigne` (boolean, optionnel)
  - `declarationSuccessionEcheance` (LocalDate, optionnel) — délai fiscal 6 mois
- Calculator :
  - **Notaire obligatoire** : si immeuble dans la succession → notaire requis (art. 1592 CGI).
  - **Étapes calendrier** :
    1. Désignation du notaire.
    2. Bilan patrimonial (inventaire des biens, dettes).
    3. Attestation après décès (pour immeubles).
    4. Projet de partage (notaire propose la répartition).
    5. Signature de l'acte de partage (tous héritiers présents ou représentés).
  - **Délai déclaration fiscale** : 6 mois depuis décès (art. 641 CGI) — alerte si dépassé.
  - **Bascule judiciaire** : si `desaccordPersistant = true` → orienter vers F-FA-17-partage-judiciaire.
- Retourne : `notaireObligatoire`, `calendrierEtapes[]`, `delaiDeclarationFiscale`, `alerteDelai`, `orientationJudiciaire`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `dateOuvertureSuccession` future → 400.
- `nombreCoheritiers < 1` → 400.

## Source juridique

- **art. 816-842 Cciv** — partage successoral.
- **art. 870+ Cciv** — déclaration de succession.
- **art. 1592 CGI** — obligation notariale en présence d'immeubles.
- **art. 641 CGI** — délai 6 mois déclaration fiscale.
- **art. 840 Cciv** — partage judiciaire (renvoi si désaccord).

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `succession_detection_v2.dateOuvertureSuccessionDetectee`
- `succession_detection_v2.nombreCoheritiersDetecte`
- `succession_detection_v2.montantSuccessionEur`
- `succession_detection_v2.typeIndivisionSuccessoraleDetecte`

**Nouveaux champs à ajouter** :
- `partageNotarialEnvisage` (boolean | null) — détecté si mention « partage amiable », « notaire désigné partage », « 816 Cciv ».
- `presenceImmeubleSuccessionDetecte` (boolean | null) — présence d'immeuble détectée dans la succession.
- `declarationSuccessionEcheancDetectee` (LocalDate | null) — date de décès extraite pour calculer l'échéance fiscale.

## Plan de test

- UT calculator : (a) immeuble + tous héritiers d'accord → notaire obligatoire, calendrier 5 étapes ; (b) désaccord → orientation judiciaire ; (c) délai 6 mois fiscal dépassé → alerte.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 297 : table `partage_successoral_notarial_analyses`.
- Migration Liquibase 298 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `partageNotarialEnvisage`, `DROIT_FAMILLE`, `FRANCE`, priority 111.
- Java : `PartageSuccessoralNotarialCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `partageNotarialEnvisage`, `presenceImmeubleSuccessionDetecte`, `declarationSuccessionEcheancDetectee`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : immeuble + accord → calendrier 5 étapes généré.
- AC2 : désaccord → orientation vers partage judiciaire.
- AC3 : délai fiscal dépassé → alerte.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-28).
- Partage judiciaire (outil existant F-FA-17-partage-judiciaire).
- Aspects fiscaux détaillés (CGI, droits de mutation).
