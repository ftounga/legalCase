# Mini-spec — F-DT-32 / SF-DT-32-01 Backend documents de fin de contrat (FR)

## Identifiant

`F-DT-32 / SF-DT-32-01`

## Feature parente

`F-DT-32` — Documents de fin de contrat

## Statut

`ready`

## Date de création

2026-04-25

## Branche Git

`feat/SF-DT-32-01-documents-fin-contrat-backend`

---

## Objectif

Outil décisionnel backend qui vérifie la conformité des trois documents de fin de contrat
légalement obligatoires en droit du travail français (certificat de travail, attestation
France Travail, reçu pour solde de tout compte) et calcule un score de conformité +
verdict de risque contentieux pour l'employeur.

---

## Comportement attendu

### Cas nominal

Entrée : date de fin de contrat, état + dates de remise des trois documents, salaire mensuel brut.

Le calculateur :
1. Vérifie le respect des délais légaux pour le certificat de travail (L.1234-19) et
   l'attestation France Travail / R.1234-9 — délai jurisprudentiel de remise « immédiate »,
   tolérance pratique 7 jours.
2. Vérifie la signature du reçu pour solde de tout compte dans les 30 jours (L.1234-20).
3. Calcule la fenêtre de contestation du STC : 6 mois après signature (art. L.1234-20 al. 2).
4. Calcule l'indemnité de retard pour chaque document non remis en temps : forfait journalier
   = `(salaire mensuel brut / 30) × jours de retard`.
5. Score de conformité 0-100 :
   - Certificat dans les délais : +35
   - Attestation dans les délais : +35
   - STC valide ou non signé (donc non discutable) : +30
6. Verdict : FAIBLE ≥ 80, MOYEN 50-79, ELEVE < 50.

Persistance : snapshot JSON complet (inputs + outputs) dans une entité 1:1 par dossier.

### Cas d'erreur

| Situation | Comportement | Code HTTP |
|-----------|-------------|-----------|
| `dateFinContrat` absente | Erreur explicite | 400 |
| `salaireMensuelBrutEur` ≤ 0 | Erreur explicite | 400 |
| Workspace pays = BELGIQUE | Refus — outil FR uniquement | 400 |
| Dossier ≠ DROIT_DU_TRAVAIL | Refus | 400 |
| `caseFileId` autre workspace | 404 | 404 |
| GET sans POST préalable | 404 | 404 |

---

## Analyse de cohérence transversale

### Périmètres scannés

- [x] Outils similaires : F-DT-08 (validité licenciement) et F-DT-10 (validité rupture conv.)
  reposent sur le même pattern « score 0-100 + verdict 3 niveaux ». Pattern réutilisé tel quel.
- [x] FR vs BE : la BE a ses propres documents de fin de contrat (C4 ONEM, certificat travail
  Loi 03/07/1978 art. 22, attestation vacances) → SF jumelle F-DT-32 BE à scoper séparément (backlog).
- [x] Domaines : transversal n'est pas pertinent — c'est strictement DROIT_DU_TRAVAIL FR.
- [x] UI patterns : pas concerné côté backend (frontend SF-DT-32-02 vague suivante).
- [x] Pré-remplissage IA : possible depuis synthèse — frontend SF-DT-32-02 le câblera.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern score+verdict (F-DT-08/10/16) | Oui | Réutilisé tel quel |
| F-DT-32 BE | Oui | Backlog feature jumelle (C4/STC BE) |
| Refresh dashboard F-IA-02 | Oui | À câbler côté frontend SF-DT-32-02 |
| F-IA-03 cohérence | Oui | Date fin contrat croise avec autres outils — frontend SF-DT-32-02 |
| F-IA-04 visibility rule | Oui | ALWAYS_ON FRANCE DROIT_DU_TRAVAIL priority 59 |

### Décision

- [x] Étendu à toutes les cibles applicables backend dans cette SF
- [x] Frontend SF-DT-32-02 vague suivante (panel + intégration F-IA-04)
- [x] BE = feature jumelle backlog

---

## Critères d'acceptation

- [x] `POST /api/v1/case-files/{caseFileId}/documents-fin-contrat` calcule + persiste
- [x] `GET` retourne le dernier snapshot
- [x] Délais certificat/attestation = +7 jours après dateFinContrat (jurisprudence pratique)
- [x] STC valide si signature dans les 30 jours suivant dateFinContrat
- [x] Fenêtre de contestation STC = 6 mois après signature, calculée par rapport à `today`
- [x] Indemnité retard = `(salaireMensuel / 30) × joursRetard` arrondie 2 décimales
- [x] Score 0-100, verdict FAIBLE/MOYEN/ELEVE
- [x] BELGIQUE refusée
- [x] Dossier non droit du travail refusé
- [x] Migration Liquibase 148 + visibility rule UUID `f1a04001-0000-0000-0000-ee0000000321`, priority 59
- [x] ≥ 14 UT + ≥ 8 IT

---

## Périmètre

### Hors scope

- Génération PDF des documents (autre SF F-DT-32-03)
- Intégration France Travail API
- Frontend (SF-DT-32-02 vague suivante)
- Belgique (feature jumelle backlog)

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|-------|-------------|--------|
| `dateFinContrat` | Oui | ISO date |
| `salaireMensuelBrutEur` | Oui | > 0 |
| `certificatTravailRemis` | Non (default false) | bool |
| `dateCertificatTravail` | Si remis | ISO date |
| `attestationFranceTravailRemise` | Non | bool |
| `dateAttestationFranceTravail` | Si remis | ISO date |
| `souldeToutCompteSigne` | Non | bool |
| `dateSouldeToutCompte` | Si signé | ISO date |
| `souldeToutCompteContestableDelai6mois` | Non (calculé) | bool |

---

## Technique

### Endpoints

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/documents-fin-contrat` | Oui |
| GET  | `/api/v1/case-files/{caseFileId}/documents-fin-contrat` | Oui |

### Tables

| Table | Opération |
|-------|-----------|
| `documents_fin_contrat_analyses` | CREATE (migration 148) |
| `decision_tool_visibility_rules` | INSERT 1 ligne ALWAYS_ON FR DROIT_DU_TRAVAIL priority 59 |

### Migration Liquibase

- [x] `148-create-documents-fin-contrat-analyses.xml`
- UUID visibility : `f1a04001-0000-0000-0000-ee0000000321`

---

## Plan de test

### Tests unitaires (≥ 14)

- [x] Trois documents OK dans les délais → score 100 FAIBLE
- [x] Certificat seul OK → score 35 ELEVE
- [x] Certificat + attestation OK, STC absent → score 70 MOYEN
- [x] Certificat hors délai (10 jours après dateFinContrat) → score perdu + indemnité retard
- [x] Indemnité retard : 30 jours, salaire 3000 → 3000 €
- [x] STC signé dans les 30j → valide
- [x] STC signé > 30j → non valide
- [x] STC signé il y a 3 mois → contestable (< 6 mois)
- [x] STC signé il y a 8 mois → non contestable
- [x] Aucun document remis → score 30 (STC non signé donc non discutable) ELEVE
- [x] Tous documents remis dans délais et STC valide → 100 FAIBLE
- [x] Salaire 0 → IllegalArgumentException
- [x] dateFinContrat null → IllegalArgumentException
- [x] Pays BELGIQUE → IllegalArgumentException
- [x] Pays null → IllegalArgumentException
- [x] BaseJuridique mentionne L.1234-19, R.1234-9, L.1234-20
- [x] Formule lisible

### Tests d'intégration (≥ 8)

- [x] POST nominal FR → 200 score 100
- [x] POST partiel → 200 score 70
- [x] POST workspace BE → 400
- [x] POST dossier immigration → 400
- [x] POST autre workspace → 404
- [x] POST salaire 0 → 400
- [x] POST sans dateFinContrat → 400
- [x] GET après POST → snapshot
- [x] GET sans POST → 404
- [x] POST upsert remplace

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

- `SF-DT-32-02` — frontend Angular (panel + section component) ; consommera le contrat ci-dessus.

---

## Notes

- Délai jurisprudentiel de 7 jours pour certificat/attestation = pratique constante (Cass. Soc.
  03/04/2007 et postérieurs) — le Code dit « lors de l'expiration du contrat » ce qui implique
  remise immédiate, mais le juge tolère le délai postal/administratif.
- L'indemnité de retard est jurisprudentielle (Cass. Soc.) — le forfait 1/30 × salaire × jours
  est une approximation usuelle, le juge peut moduler selon le préjudice réel.
- Le délai de contestation du STC de 6 mois est strictement codifié à L.1234-20 al. 2.
