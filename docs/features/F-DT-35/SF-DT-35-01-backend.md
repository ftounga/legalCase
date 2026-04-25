# Mini-spec — F-DT-35 / SF-DT-35-01 Backend Contestation ARE Pôle emploi (FR)

## Identifiant

`F-DT-35 / SF-DT-35-01`

## Feature parente

`F-DT-35` — Contentieux chômage : contestation décision Pôle emploi (devenu France Travail).

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-35-01-contestation-are-backend`

---

## Objectif

Outil décisionnel backend qui évalue la pertinence d'un recours contre une décision
France Travail (ex-Pôle emploi) relative à l'allocation de retour à l'emploi : refus
d'ouverture de droits, contestation de montant/durée d'indemnité, radiation, trop-perçu.
Calcule le respect des délais (recours hiérarchique 2 mois + contentieux TA 2 mois après
réponse explicite/implicite), score de probabilité de succès 0-100 et verdict de stratégie
(recours hiérarchique prioritaire, contentieux TA direct, motif insuffisamment fondé).

---

## Comportement attendu

### Cas nominal

Entrée :
- `typeDecisionContestee` (enum) — REFUS_OUVERTURE_DROITS, MONTANT_INDEMNITE, DUREE_INDEMNITE,
  RADIATION, TROP_PERCU, AUTRE.
- `motifContestation` (enum) — ERREUR_CALCUL_REMUNERATION_REFERENCE,
  MAUVAISE_QUALIFICATION_RUPTURE, OMISSION_PERIODES_TRAVAIL, REFUS_INJUSTIFIE, AUTRE.
- `dateNotificationDecision` (LocalDate).
- `dateRecoursHierarchiquePropose` (LocalDate, optionnel).
- `preuvesProduites` (liste enum) — BULLETIN_PAIE, ATTESTATION_EMPLOYEUR, CERTIFICAT_TRAVAIL,
  LETTRE_LICENCIEMENT, JUGEMENT_PRUDHOMAL, AUTRE.
- `montantContesteEur` (BigDecimal, optionnel ≥ 0).
- `demandeurDejaSaisiTribunal` (bool).
- `delaiContestationRespecte` (bool).

Le calculateur :
1. Calcule `delaiRecoursHierarchiqueJoursOk` = `dateRecoursHierarchiquePropose - dateNotificationDecision ≤ 60`
   (pratique standard ≈ 2 mois).
2. Calcule `delaiRecoursContentieuxTaJoursOk` = `delaiContestationRespecte && (le délai global de saisine TA après recours hiérarchique ≤ 60 jours est cohérent)`.
3. Score 0-100 :
   - `preuves.size() ≥ 2` → +30
   - motif reconnu (≠ AUTRE) → +25
   - `!demandeurDejaSaisiTribunal` → +20
   - `delaiContestationRespecte` → +15
   - `montantContesteEur ≥ 500` → +10 (montant significatif)
4. Verdict :
   - `score ≥ 70` → `RECOURS_HIERARCHIQUE_PRIORITAIRE`
   - `40 ≤ score < 70` → `CONTENTIEUX_TA_DIRECT_POSSIBLE`
   - `score < 40` → `INSUFFISAMMENT_FONDE`
   - Type AUTRE & motif AUTRE → `AUTRE_VOIE`
5. Délai d'instruction prévisionnel : 4 mois (instruction recours hiérarchique standard).
6. `expertiseTraitementSalaireRecommandee` = `motif == ERREUR_CALCUL_REMUNERATION_REFERENCE`.
7. Base juridique : `Art. L.5422-1 et s. + R.5422-1 Code travail + Loi 18/12/2023 France Travail`.
8. Formule lisible (Score X = ... → verdict Y).
9. Messages : recours préalable obligatoire avant TA (R.421-1 CJA), conseils de stratégie.

Persistance : snapshot JSON complet (inputs + outputs) dans une entité 1:1 par dossier.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `typeDecisionContestee` absent ou invalide | Erreur explicite | 400 |
| `motifContestation` absent ou invalide | Erreur explicite | 400 |
| `dateNotificationDecision` absente | Erreur explicite | 400 |
| `preuvesProduites` contient un code inconnu | Erreur explicite | 400 |
| `montantContesteEur` < 0 | Erreur explicite | 400 |
| Workspace pays = BELGIQUE | Refus — outil FR uniquement | 400 |
| Dossier ≠ DROIT_DU_TRAVAIL | Refus | 400 |
| `caseFileId` autre workspace | 404 | 404 |
| GET sans POST préalable | 404 | 404 |

---

## Impact par domaine métier

Cette feature est **sensible au domaine** : elle est strictement attachée au droit du
travail (procédure ARE / Pôle emploi). Les domaines immigration et famille n'ont pas
d'équivalent — la contestation administrative existe ailleurs (référés admin OQTF côté
immigration, ordonnance de protection côté famille) mais avec des structures juridiques
distinctes déjà couvertes par leurs propres outils. L'outil est aussi **single-country
FRANCE** : la Belgique a un système ONEM avec procédure de réclamation distincte et
juridiction du travail (TT) au lieu d'un TA — feature jumelle backlog F-DT-35-BE
ultérieure.

## Parité des domaines métier

Niveau de profondeur : 5 (scoring 0-100). Vérification de parité :

| Domaine | Équivalent existant ? | Action |
|---------|-----------------------|--------|
| Droit du travail FR | Cette SF | — |
| Droit du travail BE | Non (procédure ONEM distincte) | Backlog feature jumelle F-DT-35-BE (note : régime juridique entièrement distinct, pas un simple paramétrage) |
| Immigration FR | F-IM-08 référés admin (procédure distincte) | Concept différent — pas de parité requise |
| Immigration BE | Référés CCE | Concept différent — pas de parité requise |
| Famille FR | OP F-FA-14 (procédure distincte) | Concept différent — pas de parité requise |
| Famille BE | Procédures TF | Concept différent — pas de parité requise |

→ Décision : pas de feature jumelle bloquante. F-DT-35-BE reste backlog pour V8+ comme prévu PRODUCT_SPEC.

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires : F-FA-14 (OP — score+verdict), F-DT-08 (validité licenciement —
  score+verdict), F-DT-32 (documents fin contrat — score+verdict). Pattern réutilisé tel quel.
- [x] FR vs BE : cf. matrice "Impact par domaine métier" — feature jumelle backlog.
- [x] Domaines : strictement DROIT_DU_TRAVAIL (cf. matrice ci-dessus).
- [x] UI patterns : pas concerné côté backend.
- [x] Pré-remplissage IA : possible depuis synthèse — frontend SF-DT-35-02 le câblera.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern score+verdict (F-FA-14, F-DT-08, F-DT-32) | Oui | Réutilisé tel quel |
| F-DT-35 BE | Oui | Backlog feature jumelle |
| Refresh dashboard F-IA-02 | Oui | À câbler côté frontend SF-DT-35-02 |
| F-IA-03 cohérence | Oui | Frontend SF-DT-35-02 |
| F-IA-04 visibility rule | Oui | ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 64 |

### Décision

- [x] Étendu à toutes les cibles applicables backend dans cette SF
- [x] Frontend SF-DT-35-02 vague suivante (panel + intégration F-IA-04)
- [x] BE = feature jumelle backlog

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/contestation-are` calcule + persiste
- [x] `GET` retourne le dernier snapshot
- [x] Délai recours hiérarchique : ≤ 60 jours après notification décision
- [x] Délai contentieux TA : `delaiContestationRespecte` retourné tel quel
- [x] Score 0-100 selon barème spec
- [x] Verdict RECOURS_HIERARCHIQUE_PRIORITAIRE / CONTENTIEUX_TA_DIRECT_POSSIBLE / INSUFFISAMMENT_FONDE / AUTRE_VOIE
- [x] `expertiseTraitementSalaireRecommandee` = true ssi motif = ERREUR_CALCUL_REMUNERATION_REFERENCE
- [x] BELGIQUE refusée
- [x] Dossier non droit du travail refusé
- [x] Migration Liquibase 158 + visibility rule UUID `f1a04001-0000-0000-0000-ee0000000351`, priority 64
- [x] ≥ 14 UT + ≥ 8 IT

---

## Périmètre

### Hors scope

- Génération PDF du recours (potentielle SF F-DT-35-03 ultérieure)
- Frontend (SF-DT-35-02 vague suivante)
- Belgique (feature jumelle backlog)

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `typeDecisionContestee` | Oui | enum string |
| `motifContestation` | Oui | enum string |
| `dateNotificationDecision` | Oui | ISO date |
| `dateRecoursHierarchiquePropose` | Non | ISO date |
| `preuvesProduites` | Non (default vide) | enum array |
| `montantContesteEur` | Non | ≥ 0 |
| `demandeurDejaSaisiTribunal` | Oui (bool) | bool |
| `delaiContestationRespecte` | Oui (bool) | bool |

---

## Technique

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/contestation-are` | Oui |
| GET  | `/api/v1/case-files/{caseFileId}/contestation-are` | Oui |

### Tables

| Table | Opération |
|-------|-----------|
| `contestation_are_analyses` | CREATE (migration 158) |
| `decision_tool_visibility_rules` | INSERT 1 ligne ALWAYS_ON FR DROIT_DU_TRAVAIL priority 64 |

### Migration Liquibase

- [x] `158-create-contestation-are-analyses.xml`
- UUID visibility : `f1a04001-0000-0000-0000-ee0000000351`

---

## Plan de test

### Tests unitaires (≥ 14)

- [x] Score max (preuves≥2 + motif reconnu + pas de tribunal + delai respecté + montant ≥500) → 100
- [x] Score min (aucune preuve, motif AUTRE, tribunal saisi, delai non respecté) → 0
- [x] Verdict RECOURS_HIERARCHIQUE_PRIORITAIRE si score ≥ 70
- [x] Verdict CONTENTIEUX_TA_DIRECT_POSSIBLE si 40 ≤ score < 70
- [x] Verdict INSUFFISAMMENT_FONDE si score < 40
- [x] Verdict AUTRE_VOIE si type AUTRE et motif AUTRE
- [x] delaiRecoursHierarchiqueJoursOk = true si délai ≤ 60 jours
- [x] delaiRecoursHierarchiqueJoursOk = false si délai > 60 jours
- [x] expertiseTraitementSalaireRecommandee = true ssi motif = ERREUR_CALCUL_REMUNERATION_REFERENCE
- [x] preuves dédupliquées et triées
- [x] type inconnu → IllegalArgumentException
- [x] motif inconnu → IllegalArgumentException
- [x] preuve inconnue → IllegalArgumentException
- [x] dateNotification null → IllegalArgumentException
- [x] montant négatif → IllegalArgumentException
- [x] BaseJuridique mentionne L.5422-1 et France Travail
- [x] Formule lisible

### Tests d'intégration (≥ 8)

- [x] POST nominal FR → 200 score élevé verdict RECOURS_HIERARCHIQUE_PRIORITAIRE
- [x] POST workspace BE → 400
- [x] POST dossier immigration → 400
- [x] POST autre workspace → 404
- [x] POST type invalide → 400
- [x] POST sans dateNotification → 400
- [x] GET après POST → snapshot
- [x] GET sans POST → 404
- [x] POST upsert remplace
- [x] POST verdict INSUFFISAMMENT_FONDE pour score bas

### Isolation workspace

- [x] Applicable — test 404 si workspace différent

---

## Analyse d'impact

### Préoccupations transversales

- [x] Aucune préoccupation transversale — endpoint isolé sur un dossier

### Smoke tests E2E

- [x] Aucun smoke test concerné — outil métier indépendant.

---

## Dépendances

### Subfeatures bloquantes

- Aucune

### Frontend planifié (vague suivante)

- `SF-DT-35-02` — frontend Angular (panel + section component) ; consommera le contrat ci-dessus.

---

## Notes

- Pôle emploi est devenu France Travail le 1er janvier 2024 (Loi 18/12/2023 plein emploi),
  l'allocation reste l'ARE et le régime juridique est inchangé.
- Le délai de 2 mois pour saisir le TA après la réponse au recours hiérarchique est
  prévu à l'art. R.421-1 CJA. Le silence de l'administration vaut décision implicite de
  rejet à l'expiration de 2 mois (art. L.231-4 CRPA).
- Le recours préalable devant la commission paritaire de Pôle emploi est obligatoire
  avant TA pour les contestations de droits (art. L.5422-12 ne le précise pas explicitement
  mais c'est la pratique constante).
- L'expertise de salaire est utile quand le calcul du SJR (salaire journalier de référence)
  est en jeu — c'est le motif le plus fréquent de contestation (~40 % en pratique).
