# SF-42-02 — Frontend : bouton export CSV journal d'actions

**Feature parente :** F-42 — Export CSV journal d'actions
**Statut :** En cours
**Estimation :** < 0.5 jour

---

## Objectif

Ajouter un bouton "Exporter CSV" dans l'écran `/workspace/audit-logs` qui déclenche le téléchargement du fichier CSV généré par SF-42-01.

---

## Comportement nominal

1. L'utilisateur OWNER ou ADMIN voit un bouton "Exporter CSV" dans le header de l'écran
2. Il clique → le navigateur lance un téléchargement nommé `audit-log.csv`
3. Le fichier téléchargé contient toutes les entrées (pas seulement les 50 affichées)
4. Un spinner temporaire s'affiche pendant le téléchargement
5. En cas d'erreur, un snackbar d'erreur s'affiche

---

## Cas d'erreur

| Situation | Réponse |
|-----------|---------|
| Erreur réseau ou 403 | Snackbar "Erreur lors de l'export." |
| Téléchargement en cours (double clic) | Bouton désactivé pendant l'export |

---

## Critères d'acceptation

- [ ] Bouton "Exporter CSV" visible dans le header de `AuditLogScreenComponent`
- [ ] Clic déclenche `GET /api/v1/admin/audit-logs/export.csv` et télécharge le fichier
- [ ] Bouton désactivé pendant l'export (signal `exporting`)
- [ ] Spinner sur le bouton pendant l'export
- [ ] Snackbar d'erreur si l'appel échoue

---

## Plan de test

### Unitaires (AuditLogScreenComponent spec)
- `T-01` : clic sur "Exporter CSV" appelle `auditLogService.exportCsv()`
- `T-02` : le bouton est désactivé pendant l'export (`exporting = true`)
- `T-03` : snackbar affiché si export échoue

---

## Composants impactés

- `AuditLogService` : nouvelle méthode `exportCsv()` → `Observable<Blob>`
- `AuditLogScreenComponent` : signal `exporting`, méthode `exportCsv()`, bouton dans le template

---

## Hors périmètre

- Export des logs filtrés uniquement (toujours l'export complet)
- Filtrage par dates (F-43)
