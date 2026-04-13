# Mini-spec — F-IA-03 / SF-IA-03-04 Cohérence IA sur F-DT-07 Ancienneté

## Identifiant

`F-IA-03 / SF-IA-03-04`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur les outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-04-coherence-anciennete`

---

## Objectif

Étendre le moteur de cohérence à F-DT-07 Ancienneté : quand l'avocat saisit une valeur qui diverge au-delà d'un seuil de tolérance de ce que l'IA a extrait (`travailExtractedData` déjà exposé par F-IA-01), afficher un badge `warning` avec tooltip rappelant la valeur IA. Aucun `blocker` : l'ancienneté est factuelle, pas décisionnelle.

---

## Comportement attendu

### Cas nominal

1. `AncienneteSectionComponent` reçoit déjà `aiData: TravailExtractedData` (SF-IA-01, existant).
2. Un nouveau computed `coherenceAlerts` compare en temps réel chaque champ saisi avec la valeur IA correspondante en appliquant le seuil de tolérance propre au champ.
3. Si l'écart dépasse le seuil → badge orange à côté du champ + tooltip "L'IA a détecté : <valeur>".
4. Un bandeau récap "X incohérence(s) avec l'analyse IA" s'affiche au-dessus du formulaire si ≥ 1 alerte.
5. L'alerte est informative : le bouton "Calculer" reste actif.
6. Les alertes se recalculent en temps réel à chaque changement d'input.

### Mapping champ ↔ donnée IA + seuils

| Champ saisi | Valeur IA (`travailExtractedData`) | Seuil déclencheur |
|---|---|---|
| `conventionCode` | `conventionCollective` | mismatch exact (sensible casse, upper-case) |
| `dateEntree` | `dateEntree` | écart ≥ 15 jours calendaires |
| `salaireBase` | `salaireBrutMensuel` | écart absolu / salaire IA ≥ 5 % |
| `congesContrat` | `congesContractuels` | écart ≥ 1 jour entier |
| `primeContrat` | `primeAncienneteContractuelle` | écart ≥ 0,5 point de pourcentage |

### Règles

- Si la valeur IA est `null` ou absente → pas de comparaison, pas d'alerte sur ce champ.
- Si la valeur avocat est `0` ou chaîne vide sur un champ où l'IA a une valeur → **pas d'alerte** (l'avocat n'a probablement pas encore renseigné).
- Si la valeur avocat égale strictement la valeur IA → pas d'alerte (trivial).
- Si l'écart est sous le seuil → pas d'alerte (tolérance aux arrondis et erreurs mineures).
- Un seul niveau d'alerte : `warning`. Pas de `blocker`. Le compteur "bloquantes" n'est pas affiché.
- Si `aiData` est absent → composant fonctionne comme avant, aucune alerte.
- Si un résultat sauvegardé existe et que l'avocat n'a pas ouvert le formulaire en édition → pas d'alerte (le résultat est figé).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Date IA malformée | Comparaison ignorée silencieusement |
| Valeur IA = `null` | Pas d'alerte sur ce champ |
| Valeur avocat non renseignée (0, "") | Pas d'alerte |
| `aiData` absent | Aucune alerte, comportement actuel préservé |

---

## Critères d'acceptation

- [ ] Nouveau computed `coherenceAlerts` dans `AncienneteSectionComponent`, strictement en lecture (aucun side effect).
- [ ] Les 5 champs sont comparés avec les règles de seuil décrites.
- [ ] Badge orange à côté de chaque champ en alerte, avec tooltip `"L'IA a détecté : <valeur>"`.
- [ ] Bandeau récap conditionnel en haut du formulaire.
- [ ] Seuils respectés : convention exact match, date 15j, salaire 5 %, congés 1j, prime 0,5 pt.
- [ ] Alerte disparaît dès que l'avocat revient sous le seuil.
- [ ] Pas d'alerte quand `aiData` null, quand champ IA null, ou quand champ avocat vide/0.
- [ ] Aucune régression sur le prefill existant ou le flux calcul.
- [ ] Tests unitaires frontend couvrent les 5 mappings × matrice (match / écart sous seuil / écart sur seuil / IA null / avocat vide / aiData absent).

---

## Périmètre

### Hors scope (explicite)

- Sources F-96, questions IA, pièces manquantes — non pertinentes pour F-DT-07 (factuel, pas décisionnel).
- Extension à F-DT-09, F-FA-*, F-IM-* → subfeatures ultérieures.
- Blocker / MULTI : inutiles ici (source unique, pas d'enjeu d'interprétation).
- Justification obligatoire : non.
- Modification du calcul d'ancienneté backend : hors scope, seul le contrôle de cohérence est ajouté.

---

## Valeurs initiales

Aucune entité créée. Pur calcul dérivé côté frontend.

---

## Contraintes de validation

| Champ | Obligatoire | Format / Valeurs | Normalisation |
|-------|-------------|------------------|---------------|
| Seuil date | constante | 15 jours calendaires | `abs(Date.parse(a) - Date.parse(b)) / 86400000` |
| Seuil salaire | constante | 5 % de la valeur IA | `abs(a - ia) / max(abs(ia), 1) ≥ 0.05` |
| Seuil prime | constante | 0,5 point | `abs(a - ia) ≥ 0.5` |
| Seuil congés | constante | 1 jour entier | `abs(a - ia) ≥ 1` |
| Convention | enum exact | upper-case match | toupper des deux côtés |

---

## Technique

### Endpoint(s)

Aucun. Subfeature purement frontend — les données `travailExtractedData` sont déjà exposées par `GET /case-analysis` depuis F-IA-01.

### Tables impactées

Aucune.

### Migration Liquibase

- [ ] Oui
- [x] **Non applicable** — purement frontend.

### Composants Angular

- `AncienneteSectionComponent` :
  - Signal miroir `aiDataSignal` pour la réactivité (les inputs existants `aiData` sont synchronisés via `ngOnChanges`)
  - Types `AncienneteCoherenceAlert` et `AncienneteAlertField = 'CONVENTION' | 'DATE_ENTREE' | 'SALAIRE' | 'CONGES' | 'PRIME'`
  - Computed `coherenceAlerts: Record<field, CoherenceAlert>` en temps réel
  - Computed `alertsSummary: { total: number }`
  - Helpers purs : `dateDaysDiff`, `percentDiff`
  - Bandeau + badges dans le template

Pas de changement backend, pas de changement de modèle (`TravailExtractedData` est déjà complet).

---

## Plan de test

### Tests unitaires frontend

Pour chaque champ, tester : match exact, écart sous seuil, écart sur seuil, IA null, avocat vide, + cas transverses.

Convention (`conventionCode`) :
- [ ] Convention IA = "SYNTEC", avocat "SYNTEC" → aucune alerte
- [ ] Avocat "METALLURGIE" → badge warning CONVENTION
- [ ] Avocat "syntec" (casse) → aucune alerte (normalisé)
- [ ] IA null → aucune alerte

Date entrée :
- [ ] Écart 5 jours → aucune alerte
- [ ] Écart 15 jours pile → warning
- [ ] Écart 60 jours → warning
- [ ] Avocat "" → aucune alerte
- [ ] IA date malformée → aucune alerte

Salaire :
- [ ] IA 4000, avocat 4100 (2,5 %) → aucune alerte
- [ ] IA 4000, avocat 4300 (7,5 %) → warning
- [ ] IA 4000, avocat 0 → aucune alerte
- [ ] IA null → aucune alerte

Congés :
- [ ] IA 25, avocat 25 → aucune alerte
- [ ] IA 25, avocat 26 → warning
- [ ] Avocat 0 → aucune alerte

Prime :
- [ ] IA 5, avocat 5 → aucune alerte
- [ ] IA 5, avocat 5,3 (0,3) → aucune alerte
- [ ] IA 5, avocat 6 (1 pt) → warning
- [ ] IA null → aucune alerte

Transverses :
- [ ] `aiData` absent → aucune alerte sur aucun champ
- [ ] Compteur `alertsSummary.total` cohérent
- [ ] Flux calcul existant non régressé
- [ ] Prefill existant non régressé

### Tests d'intégration

- Aucun (frontend seul).

### Isolation workspace

- [x] Non applicable — aucun accès données.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — extension localisée d'un seul composant.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|---|---|---|
| `AncienneteSectionComponent` — flux calcul | aucun (computed ajouté, pas de side effect) | suite tests existants conservée |
| `AncienneteSectionComponent` — prefill IA existant | aucun (le prefill reste à `ngOnInit` / `ngOnChanges`, l'alerte s'ajoute en parallèle) | test prefill préservé |

### Smoke tests E2E concernés

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-01 / SF-IA-01-01` (Done) — expose `travailExtractedData`.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi source unique** : F-DT-07 traite des données factuelles. F-96, questions IA et pièces manquantes ne parlent pas de dates/montants précis. Multiplier les sources ici aurait été du cargo-culting.
- **Pourquoi pas de blocker** : si l'avocat diverge de l'IA sur une date ou un salaire, c'est probablement parce qu'il a une meilleure info (il a lu le contrat). Blocker créerait de la friction injustifiée.
- **Pourquoi des seuils numériques** : pour éviter le bruit des arrondis (salaire 4000 vs 4001) et des erreurs mineures. Les seuils sont calibrés pour ne déclencher que sur des divergences réelles.
- **Pourquoi warning à 15 jours sur les dates** : l'IA peut confondre le 01/09/2018 de début de contrat avec le 01/09/2018 d'une promotion, ou lire une date de manière ambiguë. 15 jours est un seuil qui filtre les confusions mineures (semaines) sans rater les vraies erreurs (mois/années).
- **Pas de persistance des alertes** : c'est un calcul dérivé, recalculé à chaque rendu. Cohérent avec les autres SF F-IA-03.
