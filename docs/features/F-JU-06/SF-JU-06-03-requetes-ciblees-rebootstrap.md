# Mini-spec — F-JU-06 / SF-JU-06-03 Requêtes JUDILIBRE ciblées + re-bootstrap

## Identifiant
`F-JU-06 / SF-JU-06-03`

## Feature parente
`F-JU-06`. Cadrage GO : `SF-JU-06-00-coherence.md`. Suit SF-01 (pipeline durci) + SF-02 (nettoyage existant).

## Statut
`ready`

## Date de création
2026-06-04

## Branche Git
`feat/SF-JU-06-03-requetes-ciblees`

## Type
Feature (couverture). Cadrage cohérence couvert par SF-JU-06-00. Impact écran mineur (une checkbox) → étape 0 bis non requise.

---

## Objectif (une phrase)
Permettre un re-bootstrap qui **enrichit automatiquement** les requêtes JUDILIBRE (mots-clés génériques → termes juridiques + articles de loi) pour ramener de meilleurs candidats et re-remplir les outils dégarnis par SF-02 — sans curation manuelle.

## Contexte
Après SF-02 (archivage des mauvais mappings), des outils se retrouvent sans citation. Les ré-alimenter via re-bootstrap suppose de bons candidats JUDILIBRE. Or les `motCleRecherche` actuels sont génériques (« Comparateur d'indemnités ») → bruit (l'arrêt « restauration ferroviaire » venait de là). SF-03 enrichit la requête avant l'appel JUDILIBRE. Le re-bootstrap réutilise le mécanisme existant (`startBootstrap` + CSV) avec le pipeline durci SF-01 et l'idempotence (outils déjà couverts skippés).

## Comportement nominal
- Un SUPER_ADMIN relance un bootstrap depuis `/super-admin/jurisprudence-watch` (onglet Bootstrap) en cochant **« Enrichir les requêtes (IA) »**.
- Pour chaque entrée, si l'enrichissement est activé : un appel LLM **gaté** transforme `(toolId + motCleRecherche)` en requête JUDILIBRE ciblée (termes juridiques + articles), utilisée pour `fetchArretsByKeyword`.
- Le reste du pipeline (filtre juridiction, évaluateur durci, 2ᵉ passe, rejets SF-01) s'applique inchangé.
- **Mesure** : la réponse de bootstrap (`entriesProcessed / mappingsCreated / entriesSkipped`) + le job status fournissent le rapport (combien d'outils re-remplis).

## Cas d'erreur / bords
- Enrichissement LLM échoue/vide → **fallback** sur le `motCleRecherche` original (pas de blocage).
- `enrichQueries=false` (défaut) → comportement strictement inchangé (rétrocompatibilité).
- Outil déjà couvert (mapping actif présent) → skip idempotent (inchangé).
- Coût : +1 appel LLM par entrée quand activé — assumé (job async, action opérateur hors fenêtre de déploiement).

## Solution technique
### Backend
1. **`JudilibreQueryEnricher`** (nouveau) : `enrich(toolId, brancheCalculId, motCleRecherche)` → requête optimisée via `AnthropicService` gaté (`SYSTEM_JP_BOOTSTRAP`). Prompt : « donne la meilleure requête de mots-clés JUDILIBRE (termes juridiques + articles) pour le sujet X ». Fallback = `motCleRecherche` original sur échec/vide. Tronque à une longueur raisonnable.
2. **`JurisprudenceBootstrapRequest`** : ajouter `boolean enrichQueries` + **constructeur de compatibilité** `(entries)` → `(entries, false)` pour ne casser ni les tests ni les appels existants.
3. **`JurisprudenceBootstrapService`** : injecter `JudilibreQueryEnricher` ; propager `enrichQueries` (champ de job ou paramètre du runner) ; avant `fetchArretsByKeyword`, si activé → `query = enricher.enrich(...)` sinon `entry.motCleRecherche()`.
4. Le flag traverse `startBootstrap` → `runBootstrap` (signature interne).

### Frontend
5. Onglet Bootstrap : checkbox **« Enrichir les requêtes (IA) »** (`mat-checkbox`) liée à un champ du composant, envoyée dans le payload `triggerBootstrap`. Hint expliquant le coût LLM.

## Critères d'acceptation (vérifiables)
1. `enrichQueries=false` → `fetchArretsByKeyword` reçoit le `motCleRecherche` original (test).
2. `enrichQueries=true` → `fetchArretsByKeyword` reçoit la requête enrichie (test).
3. Enrichissement en échec → fallback sur l'original, le bootstrap continue (test).
4. L'enrichisseur passe par le gate Anthropic.
5. Rétrocompat : les appels `new JurisprudenceBootstrapRequest(entries)` compilent (constructeur de compat).
6. Build + tests verts (back + front).

## Plan de test minimal
- **Unitaire `JudilibreQueryEnricher`** : réponse LLM → requête ; échec/vide → fallback original (AnthropicService mocké).
- **Unitaire `JurisprudenceBootstrapService`** : `enrichQueries=true` → enricher appelé + query enrichie passée à JUDILIBRE ; `false` → enricher jamais appelé.
- **IT** : POST `/bootstrap` avec `enrichQueries:true` accepté (202).
- **Frontend spec** : checkbox cochée → payload inclut `enrichQueries:true`.
- **Isolation workspace** : N/A (donnée globale).

## Tables / endpoints / composants impactés
- **Backend** : `JudilibreQueryEnricher` (nouveau), `JurisprudenceBootstrapRequest` (champ), `JurisprudenceBootstrapService` (intégration), `AnthropicService` (appel gaté).
- **Endpoint** : `POST /super-admin/jurisprudence-watch/bootstrap` (payload étendu, rétrocompatible).
- **Frontend** : `jurisprudence-watch` (checkbox + service).
- **Pas de migration** (le flag n'est pas persisté en colonne dédiée requise ; le job conserve son schéma — décision : flag porté en mémoire dans le runner).

### Préoccupation transversale : **Outil décisionnel métier** + **gate Anthropic**
Composants listés. Pas d'impact auth/workspace/plans/navigation → smoke E2E auth/nav non requis.

## Hors périmètre
- Identification automatique des outils dégarnis (l'opérateur re-bootstrappe le CSV complet ; l'idempotence skippe les couverts).
- Persistance d'un nouveau type de job dédié au re-bootstrap (réutilise le job bootstrap existant).
- Sources non-Cassation (F-JU-04 BE / F-JU-05 administratif).
- Curation manuelle (exclue par décision PO).
