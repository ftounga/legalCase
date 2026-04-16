# Mini-spec — F-DT-07 / SF-DT-07-05 Prime d'ancienneté : prefill convention + alerte + fix calcul

## Identifiant

`F-DT-07 / SF-DT-07-05`

## Feature parente

`F-DT-07` — Barème d'ancienneté et congés conventionnels

## Statut

`draft`

## Date de création

2026-04-16

## Branche Git

`feat/SF-DT-07-05-prime-anciennete-prefill-fix-calcul`

---

## Objectif

Triple correctif sur F-DT-07 Prime d'ancienneté observés pendant le Test 2 Martin :

1. **Bug calcul backend** : `AncienneteCalculator` utilise toujours la prime du bareme conventionnel comme `primePourcentage`, ignorant la valeur saisie par l'avocat. Si l'avocat saisit 22% (contrat plus généreux que la convention 12%), le résultat affiche encore 12%.
2. **Pas de validation IA quand on modifie la prime** : actuellement le check ne se fait que vs `aiData.primeAncienneteContractuelle` qui est null si le contrat est silencieux → aucune alerte ne se déclenche jamais.
3. **Pas de pré-remplissage** : quand l'IA n'extrait pas la prime (contrat silencieux), le champ reste à 0 → l'avocat ne voit pas que la convention en garantit 12%, doit attendre Calculer pour le découvrir.

Mêmes corrections appliquées en parallèle au champ `congesContrat` (même bug structurel).

---

## Comportement attendu

### Cas nominal après fix

1. L'avocat ouvre F-DT-07 sur un dossier analysé. Prefill :
   - Si l'IA a extrait `primeAncienneteContractuelle` → utilisée.
   - Sinon → la valeur de la convention (bareme) est pré-remplie automatiquement (ex. 12% pour BTP à 15 ans).
   - Idem pour `congesContrat`.
2. L'avocat modifie la prime à 22% (contrat plus généreux).
3. Badge de cohérence : si user diverge de la convention → warning (écart ≥ 0,5pt sur prime, ≥ 1 jour sur congés).
4. L'avocat clique Calculer.
5. Résultat : `primePourcentage = max(primeContrat, primeBareme)` = 22% (la valeur effective la plus favorable au salarié). Le montant calculé utilise 22%.
6. Carte "Écart" : ne s'affiche QUE si `primeContrat < primeBareme` (contrat sous le minimum conventionnel).

### Bug racine — fix

**`AncienneteCalculator.java` ligne 60-67** : actuellement
```java
BigDecimal primePourcentage = bareme.primesAnciennete().stream()...max...;  // bareme uniquement
BigDecimal primeMontant = salaireBase.multiply(primePourcentage)...;
```

**Après fix** :
```java
BigDecimal primeBareme = bareme.primesAnciennete().stream()...max...;
BigDecimal primeContratEff = primeContrat != null ? primeContrat : BigDecimal.ZERO;
BigDecimal primeEffective = primeContratEff.max(primeBareme);  // max(contrat, bareme)
BigDecimal primeMontant = salaireBase.multiply(primeEffective)...;
```

Idem pour congés : `congesEffectifs = max(congesContrat, congesTotal)`.

### Prefill bareme — frontend

Le composant doit connaître le bareme actuel pour pré-remplir et déclencher l'alerte avant Calculer. Solution choisie : nouvel endpoint léger qui expose le bareme par code convention.

- **Nouvel endpoint** : `GET /api/v1/anciennete/baremes/{conventionCode}`
  - Réponse : `{ conventionCode, conventionLabel, country, congesLegauxJours, primesAnciennete: [{ancienneteMinAnnees, pourcentage}], congesSupplementaires: [{ancienneteMinAnnees, joursSupplementaires}] }`
  - Pas d'authentification stricte sur le contenu du référentiel (statique), mais on garde la même protection OIDC que les autres endpoints pour cohérence.
- Le frontend appelle cet endpoint quand `conventionCode` change OU à l'ouverture si pas de saved data.
- Cache simple côté front (Map de bareme par code) pour éviter les appels répétés.

### Prefill — règles précises

À l'ouverture du formulaire (après que `aiData` ET le bareme sont chargés) :
- `conventionCode` : déjà géré (IA → conventionCollective).
- `dateEntree` : déjà géré (IA → dateEntree).
- `salaireBase` : déjà géré (IA → salaireBrutMensuel).
- `congesContrat` :
  - Si `aiData.congesContractuels != null` → utiliser cette valeur.
  - Sinon → `bareme.congesLegauxJours + maxCongesSuppApplicables(annees)`.
- `primeContrat` :
  - Si `aiData.primeAncienneteContractuelle != null` → utiliser cette valeur.
  - Sinon → `maxPrimePourcentageApplicable(bareme, annees)`.

### Alertes de cohérence — règle élargie

Comparer `primeContrat` user à **deux références** :
- `aiData.primeAncienneteContractuelle` si fournie (priorité IA).
- À défaut, `bareme.primePourcentage` (la convention).

Si user diverge de la référence prioritaire → badge warning.

### Cas d'erreur

| Situation | Comportement |
|---|---|
| Endpoint bareme retourne 404 (convention inconnue) | Pas de prefill auto, fallback comportement actuel (champs à 0) |
| Bareme chargé mais sans `primesAnciennete` (convention sans prime) | Prefill à 0 ou à la valeur extraite IA |
| User saisit prime négative | Validation existante inchangée |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Outils métier** : seul F-DT-07 a un calcul "max(contrat, convention)" structurel. Les autres outils (F-DT-09, F-IM-*, etc.) ne calculent pas selon ce pattern.
- [x] **Autres pays** : applicable FR + BE (les conventions belges ont aussi des primes / congés conventionnels).
- [x] **Autres domaines** : N/A.
- [x] **UI patterns** : nouveau pattern "prefill depuis référentiel statique via endpoint dédié" — peut servir d'exemple pour futurs outils similaires.
- [x] **Flows transversaux** : aucun.

### Niveaux de vérification

- [x] **Modèle TS** : `AncienneteResponse` étendu (ajout 2 champs bareme exposés). Nouveau modèle `BaremeResponse`.
- [x] **DTO backend** : `AncienneteResponse` + `BaremeResponse` (nouveau record).
- [x] **Service / logique** : `AncienneteCalculator` fix prime + congés effectifs.
- [x] **Entité JPA + DB** : aucun changement (bareme reste statique en code).
- [x] **Tests existants** : adaptation aux nouvelles signatures + nouveaux cas (prime contrat > bareme).

### Décision

- [x] Étendu à F-DT-07
- [ ] Subfeatures parallèles
- [ ] Backlog
- [x] Non applicable autres outils (pattern spécifique)

---

## Critères d'acceptation

- [ ] `AncienneteCalculator` : `primePourcentage` retourné = `max(primeContrat, primeBareme)`. `primeMontant` utilise cette valeur effective.
- [ ] `AncienneteCalculator` : `congesTotal` retourné = `max(congesContrat, congesLegaux + congesSupp)`. La carte d'écart s'affiche si `congesContrat < (legal + supp)`.
- [ ] Endpoint `GET /api/v1/anciennete/baremes/{conventionCode}` retournant `BaremeResponse`.
- [ ] `AncienneteResponse` étendue avec `baremePrimePourcentage` et `baremeCongesTotal` (pour usage front en lecture passive).
- [ ] Frontend : nouveau service `BaremeService.get(conventionCode)`, cache local en mémoire.
- [ ] Frontend `AncienneteSectionComponent` : prefill `primeContrat` et `congesContrat` depuis bareme si IA vide, après chargement du bareme.
- [ ] Alerte de cohérence sur prime/congés : compare user vs IA-si-fourni-sinon-bareme.
- [ ] Tests backend : prime contrat 22% > bareme 12% → résultat 22%. Prime contrat 5% < bareme 12% → résultat 12%, écart détecté.
- [ ] Tests backend : nouvel endpoint, 200 + JSON conforme, 404 si convention inconnue.
- [ ] Tests frontend : prefill depuis bareme appliqué, alerte vs bareme déclenchée si écart.
- [ ] Tests existants verts.

---

## Périmètre

### Hors scope

- Refacto du référentiel `ConventionBaremeReferentiel` (reste statique en Java).
- Ajout de nouvelles conventions ou de nouveaux paliers d'ancienneté (réutilise l'existant).
- Toucher d'autres outils (uniquement F-DT-07 a ce pattern).
- Migration DB (aucune nécessaire).
- Renommage des termes dans les écarts (cf. SF-118-05 pour terminologie).

---

## Contraintes de validation

| Champ | Obligatoire | Format |
|---|---|---|
| `primeContrat` | Oui (existant) | BigDecimal ≥ 0 |
| `congesContrat` | Oui (existant) | int ≥ 0 |

---

## Technique

### Endpoints

| Méthode | URL | Description |
|---|---|---|
| GET | `/api/v1/anciennete/baremes/{conventionCode}` | Nouveau — retourne le bareme statique |

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants backend

- `AncienneteCalculator` — fix prime + congés effectifs.
- `AncienneteResponse` — 2 champs ajoutés (`baremePrimePourcentage`, `baremeCongesTotal`).
- `BaremeResponse` (nouveau record).
- `AncienneteController` — nouveau endpoint GET bareme.
- `AncienneteService.toResponse` — propage le bareme courant.

### Composants frontend

- `core/services/bareme.service.ts` (nouveau) — GET bareme + cache local.
- `core/models/bareme.model.ts` (nouveau).
- `core/models/anciennete.model.ts` — 2 champs ajoutés.
- `AncienneteSectionComponent` — chargement bareme, prefill élargi, cohérence vs bareme.

---

## Plan de test

### Tests unitaires backend

- [ ] `AncienneteCalculatorTest` : prime contrat > bareme → `primePourcentage` = prime contrat.
- [ ] `AncienneteCalculatorTest` : prime contrat < bareme → `primePourcentage` = bareme + écart détecté.
- [ ] `AncienneteCalculatorTest` : prime contrat = bareme → `primePourcentage` = bareme, pas d'écart.
- [ ] Idem pour congés (3 cas).
- [ ] `AncienneteControllerIT` : GET /baremes/{code} → 200 JSON conforme.
- [ ] `AncienneteControllerIT` : GET /baremes/UNKNOWN → 404.
- [ ] Non-régression : tests existants adaptés.

### Tests unitaires frontend

- [ ] Prefill depuis bareme quand IA fournit pas la prime.
- [ ] Pas de prefill bareme quand IA fournit la prime (priorité IA).
- [ ] Alerte de cohérence : user 5% vs bareme 12% → warning.
- [ ] Alerte ne se déclenche pas si user = bareme (= 12%).

### Validation manuelle

- [ ] Staging : dossier Martin, F-DT-07 → prime affichée à 12% par défaut (BTP), modifier à 22% → calcul affiche 22% en montant.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** structurelle.

### Composants impactés

| Composant | Impact | Test de non-régression |
|---|---|---|
| `AncienneteCalculator` | Logique calcul prime + congés modifiée | Tests existants adaptés |
| `AncienneteResponse` | 2 champs ajoutés (rétrocompat null) | Tests existants |
| `AncienneteController` | Nouveau endpoint | Tests IT existants |
| `AncienneteSectionComponent` | Chargement bareme + prefill élargi | Specs existants + nouveaux |

### Smoke tests E2E

- [ ] Aucun.

---

## Dépendances

### Subfeatures bloquantes

- `SF-DT-07-04 Done` — pattern persistance complète.
- `SF-118-05 Done` — terminologie unifiée (les nouveaux libellés d'écart utilisent les bons termes).

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi exposer le bareme via endpoint** plutôt que dans la response Calculer : l'avocat doit voir la convention AVANT de cliquer Calculer (prefill). Sans endpoint dédié, on devrait soit dupliquer le bareme dans le bundle JS (gestion difficile), soit forcer un calcul fictif (mauvaise UX).
- **Pourquoi `max(contrat, bareme)` pour la prime/congés** : le contrat individuel ne peut pas être moins favorable que la convention (ordre public social). Si le contrat est plus favorable, c'est cette valeur qui s'applique au salarié. C'est le principe juridique élémentaire.
- **Pourquoi seuil 0,5pt sur prime** : aligné avec SF-IA-03-04 (convention `ANCIENNETE` même mécanique).
- **Pourquoi cache front simple** : le bareme est statique (changement très rare). Cache en mémoire suffit, pas besoin de TTL ni de invalidation.
