# Mini-spec — F-218 / SF-218-10 — Action de groupe en discrimination — frontend

## Identifiant

`F-218 / SF-218-10`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-10-action-groupe-discrimination-frontend`

---

## Objectif

Livrer le composant Angular `<app-action-groupe-discrimination-section>` pour `F-DT-90-action-groupe-discrimination`, affichant le verdict de recevabilité de l'action de groupe, le calcul du délai de carence de 6 mois après mise en demeure et la checklist procédurale.

---

## Comportement attendu

- Formulaire : `typeOrganisation` (select), `motifDiscrimination` (select critères L. 1132-1, pré-rempli), `dateMiseEnDemeure` (`<input type="date">`, pré-rempli), `nombrePersonnesConcernees` (number), `objetAction` (select).
- Résultat : badge verdict (`RECEVABLE` vert / `PREMATURE` or / `IRRECEVABLE_QUALITE` rouge / `INFO_MANQUANTE` or), `dateRecevabiliteSaisine` + indicateur `delaiCarenceRespecte` (JetBrains Mono), checklist procédurale (items obligatoires/bloquants), `baseJuridique` (JetBrains Mono).
- CONTEXTUAL : apparaît si flag IA `action_groupe_discrimination_envisagee` = true. Groupement thématique `CONTENTIEUX`.
- Pré-fill : `motifDiscrimination`, `dateMiseEnDemeure`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `IRRECEVABLE_QUALITE` ; or pour prématuré / info manquante ; `<input type="date">` ; JetBrains Mono dates/baseJuridique ; bannière gate FR ; MatSnackBar)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit+ngOnChanges, signals `provenanceMotifDiscrimination` / `provenanceDateMiseEnDemeure` + badges `auto_awesome` + handlers
- [x] Validation F-IA-03 : `coherenceAlerts` computed sur `motifDiscrimination` / `dateMiseEnDemeure` croisé aiData + procedureChecks ; `CoherenceAlertBuilder` partagé
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-90-action-groupe-discrimination` dans `KNOWN_FRONTEND_TOOL_IDS`
- Niveau outil : 5 (analyseur de recevabilité) → parité domaines **à renseigner** :

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Oui (cet outil F-DT-90) | — |
| Immigration | Non | Concept d'action de groupe applicable à la discrimination (L. 1134-7 vise aussi d'autres champs), mais hors P3 Travail FR — backlog VN si demande terrain |
| Famille | Non | Non pertinent : pas d'action de groupe en droit de la famille |

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] POST mise en demeure J-30 → badge `PREMATURE` (or) + `dateRecevabiliteSaisine` affichée
- [ ] POST `typeOrganisation=AUTRE` → badge `IRRECEVABLE_QUALITE` (rouge)
- [ ] `dateMiseEnDemeure` et `motifDiscrimination` pré-remplis avec badges provenance
- [ ] checklist procédurale affichée avec item bloquant si mise en demeure absente
- [ ] Tests Jest ≥ 12 (verdicts, calcul carence, checklist, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `ActionGroupeDiscriminationSectionComponent`
- **Nouveau service** `ActionGroupeDiscriminationService`
- **Nouveau modèle** `ActionGroupeDiscriminationAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-218-09 : statut `done`

## Hors périmètre

- Génération de la mise en demeure
- Phase individuelle de réparation (L. 1134-10)
