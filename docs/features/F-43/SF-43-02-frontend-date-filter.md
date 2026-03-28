# SF-43-02 — Frontend : filtres date début / date fin dans le journal d'actions

**Feature parente :** F-43 — Filtre par plage de dates — journal d'actions
**Statut :** En cours
**Estimation :** < 0.5 jour

---

## Objectif

Ajouter deux champs de date (date début / date fin) dans la barre de filtres de `AuditLogScreenComponent`. La sélection d'une date recharge les logs depuis le serveur en passant les params `?from=ISO&to=ISO`.

---

## Comportement nominal

1. L'utilisateur voit deux champs `<input type="date">` dans la barre de filtres : "Du" et "Au"
2. Modifier une date déclenche un rechargement des logs (`ngOnInit` → `loadAuditLogs()`)
3. Les dates saisies sont converties en ISO 8601 (début de journée en UTC pour `from`, fin de journée en UTC pour `to`)
4. Vider une date supprime le filtre correspondant
5. Les filtres texte et action restent côté client sur les résultats retournés par le serveur

---

## Cas d'erreur

| Situation | Réponse |
|---|---|
| `from > to` | Le backend retourne 400 → snackbar "Dates invalides" |
| Erreur réseau | Snackbar générique (comportement inchangé) |

---

## Critères d'acceptation

- [ ] Deux champs date ("Du" / "Au") visibles dans la barre de filtres
- [ ] Sélectionner une date recharge les logs depuis le serveur
- [ ] Vider une date relance un chargement sans ce filtre
- [ ] `from > to` → snackbar "Dates invalides"
- [ ] Filtres texte/action continuent de fonctionner sur les résultats filtrés

---

## Plan de test

### Unitaires (AuditLogScreenComponent spec)
- `T-12` : sélectionner une date `from` recharge les logs (appelle `auditLogService.getAuditLogs`)
- `T-13` : `from > to` → snackbar "Dates invalides"
- `T-14` : vider `from` recharge les logs sans ce paramètre

---

## Composants impactés

- `AuditLogService.getAuditLogs(from?, to?)` : ajout des params optionnels
- `AuditLogScreenComponent` : signals `dateFrom`/`dateTo`, méthode `loadLogs()` centralisée
- Template : 2 champs `<input type="date" matInput>` dans `.filters-row`

---

## Hors périmètre

- Validation côté frontend de `from > to` avant envoi (le backend renvoie 400 — traité dans le error handler)
- Filtre action côté serveur (F-44)
- Pagination des résultats filtrés (F-44)
