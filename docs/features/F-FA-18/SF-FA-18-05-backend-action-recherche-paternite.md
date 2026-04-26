# SF-FA-18-05 — Backend action en recherche de paternité (art. 327 + 340 Cciv)

> **SF-05 du chantier F-FA-18 (Filiation)** — 4/8 SF déjà livrées (SF-01
> reconnaissance paternelle, SF-02 frontend reconnaissance, SF-03 backend
> contestation paternité, SF-04 frontend contestation paternité). Cette SF
> couvre l'autre versant contentieux : **création** judiciaire d'un lien de
> paternité non volontairement reconnu — symétrique inverse de la
> contestation (qui annule un lien existant).

## Objectif

Exposer un endpoint POST/GET d'analyse de recevabilité d'une **action en recherche de paternité** (art. 327 + 340 + 16-11 + 321 Cciv) côté France, en distinguant les 3 qualités du demandeur (enfant majeur, représentant légal d'un mineur, mère), avec calcul du délai de prescription restant et orientation sur l'expertise ADN — quasi-systématique en la matière.

## Concept métier

L'action en recherche de paternité est l'action judiciaire engagée par l'enfant (ou son représentant) pour faire **établir** judiciairement un lien de paternité avec un homme qui ne l'a pas reconnu volontairement. C'est le pendant contentieux opposé à la **contestation** (SF-03) :
- Contestation = remettre en cause / **annuler** un lien existant.
- Recherche = faire **créer** un lien inexistant.

Régime applicable :
1. **Qualité à agir** (art. 327 al. 2) : l'action est strictement réservée à l'enfant. Pendant la minorité, son représentant légal (le plus souvent la mère) agit en son nom. Une fois majeur, l'enfant agit lui-même.
2. **Délai de prescription** (art. 321 Cciv) : 10 ans à compter de la majorité de l'enfant (suspension automatique pendant la minorité). Pour la mère agissant en représentation légale du mineur : le délai de l'enfant n'a pas commencé à courir, mais la mère doit agir avant la majorité. Pour la mère agissant à titre personnel : régime distinct, prescription 10 ans à compter de la naissance (jurisprudence — non couverte par la SF actuelle, à signaler dans messages).
3. **Expertise ADN** (art. 16-11 Cciv) : presque systématique en matière de filiation. La jurisprudence Cass. 1ère civ. 28 mars 2000 pose que l'expertise est de droit hors motif légitime de refus.
4. **Refus d'ADN** : présomption en faveur du demandeur si le défendeur refuse l'ADN sans motif légitime (Cass. 1ère civ. 28/3/2000).
5. **Possession d'état** : facilite l'admission (traitement, fama, nomen art. 311-1 — éléments concordants montrant que l'homme se comportait comme le père).

## Critères de validité

- `qualiteDuDemandeur` (`ENFANT_MAJEUR` / `REPRESENTANT_LEGAL_MINEUR` / `MERE`) — obligatoire
- `dateNaissanceEnfant` — obligatoire (calcul majorité + prescription)
- `presomptionPossessionEtat` (boolean) — éléments matériels établissant que l'homme s'est comporté en père
- `expertiseAdnDemandee` (boolean) — expertise ADN demandée / envisagée dans la procédure
- `pereDesigneRefuseADN` (boolean) — le père désigné a-t-il refusé l'ADN sans motif légitime (présomption)
- `motifsSerieux` (boolean) — éléments concordants (correspondances, témoignages, photos, etc.)

## Verdict

- **ELEVEE** si :
  - délai non prescrit
  - ET (expertise ADN demandée OU possession d'état OU motifs sérieux + refus ADN du défendeur)
- **MOYENNE** si :
  - délai non prescrit MAIS faisceau d'indices partiel (un seul signal positif)
- **FAIBLE** si :
  - délai prescrit (forclusion)
  - OU absence totale d'éléments (ni possession d'état, ni motifs, ni expertise demandée)

## Sortie

`verdictRecevabilite`, `scoreRecevabilite`, `delaiPrescriptionAns` (10), `delaiPrescriptionRestantMois` (peut être négatif si prescrit), `expertiseAdnRecommandee` (boolean), `presomptionRefusADN` (boolean), `risquesRefus` (List<String>), `documentsRequis` (List<String>), `baseJuridique` (`"Art. 327 + 340 + 16-11 + 321 Cciv"`), `formule`, `messages`, `country`.

## Comportement nominal

- POST `/api/v1/case-files/{caseFileId}/recherche-paternite-analysis` : calcule + persiste (upsert 1:1).
- GET `/api/v1/case-files/{caseFileId}/recherche-paternite-analysis` : renvoie la dernière analyse (404 si aucune).

## Cas d'erreur

- 400 si critères obligatoires manquants (`qualiteDuDemandeur`, `dateNaissanceEnfant` null).
- 400 si workspace BELGIQUE (single-country FR — équivalent BE distinct, art. 322 et s. CC belge → backlog jumeau).
- 400 si dossier non DROIT_FAMILLE.
- 404 si dossier hors workspace de l'utilisateur.
- 404 si GET sans POST préalable.

## Critères d'acceptation vérifiables

1. POST FR + qualité ENFANT_MAJEUR + enfant 25 ans + ADN demandée + motifs → verdict `ELEVEE`, `delaiPrescriptionAns = 10`, `delaiPrescriptionRestantMois > 0`.
2. POST FR + qualité ENFANT_MAJEUR + enfant 35 ans (>10 ans après majorité) → verdict `FAIBLE`, message "prescription acquise", `delaiPrescriptionRestantMois <= 0`.
3. POST FR + qualité REPRESENTANT_LEGAL_MINEUR + enfant 8 ans + ADN demandée → verdict `ELEVEE` (délai non échu — minorité).
4. POST FR + qualité MERE + enfant 5 ans + possession d'état true → verdict `ELEVEE`, message rappelant l'art. 327 al. 2.
5. POST FR + `pereDesigneRefuseADN = true` + ADN demandée → `presomptionRefusADN = true`, message Cass. 1ère civ. 28/3/2000.
6. POST FR + aucun critère positif (pas de possession, pas de motifs, pas d'ADN) → verdict `FAIBLE`, message exigence faisceau d'indices.
7. POST FR + ADN demandée → `expertiseAdnRecommandee = true`.
8. `baseJuridique` contient « 327 », « 340 », « 16-11 » et « 321 ».
9. `documentsRequis` contient toujours « acte de naissance » et au moins une mention « expertise ADN » + selon qualité du demandeur (justificatif autorité parentale pour REPRESENTANT_LEGAL_MINEUR).
10. POST workspace BE → 400 avec message d'orientation backlog jumeau.
11. POST workspace immigration FR → 400.
12. Cross-workspace → 404.
13. Upsert : second POST remplace l'analyse précédente.
14. GET sans POST → 404.

## Plan de test

- **Unit** (`RecherchePaterniteCalculatorTest`, ≥ 15) :
  - Verdicts ELEVEE/MOYENNE/FAIBLE × 3 qualités du demandeur.
  - Calcul du `delaiPrescriptionRestantMois` (positif, négatif, à zéro).
  - Délai 10 ans à compter de la majorité (ENFANT_MAJEUR).
  - Représentant légal : pas de prescription tant que minorité.
  - Présomption refus ADN.
  - Expertise ADN demandée → `expertiseAdnRecommandee = true`.
  - Possession d'état favorise verdict ELEVEE.
  - Validation : nulls, country BE → IllegalArgumentException.
  - `baseJuridique` 327/340/16-11/321.
  - `risquesRefus` non vide quand au moins un critère partiel.
  - Formule contient score + verdict.
  - Surcharge sans `today`.
- **IT** (`RecherchePaterniteControllerIT`, ≥ 7) :
  - POST nominal enfant majeur ELEVEE.
  - POST délai dépassé → FAIBLE.
  - POST workspace BE → 400.
  - POST domain immigration → 400.
  - POST cross-workspace → 404.
  - POST upsert.
  - POST sans qualiteDuDemandeur → 400.
  - GET 404 sans POST.
  - GET 200 après POST.

## Tables / endpoints / composants impactés

- **Migration Liquibase** : `183-create-recherche-paternite-analyses.xml` — table `recherche_paternite_analyses` (UNIQUE `case_file_id`) + INSERT `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority **92**, UUID `f1a04001-0000-0000-0000-ee0000000183`, tool_id `'F-FA-18-recherche-paternite'`).
- **Backend** : `RecherchePaterniteRequest/Response/Result/Analysis/Repository/Calculator/Service/Controller`.

## Hors périmètre

- Frontend (SF-FA-18-06 ou ultérieure).
- Belgique — recherche BE diffère (CC art. 322 et s.) → backlog jumeau.
- Possession d'état (action 317 Cciv pour faire constater) — SF-FA-18-07 ou backlog.
- Adoption simple/plénière → SF-FA-18-08 ou backlog.
- Action de la mère à titre personnel pour son propre préjudice (art. 340 ancien — caduque depuis 2005, juste signalée par message).

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (404 / 400 par gate).
- **Droit famille** :
  - **France** : couvert par cette SF (art. 327 + 340 + 16-11 + 321 Cciv).
  - **Belgique** : régime distinct (CC art. 322 al. 1, 332ter, 332-1) — **feature jumelle au backlog**.

## Parité des domaines métier (outil de niveau 5 — scoring de validité)

- **Droit du travail FR/BE** : non applicable (sphère filiation).
- **Droit immigration FR/BE** : non applicable directement (la filiation peut soutenir une demande, mais l'établir = droit famille).
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : équivalent existant en CC art. 322/332ter — **à ouvrir au backlog comme feature jumelle F-FA-18-BE-recherche-paternite**. Justification de l'asymétrie temporaire : régime juridique distinct (qualités à agir, délais, présomptions ADN différentes).

## Analyse de cohérence transversale

- **Outils décisionnels existants** : F-FA-18 SF-05 vient compléter le bloc filiation contentieuse aux côtés de SF-03 contestation paternité (PR #660 mergée). Symétrie inverse : contestation = annulation, recherche = création. Pas de chevauchement avec divorce (F-FA-08/09/10), patrimonial (F-FA-04/05/15/16/17), mesures provisoires (F-FA-12), séparation de corps (F-FA-21), PACS (F-FA-20), ordonnance de protection (F-FA-14), PMA (F-FA-27), succession (F-FA-24), majeurs protégés (F-FA-25), changement état civil (F-FA-26). Outil **single-country FRANCE** uniformément avec SF-01/SF-03.
- **Patterns transversaux** : aucun nouveau composant partagé / DTO / directive / service. Réutilisation stricte du pattern `ContestationPaternite*` (PR #660 mergée — chantier F-FA-18 SF-03) — record Request/Response/Result + Calculator pur statique + Service Spring + Controller REST + Analysis JPA + Repository.

## Préoccupations transversales

- **Auth / Principal** : aucun changement (réutilise `CurrentUserResolver` + `OAuthProviderResolver`).
- **Workspace context** : aucun changement (gate `cf.getWorkspace().getCountry()` + `cf.getLegalDomain()`).
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (endpoints REST seulement).
- **Outil décisionnel métier** : nouveau outil, scan effectué — un outil = une situation métier (recherche 327/340 ≠ contestation 332-335 = SF-03 ≠ reconnaissance volontaire 316 = SF-01 ≠ possession d'état 317 future SF). Pas de mélange dans ce calculator.

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/recherche-paternite-analysis`

Body :
```json
{
  "qualiteDuDemandeur": "ENFANT_MAJEUR" | "REPRESENTANT_LEGAL_MINEUR" | "MERE",
  "dateNaissanceEnfant": "2001-04-15",
  "presomptionPossessionEtat": true,
  "expertiseAdnDemandee": true,
  "pereDesigneRefuseADN": false,
  "motifsSerieux": true
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "qualiteDuDemandeur": "ENFANT_MAJEUR",
  "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
  "scoreRecevabilite": 88,
  "delaiPrescriptionAns": 10,
  "delaiPrescriptionRestantMois": 96,
  "expertiseAdnRecommandee": true,
  "presomptionRefusADN": false,
  "risquesRefus": ["..."],
  "documentsRequis": ["..."],
  "baseJuridique": "Art. 327 + 340 + 16-11 + 321 Cciv",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur : 400 (validation, country BE, domain mismatch), 404 (case file inconnu / autre workspace, GET sans POST).
