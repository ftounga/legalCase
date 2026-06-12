# SF-284-02 — Frontend : échéancier proactif (tête onglet Suivi)

> Feature parente : **F-284 — Échéancier procédural proactif & alertes**

## Objectif (une phrase)
Afficher en tête de l'onglet Suivi une carte « Échéancier » production-grade (gabarit F-282) qui
met en avant le prochain couperet (hero compte à rebours) + les 3 prochaines échéances priorisées
par urgence, en consommant `GET /echeancier`, avec lien vers la liste détaillée F-69.

## Comportement nominal
- Au mount, charge `GET /api/v1/case-files/{id}/echeancier`.
- **Hero** = `nextItem` : pastille d'urgence, gros `J-X` (ou « Aujourd'hui » / « J+X dépassé »),
  libellé, date (`dd/MM/yyyy` mono). Couleur selon `urgency` (rouge OVERDUE/CRITICAL, or SOON,
  navy UPCOMING).
- **Sous le hero** : jusqu'à **3 items suivants** (hors hero) en liste compacte (puce urgence +
  libellé + J-X + date + badge `kind`).
- **Compteurs** : pilule résumé en tête (`X en retard · Y urgents`) si `overdue+critical > 0`.
- **Lien** « Voir tous les délais » → scroll/déplie `case-deadlines-section` (via `(viewAllRequested)`
  remontée au parent qui scrolle vers `#section-deadlines`).
- États : **chargement** (texte sobre), **vide** (icône + invitation, renvoi section délais),
  **sous contrôle** (nextItem UPCOMING > 15j → ton navy apaisé).

## Cas d'erreur
- Échec HTTP → carte masquée (ne casse pas l'onglet) + log ; pas de snackbar bruyant (lecture seule).

## Critères d'acceptation vérifiables
1. AC1 : `nextItem` OVERDUE → hero classe `eche-hero--overdue`, libellé « J+X (dépassé) ».
2. AC2 : 5 items → hero + exactement 3 items secondaires + lien « Voir tous les délais ».
3. AC3 : `items=[]` → état vide rendu (`data-testid="eche-empty"`), pas de hero.
4. AC4 : `counts.overdue+critical=0` et nextItem UPCOMING → pas de pilule d'alarme, ton navy.
5. AC5 : carte placée avant `app-case-phases-timeline` dans le DOM de l'onglet Suivi.

## Plan de test minimal
- **Spec composant** (`echeancier-proactif.component.spec.ts`) : rendu hero par urgence (AC1/AC4),
  troncature à 3 + lien (AC2), état vide (AC3), mapping `urgencyClass`/`daysLabel`.
- **Intégration légère** : service mocké renvoie un `EcheancierResponse` ; vérifie le DOM.
- **Isolation workspace** : couverte côté backend (SF-284-01 AC4) ; le front ne porte pas de scope.

## Tables / endpoints / composants impactés
- **Composant NOUVEAU** : `frontend/src/app/case-files/echeancier-proactif/` (ts/html/scss/spec).
- **Modèle NOUVEAU** : `core/models/echeancier.model.ts` (`EcheancierItem`, `EcheancierResponse`).
- **Service NOUVEAU** : `core/services/echeancier.service.ts` (`get(caseFileId)`).
- **Modifié** : `case-file-detail.component.html` (ajout `<app-echeancier-proactif>` en 1ʳᵉ position
  de l'onglet Suivi, avant `#section-phases`) + `.ts` (handler scroll vers `#section-deadlines`).
- **Endpoint consommé** : `GET /api/v1/case-files/{id}/echeancier` (livré par SF-284-01, vérifié
  présent avant merge frontend).

## Design (impératif — gabarit F-282)
- Carte `mat-card`, `max-width: 760px`, `border-radius: 12px`, padding `20px 24px 16px`.
- Charte : navy `#1A3A5C` / or `#C9973A` via variables CSS, Merriweather titres, Inter corps,
  JetBrains Mono dates. Espacements multiples de 4px. Icône titre or (`event_available`/`alarm`).
- Hero : bloc compte à rebours sobre, pastille colorée, micro-pulse seulement si OVERDUE/CRITICAL.
- États vide/chargement/sous-contrôle soignés. Aucune saisie (lecture seule).

## Hors périmètre
- Édition / ajout de délai (reste dans `case-deadlines-section`).
- Réponse à un round (reste dans `contradictoire-timeline`).
- Toute logique d'alerte mail.

## Préoccupations transversales
- **Navigation / routing** : non (pas de nouvelle route ; composant intégré dans l'onglet existant).
- **Outil décisionnel** : non (vue de lecture, exempté du helper PrefillRules — pas un outil).
