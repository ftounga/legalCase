# Mini-spec — F-DT-04 / SF-DT-04-02 — Formulaire Angular fiche prud'homale

---

## Identifiant

`F-DT-04 / SF-DT-04-02`

## Feature parente

`F-DT-04` — Génération fiche prud'homale

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-DT-04-02-frontend-fiche-prudhomale`

---

## Objectif

Afficher et éditer la fiche prud'homale dans l'interface (nouvel onglet du dossier), en consommant les endpoints backend créés en SF-DT-04-01.

---

## Comportement attendu

### Cas nominal

- L'utilisateur accède à l'onglet "Fiche prud'homale" dans le dossier
- Au chargement : `GET /api/v1/case-files/{id}/prudhome-fiche` — le formulaire est pré-rempli (fiche existante ou draft vide)
- Un bandeau info indique : "Document pré-rempli à environ 60-70%. À vérifier avant tout usage."
- Sections éditables :
  - **Demandeur** : nom (obligatoire), prénom, adresse, téléphone, email, profession
  - **Défendeur** : nom, adresse, SIRET, représentant
  - **Demandes** : liste dynamique — chaque ligne a un label (obligatoire) et un montant (€, optionnel) — bouton "Ajouter une demande" / icône supprimer
  - **Faits** : textarea libre
  - **Moyens de droit** : textarea libre
- Section **Pièces** (lecture seule) : liste numérotée des documents du dossier
- Bouton "Enregistrer" → `PUT /api/v1/case-files/{id}/prudhome-fiche` → toast succès

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Nom demandeur vide à la soumission | Validation inline Angular, soumission bloquée |
| Label demande vide | Validation inline, soumission bloquée |
| Erreur réseau / 500 backend | Toast erreur, formulaire conservé |
| 404 (dossier inexistant) | Toast erreur, navigation vers liste dossiers |

---

## Critères d'acceptation

- [ ] L'onglet "Fiche prud'homale" est visible dans le dossier
- [ ] `GET` au chargement : formulaire pré-rempli si fiche existante, vide sinon
- [ ] Bandeau info affiché en permanence
- [ ] Toutes les sections sont éditables
- [ ] Validation : nom demandeur obligatoire, label demande obligatoire
- [ ] Demandes : ajout / suppression de lignes fonctionnel
- [ ] `PUT` à la sauvegarde → toast succès
- [ ] Erreur réseau → toast erreur, données conservées
- [ ] Pièces affichées en lecture seule, numérotées
- [ ] Respect du Design System (couleurs, polices, layout header/onglets)

---

## Périmètre

### Hors scope (explicite)

- Export PDF/Word (SF-DT-04-03)
- Génération IA automatique du contenu
- Signature électronique
- Partage de la fiche

---

## Contraintes de validation

| Champ | Obligatoire | Notes |
|-------|-------------|-------|
| demandeur.nom | Oui | Non vide |
| demande.label | Oui | Non vide |
| demande.montant | Non | Nombre ≥ 0 si renseigné |
| Tous les autres champs | Non | Texte libre |

---

## Technique

### Endpoints consommés

| Méthode | URL |
|---------|-----|
| GET | `/api/v1/case-files/{id}/prudhome-fiche` |
| PUT | `/api/v1/case-files/{id}/prudhome-fiche` |

### Tables impactées

Aucune migration — backend SF-DT-04-01 déjà mergé.

### Composants Angular

- `PrudhomeFicheComponent` — formulaire complet (standalone, reactive forms)
- `PrudhomeFicheService` — HTTP client GET/PUT
- Ajout d'un onglet dans `CaseFileDetailComponent` (ou équivalent)

---

## Plan de test

### Tests unitaires composant

- [ ] Chargement : GET retourne une fiche → formulaire pré-rempli
- [ ] Chargement : GET retourne 404 → formulaire vide (pas d'erreur)
- [ ] Sauvegarde nominale : PUT réussi → toast succès affiché
- [ ] Sauvegarde erreur : PUT échoue → toast erreur, données conservées
- [ ] Ajout/suppression d'une demande

### Tests service HTTP

- [ ] `getFiche(caseFileId)` → appel `GET` correct
- [ ] `saveFiche(caseFileId, request)` → appel `PUT` correct

### Isolation workspace

- Non applicable côté frontend — vérifiée côté backend (SF-DT-04-01)

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — ajout d'un onglet dans la vue dossier

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression prévu |
|-----------|-----------------|------------------------------|
| `CaseFileDetailComponent` (ou équivalent) | Ajout d'un onglet — vérifier que les onglets existants ne sont pas cassés | Test navigation.spec.ts smoke |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier que les chemins existants fonctionnent après ajout de l'onglet

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-04-01 — statut : done (mergée)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Reactive Forms Angular (non template-driven) — cohérent avec les autres formulaires du projet
- `FormArray` pour la liste dynamique des demandes
- Service HTTP séparé du composant (pattern existant)
- Onglet ajouté dans la page dossier existante (pas de nouvelle route)
