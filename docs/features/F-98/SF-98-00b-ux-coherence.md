# F-98 — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-18
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Périmètre du cadrage** : volet **conclusions** de F-98, V1 dev = **SF-98-01** (conclusions CPH bureau de jugement / fond, FR, demandeur, droit du travail). Le volet courrier et les 52 autres cellules de la matrice sont hors de ce cadrage.

---

## Verdict

**GO avec ajustements**

Le placement de la génération de conclusions est cohérent : l'onglet « Décision » du détail dossier, en aval des outils décisionnels, est exactement l'étape du parcours où l'avocat produit la pièce de procédure. Trois ajustements à intégrer dans la mini-spec : (a) rendre lisible que la section consomme l'amont, (b) bandeau de transparence obligatoire, (c) articuler explicitement l'état terminal du parcours, jusqu'ici non explicite.

---

## Intention métier + comportement visible attendu

**Intention** : permettre à l'avocat de générer un projet de conclusions juridiques à partir d'un dossier déjà analysé, en s'appuyant sur la synthèse, les pièces, les outils décisionnels et les pistes stratégiques.

**Comportement visible** : dans l'onglet « Décision » du détail dossier, une section « Conclusions » offre un bouton « Générer le projet de conclusions ». Après traitement asynchrone, le texte généré s'affiche, précédé d'un bandeau de transparence, avec une action de copie.

---

## Rappel verdict feature-coherence-challenger (étape 0)

`docs/features/F-98/SF-98-00-coherence.md` — verdict **GO avec ajustements** (2026-05-15). Pré-requis bloquant F-243 (stade procédural) **livré** ; pré-requis qualité F-DT-36 (nullité procédure) **livré**. Le verdict fonctionnel est donc levé : le présent cadrage écran est légitime.

---

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : `docs/business/parcours-ecran-dossier.md` (référentiel) + structure en onglets F-244 SF-244-01 + pratique avocat contentieux (droit du travail FR).

1. L'avocat ouvre un dossier → écran détail dossier, **structure en 4 onglets** (F-244) : **Dossier / Analyse / Décision / Suivi**.
2. Onglet **Dossier** : métadonnées (domaine, description), **stade procédural** (F-243 — juridiction + stade + position), import des pièces.
3. Onglet **Analyse** : lancement du pipeline IA, accès à la synthèse (faits, points juridiques, risques, timeline).
4. Onglet **Décision** : panneau des outils décisionnels (calculators / analyzers) puis tableau de bord décisionnel agrégeant les verdicts.
5. L'avocat renseigne le stade procédural du dossier (onglet Dossier).
6. L'avocat remplit les outils décisionnels pertinents (onglet Décision).
7. L'avocat consulte le tableau de bord décisionnel — verdicts agrégés (onglet Décision).
8. **L'avocat génère le projet de conclusions** ⬅ **F-98 / SF-98-01** — il consolide synthèse + stade + outils + pistes en une pièce de procédure.
9. L'avocat relit le projet, le copie, le finalise dans son outil de traitement de texte.
10. Onglet **Suivi** : échéances, notes, calendrier procédural jusqu'à l'audience.
11. **État terminal** : le traitement métier du dossier est terminé lorsque **le projet de conclusions est généré** (puis, à terme via SF-98-50, exporté). Le statut `OPEN/CLOSED` reste une action administrative distincte.

---

## État terminal du processus (explicite)

Le référentiel `parcours-ecran-dossier.md` notait l'état terminal comme « non explicite à ce jour » et renvoyait son articulation au cadrage écran de F-98. **Ce cadrage le tranche** :

> **L'état terminal du traitement métier d'un dossier = « projet de conclusions généré ».** C'est la dernière production substantielle de l'avocat dans le produit. En V1 (SF-98-01), l'avocat copie le texte ; l'export Word/PDF (SF-98-50/51) viendra fluidifier la sortie. Le suivi procédural (onglet Suivi) est un accompagnement post-conclusions, pas une étape de production.

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|
| 2 Stade procédural | Onglet Dossier — `procedure-stage-section` (F-243) | ✅ existant |
| 3 Synthèse | Onglet Analyse — `synthesis` | ✅ existant |
| 4/6 Outils décisionnels | Onglet Décision — `decisional-tools-panel` | ✅ existant |
| 7 Tableau de bord décisionnel | Onglet Décision — `case-dashboard` | ✅ existant |
| 8 **Génération conclusions** | — | ❌ **manquant → F-98 SF-98-01** |
| 10 Suivi | Onglet Suivi — `case-deadlines-section`, `case-notes-section` | ✅ existant |

---

## Position candidate de la feature

- **Écran** : détail dossier — `case-file-detail.component`.
- **Onglet** : **Décision** (`TAB_DECISION = 2`).
- **Zone** : nouveau bloc primaire `app-conclusions-section`, placé **en bas de l'onglet Décision**, après le tableau de bord décisionnel (`case-dashboard`).
- **Point d'entrée** : un seul — bouton « Générer le projet de conclusions » dans la section. Pas d'entrée depuis le stepper ni le header en V1.

---

## Challenge placement

**Question** : l'onglet Décision correspond-il à l'étape où l'avocat a besoin de générer ses conclusions ?

**Oui.** L'onglet Décision porte déjà la séquence outils → tableau de bord. La génération de conclusions est la **consolidation naturelle** de ce travail décisionnel : elle consomme précisément les verdicts des outils et les pistes stratégiques. La placer ailleurs (onglet Suivi) la couperait de ses intrants visibles. L'onglet Suivi est réservé à l'après (échéances, audience). **Placement validé.**

---

## Challenge lisibilité de la séquence

**Question** : l'UI rend-elle visible que les conclusions arrivent *après* synthèse, stade et outils ?

**Partiellement — ajustement requis.** Le placement en bas de l'onglet Décision donne déjà un ordre visuel (outils → dashboard → conclusions). Mais rien ne relie la section conclusions à l'onglet Analyse (synthèse) ni à l'onglet Dossier (stade). **Ajustement a2** : la section doit (a) afficher une ligne explicite « Ce projet s'appuie sur la synthèse, le stade procédural, les outils décisionnels et les pistes stratégiques du dossier » et (b) **gater la génération** par un message guidant lorsqu'un pré-requis manque (« Renseignez le stade procédural » / « Lancez l'analyse du dossier ») plutôt que par un bouton inerte ou une erreur muette. Le gating rend la séquence lisible par l'usage.

---

## Challenge charge écran

**Question** : densité totale de l'onglet Décision après ajout ?

Onglet Décision avant F-98 : `decisional-tools-panel` + `case-dashboard` = **2 blocs primaires**. Après ajout : **3 blocs primaires**. La structure en onglets livrée par F-244 a précisément réparti la charge de l'écran détail (auparavant ~10 blocs sur une page). 3 blocs dans un onglet thématiquement homogène (« décision ») reste lisible. **Pas de surcharge — pas d'éclatement supplémentaire requis.** Invariant : l'onglet Décision ne dépasse pas 3 blocs primaires en V1 ; toute SF F-98 ultérieure (éditeur, versions) s'intègre **dans** la section conclusions, pas comme bloc autonome.

---

## Challenge état final / continuité

**Question** : après la génération, que fait l'avocat ?

L'avocat relit le projet et le **copie** pour le finaliser dans son traitement de texte (V1). C'est un point de sortie explicite, assumé : SF-98-01 livre une action « Copier ». L'export Word (SF-98-50) et l'éditeur de relecture (SF-98-49) sont des SF distinctes de la matrice F-98 qui fluidifieront cette sortie — leur absence en V1 n'est pas un dead-end, c'est une sortie minimale viable (copier-coller). **Ajustement a4** : la section nomme son état — « Projet généré » — et n'enferme pas l'avocat (pas de ping-pong subi vers la synthèse). La régénération est un choix explicite (bouton « Régénérer »), pas un effet de bord.

---

## Ajustements IA requis (à intégrer dans la mini-spec SF-98-01)

- **a1 — Placement** : section `app-conclusions-section` dans l'onglet Décision, en bas, après `case-dashboard`.
- **a2 — Lisibilité séquence** : ligne d'explication des intrants + gating des pré-requis par message guidant (stade procédural renseigné + analyse terminée), pas de masquage muet.
- **a3 — Transparence** : bandeau permanent au-dessus du texte généré — « Projet généré automatiquement — relecture par l'avocat obligatoire avant tout dépôt » (couvre le risque « responsabilité juridique » de la mise en stand-by F-98 du 01/04 ; invariant 10 du cadrage étape 0).
- **a4 — État terminal** : la section nomme l'état « Projet généré » ; action de sortie « Copier » ; régénération explicite.

---

## Invariants anti-surcharge pour la mini-spec

1. La section conclusions est **un seul bloc primaire** dans l'onglet Décision. Toute capacité F-98 ultérieure (éditeur SF-98-49, versions SF-98-52, bandeau de régénération SF-98-53) s'intègre dans cette section — jamais comme nouveau bloc.
2. **Seuil de charge** : l'onglet Décision ne dépasse pas 3 blocs primaires en V1.
3. Tout output a un **point de sortie explicite** : « Copier » en V1, export Word en SF-98-50.
4. Les **pré-requis manquants** s'affichent comme message guidant orienté action, jamais comme bouton inerte ni erreur muette.
5. L'**ordre des étapes** (synthèse → stade → outils → conclusions) reste lisible sans interaction : placement en bas de l'onglet + ligne d'explication des intrants.
6. La **régénération** est toujours un choix explicite de l'avocat.

---

## Décision finale

**GO avec ajustements.** SF-98-01 s'insère dans l'onglet « Décision » du détail dossier, en bas, comme section `app-conclusions-section`. Les 4 ajustements a1–a4 et les 6 invariants anti-surcharge sont à reprendre dans la mini-spec. L'état terminal du parcours détail dossier est désormais articulé : « projet de conclusions généré ».

---

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` est enrichi : (a) intégration de la structure en 4 onglets F-244 dans le parcours, (b) ajout de l'étape 8 « génération conclusions » dans l'onglet Décision, (c) **l'état terminal n'est plus « non explicite »** — il est tranché à « projet de conclusions généré », (d) nouvelle ligne d'historique des passages.

---

## Liens

- `docs/features/F-98/SF-98-00-coherence.md` — cadrage cohérence fonctionnelle (étape 0)
- `ai-skills/screen-coherence-challenger.md` — skill appliquée
- `docs/business/parcours-ecran-dossier.md` — référentiel parcours écran (enrichi par ce passage)
- F-244 SF-244-01 — structure en onglets du détail dossier
- F-243 — stade procédural (pré-requis livré)
