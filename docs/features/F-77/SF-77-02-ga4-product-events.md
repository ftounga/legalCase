# Mini-spec — F-77 / SF-77-02 Tracking événements produit GA4

---

## Identifiant

`F-77 / SF-77-02`

## Feature parente

`F-77` — Google Analytics 4 — tracking + bannière consentement RGPD

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-77-02-ga4-product-events`

---

## Objectif

Envoyer des événements GA4 personnalisés sur les actions produit clés (analyse lancée, PDF exporté, upgrade cliqué) pour mesurer le funnel de conversion, en respectant le consentement RGPD déjà en place.

---

## Comportement attendu

### Cas nominal

1. L'utilisateur a donné son consentement GA4 (ConsentService → `granted`).
2. Il effectue une action clé :
   - Lance une analyse initiale → événement `analysis_launched` (`type: "STANDARD"`)
   - Lance une re-analyse enrichie → événement `analysis_launched` (`type: "ENRICHED"`)
   - Exporte un PDF depuis la synthèse → événement `pdf_exported`
   - Clique sur un CTA upgrade Pro → événement `upgrade_clicked`
3. L'`AnalyticsService` appelle `window.gtag('event', ...)` si GA4 est chargé.
4. L'événement remonte dans GA4 et est visible dans les rapports d'événements.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| GA4 non chargé (consentement non donné) | `gtag` absent → appel ignoré silencieusement (fail-open) |
| `window.gtag` lève une exception | Try/catch → pas de propagation, l'action produit continue |

---

## Critères d'acceptation

- [ ] `AnalyticsService` créé avec méthode `trackEvent(name, params?)` — fail-open si `gtag` absent
- [ ] `analysis_launched` envoyé dans `CaseFileDetailComponent` au lancement d'analyse initiale
- [ ] `analysis_launched` envoyé dans `SynthesisComponent` au lancement de re-analyse enrichie
- [ ] `pdf_exported` envoyé dans `SynthesisComponent.exportPdf()` en cas de succès
- [ ] `upgrade_clicked` envoyé dans `WorkspaceBillingComponent` + `TrialBannerComponent` au clic CTA upgrade
- [ ] Aucun événement envoyé si consentement non donné (GA4 non chargé)
- [ ] Tests unitaires `AnalyticsService` — avec et sans `window.gtag`
- [ ] Tests unitaires composants — vérifient que `trackEvent` est appelé au bon moment

---

## Périmètre

### Hors scope
- Dashboard GA4 (M-50 — tâche séparée de configuration GA4)
- Tracking backend (server-side events)
- Nouveaux événements au-delà des 4 définis

---

## Technique

### Tables impactées
Aucune — 100% frontend.

### Migration Liquibase
Non applicable.

### Composants impactés

| Composant | Modification |
|-----------|-------------|
| `AnalyticsService` (nouveau) | `trackEvent(name, params?)` — wrap `gtag`, fail-open |
| `CaseFileDetailComponent` | Appel `trackEvent('analysis_launched', {type: 'STANDARD'})` |
| `SynthesisComponent` | Appel `trackEvent('analysis_launched', {type: 'ENRICHED'})` + `trackEvent('pdf_exported')` |
| `WorkspaceBillingComponent` | Appel `trackEvent('upgrade_clicked')` au clic CTA |
| `TrialBannerComponent` | Appel `trackEvent('upgrade_clicked')` au clic CTA |

---

## Plan de test

### Tests unitaires
- [ ] `AnalyticsService.trackEvent` — `gtag` présent → appelé avec les bons paramètres
- [ ] `AnalyticsService.trackEvent` — `gtag` absent → pas d'exception
- [ ] `AnalyticsService.trackEvent` — `gtag` lève une exception → avalée silencieusement
- [ ] `CaseFileDetailComponent` — lancement analyse → `trackEvent` appelé
- [ ] `SynthesisComponent` — lancement re-analyse → `trackEvent` appelé
- [ ] `SynthesisComponent` — export PDF succès → `trackEvent('pdf_exported')` appelé
- [ ] `WorkspaceBillingComponent` — clic upgrade → `trackEvent('upgrade_clicked')` appelé
- [ ] `TrialBannerComponent` — clic upgrade → `trackEvent('upgrade_clicked')` appelé

### Isolation workspace
Non applicable — événements GA4 anonymisés, pas de données workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Navigation / routing frontend** — modification de composants existants, aucun changement de route

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test de non-régression |
|-----------|-----------------|------------------------|
| `CaseFileDetailComponent` | Injection `AnalyticsService` — tests existants à adapter | Mock `AnalyticsService` dans les specs |
| `SynthesisComponent` | Idem | Mock `AnalyticsService` dans les specs |

### Smoke tests E2E concernés
- [ ] Aucun smoke test concerné — pas de changement de routing ni d'auth

---

## Dépendances

### Subfeatures bloquantes
- SF-77-01 — statut : done

---

## Notes et décisions

- **Fail-open** : si `window.gtag` est absent (consentement refusé ou GA4 non chargé), les événements sont silencieusement ignorés. L'action produit n'est jamais bloquée.
- **4 événements** : `analysis_launched` (STANDARD/ENRICHED), `pdf_exported`, `upgrade_clicked` — périmètre volontairement limité au funnel de conversion.
