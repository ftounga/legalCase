# SF-273-01 — Garde « sauf à parfaire » des montants & intérêts (dispositif)

> Feature parente : **F-273** (Conclusions V4 ③ — Actualisation « sauf à parfaire » des montants & intérêts).
> Étape 0 : `SF-273-00-coherence.md` — verdict **GO avec ajustements**.
> Label risque : 🟠 (choix UX/produit réversible — texte de prompt). Décision par défaut tracée.

## Objectif (une phrase)

Imposer, dans le dispositif de **toutes** les conclusions (portée uniforme), la mention **« sauf à parfaire à la date de l'audience »** sur les chefs chiffrés réellement évolutifs et la **fixation du point de départ des intérêts**, en enrichissant le **point 3 (Dispositif complet)** de la garde transverse `REDACTION_QUALITY_GUARD` existante — sans nouveau provider, intrant, table, endpoint ni écran.

## Comportement nominal

- `CaseConclusionPromptBuilder.REDACTION_QUALITY_GUARD` voit son **point 3** complété : après la liste des postes systématiques (art. 700, dépens, exécution provisoire, intérêts, astreinte), il ajoute la consigne d'actualisation « sauf à parfaire » :
  - assortir d'une réserve **« sauf à parfaire à la date de l'audience »** (ou « sauf mémoire ») les sommes qui **continuent d'évoluer** : rappels de salaire et indemnités fonction du temps écoulé ou du salaire, intérêts ;
  - **préciser le point de départ des intérêts** par chef (mise en demeure, saisine, ou décision selon la nature de la créance — art. 1231-6 et 1231-7 du Code civil) ;
  - **ne pas** apposer « sauf à parfaire » sur un montant **définitivement arrêté** (préjudice forfaitaire, somme liquide non évolutive) ;
  - **ne pas** inventer de date ni de montant non fondé par le dossier (réserve qualitative ; aucun recalcul).
- La consigne fait partie de la garde commune → présente sur **chaque** prompt système (demandeur comme défendeur, 3 domaines FR), comme les points 1-10 actuels. Aucune condition de position/pays.

## Cas d'erreur / hors-cible (no-op)

- Aucun chef évolutif au dossier → la garde demande explicitement de **ne pas** plaquer « sauf à parfaire » sur les montants arrêtés (pas de mention parasite). Comportement à la charge du modèle, cadré par le texte.
- Combinaison de cellule inconnue → comportement inchangé (`IllegalStateException` du registre levée avant, comme aujourd'hui).
- Aucune branche conditionnelle nouvelle en Java : c'est une extension de **constante texte** ; aucun risque d'exception ajouté.

## Critères d'acceptation vérifiables

1. `buildSystemPrompt` (n'importe quelle cellule) contient « sauf à parfaire ».
2. Le prompt contient la consigne de **point de départ des intérêts** visant les art. 1231-6 / 1231-7 du Code civil.
3. Le prompt précise de **ne pas** apposer la réserve sur un montant **définitivement arrêté**.
4. Non-régression SF-98-55 : le point 3 contient toujours « article 700 », « 1343-2 » (capitalisation) ; les points 1-10 et les gardes F-242/F-272 restent présents.
5. Anti-jargon (non-régression) : la garde « sauf à parfaire » n'expose aucun code d'outil (« F-DT- ») ni score brut.
6. Portée uniforme : la mention « sauf à parfaire » est présente aussi bien pour DEMANDEUR FR que pour DEFENDEUR FR (pas de conditionnement).

## Plan de test minimal

Tests unitaires ajoutés dans `CaseConclusionPromptBuilderTest` :
- `buildSystemPrompt_containsSaufAParfaireGuard` — présence « sauf à parfaire » + point de départ intérêts (1231-6/1231-7) + interdiction sur montant arrêté.
- `buildSystemPrompt_saufAParfaire_doesNotRegressDispositifPostes` — non-régression point 3 (« article 700 », « 1343-2 »).
- `buildSystemPrompt_saufAParfaire_uniformOnDemandeurAndDefendeur` — présence sur DEMANDEUR_KEY ET DEFENDEUR_FR_KEY (portée uniforme).
- `buildSystemPrompt_saufAParfaireGuard_doesNotLeakToolCode` — la constante ne contient pas « F-DT- ».

Isolation workspace : N/A (assemblage de texte pur, aucune lecture DB / aucun `workspace_id`).

## Tables / endpoints / composants impactés

- **Backend** : `CaseConclusionPromptBuilder.java` — extension de la constante `REDACTION_QUALITY_GUARD` (point 3). Aucune nouvelle méthode, aucune nouvelle branche.
- **Tests** : `CaseConclusionPromptBuilderTest.java`.
- **Tables** : aucune. **Endpoints** : aucun. **Frontend** : aucun. **Migration Liquibase** : aucune.

## Hors périmètre

- Calcul automatique des montants actualisés à la date d'audience (F-273 = mention rédactionnelle, pas un calculateur ; un outil = une situation).
- Saisie par l'avocat d'une date d'audience cible (aucun nouvel intrant).
- Modification des providers de cellule (la garde s'applique par-dessus, transverse).
- Belgique : la formulation « sauf à parfaire » est une garantie de complétude du dispositif compatible toutes cellules ; aucune spécificité BE n'est ajoutée ni retirée ici (portée uniforme assumée, conforme PRODUCT_SPEC).

## Préoccupations transversales

- Auth / Principal : non. Workspace context : non. Plans/limites : non. Navigation/routing : non.
- **Outil décisionnel métier** : F-273 **n'est pas** un outil — c'est une garde de prompt. Aucun `decision_tool_visibility_rules`, aucun `TOOL_REGISTRY`. L'invariant « un outil = une situation » n'est pas touché. → pas de smoke E2E requis (aucune préoccupation transversale cochée).
