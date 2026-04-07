# Mini-spec — F-IM-06 / SF-IM-06-03 Interface formulaire, prévisualisation et export PDF

---

## Identifiant

`F-IM-06 / SF-IM-06-03`

## Feature parente

`F-IM-06` — Générateur de recours préfectoral / CGRA

## Statut

`draft`

## Date de création

2026-04-07

## Branche Git

`feat/SF-IM-06-03-frontend-recours`

---

## Objectif

Créer un composant Angular avec formulaire (type de recours, date notification, requérant, décision contestée, faits), prévisualisation du document généré, et export PDF. Section conditionnelle DROIT_IMMIGRATION.

---

## Comportement attendu

### Cas nominal

1. Section "Recours immigration" collapsible dans le dossier DROIT_IMMIGRATION
2. Si aucun recours n'existe → formulaire (type de recours, date notification, requérant, décision contestée, exposé des faits)
3. POST au clic "Générer le recours" → prévisualisation du document structuré
4. Si un recours existe → GET au chargement → prévisualisation directe
5. Bouton "Modifier" pour revenir au formulaire pré-rempli
6. Bouton "Exporter PDF" pour télécharger le document

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur API | MatSnackBar |
| Date limite dépassée | Bandeau avertissement rouge |

---

## Critères d'acceptation

- [ ] Section visible uniquement pour DROIT_IMMIGRATION
- [ ] Formulaire avec tous les champs (mat-form-field outline)
- [ ] POST génère et affiche le document
- [ ] GET charge un recours existant
- [ ] Prévisualisation affiche en-tête, visa, faits, moyens, conclusions, pièces, date limite
- [ ] Avertissement si date limite dépassée
- [ ] Export PDF via pdfmake
- [ ] Design system respecté, France + Belgique

---

## Technique

### Composants : `ImmigrationRecoursSectionComponent`
### Service : `ImmigrationRecoursService` (HTTP)
### Modèle : interfaces TypeScript pour request/response
### Intégration : `@if DROIT_IMMIGRATION` dans case-file-detail, après title-decision

---

## Plan de test

- [ ] Composant créé sans erreur
- [ ] GET appelé au ngOnInit
- [ ] Formulaire affiché si pas de recours
- [ ] POST appelé au clic "Générer"
- [ ] Prévisualisation affichée après génération

## Analyse d'impact

- [x] Aucune préoccupation transversale

## Dépendances

- SF-IM-06-01 — done
- SF-IM-06-02 — done
