# Mini-spec — F-IM-01 / SF-IM-01-03 Export PDF checklist pièces immigration

## Identifiant

`F-IM-01 / SF-IM-01-03`

## Feature parente

`F-IM-01` — Checklist pièces par type de titre de séjour

## Statut

`in-progress`

## Date de création

2026-04-04

## Branche Git

`feat/SF-IM-01-03-export-pdf-checklist-pieces`

---

## Objectif

Ajouter un bouton "Exporter PDF" dans `ImmigrationChecklistSectionComponent` qui génère via pdfmake un PDF de la checklist affichant le type de titre, le pays, la liste des pièces avec leur statut coloré, et un résumé des compteurs (présentes / absentes / inconnues).

---

## Comportement attendu

### Cas nominal

1. L'avocat a chargé une checklist (type + pays sélectionnés, pièces affichées).
2. Il clique "Exporter PDF".
3. `PdfExportService.exportImmigrationChecklist(checklist, caseFileTitle)` est appelé.
4. pdfmake génère et télécharge le PDF avec :
   - Logo + titre du dossier + sous-titre "Checklist pièces immigration"
   - Type de titre + pays
   - Résumé compteurs : N présente(s) · N absente(s) · N inconnue(s)
   - Liste des pièces avec icône statut (✔ / ✖ / ?) et libellé
   - Date d'export
5. Nom du fichier : `checklist-pieces-{slug-titre}-{YYYY-MM-DD}.pdf`

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `checklist()` null | Bouton désactivé (non cliquable) |
| checklist vide (0 pièces) | PDF généré avec message "Aucune pièce" |

---

## Critères d'acceptation

- [ ] Bouton "Exporter PDF" présent dans la section, désactivé si `checklist()` null
- [ ] PDF généré avec logo, titre dossier, type de titre, pays, date d'export
- [ ] Résumé compteurs présent (présentes / absentes / inconnues)
- [ ] Chaque pièce affichée avec son statut
- [ ] Nom de fichier conforme : `checklist-pieces-{slug}-{date}.pdf`
- [ ] Import pdfmake dynamique (pattern existant)

---

## Périmètre

### Hors scope

- Export Word
- Personnalisation du PDF par l'utilisateur
- Envoi par email

---

## Technique

### Méthodes ajoutées à `PdfExportService`

- `exportImmigrationChecklist(checklist: ImmigrationChecklist, caseFileTitle: string): void`
- `buildImmigrationChecklistDocument(checklist, caseFileTitle): object` — public pour test
- `buildImmigrationChecklistFileName(title: string): string` — public pour test

### Composant modifié

`ImmigrationChecklistSectionComponent` — ajout `@Input() caseFileTitle: string = ''` et bouton "Exporter PDF".

### Intégration

`CaseFileDetailComponent` — passer `[caseFileTitle]="caseFile()!.title"` au composant.

---

## Plan de test

### Tests unitaires

- [ ] `PdfExportService` — `buildImmigrationChecklistDocument` retourne un objet avec `content`
- [ ] `PdfExportService` — `buildImmigrationChecklistFileName` génère le bon slug + date
- [ ] `PdfExportService` — checklist avec pièces PRESENT/ABSENT/INCONNU → contenu correct
- [ ] `ImmigrationChecklistSectionComponent` — bouton désactivé si checklist null
- [ ] `ImmigrationChecklistSectionComponent` — clic bouton → appel exportImmigrationChecklist

### Isolation workspace / intégration

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — extension isolée de PdfExportService

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné

---

## Dépendances

- SF-IM-01-02 — statut : done ✅

---

## Notes et décisions

- Pattern identique à `exportChecklist` (F-107) — import dynamique pdfmake, même structure document.
- `buildImmigrationChecklistDocument` et `buildImmigrationChecklistFileName` sont publics pour être testables sans mock pdfmake.
