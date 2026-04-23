# SF-IM-07-05 — Pré-remplissage F-IM-07 depuis la détection IA

## Objectif
Faire en sorte que F-IM-07 droit au travail ouvre **pré-rempli** avec le
titre détecté par l'IA dans les pièces du dossier, au lieu de forcer
l'avocat à sélectionner manuellement. Préserve la liberté de comparaison
(simulation d'alternatives) via le bouton "Modifier".

## Comportement nominal
- **Au 1er affichage** (mount + ngOnInit) : si `aiData.typeTitreSejourCode`
  existe et matche le pays du workspace, le sélecteur s'initialise sur ce
  code. Badge `Pré-rempli depuis l'analyse` (icône `auto_awesome`) affiché.
- **À chaque nouvelle analyse** (ngOnChanges, `aiData` mis à jour) : idem,
  tant que le résultat F-IM-07 n'a pas déjà été sauvegardé pour ce dossier.
- **Modification manuelle** : `onTitreTypeChange()` efface le badge IA →
  l'avocat simule librement une alternative, le coherence alert signale
  la divergence.
- **Dossier avec résultat existant** (F-IM-07 déjà exécuté) : pas de
  prefill, on affiche la sélection précédente.

## Pourquoi
Sur Chen Wei (dossier paradigmatique), l'IA détecte déjà la carte
pluriannuelle Étudiant-Recherche — inutile de forcer l'avocat à la
sélectionner à la main. Mais le prompt IA retournait le code générique
`CARTE_PLURIANNUELLE` alors que depuis SF-IM-07-04 on a les sous-types
précis.

## Changements

### Backend — `LegalDomainPromptBuilder`
Extension de l'enum `type_titre_sejour_code` de 16 → **21 codes** avec
règles de précision ajoutées au prompt :

> *"Carte pluriannuelle + mention 'Étudiant' ou 'Étudiant-Recherche' →
> CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE (régime 964 h/an)"*
> *"Carte VPF 1 an délivrée au conjoint d'un ressortissant FRANÇAIS
> (art. L.423-1, indices : acte de mariage avec conjoint français
> présent au dossier) → CST_VPF_CONJOINT_FR"*

Les codes génériques `CARTE_PLURIANNUELLE` et `CST_VPF` ne sont utilisés
que si le motif n'est pas déterminable depuis les pièces.

### Frontend — `ImmigrationWorkRightSectionComponent`
- `ngOnInit` appelle désormais `prefillFromAi()` (garde-fou). Avant,
  seul `ngOnChanges` le faisait — cas marginal mais potentiel trou.
- `prefillFromAi()` accepte sans modification tous les nouveaux codes
  FR (whitelist `FR_TITRE_CODES` déjà étendue en SF-IM-07-04).
- Le badge "Pré-rempli depuis l'analyse" (template HTML) existait déjà.

## Cas d'erreur
- **IA retourne un code inconnu de la whitelist** : prefill silencieusement
  ignoré, sélecteur reste sur la valeur par défaut (test U-3 existant).
- **IA retourne un code du mauvais pays** (ex. CARTE_B en FR) : ignoré.
- **Résultat F-IM-07 déjà sauvegardé** : `loadExisting()` pose le résultat,
  `prefillFromAi()` ne fait rien (condition `!this.result()`).
- **Erreur de détection IA** : l'avocat corrige via le sélecteur →
  `onTitreTypeChange()` efface le badge.

## Critères d'acceptation
- [x] Prompt IA : enum étendu à 21 codes + règles de précision
- [x] Frontend : `prefillFromAi()` appelé aussi dans `ngOnInit` (garde-fou)
- [x] Tests unitaires : 3 nouveaux (ETUDIANT_RECHERCHE, CST_VPF_CONJOINT_FR,
  mount garde-fou) ; 21/21 verts
- [x] Tests backend prompt non régressés (17 tests verts)

## Plan de test
- **Unit frontend** : 21 tests verts (SF-IA-03-*, SF-IM-07-05)
- **Unit backend** : prompt builder + CaseAnalysisService verts
- **Intégration (staging — Chen Wei)** :
  1. Dossier Chen → relancer une analyse IA (pour consommer le nouveau prompt)
  2. Ouvrir F-IM-07 → le sélecteur doit être sur
     `CARTE_PLURIANNUELLE_ETUDIANT_RECHERCHE` d'entrée, avec badge
     "Pré-rempli depuis l'analyse"
  3. Cliquer "Analyser" → réponse "964 h/an"
  4. Changer le sélecteur vers `CST_VPF_CONJOINT_FR` → badge disparaît,
     simulation "plein droit" → comparaison directe possible

## Hors périmètre
- **Auto-déclenchement de l'analyse F-IM-07 au mount** : pas fait — on
  pré-remplit seulement. L'avocat reste maître du moment où il lance
  l'appel backend (qui crée une consommation d'API).
- **Pré-sélection parmi plusieurs scénarios F-151** : à traiter séparément
  si besoin (on pourrait proposer 2 titres dans un tab-view).

## Tables / endpoints / composants impactés
**Backend** :
- `LegalDomainPromptBuilder.java` : prompt F-150/F-151 builder (section
  `type_titre_sejour_code`)

**Frontend** :
- `immigration-work-right-section.component.ts` : `ngOnInit` + tests

## Impact par domaine métier
Spécifique **DROIT_IMMIGRATION**. Le pré-remplissage s'applique aux
2 pays (FR + BE), mais les nouveaux codes FR (SF-IM-07-04) ne concernent
que la France. La Belgique reste sur ses 8 codes existants.

## Analyse de cohérence transversale
- **F-IM-07 ↔ F-IM-01** : les 2 outils consomment maintenant les mêmes
  codes (incl. `CST_VPF_CONJOINT_FR`). Plus de divergence au sens
  `feedback_decision_tools_one_per_situation`.
- **Coherence alert existant** : continue de fonctionner — si l'avocat
  change le titre, le badge warning F-96/IA s'affiche via
  `coherenceAlert()`.
- **Aucun impact sur F-IM-05 arbre décisionnel** (titre cible, pas actuel).
- **Prompt caching SF-142-04** : inchangé — le system prompt est juste
  plus long de ~10 lignes, reste éligible au cache.

## Nouveau pattern UI ou service partagé
Aucun. Pattern de pré-remplissage déjà présent, simplement sécurisé
contre un cas marginal de lifecycle Angular.
