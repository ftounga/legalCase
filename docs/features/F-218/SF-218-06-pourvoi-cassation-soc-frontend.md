# Mini-spec — F-218 / SF-218-06 — Pourvoi en cassation chambre sociale — frontend

## Identifiant

`F-218 / SF-218-06`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-06-pourvoi-cassation-soc-frontend`

---

## Objectif

Livrer le composant Angular `<app-pourvoi-cassation-soc-section>` pour `F-DT-87-pourvoi-cassation-soc`, affichant le délai de pourvoi (2 mois), l'analyse des cas d'ouverture, le risque de non-admission (filtre NPC) et le verdict d'opportunité.

---

## Comportement attendu

- Formulaire : `dateNotificationArret` (`<input type="date">`, pré-rempli), `casOuverture` (multi-select chips parmi les 7 cas), `representationAvocatCassation` (checkbox), `moyenSerieuxIdentifie` (checkbox).
- Résultat : `dateLimitePourvoi` + `joursRestants` (JetBrains Mono), badge verdict global (`POURVOI_RECOMMANDE` vert / `POURVOI_RISQUE` or / `POURVOI_DECONSEILLE` navy / `DELAI_EXPIRE` rouge), badge `risqueNonAdmission` (ELEVE rouge / MODERE or / FAIBLE vert), liste des cas d'ouverture avec force probatoire, item bloquant représentation, `baseJuridique` (JetBrains Mono).
- CONTEXTUAL : apparaît si flag IA `pourvoi_cassation_soc_envisage` = true. Groupement thématique `CONTENTIEUX`.
- Pré-fill : `dateNotificationArret`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `DELAI_EXPIRE` / `risqueNonAdmission=ELEVE` ; or pour risque modéré ; `<input type="date">` ; JetBrains Mono délais/baseJuridique ; bannière gate FR ; MatSnackBar)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit+ngOnChanges, `provenanceDateNotificationArret` signal + badge `auto_awesome` + handler
- [x] Validation F-IA-03 : `coherenceAlerts` computed sur `dateNotificationArret` croisé aiData + procedureChecks ; `CoherenceAlertBuilder` partagé
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-87-pourvoi-cassation-soc` dans `KNOWN_FRONTEND_TOOL_IDS`
- Niveau outil : 5 (analyseur de cas d'ouverture + scoring risque NPC) → parité domaines **à renseigner** :

| Domaine | Équivalent existe ? | Si non → action |
|---------|---------------------|-----------------|
| Droit du travail | Oui (cet outil F-DT-87) | — |
| Immigration | Non | Non pertinent : le pourvoi en cassation immigration relève du Conseil d'État (contentieux administratif), mécanisme distinct — pas de jumeau au backlog F-218 |
| Famille | Non | Concept pertinent (pourvoi civil) mais hors P3 Travail — backlog VN si demande terrain famille émerge |

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] POST `casOuverture=[DENATURATION]` + `moyenSerieuxIdentifie=false` → badge `risqueNonAdmission=ELEVE` (rouge)
- [ ] POST notification J-50 → `DELAI_URGENT`
- [ ] `dateNotificationArret` pré-rempli avec badge provenance
- [ ] multi-select `casOuverture` affiche la force probatoire par cas
- [ ] Tests Jest ≥ 12 (verdicts, risque NPC, multi-select, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `PourvoiCassationSocSectionComponent`
- **Nouveau service** `PourvoiCassationSocService`
- **Nouveau modèle** `PourvoiCassationSocAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-218-05 : statut `done`

## Hors périmètre

- Rédaction du mémoire ampliatif
