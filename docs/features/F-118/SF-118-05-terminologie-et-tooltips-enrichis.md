# Mini-spec — F-118 / SF-118-05 Terminologie unifiée et tooltips enrichis

## Identifiant

`F-118 / SF-118-05`

## Feature parente

`F-118` — Refonte visuelle des écrans principaux / UX polish

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-118-05-terminologie-et-tooltips-enrichis`

---

## Objectif

Suite à un retour utilisateur ("ça me dérange de mettre en avant le terme IA dans mon application"), unifier la terminologie user-facing à l'intérieur de l'application en remplaçant le mot "IA" par des termes neutres et plus parlants pour l'avocat. Et enrichir les tooltips de cohérence pour afficher proprement toutes les sources contributrices (référence document, pièce manquante, checklist procédurale, question complémentaire).

Hors scope : pages marketing (landing, login), mentions légales et avertissements légaux où le terme "IA" est volontaire et explicite.

---

## Comportement attendu

### Renommage transversal (intérieur app)

| Avant | Après |
|---|---|
| `Incohérence IA` | `Incohérence détectée` |
| `Incohérence Question IA` | `Incohérence Question complémentaire` |
| `Incohérence F-96` | `Incohérence Checklist procédurale` |
| `Incohérence Pièce manquante` | inchangé (déjà clair) |
| `Incohérence multiple` | inchangé (déjà neutre) |
| `Analyse IA : …` (tooltip) | `Analyse du dossier : …` |
| `Question IA : …` (tooltip) | `Question complémentaire : …` |
| `Questions IA` (titre section) | `Questions complémentaires` |
| `Synthèse et questions IA` (billing) | `Synthèse et questions complémentaires` |
| `Pipeline IA` (super-admin) | inchangé (interne admin) |

### Tooltips enrichis

Pour chaque alerte de cohérence, le tooltip doit lister **toutes** les sources contributrices avec leur contexte :
- **Source Checklist procédurale** : statut + raison (si fournie).
- **Source Question complémentaire** : libellé de la question + réponse de l'avocat.
- **Source Analyse du dossier** : valeur attendue + justification (si fournie par l'IA, peut référencer un document).
- **Source Pièce manquante** : nom de la pièce identifiée.

L'utilisateur survole le badge → voit clairement **d'où vient chaque signal** sans avoir besoin de cliquer ailleurs.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Justification IA absente | Afficher juste `Analyse du dossier : <valeur attendue>` (sans suffixe `— …`) |
| Raison F-96 absente | Afficher juste `Checklist procédurale : <statut>` |
| Pièce manquante sans `texte` | Afficher `Pièce manquante identifiée` (fallback) |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Outils métier** : 10 outils décisionnels — tous touchés (badges + tooltips).
- [x] **Autres pays** : impact uniforme (textes français).
- [x] **Autres domaines** : impact uniforme.
- [x] **Autres UI patterns** : section Questions IA dans `case-file-detail`, écrans billing, super-admin (à laisser).
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification

- [x] **Modèle TypeScript** : types `*AlertSource` inchangés (les valeurs F96/QUESTION_IA/IA/PIECE_MANQUANTE/MULTI restent — c'est juste l'affichage user-facing qui change). Pas de breaking change.
- [x] **Record / DTO backend** : aucun changement.
- [x] **Service / logique métier** : aucun changement.
- [x] **Entité JPA** : aucun changement.
- [x] **Tests existants** : adapter aux nouveaux libellés (les assertions qui matchent "Incohérence IA" → "Incohérence détectée").

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable directement (refonte UX). Pour les futures features, la nouvelle terminologie sera la référence — checklist du template à mettre à jour (voir ci-dessous).

### Décision

- [x] Étendu à toutes les cibles applicables (10 outils + sections + billing)
- [ ] Subfeatures parallèles
- [ ] Backlog
- [x] Non applicable au marketing/legal/super-admin (justifié)

---

## Critères d'acceptation

- [ ] `alertBadgeLabel` des 10 outils retourne les nouveaux libellés.
- [ ] `alertTooltip` des 10 outils utilise les nouveaux libellés ("Analyse du dossier", "Question complémentaire", "Checklist procédurale").
- [ ] HTML : `Incohérence IA` remplacé par `Incohérence détectée` dans `divorce-checklist-section.component.html`.
- [ ] Sections `Questions IA` renommées en `Questions complémentaires` dans tous les écrans (notamment `case-file-detail`, `billing`).
- [ ] Tests Jest existants adaptés aux nouveaux libellés.
- [ ] Aucun nouveau test fonctionnel à ajouter (pas de logique modifiée).
- [ ] 958+ tests frontend verts, build OK.

---

## Périmètre

### Hors scope (explicite)

- Landing page (`/landing/*`), login (`/auth/login`) — terme "IA" est positionnement marketing.
- Mentions légales (`/legal/*`) — "Avertissement IA" est juridiquement structurant.
- `Pipeline IA` super-admin — interface interne admin uniquement.
- Tour onboarding — explication explicite du fonctionnement IA, à garder.
- PDF export — termes "Raison IA" dans les PDF (à reprendre dans une SF dédiée pour ne pas mélanger UX écran et export).
- Renommage `aiData` / `aiQuestions` au niveau code TypeScript (les noms d'API/DTO restent — refacto code = scope ≠ UX).
- Modifications backend (aucune).
- Enrichissement structuré du backend pour ajouter `documentName` aux détections (sera SF dédiée si besoin futur).

---

## Valeurs initiales

Sans objet.

---

## Contraintes de validation

Sans objet.

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

10 composants `*-section` (TS + HTML + spec) + `case-file-detail` (titre section Questions) + `workspace-billing` (label).

### Backend

Aucun impact.

---

## Plan de test

### Tests unitaires Jest

- [ ] Adapter les specs existants : `expect(...).toContain('Incohérence IA')` → `'Incohérence détectée'` etc.
- [ ] Aucun nouveau test fonctionnel (pas de logique modifiée).

### Validation manuelle

- [ ] Staging : ouvrir un outil, déclencher une incohérence → badge affiche le nouveau libellé, tooltip affiche les nouvelles formules.
- [ ] Onglet "Questions complémentaires" visible (anciennement "Questions IA").
- [ ] Page billing affiche "Synthèse et questions complémentaires" dans la liste des fonctionnalités du plan.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune fonctionnellement** — refonte UX strings uniquement.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| 10 outils `*-section` | Labels + tooltips | Specs existants adaptés |
| `case-file-detail` HTML | Section title | Spec existant |
| `workspace-billing` | Label feature | Spec existant |

### Smoke tests E2E

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- Aucune.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi pas tout renommer (landing, légal)** : ces zones ont un usage explicite et structurant du terme "IA" (positionnement marketing, encadrement juridique du service automatisé). Les retirer brouillerait le message commercial / légal.
- **Pourquoi "Question complémentaire"** (validé user)** : ces questions servent à compléter l'analyse, c'est leur fonction concrète pour l'avocat. Plus naturel que "Question IA" qui expose la mécanique sous-jacente.
- **Pourquoi "Analyse du dossier"** : neutre, descriptif, ne révèle pas l'implémentation technique. Cohérent avec ce que l'avocat perçoit (une analyse de SON dossier).
- **Pourquoi "Checklist procédurale"** plutôt que "F-96" (code interne) : le code F-96 est un identifiant projet, pas un libellé utilisateur. La nouvelle formule décrit la nature de la source.
- **Mise à jour template gouvernance** : pas dans le scope strict mais à enchaîner après cette SF (mettre à jour les exemples du template avec la nouvelle terminologie).
- **Pourquoi ne pas étendre les détections backend** : l'enrichissement structuré (ajout de `documentName`, `documentExcerpt` dans les detections) est un projet plus large. Ici on se contente d'utiliser les sources existantes proprement. SF dédiée si besoin.
