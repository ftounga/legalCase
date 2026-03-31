# Mini-spec — F-91 / SF-91-01 Hub opérationnel super-admin — liens plateformes tierces

## Identifiant

`F-91 / SF-91-01`

## Feature parente

`F-91` — Hub opérationnel super-admin — liens plateformes tierces

## Statut

`ready`

## Date de création

2026-03-31

## Branche Git

`feat/SF-91-01-super-admin-ops-hub`

---

## Objectif

Ajouter une section "Outils & monitoring" dans la page super-admin listant des liens rapides vers toutes les plateformes tierces utilisées en production.

---

## Comportement attendu

### Cas nominal

La page `/super-admin` affiche, après les métriques existantes, une nouvelle section "Outils & monitoring" organisée en deux catégories :

**Monitoring & produit**
- Google Analytics — tableau de bord GA4
- Sentry — erreurs et performances

**Infrastructure & services**
- Stripe Dashboard — paiements et abonnements
- Brevo — emails transactionnels
- n8n — automatisations
- AWS Console — infrastructure cloud
- RabbitMQ Management — file de messages

Chaque outil s'affiche sous forme de carte avec :
- Icône (Material Icon ou lettre si pas d'icône disponible)
- Nom de la plateforme
- Description courte en une ligne
- Bouton/lien "Ouvrir" → `target="_blank" rel="noopener noreferrer"`

### Cas d'erreur

Aucun — les liens sont statiques. Pas d'appel réseau, pas de vérification de disponibilité.

---

## Critères d'acceptation

- [ ] Section "Outils & monitoring" visible dans `/super-admin` uniquement
- [ ] 7 outils affichés organisés en 2 groupes
- [ ] Chaque lien s'ouvre dans un nouvel onglet (`target="_blank"`)
- [ ] Page accessible uniquement pour les SUPER_ADMIN (guard existant inchangé)
- [ ] Responsive : grille s'adapte sur mobile (1 colonne) et desktop (3-4 colonnes)
- [ ] Respecte le design system (couleurs, typographie, cartes)

---

## Périmètre

### Hors scope

- Vérification de disponibilité des plateformes (ping / health check externe)
- Affichage de métriques issues des plateformes (API GA4, Sentry, Stripe…)
- Gestion des credentials ou des accès depuis l'interface
- Configuration des URLs depuis l'UI (les URLs sont hardcodées dans le composant)

---

## Technique

### Endpoint(s)

Aucun — feature 100% frontend, données statiques.

### Tables impactées

Aucune.

### Migration Liquibase

- [x] Non applicable

### Composants Angular impactés

- `SuperAdminComponent` (`/super-admin`) — ajout d'une section HTML + styles

---

## Plan de test

### Tests unitaires / composant

- [ ] La section "Outils & monitoring" est présente dans le DOM
- [ ] Les 7 outils sont rendus (un `data-testid` ou texte par outil)
- [ ] Chaque lien a `target="_blank"` et `rel="noopener noreferrer"`
- [ ] La section n'est pas visible sur les routes non-super-admin (guard existant — déjà couvert)

### Tests d'intégration

- Non applicable (pas d'endpoint).

### Isolation workspace

- [ ] Non applicable — section super-admin globale, pas de données workspace.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [x] **Navigation / routing frontend** — ajout de contenu dans une route existante (pas de nouvelle route)
- [ ] Aucune préoccupation transversale

### Composants existants potentiellement impactés

| Composant | Impact potentiel | Test prévu |
|-----------|-----------------|------------|
| `SuperAdminComponent` | Ajout de HTML/CSS — aucun comportement modifié | Tests composant existants doivent rester verts |

### Smoke tests E2E concernés

- [ ] Aucun smoke test concerné — ajout de contenu statique dans une route protégée déjà couverte

---

## Dépendances

### Subfeatures bloquantes

Aucune.

### Questions ouvertes impactées

Aucune.

---

## Notes et décisions

- URLs hardcodées dans le composant (pas de config backend) — les URLs de ces outils ne changent pas.
- Icônes : Material Icons (`open_in_new`, `analytics`, `bug_report`, `payment`, `email`, `cloud`, `settings_input_component`) — pas de dépendance externe.
- RabbitMQ Management URL : `https://rabbitmq.staging.legalcase.ng-itconsulting.com` (accès interne staging uniquement — mentionner que la prod n'est pas exposée publiquement).
