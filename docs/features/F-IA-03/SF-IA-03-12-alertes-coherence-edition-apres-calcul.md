# Mini-spec — F-IA-03 / SF-IA-03-12 Alertes de cohérence actives pendant l'édition après calcul initial

## Identifiant

`F-IA-03 / SF-IA-03-12`

## Feature parente

`F-IA-03` — Contrôle de cohérence IA sur outils décisionnels

## Statut

`draft`

## Date de création

2026-04-14

## Branche Git

`feat/SF-IA-03-12-alertes-coherence-edition-apres-calcul`

---

## Objectif

Corriger une régression transversale découverte pendant le test 2 Martin BTP : les alertes de cohérence IA (F-IA-03) ne se déclenchent plus sur 7 outils décisionnels dès que l'avocat a cliqué une première fois sur "Calculer / Comparer / Résoudre / Générer". Quand il revient en mode édition via "Modifier" et change une valeur, aucun badge ne s'affiche — à la différence de F-DT-08 Validité licenciement qui fonctionne correctement.

Cause : une garde `|| this.result()` (ou équivalent : `this.decision()`, `this.recours()`) dans le `computed` `coherenceAlerts` de 7 composants, qui invalide le calcul d'alertes dès qu'un résultat est présent — y compris quand l'utilisateur est de retour en mode édition (`showForm()=true`) après avoir cliqué "Modifier".

---

## Comportement attendu

### Cas nominal

1. L'avocat ouvre un outil décisionnel (ex. F-DT-07 Ancienneté).
2. Il voit le formulaire pré-rempli par l'IA ; des badges de cohérence apparaissent s'il y a déjà divergence.
3. Il clique **Calculer**. Le résultat s'affiche, le formulaire est masqué (`showForm()=false`).
4. Il clique **Modifier**. Le formulaire réapparaît (`showForm()=true`), les valeurs du résultat sont pré-remplies.
5. Il change une valeur contrairement à l'IA → **badge de cohérence s'affiche immédiatement** (c'est le fix).
6. Il peut recliquer **Calculer** pour obtenir un nouveau résultat ; ou **Modifier** à nouveau, les alertes restent actives tant que `showForm()=true`.

### Règle générale retenue

- **Les alertes de cohérence sont gouvernées uniquement par `showForm()`**, pas par la présence d'un résultat précédent.
- `!this.showForm() → return {}` (rien à signaler sur un bloc résultat passif, on garde ce gate).
- `this.result() → return {}` : **retiré** — c'est le patch.

C'est le comportement actuel de **F-DT-08 Validité licenciement**, qui sert de référence.

### Périmètre exact du fix

Fichiers touchés (7) :

| Outil | Fichier | Ligne condition | Pattern à retirer |
|-------|---------|----------------|-------------------|
| F-DT-07 Ancienneté | `anciennete-section.component.ts` | 56 | `\|\| this.result()` |
| F-DT-09 Comparateur indemnités | `indemnite-comparatif-section.component.ts` | 102 | `\|\| this.result()` |
| F-FA-05 Partage immobilier | `partage-immobilier-section.component.ts` | 103 | `\|\| this.result()` |
| F-FA-06 Calendrier garde | `calendrier-garde-section.component.ts` | 80 | `\|\| this.result()` |
| F-IM-05 Titre séjour | `immigration-title-decision-section.component.ts` | 114 | `\|\| this.decision()` |
| F-IM-06 Recours | `immigration-recours-section.component.ts` | 98 | `\|\| this.recours()` |
| F-IM-07 Droit au travail | `immigration-work-right-section.component.ts` | 101 | `\|\| this.result()` |

Fichier **non modifié** :
- `licenciement-section.component.ts` (F-DT-08) — déjà correct.
- `divorce-checklist-section.component.ts` (F-FA-07) — pattern différent (pas de cycle form → résultat), la garde `if (!r) return {}` est correcte.

### Cas d'erreur

| Situation | Comportement après fix |
|-----------|----------------------|
| `showForm() = false` (bloc résultat affiché, pas le formulaire) | Aucun badge — gate `!showForm()` toujours actif |
| Premier chargement, pas de `result()` encore | Badges actifs comme aujourd'hui — pas de changement |
| Après 1er Calculer puis Modifier | **Badges actifs — c'est le fix** |
| AI data (`aiData`, `procedureChecks`, `aiQuestions`) absente | Aucun badge — comportement inchangé |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 9 outils porteurs d'alertes de cohérence F-IA-03 (F-DT-07/08/09, F-FA-05/06/07, F-IM-05/06/07)
- [x] **Autres pays** : non applicable — la garde est au niveau du composant TypeScript, pas conditionnée au pays
- [x] **Autres domaines** : applicable — les 7 outils concernés couvrent DROIT_DU_TRAVAIL, DROIT_FAMILLE et DROIT_IMMIGRATION
- [x] **Autres UI patterns** : le pattern "formulaire → résultat → Modifier → formulaire" est spécifique à ces 7 outils ; F-FA-07 (toggle inline) et F-DT-08 (formulaire persistant) utilisent un autre pattern
- [x] **Autres flows transversaux** : non applicable — pas d'impact auth / workspace / plans / navigation

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-07 Ancienneté | Oui | Intégré dans cette SF |
| F-DT-08 Validité licenciement | Non | Ne contient pas la garde `result()` — c'est la référence comportementale |
| F-DT-09 Comparateur indemnités | Oui | Intégré dans cette SF |
| F-FA-05 Partage immobilier | Oui | Intégré dans cette SF |
| F-FA-06 Calendrier garde | Oui | Intégré dans cette SF |
| F-FA-07 Checklist divorce | Non | Pattern différent (toggle inline, pas de cycle form → résultat). Garde `if (!r) return {}` correcte (alertes dépendent du résultat chargé) |
| F-IM-05 Titre séjour | Oui | Intégré dans cette SF |
| F-IM-06 Recours | Oui | Intégré dans cette SF |
| F-IM-07 Droit au travail | Oui | Intégré dans cette SF |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature (7 outils)
- [ ] Subfeature(s) parallèle(s) créée(s)
- [ ] Backlog VN
- [x] Non applicable pour F-DT-08 et F-FA-07 (justification explicite ci-dessus)

---

## Critères d'acceptation

- [ ] Les 7 fichiers listés voient leur garde `|| this.result()` / `|| this.decision()` / `|| this.recours()` retirée du `computed coherenceAlerts`.
- [ ] La garde `!this.showForm()` est conservée dans chaque cas.
- [ ] F-DT-08 et F-FA-07 ne sont pas touchés.
- [ ] Les tests Jest existants restent verts (non-régression).
- [ ] Nouveau(x) test(s) couvrant le scénario "after calculate → editForm → modifier champ → badge s'affiche" sur au moins 3 composants représentatifs (F-DT-07, F-DT-09, F-IM-05).
- [ ] Build Angular OK, 932+ tests frontend verts.

---

## Périmètre

### Hors scope (explicite)

- Toucher F-DT-08 ou F-FA-07 (corrects dans leur pattern respectif).
- Corriger le timing de masquage F-DT-08/F-DT-10 après analyse qui nécessite un F5 (problème distinct, sujet dédié à traiter séparément).
- Refondre le template des mini-specs pour inclure un "reference file" par pattern (meta-gouvernance, hors scope).
- Ajouter la cohérence IA à F-DT-08 (déjà complète sous SF-IA-03-01/02/03) ou à F-FA-07 (déjà complète sous SF-IA-03-06).
- Ajout de nouveaux champs surveillés sur les outils corrigés.

---

## Valeurs initiales

Sans objet — uniquement une condition retirée.

---

## Contraintes de validation

Sans objet — pas de donnée saisie.

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- 7 composants modifiés dans `frontend/src/app/case-files/*-section/`.
- 1 ligne éditée par fichier (condition du `computed`).
- Ajout de tests Jest ciblés.

### Backend

Aucun impact.

---

## Plan de test

### Tests unitaires Jest

- [ ] `AncienneteSectionComponent` : scénario Calculer → editForm → modifier salaire → `coherenceAlerts().SALAIRE` présent.
- [ ] `IndemniteComparatifSectionComponent` : scénario Comparer → editForm → modifier ancienneté → `coherenceAlerts().ANCIENNETE` présent.
- [ ] `ImmigrationTitleDecisionSectionComponent` : scénario Résoudre → editForm → modifier motif → `coherenceAlert()` présent.
- [ ] Non-régression : les tests existants Jest des 7 composants restent verts.

### Tests d'intégration

- [x] N/A — frontend pur.

### Isolation workspace

- [x] N/A — garantie au niveau backend, pas impactée.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — retrait d'une condition dans un `computed` signal local à chaque composant.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| 7 composants `*-section` | Alertes actives après 1er calcul | Jest existants + nouveaux ciblés |
| Tableau de bord F-IA-02 | Aucun | — |
| Endpoints backend | Aucun | — |

### Smoke tests E2E concernés

- [ ] Aucun — comportement purement client.

---

## Dépendances

### Subfeatures bloquantes

- `F-IA-03 Terminée` (toutes sous-features 01 à 11 mergées) — la garde à retirer a été introduite par SF-IA-03-04/05/06/07/08/09/10/11.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi un fix transversal** : le bug a été identifié sur F-DT-07 pendant le test 2, mais le scan transversal montre que 6 autres outils sont affectés par le même pattern. Un fix outil par outil créerait 7 PRs redondantes et des oublis.
- **Pourquoi F-DT-08 sert de référence** : c'était le premier outil couvert par F-IA-03 (SF-IA-03-01) et il n'a pas la garde. Les subfeatures suivantes ont introduit la garde par inadvertance en copiant le scaffolding d'un autre outil.
- **Pourquoi ne pas toucher F-FA-07** : la checklist divorce n'a pas de mode "calculate" : chaque toggle d'étape / pièce est persisté immédiatement. La garde `if (!r) return {}` y signifie "alertes dépendent de la présence du state chargé", ce qui est correct.
- **Pourquoi retirer la garde au lieu d'effacer `result()` dans `editForm()`** : effacer `result()` perdrait l'état côté utilisateur s'il annule l'édition. Retirer la garde préserve le state et affiche les alertes en temps réel, comportement attendu.
- **Pourquoi ne pas reparler du timing F5 ici** : problème distinct (synthesis signal qui ne propage pas). Mélanger les deux fixes dans une même SF violerait "une préoccupation à la fois".
