# SF-FA-21-01 — Séparation de corps + conversion en divorce (backend)

## Objectif (1 phrase)

Exposer un outil décisionnel backend (POST + GET `/api/v1/case-files/{id}/separation-corps`) qui évalue si une séparation de corps prononcée est convertible en divorce (art. 296 et s. + 306 Code civil français + loi 26/05/2004), avec scoring 0-100 et verdict `POSSIBLE` / `PREMATUREE` / `RECONCILIATION_BLOQUE`.

## Périmètre

Outil décisionnel **single-country FRANCE**, **DROIT_FAMILLE** uniquement.
La séparation de corps belge a un régime distinct (art. 213 et s. Code civil BE) — non couverte ici (à ouvrir au backlog si besoin métier émerge).

Le calculateur évalue **deux questions** corrélées par la même procédure :
1. La séparation de corps est-elle valablement prononcée selon l'un des 4 modes (art. 296 + 233 + 242 + 237 Cciv) ?
2. Est-elle convertissable en divorce art. 306 Cciv (délai 2 ans + absence de réconciliation) ?

## Comportement nominal

1. L'avocat saisit le mode de procédure choisi (`CONSENTEMENT_MUTUEL` / `ACCEPTATION_PRINCIPE` / `FAUTE` / `ALTERATION_DEFINITIVE`).
2. Il renseigne la date du jugement de séparation (si déjà prononcée), la durée écoulée depuis, et les facteurs de conversion (consentement mutuel à la conversion, demande de réconciliation, présence d'enfants mineurs, patrimoine commun).
3. Le service calcule :
   - `dureeSeparationOk` = `dureeSeparationAnnees ≥ 2` (art. 306 al. 1 Cciv)
   - `delaiConversion2AnsAtteint` = idem
   - `conversionAutomatiquePossible` = `dureeSeparationOk && !demandeReconciliationFormulee`
   - `scoreEligibiliteConversion` (0-100) :
     - +40 si délai 2 ans atteint
     - +25 si consentement mutuel à la conversion
     - +20 si pas de demande de réconciliation formulée
     - +15 si pas d'enfants mineurs **OU** consentement mutuel à la conversion (la présence d'enfants ne bloque pas si les époux sont d'accord)
   - `verdictConversion` :
     - `RECONCILIATION_BLOQUE` si `demandeReconciliationFormulee` (priorité — bloque la conversion art. 305 Cciv)
     - `PREMATUREE` si délai 2 ans non atteint
     - `POSSIBLE` si score ≥ 70
     - sinon `PREMATUREE` (cas par défaut quand délai non atteint et autres facteurs)
4. L'analyse est persistée 1:1 par dossier (table `separation_corps_analyses`).
5. GET retourne la dernière analyse persistée ou 404 si absente.

## Cas d'erreur

- Workspace pays ≠ FRANCE → 400 (régime FR uniquement)
- Dossier `legal_domain` ≠ DROIT_FAMILLE → 400
- Dossier dans un autre workspace → 404
- `modeProcedure` null/inconnu → 400
- `dureeSeparationAnnees` négatif → 400
- `dateJugementSeparationCorps` ou `dateRequeteConversion` futures → 400
- `enfantsMineurs` négatif → 400

## Critères d'acceptation

- [x] Endpoint `POST /api/v1/case-files/{id}/separation-corps` upsert l'analyse.
- [x] Endpoint `GET /api/v1/case-files/{id}/separation-corps` retourne l'analyse ou 404.
- [x] Gate workspace FRANCE → 400 si BE.
- [x] Gate domaine DROIT_FAMILLE → 400 sinon.
- [x] Isolation workspace : 404 si dossier d'un autre workspace.
- [x] Verdict `POSSIBLE` ssi score ≥ 70 ET pas de réconciliation ET délai 2 ans atteint.
- [x] Verdict `RECONCILIATION_BLOQUE` quand `demandeReconciliationFormulee`.
- [x] Verdict `PREMATUREE` quand délai non atteint.
- [x] `baseJuridique` contient "296" et "306".
- [x] Migration Liquibase 154 crée table + visibility rule (priority 83, ALWAYS_ON, FRANCE, DROIT_FAMILLE).
- [x] ≥ 14 tests unitaires (Calculator)
- [x] ≥ 8 tests d'intégration (Controller)

## Plan de test minimal

### Tests unitaires (Calculator) — 14+
1. mode CONSENTEMENT_MUTUEL + 3 ans + consentement conversion + pas réconciliation → POSSIBLE, score 100
2. mode ACCEPTATION_PRINCIPE + 2 ans + consentement → POSSIBLE
3. mode FAUTE + 2 ans + pas consentement → POSSIBLE (score 60+ via délai+pas réconciliation+pas enfants)
4. mode ALTERATION_DEFINITIVE + 5 ans + consentement → POSSIBLE score 100
5. délai < 2 ans → `dureeSeparationOk` false, verdict PREMATUREE
6. réconciliation formulée → verdict RECONCILIATION_BLOQUE quel que soit le délai
7. délai = 2 ans pile → ok
8. enfants mineurs sans consentement → score retire le +15 enfants mais autres restent
9. enfants mineurs avec consentement → +15 conservé
10. `baseJuridique` contient art. 296+306+loi 2004
11. `formule` contient mode et durée et verdict
12. `messages` contiennent référence art. 306 et conversion automatique
13. validation : durée négative → throws
14. validation : enfants négatifs → throws
15. validation : date jugement future → throws
16. validation : date conversion antérieure à date jugement → throws

### Tests d'intégration (Controller) — 8+
1. POST FR nominal CONSENTEMENT_MUTUEL → 200 + score
2. POST FR mode FAUTE → 200
3. POST réconciliation formulée → verdict RECONCILIATION_BLOQUE
4. POST workspace BE → 400
5. POST workspace DT → 400
6. POST autre workspace → 404
7. POST sans modeProcedure → 400
8. POST upsert (POST 2 fois) → second remplace
9. GET après POST → renvoie persisted
10. GET sans POST → 404

## Tables / endpoints / composants impactés

- **Nouvelle table** : `separation_corps_analyses` (1:1 case_file)
- **Nouveau endpoint** : `POST + GET /api/v1/case-files/{id}/separation-corps`
- **Migration** : `154-create-separation-corps-analyses.xml`
- **Visibility F-IA-04** : `f1a04001-0000-0000-0000-ee00000fa211`, ALWAYS_ON, FRANCE, DROIT_FAMILLE, priority 83

## Hors périmètre

- Frontend (sera SF-FA-21-02)
- Belgique (régime distinct art. 213 Code civil BE — backlog si besoin métier)
- Génération de l'acte de conversion (art. 1136 et s. CPC) — outil documentaire séparé
- Liaison automatique vers F-FA-08 (altération) ou F-FA-10 (consentement mutuel) post-conversion

## Analyse de cohérence transversale

| Cible | Statut | Justification |
|---|---|---|
| Outils décisionnels FR DROIT_FAMILLE existants (F-FA-08/09/10/11/13/15/19/20/22/25) | non applicable | Outil indépendant — chaque outil décisionnel = une situation métier distincte (invariant produit). La conversion ne déclenche pas automatiquement les autres outils, l'avocat les invoque ensuite manuellement. |
| Belgique — équivalent | backlog | Le régime BE de la séparation de corps (art. 213 et s. Code civil BE) suit une procédure distincte (juge de paix), pas la même mécanique de conversion 2 ans. À ouvrir comme feature jumelle si besoin métier. |
| Préoccupation auth/Principal | non applicable | Réutilise OidcUser + Principal comme tous les outils famille existants. |
| Préoccupation workspace context | intégrée | Filtre `workspaceMemberRepository.findByUserAndPrimaryTrue` identique aux autres calculators. |
| Visibility F-IA-04 | intégrée | INSERT règle ALWAYS_ON dans la même migration. |

## Impact par domaine métier

Cette feature est **sensible au domaine** : strictement DROIT_FAMILLE.
Elle ne s'applique pas au DROIT_DU_TRAVAIL ni au DROIT_IMMIGRATION (mécanisme de procédure matrimoniale typique du droit de la famille).
Elle ne s'applique qu'en France (loi 26/05/2004 + art. 296-306 Cciv français). Régime BE équivalent en backlog.

## Parité des domaines métier

L'outil livré est de **niveau 5 (scoring/analyse de validité)**.
- Droit du travail FR/BE : **non applicable** — la séparation de corps est une procédure matrimoniale propre au droit de la famille.
- Droit immigration FR/BE : **non applicable** — concept étranger au droit des étrangers.
- Droit famille FR : **livré ici**.
- Droit famille BE : **backlog** — régime distinct (art. 213 et s. Code civil BE), à instruire comme feature jumelle si besoin client.

## Nouveau pattern UI ou service partagé

Aucun nouveau service/DTO partagé. Le DTO `SeparationCorpsRequest/Response` est local, pattern strictement identique à `DivorceAccepteRequest/Response`. Aucune dette de convergence introduite.

## Contrat API (figé pour SF-FA-21-02 frontend)

### Request `POST /api/v1/case-files/{caseFileId}/separation-corps`

```json
{
  "modeProcedure": "ACCEPTATION_PRINCIPE",
  "dateJugementSeparationCorps": "2024-01-15",
  "dateRequeteConversion": null,
  "dureeSeparationAnnees": 3,
  "consentementMutuelConversion": true,
  "patrimoineCommun": true,
  "enfantsMineurs": 1,
  "demandeReconciliationFormulee": false
}
```

`modeProcedure` enum : `CONSENTEMENT_MUTUEL` (art. 296), `ACCEPTATION_PRINCIPE` (art. 233 par renvoi), `FAUTE` (art. 242), `ALTERATION_DEFINITIVE` (art. 237).

### Response

```json
{
  "caseFileId": "uuid",
  "modeProcedure": "ACCEPTATION_PRINCIPE",
  "dateJugementSeparationCorps": "2024-01-15",
  "dateRequeteConversion": null,
  "dureeSeparationAnnees": 3,
  "consentementMutuelConversion": true,
  "patrimoineCommun": true,
  "enfantsMineurs": 1,
  "demandeReconciliationFormulee": false,
  "country": "FRANCE",
  "dureeSeparationOk": true,
  "delaiConversion2AnsAtteint": true,
  "conversionAutomatiquePossible": true,
  "scoreEligibiliteConversion": 90,
  "verdictConversion": "POSSIBLE",
  "baseJuridique": "Art. 296 et s. + 306 Code civil + Loi n° 2004-439 du 26/05/2004",
  "formule": "Séparation de corps prononcée + 3 ans + consentement mutuel → conversion possible art. 306",
  "messages": ["..."]
}
```

`verdictConversion` enum : `POSSIBLE`, `PREMATUREE`, `RECONCILIATION_BLOQUE`.

### Codes d'erreur

| Code | Cas |
|---|---|
| 400 | Workspace BE, domaine non DROIT_FAMILLE, modeProcedure manquant/inconnu, dates futures, durée négative |
| 404 | Dossier inexistant ou autre workspace, GET sans analyse persistée |
