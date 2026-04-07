# Mini-spec — F-IM-06 / SF-IM-06-02 Endpoint génération et consultation du recours

---

## Identifiant

`F-IM-06 / SF-IM-06-02`

## Feature parente

`F-IM-06` — Générateur de recours préfectoral / CGRA

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-06-02-endpoint-recours`

---

## Objectif

Exposer un endpoint REST POST qui reçoit les données du recours, appelle le générateur, persiste et retourne le document structuré. Plus un endpoint GET pour récupérer un recours existant.

---

## Comportement attendu

### Cas nominal

1. `POST /api/v1/case-files/{caseFileId}/immigration/recours` avec body contenant recoursType, dateNotification, requerant (nom, prenom, nationalite, adresse), decisionContestee (autorite, date, reference), exposeFaits
2. Vérification : utilisateur authentifié, membre du workspace, domaine = DROIT_IMMIGRATION
3. Appel `RecoursGenerator.generate()`, persistance (upsert), retour du document
4. `GET /api/v1/case-files/{caseFileId}/immigration/recours` retourne le recours existant ou 404

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Type de recours inconnu | "Type de recours inconnu" | 400 |
| Date notification absente | "Date de notification requise" | 400 |
| Requérant incomplet | "Champs requérant obligatoires" | 400 |
| Dossier autre workspace | "Case file not found" | 404 |
| Domaine != DROIT_IMMIGRATION | "Ce dossier n'est pas un dossier de droit de l'immigration" | 400 |
| Aucun recours existant (GET) | "Aucun recours trouvé" | 404 |

---

## Critères d'acceptation

- [ ] POST génère et persiste le recours, retourne le document complet
- [ ] GET retourne le recours existant
- [ ] Isolation workspace respectée
- [ ] Domaine juridique vérifié
- [ ] Upsert : second POST remplace le recours précédent
- [ ] Validation des champs obligatoires
- [ ] France et Belgique supportés

---

## Technique

### Endpoint(s)

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/case-files/{caseFileId}/immigration/recours` | Oui |
| GET | `/api/v1/case-files/{caseFileId}/immigration/recours` | Oui |

### Classes Java à créer

| Classe | Rôle |
|--------|------|
| `ImmigrationRecoursService` | Validation, génération, persistance, lecture |
| `ImmigrationRecoursController` | Controller REST |
| `ImmigrationRecoursRequest` | Record requête POST |
| `ImmigrationRecoursResponse` | Record réponse |

---

## Plan de test

### Tests d'intégration

- [ ] POST → 200 avec payload valide France
- [ ] POST → 200 avec payload valide Belgique
- [ ] POST → 400 type inconnu
- [ ] POST → 400 domaine != DROIT_IMMIGRATION
- [ ] POST → 404 autre workspace
- [ ] GET → 200 après POST
- [ ] GET → 404 sans recours
- [ ] POST upsert → remplace le recours

---

## Analyse d'impact

- [x] Aucune préoccupation transversale

## Dépendances

- SF-IM-06-01 — done
