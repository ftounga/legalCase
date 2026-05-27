# Mini-spec — F-JU-01 / SF-JU-01-11 Durcir SYSTEM_PROMPT_BOOTSTRAP (JSON-only)

## Identifiant

`F-JU-01 / SF-JU-01-11`

## Feature parente

`F-JU-01`

## Statut

`draft`

## Date de création

2026-05-27

## Branche Git

`feat/SF-JU-01-11-prompt-json-strict`

---

## Objectif

Corriger un bug observé en staging le 2026-05-27 ~10:55 UTC, lors du premier bootstrap async post-déploiement SF-08+09+10 : Claude renvoyait des réponses en prose française (`"Je propose..."`) au lieu du JSON pur attendu, provoquant des `parse fail` systématiques et **0 mapping créé sur 6 entrées traitées** (même symptôme que celui que SF-08 cherchait à résoudre).

---

## Comportement attendu

### Cas nominal

Le `SYSTEM_PROMPT_BOOTSTRAP` (introduit par SF-JU-01-08) reçoit une section **« RÈGLE DE FORMAT ABSOLUE »** interdisant explicitement :
- Tout préambule prose (« Voici… », « Je propose… »)
- Tout texte après l'accolade fermante
- Les balises markdown (` ```json `)

Et inclut **deux exemples JSON inline** (un avec `action=ADD`, un avec `action=NONE`) servant de few-shot pattern.

Claude doit désormais répondre par un JSON pur démarrant par `{` et finissant par `}`. Le `extractJson(...)` existant gère déjà les préambules résiduels (`indexOf('{')`), donc aucun changement côté parser nécessaire.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Claude répond quand même en prose pure (sans aucun `{`) | Fallback safe `ClaudeEvaluation.none("Parsing JSON invalide")` (comportement inchangé) — l'entrée passe en `skipped` au lieu de bloquer le batch |
| Claude répond avec préambule mais inclut un JSON valide | `extractJson` extrait depuis le premier `{` — comportement existant inchangé |

---

## Analyse de cohérence transversale

- [x] Refactor strictement local au `SYSTEM_PROMPT_BOOTSTRAP`. Aucune autre cible.
- [x] Mode dérive (`SYSTEM_PROMPT` historique des crons SF-02/03) **inchangé** — non observé en erreur ; modification reportée à une SF dédiée si nécessaire.

---

## Conformité F-IA-04

- [x] **Non applicable** — SF backend pure.

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage**.

---

## Critères d'acceptation

- [ ] `SYSTEM_PROMPT_BOOTSTRAP` contient explicitement « RÈGLE DE FORMAT ABSOLUE », « PAS de préambule », « PAS de balises markdown ».
- [ ] Au moins 2 exemples JSON inline présents dans le prompt (un `ADD`, un `NONE`).
- [ ] Test unitaire `evaluate_bootstrapMapping_usesBootstrapPrompt` étendu pour vérifier la présence des nouveaux marqueurs.
- [ ] Anti-régression : 14/14 tests `ClaudeJurisprudenceEvaluatorTest` verts.
- [ ] Aucun impact sur le mode dérive (`SYSTEM_PROMPT` historique inchangé).

---

## Périmètre

### Hors scope

- **Durcissement du `SYSTEM_PROMPT` historique** (mode dérive crons SF-02/03) — non observé en erreur, modification différée.
- **Migration vers tool use Anthropic** (structured output natif) — refonte plus profonde, à évaluer si le bug réapparaît malgré le durcissement.

---

## Technique

| Fichier | Modification |
|---------|--------------|
| `ClaudeJurisprudenceEvaluator.java` | `SYSTEM_PROMPT_BOOTSTRAP` enrichi : section « RÈGLE DE FORMAT ABSOLUE » + 2 exemples JSON inline |
| `ClaudeJurisprudenceEvaluatorTest.java` | Test `evaluate_bootstrapMapping_usesBootstrapPrompt` étendu (4 nouvelles assertions sur le contenu du prompt) |

Aucune migration. Aucun changement d'API. Aucun composant frontend touché.

---

## Plan de test

- [ ] `evaluate_bootstrapMapping_usesBootstrapPrompt` vérifie les nouveaux marqueurs.
- [ ] Anti-régression module : 78/78 tests verts.

---

## Analyse d'impact

- [x] **Aucune préoccupation transversale**.

---

## Dépendances

- `SF-JU-01-08` (prompt bootstrap initial) ✅ done
- `SF-JU-01-10` (bootstrap async + polling) ✅ done — déployé en staging avant cette SF

---

## Notes

- **Symptôme observé** : 3 entrées sur 3 dans le job `936b1605-c533-4ad1-9b5f-1a476cce0db9` ont produit `ClaudeJurisprudenceEvaluator parse fail: Unrecognized token 'Je'` → Claude renvoyait du texte commençant par « Je… ».
- **Hypothèse** : le prompt SF-08 demandait « Réponds UNIQUEMENT par un JSON » mais ne fournissait pas d'exemple inline ni d'interdiction explicite de prose. Pour un bootstrap initial où Claude doit raisonner sur quel arrêt structurant choisir, son instinct est de présenter son raisonnement avant le JSON.
- **Mitigation forte** : few-shot avec 2 exemples JSON littéraux + interdiction explicite « PAS de préambule, PAS de balises markdown ».
- **Si le bug réapparaît** malgré ce durcissement : migrer vers l'API tool use Anthropic (structured output garanti par schéma) — SF dédiée à ouvrir.
