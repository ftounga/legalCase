# Mini-spec — F-FA-10 / SF-FA-10-01 Divorce accepté FR — BACKEND

## Objectif

Outil FR **divorce accepté** / acceptation du principe de la rupture (art. 233 Cciv). Les époux acceptent le principe de la rupture **sans considération des faits** ; seul le prononcé et les conséquences dépendent du juge.

## Règles (art. 233-234 Cciv)

- Acceptation des deux époux du principe de la rupture
- Formalisée par procès-verbal d'acceptation signé (art. 1123 CPC)
- Irrévocabilité : une fois signée, pas de retour
- Pas de recherche de tort (pas de DI art. 266)
- Prestation compensatoire possible (art. 270)

## Inputs

- `acceptationPrincipeSignee` : boolean (PV d'acceptation signé par les 2 époux)
- `dateAcceptationPV` : LocalDate nullable
- `dureeMariageAnnees` : int
- `revenusAnnuelsEpoux1Eur` : BigDecimal
- `revenusAnnuelsEpoux2Eur` : BigDecimal
- `patrimoineCommun` : boolean (régime communauté ou participation)
- `dateAssignation` : LocalDate nullable

## Outputs

- `acceptationValide` : boolean (= acceptationPrincipeSignee && !=null dateAcceptationPV)
- `ordrePublic` : boolean (pas de contestation d'ordre public — toujours true si acceptation valide)
- `eligibilite` : boolean
- `prestationCompensatoireFourchetteMin/Max`
- `delaiProcedureMoisPrevisionnel` : int (~8-12 mois)
- `criteresNonRemplis`
- `formule`
- `baseJuridique` : "Art. 233-234 Cciv + art. 1123 CPC"
- `messages` : irrévocabilité du PV, pas de DI art. 266 possibles, prestation compensatoire indépendante, conseil de formalisation convention avocats

## Architecture

Pattern F-IM-09. Single-country FR DROIT_FAMILLE. Migration 129. Table `divorce_accepte_analyses`. UUID `f1a04001-0000-0000-0000-ee00000fa101`, ALWAYS_ON FR DROIT_FAMILLE, priority 72, tool_id `F-FA-10-divorce-accepte`.

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/divorce-accepte`

## Tests
~12 UT + 8 IT. Gate FR + DROIT_FAMILLE.

## Hors scope
- Frontend
- Divorce par consentement mutuel (art. 229-1) — c'est une procédure distincte (acte sous signature privée contresigné par avocats), voir F-FA-04 / F-152 existants
