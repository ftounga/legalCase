# Feature — F-237 Fermeture des angles morts du garde-fou pré-fill IA

## Identifiant

`F-237`

## Statut

`draft`

## Date de création

2026-05-11

---

## Objectif fonctionnel

Étanchéifier la gouvernance de F-236 en fermant les 3 angles morts résiduels identifiés post-merge :
1. Le test d'intégrité actuel ne vérifie pas la **parité runtime↔static** — un agent peut faire mentir le compteur (`static getPrefillCount` retournant 0 pendant que le runtime remplit 5 champs).
2. Le **helper partagé** `<ComponentName>PrefillRules.ts` n'est pas obligatoire — 14 composants sur 103 ont leur static dupliqué hors helper, réintroduisant le risque de divergence future.
3. La **règle CLAUDE.md ligne 207** est un mur de 80+ lignes en une seule cellule de tableau ; le point (6) sur `static getPrefillCount` y est noyé et peut être sauté par un agent qui scanne rapidement.

## Valeur utilisateur

Sans F-237 : (A) un futur agent peut livrer un composant avec `static getPrefillCount` qui ment, badge faux, bug produit silencieux — exactement le bug que F-236 vient de corriger sur F-FA-07. (B) duplication tolérée → divergence inévitable à long terme. (C) règle illisible = règle régulièrement sautée.

Avec F-237 : garde-fou structurel impossible à contourner, helper obligatoire par construction, règle CLAUDE.md scannable en 10 secondes.

---

## Périmètre V1

### Inclus

- **Test de parité runtime↔static** : étendre `prefill-count-integrity.spec.ts` (ou nouveau spec dédié) qui pour chaque entrée TOOL_REGISTRY :
  - Compare `static getPrefillCount(sampleInput) === computePrefillCount(sampleInput)` du helper exporté
  - Détecte par référence d'identité que les 2 fonctions sont **les mêmes** (i.e. `Component.getPrefillCount === <ComponentName>PrefillRules.computePrefillCount`)
  - Exempte explicitement les wrappers `count=0` via une convention déclarative (cf. SF-237-02)
- **Test présence helper obligatoire** : pour chaque composant TOOL_REGISTRY non-wrapper, vérifier la présence du fichier `<component>-prefill-rules.ts` à côté + export `<ComponentName>PrefillRules.computePrefillCount`. Wrappers `count=0` exemptés via étiquette explicite (commentaire JSDoc `@prefillWrapper` ou symbole statique `static readonly PREFILL_COUNT_ALWAYS_ZERO = true;`).
- **Refactor règle CLAUDE.md ligne 207** : extraire en sections numérotées séparées, chaque sous-point sur sa propre ligne dans le tableau, lisibilité <10s.

### Exclus (hors périmètre)

- Refactor des 14 composants sans helper (sera traité dans SF-237-02 selon l'étiquetage)
- Création de nouveaux outils décisionnels
- Modification du contrat helper établi par F-236 SF-236-01

---

## Sous-fonctionnalités (Subfeatures)

| ID | Titre | Statut | Dépendances |
|----|-------|--------|-------------|
| SF-237-01 | Test parité runtime↔static via identité de référence avec le helper | `ready` | — |
| SF-237-02 | Test présence helper obligatoire + convention wrappers `count=0` | `draft` | SF-237-01 |
| SF-237-03 | Refactor lisibilité règle CLAUDE.md ligne 207 (sections numérotées) | `ready` | — (parallélisable) |

---

## Dépendances techniques

### Composants Angular

- `frontend/src/app/case-files/decisional-tools-panel/prefill-count-integrity.spec.ts` — étendu
- 14 composants à étiqueter `@prefillWrapper` ou équivalent (cf. SF-237-02)
- Convention de nommage helper consolidée (déjà documentée dans `contract-prefill-rules.md`)

### Documentation

- `CLAUDE.md` — refactor de la règle ligne 207

---

## Dépendances externes

| Feature | Raison | Statut |
|---------|--------|--------|
| F-236 (5/5 SF) | Pose le contrat helper + test d'intégrité initial | `done` |

`toutes résolues`

---

## Critères d'acceptation de la feature

- [ ] Le test d'intégrité échoue si un composant TOOL_REGISTRY a `static getPrefillCount` qui diverge du helper (test de parité actif)
- [ ] Le test d'intégrité échoue si un composant TOOL_REGISTRY n'a pas son fichier helper `<component>-prefill-rules.ts` (sauf wrappers explicitement étiquetés)
- [ ] La règle CLAUDE.md sur le composant décisionnel est lisible en < 10s — chaque sous-point sur sa propre ligne dans le tableau
- [ ] `npm test` global passe (0 régression sur les 5178 tests existants)
- [ ] Démo de violation : retirer le helper d'un composant → CI échoue avec message clair ; ré-attacher → CI passe

---

## Notes et décisions

### Convention wrappers `count=0`

Les wrappers (case-deadlines, transaction info, certains scoring affichage) n'ont pas de logique de pré-fill mais doivent quand même exposer `static getPrefillCount(): number { return 0; }` pour passer le test d'intégrité actuel. Ils n'ont pas besoin d'un fichier helper séparé.

**Convention retenue** : marquage explicite via un attribut statique de classe :

```typescript
class FooWrapperSectionComponent {
  static readonly PREFILL_COUNT_ALWAYS_ZERO = true;
  static getPrefillCount(): number { return 0; }
}
```

Le test SF-237-02 exempte ces composants de la règle helper obligatoire mais vérifie que `getPrefillCount` retourne bien 0.

Alternative considérée : convention via nom de classe (`*WrapperSection`) — rejetée car trop implicite.

### Pourquoi un test de parité plutôt qu'une "vérification de logique" plus fine

Une vérification plus fine (générer des inputs aléatoires, faire tourner `prefillFromAi()` runtime, compter les signals modifiés, comparer avec `getPrefillCount`) serait possible mais lourde et fragile. La **garantie par identité de référence** est plus simple : si `Component.getPrefillCount === HelperPrefillRules.computePrefillCount` (même pointer), la divergence est mathématiquement impossible. Coût : 0. Robustesse : maximale.

### Ordre d'exécution

- SF-237-01 et SF-237-03 peuvent tourner en parallèle (touchent des fichiers différents : spec + docs)
- SF-237-02 séquentielle après SF-237-01 (étend la même spec)
