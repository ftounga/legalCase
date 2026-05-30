# Mini-spec — F-222 / SF-222-03 — Outil Habilitation familiale

## Identifiant
`F-222 / SF-222-03` — tool_id `F-FA-HABILITATION-FAMILIALE` (Famille FR)

## Objectif (1 phrase)
Évaluer si une habilitation familiale (art. 494-1 à 494-12 Cciv) est possible pour protéger un proche majeur vulnérable, et de quel type (générale / spéciale, assistance / représentation).

## Périmètre / anti-doublon
Distinct de `F-FA-25-majeurs-proteges` (sélecteur de régime sauvegarde/curatelle/tutelle/mandat). L'habilitation familiale est l'**alternative simplifiée** quand un consensus familial existe ; l'outil en évalue les conditions propres.

## Comportement (branche `default`)
Entrées : `alterationFacultesMedicalementConstatee` (bool), `lienFamilialEligible` (enum : ASCENDANT / DESCENDANT / FRERE_SOEUR / CONJOINT_PARTENAIRE / AUTRE), `consensusFamilial` (bool : pas d'opposition d'un proche), `besoinActesPatrimoniaux` (bool), `besoinActesPersonnels` (bool), `protectionPonctuelleOuGenerale` (enum : PONCTUELLE / GENERALE).
Logique :
- **Non éligible** si `alterationFacultesMedicalementConstatee=false` ou `lienFamilialEligible=AUTRE` ou `consensusFamilial=false` (en cas de conflit familial → orienter vers curatelle/tutelle = F-FA-25).
- **Habilitation spéciale** si `protectionPonctuelleOuGenerale=PONCTUELLE` (un ou plusieurs actes déterminés).
- **Habilitation générale** si `GENERALE`.
- Modalité **assistance** (besoin léger) vs **représentation** (besoin lourd) selon `besoinActes*`.
Verdict : `ELIGIBLE_HABILITATION_GENERALE` / `ELIGIBLE_HABILITATION_SPECIALE` / `ORIENTER_VERS_MESURE_JUDICIAIRE` (curatelle/tutelle → F-FA-25), + modalité (assistance/représentation) + actes couverts.

## Contrat API
`POST /api/v1/case-files/{caseFileId}/habilitation-familiale/analyze`
- Request : `{ alterationFacultesMedicalementConstatee, lienFamilialEligible, consensusFamilial, besoinActesPatrimoniaux, besoinActesPersonnels, protectionPonctuelleOuGenerale }`
- Response : `{ verdict, modalite:"ASSISTANCE"|"REPRESENTATION"|null, actesCouverts:string[], basesJuridiques:string[], messages:string[] }`

## Critères d'acceptation
- [ ] 3 verdicts ; orientation F-FA-25 si consensus absent / lien inéligible.
- [ ] Modalité assistance vs représentation déterminée.
- [ ] Champs pré-remplis IA ; isolation workspace testée.

## Plan de test
UT (verdicts + modalité + orientation), IT (200/400/workspace), Jest (form + verdict + flush jurisprudence).

## Tables / composants
- Backend : migration `habilitation_familiale_analyses`, entité+repo+service+controller.
- Frontend : `habilitation-familiale-section.component` + `TOOL_REGISTRY` + visibility + `KNOWN_NO_DASHBOARD_TILE_IDS`.
- Champs IA `FamilleExtractedData` : `hfAlteration`, `hfLienFamilial`, `hfConsensus`, `hfActesPatrimoniaux`, `hfActesPersonnels`, `hfEtendue`.

## Hors périmètre
La requête au juge des tutelles elle-même ; le régime curatelle/tutelle (F-FA-25).
