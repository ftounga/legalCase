# SF-273-00 — Cadrage cohérence — F-273 Actualisation « sauf à parfaire » des montants & intérêts

> Étape 0 (CLAUDE.md). Feature transverse de garde de prompt, programme « Conclusions V4 ».
> Verdict : **GO avec ajustements**.

## 1. Workflow métier réel de l'avocat cible

L'avocat (demandeur comme défendeur reconventionnel) prépare le **dispositif** (« PAR CES MOTIFS ») de ses conclusions. Il y chiffre des **chefs de demande** : rappels de salaire, indemnités, dommages-intérêts, et leurs **intérêts**.

Problème métier (manque #3 de l'audit conclusions 2026-06-12) : entre la rédaction et l'**audience**, certains montants **continuent d'évoluer** :
- les **rappels de salaire / indemnités fonction du salaire** s'accumulent tant que la relation n'est pas définitivement liquidée ;
- les **intérêts au taux légal** courent jusqu'au paiement (ou jusqu'à la décision).

Un dispositif sérieux demande donc ces sommes **« sauf à parfaire à la date de l'audience »** (ou « sauf mémoire »), et fixe le **point de départ des intérêts** (mise en demeure, saisine, décision selon le chef — art. 1231-6 et 1231-7 du Code civil). À défaut, les chiffres sont **figés** au jour de la rédaction et l'avocat se prive d'une partie de ce qui lui est dû, ou doit ré-éditer manuellement à la veille de l'audience.

Aujourd'hui : rien dans le prompt n'impose la mention « sauf à parfaire » ni la fixation du point de départ des intérêts → chiffres figés.

## 2. Cartographie des features existantes

| Brique existante | Rôle | Lien F-273 |
|---|---|---|
| `CaseConclusionPromptBuilder.REDACTION_QUALITY_GUARD` **point 3 (Dispositif complet)** (SF-98-55) | impose déjà la reprise des **chefs chiffrés** et des **postes systématiques** du dispositif : art. 700 CPC, dépens, exécution provisoire, **intérêts au taux légal et leur capitalisation (art. 1343-2 C. civ.)**, astreinte | **point d'ancrage exact** — F-273 **complète ce point 3** avec « sauf à parfaire » + **point de départ** des intérêts. Le sujet « intérêts » y est déjà ; F-273 ne crée pas de doublon, il le durcit. |
| `JURISPRUDENCE_GUARD` / `PROCEDURE_ORDER_GUARD` (F-242 / F-272) | autres gardes transverses appliquées par `buildSystemPrompt` | pattern de référence (garde transverse, jamais dupliquée provider par provider) |
| Outils de chiffrage (indemnités, comparateurs F-DT-09…) | calculent des montants à une date donnée | leurs verdicts arrivent déjà dans `=== VERDICTS DES OUTILS DÉCISIONNELS REMPLIS ===`. F-273 demande au prompt de **mentionner « sauf à parfaire »** sur les chefs concernés, sans nouvel intrant ni recalcul. |

## 3. Challenge de cohérence

**Amont** — les pré-requis existent-ils ?
- Les chefs chiffrés et le bloc dispositif sont déjà construits (REDACTION_QUALITY_GUARD point 3). ✅
- Les intérêts sont déjà un poste systématique imposé. ✅ F-273 ajoute « sauf à parfaire » + point de départ : aucune nouvelle donnée, aucun recalcul.
- Aucun nouvel intrant, aucune nouvelle table, aucun endpoint. ✅

**Aval** — la sortie est-elle exploitable ?
- La sortie est le texte des conclusions, déjà éditable (F-264) et récapitulatif (F-271). Ajouter « sauf à parfaire » et le point de départ des intérêts améliore la **conformité et la complétude** de l'acte sans casser le contrat de sortie. ✅

**Anti-gadget / anti-doublon (précédents F-262/F-263 clos à la fondation)** :
- **Pas un outil décisionnel** : F-273 ne calcule rien, ne fixe aucun montant — c'est une **garde de prompt** qui impose une **mention rédactionnelle**. L'invariant « un outil = une situation » n'est pas touché : aucun `decision_tool_visibility_rules`, aucun `TOOL_REGISTRY`.
- **Pas un doublon du point 3** : le point 3 impose *quels postes* figurent au dispositif (dont les intérêts) ; F-273 impose *comment* les chiffres et intérêts sont **datés** (« sauf à parfaire à l'audience » + point de départ). Complémentaire, pas redondant — d'où l'**enrichissement du point 3 existant** plutôt qu'un 11ᵉ point isolé sur le même sujet « dispositif ».
- **Pas de séparation par garde dédiée** : créer une constante `SAUF_A_PARFAIRE_GUARD` séparée recréerait, juste à côté du point 3, un second bloc parlant du dispositif et des intérêts → bruit et risque d'instructions contradictoires. Décision : **étendre le point 3**, conforme à la directive « construite une fois ».
- **Anti-faux-positif** : la garde précise de n'apposer « sauf à parfaire » QUE sur les chefs **réellement évolutifs** (rappels/indemnités fonction du temps ou du salaire, intérêts) — **pas** sur un montant définitivement arrêté (ex. un préjudice moral forfaitaire fixé). Sinon, mention parasite.

## 4. Verdict : GO avec ajustements

**GO.** Saut métier réel (évite des montants figés / sous-évalués à l'audience), coût quasi nul, réutilise le mécanisme transverse éprouvé, zéro nouvel intrant, zéro table, zéro endpoint, zéro écran.

**Ajustements imposés à la mini-spec (invariants anti-gadget) :**
1. **Enrichir le point 3 existant** de `REDACTION_QUALITY_GUARD`, ne PAS créer une garde séparée sur le même sujet (anti-doublon).
2. **Portée uniforme** (PRODUCT_SPEC : « Portée : uniforme ») : s'applique à toutes les cellules, demandeur comme défendeur, sans condition de position ni de pays particulière — c'est une garantie de complétude du dispositif, présente sur chaque prompt comme les points 1-10. (Mécanisme transverse aux 3 domaines FR.)
3. **Ciblage des chefs évolutifs uniquement** : « sauf à parfaire » sur les sommes qui continuent d'évoluer (rappels/indemnités fonction du temps/salaire) et les intérêts ; **pas** sur les montants définitivement arrêtés.
4. **Point de départ des intérêts** : demander de préciser le point de départ pertinent par chef (mise en demeure / saisine / décision — art. 1231-6 et 1231-7 C. civ.), sans inventer de date non fondée par le dossier.
5. **Anti-jargon préservé** (non-régression SF-98-55) : formulation en langage d'acte (« sauf à parfaire à la date de l'audience »), jamais de code d'outil ni de score brut.
6. **Anti-hallucination de chiffres** (non-régression) : ne réécrit pas les montants, n'invente aucune date ni aucun montant non fourni — la garde n'ajoute qu'une **mention de réserve** et un **point de départ** qualitatif.

## 5. Pas d'impact écran

Feature purement backend (texte de prompt système). Aucun élément visible nouveau, aucun composant Angular touché, aucune route. → **Étape 0bis (cohérence écran) NON applicable** (exemption CLAUDE.md : feature purement backend sans élément visible nouveau).
