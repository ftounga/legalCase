# Mini-spec — F-218 / SF-218-08 — Saisie sur rémunération (quotité saisissable) — frontend

## Identifiant

`F-218 / SF-218-08`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-08-saisie-remuneration-frontend`

---

## Objectif

Livrer le composant Angular `<app-saisie-remuneration-section>` pour `F-DT-89-saisie-arret-remuneration`, affichant la quotité saisissable mensuelle calculée par tranches, le montant laissé au salarié, le nombre de mois de recouvrement et le détail du barème (mention « à actualiser annuellement »).

---

## Comportement attendu

- Formulaire : `remunerationNetteMensuelle` (number, pré-rempli depuis salaire), `nombrePersonnesACharge` (number, pré-rempli), `creanceTotale` (number), `creanceAlimentaire` (checkbox).
- Résultat : `quotiteSaisissableMensuelle` + `montantLaisseAuSalarie` + `nombreMoisRecouvrement` (JetBrains Mono), badge verdict (`SAISISSABLE` navy / `INSAISISSABLE` or / `ALIMENTAIRE_PAIEMENT_DIRECT` navy), tableau du détail par tranche, note « barème R. 3252-2 à actualiser annuellement », `baseJuridique` (JetBrains Mono).
- CONTEXTUAL : apparaît si flag IA `saisie_remuneration_detectee` = true. Groupement thématique `CONTENTIEUX`.
- Pré-fill : `remunerationNetteMensuelle` (depuis `salaireBrutMensuel`, badge provenance + note conversion), `nombrePersonnesACharge`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge non utilisé hors alerte critique ; navy/or pour info ; JetBrains Mono montants/baseJuridique ; bannière gate FR ; MatSnackBar)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` ngOnInit+ngOnChanges, signals `provenanceRemuneration` / `provenanceNbPersonnesACharge` + badges `auto_awesome` + handlers
- [x] Validation F-IA-03 : `coherenceAlerts` computed sur `remunerationNetteMensuelle` croisé `salaireBrutMensuel` aiData ; `CoherenceAlertBuilder` partagé
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-89-saisie-arret-remuneration` dans `KNOWN_FRONTEND_TOOL_IDS`
- Niveau outil : 3 (calculateur de quotité) → parité domaines **non applicable** (niveau < 5 ; barème de saisie = Code travail FR, pas d'équivalent immigration/famille)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] `remunerationNetteMensuelle=2000`, 0 personne à charge → quotité affichée conforme + tableau par tranche
- [ ] `nombrePersonnesACharge=3` → quotité réduite affichée
- [ ] rémunération ≤ fraction insaisissable → badge `INSAISISSABLE`
- [ ] `remunerationNetteMensuelle` pré-rempli depuis salaire avec badge provenance + note conversion
- [ ] note « barème à actualiser annuellement » visible
- [ ] Tests Jest ≥ 12 (calcul affiché, tranches, insaisissable, alimentaire, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `SaisieRemunerationSectionComponent`
- **Nouveau service** `SaisieRemunerationService`
- **Nouveau modèle** `SaisieRemunerationAnalysis`
- **Modification** `decisional-tools-panel.component.ts`

## Dépendances

- SF-218-07 : statut `done`

## Hors périmètre

- Génération de l'acte de saisie
