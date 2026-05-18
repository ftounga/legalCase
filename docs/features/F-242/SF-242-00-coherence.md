# F-242 — Cadrage cohérence fonctionnelle (étape 0)

> Produit via la skill `ai-skills/feature-coherence-challenger.md`.
> **Nature** : F-242 est le « retour » de la chaîne jurisprudence — capter dans LegalCase les références d'arrêts que l'avocat a trouvées chez son éditeur (via F-241) et les faire remonter dans les conclusions générées (F-98).

## Verdict : GO

> ⚠️ **Réserve de gouvernance — déclencheur non formellement atteint.** `PRODUCT_SPEC.md` conditionne F-242 à « ≥ 5 signaux utilisateurs concrets demandant explicitement l'import » après la livraison de F-241 (2026-05-13). Ce seuil n'est **pas formellement mesuré**. Le développement est lancé sur **override explicite du product owner** (décision 2026-05-18, assumée comme risque de scope creep prématuré). Cette réserve ne change pas le verdict de cohérence fonctionnelle (GO), mais doit rester visible jusqu'à confirmation du signal terrain.

## Intention métier (1 phrase)

Permettre à l'avocat de rapatrier dans LegalCase les références de jurisprudence retenues chez son éditeur (Doctrine / Lexis Plus / Lextenso), rattachées aux points juridiques du dossier, pour qu'elles soient reprises automatiquement dans les conclusions générées.

## Workflow métier réel de l'avocat (8-15 étapes + source)

Source : chaîne de production juridique reconstruite dans `docs/features/F-241` (entrée PRODUCT_SPEC) + `docs/features/F-98/SF-98-00-coherence.md` (workflow conclusions) + `docs/features/F-244/SF-244-00-coherence.md` (parcours détail dossier). ⚠ Parcours partiellement reconstruit — **hypothèse à valider auprès d'un avocat** sur l'étape 7.

1. Reçoit le dossier client + premières pièces.
2. Analyse le dossier — synthèse des faits, chronologie, risques, points juridiques (pipeline IA).
3. Le pipeline identifie les **questions juridiques** du dossier (déjà extraites par `EnrichedAnalysisService`).
4. Pour chaque question juridique, l'avocat cherche la jurisprudence d'appui.
5. LegalCase (F-241) génère, pour chaque question, une requête pré-formulée + un deeplink vers l'éditeur de l'avocat → il ouvre Doctrine / Lexis Plus / Lextenso.
6. L'avocat lit les arrêts chez l'éditeur et **sélectionne** ceux qui appuient son argumentaire.
7. **Il note les références retenues** (« Cass. soc. 12 oct. 2022, n° 21-12345 » + portée) — aujourd'hui sur un brouillon, hors LegalCase.
8. Il rédige les conclusions en citant ces arrêts comme appui de chaque moyen.
9. Il relit, exporte et dépose les conclusions.
10. Il suit le dossier jusqu'à clôture.

## Cartographie features actuelles ↔ workflow

| Étape métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-2 import + analyse / synthèse | F-04/05, pipeline IA F-08/09/10, synthèse | ✅ Livrée |
| 3 questions juridiques extraites | `EnrichedAnalysisService` (amont de F-241) | ✅ Livrée |
| 4-5 recherche jurispru via l'éditeur | F-241 connecteur deeplink (PR #974) | ✅ Livrée |
| 6 lecture / sélection des arrêts | — (chez l'éditeur tiers, hors LegalCase) | — |
| 7 **noter les références retenues** | **F-242 (la feature challengée)** | 🟡 Backlog |
| 8 rédaction des conclusions citant les arrêts | F-98 génération de conclusions (53/53 SF, Terminée 2026-05-18) | ✅ Livrée |
| 9 relecture / export / dépôt | F-98 éditeur (SF-98-49), versions (52), export Word/PDF (50/51) | ✅ Livrée |
| 10 clôture | F-04 statut OPEN/CLOSED | ✅ Livrée |

## Position de la nouvelle feature

F-242 s'insère à l'**étape 7** — le « retour » de la chaîne jurisprudence : capter les références sélectionnées par l'avocat et les rendre exploitables par l'étape 8 (conclusions). F-241 traite l'**aller** (LegalCase → éditeur, deeplink) ; F-242 traite le **retour** (éditeur → LegalCase, citation structurée).

## Challenge amont

*Chaque étape avant F-242 dans le workflow est-elle couverte par une feature du produit ?*

→ Oui, intégralement.
- Étapes 1-3 (analyse, synthèse, questions juridiques) — pipeline IA ✅ livré.
- Étapes 4-5 (recherche jurispru) — **F-241 livrée** (PR #974, 2026-05-13). La brique « aller » sur laquelle F-242 s'appuie existe fonctionnellement.
- Étape 6 (lecture / sélection des arrêts) — se passe chez l'éditeur tiers, **hors périmètre LegalCase** : c'est l'acte de l'avocat, F-242 n'a pas à le couvrir.

**Aucun trou amont.** L'avocat arrive à l'étape 7 avec des références en main.

## Challenge aval

*La sortie de F-242 (références de jurispru structurées, rattachées aux points juridiques) est-elle exploitable par l'aval ?*

→ L'aval direct est l'**étape 8 — génération des conclusions (F-98)**, livrée ce jour (matrice 53/53 SF). Pour que F-242 ne soit pas un champ de notes mort, le pipeline F-98 doit **intégrer les citations dans le prompt de conclusions** (`CaseConclusionPromptBuilder`). C'est exactement le « enrichissement conclusions » du titre de F-242 : **la feature porte les deux bouts** — capter la citation *et* l'injecter dans la génération. Le trou aval est donc comblé par F-242 elle-même, pas par une feature tierce manquante.

→ Aval secondaire évoqué par le titre (« … et les outils décisionnels ») : exposer les citations à côté des verdicts d'outils. Cet aval est **différable** — il n'est pas nécessaire pour que F-242 serve (les conclusions sont le débouché principal). À traiter en SF distincte si besoin, hors périmètre du noyau F-242.

**Aucun trou aval bloquant.**

## STOPs / pré-requis à ajouter au backlog

Aucun. F-241 (amont) et F-98 (aval) sont livrées. Aucune feature pré-requise à inscrire au backlog.

## Invariants anti-gadget pour la mini-spec

Le risque « gadget » de F-242 = un champ de saisie de jurispru qui ne sert à rien. La mini-spec devra respecter :

1. **Boucle fermée obligatoire** — une citation saisie DOIT effectivement remonter dans le prompt de génération des conclusions F-98 (`CaseConclusionPromptBuilder`). Sans cette injection, F-242 est un bloc-notes mort : non négociable.
2. **Rattachement à un point juridique précis** — une citation est attachée à un point juridique / moyen identifié de la synthèse, pas à un champ libre global du dossier : l'avocat doit savoir à quel moyen l'arrêt s'applique.
3. **Saisie légère** — référence (`Cass. soc. 12 oct. 2022, n° 21-12345`) + une ligne de portée, **pas le texte intégral** de l'arrêt. Friction faible = adoption (cf. arbitrage des 4 options techniques dans PRODUCT_SPEC).
4. **Pas de scraping d'éditeur** — l'option β (drag-drop URL + scraping Doctrine) est exclue : non viable techniquement (Cloudflare) et juridiquement (zone grise CGU). La mini-spec ne la retient pas.
5. **Péremption des conclusions** — si une version de conclusions a déjà été générée puis qu'une citation est ajoutée/modifiée, la version doit être marquée « à régénérer » (réutiliser le mécanisme `stale` de SF-98-53). Sinon les citations n'apparaissent jamais dans les conclusions déjà produites.
6. **Isolation workspace** — les citations sont rattachées au dossier et isolées par workspace, comme toute donnée de dossier.

## Décision finale

**GO.** F-242 referme la chaîne jurisprudence ouverte par F-241 : toutes les briques amont (F-241, pipeline IA) et le débouché aval (F-98 conclusions) sont **livrés**. Aucun trou fonctionnel, aucun pré-requis backlog. La feature passe `Backlog` → `À faire`.

**Réserve** : le déclencheur « ≥ 5 signaux terrain » de PRODUCT_SPEC n'est pas formellement atteint — développement sur override product owner assumé (2026-05-18).

Étape suivante : **étape 0 bis** — `SF-242-00b-ux-coherence.md` (F-242 ajoute un élément visible sur l'écran de synthèse → cadrage écran requis), puis mini-spec.
