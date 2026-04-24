# Mini-spec — F-FA-09 / SF-FA-09-01 Divorce pour faute FR — BACKEND

## Objectif

Outil FR **divorce pour faute** (art. 242 Cciv). ~10-15 % des divorces. Nécessite prouver des faits constitutifs d'une **violation grave ou renouvelée des devoirs du mariage, rendant intolérable le maintien de la vie commune**.

## Règles (art. 242-246 Cciv)

- Fautes recevables : adultère, violences, abandon, outrages, comportement irrespectueux grave, violations fidélité/assistance/communauté de vie
- Charge de la preuve sur le demandeur
- Le juge peut retenir : tort exclusif / torts partagés
- Conséquences : dommages-intérêts possibles art. 266 Cciv (préjudice d'une particulière gravité) + prestation compensatoire art. 270

## Inputs

- `fautesInvoquees` : liste enum — `ADULTERE`, `VIOLENCES`, `ABANDON`, `OUTRAGES`, `DEVOIR_ASSISTANCE`, `DEVOIR_FIDELITE`, `DEVOIR_COMMUNAUTE_VIE`, `AUTRE`
- `preuvesDocumentaires` : boolean (constats d'huissier, témoignages, mains courantes, jugements antérieurs)
- `tortsAdverseInvoques` : boolean (demande reconventionnelle possible)
- `dureeMariageAnnees` : int
- `revenusAnnuelsDemandeurEur` : BigDecimal
- `revenusAnnuelsDefendeurEur` : BigDecimal
- `dateDepotAssignation` : LocalDate nullable

## Outputs

- `nombreFautesInvoquees` : int
- `solidariteeFautesOk` : boolean (≥ 1 faute recevable + preuves)
- `risqueTortsPartages` : boolean (= tortsAdverseInvoques)
- `scoreGlobal` : 0-100
- `verdictTortsEstimes` : EXCLUSIF_DEFENDEUR / PARTAGES / IMPREDICTIBLE
- `damagesInteretsArt266FourchetteMin/Max` : selon gravité + durée mariage
- `prestationCompensatoireFourchetteMin/Max`
- `criteresNonRemplis`
- `formule`
- `baseJuridique` : "Art. 242-246 + 266 + 270 Cciv"
- `messages` : importance preuves, DI art. 266 conditionnés à "particulière gravité", prestation compensatoire indépendante des torts

## Architecture

Pattern F-IM-09. Single-country FR DROIT_FAMILLE. Migration 128. Table `divorce_faute_analyses`. UUID `f1a04001-0000-0000-0000-ee00000fa091`, ALWAYS_ON FR DROIT_FAMILLE, priority 71, tool_id `F-FA-09-divorce-faute`.

## Contrat API

POST + GET `/api/v1/case-files/{caseFileId}/divorce-faute`

## Tests
~16 UT (7 types de fautes) + 8 IT. Gate FR + DROIT_FAMILLE.

## Hors scope
- Frontend (SF ultérieure)
- Prononcé torts exclusifs vs partagés : seul le juge décide — l'outil produit une estimation indicative
