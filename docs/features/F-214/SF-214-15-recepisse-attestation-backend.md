# Mini-spec — F-214 / SF-214-15 — Récépissé vs attestation prolongation — backend

## Identifiant

`F-214 / SF-214-15`

## Feature parente

`F-214` — P2 Immigration FR — ~22 outils fréquence haute

## Statut

`ready`

## Date de création

2026-05-20

---

## Objectif

Distinguer les droits attachés au récépissé (autorise séjour + travail) vs l'attestation de prolongation d'instruction (séjour autorisé mais PAS de droit au travail), confusion fréquente source de sanctions employeur.

---

## Comportement attendu

### Cas nominal

- **POST** `/api/v1/case-files/{caseFileId}/recepisse-attestation-analysis`
- Body : `typeDocument` (enum `RECEPISSE` | `ATTESTATION_PROLONGATION` | `INCONNU`), `dateDelivrance` (LocalDate), `dateExpiration` (LocalDate), `mentionAutorisationTravail` (boolean, optionnel)
- Analyzer `RecepisseAttestationAnalyzer` :
  - `droitSejour` : boolean (toujours true pour les deux)
  - `droitTravail` : boolean (true si RECEPISSE ; false si ATTESTATION_PROLONGATION)
  - `dureeValiditeJours` : dateExpiration - dateDelivrance
  - `risqueEmployeur` : si ATTESTATION_PROLONGATION → risque sanctions L. 8253-1 Code travail
  - `recommandations` : si ATTESTATION_PROLONGATION → avocat doit vérifier type de procédure pour obtenir récépissé
  - `baseJuridique` : R. 311-4 + R. 311-6 CESEDA
- Output persisté dans `recepisse_attestation_analyses` (1:1 case_file)
- **GET** `/api/v1/case-files/{caseFileId}/recepisse-attestation-analysis` → 200 ou 404

---

## Source juridique

- **R. 311-4 CESEDA** — récépissé autorisant le séjour et le travail.
- **R. 311-6 CESEDA** — attestation de prolongation d'instruction (séjour seulement, sans travail).
- **L. 8253-1 Code du travail** — sanctions employeur pour emploi de travailleur sans autorisation.
- **Circ. INTV1518773C** (à vérifier) — droits attachés aux différents documents.

---

## Champs IA à extraire

| Champ | Type | Champ source `ImmigrationExtractedData` | Extension |
|---|---|---|---|
| `typeDocument` | enum | Absent | Extension record + prompt (`recepisseOuAttestationType`) |
| `dateExpiration` | date | `dateExpirationTitre` (proxy) | Réutiliser |

**Trigger CONTEXTUAL** : `recouvrement_titre_en_cours` (nouveau flag) — extraction : présence de "récépissé", "attestation de prolongation", "en cours de renouvellement", "attente de décision" dans les pièces. Ajouté dans `ImmigrationExtractedData` + prompt.

---

## Critères d'acceptation

- [x] POST RECEPISSE → droitTravail=true, risqueEmployeur=false
- [x] POST ATTESTATION_PROLONGATION → droitTravail=false, risqueEmployeur=true
- [x] POST INCONNU → recommandations d'identification
- [x] POST workspace BE → 400
- [x] GET sans POST → 404
- [x] Isolation workspace
- [x] `F-IM-32-recepisse-attestation-fr` dans KNOWN_FRONTEND_TOOL_IDS
- [x] Seed : CONTEXTUAL, trigger_field=`recouvrement_titre_en_cours`

## Plan de test minimal

- **UT** `RecepisseAttestationAnalyzerTest` : 5+ cas
- **IT** `RecepisseAttestationControllerIT` : 5+ cas

## Tables / endpoints / composants impactés

- **Nouvelle table** `recepisse_attestation_analyses`
- **Migration Liquibase** + seed visibility rules
- **Extension** `ImmigrationExtractedData` : flag `recouvrementTitreEnCours` + champ `recepisseOuAttestationType`
- **Endpoint** `RecepisseAttestationController`

## Hors périmètre

- Composant Angular (SF-214-16)
