# Plan de test — Fonctionnalités jurisprudence (staging) — version chirurgicale

> Environnement : **https://staging.legalcase.fr** · Workspace **`DROIT_DU_TRAVAIL / FRANCE`** · Dossier de test **`dossier-jurisprudence-lemaire/`** (4 PDF).
> Tous les libellés entre « » sont les **textes exacts affichés** dans l'UI.

## ⚠️ Ce qui a changé depuis le dernier test (correctifs déployés 2026-06-08, à valider en priorité)

| Fix | Où le valider | Attendu |
|-----|---------------|---------|
| **SF-179-05** — confirmation existence FR via JUDILIBRE | TEST 1 (1.7) | Les arrêts FR réels moins connus (`98-41.609`, `88-44.308`) passent **« Vérifiée »** au lieu de « Incertaine » (si présents dans le fonds JUDILIBRE) |
| **Liens courdecassation** (PR #1603) | TEST 2 (2.5) | Le lien sous un arrêt d'outil ouvre une **page valide** (`courdecassation.fr/decision/...`), plus de 403 Légifrance |
| **F-258** — alerte outils non calculés | TEST 3 (3.0) | Encart **« N outil(s)… ne sont pas encore calculés »** au-dessus du bouton « Générer », bouton **« Voir les outils à calculer »** |
| **F-DT-08/09 visibilité** (migration 544) | TEST 2 (2.1) | **« Comparateur d'indemnités »** et **« Validité du licenciement »** de nouveau **visibles/proposés** sur le dossier licenciement FR |
| **F-JU-06 assainissement** | TEST 2 (2.4) | **Plus d'arrêt hors-sujet** (« restauration ferroviaire ») sous le Comparateur d'indemnités |

> 🔑 **PRÉ-REQUIS** : ces fixes s'appliquent à une **analyse fraîche**. Les vérifications du dossier déjà testé datent d'avant le déploiement. **Crée un NOUVEAU dossier** (« Lemaire — jurisprudence v2 ») OU **relance une analyse** sur le dossier existant avant de tester TEST 1 / TEST 3.

## Carte de navigation (routes réelles)

| Écran | Route |
|-------|-------|
| Détail dossier | `/case-files/:id` — onglets : « Dossier » · « Analyse » · « Décision » · « Suivi » |
| Synthèse | `/case-files/:id/synthesis` |
| Points juridiques | `/case-files/:id/synthesis/points-juridiques` |
| Admin jurisprudence | `/super-admin/jurisprudence-watch` |

---

## ÉTAPE 0 — Préparer le dossier

| # | Clic / action exacte | Résultat attendu exact |
|---|----------------------|------------------------|
| 0.1 | Login staging, sélectionner le workspace `DROIT_DU_TRAVAIL / FRANCE` | Connecté sur le bon workspace |
| 0.2 | Créer un dossier, titre « Lemaire — jurisprudence » | Arrivée sur `/case-files/:id`, onglet **« Dossier »** actif |
| 0.3 | Onglet **« Dossier »** → bouton **« Ajouter des documents »** → sélectionner les **4 PDF** de `dossier-jurisprudence-lemaire/` | 4 documents listés dans la section DOCUMENTS |
| 0.4 | Onglet **« Analyse »** → bouton **« Analyser le dossier »** (icône baguette) | Job lancé ; à la fin, l'analyse est marquée terminée |
| 0.5 | Onglet **« Analyse »** → bouton **« Voir la synthèse »** | Redirige vers `/case-files/:id/synthesis` |

---

## TEST 1 — F-179 · jurisprudences **citées** dans les documents
**Écran : page Synthèse** (`/case-files/:id/synthesis`)

| # | Clic / action | Résultat attendu exact |
|---|---------------|------------------------|
| 1.1 | Sur la synthèse, dérouler la section **« Jurisprudences citées »** (icône marteau) | La section existe et liste les références détectées dans `04-conclusions-salarie.pdf` |
| 1.2 | Lire le **badge** de chaque référence | Voir le tableau des statuts attendus ci-dessous |
| 1.3 | Sur `Cass. soc. 11 mai 2022, n° 21-14.490` | Badge **« Vérifiée »** (vert, ✓) + lien source cliquable |
| 1.4 | Sur `Cass. soc. 30 février 2021, n° 99-99.999` | Badge **« Non trouvée »** (✗), **aucun lien** vers un arrêt |
| 1.5 | Sur `Cass. civ. 2e 11 mai 2022, n° 21-14.490` | Badge **« Suspecte »** + mention **« Citation à vérifier »** ; survol → popover « Position alléguée incohérente… » |
| 1.6 | Cliquer le lien source d'une « Vérifiée » | Ouvre l'arrêt sur Légifrance/Judilibre |
| 1.7 | **(SF-179-05)** Regarder le statut de `98-41.609` et `88-44.308` | Idéalement **« Vérifiée »** (confirmés par JUDILIBRE) — c'est le gain attendu vs « Incertaine » avant. « Incertaine » reste **acceptable** si l'arrêt n'est pas dans le fonds JUDILIBRE (1990 = couverture historique partielle) |

**Statuts attendus** (les libellés sont les textes UI réels) :

| Référence dans le PDF | Badge attendu |
|-----------------------|---------------|
| Cass. soc. 11 mai 2022, n° 21-14.490 | **« Vérifiée »** |
| Cass. soc. 12 déc. 2000, n° 98-41.609 | **« Vérifiée »** (SF-179-05 via JUDILIBRE) — sinon « Incertaine » |
| Cass. soc. 30 nov. 1990, n° 88-44.308 | **« Vérifiée »** (SF-179-05) — « Incertaine » tolérée si hors fonds JUDILIBRE |
| Cass. **civ. 2e** 11 mai 2022, n° 21-14.490 | **« Suspecte »** *(si « Vérifiée » → faille : la chambre n'est pas contrôlée → à signaler)* |
| Cass. soc. 30 février 2021, n° 99-99.999 | **« Non trouvée »** |

> ✅ **Invariant SF-179-05** : aucun **faux** « Vérifiée » ne doit apparaître — un arrêt n'est confirmé que si JUDILIBRE renvoie le **numéro de pourvoi exact**. La citation `civ. 2e` (mauvaise chambre, même numéro) doit rester **« Suspecte »**, jamais « Vérifiée ».

> Si **toutes** les références ressortent « Incertaine », un bandeau s'affiche : « La vérification automatique n'a pas pu conclure… Vérifiez-les manuellement (Légifrance / Juridat). » → acceptable, mais le cas `99-99.999` doit rester « Non trouvée ».

---

## TEST 2 — F-JU-01 · « Jurisprudence applicable » dans un outil décisionnel
**Écran : onglet « Décision »**

> ✅ **(migration 544)** Le **Comparateur d'indemnités (F-DT-09)** et la **Validité du licenciement (F-DT-08)** sont désormais **proposés** sur un dossier de licenciement FR (ils étaient invisibles avant le fix). Vérifie en **2.1** qu'ils apparaissent bien dans le panneau d'outils (section « Indemnités & calculs » / « Validité »).
> Pour un test jurisprudence fiable, on cible aussi un outil **déjà affiché ET pré-rempli** : **« Ancienneté et congés »** (`F-DT-07`, chambre sociale), qui possède un mapping jurisprudence. Tous les outils ci-dessous affichent le bloc s'ils sont mappés.

| # | Clic / action | Résultat attendu exact |
|---|---------------|------------------------|
| 2.1 | Onglet **« Décision »** → panneau d'outils, section **« Indemnités & calculs »** | Grille de tuiles |
| 2.2 | Cliquer la tuile **« Ancienneté et congés »** (1ʳᵉ tuile) | Le panneau de l'outil se déploie, champs pré-remplis par l'IA (convention SYNTEC, date d'entrée 05/03/2017, ancienneté ≈ 9 ans) |
| 2.3 | Si un bouton de calcul est présent (ex. **« Calculer »**), le cliquer ; sinon le résultat est déjà affiché | Résultat de l'outil affiché |
| 2.4 | **Descendre sous le résultat** → repérer le bloc **« Jurisprudence applicable »** | ≥ 1 arrêt de la chambre sociale cité, avec lien. **(F-JU-06)** Aucun arrêt **hors-sujet** (ex. plus de « restauration ferroviaire » sur le Comparateur d'indemnités) |
| 2.5 | **(PR #1603)** Cliquer le lien de l'arrêt | Ouvre une **page valide** de l'arrêt (`courdecassation.fr/decision/...` ou Légifrance) — **plus de page morte / 403** |
| 2.5b | **(Comparateur d'indemnités F-DT-09)** L'ouvrir (panneau ou Catalogue), **« Comparer »**, regarder « Jurisprudence applicable » | Soit un arrêt **pertinent** au barème Macron, soit **aucun** arrêt (silence > erreur) — mais **jamais** un arrêt hors-sujet |
| 2.6 | Cliquer **« Signaler »** (icône drapeau) | Un formulaire inline apparaît avec un champ **« Commentaire (optionnel) »** |
| 2.7 | Saisir un commentaire → **« Envoyer »** | Snackbar **« Signalement transmis. Merci. »** |
| 2.8 | Répéter sur 1–2 autres outils affichés : **« Licenciement nul – détection »**, **« Nullité de procédure de licenciement »**, **« Fiche prud'homale »** | Bloc **« Jurisprudence applicable »** présent (ces outils FR sont mappés) |

> Un outil **sans** bloc jurisprudence = soit non mappé (normal), soit calcul pas encore lancé. Pour ouvrir le Comparateur d'indemnités spécifiquement : panneau d'outils → section **« Catalogue »** (bas de page) → chercher **« Comparateur d'indemnités »** → l'ouvrir manuellement → **« Comparer »**.

---

## TEST 3 — F-JU-02 · jurisprudence dans les **conclusions générées**
**Écran : onglet « Décision »** (après avoir utilisé ≥ 1 outil mappé au TEST 2)

| # | Clic / action | Résultat attendu exact |
|---|---------------|------------------------|
| 3.0 | **(F-258)** AVANT de calculer tous les outils, descendre à la section **« Projet de conclusions »** | Si des outils proposés ne sont pas encore calculés → **encart d'avertissement** « N outil(s) décisionnel(s) pertinent(s) ne sont pas encore calculés… » au-dessus du bouton **« Générer »**, avec un bouton **« Voir les outils à calculer »**. Le bouton « Générer » **reste actif** (non bloquant) |
| 3.0b | **(F-258)** Cliquer **« Voir les outils à calculer »** | La page **défile** vers le panneau d'outils décisionnels (mise en évidence brève) |
| 3.0c | **(F-258)** Calculer tous les outils proposés, revenir à la section conclusions (ré-afficher) | L'encart **disparaît** quand il ne reste plus d'outil non calculé (N=0) |
| 3.1 | Onglet **« Décision »** → section **Conclusions** (colonne verdict) | Générateur de conclusions disponible |
| 3.2 | Générer / régénérer les conclusions | Document produit |
| 3.3 | Dans le document, chercher la rubrique **« Jurisprudence applicable »** | Les arrêts des outils **calculés** (TEST 2) y figurent. ⚠️ Rappel : seuls les outils **calculés** alimentent les conclusions (cf. F-258) — d'où l'intérêt de les calculer avant |

---

## TEST 3bis — SF-98-55 · qualité rédactionnelle des conclusions (anti-jargon)
**Écran : onglet « Décision »** → conclusions générées au TEST 3 (lire le **texte de l'acte**, pas les outils)

> 🎯 **Objet** : la garde `REDACTION_QUALITY_GUARD` (PR #1611) interdit le jargon interne dans l'acte et impose syllogisme + dispositif complet. Elle s'applique à **toute** génération (même sans outil calculé). Se valide **à l'œil** sur le texte produit en 3.2.

| # | À chercher dans le texte de l'acte | Résultat attendu exact |
|---|------------------------------------|------------------------|
| 3b.1 | **Codes d'outils** : `Ctrl+F` sur « F-DT », « F-IM », « F-FA », « f-dt-08 » | **Aucune occurrence** — un code d'outil dans l'acte = FAIL |
| 3b.2 | **Scores bruts** : chercher « critères sur », « niveau de risque », « ÉLEVÉ », « INVALIDE » / « VALIDE » employés seuls | **Aucun** score brut d'outil dans le corps — traduits en moyens de droit |
| 3b.3 | **Libellés d'outils** : si un sujet d'outil est repris | En **clair / langage métier** (ex. « la validité du licenciement »), jamais l'id technique |
| 3b.4 | **Visa des articles** : chaque moyen vise un texte | ≥ 1-2 visas explicites (ex. « art. L. 1235-3 du Code du travail ») |
| 3b.5 | **Syllogisme** : règle → application aux faits → conséquence | Progression logique visible (« Or, en l'espèce… », « Par suite… »), pas une liste à puces sans lien |
| 3b.6 | **Rattachement aux pièces** | Références « Pièce n° X » dans les moyens |
| 3b.7 | **Dispositif « PAR CES MOTIFS »** | Présence des postes systématiques : **article 700 CPC**, **dépens**, **exécution provisoire**, **intérêts au taux légal + capitalisation (art. 1343-2 C. civ.)**, **astreinte** sur remise des documents (selon stade/juridiction) |
| 3b.8 | **Faits & procédure** | Chronologie claire + cadre procédural, ton sobre et strictement juridique |

> ✅ **Invariant** : le verdict brut des outils (« 2 critères sur 7 », « F-DT-08 ») reste de la **matière première interne** — il alimente le raisonnement mais ne doit **jamais** apparaître mot pour mot dans l'acte remis au juge.

---

## TEST 4 — F-241 · deeplinks éditeurs externes
**Écran : page Points juridiques** (`/case-files/:id/synthesis/points-juridiques`) ou sous un outil

| # | Clic / action | Résultat attendu exact |
|---|---------------|------------------------|
| 4.1 | Repérer la ligne **« Rechercher la jurisprudence : »** sous un point juridique | Boutons **« Doctrine »**, **« Lexis+ »**, **« Lextenso »** (icône loupe) |
| 4.2 | Cliquer **« Doctrine »** | **Nouvel onglet** avec recherche pré-remplie |
| 4.3 | Cliquer **« Lexis+ »** puis **« Lextenso »** | Recherche pré-remplie chez chaque éditeur |

> ⚠️ Sur un workspace **FR**, les 3 boutons sont visibles. En **BE**, « Lexis+ » et « Lextenso » sont **masqués** (normal, condition pays).

---

## TEST 5 — F-242 · ajout **manuel** d'une jurisprudence
**Écran : page Points juridiques** (`/case-files/:id/synthesis/points-juridiques`)

| # | Clic / action | Résultat attendu exact |
|---|---------------|------------------------|
| 5.1 | Sous un point juridique, cliquer **« + Jurisprudence à l'appui »** | Le panneau **« Jurisprudence à l'appui »** (icône marteau) se déploie avec un formulaire |
| 5.2 | Champ **« Référence »** → saisir `Cass. soc. 11 mai 2022, n° 21-14.490` | Saisie acceptée (placeholder d'origine : « Cass. soc. 12 oct. 2022, n° 21-12345 ») |
| 5.3 | Champ **« Portée (optionnel) »** → saisir `Barème Macron validé` | Saisie acceptée |
| 5.4 | Cliquer **« Ajouter »** | La citation apparaît dans la liste sous le point |
| 5.5 | Icône crayon (**« Modifier »**) → changer la portée → enregistrer | Citation mise à jour |
| 5.6 | Icône corbeille (**« Supprimer »**) | Citation retirée |
| 5.7 | (option) Régénérer les conclusions (TEST 3) | La citation manuelle apparaît au bon point |

---

## TEST 6 — (SUPER_ADMIN) `/super-admin/jurisprudence-watch`

| # | Clic / action | Résultat attendu exact |
|---|---------------|------------------------|
| 6.1 | Aller sur `/super-admin/jurisprudence-watch` | 4 onglets : **« Bootstrap »** · **« Flags à arbitrer »** · **« Audit log »** · **« Ajouter mapping »** |
| 6.2 | Onglet **« Audit log »** → **« Rafraîchir »** | Le signalement créé au TEST 2.7 apparaît dans l'historique |
| 6.3 | Onglet **« Flags à arbitrer »** → si un flag présent, **« Arbitrer »** | Form inline avec **« Remplacer » / « Ajouter » / « Ignorer » / « Annuler »** |
| 6.4 | Onglet **« Bootstrap »** | Boutons « Charger depuis un fichier .csv », « Exemple », « Lancer le bootstrap ». **NE PAS lancer de bootstrap BE** (F-JU-04 parké) |

---

## Check-list finale

- [ ] **F-179** : section « Jurisprudences citées » → 1 « Non trouvée » (99-99.999) + 1 « Suspecte » (civ. 2e) + ≥ 1 « Vérifiée », aucun lien mensonger
- [ ] **SF-179-05** : `98-41.609` / `88-44.308` passent « Vérifiée » (ou « Incertaine » si hors fonds JUDILIBRE) — **aucun faux « Vérifiée »**, `civ. 2e` reste « Suspecte »
- [ ] **F-DT-08/09 (migr. 544)** : « Comparateur d'indemnités » + « Validité du licenciement » proposés sur le dossier
- [ ] **F-JU-01** : bloc « Jurisprudence applicable » + **lien valide** (courdecassation/Légifrance, plus de 403) ; « Signaler » → « Signalement transmis. Merci. »
- [ ] **F-JU-06** : aucun arrêt hors-sujet sous le Comparateur d'indemnités
- [ ] **F-258** : encart « outils non calculés » avant génération + « Voir les outils à calculer » + disparition à N=0
- [ ] **F-JU-02** : rubrique « Jurisprudence applicable » dans les conclusions générées (outils calculés)
- [ ] **SF-98-55** : acte généré **sans jargon** (aucun « F-DT-XX », aucun score brut) + visa d'articles + syllogisme + dispositif complet (art. 700, intérêts + capitalisation, astreinte)
- [ ] **F-241** : « Doctrine » / « Lexis+ » / « Lextenso » ouvrent une recherche pré-remplie
- [ ] **F-242** : « + Jurisprudence à l'appui » → Ajouter / Modifier / Supprimer OK
- [ ] **Admin** : signalement visible dans « Audit log » (+ entrées `AUTO_REACTIVATE` si un re-bootstrap a réactivé un mapping — SF-JU-06-04)

> **Second jeu F-179 « grandeur nature »** : `dossier-licenciement-durand/` contient déjà 8 vraies citations (Cass. soc. 06-43.867, 89-42.263, 03-41.237, 95-43.370, 94-44.340, 12-22.660, 17-10.152, 06-43.680).
