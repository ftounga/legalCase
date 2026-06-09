# Mini-spec — F-98 / SF-98-55 Garde de qualité rédactionnelle commune (anti-jargon + dispositif + syllogisme)

## Identifiant
`F-98 / SF-98-55`

## Feature parente
`F-98` (Génération de conclusions). SF de durcissement de la qualité rédactionnelle — bugfix/évolution interne du prompt, **pas de nouvel écran ni workflow** → exemptée des étapes 0 / 0 bis.

## Statut
`ready`

## Branche Git
`feat/SF-98-55-garde-qualite-redactionnelle`

## Objectif (une phrase)
Garantir, sur **les 45 cellules** de génération, des conclusions de qualité professionnelle — **sans jargon interne LegalCase**, avec **dispositif complet**, **syllogisme** et **visa des articles** — en enrichissant la **garde commune** du prompt système (mécanisme `JURISPRUDENCE_GUARD`), sans toucher aux 45 trames individuelles.

## Problème (audit du rendu réel — dossier LEMAIRE, 2026-06-09)
1. **🔴 Jargon interne dans l'acte** : le rendu cite *« l'outil décisionnel de validité du licenciement (F-DT-08) »*, *« 2 critères sur 7 non conformes »*, *« niveau de risque ÉLEVÉ avec 2 sanctions cumulables »*. Sources confirmées : le `toolId` brut injecté (`CaseConclusionPromptBuilder` l.191 `"Outil : " + entry.toolId()`) et les `label`/`primaryValue`/`secondaryValue` des tiles (section « VERDICTS DES OUTILS »). Un acte déposé ne doit **jamais** exposer un code produit ni un score d'outil.
2. **Dispositif non garanti** : le modèle a produit art. 700 / dépens / exécution provisoire / astreinte **spontanément**, mais a **oublié les intérêts légaux + capitalisation** — la trame (12 lignes) ne les impose pas → aléatoire selon la cellule/le dossier.
3. **Syllogisme & visa des articles** réussis « par chance », non garantis par la trame.
4. **Rappel procédural** des faits non cadré.

## Comportement nominal
`CaseConclusionPromptBuilder.buildSystemPrompt` ajoute, après `JURISPRUDENCE_GUARD`, une **garde de qualité rédactionnelle commune** (constante `REDACTION_QUALITY_GUARD`) imposant :
- **Anti-jargon** : ne jamais mentionner les outils internes de LegalCase, leurs **codes** (ex. « F-DT-08 ») ni leurs **scores bruts** (ex. « 2 critères sur 7 », « niveau ÉLEVÉ ») ; ces éléments sont une **matière première interne** à traduire en moyens et arguments de droit.
- **Syllogisme** : chaque moyen = règle de droit **avec visa de l'article applicable** → application aux faits → pièce(s) → conséquence juridique.
- **Dispositif complet** (quand applicable au stade/à la juridiction) : reprendre les chefs chiffrés, **+ article 700, dépens, exécution provisoire, intérêts au taux légal et capitalisation (art. 1343-2 C. civ.), astreinte** sur la remise des documents.
- **Faits et procédure** : exposer la **chronologie** et rappeler le cadre procédural.

En complément (défense en profondeur), `appendToolJurisprudenceCitations` n'expose plus le `toolId` **brut** : il est remplacé par un **libellé lisible** dérivé (`humanizeToolId`, ex. `f-dt-08-licenciement-validite` → « Licenciement validité »).

## Cas d'erreur / bords
- Dossier **sans** outil ni jurisprudence → la garde n'impose rien d'incohérent (instructions conditionnelles « quand applicable ») ; aucune section vide forcée.
- La garde s'ajoute **sans modifier** les 45 trames ni la `JURISPRUDENCE_GUARD` existante (non-régression).
- `humanizeToolId` sur un `toolId` null/vide → chaîne vide, pas d'exception.

## Solution technique (backend uniquement, **pas de migration**)
1. **`CaseConclusionPromptBuilder`** : nouvelle constante `REDACTION_QUALITY_GUARD` (texte ci-dessus) ; l'ajouter dans `buildSystemPrompt` juste après l'append de `JURISPRUDENCE_GUARD`.
2. **`CaseConclusionPromptBuilder`** : méthode `humanizeToolId(String toolId)` (retire le préfixe `f-xx-NN-`, remplace les tirets par des espaces, met une capitale) ; l'utiliser l.191 à la place du `toolId` brut.
3. Section « VERDICTS DES OUTILS » : `label` conservé (lisible) ; les `primaryValue`/`secondaryValue` restent fournis comme matière interne — la garde anti-jargon interdit leur citation littérale.

## Critères d'acceptation (vérifiables)
1. Pour **toute** cellule, `buildSystemPrompt(key, [])` contient la garde de qualité (marqueurs : interdiction du jargon outil, exigence de visa d'article, dispositif art. 700/dépens/exécution provisoire/intérêts). (test)
2. `buildUserMessage` n'expose plus de `toolId` **brut** (`f-dt-08…`) dans la section jurisprudence — un libellé lisible apparaît à la place. (test)
3. `humanizeToolId('f-dt-08-licenciement-validite')` = « Licenciement validité » ; null/vide → vide. (test)
4. Non-régression : `JURISPRUDENCE_GUARD` et les 45 `PromptProvider` restent intacts (suite verte).
5. **Validation manuelle staging** : régénérer LEMAIRE → plus aucun « F-DT-08 » ni « 2 critères sur 7 » dans le texte ; dispositif incluant intérêts légaux + capitalisation.

## Plan de test minimal
- **`CaseConclusionPromptBuilderTest`** : (a) la garde qualité est présente dans `buildSystemPrompt` ; (b) `buildUserMessage` avec une citation par outil ne contient pas le `toolId` brut mais son libellé ; (c) `humanizeToolId` (cas nominal + null/vide).
- **Non-régression** : suite `*PromptProviderTest` (45) + `CaseConclusionPromptBuilderTest` existante vertes.
- **Isolation workspace** : N/A (assemblage de prompt, pas d'accès données).

## Tables / endpoints / composants impactés
- **Backend** : `CaseConclusionPromptBuilder` (garde commune + `humanizeToolId`). Aucune trame individuelle modifiée, aucun endpoint, **aucune migration**.
- **Frontend** : aucun.

### Préoccupation transversale : **Génération de conclusions (45 cellules)**
La garde commune s'applique à **toutes** les cellules (Travail/Immigration/Famille × FR/BE × juridictions × stades × positions). Vérifier que la non-régression couvre l'ensemble via la suite `*PromptProviderTest`. Pas d'auth/workspace/navigation/plan impacté. Pas d'outil décisionnel modifié.

## Hors périmètre
- **F-179 → conclusions** (injecter les citations adverses détectées pour réfutation) : **SF distincte** (SF-98-56 envisagée) — plus lourde (nouvel intrant dans le builder).
- Refonte des **45 trames individuelles** (on agit sur la garde commune, pas trame par trame).
- Garanties non vérifiables automatiquement (qualité de sortie LLM) : validées **manuellement** en staging, non par test unitaire.
- Numérotation des pièces / bordereau structuré (#2 de l'audit rendu) : relève des **manques fonctionnels (Phase 3)**.
