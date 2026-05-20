# SF-216-08 — ARIPA recouvrement pension alimentaire impayée — frontend

## Objectif

Section `aripa-recouvrement-section` : guide l'avocat dans la démarche ARIPA avec pré-remplissage IA du montant de la pension et du titre exécutoire, affiche la voie de recouvrement recommandée et les étapes concrètes.

## Comportement nominal

- Composant `AripaRecouvrementSectionComponent` (OnPush).
- TOOL_REGISTRY : `['F-FA-ARIPA-RECOUVREMENT', { component: AripaRecouvrementSectionComponent }]`.
- Champs :
  - `montantPensionMensuelleEur` — pré-rempli `montantPensionMensuelleDueEur`
  - `nombreMoisImpayes` — saisie manuelle
  - `titreExecutoire` — checkbox, pré-coché si `titreExecutoireDetecte=true`
  - `situationCreancier` / `situationDebiteur` — select enum
  - `nombreEnfantsACharge` — pré-rempli `filiation_detection_v2.nombreEnfantsDetecte`
  - `debiteurEnFrance` — checkbox
- Résultat : voie recommandée, montant ASF éligible, étapes sous forme de timeline, base légale.
- Gate `workspaceCountry=FRANCE`.

## Champs IA pré-remplis

| Champ | Source | Statut F-246 |
|---|---|---|
| `montantPensionMensuelleEur` | `montantPensionMensuelleDueEur` | **Nouveau — SF-216-07** |
| `titreExecutoire` | `titreExecutoireDetecte` | **Nouveau — SF-216-07** |
| `nombreEnfantsACharge` | `filiation_detection_v2.nombreEnfantsDetecte` | Branché F-246 |

## Plan de test

- UT helper `aripa-recouvrement-prefill-rules.ts`.
- Self-check grep : TOOL_REGISTRY présent.
- Smoke test : dossier avec titre exécutoire détecté → checkbox pré-cochée.

## Composants impactés

- Nouveau répertoire `frontend/src/app/case-files/aripa-recouvrement-section/`.
- `decisional-tools-panel.component.ts` — TOOL_REGISTRY + KNOWN_FRONTEND_TOOL_IDS.

## Critères d'acceptation

- AC1 : montant pension pré-rempli depuis IA.
- AC2 : workspace BE → bannière info.
- AC3 : soumission → voie recommandée + étapes affichées.

## Hors périmètre

- Backend (SF-216-07).
