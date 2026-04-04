# Mini-spec — F-DT-04 / SF-DT-04-03 — Export PDF fiche prud'homale

---

## Identifiant

`F-DT-04 / SF-DT-04-03`

## Feature parente

`F-DT-04` — Génération fiche prud'homale

## Statut

`ready`

## Date de création

2026-04-04

## Branche Git

`feat/SF-DT-04-03-export-pdf-fiche-prudhomale`

---

## Objectif

Générer un PDF structuré de la fiche prud'homale depuis le formulaire Angular via pdfmake, avec un bouton dédié dans la section.

---

## Comportement attendu

### Cas nominal

- Bouton "Exporter PDF" dans la section fiche, à côté de "Enregistrer"
- Clique → appelle `PdfExportService.exportPrudhomeFiche(fiche, caseFileTitle)`
- Construit le document PDF avec pdfmake et déclenche le téléchargement
- Nom du fichier : `fiche-prudhomale-{titre-dossier-slug}.pdf`
- Sections du PDF :
  1. En-tête : titre du dossier + date de génération
  2. Demandeur (nom, prénom, adresse, téléphone, email, profession)
  3. Défendeur (nom, adresse, SIRET, représentant)
  4. Demandes chiffrées — tableau Intitulé / Montant (€)
  5. Exposé des faits
  6. Moyens de droit
  7. Bordereau de pièces — liste numérotée

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Formulaire invalide (nom demandeur vide) | Bouton désactivé |
| Erreur pdfmake | Toast erreur |

---

## Critères d'acceptation

- [ ] Bouton "Exporter PDF" visible dans la section fiche
- [ ] Bouton désactivé si formulaire invalide
- [ ] PDF généré contient toutes les sections
- [ ] Tableau demandes avec colonnes Intitulé / Montant
- [ ] Bordereau de pièces numéroté
- [ ] Nom de fichier : `fiche-prudhomale-{slug}.pdf`

---

## Périmètre

### Hors scope (explicite)

- Export Word/DOCX
- Envoi par email
- Signature électronique
- Sauvegarde automatique avant export

---

## Contraintes de validation

| Champ | Règle |
|-------|-------|
| Bouton export | Désactivé si `form.invalid` |
| Montant demande | Affiché "—" si null |

---

## Technique

### Endpoints consommés

Aucun nouveau endpoint — utilise les données du formulaire en mémoire.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `PrudhomeFicheSectionComponent` — ajout du bouton export + appel service
- `PdfExportService` — ajout méthode `exportPrudhomeFiche()`

---

## Plan de test

### Tests unitaires PdfExportService

- [ ] `exportPrudhomeFiche` — toutes les sections présentes dans le document
- [ ] Tableau demandes — colonnes Intitulé + Montant
- [ ] Pièces numérotées dans le bordereau
- [ ] Montant null affiché "—"

### Isolation workspace

Non applicable (export local, pas d'accès réseau).

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune préoccupation transversale** — subfeature isolée, impact limité à son périmètre

### Smoke tests E2E concernés

- Aucun smoke test concerné (génération PDF côté client, pas de navigation ni auth).

---

## Dépendances

### Subfeatures bloquantes

- SF-DT-04-02 — statut : done (mergée)

### Questions ouvertes impactées

- Aucune

---

## Notes et décisions

- Pattern identique à `PdfExportService.exportChecklist()` existant (pdfmake)
- Les données proviennent du formulaire Angular en mémoire (pas de re-fetch)
- Slug du titre dossier : remplacement des espaces/caractères spéciaux par `-`
