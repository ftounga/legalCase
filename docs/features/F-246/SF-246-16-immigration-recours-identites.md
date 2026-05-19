# SF-246-16 — Pré-remplissage IA : immigration-recours — identités requérant + décision contestée

## Statut
À faire

## Feature parente
F-246 — Complétion du pré-remplissage IA des outils décisionnels (vague A)

## Objectif
Étendre le pré-remplissage IA de `immigration-recours-section` (F-IM-06) aux champs
identité requérant et décision contestée : `nom`, `prenom`, `nationalite`, `dateDecision`,
`reference`.
- `nationalite` : champ déjà présent dans le record backend `ImmigrationExtractedData`
  (F-235), absent du DTO TypeScript frontend et du helper → frontend seul.
- `nom`, `prenom`, `dateDecisionContestee`, `referenceDecision` : absents du record
  backend → extension full-stack (backend record + prompt + extractor + DTO + helper + composant).
`referes-admin-section` : déjà entièrement implémenté (SF-236-02) — aucun travail.

## Périmètre

### Composants touchés
| Couche | Fichier | Modification |
|--------|---------|--------------|
| Backend record | `CaseAnalysisResponse.java` — `ImmigrationExtractedData` | +4 champs (`nomRequerant`, `prenomRequerant`, `dateDecisionContestee`, `referenceDecision`) |
| Backend builder | `ImmigrationExtractedData.Builder` | +4 méthodes builder |
| Backend extractor | `extractImmigrationData()` | parse `nom_requerant`, `prenom_requerant`, `date_decision_contestee`, `reference_decision` |
| Backend prompt | `LegalDomainPromptBuilder.IMMIGRATION_INSTRUCTION` | +4 instructions champs |
| Frontend DTO | `case-analysis.model.ts` — `ImmigrationExtractedData` | +5 champs (nationalite + 4 nouveaux) |
| Frontend helper | `immigration-recours-section-prefill-rules.ts` | +5 compute functions + count |
| Frontend composant | `immigration-recours-section.component.ts` | extend `prefillFromAi()` + 5 signaux provenance + handlers |

### Mapping champs
| Signal composant | Champ aiData | Source |
|-----------------|--------------|--------|
| `nom` | `nomRequerant` | NOUVEAU (full-stack) |
| `prenom` | `prenomRequerant` | NOUVEAU (full-stack) |
| `nationalite` | `nationalite` | Frontend seul (backend F-235) |
| `dateDecision` | `dateDecisionContestee` | NOUVEAU (full-stack) |
| `reference` | `referenceDecision` | NOUVEAU (full-stack) |

### Hors périmètre
- `adresse` requérant : non extractible de façon fiable depuis les pièces immigration
- `autorite` et `exposeFaits` : champs libres non structurés, hors périmètre pré-fill IA
- `referes-admin-section` : déjà implémenté
- Toute extension des outils immigration autres que F-IM-06

## Comportement nominal

### Backend
1. `extractImmigrationData()` parse les 4 nouveaux champs JSON depuis le nœud racine de l'IA
2. Champs texte : `textOrNull()` — null si absent ou vide
3. `dateDecisionContestee` : format texte libre (ISO YYYY-MM-DD attendu mais non normalisé côté Java — le frontend rejette via `ISO_DATE_RE`)
4. Les 4 nouveaux champs font partie du guard `if (... && nomRequerant == null && ...)` → retour null si TOUT est null

### Frontend helper (pur)
- `computeNomRequerant(input)` → `nonEmpty(aiData?.nomRequerant)` 
- `computePrenomRequerant(input)` → `nonEmpty(aiData?.prenomRequerant)`
- `computeNationalite(input)` → `nonEmpty(aiData?.nationalite)`
- `computeDateDecision(input)` → `nonEmpty + ISO_DATE_RE.test` (même garde que `computeDateNotification`)
- `computeReferenceDecision(input)` → `nonEmpty(aiData?.referenceDecision)`
- `computePrefillCount` passe de 2 à 7 maximum

### Frontend composant
- `prefillFromAi()` : 5 nouveaux blocs `if (val !== null) { signal.set(val); provenanceX.set('IA'); }`
- Guard `if (!signal())` : préserve la saisie manuelle (ne préfill que si signal vide)
- 5 nouveaux `provenanceX = signal<'IA' | null>(null)`
- 5 nouveaux handlers `onXChange()` qui appellent `provenanceX.set(null)`
- `patchForm()` / `editForm()` : reset toutes les provenances à null (identique à SF-246-15)

## Cas d'erreur
- Champ absent ou null dans la réponse IA → compute function retourne null → aucun pré-fill
- Date non ISO (`dateDecision`) → `ISO_DATE_RE` échoue → null → aucun pré-fill
- Signal déjà renseigné manuellement → guard préserve la saisie existante
- Stash SF-246-13 déjà appliqué → travailler proprement sur cette branche

## Critères d'acceptation
1. `computePrefillCount` retourne 7 si les 7 champs sont renseignés dans `aiData`
2. `computeNomRequerant({})` retourne null
3. `computeNomRequerant({ aiData: { nomRequerant: 'Dupont' } })` retourne `'Dupont'`
4. `computeDateDecision({ aiData: { dateDecisionContestee: '2024-05-15' } })` retourne `'2024-05-15'`
5. `computeDateDecision({ aiData: { dateDecisionContestee: 'demain' } })` retourne null
6. `prefillFromAi()` ne préfill pas un signal déjà non vide
7. `onNomChange()` remet provenanceNom à null
8. Backend : `extractImmigrationData(node)` parse `nom_requerant` → `nomRequerant`
9. Backend : guard null total toujours respecté (pas de régression)

## Plan de test
- Tests unitaires Jest (helper) : 20+ cas couvrant les 5 fonctions
- Tests backend : 1 test `extractImmigrationData` avec les 4 nouveaux champs
- Smoke E2E : `cd e2e && npm test` — ~27 failures pré-existants tolérés

## Tables / endpoints impactés
- Aucune migration DB
- Endpoint `GET /api/case-files/{id}/analysis` : le JSON `immigrationExtractedData`
  contiendra les 4 nouveaux champs à la prochaine ré-analyse

## Parité static / runtime
`ImmigrationRecoursSectionComponent.getPrefillCount()` délègue à
`ImmigrationRecoursPrefillRules.computePrefillCount()` — parité garantie par construction.
