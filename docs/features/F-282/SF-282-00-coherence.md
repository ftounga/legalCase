# F-282 — Cycle contradictoire (rounds d'échange) — Cadrage cohérence (étape 0)

> Skill `feature-coherence-challenger`. Date : 2026-06-12. Issue de l'audit `docs/business/audit-workflow-decisionnel.md` (P1, levier rétention #1).

## Verdict : **GO**
Toutes les briques amont existent et sont livrées ; la feature les **orchestre** en cycle d'échanges. Zéro pré-requis manquant. Distincte des features conclusions existantes (vérifié dans le code, cf. ci-dessous).

## Intention métier (1 phrase)
Modéliser et accompagner le **cycle contradictoire** d'un dossier (échanges successifs de conclusions entre les parties), pour que l'avocat **revienne à chaque tour** générer sa réplique au dernier jeu adverse — transformant l'usage one-shot en usage récurrent.

## Workflow métier réel (source : pratique contentieuse + dossier STANOJEVIC ; ⚠ à valider avocat)
1. Saisine : l'avocat dépose son 1er jeu de conclusions (round 1).
2. L'adversaire dépose ses conclusions en réponse (round 2).
3. L'avocat **réplique** : nouveau jeu qui **répond point par point au jeu adverse du round 2**, en reprenant ses propres prétentions (récapitulatives).
4. L'adversaire re-conclut (round 4) ; l'avocat re-réplique (round 5)… 2 à 4 échanges typiques.
5. Clôture de la mise en état → audience. À chaque round : un **délai pour répondre**.

## Cartographie features actuelles ↔ workflow
| Étape | Feature LegalCase | Statut |
|---|---|---|
| Upload du jeu adverse | tag « Écritures adverses » (F-261, booléen `adverse_pleadings`) | ✅ Livrée |
| Extraction + réfutation des moyens adverses | F-261 (SF-261-02/03/04, 3 domaines FR) | ✅ Livrée |
| Conclusions récapitulatives (reprise de TES versions) | F-271 (Conclusions V4 ①) | 🟡 Backlog |
| Génération / versions / lifecycle / diff conclusions | F-98 / SF-98-52 / F-280 | ✅ Livrées |
| Stade procédural | F-243 | ✅ Livrée |
| Délais procéduraux | F-69 (Suivi) | ✅ Livrée (passive) |
| **Modèle de ROUND / séquence d'échanges (cette feature)** | **F-282** | ❌ **Inexistant** |

## Position de la nouvelle feature
Couche d'**orchestration temporelle** au-dessus des briques conclusions : représente le dossier comme une **suite de rounds** (qui a déposé quoi, à qui le tour, délai), et au round « à toi » génère une **réplique** = conclusions récapitulatives (F-271) **réfutant** (F-261) le **jeu adverse de ce round précis**.

## Challenge amont
*Tout est en place :* upload + tag adverse (F-261), extraction/réfutation (F-261), récap (F-271 backlog), génération/versions (F-98), stade (F-243), délais (F-69). **Aucun trou amont.** F-282 ne réinvente rien — il séquence.

## Challenge aval
La sortie d'un round = un jeu de conclusions (rendu F-259, export Word/PDF, versions). Pipeline aval **déjà livré**. ✅

## Distinction vérifiée (anti-doublon, code lu le 2026-06-12)
- `adverse_pleadings` est un **booléen** sur `documents` (migration 599) → F-261 ne modélise **pas** de séquence. Aucun concept de round/échange/réplique dans le code.
- F-271 = conclusions **récapitulatives de TON côté** (« chaque génération repart de zéro » → reprendre tes versions), **pas** le cycle adverse.
- F-274 = traitement (communication/rejet) des **pièces** adverses, pas le cycle.
→ F-282 est **distinct et complémentaire** : il apporte le **modèle de round** que ni F-261, ni F-271, ni F-274 ne portent.

## STOPs / pré-requis à ajouter
**Aucun.** Orchestration de briques livrées.

## Invariants anti-gadget (pour la mini-spec)
1. **Vrai modèle de round** : chaque round identifie l'auteur (nous/adverse), son jeu de conclusions (document ou version), sa date, le délai de réponse. Pas un simple compteur cosmétique.
2. **La réplique cible le jeu adverse DE CE round** (pas « les pièces adverses » en vrac) — réutilise l'extraction F-261 sur le document du round courant.
3. **Récapitulatif** : la réplique reprend les prétentions du round précédent (art. 768 CPC) via F-271 — un moyen non repris ≠ abandonné par accident.
4. **Délai non orphelin** : le délai de réponse d'un round crée une échéance dans l'onglet Suivi (invariant F-206).
5. **Pas de doublon** : F-282 n'extrait ni ne réfute lui-même (délègue à F-261), ne récapitule pas lui-même (délègue à F-271) — il **orchestre**.
6. **Exigence design impérative** : visualisation du cycle (timeline des échanges) de **qualité premier ordre** (charte DESIGN_SYSTEM.md ; production-grade ; pas d'« AI-generic »). **Étape 0 bis (cohérence écran) obligatoire** — placement de la timeline des rounds sans surcharger l'écran dossier (4 onglets).

## Décision finale
**GO** — enchaîner étape 0 bis (cohérence écran : où vit la « timeline des échanges » + le bouton « générer ma réplique au round N ») puis mini-spec. Statut PRODUCT_SPEC : `Backlog` → `À faire`.
