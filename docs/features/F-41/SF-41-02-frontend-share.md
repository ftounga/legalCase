---
id: SF-41-02
feature: F-41
title: Frontend — bouton Partager + page publique /share/{token}
status: In Progress
---

## Objectif

Permettre à l'avocat de partager un dossier via un lien, et au visiteur de consulter la synthèse sans compte.

## Composants

1. CaseFileShareService (Angular) — appels API
2. ShareDialogComponent — dialog durée + lien + copie + révocation
3. Bouton "Partager" dans case-file-detail (section Synthèse)
4. PublicShareComponent — route /share/:token, hors shell, sans authGuard

## Comportement nominal

- Bouton "Partager" visible quand synthèse DONE existe
- Clic → dialog : sélecteur 7/14/30 jours + bouton Générer
- Après génération : champ URL + bouton Copier (Clipboard API)
- Liste des liens actifs avec bouton Révoquer
- /share/:token accessible sans auth, affiche titre + domaine + synthèse
- Token invalide/expiré → message d'erreur clair

## Critères d'acceptation

1. Bouton Partager visible uniquement si synthèse disponible
2. Dialog : génération lien + copie clipboard
3. Dialog : liste actifs + révocation
4. /share/:token accessible sans auth, affiche la synthèse
5. Token invalide → message erreur lisible
6. Page publique responsive

## Plan de test

- T-01 : ShareDialogComponent — appelle createShare au clic Générer
- T-02 : ShareDialogComponent — affiche lien généré après succès
- T-03 : PublicShareComponent — appelle getPublicShare(token) au init
- T-04 : PublicShareComponent — affiche titre + synthèse si 200
- T-05 : PublicShareComponent — affiche message erreur si 404

## Hors périmètre

- Statistiques de consultation
- Notifications d'expiration
