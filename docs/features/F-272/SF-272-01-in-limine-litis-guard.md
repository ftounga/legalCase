# SF-272-01 — Garde d'ordre *in limine litis* pour les conclusions du défendeur (FR)

> Feature parente : **F-272** (Conclusions V4 ② — Moyens de procédure systématiques & ordre *in limine litis*, art. 74 CPC).
> Étape 0 : `SF-272-00-coherence.md` — verdict **GO avec ajustements**.
> Label risque : 🟠 (choix UX/produit réversible — texte de prompt conditionné). Décision par défaut tracée.

## Objectif (une phrase)

Imposer, **uniquement pour les conclusions du défendeur en procédure FR**, l'ossature **exceptions de procédure → fins de non-recevoir → défense au fond** (art. 74 CPC), via une garde de prompt transverse réutilisant le mécanisme `REDACTION_QUALITY_GUARD` existant, sans nouveau provider, intrant, table, endpoint ni écran.

## Comportement nominal

- `CaseConclusionPromptBuilder.buildSystemPrompt(key, styleSignatures)` injecte, **après** `REDACTION_QUALITY_GUARD` et **avant** la consigne de style, un bloc `PROCEDURE_ORDER_GUARD` **si et seulement si** :
  - `key.country() == ProcedureStageCatalog.FRANCE`, ET
  - `key.position() ∈ { "DEFENDEUR", "INTIME", "DEFENDEUR_POURVOI" }`.
- Le bloc impose l'ordre des sections et le *in limine litis* (art. 73-74 CPC pour les exceptions de procédure, art. 122 CPC pour les fins de non-recevoir, prescription), demande de **tisser** les vices de procédure / nullités déjà fournis par les outils décisionnels (sans citer leur code), et précise de **n'ajouter ces sections que si une exception/FNR est réellement fondée** par les faits, pièces ou verdicts fournis (sinon pas de rubrique vide).

## Cas d'erreur / hors-cible (no-op)

- Cellule **demandeur** (DEMANDEUR, APPELANT, REQUERANT, DEMANDEUR_TITRE, DEMANDEUR_POURVOI) → bloc **absent**.
- Cellule **BE** (country ≠ FRANCE) → bloc **absent** (le Code judiciaire belge a son propre régime ; hors périmètre directive « 3 domaines FR »).
- Combinaison inconnue → comportement inchangé (l'exception `IllegalStateException` du registre est levée avant, comme aujourd'hui).

## Critères d'acceptation vérifiables

1. `buildSystemPrompt` pour CPH/FOND/**DEFENDEUR** FR contient « in limine litis », « exceptions de procédure », « fins de non-recevoir », « article 74 ».
2. `buildSystemPrompt` pour CPH/FOND/**DEMANDEUR** FR **ne contient pas** « in limine litis ».
3. La garde s'applique aussi à **INTIME** (appel) et **DEFENDEUR_POURVOI** (cassation) FR.
4. Une cellule **BE défendeur** (ex. tribunal du travail BE, TtFondDefendeur) **ne contient pas** « in limine litis ».
5. Non-régression SF-98-55 : le prompt défendeur contient toujours `REDACTION_QUALITY_GUARD` (anti-jargon, syllogisme) ET le nouveau bloc ; la garde n'expose aucun code d'outil (« F-DT-36 »).
6. Non-régression style : l'ordre reste `base → JURISPRUDENCE_GUARD → REDACTION_QUALITY_GUARD → [PROCEDURE_ORDER_GUARD si défendeur FR] → style`. Pour un demandeur sans style, le prompt est inchangé par rapport à master (octet pour octet sur la partie non-défendeur — vérifié par l'absence de la chaîne).

## Plan de test minimal

Tests unitaires dans `CaseConclusionPromptBuilderTest` (registre élargi aux providers défendeur/intimé/pourvoi + 1 BE) :
- `buildSystemPrompt_defendeurFr_containsInLimineLitisGuard`
- `buildSystemPrompt_demandeurFr_omitsInLimineLitisGuard`
- `buildSystemPrompt_intimeFr_containsInLimineLitisGuard`
- `buildSystemPrompt_defendeurPourvoiFr_containsInLimineLitisGuard`
- `buildSystemPrompt_defendeurBe_omitsInLimineLitisGuard`
- `buildSystemPrompt_defendeurFr_stillContainsRedactionQualityGuard` (non-régression SF-98-55)
- `buildSystemPrompt_inLimineLitisGuard_doesNotLeakToolCode` (pas de « F-DT-36 »)

Isolation workspace : N/A (assemblage de texte pur, aucune lecture DB / aucun `workspace_id`).

## Tables / endpoints / composants impactés

- **Backend** : `CaseConclusionPromptBuilder.java` (nouvelle constante `PROCEDURE_ORDER_GUARD` + condition dans `buildSystemPrompt`). Helper privé `appliesProcedureOrderGuard(CombinationKey)`.
- **Tests** : `CaseConclusionPromptBuilderTest.java`.
- **Tables** : aucune. **Endpoints** : aucun. **Frontend** : aucun. **Migration Liquibase** : aucune.

## Hors périmètre

- Belgique (régime du Code judiciaire distinct).
- Conclusions du demandeur (pas d'exception in limine litis à sa charge).
- Création d'un outil décisionnel « exceptions de procédure » (interdit : un outil = une situation ; ici garde de prompt).
- Nouvel intrant d'extraction des exceptions/nullités (réutilise les verdicts d'outils déjà transmis).
- Restructuration des providers de cellule (la garde s'applique par-dessus).

## Préoccupations transversales

- Auth / Principal : non. Workspace context : non. Plans/limites : non. Navigation/routing : non.
- **Outil décisionnel métier** : F-272 **n'est pas** un outil — c'est une garde de prompt. Aucun `decision_tool_visibility_rules`, aucun `TOOL_REGISTRY`. L'invariant « un outil = une situation » n'est pas touché. → pas de smoke E2E requis (aucune préoccupation transversale cochée).
