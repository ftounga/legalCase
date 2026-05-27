# Mini-spec — F-JU-01 / SF-JU-01-12 Assouplir SYSTEM_PROMPT_BOOTSTRAP (anti-NONE-systémique)

## Identifiant

`F-JU-01 / SF-JU-01-12`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-12-prompt-permissive`

---

## Objectif

Corriger un comportement observé sur le job `d3a49987-...` lancé en staging à 11:35 UTC (après déploiement SF-11) : **18 entrées traitées, 0 mapping créé, 18 skipped en `NONE`**. Le format JSON est désormais correct (SF-11 a corrigé `parse fail`), mais Claude renvoie systématiquement `action=NONE` même pour les entrées FR évidentes (`F-DT-04-fiche-prudhomale,chambre sociale`), car le prompt actuel exige des arrêts « plénière, publiés au Bulletin » — exigence non remplie sur la plupart des candidats JUDILIBRE retournés par recherche mot-clé.

---

## Comportement attendu

Le `SYSTEM_PROMPT_BOOTSTRAP` est réécrit pour :
- **Retirer les priorités strictes** (plénière, Bulletin, formation de section) qui poussent Claude à NONE par défaut
- **Préférer ADD chaque fois qu'un candidat évoque vaguement le sujet** — le confidence_score (0.3-0.5) reflète l'incertitude, mais l'action reste ADD
- **Rappeler explicitement** que la veille mensuelle ultérieure affine les choix — le bootstrap n'a pas besoin de l'arrêt parfait
- **Ajouter un 2ᵉ exemple ADD** avec confidence faible (0.40) pour ancrer le pattern « ADD avec incertitude »
- **Mentionner explicitement le mismatch FR/BE** comme seul cas légitime de NONE

Le format JSON-only (SF-11) est conservé tel quel — c'est un acquis stable.

---

## Critères d'acceptation

- [ ] Le prompt contient explicitement : « MÊME s'il n'est pas idéal », « Aucune exigence de formation plénière », « TRÈS exceptionnelle »
- [ ] Le prompt mentionne « préfère la chambre la plus adaptée » + « date la plus récente » pour le tie-break ADD
- [ ] Le prompt contient au moins 3 exemples JSON inline : 1 ADD confiance haute, 1 ADD confiance faible, 1 NONE
- [ ] Test `evaluate_bootstrapMapping_usesBootstrapPrompt` étendu avec 3 nouvelles assertions ancrant l'assouplissement (anti-régression future)
- [ ] Anti-régression : 14/14 tests `ClaudeJurisprudenceEvaluatorTest` verts

---

## Hors scope

- **Migration tool use Anthropic** (structured output natif) — reste en hors scope V1
- **Durcissement `SYSTEM_PROMPT` historique** (mode dérive) — pas observé en erreur
- **Modification du `JudilibreApiClient`** (filtre juridiction, plafond 20 candidats) — reste tel quel, voir si pertinent dans une SF future si on continue à voir des résultats faibles

---

## Technique

| Fichier | Modification |
|---------|--------------|
| `ClaudeJurisprudenceEvaluator.java` | `SYSTEM_PROMPT_BOOTSTRAP` : règles ADD/NONE assouplies, 3 exemples JSON (haute confiance, marginale, NONE BE) |
| `ClaudeJurisprudenceEvaluatorTest.java` | 3 nouvelles assertions sur le contenu du prompt (ancrage anti-régression) |

---

## Notes

- **Itération PR rapide sur le prompt LLM** : on a maintenant un cycle SF-08 → SF-11 → SF-12 sur le même prompt. Acceptable car chaque itération est livrée en < 30 min et observable directement en staging. Un prompt LLM ne se teste pas à 100 % en unitaire — il faut le voir tourner contre Claude réel.
- **Si SF-12 produit encore 100 % NONE** : tirer la sonnette + investiguer côté JudilibreApiClient (peut-être que les candidats retournés sont déjà trop génériques par construction).
- **Si SF-12 produit ~30-70 % ADD** : c'est un bon résultat de bootstrap initial — la veille mensuelle affinera ensuite.
