# Mini-spec — F-IM-08 / SF-IM-08-05 Annexe 13 (OQT BE) — BACKEND

## Identifiant
`F-IM-08 / SF-IM-08-05`

## Feature parente
`F-IM-08` — OQTF et contentieux éloignement

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-IM-08-05-annexe13-be-backend`

## Objectif

Outil décisionnel dédié à l'**ordre de quitter le territoire belge (OQT)** notifié par annexe 13 de l'AR du 08/10/1981 d'exécution de la Loi du 15/12/1980 sur les étrangers. Équivalent belge de l'OQTF française mais procédure juridiquement distincte : recours devant le **Conseil du contentieux des étrangers (CCE)** dans un délai de 30 jours.

**Single-country BE** — invariant "un outil = une situation métier" respecté.

## Comportement

### Règles (Loi 15/12/1980 + AR 08/10/1981)

- **Annexe 13** : décision administrative notifiée par l'Office des étrangers (OE) ou la commune, imposant le départ du territoire belge dans un délai (souvent 30 jours, parfois 0 jours si risque de fuite ou OQT précédente).
- **Recours suspensif en extrême urgence** (art. 39/82 Loi 15/12/1980) devant le CCE : **5 jours ouvrables** si mesure d'éloignement imminente (transfert imminent), sinon recours en annulation classique **30 jours** (non suspensif automatique mais demande de suspension possible).
- **Recours en annulation** (art. 39/2 §2) : 30 jours de calendrier.
- Pour simplification : focaliser sur le délai 30 jours (cas standard). Message signale la voie extrême urgence 5 jours ouvrables si transfert imminent.

### Inputs

- `dateNotificationAnnexe13` : LocalDate
- `delaiDepartImposeJours` : int (souvent 30, parfois 0, 7, 15)
- `motifOqt` : enum :
  - `SEJOUR_IRREGULIER_ART_7` (art. 7 Loi 15/12/1980)
  - `REFUS_SEJOUR_APRES_DEMANDE` (refus carte A suite demande titre)
  - `FIN_SEJOUR_REGULIER` (expiration non renouvelée)
  - `AUTRE`
- `transfertImminent` : boolean (déclenche recours extrême urgence 5 jours ouvrables)
- `recoursForme` : boolean
- `dateRecours` : LocalDate nullable
- `typeRecours` : enum nullable — `ANNULATION_30J` / `EXTREME_URGENCE_5JO`

### Outputs

- `dateExpirationDelaiDepart` = notification + delaiDepartImposeJours
- `dateExpirationRecoursAnnulation` = notification + 30 jours (calendaires)
- `dateExpirationRecoursExtremeUrgence` = notification + 5 jours ouvrables (via `BelgianBusinessDaysCalculator`)
- `joursRestantsAvantExpirationAnnulation` : long
- `statutRecoursAnnulation` : DISPONIBLE / URGENT (≤3j) / EXPIRE / RECOURS_FORME
- `dateAudiencePrevisionnelle` = recours + 30 jours (si recours formé — estimation)
- `dateDecisionPrevisionnelle` = recours + 90 jours (moyenne)
- `referedDisponibles` :
  - `DEMANDE_SUSPENSION_ART_39_2` (toujours)
  - `RECOURS_EXTREME_URGENCE_39_82` si transfertImminent
- `formule`
- `baseJuridique` : "Loi 15/12/1980 art. 7 + art. 39/2 §2 + annexe 13 AR 08/10/1981"
- `messages` différenciés

### Messages

- "Recours devant le Conseil du contentieux des étrangers (CCE) — pas de demande d'asile à introduire, c'est un recours distinct."
- "Recours en annulation : 30 jours calendaires (art. 39/2 §2). Pas d'effet suspensif automatique — demander la suspension séparément (art. 39/82)."
- Si `transfertImminent=true` : "**URGENT** : recours en extrême urgence (art. 39/82 §4) à former dans les 5 jours ouvrables. Décision CCE sous 72h."
- Si `motifOqt = FIN_SEJOUR_REGULIER` : "Contester en prouvant le droit au séjour ou demander renouvellement avant expiration."
- "Aide juridique 2e ligne : vérifier éligibilité (revenus + procédure BAJ), pro deo possible."

### Cas d'erreur
- dateNotification future → 400
- delaiDepartImposeJours < 0 → 400
- motifOqt null → 400
- recoursForme=true sans dateRecours ou typeRecours → 400
- Workspace FRANCE → 400 "Annexe 13 procédure BE uniquement — en France voir OQTF (SF-IM-08-01/03)"
- Dossier autre domaine → 400
- Workspace étranger → 404

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/annexe13-be`

**Request :**
```json
{
  "dateNotificationAnnexe13": "2026-04-01",
  "delaiDepartImposeJours": 30,
  "motifOqt": "SEJOUR_IRREGULIER_ART_7",
  "transfertImminent": false,
  "recoursForme": false,
  "dateRecours": null,
  "typeRecours": null
}
```

**Response :** tous les champs outputs ci-dessus.

## Architecture

Pattern Discrimination/MotifGraveBe (single-country BE). Réutilise `BelgianBusinessDaysCalculator` (créé par SF-DT-27-01).

Table `annexe13_be_analyses` (migration 118). Tool_id `F-IM-08-annexe13-be`. **Règle visibility CONTEXTUAL BE** sur trigger `type_procedure_detectee=ANNEXE_13_BE` (à ajouter à l'enum dans F-IM-16 — prévoir migration 118 pour ajouter ce code au prompt aussi).

UUID visibility : `f1a04001-0000-0000-0000-ee000000085a`, priority 59.

**Alternative** : si F-IM-16 doit étendre l'enum pour BE, on peut faire dans cette SF : ajouter `ANNEXE_13_BE`, `OQT_BE_TRANSFERT_IMMINENT` au prompt et à l'enum. Pour éviter scope creep, **laisser l'outil always-on pour workspace BE** (pas de trigger IA) pour cette SF. ALWAYS_ON BE simple.

**Décision** : règle visibility `ALWAYS_ON` pour BE (pas CONTEXTUAL — évite extension enum simultanée).

## Composants
- `Annexe13BeCalculator.java`
- `Annexe13BeAnalysis.java`
- `Annexe13BeRepository.java`
- `Annexe13BeRequest/Response/Result.java`
- `Annexe13BeService.java` (gate country==BELGIQUE)
- `Annexe13BeController.java`
- Migration `118-create-annexe13-be-analyses.xml`

## Tests
- Calc délai 30j + délai ouvrables 5j via BelgianBusinessDaysCalculator
- 4 motifs + messages différenciés
- transfertImminent=true → message extrême urgence
- 2 types de recours
- Validation + cross-country FR rejeté
- ≥14 UT + ≥8 IT

## Impact domaine

DROIT_IMMIGRATION BE. FR = SF-IM-08-01/03 distinct.

## Hors scope
- Frontend (SF-IM-08-06 parallèle)
- Recours extrême urgence comme outil distinct (peut rester message)
- CRA belge (centre fermé) — distinct, backlog
- Annexe 35 / 35bis / autres annexes — backlog
