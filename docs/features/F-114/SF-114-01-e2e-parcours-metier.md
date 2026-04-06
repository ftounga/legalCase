# Mini-spec — F-114 / SF-114-01 Tests E2E fonctionnels métier

---

## Identifiant

`F-114 / SF-114-01`

## Feature parente

`F-114` — Tests E2E fonctionnels métier

## Statut

`draft`

## Date de création

2026-04-06

## Branche Git

`feat/SF-114-01-e2e-parcours-metier`

---

## Objectif

Ajouter des tests E2E Playwright couvrant le parcours critique complet : créer dossier → uploader document → lancer analyse → consulter synthèse → exporter PDF. Exécutés sur staging après déploiement.

---

## Comportement attendu

### Parcours testé

1. Login (réutilise `loginLocal()`)
2. Créer un dossier (nom préfixé `[E2E]`)
3. Uploader un document PDF de test
4. Lancer l'analyse IA
5. Attendre que l'analyse se termine (polling SSE ou polling page)
6. Naviguer vers la synthèse
7. Vérifier la présence des sections (faits, risques, points juridiques, timeline)
8. Cliquer "Exporter PDF" et vérifier le téléchargement
9. Cleanup : supprimer le dossier créé

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Analyse IA timeout (> 2 min) | Test échoue avec message clair |
| Document non uploadé | Test échoue à l'étape upload |
| Synthèse vide | Test échoue — vérifie la présence de contenu |

---

## Critères d'acceptation

- [ ] Fichier `e2e/smoke/case-analysis-flow.spec.ts` créé
- [ ] Un document PDF de test existe dans `e2e/fixtures/`
- [ ] Le test couvre : créer dossier → upload → analyse → synthèse → export PDF
- [ ] Le dossier est supprimé dans `afterAll` (cleanup)
- [ ] Le test passe sur staging avec le compte e2e
- [ ] Le workflow CI existant (`smoke.yml`) exécute le nouveau test
- [ ] Les tests existants restent verts

---

## Périmètre

### Hors scope

- Tests de l'analyse enrichie (Q&A)
- Tests du diff inter-analyses
- Tests des délais légaux / checklist procédurale
- Mocking de l'API Anthropic

---

## Technique

### Fichiers créés

| Fichier | Description |
|---------|-------------|
| `e2e/smoke/case-analysis-flow.spec.ts` | Test du parcours complet |
| `e2e/fixtures/test-contrat-travail.pdf` | Document PDF de test pour l'upload |

### Fichiers existants non modifiés

- `e2e/helpers/auth.helper.ts` — réutilisé tel quel
- `e2e/playwright.config.ts` — pas de changement
- `e2e/global-setup.ts` — nettoie déjà les dossiers e2e avant chaque run
- `.github/workflows/smoke.yml` — exécute déjà tous les `.spec.ts` du dossier smoke

---

## Plan de test

- [ ] Le test passe en local contre un backend dev (si disponible)
- [ ] Le test passe sur staging via le workflow smoke.yml
- [ ] Les tests E2E existants restent verts

## Analyse d'impact

- [x] **Aucune préoccupation transversale** — ajout de tests uniquement, aucun code applicatif modifié
