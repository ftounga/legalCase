# Mini-spec — F-294 / SF-294-02 — Travail BE : contenu `EXPECTED_PIECES`

> Étape 1. Étape 0 couverte par `SF-294-00-coherence.md` (mécanisme F-294). Jumelle de SF-294-01 (Travail FR) pour le **contenu belge**, validé par le PO le 2026-06-16 (liste sourcée SPF Emploi / ONEM / Acerta / Wolters Kluwer). Backend pur.

---

## Identifiant

`F-294 / SF-294-02`

## Feature parente

`F-294` — Référentiel de pièces attendues par situation procédurale

## Statut

`draft`

## Branche Git

`feat/SF-294-02-travail-be-expected-pieces`

## Date de création

2026-06-16

---

## Objectif

> En une phrase.

Seeder le **contenu Travail BE** du référentiel `EXPECTED_PIECES` (14 pièces validées par le PO) + le fallback Java `TravailPieceReferentiel` BELGIQUE, pour que le socle de pièces attendues + la canonisation F-194 s'appliquent aux dossiers de **droit du travail belge**, sur le mécanisme déjà livré en SF-294-01 (inchangé).

---

## Comportement attendu

### Cas nominal

1. `getExpectedPieces("DROIT_DU_TRAVAIL", "BELGIQUE", procedureStage)` (déjà existant SF-294-01) résout désormais un socle non vide : DB-first `EXPECTED_PIECES` (legal_domain `DROIT_DU_TRAVAIL`, country `BELGIQUE`) filtré par stade, fallback Java `TravailPieceReferentiel.getPieces("BELGIQUE")`.
2. Filtrage par stade : pièces avec `stages` contenant le stade du dossier OU `stages` vide (générique). Stades BE de 1re instance = `FOND`, `REFERE` (tribunal du travail `TT`, cf. `ProcedureStageCatalog`).
3. Aval inchangé : injection dans `EnrichedAnalysisService`, canonisation dans `PieceManquanteAlignmentService`, overlay F-194, affichage F-289 — tout s'applique tel quel.

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| `value_json` malformé / exception | Fail-open : fallback Java puis liste vide ; run abouti (hérité SF-294-01) |
| Stade non couvert (ex. POURVOI) | Seules les pièces génériques remontent ; jamais d'erreur |

---

## Analyse de cohérence transversale

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `EXPECTED_PIECES` / `getExpectedPieces` / `TravailPieceReferentiel` | Oui | **Réutilisés** : ajout du contenu BE (DB seed + branche `BELGIQUE` du fallback Java). Mécanisme inchangé. |
| Travail FR (SF-294-01) | Non | Strictement inchangé (CA de non-régression). |
| Famille (SF-294-03) / Immigration | Non | Inchangés. |
| Auth / Workspace / Plans / Navigation | Non | Aucun. |

### Décision

- [x] Étendu à la cible (Travail BE).

---

## Conformité F-IA-04 / Pré-fill IA

- [x] **Non applicable** — SF backend pure (seed référentiel + fallback Java), pas d'outil décisionnel, pas d'écran.

---

## Critères d'acceptation

- [ ] **CA1** : un dossier **Travail BE** au stade `FOND` (ou `REFERE`) reçoit comme socle au minimum les pièces seedées rattachées à ce stade.
- [ ] **CA2 (générique)** : `REGLEMENT_TRAVAIL` et `CCT_APPLICABLE` (stages vides) remontent quel que soit le stade, y compris si `procedureStage` est nul.
- [ ] **CA3 (alignement Java↔DB)** : `TravailPieceReferentiel.getPieces("BELGIQUE")` renvoie EXACTEMENT les mêmes codes/labels/stages que le seed DB (DB-first prime, fallback identique).
- [ ] **CA4 (description avocat)** : chaque INSERT système porte une `description` en langage avocat (exigence readiness `legal_referentials` + `LegalReferentialDescriptionIntegrityIT`).
- [ ] **CA5 (non-régression FR)** : `getExpectedPieces("DROIT_DU_TRAVAIL","FRANCE",...)` inchangé ; build complet vert.
- [ ] **CA6 (additif/canonisation hérités)** : socle BE additif (union LLM ∪ socle) + canonisation par libellé exact (une pièce BE « obtenue » ne réapparaît pas).
- [ ] **CA7 (fail-open)** : exception → liste vide, run abouti.
- [ ] **CA8 (stades réels)** : les pièces sont rattachées à des stades existant dans `ProcedureStageCatalog` BE (`FOND/REFERE/APPEL/POURVOI`), pas à un stade fantôme.

---

## Valeurs initiales (contenu validé PO — 2026-06-16)

> `legal_domain='DROIT_DU_TRAVAIL'`, `country='BELGIQUE'`, `referential_type='EXPECTED_PIECES'`, `is_system=true`. `value_json = {"stages":[...]|absent, "obligatoire":bool, "ordre":int}`.

| `entry_key` | `label` | `stages` | oblig. | ordre |
|---|---|---|---|---|
| `CONTRAT_TRAVAIL` | Contrat de travail | FOND, REFERE | ✓ | 1 |
| `FICHES_PAIE` | Fiches de paie (dernières) | FOND, REFERE | ✓ | 2 |
| `LETTRE_CONGE` | Lettre de notification du congé (licenciement) | FOND, REFERE | ✓ | 3 |
| `C4` | Formulaire C4 (certificat de chômage) | FOND, REFERE | ✓ | 4 |
| `CERTIFICAT_TRAVAIL` | Certificat de travail | FOND, REFERE | ✓ | 5 |
| `DECOMPTE_SORTIE` | Décompte de sortie (dernières prestations) | FOND, REFERE | ✓ | 6 |
| `DECOMPTE_INDEMNITE_RUPTURE` | Décompte de l'indemnité de rupture | FOND, REFERE | ✓ | 7 |
| `ATTESTATION_VACANCES` | Attestation(s) de vacances | FOND, REFERE | ✓ | 8 |
| `FICHE_FISCALE_281_10` | Fiche fiscale 281.10 | FOND, REFERE | — | 9 |
| `MOTIVATION_CCT109` | Motivation du licenciement (CCT n°109) — demande + réponse | FOND | — | 10 |
| `NOTIFICATION_MOTIF_GRAVE` | Notification du motif grave (si applicable) | FOND, REFERE | — | 11 |
| `REGLEMENT_TRAVAIL` | Règlement de travail | (générique) | — | 12 |
| `CCT_APPLICABLE` | CCT / commission paritaire applicable | (générique) | — | 13 |
| `AVERTISSEMENTS_EVALUATIONS` | Avertissements / évaluations (dossier disciplinaire) | FOND | — | 14 |

> `description` (langage avocat) à rédiger pour chaque entrée au dev — courte, factuelle (ex. C4 : « Document ONEM confirmant la fin du contrat ; mentionne le motif de rupture, le préavis et l'éventuelle indemnité — pièce centrale du litige »).

---

## Technique

### Endpoint(s)

> Aucun.

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `legal_referentials` | INSERT (seed) | 14 entrées `EXPECTED_PIECES` BE. Colonnes alignées sur `048` (+`093` description). |

### Migration Liquibase

- [x] Oui — `NNN-f294-expected-pieces-travail-be.xml` (INSERT seul, plage UUID dédiée `f2940002-…` sans collision avec 607 `f2940001-…`, rollback). Vérifier noms de colonnes vs `048`.

### Backend — à modifier

- `TravailPieceReferentiel` : ajouter `BE_PIECES` (List<ExpectedPiece>) **alignée 1:1** avec le seed, et faire `getPieces("BELGIQUE")` la renvoyer (aujourd'hui `List.of()`).
- **Aucune autre modification** : `getExpectedPieces` (SF-294-01) gère déjà DROIT_DU_TRAVAIL × tout pays via DB + fallback.

---

## Plan de test

### Tests unitaires

- [ ] `TravailPieceReferentiel.getPieces("BELGIQUE")` renvoie les 14 pièces (codes/stages attendus) (CA3).
- [ ] Alignement Java↔seed : mêmes codes/labels/stages (test ou vérif).
- [ ] `getExpectedPieces("DROIT_DU_TRAVAIL","BELGIQUE","FOND")` → pièces FOND + génériques (CA1/CA2).
- [ ] `procedureStage` nul → génériques seulement (CA2).
- [ ] Non-régression FR (CA5).

### Tests d'intégration

- [ ] `ExpectedPiecesSeedIT` : dossier Travail BE `FOND` matérialise au minimum les pièces BE seedées (CA1) + `LegalReferentialDescriptionIntegrityIT` vert (CA4).

### Isolation workspace

- [x] Applicable (indirect) — entrées système non-tenant, override workspace via mécanisme `legal_referentials` existant.

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale**.
- [x] **Aucun smoke E2E**.

---

## Dépendances

- SF-294-01 (mécanisme + `TravailPieceReferentiel` + migration 607) — Done.
- Contenu BE validé PO (2026-06-16).

---

## Notes et décisions

- **Contenu validé PO** (2026-06-16), sourcé : SPF Emploi, ONEM (C4), Acerta, Wolters Kluwer. Les 8 premières pièces (documents sociaux + congé) obligatoires ; `MOTIVATION_CCT109` / `NOTIFICATION_MOTIF_GRAVE` conditionnelles (non obligatoires).
- **Stades** : pièces de litige rattachées à `FOND`/`REFERE` (1re instance `TT`), sur le modèle Travail FR (BCO/FOND/REFERE/DEPARTAGE) ; `REGLEMENT_TRAVAIL`/`CCT_APPLICABLE` génériques.
- **Codes communs FR/BE** (ex. `CONTRAT_TRAVAIL`) sans collision : unicité `legal_referentials` inclut `country`.
- Respect des invariants F-294 (additif, canonisation exacte, fail-open, DB-first).
