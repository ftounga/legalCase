# Mini-spec — F-222 / SF-222-04 — Outil Assistance éducative (enfant en danger)

## Identifiant
`F-222 / SF-222-04` — tool_id `F-FA-ASSISTANCE-EDUCATIVE` (Famille FR)

## Objectif (1 phrase)
Évaluer la situation d'un mineur en danger (art. 375 Cciv) et orienter vers la mesure de protection adaptée — **un seul outil** couvrant AED / AEMO / OPP / placement (invariant « 1 situation = 1 outil », cf. étape 0).

## Périmètre
Couvre les 3 issues d'UNE situation (mineur en danger) : AED (administrative, ASE), AEMO (judiciaire, milieu ouvert), OPP/placement (judiciaire, retrait). Ne pas découper en 3 outils.

## Comportement (branche `default`)
Entrées : `dangerCaractérise` (bool : santé/sécurité/moralité/conditions d'éducation gravement compromises), `urgence` (bool : danger immédiat), `adhesionFamille` (bool), `maintienMilieuFamilialPossible` (bool), `mesureAmiableASEEnvisageable` (bool).
Logique :
- **Pas de mesure** si `dangerCaractérise=false`.
- **AED** (administrative) si `adhesionFamille=true` ET `mesureAmiableASEEnvisageable=true` ET pas d'urgence (accord parental, contractualisée ASE).
- **AEMO** (judiciaire, JE) si `dangerCaractérise` ET (pas d'adhésion OU AED insuffisante) ET `maintienMilieuFamilialPossible=true`.
- **OPP / placement** (art. 375-3 / 375-5) si `maintienMilieuFamilialPossible=false` OU `urgence=true` (placement provisoire, 375-5 par le procureur/JE).
Verdict : `AED` / `AEMO` / `OPP_PLACEMENT` / `PAS_DE_MESURE`, + juridiction compétente (ASE vs juge des enfants) + critères de l'art. 375.

## Contrat API
`POST /api/v1/case-files/{caseFileId}/assistance-educative/analyze`
- Request : `{ dangerCaractérise, urgence, adhesionFamille, maintienMilieuFamilialPossible, mesureAmiableASEEnvisageable }` (bool)
- Response : `{ verdict, juridiction:string, mesureOrientee:string, basesJuridiques:string[], messages:string[] }`

## Critères d'acceptation
- [ ] 4 verdicts (AED/AEMO/OPP_PLACEMENT/PAS_DE_MESURE) — un SEUL outil.
- [ ] Juridiction compétente indiquée (ASE / juge des enfants).
- [ ] Champs pré-remplis IA ; isolation workspace testée.

## Plan de test
UT (4 verdicts + frontières urgence/maintien), IT (200/400/workspace), Jest (form + verdict + flush jurisprudence).

## Tables / composants
- Backend : migration `assistance_educative_analyses`, entité+repo+service+controller.
- Frontend : `assistance-educative-section.component` + `TOOL_REGISTRY` + visibility + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA `FamilleExtractedData` : `aeDangerCaracterise`, `aeUrgence`, `aeAdhesionFamille`, `aeMaintienMilieu`, `aeMesureAmiable`.

## Hors périmètre
La procédure devant le juge des enfants ; l'assistance éducative au pénal.
