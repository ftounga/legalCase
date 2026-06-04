# Mini-spec — F-JU-06 / SF-JU-06-01 Évaluateur durci + 2ᵉ passe de pertinence

## Identifiant
`F-JU-06 / SF-JU-06-01`

## Feature parente
`F-JU-06` — qualité des citations jurisprudence (ré-évaluation outillée). Cadrage cohérence GO : `docs/features/F-JU-06/SF-JU-06-00-coherence.md`.

## Statut
`ready`

## Date de création
2026-06-03

## Branche Git
`feat/SF-JU-06-01-evaluateur-durci`

---

## Objectif (une phrase)
Empêcher, **à la source**, qu'un arrêt hors-sujet ou sans chapeau soit mappé à un outil décisionnel, en durcissant le prompt de l'évaluateur et en ajoutant une **2ᵉ passe de pertinence sémantique** + des rejets déterministes (chapeau vide, confiance sous seuil).

## Contexte
Diagnostic 2026-06-03 (comparateur F-DT-09) : l'évaluateur a retenu un arrêt « restauration ferroviaire » (prime d'ancienneté) à confiance 0,72 + un arrêt à chapeau vide. Causes dans le code actuel :
- `ClaudeJurisprudenceEvaluator` (mode bootstrap) : le prompt impose « ADD ou NONE », encourage à choisir « l'arrêt le moins éloigné » et qualifie NONE de « très exceptionnel » → biais pro-ADD, même pour un arrêt marginal (exemple du prompt : ADD à 0,40).
- `JurisprudenceBootstrapService` ligne ~290 : `setChapeauOfficiel(chosen.chapeauOfficiel() == null ? "" : …)` → stocke un chapeau vide au lieu de rejeter.
- Aucun seuil de confiance minimal à l'insertion, aucune vérification que l'arrêt porte réellement sur le sujet de l'outil.

## Comportement nominal
Lors de l'évaluation d'un couple (outil × branche) avec des candidats JUDILIBRE :
1. **Évaluateur durci** : le prompt n'impose plus de choisir un arrêt « par défaut ». Il sélectionne un arrêt **uniquement** s'il porte réellement sur le sujet de l'outil ; sinon il renvoie `NONE`. Le biais est inversé : en cas de doute → `NONE`.
2. **2ᵉ passe de pertinence** : si l'évaluateur a choisi un arrêt (ADD), un appel LLM **dédié** confronte la *situation métier de l'outil* (description de l'entrée bootstrap) au *contenu réel de l'arrêt* (chapeau/sommaire) et répond `{pertinent: true|false, raison}`. Si `false` → l'arrêt est **rejeté** (aucun mapping inséré).
3. **Rejets déterministes** avant insertion :
   - chapeau de l'arrêt choisi **null/vide/blanc** → rejet ;
   - `confidence_score < MIN_BOOTSTRAP_CONFIDENCE` (0,70) → rejet.
4. Si tout est passé → mapping inséré (chapeau garanti non vide, confiance ≥ 0,70, pertinence confirmée).

> Invariant « silence > erreur » : un couple (outil × branche) sans arrêt valide reste **sans mapping** (aucune citation), jamais avec un arrêt forcé.

## Cas d'erreur / bords
- 0 candidat JUDILIBRE → `NONE`, pas de mapping (inchangé).
- 2ᵉ passe LLM échoue (timeout/parse) → **rejet par défaut** (silence > erreur), audit log le note.
- Tous les candidats rejetés par la 2ᵉ passe → pas de mapping (résultat attendu pour les outils sans jurisprudence Cassation pertinente).
- Appel LLM de la 2ᵉ passe : **gaté** via `AiCallContext` / `AnthropicService` (cf. gate Anthropic obligatoire) avec un `JobType` approprié.

## Solution technique
1. **`ClaudeJurisprudenceEvaluator`** — réécrire le bloc prompt bootstrap : retirer « choisis l'arrêt le moins éloigné / NONE très exceptionnel » ; instruire « ne choisis un arrêt que s'il fonde réellement la situation de l'outil, sinon NONE ». Conserver le format de sortie JSON.
2. **Nouvelle 2ᵉ passe** — méthode dédiée (p. ex. `JurisprudenceRelevanceGate.assess(toolDescription, branche, arret)`) faisant un appel `AnthropicService` gaté, prompt focalisé « cet arrêt fonde-t-il un outil qui traite de : <description> ? Réponds pertinent true/false + raison ». Sortie structurée. Modèle léger (Haiku) suffisant — à trancher au dev.
3. **`JurisprudenceBootstrapService`** — avant `mappingRepository.save` : (a) rejeter si chapeau vide/blanc ; (b) rejeter si `confidence < 0,70` ; (c) appeler la 2ᵉ passe et rejeter si non pertinent. Chaque rejet → audit log dédié (`AUTO_REJECT` ou équivalent) avec la raison, pour le rapport de mesure (SF-03).
4. Constante `MIN_BOOTSTRAP_CONFIDENCE = 0,70` (≥ seuil d'affichage 0,60 de SF-JU-01-FIX).

## Critères d'acceptation (vérifiables)
1. Un candidat dont le chapeau est vide/blanc n'est jamais inséré (test).
2. Un candidat à confiance < 0,70 n'est jamais inséré (test).
3. Un candidat jugé non pertinent par la 2ᵉ passe n'est jamais inséré, même si confiance ≥ 0,70 (test — reproduit le cas « restauration ferroviaire »).
4. Un candidat pertinent, chapeau plein, confiance ≥ 0,70 est inséré normalement.
5. La 2ᵉ passe passe par le gate Anthropic (record d'usage présent).
6. Build + tests verts ; aucun mapping existant supprimé par cette SF (la correction des données = SF-03 re-bootstrap).

## Plan de test minimal
- **Unitaire `ClaudeJurisprudenceEvaluator`** : prompt durci → NONE quand candidats hors-sujet (réponse LLM mockée).
- **Unitaire 2ᵉ passe** (`JurisprudenceRelevanceGate`) : pertinent=false → rejet ; true → accepté (AnthropicService mocké).
- **IT `JurisprudenceBootstrapService`** : jeux de candidats (chapeau vide / confiance 0,55 / hors-sujet pertinent=false / valide) → seul le valide produit un mapping.
- **Gate** : vérifier l'appel gaté de la 2ᵉ passe (record usage).
- **Isolation workspace** : N/A (donnée globale).

## Tables / endpoints / composants impactés
- **Backend** : `ClaudeJurisprudenceEvaluator` (prompt), nouveau `JurisprudenceRelevanceGate` (2ᵉ passe), `JurisprudenceBootstrapService` (rejets + audit), constante seuil. `AnthropicService` (appel gaté).
- **Table** : `tool_jurisprudence_mappings` (écriture — moins d'insertions), `jurisprudence_audit_log` (nouveaux rejets tracés). Aucune migration de schéma a priori (vérifier si `AUTO_REJECT` doit être ajouté à l'enum d'action audit → micro-migration si enum en DB).
- **Pas de frontend.**

### Préoccupation transversale : **Outil décisionnel métier** + **gate Anthropic**
- Outil décisionnel : améliore la fiabilité des citations affichées (F-JU-01) et citées en conclusions (F-JU-02) — point amont commun.
- Gate Anthropic : la 2ᵉ passe est un nouvel appel LLM → **doit** passer par `AiCallContext`/`AnthropicService` (record obligatoire), cf. centralisation F-257. Pas d'IA synchrone côté requête utilisateur (tout est dans le job de bootstrap async).

## Hors périmètre
- **SF-JU-06-02** : requêtes JUDILIBRE ciblées (réduire le bruit en amont).
- **SF-JU-06-03** : re-bootstrap effectif des 121 outils FR + rapport de mesure (rétention, % chapeaux pleins). **Cette SF-01 ne relance pas le bootstrap** ; elle outille le pipeline. Les mappings douteux existants restent jusqu'à SF-03 (le filtre SF-JU-01-FIX masque déjà les pires : vide + confiance < 0,60).
- Curation manuelle (exclue par décision PO).
- Sources non-Cassation (F-JU-04 BE, F-JU-05 administratif).
