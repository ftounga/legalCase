# Mini-spec — F-172 / SF-172-02 — Frontend : badge "Événement programmé" sur événements futurs

## Identifiant

`F-172 / SF-172-02`

## Feature parente

`F-172` — Élargissement détection événements déclencheurs immigration FR aux faits imminents documentés

## Statut

`draft`

## Date de création

2026-04-30

## Branche Git

`feat/SF-172-02-frontend-event-programme-badge`

---

## Objectif

Afficher un **badge subtil "Événement programmé"** à côté de la date sur la carte d'événement déclencheur immigration (`<app-immigration-events-section>`) quand `event_date > today`, pour que l'avocat distingue d'un coup d'œil un événement acquis (mariage célébré) d'un événement à venir (soutenance prévue).

---

## Comportement attendu

### Cas nominal

1. Le dossier Chen contient un événement détecté par l'IA : `{ event_code: "DOCTORAT_OBTENU", event_label: "Doctorat obtenu ou soutenance programmée en France", event_date: "2026-10-15", ... }`
2. Le composant `<app-immigration-events-section>` affiche la carte avec icône, date, message, base légale.
3. **Comparaison `event_date > today`** : `2026-10-15 > 2026-04-30` → vrai → badge "Événement programmé" affiché à droite de la date (ou en dessous selon mobile).
4. Pour un mariage avec `event_date: "2025-03-15"` (passé), aucun badge supplémentaire n'apparaît (comportement actuel).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| `event_date` null | Pas de badge (impossible de comparer) |
| `event_date` égale à today (à la journée près) | Pas de badge (par défaut on considère l'événement déjà acquis) |
| Format de date invalide | Pas de badge + console.warn (fail-open) |

---

## Contrat (importé de SF-172-01)

Le backend continue d'envoyer `event_date` au format `YYYY-MM-DD`. Aucun nouveau champ requis. Cette SF ne dépend que du backend tel qu'il existe — elle peut donc être mergée avant ou après SF-172-01 sans casser quoi que ce soit (l'effet visuel n'est notable qu'une fois SF-172-01 mergée et que des événements imminents sont détectés).

---

## Analyse de cohérence transversale

### Périmètres scannés

- **Autres composants affichant des dates** : F-IM-08 (OQTF avec/sans délai — délais procéduraux à respecter), F-DT-03 (prescription), F-FA-12 (mesures provisoires), F-DT-25 (préavis). Aucun n'a un concept "événement programmé" similaire — tous gèrent des deadlines à respecter, pas des événements futurs ouvrant un droit. Pattern différent, pas d'harmonisation requise.
- **Autres pays** : Belgique. F-IM-14 (9bis, 9ter, 40bis, 40ter) gère ses propres outils décisionnels — pas concerné par cette SF (immigration FR uniquement).
- **Pattern UI** : badge inline à côté d'une date. Pattern simple, pas un nouveau composant partagé. Si d'autres composants veulent un badge "futur" plus tard, on extraira `<app-future-event-badge>` à ce moment-là.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| `immigration-events-section` | Oui | Modifié dans cette SF |
| Autres composants avec date | Non | Pattern différent (deadlines vs événements ouvrant un droit) |
| Création composant partagé `<app-future-event-badge>` | Non | YAGNI — un seul cas d'usage aujourd'hui, extraction future si autre besoin |

### Décision

- [x] Modifié pour la cible directe (immigration-events-section)
- [x] Pas de pattern partagé créé (un seul cas d'usage)
- [x] Pas d'extension à d'autres composants (pattern différent)

---

## Impact par domaine métier

**Immigration France uniquement.** Les événements déclencheurs (F-150) sont immigration FR. Belgique : F-IM-14 a ses propres mécanismes. Travail / Famille : non concernés.

---

## Critères d'acceptation

- [ ] **C1** — Sur la carte `<app-immigration-events-section>`, le badge "Événement programmé" apparaît à droite de la date quand `event_date > today`
- [ ] **C2** — Le badge n'apparaît PAS quand `event_date <= today` ou `event_date` est null
- [ ] **C3** — Le badge respecte le DESIGN_SYSTEM.md : palette navy/or, typographie Inter, taille discrète (12-13px), padding 4-8px multiples de 4
- [ ] **C4** — Le badge est purement informatif (pas de tooltip, pas d'action) — discret pour ne pas distraire
- [ ] **C5** — Tests Jest : la logique de comparaison `event_date > today` est testée avec dates passé / présent / futur / null / invalide
- [ ] **C6** — Aucune régression sur les autres parties du composant (icône, base légale, suggested title)

---

## Périmètre

### Hors scope

- Backend (couvert par SF-172-01)
- Tooltip détaillé sur le badge
- Filtre / tri par "événements à venir vs acquis"
- Compteur agrégé "N événements programmés" en haut de section

---

## Technique

### Composant impacté

`frontend/src/app/case-files/immigration-events-section/immigration-events-section.component.ts` (+ `.html`, `.scss`, `.spec.ts`)

### Logique

```typescript
isProgrammedEvent(eventDate: string | null): boolean {
  if (!eventDate) return false;
  const parsed = Date.parse(eventDate);
  if (isNaN(parsed)) return false;
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  return parsed > today.getTime();
}
```

### Style SCSS (suggestion)

```scss
.event-programmed-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: #FFF8E1; // fond or clair DESIGN_SYSTEM
  color: #1A3A5C; // navy DESIGN_SYSTEM
  border: 1px solid #C9973A; // or DESIGN_SYSTEM
  border-radius: 4px;
  font-family: 'Inter', sans-serif;
  font-size: 12px;
  font-weight: 500;
  margin-left: 8px;
}
```

### Endpoints / tables

Aucun changement.

### Migration Liquibase

Non applicable.

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `isProgrammedEvent('2027-01-15')` → true (date future)
- [ ] `isProgrammedEvent('2025-01-15')` → false (date passée)
- [ ] `isProgrammedEvent(null)` → false
- [ ] `isProgrammedEvent('invalid')` → false
- [ ] `isProgrammedEvent(today.toISOString().slice(0,10))` → false (égalité)
- [ ] Spec composant : badge présent/absent dans le DOM selon la date

### Tests d'intégration

Non applicable (composant pure presentation).

### Isolation workspace

Non applicable.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal — non
- [ ] Workspace context — non
- [ ] Plans / limites — non
- [ ] Navigation / routing — non
- [x] Aucune préoccupation transversale

### Smoke tests E2E

Aucun smoke test concerné — modification visuelle isolée.

---

## Dépendances

### Subfeatures bloquantes

Aucune. Démarrable immédiatement en parallèle avec SF-172-01. La SF est utile dès aujourd'hui même sans SF-172-01 (il y a déjà des événements déclencheurs détectés sur certains dossiers, certains avec date future).

---

## Notes et décisions

- **Décision** : pas d'extraction en composant partagé `<app-future-event-badge>`. YAGNI — un seul cas d'usage. Si d'autres composants veulent un pattern similaire plus tard, on extrait à ce moment-là.
- **Décision** : badge informatif simple, pas de tooltip ni d'action. Évite la sur-ingénierie.
- **Décision** : seuil de comparaison = `event_date > today` (strict). Un événement le jour même est considéré comme acquis pour ne pas ajouter de bruit.
