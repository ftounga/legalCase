# SF-216-02 — Prestation compensatoire FR — frontend

## Objectif

Section décisionnelle Angular `prestation-compensatoire-section` affichant le calculateur de prestation compensatoire dans le panel F-IA-04, avec pré-remplissage IA des champs durée du mariage, revenus, âge des époux et affichage du verdict (montant, forme recommandée, base juridique).

## Comportement nominal

- Composant `PrestationCompensatoireSectionComponent` (OnPush).
- Entrée `TOOL_REGISTRY` : `['F-FA-01-prestation-compensatoire', { component: PrestationCompensatoireSectionComponent }]`.
- Champs saisissables :
  - `dureeMariageAnnees` — pré-rempli depuis `aiData.vie_commune_detection.dureeMariageAnnees`
  - `revenusAnnuelsEpoux1Eur` — pré-rempli depuis `aiData.vie_commune_detection.revenusAnnuelsEpoux1`
  - `revenusAnnuelsEpoux2Eur` — pré-rempli depuis `aiData.vie_commune_detection.revenusAnnuelsEpoux2`
  - `ageEpoux1Annees` — pré-rempli depuis `aiData.ageEpoux1Annees`
  - `ageEpoux2Annees` — pré-rempli depuis `aiData.ageEpoux2Annees`
  - `formePrestationDemandee` — select enum (CAPITAL / RENTE / RENTE_CONVERTIBLE / INCERTAIN)
  - `patrimoinePropre1Eur`, `patrimoinePropre2Eur` — saisie manuelle
  - `avantageMatrimonialDetecte` — checkbox pré-cochée si `clauseAttributionIntegraleDetected`
- `prefillFromAi()` : renseigne les champs issus du record IA ; badge `auto_awesome` + provenance affiché sur les champs pré-remplis.
- Résultat affiché : montant capital indicatif, rente mensuelle indicative, forme recommandée, message disparité, base juridique (art. 270-281 Cciv).
- Gate : si `workspaceCountry ≠ FRANCE` → bannière info « outil non applicable (FR uniquement) » + composant désactivé.

## Cas d'erreur

- Backend 400 / 500 → toast d'erreur standard.
- Champs obligatoires manquants (durée, revenus, âge) → validation inline.

## Source juridique

- art. 270-281 Cciv (voir SF-216-01).

## Champs IA pré-remplis (depuis FamilleExtractedData)

| Champ formulaire | Source FamilleExtractedData | Statut F-246 |
|---|---|---|
| `dureeMariageAnnees` | `vie_commune_detection.dureeMariageAnnees` | Branché F-246 |
| `revenusAnnuelsEpoux1Eur` | `vie_commune_detection.revenusAnnuelsEpoux1` | Branché F-246 |
| `revenusAnnuelsEpoux2Eur` | `vie_commune_detection.revenusAnnuelsEpoux2` | Branché F-246 |
| `ageEpoux1Annees` | `ageEpoux1Annees` | **Nouveau — SF-216-01** |
| `ageEpoux2Annees` | `ageEpoux2Annees` | **Nouveau — SF-216-01** |
| `avantageMatrimonialDetecte` | `regimes_vie_commune_detection_v2.clauseAttributionIntegraleDetected` | Branché F-246 |

## Plan de test

- UT helper `prestation-compensatoire-prefill-rules.ts` : vérifier mapping de chaque champ source → cible ; vérifier badge provenance.
- Smoke test : dossier avec pièces contenant revenus connus → 3+ champs pré-remplis.
- E2E : soumettre le formulaire avec données test → verdict affiché.
- Self-check grep pré-commit : `grep -r "F-FA-01-prestation-compensatoire" frontend/src/` doit retourner l'entrée TOOL_REGISTRY.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/prestation-compensatoire-section/`.
- Fichiers : `prestation-compensatoire-section.component.ts/html/scss`, `prestation-compensatoire-prefill-rules.ts`, modèle `prestation-compensatoire.model.ts`.
- `decisional-tools-panel.component.ts` — ajout entrée `TOOL_REGISTRY` + `KNOWN_FRONTEND_TOOL_IDS`.

## Critères d'acceptation

- AC1 : ouverture dossier FR avec revenus connus → `dureeMariageAnnees`, `revenusAnnuels*Eur` pré-remplis, badges `auto_awesome`.
- AC2 : workspace BE → bannière info, pas d'appel backend.
- AC3 : soumission formulaire complet → verdict affiché, base juridique visible.
- AC4 : self-check grep TOOL_REGISTRY → OK.

## Hors périmètre

- Backend (SF-216-01).
- Historique des révisions post-jugement (F-FA-13-revisions).
