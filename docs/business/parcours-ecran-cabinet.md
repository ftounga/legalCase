# Parcours écran — Espace cabinet (niveau workspace)

> Référentiel d'architecture de l'information construit incrémentalement par la skill `screen-coherence-challenger` (étape 0 bis). Pendant que `parcours-ecran-dossier.md` couvre le travail **dans un dossier**, ce document couvre les écrans de **configuration du cabinet** (niveau workspace).

**Utilisateur cible** : avocat administrant son cabinet
**Navigation** : `frontend/src/app/layout/shell/shell.component`

---

## Rubriques du menu latéral

| Rubrique | Entrées | Rôle |
|---|---|---|
| **DOSSIERS** | Tableau de bord, Dossiers, Recherche | Le travail quotidien sur les dossiers |
| **OUTILS** | Guides & barèmes, Simulateurs, Rapport de temps | Ressources transverses |
| **GESTION** | Membres, Abonnement, Administration, **Corpus de style** (F-98) | Configuration du cabinet |

---

## Écrans de niveau cabinet

| Écran | Route | Rôle |
|---|---|---|
| Membres | `/workspace/members` | Invitation / rôles des membres |
| Abonnement | `/workspace/billing` | Plan, facturation et **résiliation self-service (F-247)** |
| Administration | `/workspace/admin` | Plan, consommation tokens / OCR, taux de facturation |
| Rapport de temps | `/workspace/time-report` | Suivi du temps |
| **Corpus de style** | `/workspace/style-learning` | **F-98 — corpus de conclusions de référence pour l'apprentissage du style rédactionnel** |

---

## Parcours réel de l'avocat (configuration cabinet)

1. L'avocat travaille principalement dans ses dossiers (rubrique DOSSIERS).
2. Ponctuellement, il configure son cabinet via la rubrique GESTION.
3. **Corpus de style** (`/workspace/style-learning`) : il téléverse quelques conclusions de référence ; LegalCase en apprend le style. Tâche faite une fois puis ajustée.
4. De retour dans un dossier, la génération de conclusions (F-98, onglet Décision) adopte le style appris.

## État terminal du processus (cabinet)

La configuration cabinet n'a pas d'« état terminal » unique — c'est un ensemble de réglages persistants. Pour le **corpus de style** spécifiquement : l'état « configuré » = au moins une conclusion de référence ingérée et apprentissage actif ; l'effet se matérialise dans le parcours dossier (section « Conclusions »).

## Parcours périphérique — désinscription des emails (F-248)

Le désabonnement des emails non-transactionnels n'est **pas un écran applicatif** : c'est un parcours déclenché depuis la boîte mail de l'avocat.

1. L'avocat reçoit un email non-transactionnel (séquence d'onboarding F-73, newsletter mensuelle).
2. Il clique sur « Se désinscrire » dans le pied de l'email.
3. Le navigateur ouvre la **page publique `/unsubscribe?token=…`** (hors `ShellComponent`, sans login).
4. La page confirme l'action et reste **bidirectionnelle** (réabonnement possible avec le même token).

Cette page n'impacte aucun écran cabinet et n'ajoute pas d'entrée de menu. Elle rejoint la famille des routes publiques token-based (`verify-email`, `reset-password`, `share/:token`).

## Parcours écran — Tableau de bord d'accueil (F-249)

> Ajouté au passage F-249 (refonte futuriste du dashboard d'accueil). Le tableau de bord d'accueil (`/dashboard`) est l'écran de niveau workspace par lequel l'avocat **entre dans l'application** chaque jour — distinct des écrans de configuration ci-dessus.

**Écran** : `/dashboard` — `frontend/src/app/dashboard/dashboard.component` — route post-login par défaut.
**Rôle** : hub d'orientation. Répond à « qu'est-ce qui demande mon attention aujourd'hui ? ».

**Parcours réel** :
1. L'avocat se connecte → redirection automatique vers `/dashboard`.
2. Il scanne, par ordre de priorité : délais procéduraux urgents (vital — risque de forclusion), alertes de checklist, dossiers ouverts, activité récente.
3. Il clique vers le dossier prioritaire → écran `case-file-detail` (il quitte le dashboard).
4. Il revient au dashboard plusieurs fois par jour, entre deux tâches, pour se ré-orienter (rubrique DOSSIERS → Tableau de bord).

**Blocs primaires** (post-F-249) : ~5 — un hero d'accueil (accueil personnalisé + date + headline actionnable + compteurs KPI intégrés) puis 4 sections (délais, alertes, dossiers, activité).

**État terminal** : un hub récurrent n'a pas d'état terminal. L'état terminal d'une *visite* = l'avocat est orienté en quelques secondes (il a cliqué vers le dossier à traiter, ou constaté l'absence d'urgence).

## Parcours écran — Création d'un workspace supplémentaire (F-156)

> Ajouté au passage F-156 (restriction de la création d'un workspace supplémentaire aux plans TEAM / PRO). Ce parcours traverse le **workspace switcher** du header — un avocat qui opère plusieurs entités juridiques (cabinet A + activité de conseil, double inscription FR/BE, holding/filiale…) crée des workspaces additionnels pour cloisonner ses données.

**Écrans traversés** :
- **Header** — `WorkspaceSwitcherComponent` (F-154) — visible depuis tout l'app authentifié.
- **Dialog modal** — `WorkspaceCreateDialogComponent` (existant, enrichi par F-156).
- **Stripe Checkout** — page externe, hors app.
- **Dashboard du nouveau workspace** — écran de niveau workspace standard, avec banderole d'état temporaire (`WorkspaceStatusBanner`, nouveau).

**Parcours réel** :
1. L'avocat clique sur le **workspace switcher** du header → dropdown listant ses workspaces.
2. Option **« + Créer un workspace »** au bas du dropdown — visible **uniquement si OWNER TEAM ou PRO** (invisible pour FREE et SOLO).
3. Clic → dialog modal avec champ « Nom du workspace » + **cards plan TEAM / PRO** (mutuellement exclusives, prix issus du pricing F-123) + bouton « Créer + payer » (disabled tant que nom et plan ne sont pas tous deux renseignés).
4. Submit → `POST /api/v1/workspaces` → réponse `201` avec `stripeCheckoutUrl` → **redirection navigateur** (pas `router.navigate`) vers Stripe Checkout.
5. Paiement chez Stripe → redirection vers `/workspaces?workspace_created=success&workspace_id=<id>`.
6. Handler de route → **bascule automatique** sur le nouveau workspace via le switcher.
7. Dashboard du nouveau workspace : banderole d'état `PENDING_PAYMENT` (« Paiement en cours d'activation… ») jusqu'à réception du webhook `customer.subscription.created` ; puis banderole de bienvenue 5 s.
8. Tant que `PENDING_PAYMENT`, les **actions d'écriture** (créer dossier, inviter, uploader) sont visuellement désactivées + tooltip explicatif — évite que l'avocat crée du contenu qui sera perdu si le paiement échoue.

**Cas FREE / SOLO** : l'option « + Créer un workspace » n'apparaît pas. Si l'avocat bricole une URL `/workspaces/new`, un guard `WorkspacePlanGuard` le redirige vers `WorkspaceUpgradeRequiredPageComponent` (titre « Création d'un workspace supplémentaire », sous-titre « TEAM ou PRO requis », CTA « Découvrir les plans » → ouvre `/pricing` en nouvelle page).

**Cas annulation Stripe** : retour sur `/workspaces?workspace_created=cancelled&workspace_id=<id>` → snackbar « Création annulée — aucun frais prélevé » + bascule sur le workspace d'origine. Le workspace `PENDING_PAYMENT` créé en step 4 sera nettoyé par le cron backend (24 h max).

**Blocs primaires ajoutés** : aucun nouveau bloc principal d'écran applicatif — la feature vit dans des écrans existants (switcher dropdown + dialog) + une banderole fine d'état transitoire (~40-48 px).

**État terminal** : l'avocat travaille dans le nouveau workspace (statut `ACTIVE` confirmé), prêt à inviter ses membres ou créer son premier dossier. Le workspace d'origine reste accessible à tout moment via le switcher.

## Historique des passages

| Date | Feature | Apport au parcours |
|---|---|---|
| 2026-05-18 | F-98 style learning (cadrage écran SF-98-46-00b) | Création du référentiel parcours cabinet. Ajout de l'écran « Corpus de style » (`/workspace/style-learning`) dans la rubrique GESTION. Verdict GO avec ajustements. |
| 2026-05-19 | F-247 résiliation self-service (cadrage écran SF-247-00b) | Écran Abonnement enrichi : section de résiliation self-service en bas de page (visible si plan payant + OWNER) + bandeau « résiliation programmée » en haut. Verdict GO. |
| 2026-05-19 | F-248 désabonnement emails (cadrage écran SF-248-00b) | Ajout du parcours périphérique de désinscription email (email → page publique `/unsubscribe`). N'impacte aucun écran cabinet applicatif. Verdict GO avec ajustements. |
| 2026-05-19 | F-249 refonte futuriste dashboard d'accueil (cadrage écran SF-249-00b) | Création de la section « Tableau de bord d'accueil ». Le hero d'accueil absorbe la barre KPI (charge d'écran constante ~5 blocs primaires). Verdict GO avec ajustements. |
| 2026-05-19 | F-156 création workspace supplémentaire TEAM/PRO (cadrage écran SF-156-00b) | Ajout du parcours « Création d'un workspace supplémentaire » — switcher dropdown + dialog enrichi + Stripe Checkout + banderole `PENDING_PAYMENT` + page d'erreur FREE/SOLO. Aucun nouvel écran applicatif, juste une banderole fine. Verdict GO. |
