# Plan de test — Module Conclusions (validation des ajouts session 2026-06-09/11)

> Environnement : **https://staging.legalcase.fr** · Workspace **`DROIT_DU_TRAVAIL / FRANCE`** · onglet **« Décision »** → section **« Projet de conclusions »**.
> Libellés entre « » = textes UI exacts attendus.

## ⚠️ Pré-requis du test
1. **Déploiement à jour** : l'image backend doit inclure SF-265-01 (régénération de section). Vérifier avant : la régénération de section (TEST 5) doit répondre, sinon attendre le déploiement.
2. **Dossier de test** : créer un dossier licenciement FR avec :
   - les pièces **client** (contrat, lettre de licenciement, bulletins) ;
   - **une pièce « écritures adverses »** = les conclusions de la partie adverse (pour TEST 1). ⚠️ Elle doit porter sur **le même dossier** (mêmes parties/faits) — sinon contamination (cf. incident validation 10/06).
3. Lancer l'analyse, renseigner le **stade procédural** (onglet Dossier : CPH / Bureau de jugement / Demandeur), générer les conclusions.

---

## TEST 1 — F-261 · Conclusions EN RÉPONSE (réfutation des moyens adverses)
**But** : marquer le document adverse → ses moyens sont réfutés dans l'acte.

| # | Action | Résultat attendu |
|---|--------|------------------|
| 1.1 | Onglet **Dossier**, table des documents → sur la pièce des **conclusions adverses**, activer le bouton **« Écritures adverses »** (icône maillet `gavel`) | Le document est marqué (tooltip « Marquer comme écritures de la partie adverse — ses moyens seront réfutés dans les conclusions ») |
| 1.2 | Onglet **Décision** → **régénérer** les conclusions | Document produit |
| 1.3 | Lire l'acte | Une argumentation **réfute les moyens réels de l'adversaire** (« la partie adverse soutient que… ; ce moyen est mal fondé car… »), distincte de la réfutation des **citations** adverses (TEST 2) |
| 1.4 | Vérifier la fidélité | Les moyens réfutés correspondent au **contenu réel** de la pièce adverse ; aucun moyen inventé ; pas de confusion de parties/faits |

> ✅ Invariant : seuls les documents **marqués** « écritures adverses » alimentent la réfutation des moyens (jamais déduit à l'aveugle).

---

## TEST 2 — SF-98-56 · Réfutation des CITATIONS adverses (rappel, déjà validé)
| # | Action | Résultat attendu |
|---|--------|------------------|
| 2.1 | Sur la **Synthèse**, section « Jurisprudences citées » → marquer une citation **Suspecte/Non trouvée** « Marquer comme adverse à réfuter » | Marquage persistant |
| 2.2 | Régénérer | Section qui **réfute** l'arrêt adverse (inexistant / portée dénaturée), **sans le citer « avec autorité »** |

---

## TEST 3 — F-264 · Éditeur document natif (markdown enrichi + aperçu live)
**But** : éditer dans un rendu « acte », plus le textarea brut.

| # | Action | Résultat attendu |
|---|--------|------------------|
| 3.1 | Section conclusions (état `DONE`, brouillon) → **« Modifier »** | Mode édition : zone d'édition **+ barre d'outils** (Titre, Sous-titre, **Gras**, *Italique*, Liste, Citation) **+ aperçu formaté « acte » à côté** |
| 3.2 | Sélectionner du texte → cliquer **Gras** | La sélection est entourée de `**…**` ; l'aperçu affiche le texte **en gras** en live |
| 3.3 | Cliquer **Titre** sur une ligne | La ligne est préfixée `## ` ; l'aperçu affiche un titre formaté |
| 3.4 | Réduire la fenêtre (mobile) | Bascule **« Édition » / « Aperçu »** (pas de côte-à-côte illisible) |
| 3.5 | **Enregistrer** | Retour en lecture (rendu acte) ; le contenu est conservé |
| 3.6 | **Exporter Word puis PDF** | L'export reflète les modifications **sans perte** (le contenu reste markdown — non-régression) |

---

## TEST 4 — Récap des durcissements F-98 (rappel, déjà validés — re-vérifier sur l'acte)
| Élément | Attendu dans l'acte |
|---|---|
| **SF-98-55** anti-jargon | Aucun code d'outil (« F-DT-XX »), aucun score brut (« 2 critères sur 7 ») |
| **F-260** numérotation | Renvois « Pièce n° X » stables ; réordonnancement (flèches) dans la table documents |
| **SF-98-57** bordereau | Section finale « BORDEREAU DE PIÈCES COMMUNIQUÉES » cohérente avec les renvois |
| **SF-98-60** subsidiaires | Dispositif « À titre principal » / « À titre subsidiaire » |
| **SF-98-61** finitions | Vraies adresses des parties (pas « [adresse] ») ; signature « [Nom et qualité de l'avocat] » (pas de nom inventé) |

---

## TEST 5 — F-265 · Co-rédaction au paragraphe (régénération de section sur instruction IA)
**But** : régénérer/renforcer **une section** de l'acte sur instruction libre.

| # | Action | Résultat attendu |
|---|--------|------------------|
| 5.1 | En mode édition, repérer le bloc de co-rédaction : un menu **« Choisir une section… »** + un champ **« Instruction : « renforce la prescription »… »** | Le menu liste les **sections de l'acte** (titres `##`/`###`) ; champ d'instruction libre + bouton de régénération |
| 5.2 | Choisir une section (ex. « Sur l'absence de faute grave ») → saisir une instruction (ex. « renforce ce moyen sur la prescription », « durcis le ton ») → lancer | **Seule la section choisie** est régénérée selon l'instruction ; le reste de l'acte est **inchangé** |
| 5.3 | Vérifier | Le markdown reste valide (titres conservés) ; pas de jargon ; cohérent avec les gardes (anti-invention) |

> ⚠️ Si 5.1/5.2 ne répond pas → le **backend SF-265-01 n'est pas encore déployé** (cf. pré-requis 1).

---

## TEST 6 — F-266 · Traçabilité au survol + export à en-tête cabinet
| # | Action | Résultat attendu |
|---|--------|------------------|
| 6.1 | Dans l'**aperçu** de l'acte, **survoler un renvoi « Pièce n° X »** | Infobulle indiquant **le libellé de la pièce** correspondante (traçabilité fait → pièce, ancrage sur `piece_number` F-260) |
| 6.2 | Avant export, renseigner la zone **« En-tête du cabinet »** (placeholder « Cabinet Durand & Associés / 12 rue de la Loi, 75001 Paris / Avocat au Barreau de Paris ») | Champ libre accepté |
| 6.3 | **Exporter** (Word puis PDF) | Le document exporté porte l'**en-tête du cabinet** saisi, en tête de l'acte |

> Hors scope (différé) : survol montant→calcul (F-263 clos), survol article→texte (pas de base légale embarquée).

---

## Check-list finale
- [ ] **F-261** : moyens adverses réfutés (doc marqué « écritures adverses »)
- [ ] **F-264** : éditeur barre d'outils + aperçu live ; export sans perte
- [ ] **F-265** : régénération d'une section sur instruction (le reste inchangé)
- [ ] **F-266** : survol « Pièce n° X » → libellé ; export en-tête cabinet
- [ ] **Rappels F-98** : anti-jargon, numérotation/bordereau, subsidiaires, finitions OK

> **Données de test** : `test-data/dossier-licenciement-durand/` (avec `06-conclusions-adverses-employeur.txt` pour F-261 — ⚠️ à rendre cohérent avec les parties du dossier). `dossier-jurisprudence-lemaire/` pour les citations.
