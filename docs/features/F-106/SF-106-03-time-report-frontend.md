# Mini-spec — F-106 / SF-106-03 — Suivi du temps facturable — rapport mensuel frontend

## Identifiant
`F-106 / SF-106-03`

## Feature parente
`F-106` — Suivi du temps facturable par dossier

## Statut
`draft`

## Date de création
2026-04-03

## Branche Git
`feat/SF-106-03-time-report-frontend`

---

## Objectif

Créer la page `/workspace/time-report` affichant le rapport mensuel des heures facturables par dossier, avec sélecteur de mois, tableau récapitulatif et bouton d'export CSV.

---

## Comportement attendu

### Cas nominal

**Page `/workspace/time-report`**
- Accessible via un lien "Rapport de temps" dans `/workspace/admin` (à côté des autres sections existantes).
- Route Angular `workspace/time-report`, protégée (auth guard existant), accessible à tous les membres du workspace.
- En-tête : titre "Rapport de temps facturable", sélecteur de mois (mois courant par défaut, mois précédents accessibles jusqu'à 12 mois en arrière).
- Chargement automatique lors du changement de mois → appel `GET /api/v1/workspace/time-report?month=YYYY-MM`.

**Tableau récapitulatif**
- Colonnes : Dossier, Utilisateur, Durée (formatée "Xh Ymin"), Taux horaire (€/h), Montant (€).
- Trié par Dossier ASC, puis Utilisateur ASC.
- Si aucune entrée pour le mois → message vide "Aucune session enregistrée pour ce mois."
- Ligne totale en bas : somme des durées et somme des montants.

**Export CSV**
- Bouton "Exporter CSV" en haut à droite → appel `GET /api/v1/workspace/time-report/export?month=YYYY-MM` → déclenchement du téléchargement navigateur (`Content-Disposition: attachment`).
- Feedback MatSnackBar pendant le téléchargement ("Export en cours…") puis "Export terminé" au succès.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Erreur réseau / 500 au chargement | Message d'erreur inline : "Impossible de charger le rapport. Réessayez." |
| Erreur export CSV | Toast erreur : "Erreur lors de l'export." |
| Mois sans entrée | Message vide "Aucune session enregistrée pour ce mois." |

---

## Critères d'acceptation

- [ ] Route `/workspace/time-report` accessible et protégée (auth guard)
- [ ] Lien vers la page dans `/workspace/admin`
- [ ] Sélecteur de mois (mois courant par défaut, 12 mois en arrière)
- [ ] Tableau avec colonnes : Dossier, Utilisateur, Durée, Taux horaire, Montant
- [ ] Ligne totale (durée totale + montant total)
- [ ] Message vide si aucune session
- [ ] Bouton "Exporter CSV" déclenche le téléchargement
- [ ] Feedback MatSnackBar sur l'export
- [ ] Smoke tests navigation 6/6 après ajout de la route
- [ ] Couleurs et composants conformes au design system

---

## Périmètre

### Hors scope
- Insight IA dans la synthèse (SF-106-04)
- Modification / suppression manuelle d'une session
- Filtrage par utilisateur (tableau montre toutes les lignes du workspace)
- Pagination (max ~50 lignes par mois suffisant pour V4)

---

## Technique

### Composants Angular

- `TimeReportComponent` (standalone) — page complète avec sélecteur mois, tableau, export. Nouvelle route `workspace/time-report`.
- `TimeReportService` — `getReport(month: string): Observable<TimeReportRow[]>`, `exportCsv(month: string): Observable<Blob>`.
- Ajout du lien dans `WorkspaceAdminComponent` (section existante, lien mat-stroked-button).
- Ajout de la route dans `app.routes.ts` (lazy-loaded, même guard que les autres routes workspace).
- Ajout du lien dans la sidenav `ShellComponent` (icône `schedule`, libellé "Rapport de temps").

### Modèle de données (réponse API)

```typescript
interface TimeReportRow {
  caseFileId: string;
  caseFileTitle: string;
  userId: string;
  userDisplayName: string;
  totalSeconds: number;
  ratePerHour: number | null;
  totalAmount: number | null;
}
```

### Endpoints consommés (SF-106-01)
- `GET /api/v1/workspace/time-report?month=YYYY-MM`
- `GET /api/v1/workspace/time-report/export?month=YYYY-MM`

### Migration Liquibase
- [x] Non applicable (frontend only)

---

## Plan de test

### Tests unitaires

- [ ] `TimeReportService` — `getReport()` appelle le bon endpoint avec le bon mois
- [ ] `TimeReportService` — `exportCsv()` appelle le bon endpoint et retourne un Blob
- [ ] `TimeReportComponent` — affiche le tableau avec les lignes
- [ ] `TimeReportComponent` — affiche le message vide si aucune entrée
- [ ] `TimeReportComponent` — sélecteur de mois déclenche un rechargement
- [ ] `TimeReportComponent` — clic "Exporter CSV" appelle exportCsv()
- [ ] formatDuration() — 3661s → "1h 01min"

### Isolation workspace
- [ ] Non applicable — isolation vérifiée côté backend (SF-106-01)

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing frontend** — nouvelle route `workspace/time-report` + lien sidenav

### Composants / endpoints existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|----------------------|
| `app.routes.ts` | Ajout d'une route — pas de modification des routes existantes | Smoke tests navigation |
| `ShellComponent` | Ajout d'un lien sidenav | Smoke tests navigation |
| `WorkspaceAdminComponent` | Ajout d'un lien — pas de modification du contenu existant | Tests existants doivent rester verts |

### Smoke tests E2E concernés
- [ ] `e2e/smoke/navigation.spec.ts` — routes protégées toujours fonctionnelles

---

## Dépendances

### Subfeatures bloquantes
- SF-106-01 — statut : done ✅
- SF-106-02 — statut : done ✅

---

## Notes et décisions

- **Export CSV** : utiliser `window.URL.createObjectURL(blob)` + clic programmatique sur un `<a>` temporaire pour déclencher le téléchargement sans ouvrir de nouvel onglet. Nettoyer l'URL après.
- **Sélecteur de mois** : utiliser `<input matInput [matDatepicker]>` de type `month` ou un `mat-select` avec les 13 derniers mois (mois courant inclus). Préférer `mat-select` pour la compatibilité cross-browser.
- **Taux null** : si `ratePerHour = null` (utilisateur sans taux configuré), afficher "—" dans les colonnes Taux et Montant.
- **Ligne totale** : calculée côté frontend à partir des lignes reçues (pas d'endpoint dédié).
