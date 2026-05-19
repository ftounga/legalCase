# F-180 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO avec ajustements

## Intention métier + comportement visible attendu (1-2 phrases)

Donner au super-admin une vue unique de l'état de santé runtime des 85 mappers `DashboardTile` de F-167. Comportement visible : une nouvelle tab « Audit dashboard » dans `/super-admin/backlog`, affichant 3 panels (🔴 mappers en erreur / 🟡 tiles dormantes / 🟢 tiles actives) plus un header avec le timestamp du dernier run et un bouton « Relancer maintenant ».

## Rappel verdict feature-coherence-challenger (étape 0)

**GO** — briques amont (F-167 instrumentable, F-178 écran super-admin) livrées, sortie exploitable, aucune redondance avec le garde-fou statique SF-DT-36-03.

## Parcours écran réel du super-admin (ouverture → état terminal)

Source : routes Angular réelles (`frontend/src/app/super-admin/`), composant `SuperAdminBacklogComponent` (tabs Produit / Marketing), pratique de pilotage produit du projet. Référentiel créé : `docs/business/parcours-ecran-super-admin.md`.

1. Super-admin se connecte (OAuth) → identité résolue, `isSuperAdmin = true`.
2. Accède au menu super-admin → écran `/super-admin` (métriques, workspaces, users, pipeline health).
3. Ouvre `/super-admin/backlog` → écran à onglets : tab **Produit**, tab **Marketing**.
4. Header de l'écran : indicateur de fraîcheur sync + bouton « Resync now ».
5. Tab Produit : MatTable filtrable/paginée des features ; tab Marketing : idem tâches marketing.
6. Le super-admin consulte, filtre, ouvre le détail d'une feature (dialog).
7. **État terminal** : le super-admin a obtenu l'information de pilotage qu'il cherchait (statut backlog, fraîcheur, ou — avec F-180 — santé runtime des tiles). Il quitte l'écran ou agit hors app (corriger un mapper, archiver un outil).

## État terminal du processus (explicite)

Le parcours super-admin n'a pas d'« état terminal » au sens d'un dossier clos : c'est un écran de **consultation de pilotage**. L'état terminal d'une session est « l'information recherchée a été obtenue ». Pour F-180 : l'état terminal est la lecture du tab « Audit dashboard » → le super-admin sait quels mappers corriger / quels outils sont dormants. L'action corrective (fix mapper) se déroule hors écran, dans le cycle de dev standard.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|
| 2. Menu super-admin | `/super-admin` | ✅ existant |
| 3. Écran backlog à onglets | `/super-admin/backlog` `SuperAdminBacklogComponent` | ✅ existant |
| 4. Header fraîcheur + bouton resync | `super-admin-backlog.component.html` `freshness-block` | ✅ existant — pattern réutilisable |
| 5. Tabs MatTable filtrables | `mat-tab-group` Produit / Marketing | ✅ existant |
| 3bis. **Tab « Audit dashboard »** | **F-180 (la feature challengée)** | — |

## Position candidate de la feature (écran, zone, points d'entrée)

- **Écran** : `/super-admin/backlog` — même écran que F-178, comme prévu par la spec PRODUCT_SPEC F-180.
- **Zone** : nouvelle 3e tab du `mat-tab-group`, après « Produit » (index 0) et « Marketing » (index 1) → index 2.
- **Point d'entrée** : le super-admin clique sur l'onglet « Audit dashboard ». Aucune nouvelle route, aucun nouveau guard.
- **Contenu de la tab** : header (timestamp dernier run + bouton « Relancer maintenant ») + 3 panels empilés verticalement (🔴 erreurs / 🟡 dormantes / 🟢 actives). Panels 🔴 et 🟢 = MatTable triable ; panel 🟡 = liste simple.

## Challenge placement

L'écran `/super-admin/backlog` est-il le bon endroit ? **Oui.** La spec PRODUCT_SPEC F-180 l'impose explicitement (« réutilise l'écran F-178 »), et c'est cohérent : le super-admin qui pilote le backlog produit est exactement le même persona qui veut savoir quels outils décisionnels crashent ou dorment. Un écran dédié serait une dispersion. Aucune création de route. Placement validé.

## Challenge lisibilité de la séquence

Le tab « Audit dashboard » est indépendant des tabs Produit / Marketing — il n'y a pas de séquence imposée entre eux (3 vues parallèles d'un même écran de pilotage). Pas de risque de séquence cassée. **Ajustement** : l'ordre des 3 panels dans la tab doit refléter la priorité d'action décroissante — 🔴 erreurs en premier (action urgente), puis 🟡 dormantes (info), puis 🟢 actives (info). Le header (timestamp + bouton) en tête de tab.

## Challenge charge écran

Densité de l'écran `/super-admin/backlog` après ajout : il porte aujourd'hui 1 header + 2 tabs. Ajouter une 3e tab **n'augmente pas la densité visible simultanément** — un `mat-tab-group` ne montre qu'une tab à la fois. La charge perçue reste constante. À l'intérieur de la nouvelle tab : header + 3 panels, soit ~4 blocs primaires — comparable au contenu de la tab Produit (filtres + table + paginator + toggle vue). **Pas de surcharge.** L'éclatement en onglets est précisément le mécanisme anti-surcharge déjà retenu par F-178.

## Challenge état final / continuité

Après la lecture du tab « Audit dashboard », que fait le super-admin ?
- S'il y a un mapper 🔴 en erreur → il va corriger le code (hors écran, cycle de dev). Le panel 🔴 affiche un message d'erreur + un `toolId` exploitable pour identifier le mapper. **Ajustement** : afficher aussi une commande `kubectl logs` suggérée (prévu par la spec) pour fluidifier la transition vers l'investigation.
- S'il veut un rapport frais → bouton « Relancer maintenant » (POST) régénère un run. Pas de dead-end.
- S'il n'y a aucune erreur → panel 🔴 vide avec état explicite (« Aucun mapper en erreur sur les 7 derniers jours »), pas un tableau vide ambigu.

Pas de ping-pong subi. Continuité assurée.

## Ajustements IA requis (à intégrer dans la mini-spec)

1. **Ordre des panels** = priorité d'action décroissante : 🔴 erreurs → 🟡 dormantes → 🟢 actives.
2. **Header de tab** : timestamp lisible du dernier run + bouton « Relancer maintenant » (POST), pattern symétrique du bouton « Resync now » de F-178.
3. **États vides explicites** : chaque panel affiche un message clair quand il est vide (pas un tableau vide muet). Le panel 🔴 vide est l'état nominal sain.
4. **Commande `kubectl logs` suggérée** dans chaque ligne du panel 🔴 pour fluidifier l'investigation (transition vers l'état aval « corriger le mapper »).
5. **Lazy-load** de la tab : le rapport n'est chargé qu'à l'ouverture de la tab (pattern `onTabChange` déjà présent pour la tab Marketing).
6. **Palette DESIGN_SYSTEM** : badges 🔴 = `#FFEBEE`/`#C0392B`, 🟡 = `#FFF8E1`/`#F9A825`, 🟢 = `#E8F5E9`/`#27AE60` (table « Badges et statuts » du design system). Pas de rouge ailleurs que le panel erreurs.

## Invariants anti-surcharge pour la mini-spec

1. **Aucune nouvelle route Angular** : F-180 est une tab du composant existant, pas un écran.
2. **Un seul `mat-tab-group`** : la tab « Audit dashboard » s'ajoute au groupe existant, on ne crée pas de second niveau d'onglets.
3. **Lazy-load obligatoire** : pas de requête au backend tant que la tab n'est pas ouverte.
4. **3 panels maximum** dans la tab — pas d'ajout de panel sans nouveau cadrage.
5. **Tout panel a un état vide explicite** — jamais de tableau vide muet.

## Décision finale

**GO avec ajustements.** Placement imposé et cohérent (`/super-admin/backlog`, 3e tab), aucune route nouvelle, aucune surcharge (les onglets sont le mécanisme anti-surcharge), continuité assurée vers l'action corrective. Les 6 ajustements IA ci-dessus sont à reporter dans la mini-spec SF-180-02.

## MAJ apportée au parcours écran de référence

Création de `docs/business/parcours-ecran-super-admin.md` — premier référentiel du parcours écran super-admin (le référentiel n'existait que pour `cabinet` et `dossier`). Y est consigné le parcours en 7 étapes ci-dessus et l'articulation de l'« état terminal » d'une session de pilotage.
