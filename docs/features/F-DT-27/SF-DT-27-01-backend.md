# Mini-spec — F-DT-27 / SF-DT-27-01 Motif grave BE — validité délais + indemnisation — BACKEND

## Identifiant
`F-DT-27 / SF-DT-27-01`

## Feature parente
`F-DT-27` — Licenciement pour motif grave BE (critique 🔴)

## Statut `draft` · Date `2026-04-24` · Branche `feat/SF-DT-27-01-motif-grave-be-backend`

---

## Objectif

Outil décisionnel dédié à la **validité procédurale** du licenciement pour motif grave en Belgique (art. 35 Loi 03/07/1978 sur les contrats de travail) : vérifie le respect des deux délais de 3 jours ouvrables + calcule l'indemnité compensatoire de préavis due si le motif grave est procéduralement invalide.

**Spécificité** : outil **single-country** (BE uniquement — l'équivalent FR est le licenciement pour faute grave, procédure différente, déjà couverte par F-DT-01 et F-DT-04 et F-DT-08).

---

## Comportement

### Règles (art. 35 Loi 03/07/1978)

1. La partie qui souhaite invoquer le motif grave doit **notifier la rupture dans les 3 jours ouvrables** suivant la connaissance du fait.
2. Elle doit ensuite **notifier les motifs par recommandé dans les 3 jours ouvrables** suivant la rupture (soit maximum 6 jours ouvrables après la connaissance du fait).
3. **Si l'un des délais n'est pas respecté** → le motif grave est **procéduralement invalide** :
   - Indemnité compensatoire de préavis due (calculée selon loi 26/12/2013)
   - Possibilité d'indemnité pour licenciement manifestement déraisonnable (CCT 109 → fourchette jusqu'à 17 semaines)
4. **Si les délais sont respectés** + fait avéré → motif grave valable → zéro préavis, zéro indemnité de rupture.

### Inputs

- `dateConnaissanceFait` : LocalDate (jour de la découverte de la faute)
- `dateNotificationRupture` : LocalDate (jour de notification de la rupture)
- `dateNotificationMotifs` : LocalDate (jour d'envoi du recommandé motivé)
- `anciennetteAnnees` : int ≥ 0 (pour calculer le préavis indemnisé si invalide)
- `salaireMensuelReference` : BigDecimal > 0

### Calcul des jours ouvrables (jours ouvrés lundi-samedi, sauf jours fériés légaux)

Pour simplifier, la SF utilise une approximation : jours ouvrables = tous les jours sauf samedi/dimanche ET 10 jours fériés belges standard (1/1, lundi de Pâques, 1/5, Ascension, lundi de Pentecôte, 21/7, 15/8, 1/11, 11/11, 25/12). Jour de Pâques = jour férié supplémentaire. Pour une année donnée, utiliser une fonction utilitaire.

### Outputs

- `delaiRuptureJoursOuvrables` : int (jours écoulés entre dateConnaissance et dateNotificationRupture, exclusion du jour de départ)
- `delaiMotifsJoursOuvrables` : int (idem entre dateNotificationRupture et dateNotificationMotifs)
- `motifGraveProceduralementValide` : boolean (`delaiRuptureJoursOuvrables ≤ 3 && delaiMotifsJoursOuvrables ≤ 3`)
- `indemnitePreavisSiInvalide` : BigDecimal
  - Si invalide : appliquer la table belge de préavis selon ancienneté (loi 26/12/2013, approximation : ancienneté × 3 semaines par année dans les 5 premières années, puis 2 semaines/an ; plafond pratique pour la simplification : max 62 semaines → ~14.3 mois → `min(anciennetteAnnees × 3, 62) / 4.33` mois)
  - Si valide : 0
- `indemniteManifestementDeraisonnableMin` / `Max` (si invalide) : fourchette 3 à 17 semaines × salaire_hebdomadaire (salaire_mensuel ÷ 4.33) — CCT 109
- `formule` : texte
- `baseJuridique` : "Art. 35 Loi 03/07/1978 sur les contrats de travail + CCT 109 (si invalide)"
- `messages` : liste

### Cas d'erreur
- `dateNotificationRupture` avant `dateConnaissanceFait` → 400
- `dateNotificationMotifs` avant `dateNotificationRupture` → 400
- salaire ≤ 0 → 400
- ancienneté < 0 → 400
- Workspace FRANCE → 400 "Motif grave procédure BE uniquement — en France voir F-DT-08/F-DT-01"
- Dossier autre domaine → 400
- Workspace étranger → 404

---

## Contrat API

### POST `/api/v1/case-files/{caseFileId}/motif-grave-be`

**Request :**
```json
{
  "dateConnaissanceFait": "2026-03-01",
  "dateNotificationRupture": "2026-03-05",
  "dateNotificationMotifs": "2026-03-08",
  "anciennetteAnnees": 5,
  "salaireMensuelReference": 3500.00
}
```

**Response :**
```json
{
  "caseFileId": "uuid",
  "dateConnaissanceFait": "2026-03-01",
  "dateNotificationRupture": "2026-03-05",
  "dateNotificationMotifs": "2026-03-08",
  "anciennetteAnnees": 5,
  "salaireMensuelReference": 3500.00,
  "delaiRuptureJoursOuvrables": 4,
  "delaiMotifsJoursOuvrables": 3,
  "motifGraveProceduralementValide": false,
  "indemnitePreavisSiInvalide": 12125.00,
  "indemniteManifestementDeraisonnableMin": 2424.00,
  "indemniteManifestementDeraisonnableMax": 13744.00,
  "formule": "…",
  "baseJuridique": "Art. 35 Loi 03/07/1978",
  "messages": ["…"]
}
```

### GET même réponse (404 si pas de POST préalable).

---

## Architecture

Pattern standard. Table `motif_grave_be_analyses` (migration 115). Tool_id `F-DT-27-motif-grave-be`. **1 règle visibility ALWAYS_ON BELGIQUE uniquement** (pas FR — c'est un outil spécifique BE). UUID `f1a04001-0000-0000-0000-ee0000000271`, priority 56.

### Composants à créer

- `MotifGraveBeCalculator.java` (logique pure, calcul jours ouvrables + validation délais + indemnisation)
- `MotifGraveBeAnalysis.java` (entity avec colonnes dates + ancienneté + salaire + result_data)
- `MotifGraveBeRepository.java`
- `MotifGraveBeRequest/Response/Result.java`
- `MotifGraveBeService.java` (gate country == BELGIQUE explicite)
- `MotifGraveBeController.java`
- Migration `115-create-motif-grave-be-analyses.xml`

## Plan de test

### UT (`MotifGraveBeCalculatorTest`)
- Cas valide : délais 2j et 2j → motifGraveProceduralementValide=true, indemnité=0
- Cas invalide délai rupture : 4j et 2j → false, indemnité préavis > 0
- Cas invalide délai motifs : 2j et 4j → false
- Cas limite délais = 3j exactement → valide (≤ 3)
- Cas invalide : weekend non compté (1/3 samedi → lundi = 1 jour ouvrable)
- Cas invalide : jour férié non compté (1er mai)
- Validation : dateNotifRupture avant dateConnaissance → IllegalArgumentException
- Validation : dateNotifMotifs avant dateNotifRupture → IllegalArgumentException
- Validation : salaire ≤ 0 ou ancienneté < 0 → IllegalArgumentException
- Fourchette indemnité manifestement déraisonnable (3 à 17 semaines)

### IT (`MotifGraveBeControllerIT`)
- POST BE workspace nominal valide → 200
- POST BE workspace invalide → 200 avec indemnité non nulle
- POST workspace FRANCE → 400
- POST dossier immigration → 400
- Workspace étranger → 404
- Upsert
- GET après POST
- GET sans POST → 404

---

## Impact domaine

DROIT_DU_TRAVAIL BE uniquement. Équivalent FR = F-DT-08 (validité licenciement faute grave, délai 2 mois L.1332-4). Pas d'extension FR car procédures différentes.

**Justification absence FR** : l'équivalent français de la procédure pour faute grave est déjà couvert par F-DT-08 (validité disciplinaire) et F-DT-04 (fiche prud'homale). Le délai de prescription française (L.1332-4) est de 2 mois entre connaissance et notification, donc très différent des 3 jours ouvrables belges.

## Parité niveau ≥5

Niveau 3 (calculateur + arbre décisionnel simple). Parité N/A.

## Critères d'acceptation

- [ ] Calcul jours ouvrables excluant samedi/dimanche + jours fériés BE.
- [ ] Cas valide : délais ≤ 3 + ≤ 3 → `motifGraveProceduralementValide=true`.
- [ ] Cas invalide : dépassement d'un délai → indemnité préavis calculée (table loi 26/12/2013 approximée).
- [ ] Cas invalide : fourchette indemnité manifestement déraisonnable CCT 109 retournée [3 sem, 17 sem].
- [ ] Gate country BELGIQUE strict.
- [ ] Isolation workspace + gate DROIT_DU_TRAVAIL.
- [ ] Migration 115 + 1 règle visibility BELGIQUE only.
- [ ] +14 UT + 8 IT verts.

## Hors scope

- Frontend (SF-DT-27-02 ultérieure).
- Calcul exact de la table préavis belge (simplifié ici) — peut être raffiné avec le référentiel CP belge étendu F-129 SF-129-04 (déjà mergée).
- Discussion du caractère grave substantiel de la faute (question de fond) — hors scope (outil procédural).

## Notes

- Les jours fériés belges sont calculés annuellement par utilitaire. Pâques utilise l'algorithme de Meeus/Jones/Butcher.
- Pour la SF MVP, utiliser une liste statique des 10 jours fériés fixes + calcul algorithmique Pâques/Ascension/Pentecôte via `LocalDate`.
- Alternative future : externaliser dans `legal_referentials` type `JOURS_FERIES_BE`.
