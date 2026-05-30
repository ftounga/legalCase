# Mini-spec — F-218 / SF-218-02 — Appel CPH devant la Cour d'appel — frontend

## Identifiant

`F-218 / SF-218-02`

## Feature parente

`F-218a` — Procédure CPH avancée (P3 Travail FR)

## Statut

`ready`

## Date de création

2026-05-30

## Branche Git

`feat/SF-218-02-appel-cph-cour-appel-frontend`

---

## Objectif

Livrer le composant Angular `<app-appel-cph-section>` pour `F-DT-86-appel-cph-cour-appel`, affichant le délai d'appel calculé (1 mois), le verdict de recevabilité et la checklist des formalités d'appel social.

---

## Comportement attendu

- Formulaire : `dateNotificationJugement` (`<input type="date">`, pré-rempli), `partieAppelante` (select SALARIE/EMPLOYEUR), `modeNotification` (select), `representationConstituee` (select AVOCAT/DEFENSEUR_SYNDICAL/AUCUNE), `jugementEnDernierRessort` (checkbox).
- Résultat : `dateLimiteAppel` + `joursRestants` (JetBrains Mono), badge verdict (`DELAI_OUVERT` vert / `DELAI_URGENT` or / `DELAI_EXPIRE` rouge / `VOIE_FERMEE` navy avec lien F-DT-87), checklist formalités (liste avec items obligatoires/bloquants), `baseJuridique` (JetBrains Mono).
- CONTEXTUAL : apparaît si flag IA `appel_cph_envisage` = true. Groupement thématique `CONTENTIEUX`.
- Pré-fill : `dateNotificationJugement` depuis `TravailExtractedData`.

---

## Conformité F-IA-04

- [x] Cohérence visuelle (rouge réservé `DELAI_EXPIRE` ; navy/or pour info ; `<input type="date">` ; JetBrains Mono baseJuridique ; bannière gate FR ; MatSnackBar ; refresh dashboard non requis — outil de calcul sans action validée impactant les cards)
- [x] Pré-fill IA : `@Input() aiData?: TravailExtractedData`, `prefillFromAi()` dans ngOnInit + ngOnChanges, `provenanceDateNotification` signal + badge `auto_awesome` + handler `onDateNotificationChange()`
- [x] Validation F-IA-03 : `coherenceAlerts` computed sur `dateNotificationJugement` croisé aiData / procedureChecks F-96 ; `CoherenceAlertBuilder` partagé ; popover trigger
- [x] TOOL_REGISTRY symétrique + `getPrefillCount(input)` (0 / partiel / nominal) ; `F-DT-86-appel-cph-cour-appel` dans `KNOWN_FRONTEND_TOOL_IDS`
- Niveau outil : 3 (calculateur de délai + checklist) → parité domaines **non applicable** (niveau < 5 ; procédure d'appel CPH = mécanisme FR-only, pas d'équivalent immigration/famille pertinent)

---

## Critères d'acceptation

- [ ] BUILD SUCCESS 0 erreur TypeScript
- [ ] POST `jugementEnDernierRessort=true` → verdict `VOIE_FERMEE` avec lien vers F-DT-87
- [ ] POST notification J-29 → badge `DELAI_URGENT` (or)
- [ ] `dateNotificationJugement` pré-rempli depuis `TravailExtractedData` avec badge provenance
- [ ] `representationConstituee=AUCUNE` → item checklist bloquant mis en avant
- [ ] Tests Jest ≥ 12 (rendu form, verdicts, pré-fill, getPrefillCount 0/partiel/nominal, gate FR)

## Tables / endpoints / composants impactés

- **Nouveau composant** `AppelCphSectionComponent`
- **Nouveau service** `AppelCphService`
- **Nouveau modèle** `AppelCphAnalysis`
- **Modification** `decisional-tools-panel.component.ts` (TOOL_REGISTRY + TOOL_LABEL + TOOL_ICON)

## Dépendances

- SF-218-01 : statut `done`

## Hors périmètre

- Génération de la déclaration d'appel (export RPVA)
