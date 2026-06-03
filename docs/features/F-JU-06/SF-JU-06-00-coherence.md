# F-JU-06 — Cadrage cohérence (étape 0)

## Verdict : **GO**

## Intention métier (1 phrase)
Garantir que les arrêts cités sous les outils décisionnels et dans les conclusions sont **réellement pertinents** pour la situation traitée (et jamais vides), en durcissant le pipeline d'évaluation des mappings jurisprudence et en re-bootstrappant les outils existants — **sans curation manuelle** (effectif / volumétrie).

## Source du workflow
Pratique standard de l'avocat (plaidoirie / rédaction de conclusions) + **signal terrain documenté 2026-06-03** : sur le comparateur d'indemnités (F-DT-09), citations affichées = 1 arrêt hors-sujet (« restauration ferroviaire », confiance 0,72) + 1 chapeau vide. Référence : `reference_be_jurisprudence_sources` (invariant « silence > erreur » établi sur F-JU-04 BE).

## Workflow métier réel de l'utilisateur cible (avocat)
1. L'avocat ouvre un dossier et y dépose les pièces. *(⚠ pratique standard)*
2. Il lance l'analyse → faits, points juridiques, risques.
3. Il identifie l'outil décisionnel pertinent (ex. comparateur d'indemnités).
4. Il exécute l'outil → résultat chiffré (fourchette, score…).
5. **Il consulte la jurisprudence qui FONDE ce calcul** (arrêts cités sous le résultat) — il a besoin d'arrêts *applicables à sa situation*.
6. **Il vérifie l'arrêt** : lit le chapeau, ouvre Légifrance, juge s'il soutient son argument.
7. Il s'appuie sur l'arrêt pour argumenter / l'intègre à ses conclusions.
8. Il génère les conclusions intégrant la jurisprudence.
9. Il dépose / plaide devant la juridiction.

**Point critique métier (étapes 5-7)** : citer un arrêt **hors-sujet** ou **vide** devant un juge ou un client **décrédibilise** l'avocat — c'est pire que ne rien citer. La *fiabilité* de la citation est une exigence métier dure, pas un confort.

## Cartographie features actuelles ↔ workflow
| Étape métier | Feature(s) LegalCase | Statut |
|---|---|---|
| 1-2. Dossier + analyse | F-3/F-4/F-5 analyse de dossier | ✅ Livrée |
| 3-4. Outil décisionnel + calcul | Outils F-DT/F-IM/F-FA + F-IA-04 visibilité | ✅ Livrée |
| 5. Jurisprudence qui fonde le calcul | **F-JU-01** citations dans les outils | ✅ Terminée (Cassation V1) |
| 6. Vérification de l'arrêt | F-JU-01 lien Légifrance ✅ · F-241 deeplinks ✅ · F-179 vérif citations docs ✅ · **SF-JU-01-FIX** filtre vide/confiance | 🟡 PR #1568 |
| 7-8. Argumentation / conclusions | **F-JU-02** jurisprudence dans conclusions ✅ · F-242 ajout manuel ✅ | ✅ Livrée |
| 9. Dépôt / plaidoirie | hors périmètre LegalCase | — |
| **Pertinence sémantique des arrêts** (transverse 5-8) | *aucune* — pas de garde-fou « l'arrêt porte-t-il vraiment sur le sujet de l'outil ? » | ❌ **Manquante → F-JU-06** |

## Position de la nouvelle feature
F-JU-06 agit **à la source** (pipeline de bootstrap / évaluation) pour que les étapes 5-8 reçoivent des arrêts fiables. Elle est transverse à toutes les sources (Cassation F-JU-01, et plus tard administrative F-JU-05, BE F-JU-04).

## Challenge amont
Tout ce que F-JU-06 suppose existe déjà :
- mappings à ré-évaluer → table `tool_jurisprudence_mappings` (F-JU-01) ✅
- source d'arrêts → JUDILIBRE via PISTE OAuth2 (`JudilibreApiClient`) ✅
- évaluateur LLM → `ClaudeJurisprudenceEvaluator` ✅
- orchestrateur de bootstrap → `JurisprudenceBootstrapService` (idempotent via `existsBy…`) ✅
- gate Anthropic obligatoire (la 2ᵉ passe = appel LLM) → `AiCallContext` / `AnthropicService` ✅
- écran admin de suivi/arbitrage → `/super-admin/jurisprudence-watch` (audit log) ✅

→ **Aucun trou amont. GO amont.**

## Challenge aval
La sortie de F-JU-06 = des mappings de meilleure qualité, consommés par :
- F-JU-01 affichage outil ✅ · F-JU-02 conclusions ✅ · filtre SF-JU-01-FIX ✅
- mesure de qualité → exploitable dans le dashboard admin ✅

→ **Aucun trou aval. GO aval.**

## STOPs / pré-requis à ajouter au backlog
Aucun bloquant. Pré-requis tous livrés. La 2ᵉ passe pertinence augmente le **coût LLM** du bootstrap (un appel de vérification par mapping) — à dimensionner en mini-spec, pas un trou fonctionnel.

## Invariants anti-gadget pour la mini-spec
1. **2ᵉ passe sémantique obligatoire** : la pertinence est jugée par confrontation *situation métier de l'outil ↔ contenu de l'arrêt*, **pas** par un simple seuil numérique (le 0,72 hors-sujet prouve que `confidence_score` ne suffit pas).
2. **« silence > erreur »** : si aucun arrêt ne passe le contrôle, l'outil n'affiche **aucune** citation — interdiction d'un fallback « meilleur disponible » forcé.
3. **Zéro hallucination** : uniquement des arrêts réels JUDILIBRE, lien Légifrance vérifié (cohérent avec le parking F-JU-04 BE).
4. **Chapeau non vide** garanti (déjà via SF-JU-01-FIX, conservé).
5. **Mesure obligatoire** post-re-bootstrap : taux de rétention, % chapeaux pleins, échantillon de pertinence vérifié — **pas de re-bootstrap aveugle**.
6. **Idempotence + fenêtre** : re-bootstrap idempotent et lancé **hors fenêtre de déploiement** (un rolling update tue les jobs async — cf. incidents F-JU-01).

## Décision finale
**GO** — toutes les briques amont/aval existent ; F-JU-06 est une amélioration qualité sans trou fonctionnel. Statut PRODUCT_SPEC : `Backlog` → `À faire`. Découpage indicatif (à préciser en mini-spec) : SF-01 évaluateur durci + 2ᵉ passe pertinence ; SF-02 requêtes JUDILIBRE ciblées ; SF-03 re-bootstrap outillé des 121 outils FR + rapport de mesure.
