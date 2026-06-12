# F-283 — Cadrage cohérence fonctionnelle (étape 0)

> Skill `feature-coherence-challenger`. Date : 2026-06-12. Feature : **F-283 — Dossier vivant : phases procédurales & vagues de pièces**. Levier rétention #2 (suite F-282). Exigence design impérative.

## Verdict global : **GO avec ajustements**
F-283 se découpe en **deux subfeatures cohérentes** d'un même thème (« le dossier vivant ») :
- **SF-283-01 — Phases procédurales** : la progression *datée* du dossier (référé → bureau de conciliation → fond → appel → cassation). **GO.**
- **SF-283-02 — Vagues de pièces** : rendre l'ajout de pièces *incrémental et lisible* (« N nouvelles pièces depuis la dernière analyse → voici l'impact ») au lieu d'une ré-analyse opaque. **GO avec ajustements** (élève l'avertissement plat existant, ne le duplique pas).

## Workflow métier réel de l'avocat cible
Un dossier prud'homal (ou autre) **n'est pas un instantané**. Dans la vraie vie :
1. L'avocat ouvre le dossier à la **saisine** (souvent un référé ou une requête au fond).
2. Les **pièces arrivent par vagues** sur des semaines : pièces du client, pièces adverses, attestations tardives, pièces complémentaires demandées par le conseil.
3. Le dossier **avance par phases** : audience de conciliation (BCO), renvoi au fond, jugement, **appel**, le cas échéant **cassation**. Chaque phase a sa logique, ses échéances, son jeu de pièces.
4. À chaque vague de pièces, l'avocat se demande : « qu'est-ce que ça change à mon analyse / mes conclusions ? ».

Aujourd'hui le produit modélise le dossier comme un **snapshot** (upload → analyse instantanée). F-243 a posé un **stade procédural statique** (un libellé : juridiction/stade/position) — utile mais figé. F-282 a posé le **cycle contradictoire** (rounds d'échange). **Il manque deux choses** : (a) la **progression de phases dans le temps** (pas un libellé, une frise datée) ; (b) la **lisibilité de l'incrément de pièces** entre deux analyses.

## Cartographie des features existantes sur ce workflow
| Étape métier | Brique LegalCase existante | Statut |
|---|---|---|
| Saisine / stade courant (libellé) | **F-243** `procedure_stage` (juridiction/stade/position) | ✅ existant — *statique* |
| Échanges contradictoires (rounds) | **F-282** `contradictoire_rounds` | ✅ existant |
| Échéances | **F-69** `app-case-deadlines-section` | ✅ existant |
| Pièces (upload, n° de pièce, bordereau) | F-260 / SF-98-57 | ✅ existant |
| « Documents ajoutés depuis la dernière synthèse » | `outdatedDocuments` (detail) → **avertissement plat** | ✅ existant — *non élevé* |
| **Progression datée des phases** | — | 🆕 SF-283-01 |
| **Vague de pièces lisible + impact** | — | 🆕 SF-283-02 |

## Distinction explicite (anti-doublon — invariant central)
- **vs F-243 (stade statique)** : F-243 = **un seul libellé courant** édité par sélecteurs (métadonnée). SF-283-01 = **suite ordonnée et datée de transitions** (le dossier *traverse* des phases ; on garde la trace de « entré en phase X le JJ/MM »). F-283 **consomme** le libellé F-243 comme valeur possible d'une phase mais ne le remplace pas. Décision : SF-283-01 introduit une **nouvelle table `case_phases`** (transitions datées), F-243 reste la métadonnée « stade courant ». Pas de duplication : la frise de phases *lit* mais ne *réécrit* pas `procedure_stage`.
- **vs F-282 (rounds)** : un round = un **échange de conclusions** (qui répond à qui). Une phase = un **stade de la procédure** (où en est le dossier dans son cycle de vie juridictionnel). Orthogonaux : un même round vit dans une phase ; plusieurs rounds peuvent vivre dans la phase « fond ». Tables distinctes.
- **vs programme Conclusions (F-261→281)** : F-283 ne **rédige rien**. Couche cycle-de-vie uniquement.
- **vs SF-283-02** : SF-283-01 = axe **phases** ; SF-283-02 = axe **pièces**. Indépendants fonctionnellement (deux tables/zones distinctes), réunis par le thème.

## Cohérence amont (les pré-requis existent-ils ?)
- SF-283-01 : a besoin d'un dossier (`case_files`) et d'un référentiel de phases. ✅ Le dossier existe ; le référentiel de phases peut réutiliser les valeurs `procedure_stage` de F-243 (pas de nouveau référentiel métier risqué). Isolation workspace via `case_file_id` (pattern F-282). ✅
- SF-283-02 : a besoin (a) de la date de dernière analyse dossier et (b) de la date de création des pièces. ✅ `AnalysisJob(CASE_ANALYSIS).updatedAt` (quand `status=DONE`) + `documents.created_at`. La logique `outdatedDocuments` existe **déjà** côté front — SF-283-02 la **fiabilise/élève**, ne la réinvente pas.

## Cohérence aval (la sortie est-elle exploitable ?)
- SF-283-01 : la frise de phases est consommée **visuellement** (onglet Suivi) et alimente le fil rouge. Pas d'orphelin : chaque phase porte sa date ; la phase courante est mise en exergue. Aval V1.1 possible : déclencher des échéances-type par phase (différé, non bloquant).
- SF-283-02 : la vague de pièces porte **son action** (« Relancer / Enrichir l'analyse ») — invariant anti-orphelin F-206. Le CTA route vers l'action d'analyse existante (pas de nouvelle pipeline). ✅

## Invariants anti-gadget (la mini-spec DOIT les respecter)
1. **SF-283-01 ne remplace pas F-243** : `procedure_stage` reste la métadonnée « stade courant » ; la table `case_phases` est la *trace datée*. La phase courante = la dernière transition (et peut être synchronisée avec `procedure_stage` — décision mini-spec).
2. **SF-283-02 n'ajoute aucune pipeline d'analyse** : elle rend lisible un état déjà calculable (delta de pièces) et **route** vers l'action existante. Zéro nouveau job, zéro nouvel appel IA.
3. **Aucun outil décisionnel touché** (pas de `TOOL_REGISTRY`, pas de gate F-IA-03, pas de pré-fill décisionnel). Vues de suivi/lisibilité.
4. **Pas d'écran vide** : dossier neuf sans phase → SF-283-01 montre « Phase 1 — Saisine » par défaut ; 0 pièce en attente → SF-283-02 affiche un état « à jour » discret (pas de bloc vide criard).
5. **Beauté = critère d'acceptation** (revue visuelle PO), gabarit = frise F-282.

## Décision finale
**GO avec ajustements.** Les deux subfeatures sont distinctes des features existantes (F-243 statique, F-282 rounds, Conclusions rédaction) et comblent un trou réel du workflow (progression datée + lisibilité d'incrément). Statut PRODUCT_SPEC → **À faire**. Étape 0 bis obligatoire (les deux à impact écran).
