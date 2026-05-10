# Mini-spec — F-236 / SF-236-03 Correction des divergences runtime/static

## Identifiant

`F-236 / SF-236-03`

## Feature parente

`F-236` — Robustesse pré-fill IA outils décisionnels frontend

## Statut

`draft`

## Date de création

2026-05-10

## Branche Git

`feat/SF-236-03-corrections-divergences`

---

## Objectif

Corriger les divergences `prefillFromAi()` runtime ↔ `static getPrefillCount` détectées par SF-236-01, à minima la divergence connue F-FA-07 (badge `1` mais runtime remplit 2 étapes), en alignant runtime ET static sur la même fonction du helper partagé livré par SF-236-02.

---

## Comportement attendu

### Cas nominal

1. Lister les divergences identifiées par SF-236-01 (au minimum F-FA-07)
2. Pour chaque divergence : choisir le compteur qui reflète l'expérience UX réelle (généralement le maximum runtime, car c'est ce que l'avocat perçoit)
3. Aligner le helper sur ce compteur
4. Vérifier que le test Jest cas N reflète bien le compteur attendu
5. Vérifier le badge sur le panel pour le composant concerné

### Cas d'erreur

| Situation | Comportement |
|-----------|--------------|
| La logique runtime remplit conditionnellement plusieurs champs liés à un même input | Compter chaque champ logique distinct (pas chaque setSignal) |
| La divergence est due à un guard manquant dans le static | Ajouter le guard manquant via le helper |
| La divergence est due à une logique morte dans le runtime | Supprimer la logique morte |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : tous les composants où SF-236-01 a détecté une divergence
- [ ] **Autres pays** : applicable si la divergence concerne un guard de pays
- [ ] **Autres domaines** : applicable si la divergence est cross-domaine
- [ ] **Autres UI patterns** : non applicable
- [ ] **Autres flows transversaux** : non applicable

### Niveaux de vérification

- [x] Modèle TypeScript / API exposée
- [x] Service / logique métier — runtime + static
- [x] Tests existants — étendus pour bétonner le compteur

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — correction de bugs ponctuels.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Composants divergents listés par SF-236-01 | Oui | Correction directe |
| Composants conformes | Non | Préservés |

### Décision

- [x] Étendu à toutes les cibles applicables (toutes les divergences détectées)

---

## Conformité F-IA-04 (SF frontend décisionnelle)

Cette SF modifie des composants décisionnels — tous les blocs F-IA-04 s'appliquent et sont préservés à l'identique (la SF corrige uniquement le compteur, pas le pré-fill ou la validation).

### 1-5. Conformité préservée

Tous les blocs (cohérence visuelle, pré-fill IA, validation F-IA-03, TOOL_REGISTRY symétrique, parité domaines) sont préservés inchangés. Diff = correction du compteur via le helper.

---

## Critères d'acceptation

- [ ] Toutes les divergences listées par SF-236-01 sont corrigées
- [ ] Pour chaque correction, un test Jest atteste la nouvelle parité runtime/static
- [ ] Le badge du panel reflète correctement le compteur runtime sur chaque composant corrigé
- [ ] `npm run build` passe
- [ ] `npm test` passe

---

## Périmètre

### Hors scope (explicite)

- Robustification (couvert par SF-236-04)
- Garde-fou CI (couvert par SF-236-05)

---

## Plan de test

### Tests unitaires

- [ ] Pour chaque composant corrigé : test Jest qui vérifie `runtime → N champs setSignal === getPrefillCount(input) === N`

### Tests d'intégration

Non applicable.

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale**

### Smoke tests E2E concernés

- [x] Aucun

---

## Dépendances

### Subfeatures bloquantes

- SF-236-02 — doit être `done` (helper partagé déployé, sans quoi la correction n'a pas de surface unifiée à corriger)

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

### Cas connu F-FA-07

`getPrefillCount` retourne actuellement `1` (présence de `dateAcceptationPV`), mais `prefillFromAi()` runtime pré-coche **2 étapes** de la checklist (`SIGNATURE_STEP_CODES`). Correction : le helper doit exposer une fonction `countDateAcceptationPV(input): number` qui retourne `2` quand la date est valide (et ce comptage représente fidèlement les 2 étapes pré-cochées). Le runtime applique le même résultat aux 2 signals concernés.

Alternative : réviser `prefillFromAi()` pour ne pré-cocher qu'**une** étape (la plus représentative) → simplifierait à 1. Décision tranchée dans SF-236-01.
