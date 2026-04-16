# Mini-spec — F-DT-09 / SF-DT-09-06 Ajout du champ mois au Comparateur d'indemnités

## Identifiant

`F-DT-09 / SF-DT-09-06`

## Feature parente

`F-DT-09` — Comparateur jurisprudentiel d'indemnités

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-DT-09-06-champ-mois-comparateur`

---

## Objectif

Ajouter un champ "mois" en complément du champ "ancienneté en années" dans le formulaire F-DT-09. Bug observé pendant le Test 2 Martin : l'IA extrait une ancienneté de 16 ans 1 mois, pré-remplit 16 ans côté Comparateur, et la cohérence de cohérence affiche un faux positif "Incohérence IA" car elle compare 16,08 vs 16 — et l'avocat n'a aucun moyen de corriger puisque le champ mois est absent.

---

## Comportement attendu

### Cas nominal

1. L'IA extrait `compensation_data.ancienneteAnnees` et `compensation_data.ancienneteMois` (les deux existent déjà côté extraction).
2. Le formulaire F-DT-09 affiche désormais **deux champs numériques** : "Ancienneté (années)" et "(mois)".
3. Pré-remplissage : `ancienneteAnnees` et `ancienneteMois` depuis la synthèse IA.
4. Le backend calcule les fourchettes en utilisant le total en mois (conversion `annees + mois/12`).
5. L'alerte de cohérence compare la saisie utilisateur (années + mois) au total IA (années + mois), en mois totaux — seuil 1 mois pour éviter les faux positifs.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| `ancienneteMois` absent de l'IA | Pré-remplissage à 0, comme aujourd'hui |
| Résultat legacy sans `ancienneteMois` (avant cette SF) | `ancienneteMois = 0` à la lecture GET, fallback gracieux |
| `ancienneteMois` > 11 | Validation backend 400 "Mois doit être entre 0 et 11" |
| `ancienneteMois` < 0 | Validation backend 400 "Mois doit être positif" |

### Règles de cohérence ajustées

- Comparaison en mois totaux : `(userAnnees + userMois/12)` vs `(iaAnnees + iaMois/12)`.
- Alerte `ANCIENNETE` si écart ≥ 1 mois (0,083 année — remplace le seuil actuel 0,5 an qui est trop lâche).
- Source, level, etc. : inchangés.

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : F-DT-07 Ancienneté a déjà un champ années/mois séparé. F-FA-05/06, F-IM-*, F-FA-07 n'ont pas d'ancienneté.
- [x] **Autres pays** : le champ mois est universel (FR + BE).
- [x] **Autres domaines** : N/A.
- [x] **Autres UI patterns** : cohérence F-DT-07 (déjà en place).
- [x] **Autres flows transversaux** : aucun.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript** : `IndemniteComparatifRequest`, `IndemniteComparatifResponse` étendus.
- [x] **Record / DTO backend** : `IndemniteComparatifRequest` + `IndemniteComparatifResponse` étendus.
- [x] **Service / logique métier** : calcul des fourchettes utilise le total en mois.
- [x] **Entité JPA + schéma DB** : ajouter colonne `anciennete_mois` nullable (migration 074).
- [x] **Tests existants** : adapter aux nouveaux champs.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|---|---|---|
| F-DT-09 | Oui | Intégré dans cette SF |
| Autres outils | Non | Pas de champ ancienneté similaire |

### Décision

- [x] Étendu à la cible applicable
- [x] Non applicable aux autres outils

---

## Critères d'acceptation

- [ ] Migration `074-add-anciennete-mois-to-indemnite-comparatifs.xml` ajoute colonne `anciennete_mois INTEGER` nullable.
- [ ] `IndemniteComparatifRequest` ajoute `int ancienneteMois` (obligatoire, validation 0-11).
- [ ] `IndemniteComparatifResponse` ajoute `int ancienneteMois`.
- [ ] Entité `IndemniteComparatif` : champ `ancienneteMois` + persistance.
- [ ] Service : validation + persistance + calcul total mois pour fourchettes.
- [ ] Frontend `IndemniteComparatifRequest` / `Response` TS model étendus.
- [ ] `IndemniteComparatifSectionComponent` : signal `ancienneteMois`, input HTML.
- [ ] Pré-remplissage IA de `ancienneteMois` depuis `synthesis.compensationEstimate.ancienneteMois`.
- [ ] Alerte de cohérence `ANCIENNETE` compare total mois, seuil 1 mois.
- [ ] Rétrocompat : résultats legacy sans `ancienneteMois` chargent avec mois=0.
- [ ] Tests backend : request avec mois valide, mois invalide (−1, 12), round-trip POST/GET.
- [ ] Tests frontend : alerte ne se déclenche pas si écart < 1 mois, se déclenche si ≥ 1 mois.
- [ ] 957+ tests frontend verts, build OK.

---

## Périmètre

### Hors scope

- Toucher F-DT-07 (déjà OK).
- Changer la logique de calcul des fourchettes Macron/CCT 109 (seulement la conversion en mois totaux).
- Refactoriser le pattern ancienneté en composant réutilisable (pas de gain immédiat).

---

## Contraintes de validation

| Champ | Obligatoire | Format | Normalisation |
|---|---|---|---|
| `ancienneteMois` | Oui | entier 0-11 | clamp côté backend si nécessaire |

---

## Technique

### Endpoints

Pas de nouveau endpoint. Request/Response enrichis.

### Tables impactées

| Table | Opération | Notes |
|---|---|---|
| `indemnite_comparatifs` | ALTER — 1 colonne nullable | migration 074 |

### Migration Liquibase

- [x] Oui — `074-add-anciennete-mois-to-indemnite-comparatifs.xml`, réversible (DROP COLUMN).

### Composants backend

- `IndemniteComparatif` (entité), `IndemniteComparatifRequest` / `Response` (records), `IndemniteComparatifService` (validation + persistance + calcul adapté).

### Composants Angular

- `core/models/indemnite-comparatif.model.ts` : 2 champs ajoutés.
- `IndemniteComparatifSectionComponent` : signal + input + prefill + cohérence.

---

## Plan de test

### Tests unitaires backend

- [ ] POST avec `ancienneteMois=3` → persisté, retourné par GET.
- [ ] POST avec `ancienneteMois=-1` → 400.
- [ ] POST avec `ancienneteMois=12` → 400.
- [ ] GET sur dossier legacy (colonne NULL) → `ancienneteMois=0` dans la réponse.

### Tests unitaires frontend

- [ ] Pré-remplissage depuis synthesis : `ancienneteMois` set.
- [ ] Alerte `ANCIENNETE` ne se déclenche pas si user 16 ans 1 mois et IA 16 ans 1 mois.
- [ ] Alerte se déclenche si user 16 ans 0 mois et IA 16 ans 6 mois (écart 6 mois ≥ 1).
- [ ] Non-régression : tests existants adaptés.

### Validation manuelle

- [ ] Staging : dossier Martin, F-DT-09 → champ mois visible + pré-rempli + pas de faux positif quand l'IA a mois = 1.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** — extension localisée de l'outil.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `IndemniteComparatifService` | Extension request/response + calcul | Tests existants |
| `IndemniteComparatifSectionComponent` | Nouveau champ UI + alerte ajustée | Tests existants + nouveaux |

### Smoke tests E2E

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-09-04 Done` — pattern request/response enrichi en place.
- `SF-DT-09-05 Done` — extraction fiable.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi colonne nullable** : rétrocompat pour dossiers legacy, pas de backfill.
- **Pourquoi seuil 1 mois (pas 0)** : tolérer les petits arrondis possibles ; 1 mois est suffisamment fin pour être actionnable sans faux positifs.
- **Pourquoi ne pas utiliser un float `ancienneteTotaleAnnees`** : cohérence avec le pattern existant années+mois séparés (F-DT-07 idem, `compensation_data.anciennete_annees` + `anciennete_mois`).
