# SF-FA-18-03 — Backend contestation de paternité (art. 332-335 Cciv)

> **SF-03 du chantier F-FA-18 (Filiation)** — 5-7 SF prévues. Précédentes :
> SF-01 reconnaissance paternelle (art. 316), SF-02 frontend reconnaissance.
> Cette SF complète le pendant contentieux : contestation d'une filiation
> paternelle déjà établie (art. 332-335 Cciv).

## Objectif

Exposer un endpoint POST/GET d'analyse de recevabilité d'une **action en contestation de paternité** (art. 332-335 Cciv) côté France, en distinguant les 4 qualités à agir (père déclaré, père biologique présumé, mère, enfant majeur), avec calcul du délai de prescription restant et orientation sur l'expertise ADN.

## Concept métier

L'action en contestation de paternité remet en cause un lien de filiation paternelle déjà établi (par reconnaissance, par effet de la loi ou par possession d'état). Les règles dépendent du contestant :

1. **CONTESTATION_PAR_LE_PERE_DECLARE** (art. 332-333 Cciv) — le père légalement reconnu conteste sa propre paternité. Délai 5 ans à compter de la connaissance que la paternité ne lui appartient pas (333 al. 1). Si la possession d'état est conforme depuis 5 ans, l'action devient irrecevable (art. 333 al. 2).
2. **CONTESTATION_PAR_LE_VERITABLE_PERE** (art. 333) — le père biologique présumé conteste la filiation établie au profit d'un autre. Délai 5 ans à compter de la connaissance.
3. **CONTESTATION_PAR_LA_MERE** (art. 333) — la mère, le plus souvent pour permettre la reconnaissance par le père biologique. Délai 5 ans à compter de la connaissance.
4. **CONTESTATION_PAR_L_ENFANT_MAJEUR** (art. 333) — l'enfant à sa majorité. Délai 10 ans à compter de la majorité (art. 321).

Si la possession d'état dure depuis plus de 5 ans, la contestation n'est plus recevable que par l'enfant lui-même (art. 333 al. 2).

## Critères de validité

- `qualiteAagir` (`PERE_DECLARE` / `PERE_BIOLOGIQUE_PRESUME` / `MERE` / `ENFANT_MAJEUR`) — obligatoire
- `dateEtablissementFiliation` — date de la reconnaissance ou du jugement contesté
- `dateConnaissanceVerite` — date à laquelle le contestant a su que la filiation déclarée ne correspondait pas à la vérité biologique
- `possessionEtatConforme5Ans` (boolean) — la possession d'état conforme à la filiation a-t-elle duré 5 ans ou plus ?
- `expertiseAdnDemandee` (boolean) — une expertise ADN est-elle déjà demandée / envisagée ? (l'expertise est de droit en matière de filiation hors motif légitime de refus, Cass. 1ère civ. 28 mars 2000)
- `motifsSerieux` (boolean) — l'action est-elle fondée sur des éléments précis et concordants (autres que l'absence de ressemblance) — exigence de la jurisprudence pour ouvrir l'expertise

## Verdict

- **ELEVEE** si qualité à agir reconnue + délai non prescrit + (motifs sérieux OU expertise ADN demandée) + (pas de fin de non-recevoir liée à la possession d'état conforme 5 ans, sauf enfant)
- **MOYENNE** si délai partiellement écoulé (>50% consommé) OU absence de motifs sérieux explicites OU possession d'état favorable au défendeur
- **FAIBLE** si délai prescrit OU qualité à agir éteinte (possession d'état conforme 5 ans pour les contestants autres que l'enfant) OU absence totale d'élément de preuve

## Sortie

`verdictRecevabilite`, `scoreRecevabilite`, `delaiPrescriptionRestantMois` (peut être négatif si prescrit), `delaiPrescriptionAns` (5 ou 10), `expertiseAdnRecommandee` (boolean), `risquesRefus` (List<String>), `documentsRequis` (List<String>), `baseJuridique` (`"Art. 332-335 + 311-1 + 321 + 372 Cciv"`), `formule`, `messages`, `country`.

## Comportement nominal

- POST `/api/v1/case-files/{caseFileId}/contestation-paternite-analysis` : calcule + persiste (upsert 1:1).
- GET `/api/v1/case-files/{caseFileId}/contestation-paternite-analysis` : renvoie la dernière analyse (404 si aucune).

## Cas d'erreur

- 400 si critères obligatoires manquants (`qualiteAagir`, `dateEtablissementFiliation`, `dateConnaissanceVerite` null).
- 400 si workspace BELGIQUE (single-country FR — équivalent BE distinct, art. 318 et s. CC belge → backlog jumeau).
- 400 si dossier non DROIT_FAMILLE.
- 404 si dossier hors workspace de l'utilisateur.
- 404 si GET sans POST préalable.

## Critères d'acceptation vérifiables

1. POST FR + qualité PERE_DECLARE + délai 5 ans non écoulé + motifs sérieux → verdict `ELEVEE`, `delaiPrescriptionAns = 5`, `delaiPrescriptionRestantMois > 0`.
2. POST FR + qualité ENFANT_MAJEUR + délai 10 ans → `delaiPrescriptionAns = 10`.
3. POST FR + délai dépassé → verdict `FAIBLE`, message "prescription acquise", `delaiPrescriptionRestantMois <= 0`.
4. POST FR + qualité PERE_DECLARE + possession d'état conforme 5 ans → verdict `FAIBLE`, message "fin de non-recevoir art. 333 al. 2".
5. POST FR + qualité ENFANT_MAJEUR + possession d'état conforme 5 ans → l'enfant reste recevable → verdict ≥ `MOYENNE`.
6. POST FR + motifs sérieux false + expertiseAdnDemandee false → verdict `MOYENNE` ou `FAIBLE`, message exigence motifs.
7. POST FR + expertiseAdnDemandee true → `expertiseAdnRecommandee = true`, message Cass. 1ère civ. 28/3/2000.
8. `baseJuridique` contient « 332 », « 333 », « 311-1 » et « 321 ».
9. `documentsRequis` contient toujours « acte de naissance » et au moins une mention « expertise ADN » (selon contexte).
10. POST workspace BE → 400 avec message d'orientation backlog jumeau.
11. POST workspace immigration FR → 400.
12. Cross-workspace → 404.
13. Upsert : second POST remplace l'analyse précédente.
14. GET sans POST → 404.

## Plan de test

- **Unit** (`ContestationPaterniteCalculatorTest`, ≥ 15) :
  - Verdicts ELEVEE/MOYENNE/FAIBLE × 4 qualités à agir.
  - Calcul du `delaiPrescriptionRestantMois` (positif, négatif, à zéro).
  - Délai 5 ans (PERE/MERE/PERE_BIOLOGIQUE) vs 10 ans (ENFANT_MAJEUR).
  - Fin de non-recevoir possession d'état 5 ans (effet pour PERE/MERE/PERE_BIOLOGIQUE, pas pour ENFANT).
  - Expertise ADN demandée → `expertiseAdnRecommandee = true`.
  - Validation : nulls, country BE → IllegalArgumentException.
  - `baseJuridique` 332/333/311-1/321.
  - `risquesRefus` non vide quand au moins un critère partiel.
  - Formule contient score + verdict.
- **IT** (`ContestationPaterniteControllerIT`, ≥ 7) :
  - POST nominal pere declaré ELEVEE.
  - POST délai dépassé → FAIBLE.
  - POST workspace BE → 400.
  - POST domain immigration → 400.
  - POST cross-workspace → 404.
  - POST upsert.
  - POST sans qualiteAagir → 400.
  - GET 404 sans POST.
  - GET 200 après POST.

## Tables / endpoints / composants impactés

- **Migration Liquibase** : `181-create-contestation-paternite-analyses.xml` — table `contestation_paternite_analyses` (UNIQUE `case_file_id`) + INSERT `decision_tool_visibility_rules` (ALWAYS_ON, DROIT_FAMILLE, FRANCE, priority **90**, UUID `f1a04001-0000-0000-0000-ee000000181a` (variant `a` car `ee0000000181` déjà utilisé par migration 132 pour `F-DT-18-fin-mission-interim`), tool_id `'F-FA-18-contestation-paternite'`).
- **Backend** : `ContestationPaterniteRequest/Response/Result/Analysis/Repository/Calculator/Service/Controller`.

## Hors périmètre

- Frontend (SF-FA-18-04 ou ultérieure).
- Belgique — contestation BE diffère (CC art. 318, 330) → backlog jumeau.
- Action en recherche de paternité (art. 327 et s.) → SF-FA-18-05.
- Possession d'état (art. 317) → SF-FA-18-06.
- Adoption simple/plénière → SF-FA-18-07/08.

## Impact par domaine métier

Feature **sensible au domaine** :
- **Droit du travail / Immigration** : non applicable (404 / 400 par gate).
- **Droit famille** :
  - **France** : couvert par cette SF (art. 332-335 + 311-1 + 321 Cciv).
  - **Belgique** : régime distinct (CC art. 318 al. 1, 330) — **feature jumelle au backlog**.

## Parité des domaines métier (outil de niveau 5 — scoring de validité)

- **Droit du travail FR/BE** : non applicable (sphère filiation).
- **Droit immigration FR/BE** : non applicable directement (la filiation peut soutenir une demande, mais l'établir/contester = droit famille).
- **Droit famille FR** : couvert par cette SF.
- **Droit famille BE** : équivalent existant en CC art. 318/330 — **à ouvrir au backlog comme feature jumelle F-FA-18-BE-contestation**. Justification de l'asymétrie temporaire : régime juridique distinct (en BE, la contestation par le père légal est conditionnée à un délai d'1 an à compter de la connaissance, l'action de la mère existe en parallèle, et l'expertise ADN suit un cadre procédural différent).

## Analyse de cohérence transversale

- **Outils décisionnels existants** : F-FA-18 SF-03 vient compléter le bloc filiation contentieuse aux côtés de SF-01 (reconnaissance volontaire 316). Pas de chevauchement avec les blocs divorce (F-FA-08/09/10), patrimonial (F-FA-04/05/15/16/17), mesures provisoires (F-FA-12), séparation de corps (F-FA-21), PACS (F-FA-20), ordonnance de protection (F-FA-14), PMA (F-FA-27), succession (F-FA-24). Outil **single-country FRANCE** uniformément avec SF-01.
- **Patterns transversaux** : aucun nouveau composant partagé / DTO / directive / service. Réutilisation stricte du pattern `ReconnaissancePaternele*` (PR #652 mergée — chantier F-FA-18 SF-01) — record Request/Response/Result + Calculator pur statique + Service Spring + Controller REST + Analysis JPA + Repository.

## Préoccupations transversales

- **Auth / Principal** : aucun changement (réutilise `CurrentUserResolver` + `OAuthProviderResolver`).
- **Workspace context** : aucun changement (gate `cf.getWorkspace().getCountry()` + `cf.getLegalDomain()`).
- **Plans / limites** : non concerné.
- **Navigation / routing** : aucun ajout (endpoints REST seulement).
- **Outil décisionnel métier** : nouveau outil, scan effectué — un outil = une situation métier (contestation 332-335 ≠ reconnaissance volontaire 316 = SF-01 ≠ recherche paternité 327 future SF ≠ possession d'état 317 future SF). Pas de mélange dans ce calculator.

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/contestation-paternite-analysis`

Body :
```json
{
  "qualiteAagir": "PERE_DECLARE" | "PERE_BIOLOGIQUE_PRESUME" | "MERE" | "ENFANT_MAJEUR",
  "dateEtablissementFiliation": "2018-04-15",
  "dateConnaissanceVerite": "2025-01-20",
  "dateMajoriteEnfant": "2026-06-01",
  "possessionEtatConforme5Ans": false,
  "expertiseAdnDemandee": true,
  "motifsSerieux": true
}
```

Response 200 :
```json
{
  "caseFileId": "uuid",
  "qualiteAagir": "PERE_DECLARE",
  "verdictRecevabilite": "ELEVEE" | "MOYENNE" | "FAIBLE",
  "scoreRecevabilite": 88,
  "delaiPrescriptionAns": 5,
  "delaiPrescriptionRestantMois": 42,
  "expertiseAdnRecommandee": true,
  "risquesRefus": ["..."],
  "documentsRequis": ["..."],
  "baseJuridique": "Art. 332-335 + 311-1 + 321 + 372 Cciv",
  "formule": "...",
  "messages": ["..."],
  "country": "FRANCE"
}
```

Codes d'erreur : 400 (validation, country BE, domain mismatch), 404 (case file inconnu / autre workspace, GET sans POST).
