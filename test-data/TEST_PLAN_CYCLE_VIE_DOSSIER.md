# Plan de test — Vague « Cycle de vie du dossier » (F-282 → F-286)

> Périmètre : les 5 features livrées le 2026-06-12 (audit `docs/business/audit-workflow-decisionnel.md`).
> Objectif : valider, écran par écran, le comportement nominal + les cas limites + l'isolation.
> Niveau : test manuel pas-à-pas (clic par clic), avec valeurs exactes et résultats attendus précis.

---

## 0. Pré-requis

| Élément | Valeur |
|---|---|
| Environnement | **Staging** — https://staging.legalcase.fr |
| Compte de test | un compte avec un workspace actif (ex. `e2e@legalcase.test`) |
| Compte « tiers » (isolation) | un 2ᵉ compte sur un **autre** workspace |
| Navigateur | Chrome **configuré en français** (important : les champs date natifs suivent la locale du navigateur → `jj/mm/aaaa` sur un Chrome FR ; un navigateur en-US affiche `mm/dd/yyyy` — ce n'est pas un bug applicatif) |
| Données | prévoir 3-4 PDF lisibles (lettre de licenciement, bulletins, contrat) |

**Couverture domaines/pays** : F-282/F-283(vague)/F-284/F-285/F-286 sont **transversales** (s'affichent sur tout dossier). Le **scénario principal ci-dessous** se déroule sur un dossier **droit du travail FR** (cas le plus riche). La **§7** ajoute un **spot-check immigration et famille (FR + BE)** pour prouver l'agnosticité. Réserve connue : les *suggestions* de la frise des phases (F-283) sont en vocabulaire civil FR (cf. §3, cas IMM/BE).

**Convention de notation** : `[ ]` = à cocher. **RA** = Résultat Attendu. Chaque RA est observable à l'écran.

---

## 1. F-285 — Qualification d'entrée (intake guidé) · onglet **Dossier**

**Emplacement** : carte « Qualification d'entrée » tout en haut de l'onglet Dossier (au-dessus de la carte d'identité).

### 1.1 Comportement nominal
1. `[ ]` Créer un dossier **droit du travail / France**, intitulé `[TEST] Licenciement Martin c/ Atlas Logistique`.
2. `[ ]` Onglet **Dossier** → repérer la carte **« Qualification d'entrée »** en tête → cliquer **Modifier**.
3. `[ ]` Renseigner :
   - Type de litige : `Licenciement pour motif personnel contesté`
   - Recevabilité : `Recevable`
   - Prescription : `Risque de forclusion` + date `15/09/2026`
   - Juridiction/compétence : `Conseil de prud'hommes de Lyon`
   - Valeur estimée : `25000` (saisie en €)
   - Note : `Saisine en référé envisagée pour rappel de salaires ; prescription 12 mois (art. L1471-1).`
4. `[ ]` **Enregistrer**.
   - **RA** : badge **« Recevable »** vert serti ; pill ambre **« Risque de forclusion · 15/09/2026 »** ; chips `Licenciement pour motif personnel contesté`, `Conseil de prud'hommes de Lyon`, **`25 000 €`** (formaté, police monospace) ; note grise en bas.
   - **RA** : aucun rechargement de page nécessaire ; la carte reflète immédiatement la saisie.

### 1.2 Caractère NON bloquant
5. `[ ]` Créer un 2ᵉ dossier et **ne PAS** remplir l'intake → uploader directement une pièce.
   - **RA** : l'upload et l'analyse fonctionnent **sans** que l'intake soit rempli (la carte reste à l'état « à qualifier », aucune obstruction).

### 1.3 Valeur & persistance
6. `[ ]` Modifier la valeur à `30000`, enregistrer, **recharger** la page (F5).
   - **RA** : `30 000 €` persisté (stockage en centimes côté serveur, affichage en €).
7. `[ ]` Repasser la recevabilité à `Irrecevable` puis re-`Recevable`.
   - **RA** : le badge change de couleur en conséquence ; la date de 1ʳᵉ qualification (`qualifiedAt`) n'est pas écrasée par les éditions suivantes.

---

## 2. Pièces + analyse (pré-requis pour F-283 vague + F-286 stratégie)

8. `[ ]` Sur le dossier `[TEST] Licenciement Martin`, uploader 3 PDF (lettre de licenciement, bulletin, contrat).
9. `[ ]` Lancer l'analyse → **attendre l'état `DONE`** (asynchrone, quelques minutes ; suivre l'indicateur d'analyse).
   - **RA** : synthèse disponible, outils décisionnels proposés et **pré-remplis** par l'IA.

---

## 3. F-283 — Dossier vivant

### 3.1 Carte « vague de pièces » · onglet **Dossier**
**Emplacement** : carte « vague de pièces » au-dessus du tableau des pièces.

10. `[ ]` **Avant** d'ajouter quoi que ce soit après l'analyse : vérifier l'onglet Dossier.
    - **RA** : la carte « vague de pièces » est **absente** (delta = 0). C'est l'état normal, **pas un bug**.
11. `[ ]` **Après** l'analyse `DONE`, uploader **2 nouvelles pièces** (ex. `attestation-temoin.pdf`, `echange-mail.pdf`).
12. `[ ]` Revenir sur l'onglet Dossier.
    - **RA** : la carte **« vague de pièces »** apparaît avec **« 2 nouvelles pièces depuis la dernière analyse »** + un CTA de ré-analyse.
    - **RA** : cliquer le CTA route vers la ré-analyse **existante** (pas de nouveau pipeline « caché »).
13. `[ ]` Relancer l'analyse → attendre `DONE` → vérifier que le delta **retombe à 0** (carte de nouveau absente).

### 3.2 Frise des phases · onglet **Suivi**
**Emplacement** : frise « Phases du dossier » en **tête** de l'onglet Suivi.

14. `[ ]` Onglet **Suivi** → carte « Phases du dossier » → **Enregistrer une phase** :
    - Phase `Saisine`, libellé `Saisine du Conseil de prud'hommes`, entrée le `10/02/2026`, note `Requête déposée au greffe.`
15. `[ ]` Ajouter `Conciliation`, libellé `Audience BCO`, entrée le `03/04/2026`, note `Échec de la conciliation, renvoi au fond.`
16. `[ ]` Ajouter `Mise en état`, libellé `Mise en état — échange des conclusions`, entrée le `15/05/2026`.
    - **RA** : frise verticale (ligne + pastilles) ordonnée par date ; **dernière pastille pleine navy** ; badge **« EN COURS »** (ambre) sur « Mise en état » ; pill **« Phase courante · Mise en état »** en haut à droite ; dates en monospace `entrée le ../../....` ; icônes edit/delete par étape.
17. `[ ]` **Supprimer** la phase « Conciliation ».
    - **RA** : suppression **directe** (sans pop-up de confirmation — comportement aligné F-282) ; la frise se réordonne ; « Saisine » → « Mise en état ».
18. `[ ]` **Modifier** la date de « Mise en état » à `20/05/2026`.
    - **RA** : réordonnancement correct, badge « EN COURS » conservé sur la plus récente.

> **Cas IMMIGRATION / BELGIQUE (corrigé par SF-283-03)** : les suggestions de phases sont désormais adaptées au domaine du dossier × pays du workspace.
> 19. `[ ]` Sur un dossier **immigration FR**, ouvrir le formulaire d'ajout de phase.
>     - **RA** : le sélecteur propose la **procédure administrative** : *Recours gracieux/hiérarchique → Tribunal administratif → CNDA (asile) → Cour administrative d'appel → Conseil d'État → Exécution* ; sélectionner un type **pré-remplit le libellé** (éditable).
> 20. `[ ]` Sur un dossier **travail BE** (workspace Belgique), idem.
>     - **RA** : *Introduction (citation/requête) → Mise en état (art. 747 C. jud.) → Audience de plaidoiries → Jugement → **Cour du travail** → Cassation → Exécution* (plus de « conciliation BCO »).
> 21. `[ ]` Combinaison non couverte (autre domaine/pays) → **RA** : fallback sur la liste civile FR (8 phases), aucun écran vide.

> ⚠️ La numérotation ci-dessous (§4+) est décalée de +2 après l'ajout des étapes 20-21.

---

## 4. F-284 — Échéancier procédural proactif · onglet **Suivi**

**Emplacement** : carte « Échéancier » en **1re position** de l'onglet Suivi (au-dessus de la frise des phases).

20. `[ ]` Onglet Suivi → carte « Échéancier » → lien **« Gérer les délais »** → créer 3 délais :
    - `Communication des pièces adverses` — échéance `05/06/2026` (**dans le passé** → dépassé)
    - `Dépôt des conclusions récapitulatives (mise en état)` — échéance `22/06/2026`
    - `Audience de plaidoirie au fond` — échéance `14/10/2026`
21. `[ ]` Revenir sur la carte « Échéancier ».
    - **RA** : **hero « prochain couperet »** = le plus urgent = `Communication des pièces adverses`, affiché **« J+7 (dépassé) »** en **rouge** + sous-ligne `DÉLAI 05/06/2026` (monospace).
    - **RA** : pill **« 1 en retard »** en haut à droite.
    - **RA** : sous le hero, **liste des échéances suivantes** triées par urgence, chacune avec un **J-XX** (monospace) et la date à droite ; puces colorées par urgence.
    - **RA** : CTA discret **« Gérer les délais »** renvoyant vers la liste détaillée F-69.
22. `[ ]` **Synergie F-282** : après avoir créé un round contradictoire avec échéance de réponse (cf. §5), revenir ici.
    - **RA** : l'échéance de réponse du round **apparaît aussi** dans l'échéancier (agrégation `case_deadlines` + `contradictoire_rounds`).
23. `[ ]` Supprimer tous les délais.
    - **RA** : la carte affiche un **état vide soigné** (pas de hero rouge, message neutre).

---

## 5. F-282 — Cycle contradictoire (frise des rounds) · onglet **Suivi**

**Emplacement** : frise « Cycle contradictoire » sous la frise des phases.

24. `[ ]` Onglet Suivi → carte « Cycle contradictoire » → **Enregistrer un échange** :
    - Round 1 : Partie **Nous**, libellé `Requête de saisine valant conclusions`, date `20/05/2026`.
25. `[ ]` Ajouter Round 2 : Partie **Partie adverse**, libellé `Conclusions en réponse de l'employeur`, date `18/06/2026`, **échéance de réponse** `18/07/2026`.
    - **RA** : frise verticale ; Round 1 badge gris-bleu **« NOUS »** + pastille navy ; Round 2 badge ambre **« PARTIE ADVERSE »** + pastille ambre + ligne ambre **« réponse avant 18/07/2026 »** (icône calendrier).
    - **RA** : pill résumé en haut **« Round 2 · À vous · échéance 18/07/2026 »**.
    - **RA** : nœud terminal **« À vous de répondre »** + bouton navy **« Générer ma réplique »**.
26. `[ ]` Cliquer **« Générer ma réplique »**.
    - **RA** : redirige vers l'onglet **Décision** (génération de conclusions au jeu adverse du dernier round).
27. `[ ]` Ajouter un Round 3 **Nous** (réplique déposée).
    - **RA** : la pill résumé bascule sur **« En attente de la partie adverse »** (ce n'est plus à nous).
28. `[ ]` **Format de date** : ouvrir le formulaire d'ajout, observer les champs date.
    - **RA** (Chrome FR) : `jj/mm/aaaa`. (Chrome en-US : `mm/dd/yyyy` — limite navigateur, pas applicative.)

---

## 6. F-286 — Stratégie de dossier unifiée · onglet **Décision**

**Emplacement** : carte **« Stratégie de dossier »** en coiffe de la **colonne verdict (droite)**, au-dessus du tableau de bord décisionnel.

### 6.1 Cas vide (EMPTY_INPUT) — à tester EN PREMIER
29. `[ ]` Sur un dossier **sans aucun outil décisionnel calculé**, onglet Décision → carte « Stratégie de dossier ».
    - **RA** : **état `EMPTY_INPUT` soigné** : message explicite type « calcule d'abord tes outils décisionnels » ; **aucune** reco inventée ; **aucun** appel IA déclenché.

### 6.2 Cas nominal
30. `[ ]` Sur `[TEST] Licenciement Martin`, onglet Décision → **calculer au moins 2 outils** (ex. `Indemnité de licenciement`, `Rappel de salaire`) — valider les valeurs pré-remplies par l'IA.
31. `[ ]` Carte « Stratégie de dossier » → générer la stratégie.
    - **RA** : reco en markdown structuré avec EXACTEMENT ces sections : **## Voie procédurale** (référé vs fond), **## Posture** (concilier/transiger vs plaider), **## Priorisation des chefs de demande** (liste à puces, un chef = une demande), **## Séquencement** (prochaines actions).
    - **RA** : la reco **reprend les verdicts calculés** (montants/forces) sans les recalculer ni les contredire ; **aucun jargon interne** (pas de « F-DT-08 », pas de nom d'outil technique) ; pas de jurisprudence inventée.
    - **RA visuel** : carte « document » (≤ 760px), titre navy + liseré or, sections h2/h3 stylées, listes à puces or, états chargement (spinner) / erreur (snackbar) soignés.

### 6.3 Garde-fou invariant « 1 outil = 1 situation »
32. `[ ]` Après génération de la stratégie, retourner au **tableau de bord décisionnel**.
    - **RA** : **aucun outil n'a été modifié, déplacé, masqué ni réordonné** par la stratégie. Les valeurs/positions des outils sont identiques à l'avant-génération. (La stratégie est une **lecture**, jamais une mutation.)
33. `[ ]` Régénérer la stratégie une 2ᵉ fois.
    - **RA** : **1 seule ligne** en base par dossier (upsert) — la nouvelle reco remplace l'ancienne, pas d'empilement.

---

## 7. Spot-check multi-domaines / multi-pays (agnosticité)

> Objectif : prouver que les features transversales s'affichent et fonctionnent hors travail FR.

34. `[ ]` Créer un dossier **droit des étrangers / France** (ex. contestation OQTF).
    - **RA** : carte **Intake** (F-285), **Échéancier** (F-284), **Cycle contradictoire** (F-282), **Stratégie** (F-286) présentes et fonctionnelles. Stratégie : le prompt mentionne « droit des étrangers » → reco cohérente.
35. `[ ]` Créer un dossier **droit de la famille / France** (ex. divorce).
    - **RA** : idem ; intake `valeur estimée` pertinente, stratégie cohérente (famille).
36. `[ ]` Créer un dossier **droit du travail / Belgique**.
    - **RA** : toutes les cartes présentes ; échéancier/intake/contradictoire/stratégie OK ; phases utilisables via libellé libre (réserve §3.19).

---

## 8. Tests transverses (sécurité / isolation)

37. `[ ]` Avec le **2ᵉ compte (autre workspace)**, tenter d'accéder à l'URL d'un dossier du 1ᵉʳ compte.
    - **RA** : **404** (pas de fuite). Vrai pour les endpoints des 5 features (contradictoire, phases, intake, échéancier, stratégie).
38. `[ ]` Non connecté, appeler un endpoint (ex. `GET /api/v1/case-files/{id}/echeancier`).
    - **RA** : **401**.

---

## 9. Checklist de validation finale

- `[ ]` F-285 Intake : verdict + pills + valeur €, non bloquant, persistant.
- `[ ]` F-283 Vague de pièces : apparaît sur delta > 0, absente sinon, retombe à 0 après ré-analyse.
- `[ ]` F-283 Phases : frise ordonnée, phase courante, edit/delete, (réserve vocabulaire IMM/BE notée).
- `[ ]` F-284 Échéancier : hero couperet, retards, J-XX, synergie rounds, état vide.
- `[ ]` F-282 Contradictoire : rounds NOUS/ADVERSE, à-qui-le-tour, échéance, « Générer ma réplique ».
- `[ ]` F-286 Stratégie : EMPTY_INPUT honnête, reco structurée sur outils calculés, **invariant outils intact**, upsert.
- `[ ]` Multi-domaines : travail/immigration/famille FR + travail BE OK.
- `[ ]` Isolation : 404 cross-workspace, 401 non authentifié.

---

### Annexe — Référence technique (pour debug)
| Feature | Endpoint(s) | Table |
|---|---|---|
| F-282 | `/api/v1/case-files/{id}/contradictoire-rounds` (GET/POST/PUT/DELETE) | `contradictoire_rounds` |
| F-283 phases | `/api/v1/case-files/{id}/phases` (GET/POST/PUT/DELETE) | `case_phases` |
| F-283 vague | `/api/v1/case-files/{id}/pieces-wave` (GET, lecture seule) | — (dérivé `analysis_jobs`+`documents`) |
| F-284 | `/api/v1/case-files/{id}/echeancier` (GET, lecture seule) | — (agrège `case_deadlines`+`contradictoire_rounds`) |
| F-285 | `/api/v1/case-files/{id}/intake` (GET/PUT) | colonnes `intake_*` sur `case_files` |
| F-286 | `/api/v1/case-files/{id}/strategy` (GET/POST) | `case_strategy` |

> Vérifier les chemins exacts dans les `*Controller.java` si un appel échoue (certains peuvent différer légèrement).
