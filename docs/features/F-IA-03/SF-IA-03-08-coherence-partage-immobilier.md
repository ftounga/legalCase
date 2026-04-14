# Mini-spec — F-IA-03 / SF-IA-03-08 Cohérence IA sur F-FA-05 Partage immobilier

## Identifiant

`F-IA-03 / SF-IA-03-08`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-08-coherence-partage-immobilier`

---

## Objectif

Étendre le moteur de cohérence à F-FA-05 Partage immobilier sur les champs numériques `valeurVenale` et `capitalRestantDu`. Source unique = détection IA (via la meilleure correspondance dans `liquidationCommunaute.actifCommun` / `passifCommun`). Warning uniquement sur seuil 10 %. Pas de surveillance sur `quotePartAttributaire` (non extraite par l'IA) ni sur `country`/`isDivorce` (décisions produit).

---

## Comportement attendu

### Champs surveillés

| Champ user | Source IA | Seuil | Niveau |
|---|---|---|---|
| `valeurVenale` | meilleure correspondance dans `actifCommun[]` filtrée immo | écart relatif ≥ 10 % | `warning` |
| `capitalRestantDu` | meilleure correspondance dans `passifCommun[]` filtrée prêt | écart relatif ≥ 10 % | `warning` |

### Règle "meilleure correspondance"

1. Si l'avocat a importé un bien via le panneau SF-FA-05-04 et la provenance IA est active → la source est exactement ce bien (valeur IA = valeur initialement importée). Si l'avocat modifie la valeur, on compare à la valeur IA d'origine.
2. Sinon (pas d'import effectué) → on prend dans la liste filtrée l'item dont la `valeur` est numériquement la plus proche de la valeur user. Si l'écart relatif à cet item est ≥ 10 %, alerte. Sinon, pas d'alerte.
3. Si la liste filtrée est vide → pas de surveillance.

La logique "meilleure correspondance" permet de traiter les dossiers où l'avocat saisit manuellement sans import (cas fréquent pour un premier passage).

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `liquidationCommunaute` null | pas d'alerte |
| Liste filtrée vide | pas d'alerte |
| User valeur = 0 ou vide | pas d'alerte (non saisi) |
| User valeur = valeur IA exacte | pas d'alerte |
| Écart < 10 % | pas d'alerte |
| Résultat F-FA-05 sauvegardé | alerte gelée |

---

## Critères d'acceptation

- [ ] Nouveau computed `coherenceAlerts` sur `PartageImmobilierSectionComponent`.
- [ ] Surveillance de `valeurVenale` et `capitalRestantDu` avec seuil 10 %.
- [ ] Logique "meilleure correspondance" : utilise la valeur provenant de l'import si présente, sinon la plus proche dans la liste filtrée.
- [ ] `warning` uniquement — pas de `blocker`. Pas de compteur "bloquantes".
- [ ] Badge + tooltip à côté de chaque champ, bandeau récap si ≥ 1 alerte.
- [ ] Alerte gelée quand résultat chargé ou formulaire masqué.
- [ ] Rétrocompat : SF-FA-05-04 intacte, 4 tests existants préservés.
- [ ] Tests unitaires frontend : matrice écart/seuil, import vs saisie manuelle, liste vide, valeur 0.

---

## Périmètre

### Hors scope (explicite)

- Surveillance de `quotePartAttributaire` (non extraite IA, presque toujours 50 %).
- Surveillance de `country` et `isDivorce` (choix produit de l'avocat, pas factuels).
- Sources F-96 / questions IA : F-FA-05 est purement numérique, ces sources ne s'appliquent pas naturellement.
- Extension à F-IM-* → SF-IA-03-09+.
- Niveau `info` + justification obligatoire.

---

## Valeurs initiales

Aucune entité créée. Pur calcul dérivé.

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|-------|-------------|--------|---------------|
| Seuil écart | constante | 10 % relatif | `abs(a - ia) / max(abs(ia), 1) ≥ 0.10` |

---

## Technique

### Endpoint(s)

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] **Non applicable**.

### Composants Angular

- `PartageImmobilierSectionComponent` :
  - Types `PartageCoherenceAlert`, `PartageAlertField = 'VALEUR_VENALE' | 'CAPITAL_RESTANT'`
  - Helpers : `findBestMatch(value, list)` — retourne l'item le plus proche numériquement
  - Computed `coherenceAlerts: Partial<Record<field, alert>>`
  - Computed `alertsSummary: {total, blockers}` (blockers toujours 0)
  - Badge + tooltip par champ + bandeau récap
  - Alerte gelée quand `!showForm() || result()`

---

## Plan de test

### Tests unitaires frontend

Valeur vénale :
- [ ] IA actif "Maison 400k", user 400000 → pas d'alerte.
- [ ] IA actif "Maison 400k", user 420000 (5 %) → pas d'alerte (sous seuil).
- [ ] IA actif "Maison 400k", user 450000 (12.5 %) → warning.
- [ ] IA 2 biens "Maison 400k" + "Appt 300k", user 310000 → best match appt, écart 3.3 % → pas d'alerte.
- [ ] IA 2 biens, user 360000 → best match entre 400k (écart 10 %) et 300k (écart 20 %) → best = 400k, écart exactement 10 % → warning.
- [ ] User 0 → pas d'alerte.
- [ ] liquidationCommunaute null → pas d'alerte.
- [ ] Liste immo filtrée vide → pas d'alerte.

Capital restant :
- [ ] IA passif "Prêt BNP 150k", user 150000 → pas d'alerte.
- [ ] IA passif, user 170000 (13 %) → warning.
- [ ] User 0 → pas d'alerte.
- [ ] Liste prêt filtrée vide → pas d'alerte.

Transverses :
- [ ] Compteur agrège 2 champs.
- [ ] Résultat sauvegardé → alertes gelées.
- [ ] Provenance IA active (SF-FA-05-04) + user modifie valeur de 15 % → warning sur valeur originale IA.

### Isolation workspace

- [x] Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune**.

### Composants impactés

| Composant | Impact |
|---|---|
| `PartageImmobilierSectionComponent` | ajouts computed + UI, SF-FA-05-04 intacte |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-FA-05-04` (Done) — fournit le filtrage par mots-clés et les données `liquidationCommunaute` déjà en place.
- `F-IA-01` (Done).

### Questions ouvertes

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi source IA seule, pas F-96 ni questions** : F-FA-05 est purement numérique. F-96 parle de procédure, pas de valeurs vénales. Les questions IA numériques sont rares et fragiles à interpréter.
- **Pourquoi warning only** : pattern cohérent avec SF-IA-03-04 (F-DT-07 Ancienneté, également purement numérique). Un écart factuel n'est pas un blocage juridique.
- **Pourquoi "meilleure correspondance"** : l'IA extrait une liste, pas un bien spécifique. Matcher le plus proche numériquement est pragmatique et couvre le cas simple (1 bien).
- **Pourquoi pas de surveillance sur quote-parts** : l'IA ne les extrait pas (SF-FA-05-04 l'a confirmé). Surveiller un champ non extrait ne produirait jamais d'alerte.
- **Pourquoi exploiter la provenance IA de SF-FA-05-04** : si l'avocat a importé via le panneau, on connaît EXACTEMENT la valeur IA de référence. Plus précis que "best match". La logique fallback sur "best match" couvre le cas sans import.
