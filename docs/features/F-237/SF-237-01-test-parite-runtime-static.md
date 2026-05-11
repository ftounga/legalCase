# Mini-spec — F-237 / SF-237-01 Test parité runtime↔static via identité de référence

## Identifiant

`F-237 / SF-237-01`

## Feature parente

`F-237` — Fermeture des angles morts du garde-fou pré-fill IA

## Statut

`ready`

## Date de création

2026-05-11

## Branche Git

`feat/SF-237-01-test-parite-runtime-static`

---

## Objectif

Étendre `prefill-count-integrity.spec.ts` pour vérifier que `Component.getPrefillCount` est **la même référence** que `<ComponentName>PrefillRules.computePrefillCount` du helper — garantit la parité runtime/static par identité de pointeur.

---

## Comportement attendu

### Cas nominal

Pour chaque entrée TOOL_REGISTRY :
1. Tenter d'importer dynamiquement le helper `<component>-prefill-rules.ts` à côté du composant
2. Si le helper existe et exporte `<ComponentName>PrefillRules.computePrefillCount` :
   - Vérifier `Component.getPrefillCount === <ComponentName>PrefillRules.computePrefillCount` (identité stricte)
3. Si le composant est étiqueté wrapper (`static PREFILL_COUNT_ALWAYS_ZERO === true`) :
   - Exempter de la règle helper, vérifier juste que `getPrefillCount` retourne 0
4. Sinon :
   - **Échec** : helper manquant ou static non aligné

### Cas d'erreur

| Situation | Message d'échec |
|-----------|----------------|
| Helper introuvable | `${toolId}: helper file '<component>-prefill-rules.ts' not found` |
| Helper sans `computePrefillCount` | `${toolId}: helper exports no '<ComponentName>PrefillRules.computePrefillCount' function` |
| Static ≠ helper fonction | `${toolId}: static getPrefillCount references a different function than helper.computePrefillCount — divergence possible` |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 103 entrées TOOL_REGISTRY — cible directe
- [ ] **Autres pays** : non applicable (test générique)
- [ ] **Autres domaines** : non applicable
- [x] **Autres UI patterns** : pattern miroir de `LegalReferentialDescriptionIntegrityIT` et `DecisionToolVisibilityIntegrityIT` — cohérence des garde-fous
- [ ] **Autres flows transversaux** : non applicable

### Niveaux de vérification

- [ ] Modèle TypeScript / API exposée — non applicable
- [ ] Service / logique métier — non applicable
- [x] Tests existants — étendus

### Cas spécifique : nouveau pattern UI ou service partagé

Non applicable — test pur.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| 103 entrées TOOL_REGISTRY | Oui | Test exhaustif |
| Wrappers `count=0` | Oui | Exemption explicite |
| Garde-fous existants | Cohérence | Mêmes conventions |

### Décision

- [x] Étendu à toutes les cibles applicables

---

## Conformité F-IA-04

- [x] **Non applicable** — SF d'infrastructure CI.

---

## Critères d'acceptation

- [ ] Le test étendu PASSE sur master post-SF-236 (89 helpers présents + 14 wrappers étiquetés)
- [ ] Le test ÉCHOUE si on remplace `Component.getPrefillCount` par une copie locale (test du test — commit jetable)
- [ ] Le test ÉCHOUE si on retire le helper d'un composant non-wrapper
- [ ] Message d'échec mentionne `tool_id` + nom de classe + raison précise
- [ ] `npm test` global : 0 régression

---

## Périmètre

### Hors scope (explicite)

- Refactor des 14 composants sans helper (couvert par SF-237-02 — l'étiquette wrapper d'abord)
- Modification de la règle CLAUDE.md (couvert par SF-237-03)

---

## Plan de test

### Tests unitaires

- Test du test : commit jetable retirant helper d'1 composant → CI échoue → revert → CI passe
- Test du test : modification du static pour le détacher du helper → échoue → revert → passe

### Tests d'intégration / E2E

Non applicable.

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** (CI test additif)

### Smoke tests E2E concernés

- [x] Aucun

---

## Dépendances

- F-236 ✅ (test d'intégrité initial)

---

## Notes et décisions

### Méthode d'import dynamique du helper

Jest support pour imports dynamiques :
```typescript
const helperPath = `./${componentFolder}/${componentBaseName}-prefill-rules`;
let helper: { [key: string]: { computePrefillCount: Function } } | null = null;
try {
  helper = await import(helperPath);
} catch {
  helper = null;
}
```

Si l'import échoue → fichier absent → marquer en échec (sauf wrapper exempté).

### Convention du nom du helper

`<ComponentName>PrefillRules` exporté depuis `<component>-prefill-rules.ts`. Cf. `docs/features/F-236/contract-prefill-rules.md` §1.

### Tolérance des wrappers

Un composant marqué `static readonly PREFILL_COUNT_ALWAYS_ZERO = true` est exempté de la règle helper mais reste soumis à `getPrefillCount({}) === 0`.
