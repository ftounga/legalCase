# Mini-spec — F-IM-06 / SF-IM-06-01 Référentiel types de recours et générateur de document (backend)

---

## Identifiant

`F-IM-06 / SF-IM-06-01`

## Feature parente

`F-IM-06` — Générateur de recours préfectoral / CGRA

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-06-01-referentiel-recours`

---

## Objectif

Créer le référentiel statique des types de recours immigration (France + Belgique), un générateur de document structuré qui produit un recours pré-rempli à partir des données du dossier, et l'entity de persistance 1:1 par dossier.

---

## Comportement attendu

### Cas nominal

1. Le référentiel contient les types de recours pour France et Belgique, chacun avec :
   - un code unique (ex: `RECOURS_GRACIEUX_PREFET`, `RECOURS_CGRA`)
   - un libellé
   - un pays (`FRANCE` / `BELGIQUE`)
   - le délai légal de recours (en jours, depuis la notification du refus)
   - les textes applicables (articles de loi)
   - la juridiction compétente
   - la structure du document (sections attendues)

2. Le générateur reçoit :
   - le type de recours
   - la date de notification du refus
   - les données du requérant (nom, prénom, nationalité, adresse)
   - les données de la décision contestée (autorité, date, référence)
   - un exposé des faits (texte libre)

3. Il produit un document structuré (JSON) contenant :
   - en-tête (juridiction, parties)
   - visa des textes applicables
   - exposé des faits
   - moyens de droit (pré-remplis selon le type)
   - demande (conclusions)
   - pièces à joindre (liste standard par type)
   - date limite de dépôt calculée (date notification + délai légal)

4. Le résultat est persisté en table `immigration_recours` (1:1 avec le dossier).

### Cas d'erreur

| Situation | Comportement attendu | Code HTTP |
|-----------|---------------------|-----------|
| Type de recours inconnu | Message d'erreur explicite | 400 |
| Pays non supporté | Message d'erreur | 400 |
| Date de notification absente | Message d'erreur | 400 |
| Dossier inexistant ou autre workspace | Accès refusé | 404 |
| Domaine != DROIT_IMMIGRATION | Message d'erreur | 400 |
| Date limite dépassée | Génération OK + avertissement dans le document | 200 |

---

## Critères d'acceptation

- [ ] Le référentiel couvre au minimum 3 types de recours France (gracieux préfet, contentieux TA, CNDA) et 3 types Belgique (CGRA, CCE, Conseil d'État)
- [ ] Les données sont configurées pour FRANCE et BELGIQUE
- [ ] Le générateur produit un document structuré complet pour chaque type
- [ ] La date limite est correctement calculée (date notification + délai légal)
- [ ] Un avertissement est inclus si la date limite est dépassée
- [ ] Les résultats sont persistés en DB avec FK vers case_file et contrainte unique
- [ ] L'isolation workspace est respectée
- [ ] Le service suit les patterns existants (`ImmigrationTitleReferentiel`, `PrudhomeFiche`)

---

## Périmètre

### Hors scope (explicite)

- Endpoint REST (SF-IM-06-02)
- Interface frontend (SF-IM-06-03)
- Export PDF (SF-IM-06-03)
- Envoi automatique du recours
- Signature électronique

---

## Contraintes de validation

| Champ | Obligatoire | Valeurs autorisées | Normalisation |
|-------|-------------|-------------------|---------------|
| recoursType | Oui | Codes du référentiel | — |
| dateNotification | Oui | Date ISO (passée ou aujourd'hui) | — |
| requerant.nom | Oui | Texte, max 255 | trim() |
| requerant.prenom | Oui | Texte, max 255 | trim() |
| requerant.nationalite | Oui | Texte, max 100 | trim() |
| requerant.adresse | Oui | Texte, max 500 | trim() |
| decisionContestee.autorite | Oui | Texte, max 255 | trim() |
| decisionContestee.date | Oui | Date ISO | — |
| decisionContestee.reference | Non | Texte, max 100 | trim() |
| exposeFaits | Non | Texte libre, max 5000 | — |

---

## Technique

### Tables impactées

| Table | Opération | Notes |
|-------|-----------|-------|
| `immigration_recours` | CREATE | Nouvelle table |
| `case_files` | SELECT | Vérification domaine + workspace |

### Migration Liquibase

- [x] Oui — `059-create-immigration-recours.xml`

**Structure de la table `immigration_recours` :**

| Colonne | Type | Contrainte |
|---------|------|-----------|
| `id` | UUID | PK |
| `case_file_id` | UUID FK | UNIQUE, NOT NULL |
| `recours_type` | VARCHAR(50) | NOT NULL |
| `date_notification` | DATE | NOT NULL |
| `date_limite` | DATE | NOT NULL |
| `requerant_data` | TEXT | NOT NULL — JSON (nom, prénom, nationalité, adresse) |
| `decision_contestee_data` | TEXT | NOT NULL — JSON (autorité, date, référence) |
| `expose_faits` | TEXT | nullable |
| `generated_document` | TEXT | NOT NULL — JSON du document structuré complet |
| `created_at` | TIMESTAMP | NOT NULL |
| `updated_at` | TIMESTAMP | NOT NULL |

### Classes Java à créer

| Classe | Rôle |
|--------|------|
| `ImmigrationRecoursReferentiel` | Référentiel statique : types de recours FR + BE avec délais, textes, structure |
| `RecoursType` | Record : code, label, country, delaiJours, textesApplicables, juridiction, sections, piecesStandard |
| `RecoursGenerator` | Génère le document structuré à partir des données + type de recours |
| `GeneratedRecours` | Record : résultat de la génération (sections, dateLimite, avertissement) |
| `ImmigrationRecours` | Entity JPA |
| `ImmigrationRecoursRepository` | Repository JPA |

### Composants Angular (si applicable)

- Non applicable (SF-IM-06-01 = backend uniquement)

---

## Plan de test

### Tests unitaires

- [ ] `ImmigrationRecoursReferentiel` — au moins 3 types FR et 3 types BE
- [ ] `ImmigrationRecoursReferentiel` — `getTypes(country)` retourne uniquement le pays demandé
- [ ] `ImmigrationRecoursReferentiel` — chaque type a des textes applicables non vides
- [ ] `RecoursGenerator` — recours gracieux France → document avec en-tête préfet, visa CESEDA
- [ ] `RecoursGenerator` — recours CGRA Belgique → document avec en-tête CGRA, visa loi 1980
- [ ] `RecoursGenerator` — date limite calculée correctement (notification + délai)
- [ ] `RecoursGenerator` — date limite dépassée → avertissement dans le document
- [ ] `RecoursGenerator` — type inconnu → exception
- [ ] `RecoursGenerator` — tous les champs du document structuré sont remplis

### Tests d'intégration

- [ ] Persistance d'un recours → relecture OK
- [ ] Upsert : second recours sur le même dossier remplace le premier
- [ ] Dossier avec domaine != DROIT_IMMIGRATION → rejet

### Isolation workspace

- [x] Applicable — via FK case_file_id, contrôle d'accès dans le service (SF-IM-06-02)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, pattern existant réutilisé

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné (nouvelle feature backend, aucun composant existant modifié)

---

## Dépendances

### Subfeatures bloquantes

- Aucune — SF-IM-06-01 est la première du découpage

### Questions ouvertes impactées

- [ ] Aucune question ouverte de `docs/OPEN_QUESTIONS.md` n'est impactée

---

## Notes et décisions

- Le référentiel est statique en Java (comme `ImmigrationTitleReferentiel`)
- Le document généré est stocké en JSON TEXT (comme `PrudhomeFiche`)
- La relation avec `case_files` est 1:1 (contrainte UNIQUE)
- Les moyens de droit sont pré-remplis avec des formulations standards par type de recours — l'avocat pourra les modifier via le frontend (SF-IM-06-03)
- Les textes applicables référencent le CESEDA (France) et la loi du 15/12/1980 + arrêté royal 8/10/1981 (Belgique)
