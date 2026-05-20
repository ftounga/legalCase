# SF-216-09 — Délégation d'autorité parentale FR — backend

## Objectif

Outil décisionnel `F-FA-DELEGATION-AP` : arbre décisionnel déterminant la recevabilité et la voie d'une délégation d'autorité parentale (art. 376-1 Cciv) — délégation volontaire conjointe, délégation judiciaire sur désintérêt, délégation au profit d'un tiers (grand-parent, famille, association habilitée).

## Comportement nominal

- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/delegation-autorite-parentale`.
- Body :
  - `typeDelégation` (VOLONTAIRE_CONJOINTE | JUDICIAIRE_TIERS | JUDICIAIRE_DESINTERET)
  - `tiersLienFamilial` (GRANDS_PARENTS | ONCLE_TANTE | FAMILLE_ELARGIE | ASSOCIATION_HABILITEE | AUTRE)
  - `accordParents` (boolean) — les deux parents consentent
  - `motivationDelégation` — texte libre
  - `ageEnfant` (int, requis)
  - `dangerCaracterise` (boolean, optionnel) — danger pour l'enfant justifiant procédure urgente
  - `decisionsJudiciairesPrecedentes` (boolean, optionnel)
- Calculator :
  - **Voie volontaire conjointe** (art. 376-1 al. 1) : si accord des deux parents + tiers acceptant → voie notariale ou déclaration JAF.
  - **Voie judiciaire sur désintérêt** (art. 376-1 al. 2) : si un parent s'est désintéressé de l'enfant depuis + d'un an → procédure JAF contentieuse.
  - **Voie judiciaire tiers** : à défaut d'accord, saisine JAF sur requête.
  - **Intérêt supérieur de l'enfant** : alerte si enfant < 18 mois (délégation rare) ou > 18 ans (inutile).
  - **Articulation retrait AP** : si danger caractérisé → orienter vers SF-216-11 retrait AP.
- Retourne : `verdictRecevabilite`, `voieProcédurale`, `etapesConcretes`, `dureeEstimee`, `baseLegale`, `messages`, `alertes`.
- Persiste 1:1 par dossier.

## Cas d'erreur

- `country ≠ FRANCE` → 400.
- `legalDomain ≠ DROIT_FAMILLE` → 400.
- `ageEnfant < 0` ou `ageEnfant > 18` → 400.
- Workspace mismatch → 404.

## Source juridique

- **art. 376-1 Cciv** — délégation d'autorité parentale (volontaire et judiciaire).
- **art. 376-1 al. 2 Cciv** — désintérêt de l'enfant depuis plus d'un an.
- **art. L. 228-1 CASF** — associations habilitées susceptibles de se voir déléguer l'AP.
- **Cass. 1ère civ., 22/11/2005** — conditions de la délégation judiciaire.

## Champs IA à extraire (FamilleExtractedData)

**Réutilisés (F-246)** :
- `filiation_detection_v2.agesEnfantsDetectes`
- `filiation_detection_v2.nombreEnfantsDetecte`

**Nouveaux champs à ajouter** :
- `delegationApEnvisagee` (boolean | null) — détecté si mention « délégation autorité parentale », « art. 376-1 », « tiers détenteur AP ».
- `tiersLienFamilialDetecte` (String | null) — type de tiers identifié dans les pièces.
- `accordParentsDetecte` (boolean | null) — accord des deux parents mentionné dans les pièces.

## Plan de test

- UT calculator : (a) accord des deux parents + tiers acceptant → voie `VOLONTAIRE_CONJOINTE` ; (b) désintérêt > 1 an → voie `JUDICIAIRE_DESINTERET` ; (c) danger caractérisé → alerte `RETRAIT_AP_A_ENVISAGER` ; (d) enfant > 18 ans → erreur 400.
- UT service : gates pays + domaine.
- IT controller : POST + GET.

## Composants impactés

- Migration Liquibase 279 : table `delegation_ap_analyses`.
- Migration Liquibase 280 : INSERT `decision_tool_visibility_rules` CONTEXTUAL `delegationApEnvisagee`, `DROIT_FAMILLE`, `FRANCE`, priority 102.
- Java : `DelegationAutoriteParentaleCalculator`, result, analysis, repository, service, controller, `TypeDelegationEnum`, `TiersLienEnum`.
- `CaseAnalysisResponse.java` — ajout `delegationApEnvisagee`, `tiersLienFamilialDetecte`, `accordParentsDetecte`.
- `LegalDomainPromptBuilder`.

## Critères d'acceptation

- AC1 : accord deux parents + tiers → voie `VOLONTAIRE_CONJOINTE`, étapes listées.
- AC2 : danger caractérisé → alerte « voir outil retrait autorité parentale ».
- AC3 : enfant > 18 ans → 400.
- AC4 : `country=BELGIQUE` → 400.

## Hors périmètre

- Frontend (SF-216-10).
- Retrait d'autorité parentale (SF-216-11/12).
