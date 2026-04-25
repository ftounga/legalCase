# Mini-spec — F-FA-13 / SF-FA-13-01 Révisions post-divorce FR — BACKEND

## Objectif

Outil décisionnel **single-country FRANCE** d'évaluation de la révision d'une décision post-divorce sur 5 sujets clés : pension alimentaire (art. 209 Cciv), résidence alternée (art. 373-2-13 Cciv), droit de visite (art. 373-2-9 Cciv), prestation compensatoire (art. 279 Cciv) et déménagement parent (art. 373-2 Cciv). L'outil détermine si la **modification de circonstances** invoquée justifie une procédure de révision.

## Contexte juridique

- **Pension alimentaire (art. 209 Cciv)** : « Quand celui qui fournit ou celui qui reçoit des aliments est replacé dans un état tel que l'un ne puisse plus en donner ou que l'autre n'en ait plus besoin […], la décharge ou la réduction peut en être demandée ». Critère retenu en jurisprudence : modification substantielle des ressources / besoins (≥ 20 % usuellement).
- **Résidence (art. 373-2-13)** : modification possible « en cas d'éléments nouveaux ». Le JAF apprécie librement l'intérêt supérieur de l'enfant.
- **Droit de visite (art. 373-2-9)** : voix de l'enfant pris en compte (art. 388-1 Cciv) — pertinence accrue pour les enfants ≥ 13 ans (préadolescents).
- **Prestation compensatoire (art. 279 Cciv)** : révision uniquement par voie de procédure spécifique, conditions strictes (révision exceptionnelle des modalités de paiement, art. 276-3 / 280-1).
- **Déménagement parent (art. 373-2 al. 3)** : « Tout changement de résidence de l'un des parents […] doit faire l'objet d'une information préalable et en temps utile de l'autre parent ». Distance et impact sur le lien parental sont les critères clés.

## Comportement nominal

L'avocat saisit :
- le `typeRevision` parmi 5 valeurs ;
- la `dateDecisionInitiale` (date du jugement de divorce ou de la dernière révision) ;
- la `changementCirconstance` (description libre du fait nouveau) ;
- des champs spécialisés selon le type (revenus, mode de résidence, âge des enfants, etc.).

Le calculator :
1. Calcule un `ecartRevenusPct` quand pertinent (PENSION, PRESTATION_COMPENSATOIRE).
2. Détermine `modificationSubstantielle` : `true` si l'écart de revenus dépasse 20 % (PENSION/PRESTATION) ou si un critère métier dédié est rempli (RESIDENCE/DROIT_VISITE/DEMENAGEMENT).
3. Évalue `motivationSuffisante` : booléen calé sur la longueur/structure de `changementCirconstance` (≥ 30 caractères non vide).
4. Calcule un `scoreGlobal` 0–100 :
   - 50 points si `modificationSubstantielle`
   - 25 points si `motivationSuffisante`
   - 15 points si `ancienneteDecisionMois` ≥ 6 (modification ne peut pas être trop précoce sans abus)
   - 10 points bonus selon critère spécifique du type :
     - PENSION : enfants encore à charge (`nbEnfantsACharge ≥ 1`)
     - RESIDENCE : âge enfants compatible avec changement (≥ 6 ans)
     - DROIT_VISITE : enfant préadolescent (≥ 13 ans), voix prise en compte
     - PRESTATION_COMPENSATOIRE : ancienneté décision ≥ 12 mois
     - DEMENAGEMENT_PARENT : information préalable de l'autre parent constatée (champ `informationPrealable`)
5. Verdict : `ELEVEE` (score ≥ 75), `MOYENNE` (50–74), `FAIBLE` (< 50).
6. `baseJuridique` adaptée au `typeRevision` (5 articles distincts).
7. `formule` lisible avec valeurs clés.
8. `messages` pédagogiques rappelant la base juridique, la charge de la preuve et les pièges classiques.

## Cas d'erreur

- Workspace `country != FRANCE` → 400 « Outil propre au droit français ».
- Case file `legalDomain != DROIT_FAMILLE` → 400.
- `typeRevision` manquant ou inconnu → 400.
- `dateDecisionInitiale` postérieure à `now` → 400.
- `nbEnfantsACharge` négatif → 400.
- Revenus négatifs → 400.
- Case file inexistant ou autre workspace → 404.

## Critères d'acceptation

1. `POST /api/v1/case-files/{caseFileId}/revisions-post-divorce` retourne 200 + JSON avec score, verdict, modificationSubstantielle, baseJuridique adaptée pour chacun des 5 types.
2. Pour `PENSION_ALIMENTAIRE` : revenus initiaux 4000 → actuels 1600 ⇒ `ecartRevenusPct = -60`, `modificationSubstantielle = true`.
3. Verdict `ELEVEE` quand modification substantielle + motivation ≥ 30 chars + ancienneté ≥ 6 mois + critère spécifique rempli.
4. Workspace BE est rejeté en 400.
5. Domain travail/immigration est rejeté en 400.
6. Repository : 1 ligne par dossier (upsert) — uniqueness contrainte `case_file_id`.
7. Migration Liquibase 135 idempotente avec rollback complet (drop table + delete visibility rule).
8. Visibility rule `decision_tool_visibility_rules` : ALWAYS_ON, FRANCE, DROIT_FAMILLE, priority 73, tool_id `F-FA-13-revisions-post-divorce`, UUID `f1a04001-0000-0000-0000-ee00000fa131`.

## Plan de test minimal

**Unitaires (≥ 18, ≥ 3 par typeRevision × 5 + edges)**
- 5 tests « nominal_ELEVEE » par type (un par type).
- 5 tests « modification non substantielle ⇒ score réduit ».
- 5 tests « base juridique correcte » par type.
- Edges : type inconnu (throw), nbEnfantsACharge négatif (throw), revenus négatifs (throw), date future (throw), motivation vide (booléen false).
- Verdict thresholds (FAIBLE/MOYENNE/ELEVEE).

**Intégration (≥ 10)**
- POST FR nominal pour PENSION_ALIMENTAIRE.
- POST FR pour chacun des 5 types (5 tests).
- POST workspace BE → 400.
- POST workspace FR DROIT_DU_TRAVAIL → 400.
- POST autre workspace → 404.
- POST upsert remplace.
- GET après POST renvoie persistance.
- GET sans POST → 404.

**Isolation workspace** : couverte via tests `POST_otherWorkspace_returns404` (pattern F-FA-09).

## Tables / endpoints / composants impactés

- **Nouvelle table** : `revisions_post_divorce_analyses` (1:1 case_file).
- **Nouvelle migration** : `135-create-revisions-post-divorce-analyses.xml`.
- **Insert** : `decision_tool_visibility_rules` (UUID `f1a04001-0000-0000-0000-ee00000fa131`).
- **Endpoints** : `POST + GET /api/v1/case-files/{caseFileId}/revisions-post-divorce`.
- **Code** : `RevisionsPostDivorceCalculator`, `Service`, `Controller`, `Repository`, `Analysis`, `Request`, `Response`, `Result`, `TypeRevision` enum, `ModeResidence` enum.

## Contrat API (FIGÉ pour SF-FA-13-02 frontend)

### POST + GET `/api/v1/case-files/{caseFileId}/revisions-post-divorce`

**Request** (POST) :
```json
{
  "typeRevision": "PENSION_ALIMENTAIRE",
  "dateDecisionInitiale": "2024-06-15",
  "changementCirconstance": "Perte d'emploi du débiteur depuis 6 mois, baisse 60% revenus",
  "revenusInitialsDebiteurEur": 4000.00,
  "revenusActuelsDebiteurEur": 1600.00,
  "revenusInitialsCreancierEur": 2000.00,
  "revenusActuelsCreancierEur": 2200.00,
  "nbEnfantsACharge": 2,
  "ageEnfants": [10, 14],
  "modeResidenceActuel": "EXCLUSIVE_MERE",
  "modeResidenceDemande": "ALTERNEE",
  "informationPrealable": null,
  "distanceDemenagementKm": null
}
```

`typeRevision` enum (string, requis) : `PENSION_ALIMENTAIRE`, `RESIDENCE`, `DROIT_VISITE`, `PRESTATION_COMPENSATOIRE`, `DEMENAGEMENT_PARENT`.

`modeResidenceActuel` / `modeResidenceDemande` enum (string, optionnels) : `ALTERNEE`, `EXCLUSIVE_MERE`, `EXCLUSIVE_PERE`, `LIBRE`.

Champs facultatifs selon `typeRevision` :
- `PENSION_ALIMENTAIRE` : revenus * 4 + nbEnfantsACharge requis pour calcul utile (sinon score réduit).
- `PRESTATION_COMPENSATOIRE` : revenus * 4 requis pour calcul utile.
- `RESIDENCE` / `DROIT_VISITE` : `ageEnfants` + `modeResidenceActuel`/`modeResidenceDemande`.
- `DEMENAGEMENT_PARENT` : `distanceDemenagementKm` (optionnel) + `informationPrealable` (Boolean optionnel).

**Response** :
```json
{
  "caseFileId": "uuid",
  "typeRevision": "PENSION_ALIMENTAIRE",
  "dateDecisionInitiale": "2024-06-15",
  "changementCirconstance": "Perte d'emploi du débiteur depuis 6 mois, baisse 60% revenus",
  "revenusInitialsDebiteurEur": 4000.00,
  "revenusActuelsDebiteurEur": 1600.00,
  "revenusInitialsCreancierEur": 2000.00,
  "revenusActuelsCreancierEur": 2200.00,
  "nbEnfantsACharge": 2,
  "ageEnfants": [10, 14],
  "modeResidenceActuel": "EXCLUSIVE_MERE",
  "modeResidenceDemande": "ALTERNEE",
  "informationPrealable": null,
  "distanceDemenagementKm": null,
  "country": "FRANCE",
  "ecartRevenusPct": -60,
  "ancienneteDecisionMois": 22,
  "modificationSubstantielle": true,
  "motivationSuffisante": true,
  "scoreGlobal": 100,
  "verdictRevisionPossible": "ELEVEE",
  "baseJuridique": "Art. 209 Cciv (pension alimentaire) + jurisprudence Cass. 1ère civ.",
  "formule": "Baisse 60% revenus débiteur = modification substantielle (>20%)",
  "messages": ["..."]
}
```

`verdictRevisionPossible` (string) : `ELEVEE` (≥ 75), `MOYENNE` (50–74), `FAIBLE` (< 50).

**Codes erreur**
- 400 « typeRevision est requis »
- 400 « typeRevision inconnu : XXX »
- 400 « Outil propre au droit français »
- 400 « Ce dossier n'est pas un dossier de droit de la famille »
- 400 « dateDecisionInitiale ne peut être dans le futur »
- 400 « nbEnfantsACharge ne peut être négatif »
- 400 « revenus ne peuvent être négatifs »
- 404 case file introuvable / autre workspace.

## Hors scope

- Frontend (SF-FA-13-02 ultérieure).
- BE (n'a pas d'équivalent direct article 209 — couverture BE non prévue dans cette feature).
- Calcul automatique de la nouvelle pension/prestation : seul l'avocat reste compétent pour la quantification, l'outil indique seulement si la révision est juridiquement défendable.
- Génération automatique de la requête en révision (= F-FA-XX générateur ultérieur).

## Analyse de cohérence transversale

| Cible scannée | Statut |
|---|---|
| Outils décisionnels FA existants (F-FA-05/06/07/08/09/10) | Pattern réutilisé : single-country FR, scoring 0-100, verdict ELEVEE/MOYENNE/FAIBLE, gate FRANCE+DROIT_FAMILLE. |
| Mécanisme de révision côté autres domaines (DT/IM) | Non applicable : la révision post-divorce est spécifique au droit de la famille FR (art. 209). |
| Préoccupation transversale Auth/Principal | Pas de modif. Pattern `@AuthenticationPrincipal OidcUser` standard. |
| Préoccupation transversale Workspace | Aucune nouvelle résolution — ré-utilise `WorkspaceMemberRepository.findByUserAndPrimaryTrue`. |
| Préoccupation transversale Outil décisionnel | Outil neuf, single-situation (« révision post-divorce »). Le `typeRevision` est un paramètre métier (5 articles distincts du même chapitre), **pas un switch entre situations distinctes** — l'invariant « un outil = une situation » est respecté car les 5 types partagent la même logique de scoring (« modification de circonstances justifiant la révision »). Aucun outil jumeau à scinder. |
| Pattern frontend partagé | Aucun composant frontend dans cette SF (séquentiel). |

## Impact par domaine métier

- **Droit du travail** : non applicable.
- **Immigration** : non applicable.
- **Droit de la famille** : feature centrale (5 articles Cciv).
- **Pays** : FRANCE uniquement. La Belgique n'a pas d'équivalent direct article 209 — la révision en BE relève d'autres mécanismes (art. 301 § 7 CC pour pension après divorce ; art. 374 CC pour autorité parentale) et fera l'objet d'une feature jumelle au backlog si nécessaire (à signaler dans PRODUCT_SPEC évolutions).

## Parité des domaines métier (niveau ≥ 5 — scoring)

L'outil est un **scoring (niveau 5)**. La règle CLAUDE.md impose de lister la parité.

| Domaine | Équivalent | Statut |
|---|---|---|
| Droit du travail | Pas d'analogue direct (la révision d'un jugement prud'homal repose sur appel/cassation, pas sur « modification de circonstances »). | Non applicable. |
| Immigration | Demande de renouvellement / changement de statut = analogue partiel mais déjà couvert par F-IM-08 et suivants. | Couvert ailleurs. |
| Famille FRANCE | Ce SF. | Livré. |
| Famille BELGIQUE | Article 301 § 7 CC (pension après divorce révisable « en cas de circonstances nouvelles indépendantes de la volonté des parties ») — équivalent direct mais base juridique distincte. | **À ouvrir au backlog** : feature jumelle « F-FA-XX Révisions post-divorce BE » (à proposer dans le commit docs post-merge). Justification du report : la SF actuelle est volontairement single-country FR (les 5 articles cités sont Cciv FR ; le mécanisme BE est régi par des articles distincts du Code civil belge et nécessite scoring + critères dédiés). |

## Plan de migration

- **Migration 135** : create table `revisions_post_divorce_analyses` + insert visibility rule.
- Description visibility rule : non requise (la table `decision_tool_visibility_rules` n'est pas `legal_referentials`).
- Aucun référentiel `legal_referentials` ajouté → règle SF-140-03 non applicable.
