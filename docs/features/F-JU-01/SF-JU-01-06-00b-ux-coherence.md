# F-JU-01 / SF-JU-01-06 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO

## Intention métier + comportement visible attendu

Donner au super-admin un **bouton « Lancer un bootstrap »** depuis `/super-admin/jurisprudence-watch` qui ouvre un formulaire (textarea CSV `toolId,brancheCalculId,motCleRecherche`) puis appelle l'endpoint backend `POST /api/v1/super-admin/jurisprudence-watch/bootstrap` (qui existe depuis SF-JU-01-05) en batches de 200. Le résultat (`processed / created / skipped / durationMs`) s'affiche dans l'onglet Audit log.

## Rappel verdict feature-coherence-challenger

F-JU-01 elle-même = GO (cohérence fonctionnelle déjà tranchée et livrée 5/5 SF). Cette SF est un **complément ops** qui rend exploitable depuis l'UI l'endpoint backend déjà existant. Pas d'étape 0 supplémentaire à produire.

## Parcours écran réel — super-admin (et non avocat)

Cette feature ne touche **pas** le parcours avocat. L'écran cible (`/super-admin/jurisprudence-watch`) est un écran interne ops, accessible uniquement aux comptes super-admin. Le parcours pertinent est celui du super-admin opérant la veille jurisprudentielle :

1. Super-admin se connecte (OAuth Google/Microsoft) sur staging/prod
2. Ouvre `/super-admin/jurisprudence-watch`
3. Onglet « Flags à arbitrer » → arbitre les flags PENDING (REPLACE / ADD / IGNORE) — pattern courant après run du cron mensuel
4. Onglet « Audit log » → vérifie les décisions passées (sien + Claude AUTO_ADD)
5. **Action ponctuelle de bootstrap initial** : aujourd'hui non exposée à l'écran → besoin d'utiliser `curl` + JWT extracté du navigateur
6. État terminal = `tool_jurisprudence_mappings` peuplé pour les ~480 paires `(toolId, brancheCalculId)` instrumentées par F-JU-03

## État terminal du processus

Pour le super-admin opérant la veille : « la table `tool_jurisprudence_mappings` est à jour, les flags PENDING ont tous été arbitrés ». Pour l'avocat (utilisateur final) : invisible — il voit juste 1 à 3 arrêts à côté de chaque résultat d'outil décisionnel via `<app-tool-jurisprudence-citations>` (livré SF-JU-01-04).

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours admin | Écran / zone LegalCase | Statut |
|---|---|---|
| 1. Auth super-admin | `/auth` + `SuperAdminGuard` | ✅ existant |
| 2. Ouverture dashboard | `/super-admin/jurisprudence-watch` | ✅ existant (SF-JU-01-05) |
| 3. Arbitrage flags | Onglet « Flags à arbitrer » | ✅ existant |
| 4. Consultation audit | Onglet « Audit log » | ✅ existant |
| 5. **Bootstrap initial** | ❌ **manquant** — endpoint backend OK mais pas d'UI | ❌ **manquant** — sujet de cette SF |
| 6. État terminal (mappings peuplés) | Vérification via onglet Audit log (lignes `AUTO_ADD` actor `SUPER_ADMIN`) | ✅ existant |

## Position candidate de la feature

- **Écran cible** : `/super-admin/jurisprudence-watch` (composant `JurisprudenceWatchComponent`)
- **Zone** : nouveau 3ème onglet `mat-tab label="Bootstrap"` à droite des onglets existants « Flags à arbitrer » et « Audit log »
- **Points d'entrée** : aucun ailleurs (action ponctuelle, pas de raccourci depuis le détail du dossier ou la nav globale — ce serait du bruit pour l'avocat)

## Challenge placement

> *« L'écran candidat correspond-il à l'étape du parcours où l'admin a besoin de la feature ? »*

OUI. Le dashboard veille jurisprudentielle est l'écran ops dédié au domaine `tool_jurisprudence_mappings`. Y placer le bootstrap = colocation des 3 actions ops du domaine (alimenter, arbitrer, vérifier). Aucun autre écran ne fait sens.

## Challenge lisibilité de la séquence

> *« L'UI rend-elle visible l'ordre des étapes ? »*

OUI, à condition de placer l'onglet « Bootstrap » **en premier** dans la séquence des tabs (ordre logique : alimenter → arbitrer → vérifier). L'ordre alphabétique ou chronologique d'usage rendrait la séquence illisible.

**Invariant pour la mini-spec** : ordre des tabs imposé = `[Bootstrap | Flags à arbitrer | Audit log]`.

## Challenge charge écran

> *« Quelle est la densité TOTALE de l'écran cible APRÈS ajout ? »*

3 onglets `mat-tab` au lieu de 2. Largement sous le seuil de saturation (un dashboard admin Material Design tolère 5-6 tabs sans bruit visuel). Aucune restructuration de l'écran requise. Pas de découpage en sous-onglets nécessaire.

## Challenge état final / continuité

> *« Après l'output, que fait l'admin ? »*

Après un POST bootstrap réussi, l'admin :
1. Voit le récap inline (`X processed, Y created, Z skipped, Wms`)
2. Bascule (manuellement) sur l'onglet « Audit log » pour vérifier les lignes `AUTO_ADD` actor `SUPER_ADMIN` créées
3. Re-déclenche si nécessaire un batch suivant (les ~480 entrées ne tiennent pas dans un seul batch de 200)

L'état terminal du parcours admin est explicite : « les mappings de la table sont peuplés pour toutes les paires instrumentées ». Pas de ping-pong subi.

## Ajustements IA requis

Aucun ajustement structurel. Mineur côté UX :

- L'ordre des tabs doit être imposé `[Bootstrap | Flags | Audit log]` (cf. lisibilité séquence)
- Un lien interne ou snackbar « Voir l'audit log » après succès du POST aiderait la continuité (étape 2 ci-dessus) — bonus à laisser à la mini-spec

## Invariants anti-surcharge pour la mini-spec

1. **Ne pas exposer le bootstrap ailleurs** que dans ce dashboard (pas de raccourci sidebar, pas de bouton dans le détail du dossier, pas d'item de menu). Action ops, jamais utilisée par l'avocat.
2. **Pas de mise en place d'un éditeur visuel avancé** (pas de tableau éditable cellule par cellule). Textarea CSV brute = suffisant V1 — un admin qui lance 480 mappings sait paste depuis un Google Sheet.
3. **Pas de stockage côté frontend** des batches précédents (pas d'historique inline). L'audit log backend (`jurisprudence_audit_log`) est la source de vérité — onglet Audit log déjà disponible.
4. **Pas de polling / progress bar serveur** — endpoint synchrone, on attend la réponse HTTP (jusqu'à ~60s pour 200 entrées vu l'appel JUDILIBRE + Claude par entrée — à confirmer en dev).
5. **Pas de bouton « Cancel »** côté UI une fois la requête envoyée (pas d'endpoint backend pour ça). Le frontend désactive juste le bouton « Lancer » pendant la requête.

## Décision finale

**GO** — l'ajout d'un 3ème onglet `Bootstrap` dans `JurisprudenceWatchComponent` est le placement naturel et le moins coûteux. Aucun pré-requis bloquant.

## MAJ apportée au parcours écran de référence

Pas de `docs/business/parcours-ecran-*.md` dédié au parcours **super-admin** à date (le référentiel couvre le parcours avocat). Création d'un tel doc serait disproportionnée pour un écran ops unique : la trace de ce cadrage suffit. À reconsidérer si un 2ème écran super-admin émerge.
