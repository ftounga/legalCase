# SF-216-29 — Donation-partage FR — backend

## Objectif

Outil décisionnel `F-FA-DONATION-PARTAGE` : évalue les conditions de validité d'une donation-partage (art. 1075-1080 Cciv), son intérêt par rapport à une succession ordinaire (gel de la valeur au jour de la donation, neutralisation des rapports), et détecte les cas spéciaux (donation-partage conjonctive entre les deux parents, réincorporation de donations antérieures).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/donation-partage`.
- Body :
  - `nombreDescendants` (int, requis)
  - `presencePetitsEnfantsParSubstitution` (boolean) — art. 1075-1 : possibilité de donner directement aux petits-enfants
  - `donationPartageConjonctive` (boolean) — les deux parents font une donation conjointe
  - `valeurPartageTotal` (int, optionnel)
  - `respectQuotiteDisponible` (boolean, optionnel)
  - `donationsAnterieuresAReinorporer` (boolean) — réincorporation d'anciennes donations pour équilibrer
  - `agesDonateurs` (List<Integer>, requis, max 2 éléments)
- Calculator :
  - **Gel de valeur** : les biens donnés sont évalués à la date de la donation-partage, pas à la date du décès — intérêt fiscal et successoral.
  - **Exclusion du rapport** : les biens donnés-partagés ne sont pas sujets au rapport (art. 1075-3 Cciv).
  - **Petits-enfants par substitution** (art. 1075-1) : possible si descendant visé y consent.
  - **Donation-partage conjonctive** : les deux parents font ensemble une donation de leurs biens propres et communs.
  - **Quotité disponible** : alerte si dépassée.
  - **Réincorporation** : possibilité d'intégrer des donations antérieures pour équilibrer (art. 1078-1 Cciv).
- Retourne : `conditionsRemplies`, `interet`, `gelValeurEffet`, `rapportExclu`, `alerteQuotite`, `etapesNotariales`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `nombreDescendants < 1` → 400.
- Âges donateurs négatifs → 400.

## Source juridique

- **art. 1075 Cciv** — donation-partage, conditions.
- **art. 1075-1 Cciv** — donation-partage aux petits-enfants (avec substitution).
- **art. 1075-2 Cciv** — donation-partage conjonctive.
- **art. 1075-3 Cciv** — exclusion du rapport successoral.
- **art. 1078-1 Cciv** — réincorporation de donations antérieures.
- **art. 1080 Cciv** — quasi-usufruit en donation-partage.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `succession_detection_v2.nbDescendantsDetecte`
- `succession_detection_v2.montantDonationsRecuesEur`
- `regimes_vie_commune_detection_v2.respectQuotiteDisponibleDetected`

**Nouveaux champs à ajouter** :
- `donationPartageEnvisagee` (boolean | null) — détecté si mention « donation-partage », « art. 1075 », « répartir patrimoine aux enfants ».
- `presencePetitsEnfantsSubstitutionDetectee` (boolean | null) — petits-enfants mentionnés comme bénéficiaires potentiels.
- `donationPartageConjonctiveDetectee` (boolean | null) — deux parents auteurs de la donation détectés.

## Plan de test

- UT calculator : (a) 2 descendants + gel valeur → rapport exclu, gel confirmé ; (b) quotité disponible dépassée → alerte ; (c) réincorporation donations antérieures → équilibre calculé ; (d) petits-enfants par substitution → condition consentement signalée.
- UT service : gates.
- IT : POST + GET.

## Composants impactés

- Migration Liquibase 299 : table `donation_partage_analyses`.
- Migration Liquibase 300 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `donationPartageEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 112.
- Java : `DonationPartageCalculator`, result, analysis, repository, service, controller.
- `CaseAnalysisResponse.java` — ajout `donationPartageEnvisagee`, `presencePetitsEnfantsSubstitutionDetectee`, `donationPartageConjonctiveDetectee`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : 2 descendants → gel valeur confirmé, rapport exclu.
- AC2 : quotité dépassée → alerte.
- AC3 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-30).
- Donation entre vifs générique (outil existant F-FA-24-donation).
