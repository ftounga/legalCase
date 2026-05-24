# Bootstrap initial — F-JU-01 JurisprudenceWatch

Fichiers CSV prêts à coller dans l'onglet **« Bootstrap »** du dashboard
`/super-admin/jurisprudence-watch` (livré par PR #1293, SF-JU-01-06).

## Contexte

F-JU-01 est activée en staging depuis 2026-05-24. Le déclencheur restant
est l'**initialisation** de la table `tool_jurisprudence_mappings` :
pour chaque (outil décisionnel × branche de calcul × mot-clé), le backend
interroge JUDILIBRE, fait évaluer la pertinence par Claude Sonnet, puis
INSERT une (pseudo-)entrée pour amorcer la veille mensuelle.

Sans ce bootstrap, la veille mensuelle n'a rien sur quoi pivoter et
aucune citation jurisprudentielle n'apparaît dans les outils.

## Fichiers

| Fichier | Lignes | Usage |
|---|---|---|
| `bootstrap-batch-1.csv` | 200 | À coller en 1er |
| `bootstrap-batch-2.csv` | 165 | À coller après succès du batch 1 |

**Total : 365 entrées** (149 outils du `TOOL_REGISTRY` × 1-3 mots-clés).
Limite UI 200 lignes/batch, d'où le découpage en deux passes.

## Mode d'emploi

1. Ouvrir https://staging.legalcase.fr/super-admin/jurisprudence-watch
2. Onglet **« Bootstrap »** (1er onglet)
3. Ouvrir `bootstrap-batch-1.csv` dans un éditeur, **tout sélectionner**, copier
4. Coller dans le textarea — le compteur doit afficher **200 / 200 entrées détectées**
5. Cliquer **« Lancer le bootstrap »** — attendre la snackbar succès
   `Bootstrap terminé : X processed, Y created, Z skipped (Wms)`
6. Vérifier l'onglet **« Audit log »** (rechargé automatiquement)
7. Répéter avec `bootstrap-batch-2.csv` (165 lignes)

## Format CSV

```
toolId,brancheCalculId,motCleRecherche,juridictionFiltre,dateMin
```

- `toolId` — clé du `TOOL_REGISTRY` (frontend `decisional-tools-panel.component.ts`)
- `brancheCalculId` — `default` partout en V1 (un seul `ToolBranchRegistry`
  par outil retourne `Set.of("toolId:default")`)
- `motCleRecherche` — expression libre passée à JUDILIBRE
- `juridictionFiltre` — substring matché en lowercase contre
  `arret.juridiction()` (cf. `JurisprudenceBootstrapService.filterByJuridiction`).
  Valeurs utilisées :
  - Outils Travail FR → `chambre sociale`
  - Outils Famille FR → `chambre civile`
  - Outils Immigration FR → vide (mixte Conseil d'État / TA)
  - Outils BE → vide (JUDILIBRE est FR-only ; fallback retourne candidates)
- `dateMin` — vide partout (laisser JUDILIBRE filtrer par défaut)

## Note sur les outils belges

Le backend bootstrap actuel (`JudilibreApiClient`) interroge uniquement
JUDILIBRE (Cour de cassation FR + Conseil d'État via PISTE). Pas de
client Juridat/Juportal côté F-JU-01 V1.

Les 33 outils BE présents dans le CSV sont quand même seedés (1 ligne
chacun) pour deux raisons :
1. Ne pas oublier ces outils quand l'API BE sera branchée plus tard
2. Avoir une entrée audit log par outil traçant l'intention

En pratique, Claude évaluera la pertinence comme faible (arrêts FR
retournés en fallback) et le mapping restera vide (`confidence_score < 60 %`
= silence > erreur, cf. mini-spec).

## Regénérer le CSV

Si `TOOL_REGISTRY` évolue (nouveaux outils livrés), relancer :

```bash
python3 /tmp/gen_bootstrap_csv.py
```

Le script lit `frontend/src/app/case-files/decisional-tools-panel/decisional-tools-panel.component.ts`,
extrait les `displayLabel` et dérive 3 mots-clés par outil FR + 1 par outil BE.

Heuristique mots-clés (par outil FR) :
1. Label nettoyé (sans `(FR)`, `(BE)`, etc.)
2. Termes extraits de l'ID (suffixe après préfixe `F-XX-NN-`)
3. Label + juridiction (« Cour de cassation chambre sociale » / etc.)

Pour des mots-clés sur mesure (articles précis, jurisprudence emblématique),
éditer le CSV à la main avant collage — le format est lisible et trivial.

## Suivi

Après chaque batch, l'onglet **« Audit log »** liste :
- `processed` — nombre d'entrées CSV traitées
- `created` — mappings effectivement INSERT (Claude confident ≥ 60 %)
- `skipped` — entrées rejetées (confiance < 60 %, JUDILIBRE vide, etc.)

Les entrées `skipped` sont normales et attendues sur les outils BE
(JUDILIBRE FR-only) et sur certains mots-clés trop génériques. À ré-itérer
au besoin avec un CSV affiné.
