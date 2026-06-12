# SF-280-00 — Cadrage cohérence fonctionnelle (F-280 : comparaison de versions / diff lisible)

> Skill `ai-skills/feature-coherence-challenger.md` — étape 0. Verdict pilote le statut PRODUCT_SPEC.

## 1. Workflow métier réel de l'avocat

En procédure écrite, l'avocat itère sur ses conclusions : il génère une version, l'édite, la régénère (nouvelle version), ré-édite, etc. Le programme Conclusions V4 (F-271 récapitulatives, F-278 garde anti-écrasement, F-279 autosave) a renforcé la *sécurité du travail* à travers les versions. Mais l'avocat, devant 3-4 versions, ne peut pas savoir **ce qui a concrètement changé** entre deux versions — surtout après une régénération qui repart de la version précédente (F-271) : a-t-elle conservé ses ajouts ? Qu'a-t-elle reformulé ? Quelle demande a disparu (risque art. 768 CPC d'abandon implicite) ?

Étapes :
1. Ouvrir le dossier → onglet conclusions.
2. Constater l'historique : sélecteur de version (V1, V2, V3…) déjà présent (SF-98-52).
3. **(trou)** Comparer V(n-1) et V(n) pour valider/auditer l'évolution.
4. Éditer / valider / déposer.

## 2. Cartographie des features existantes sur ce workflow

| Étape | Feature existante | État |
|-------|-------------------|------|
| Génération versionnée | SF-98-52 (versions + cycle de vie) | livré |
| Sélecteur de version | SF-98-52 (mat-select header) | livré |
| Chargement du content d'une version | `ConclusionsService.getVersion` | livré |
| Rendu « acte » du content | `ConclusionDocumentComponent` (marked) | livré |
| Édition / autosave | F-264 / F-279 | livré |
| **Diff lisible entre 2 versions** | — | **TROU = F-280** |

Le contenu de chaque version est **déjà disponible** côté API (`GET …/versions/{versionId}` → `content`). F-280 est donc **100 % frontend** : aucune table, aucun endpoint, aucun backend.

## 3. Challenge cohérence amont / aval

**Amont** — Les pré-requis existent-ils ?
- Historique multi-versions : OUI (SF-98-52).
- Endpoint pour charger le content de **n'importe quelle** version : OUI (`getVersion`).
- Au moins 2 versions pour qu'un diff ait un sens : géré (le diff n'est proposé que si `versions().length >= 2`).
- ✅ Amont sain.

**Aval** — La sortie est-elle exploitable ?
- Le diff est un **outil de lecture/audit**, terminal : il n'alimente aucune étape downstream, il informe la décision (éditer / valider). Pas de dépendance aval. ✅

## 4. Anti-gadget / anti-doublon (vigilance précédents F-262/F-263)

- **Doublon ?** Aucun écran ne montre aujourd'hui un diff. Le sélecteur de version permet de *basculer* mais pas de *comparer*. Pas de chevauchement.
- **Gadget ?** Non : le diff répond à un besoin métier précis et grave (art. 768 — vérifier qu'aucune prétention/moyen n'a disparu d'une version à l'autre). C'est exactement le complément d'audit de F-271 (régénération récapitulative).
- **Périmètre serré** : diff **texte/ligne** lisible (ajouts / suppressions surlignés), pas de moteur de merge, pas d'édition dans le diff, pas de diff mot-à-mot intra-ligne en V1 (line-level suffit pour des conclusions structurées en paragraphes). Lib externe **écartée** : algorithme LCS line-based pur-TS (zéro nouvelle dépendance npm, zéro risque build/CI).

## 5. Invariants anti-gadget pour la mini-spec

1. Le diff est **lecture seule** : il ne modifie ni le content, ni aucune version, ni le serveur (aucun appel d'écriture).
2. Proposé **uniquement** s'il existe ≥ 2 versions.
3. **Aucune nouvelle dépendance** : diff implémenté en TS pur (LCS line-based), pur et testé unitairement.
4. Comparaison par défaut **version sélectionnée vs version précédente** (le cas d'usage dominant : « qu'a fait ma dernière régénération ? »), avec possibilité de choisir l'autre borne.
5. Réutilise les **tokens de couleur du design system** (success `#27AE60` pour ajouts, error `#C0392B` pour suppressions) — jamais de vert/rouge inventés.
6. Pas de régression : le diff est un mode **additif** dans `conclusions-section`, masqué par défaut.

## 6. Verdict

**GO.** Pré-requis amont tous présents (versions + API getVersion + rendu). Sortie terminale exploitable (audit). Aucun risque de doublon/gadget. Feature frontend pure, réversible. Statut PRODUCT_SPEC : `À faire` → dev.
