# Audit cohérence écran — Détail du dossier (outils décisionnels)

> Audit produit via la skill `ai-skills/screen-coherence-challenger.md` (étape 0 bis du cycle).
> **Nature** : audit rétrospectif sur un écran existant — pas un cadrage 0 bis pré-dev.
> **Date** : 2026-05-15 — **2ᵉ passage** sur l'écran détail dossier (le 1ᵉʳ est le cadrage F-243).
> **Référentiel** : `docs/business/parcours-ecran-dossier.md`.

## Verdict : GO avec ajustements

Le concept — outils décisionnels et tableau de bord sur l'écran détail dossier — est **cohérent**. L'avocat y est déjà (import, analyse) : c'est son plan de travail du dossier. Le doute initial portait à juste titre sur l'**exécution**, pas sur le concept. Quatre défauts d'architecture de l'information sont à corriger.

## Intention métier + comportement visible audité

Donner à l'avocat, sur l'écran du dossier, des outils de simulation (calcul d'indemnités, fiche prud'homale, checklist, etc.) et un tableau de bord agrégeant leurs verdicts, pour l'aider à arbitrer sa stratégie.

## Rappel verdict cohérence fonctionnelle

Audit rétrospectif : pas d'étape 0 amont. Les features auditées — F-IA-01 (pré-remplissage), F-IA-02 (tableau de bord transversal), F-IA-03 (contrôle de cohérence), F-IA-04 (affichage conditionnel), outils par domaine (F-DT/F-IM/F-FA), F-184 (repositionnement) — sont **livrées**. Leur existence fonctionnelle est acquise. L'audit ne challenge **que leur insertion écran**.

## Parcours écran réel de l'avocat (ouverture du dossier → état terminal)

Source : ⚠ **hypothèse à valider auprès d'un avocat**. Étayée par `docs/features/F-114/SF-114-01-e2e-parcours-metier.md` (parcours e2e), `docs/PRODUCT_SPEC.md`, et signaux terrain (démos Renversez 13/05 et Mengue 11/05 — friction UX post-synthèse documentée ; F-98/F-243 « conclusions » sont nées de ces démos).

| # | Étape métier de l'avocat | Écran / zone |
|---|---|---|
| 1 | Reçoit le dossier + pièces du client, crée le dossier | Liste dossiers → **détail dossier** (vide) |
| 2 | Verse les pièces | Détail dossier — col-left, section Documents |
| 3 | Lance l'analyse (asynchrone) | Détail dossier — col-right, section Analyse + pipeline |
| 4 | Consulte la synthèse (faits, timeline, points juridiques, risques, questions ouvertes) | **Écran synthèse dédié** `/case-files/:id/synthesis` |
| 5 | Répond aux questions complémentaires → re-synthèse enrichie | Écran synthèse dédié |
| 6 | Identifie la situation juridique (type de litige / procédure) | F-IA-04 (détection) + `decisional-tools-panel` (affichage) |
| 7 | Renseigne les outils décisionnels pertinents | Détail dossier — **col-left, bas** (`decisional-tools-panel`) |
| 8 | Lit les verdicts agrégés | Détail dossier — **col-right, bas** (`decisional-summary-panel`) |
| 9 | Vérifie la cohérence saisie ↔ preuves du dossier (F-IA-03) | Réparti entre col-left (outils) et écran synthèse (preuves) |
| 10 | Arbitre la stratégie sur la base des verdicts | Aucune zone dédiée |
| 11 | Produit le livrable de procédure (fiche prud'homale F-DT-04, requête F-DT-06, recours F-IM-06, conclusions F-98/F-243) | Outils d'export par domaine |
| 12 | Dépose / transmet le livrable | Hors LegalCase |
| 13 | Clôture le dossier ou le garde ouvert pour suivi | Détail dossier — bouton « Clôturer » (en-tête) |

## État terminal du processus

⚠ **Non matérialisé.** L'état terminal réel est soit « livrable de procédure produit et déposé », soit « dossier clôturé ». Le produit ne porte aucun jalon UI unique « dossier traité ». Le statut `OPEN`/`CLOSED` du dossier en est le plus proche, mais il ne reflète pas l'achèvement du *traitement métier*. F-98 / F-243 (génération de conclusions) sont la brique d'état terminal en cours de construction.

## Cartographie écrans / zones ↔ parcours

L'écran détail dossier (`/case-files/:id`, `CaseFileDetailComponent`) est en **2 colonnes** sous un en-tête plein largeur :

- **En-tête** : titre + actions (Export ZIP, Clôturer, Supprimer) · `app-timer-widget` · `app-case-dashboard-stepper` (5 étapes)
- **col-left** : `detail-card` (métadonnées) · `stats-card` · section Documents (repliable) · `case-deadlines-section` · `case-notes-section` · **`decisional-tools-panel`**
- **col-right** : `quota-error-banner` · section Analyse · `analysis-pipeline` · section Synthèse (lien) · `questions-banner` · **`decisional-summary-panel` / `case-dashboard`**

La synthèse n'est **pas** un bloc de cet écran : c'est une page dédiée, atteinte par le lien « Voir la synthèse » dans col-right.

## Position des éléments audités

- **Outils décisionnels** (`decisional-tools-panel`) — étape 7 du parcours — **col-left, dernier bloc**.
- **Tableau de bord décisionnel** (`decisional-summary-panel`) — étape 8 — **col-right, dernier bloc**.

Les deux moitiés d'une même activité (saisir une simulation / lire son verdict) sont dans **deux colonnes différentes**.

## Challenge placement

L'écran détail dossier est un hôte **défendable** : l'avocat y travaille déjà. Mais deux défauts :

- **Défaut 1 — couplage saisie → verdict fragile.** Le découpage *colonne d'entrée (outils, col-left) / colonne de sortie (verdicts, col-right)* est un modèle **défendable** : le tableau de bord en lecture seule joue un rôle de *scoreboard* persistant pendant que l'avocat travaille les outils. Mais le pattern ne tient qu'à deux conditions, **non garanties aujourd'hui** : (a) **alignement vertical** — outils (bas col-left) et tableau de bord (bas col-right) doivent être à la même hauteur, or col-left est souvent bien plus haute (section Documents volumineuse), ce qui sort le verdict du champ de vision ; (b) **découvrabilité du lien** — rien ne signale que remplir un outil à gauche met à jour le tableau à droite.
- **Défaut 2 — la synthèse est sur un autre écran.** Réponse directe à la question « faut-il que ce soit accessible depuis la synthèse ? » : aujourd'hui ce n'est **pas** le cas. Les outils (étape 7) et la synthèse (étape 4) ne sont pas « au même endroit ». Le contrôle de cohérence F-IA-03 (étape 9 : croiser saisie ↔ preuves) force un aller-retour entre deux écrans.

## Challenge lisibilité de la séquence

La séquence métier réelle — documents → analyse → synthèse → questions → outils → tableau de bord → livrable — **n'est pas lisible** :

- Le `case-dashboard-stepper` (haut d'écran) affiche un **autre** modèle en 5 étapes (Documents → Analyse → Questions → Délais → Pièces manquantes). Il **omet** la synthèse, les outils décisionnels et le tableau de bord — précisément les étapes en jeu. Le seul élément qui pourrait porter la séquence en ignore la moitié.
- La disposition en 2 colonnes casse l'ordre de lecture : descendre l'écran ne suit pas le parcours.
- La synthèse n'apparaît que comme un lien noyé dans col-right → **impossible de « voir que la synthèse était là avant les outils »**. Le constat de l'utilisateur est exact.
- Symptôme révélateur : le référentiel `parcours-ecran-dossier.md` (cadrage F-243) **fusionnait lui-même** « synthèse » et « tableau de bord décisionnel » en un seul bloc. La frontière n'est claire pour personne — ce n'est pas un détail, c'est la racine de la confusion.

## Challenge charge écran

Décompte des régions UI sur la route `/case-files/:id` : ~4 (en-tête) + 6 (col-left) + ~6 (col-right) = **~16 régions distinctes sur une seule route**.

L'écran est **déjà reconnu surchargé** : les commentaires `F-184 SF-184-01` (« tableau de bord repositionné dans col-right ») et `F-184 SF-184-02` (« remontée des 3 blocs ex-bottom-sections dans col-left pour combler le trou visuel ») prouvent qu'une vague entière a été nécessaire pour rééquilibrer cet écran. Le référentiel F-243 le note : « ~10 blocs primaires. Écran dense. »

**L'instinct de l'utilisateur est correct : l'écran détail dossier est en surcharge.** Les composants décisionnels, eux-mêmes denses (panel à 3 couches, dashboard multi-thèmes), aggravent la situation en l'état.

## Challenge état final / continuité

Question centrale de l'utilisateur : « après que les dashboards sont calculés, quelle est la suite ? »

- **Réponse de l'audit : aujourd'hui, rien.** Le `decisional-summary-panel` est le dernier bloc de col-right. Après les verdicts, aucune affordance « étape suivante ». Le tableau de bord est un **cul-de-sac visuel**.
- L'action terminale réelle — produire le livrable (fiche prud'homale, requête, recours) ou clôturer — n'est **pas connectée** au tableau de bord : les exports sont dans les outils (col-left) ; le bouton « Clôturer » est dans l'en-tête, à l'opposé de l'endroit où le processus se conclut.
- Le « ping-pong avec la synthèse » redouté est **réel** : F-IA-03 pousse l'avocat à revenir vérifier la synthèse (autre écran) pendant qu'il renseigne les outils. Va-et-vient subi, non conçu.

## Ajustements IA requis

1. **Fiabiliser le couplage saisie → verdict.** Conserver le découpage colonne d'entrée / colonne de sortie (modèle *scoreboard*). Garantir (a) l'alignement vertical des outils et du tableau de bord pour que le verdict reste dans le champ de vision pendant la saisie, et (b) un signal explicite du lien outil → tableau de bord (retour visuel à la mise à jour). Ne pas fusionner les deux blocs.
2. **Rendre la séquence lisible.** Étendre le `case-dashboard-stepper` à Synthèse → Outils décisionnels → Tableau de bord → Conclusions, ou numéroter les blocs de traitement. Le stepper doit refléter le parcours réel.
3. **Reconnecter synthèse et outils.** Point d'entrée explicite synthèse ↔ outils dans les deux sens, pour que F-IA-03 ne force pas un aller-retour aveugle.
4. **Donner un état terminal.** Le tableau de bord doit mener à l'action terminale : « générer les conclusions » (F-98) et/ou « clôturer le dossier ».
5. **Plafonner la charge et structurer en onglets.** L'écran (~16 régions) est saturé. Deux volets :
   - **(A) Règle plafond** — aucun nouveau bloc primaire autonome sur `/case-files/:id` sans en retirer ou regrouper un autre.
   - **(B) Regroupement structurel** — remplacer la page à scroll unique par des onglets par phase du parcours : Dossier (métadonnées, documents) / Analyse (pipeline, lien synthèse, questions) / Décision (outils + tableau de bord) / Suivi (échéances, notes). L'onglet « Décision » héberge le duo outils + tableau de bord, agencement col-left / col-right conservé (cf. ajustement 1).
   - **Sous-règle pré-remplissage IA** — un onglet fermé ne doit pas masquer le travail de l'IA. L'onglet « Décision » porte un badge agrégé `auto_awesome` égal à la somme des `getPrefillCount()` des outils visibles ; si ce total > 0 au retour de l'analyse, l'onglet est mis en avant (pastille ou pré-sélection). Le badge se met à jour via `CaseDashboardRefreshService.triggerRefresh()` (+ `markForCheck()` si le composant est en OnPush). Le `case-dashboard-stepper` étendu (ajustement 2) porte le même compteur sur son étape « Outils décisionnels ».
   - **Compromis assumé** — les onglets sacrifient la vue « tout en un seul scroll ». Si cette vue d'ensemble est jugée nécessaire : sections repliables plutôt qu'onglets, ou un onglet « Vue d'ensemble ».

Ces ajustements doivent être triés au backlog `PRODUCT_SPEC.md` (validation requise avant dev) — ils ne sont pas créés ici. Candidat naturel : une feature « refonte IA de l'espace décisionnel du dossier ».

## Invariants anti-surcharge (toute mini-spec future touchant cet écran)

- Aucun nouveau bloc primaire autonome sur `/case-files/:id` sans en retirer ou regrouper un autre — l'écran est **plafonné**.
- Toute surface décisionnelle nouvelle s'insère dans l'« espace décisionnel » regroupé, jamais dispersée entre colonnes.
- La saisie d'un outil et son verdict restent visibles ensemble — pas de couplage cause → effet hors champ de vision.
- Tout bloc du parcours de traitement déclare sa position dans la séquence (stepper ou numérotation) — pas de bloc « hors séquence ».
- Tout bloc terminal (tableau de bord, conclusions) expose l'étape suivante ou l'action de clôture — pas de cul-de-sac.
- Un onglet ou une section repliable ne doit jamais masquer un signal de pré-remplissage IA : tout compteur de prefill (`getPrefillCount()`) remonte au niveau du conteneur (onglet, stepper). L'avocat doit voir que l'IA a travaillé pour lui sans avoir à ouvrir le conteneur.

## Décision finale

**GO avec ajustements.** Le concept est validé ; l'utilisateur a eu raison de challenger l'exécution. Les 5 ajustements IA sont à porter au backlog comme feature de refonte avant tout nouvel ajout sur l'écran détail dossier.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` enrichi :
- Correction : « synthèse » (écran dédié) et « tableau de bord décisionnel » (bloc col-right) dissociés — ils étaient fusionnés à tort.
- Ajout : disposition en 2 colonnes ; la synthèse n'est pas un bloc de cet écran.
- Ajout : ligne d'historique du passage 2026-05-15 (audit outils décisionnels).
