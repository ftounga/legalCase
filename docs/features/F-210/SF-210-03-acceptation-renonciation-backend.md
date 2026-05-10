# SF-210-03 — Acceptation / renonciation succession FR (backend)

## Objectif
Backend de l'outil décisionnel `acceptation-renonciation-succession` : à partir de la date d'ouverture de la succession et d'indices factuels, détermine quelles options successorales sont ouvertes (acceptation pure et simple, acceptation à concurrence de l'actif net, renonciation), calcule le délai restant (4 mois art. 771, +2 mois art. 772 second rang) et formule une recommandation prudente.

## Comportement nominal
- Endpoint `POST/GET /api/v1/case-files/{caseFileId}/acceptation-renonciation-succession`.
- Body : `dateOuvertureSuccession` (LocalDate, requis), `qualiteHeritier` (PREMIER_RANG / SECOND_RANG), `actifBrutEur`, `passifEur`, `actesEquivalentAcceptationDejaPosesDetected` (boolean), `inventaireRealise` (boolean), `dettesIncertainesDetected` (boolean), `intentionExprimee` (PURE_SIMPLE / CONCURRENCE_ACTIF / RENONCIATION / INCERTAIN).
- Calculator détermine :
  - **Délai restant** : 4 mois (premier rang) ou 6 mois (second rang) depuis l'ouverture.
  - **Option fermée par actes équivalents** : si `actesEquivalentAcceptationDejaPoses=true`, l'acceptation pure et simple est *réputée acquise* (art. 783) — la renonciation n'est plus possible.
  - **Recommandation** : 
    - Si actif clairement positif et dettes connues → `ACCEPTATION_PURE_SIMPLE`.
    - Si dettes incertaines ou actif/passif proches → `ACCEPTATION_CONCURRENCE_ACTIF` (limitation responsabilité art. 791).
    - Si passif >> actif et pas d'acte équivalent → `RENONCIATION`.
- Retourne le verdict, la liste des options ouvertes, le délai restant en jours, base juridique, formule, messages.
- Persiste 1:1 par dossier (table `acceptation_renonciation_succession_analyses`).

## Cas d'erreur
- Pays ≠ FRANCE → 400.
- Domaine ≠ DROIT_FAMILLE → 400.
- `dateOuvertureSuccession` future ou null → 400.
- Actif/passif négatifs → 400.

## Critères d'acceptation
- AC1 : ouverture il y a 30 jours + premier rang + actif positif → délai restant ~ 90 jours, options = [PURE_SIMPLE, CONCURRENCE_ACTIF, RENONCIATION], reco = PURE_SIMPLE.
- AC2 : ouverture il y a 30 jours + second rang → délai restant ~ 150 jours.
- AC3 : ouverture il y a 1 an → délai restant 0, message "délai dépassé — option à régulariser par déclaration au greffe".
- AC4 : `actesEquivalentAcceptationDejaPoses=true` → renonciation EXCLUSIVE de la liste des options ouvertes, message art. 783.
- AC5 : actif < passif + dettes incertaines → reco = `RENONCIATION`.
- AC6 : POST puis GET → idempotent.

## Plan de test
- UT calculator : 8 cas couvrant délais, rangs, intentions.
- IT controller : 1 happy path POST + GET.

## Tables / endpoints / composants impactés
- Migration Liquibase : 219 (nouveau).
- Table : `acceptation_renonciation_succession_analyses`.
- Java : package `fr.ailegalcase.casefile`.
- Endpoint : `POST/GET /api/v1/case-files/{id}/acceptation-renonciation-succession`.
- Migration 219 INSERT règle CONTEXTUAL `succession_envisagee` (déjà existant via F-200) pour `acceptation-renonciation-succession`, priority 97.

## Hors périmètre
- Frontend (SF-210-04).
- Calcul fiscal des droits de succession.

## Analyse de cohérence transversale
- **Outils décisionnels Famille FR succession existants** : `F-FA-24-devolution-legale`, `F-FA-24-testament-validite`, `F-FA-24-donation`, `F-FA-24-reserve-heriditaire`, `F-FA-24-partage-successoral`, `F-FA-24-indivision-successorale`, `F-FA-24-rapport-succession`. Aucun ne couvre le **choix d'option successorale** (gap rang 5 dans `audit-famille-fr-exhaustif.md`).
- **Pas d'override** sur outils existants.
- **Réutilise** le flag pivot `succession_envisagee` (livré par F-200 SF-200-01).

## Nouveau pattern UI ou service partagé
- Aucun.

## Impact par domaine métier
- **Famille FR** : central. Outil pivot succession après détection `succession_envisagee`.
- **Famille BE** : non applicable directement (CC BE art. 786 et s. — règles voisines mais délais différents 3 mois + 3 mois). Outil BE équivalent à ouvrir au backlog Famille BE.
- **Travail/Immigration FR/BE** : non applicable.

## Audit "Impact F-166 cross-C×D"
- FRxFamille : nouvelle entrée CONTEXTUAL `succession_envisagee → acceptation-renonciation-succession`, priority 97. Apparaît dès qu'une succession est détectée (cohérence avec les 7 outils F-FA-24-*).
- Tous autres C×D : aucun impact.

## Audit "exhaustivité droit national FR"
- Source : Cciv art. 768+ (3 options) + art. 771 (délai 4 mois 1er rang) + art. 772 (délai 2 mois supplémentaires 2e rang) + art. 783 (acceptation tacite par actes) + art. 791 (effet de l'acceptation à concurrence de l'actif).
- BE : règles voisines mais distinctes (CC BE art. 784-787, délais 3+3 mois). Outil BE équivalent à créer séparément (signalé hors-scope F-210, à porter au backlog Famille BE — règle "un outil = une situation métier" + "Belgique never forget").

## Contrat API
- `POST /api/v1/case-files/{caseFileId}/acceptation-renonciation-succession`
- Body :
  ```json
  {
    "dateOuvertureSuccession": "2026-04-01",
    "qualiteHeritier": "PREMIER_RANG",
    "actifBrutEur": 250000,
    "passifEur": 50000,
    "actesEquivalentAcceptationDejaPosesDetected": false,
    "inventaireRealise": false,
    "dettesIncertainesDetected": false,
    "intentionExprimee": "INCERTAIN"
  }
  ```
- Réponse : `caseFileId`, `dateOuvertureSuccession`, `qualiteHeritier`, `actifBrutEur`, `passifEur`, `actesEquivalentAcceptationDejaPosesDetected`, `inventaireRealise`, `dettesIncertainesDetected`, `intentionExprimee`, `optionsOuvertes` (List<String>), `optionRecommandee` (String), `delaiRestantJours` (int, peut être négatif), `delaiTotalJours` (int — 120 ou 180), `baseJuridique`, `formule`, `messages` (List<String>), `country`.
- 400 si dossier non FR, dossier non DROIT_FAMILLE, body invalide.
