# SF-212-32 — Frontend : section « élections CSE — conformité »

> Feature F-212. Outil : `F-DT-65-elections-cse-conformite`. Contrat API : `SF-212-31` (figé).

## Objectif

Afficher la section permettant à l'avocat de vérifier la conformité du processus électoral et d'identifier les bases de contestation dans le délai de 15 jours.

## Comportement nominal

Composant standalone `ElectionsCseConformiteSectionComponent`, sous `F-DT-65-elections-cse-conformite`. Affiché en `CONTEXTUAL` (flag `election_cse_detectee`). **Gate `isFrance`**.

Formulaire (7 champs) : effectif, PAP négocié (toggle), délai invitation OS (toggle), collèges conformes (toggle), résultats contestés (toggle), date élection, motif contestation (texte). Bouton « Analyser » → verdict + délai contestation 15 j (affiché en rouge si urgent) + date limite contestation + points d'irrégularité.

**Pré-remplissage IA** depuis `aiData.travailExtractedData.electionCseDetail`. **Cohérence F-IA-03** sur 4 `critereCode`.

## Cas d'erreur

- Backend 422 → message outil FR-only.
- 4xx/5xx → `MatSnackBar`.

## Critères d'acceptation

1. Section visible uniquement si `election_cse_detectee = true`.
2. Délai 15 j affiché et mis en évidence si date élection proche.
3. Verdict 3 états couleur.
4. Gate `isFrance` → bannière.
5. Pré-remplissage IA + `getPrefillCount()`.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu, pré-remplissage, délai 15 j urgent, verdict 3 états.

## Tables / endpoints / composants impactés

- Nouveaux fichiers composant, helper, service.
- Modifié : `TOOL_REGISTRY`.

## Hors périmètre

Backend (→ SF-212-31).
