# Mini-spec — F-148 / SF-148-03 Rename LegalCase Vision + indicateur progress async

## Identifiant · `F-148 / SF-148-03`
## Date · `2026-04-23` · Branche · `feat/SF-148-03-vision-progress-rename`

## Objectif
Deux points suite retour staging :
1. Remplacer "Claude Vision" par **"LegalCase Vision"** partout dans l'UI — positionnement produit (on est un produit LegalCase, pas un wrapper Claude).
2. Afficher un **indicateur visuel d'enrichissement en cours** pendant que vision tourne en async, pour que l'avocat comprenne pourquoi la description visuelle n'apparaît pas immédiatement après l'upload.

## Comportement nominal

### A — Rename UI
Remplacer toutes les occurrences de "Claude Vision" / "Vision Claude" par "LegalCase Vision" :
- Badge banner pièce : "Vision" (inchangé — court)
- Tooltip sidebar icône : "Description visuelle LegalCase Vision disponible"
- Tooltip banner badge : "LegalCase Vision a analysé cette pièce"
- Titre panneau : "DESCRIPTION VISUELLE (LEGALCASE VISION)"

### B — Statut enrichissement par pièce
Nouveau champ `visionStatus` sur `DocumentPiece` avec 4 valeurs :
- `NOT_APPLICABLE` (défaut) — pièce non éligible à vision (blacklist, etc.)
- `PENDING` — vision en cours (appel Anthropic démarré, pas encore fini)
- `DONE` — description persistée avec succès
- `FAILED` — erreur lors de l'appel (fail-open déjà en place, mais on trace)

`VisionEnrichmentService.enrichDocument` :
1. Phase 1 (transactionnelle courte) — pour chaque pièce éligible : `visionStatus = PENDING`, save, commit.
2. Phase 2 — pour chaque pièce `PENDING` : appel Anthropic, update status `DONE`/`FAILED` + `visualDescription`, save.

### C — Expose le status au frontend
`DocumentPieceSummary` expose `visionStatus`. Frontend :
- Sidebar DocumentPreviewDialog : si `visionStatus === 'PENDING'` → spinner à la place de l'icône œil, tooltip "Analyse visuelle en cours…"
- Banner pièce sélectionnée : si PENDING → badge gris avec spinner "En cours…" au lieu du badge "Vision" doré
- Encart description : si PENDING et pas encore de description → affiche "Analyse visuelle en cours…" avec spinner, au lieu de l'encart doré

## Critères d'acceptation
- [ ] Migration 100 : colonne `vision_status` varchar(20) NOT NULL default `NOT_APPLICABLE` sur `document_pieces`
- [ ] Enum `VisionStatus` + field sur `DocumentPiece`
- [ ] `DocumentPieceSummary` expose `visionStatus`
- [ ] `VisionEnrichmentService` : phase PENDING/DONE/FAILED appliquée
- [ ] Frontend : spinner sidebar + banner + encart si PENDING
- [ ] Frontend : labels renommés "LegalCase Vision"
- [ ] Tests unitaires : service gère les 3 transitions
- [ ] Suite backend + frontend verte

## Plan de test minimal
- U-01 : enrichDocument marque eligible pieces PENDING avant l'appel
- U-02 : succès → DONE + visualDescription
- U-03 : échec Anthropic → FAILED, pas de visualDescription
- U-04 : non éligible → NOT_APPLICABLE inchangé
- U-05 : piece déjà DONE → skippée (idempotence)
- U-06 frontend : PENDING → spinner visible, description pas affichée
- U-07 frontend : DONE avec description → encart affiché

## Impact par domaine métier
Neutre — la feature est agnostique au domaine (le status est un état technique).

## Hors scope
- Polling auto frontend pour rafraîchir les status (v1 : utilisateur close+reopen la popup pour voir l'update). À ajouter si feedback UX.
- Re-trigger manuel d'une pièce FAILED.
