# Parcours écran — Espace super-admin (pilotage produit)

> Référentiel d'architecture de l'information construit incrémentalement par la skill `screen-coherence-challenger` (étape 0 bis). Pendant que `parcours-ecran-cabinet.md` et `parcours-ecran-dossier.md` couvrent le travail de l'**avocat**, ce document couvre les écrans de **pilotage produit interne**, réservés au super-admin (`users.is_super_admin = true`).

**Utilisateur cible** : équipe produit / dev LegalCase (super-admin), pas l'avocat
**Gating** : `SuperAdminService.assertSuperAdmin` (backend) + `auth.currentUser().isSuperAdmin` (guards Angular)

---

## Écrans de niveau super-admin

| Écran | Route | Rôle |
|---|---|---|
| Tableau de bord super-admin | `/super-admin` | Métriques globales, workspaces, users, pipeline health |
| Backlog | `/super-admin/backlog` | Vue backlog produit + marketing (F-178), source de vérité Markdown |
| Blog | `/super-admin/blog` | Gestion du blog SEO (F-120) |
| One-pager traction | `/super-admin/traction-onepager` | Génération du one-pager investisseurs |

---

## Parcours type — session de pilotage backlog / audit

Source : routes Angular réelles (`frontend/src/app/super-admin/`), composant `SuperAdminBacklogComponent`.

1. Super-admin se connecte (OAuth) → identité résolue, `isSuperAdmin = true`.
2. Accède au menu super-admin → `/super-admin` (métriques, pipeline health).
3. Ouvre `/super-admin/backlog` → écran à onglets.
4. Header : indicateur de fraîcheur de la sync MD→DB + bouton « Resync now ».
5. Onglets disponibles :
   - **Produit** — MatTable filtrable/paginée des features du backlog produit.
   - **Marketing** — MatTable des tâches marketing.
   - **Audit dashboard** (F-180) — santé runtime des 85 mappers `DashboardTile` de F-167 : 3 panels 🔴 mappers en erreur / 🟡 tiles dormantes / 🟢 tiles actives.
6. Le super-admin consulte, filtre, ouvre un détail (dialog), ou relance un audit.

## État terminal d'une session de pilotage

Le parcours super-admin est un parcours de **consultation**, sans « dossier clos ». L'état terminal d'une session est : *« l'information de pilotage recherchée a été obtenue »*. L'action corrective qui en découle (corriger un mapper, archiver un outil dormant, prioriser un polish) se déroule **hors de cet écran**, dans le cycle de développement standard (mini-spec → PR). Les écrans super-admin ne portent pas l'action, ils portent l'information qui la déclenche.

---

## Tab « Audit dashboard » (F-180)

| Zone | Contenu |
|---|---|
| Header de tab | Timestamp du dernier run d'audit + bouton « Relancer maintenant » (POST) |
| Panel 🔴 Mappers en erreur | `toolId` ayant crashé sur 168h : compte de crashes, dernier message, commande `kubectl logs` suggérée. Vide = état sain. |
| Panel 🟡 Tiles dormantes | Tables de résultat décisionnel avec 0 row — outils jamais consommés en prod. |
| Panel 🟢 Tiles actives | Tables de résultat décisionnel avec ≥ 1 row, triées par count desc — top des outils consommés. |

Ordre des panels = priorité d'action décroissante (urgent → informatif). Chargement *lazy* : le rapport n'est chargé qu'à l'ouverture de la tab.
