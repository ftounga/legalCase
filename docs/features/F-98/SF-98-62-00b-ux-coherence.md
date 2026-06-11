# SF-98-62 / SF-98-63 — Cadrage cohérence écran (étape 0 bis)

> Feature : couverture du stade **BCO (Bureau de Conciliation et d'Orientation)** du CPH FR dans la génération de conclusions F-98.
> Skill : `ai-skills/screen-coherence-challenger.md`. Date : 2026-06-11.

## Verdict : **GO avec ajustements (légers)**

## Intention métier + comportement visible attendu (1-2 phrases)

Sur un dossier prud'homal réglé au stade **BCO**, le bouton « Générer le projet de conclusions » (onglet Décision) — aujourd'hui **bloqué en silence** — produit l'acte écrit attendu (requête de saisine valant conclusions côté demandeur ; observations en défense côté défendeur). Le parcours écran ne change pas ; un cul-de-sac muet est supprimé.

## Rappel verdict étape 0 (feature-coherence-challenger)

**GO** (`SF-98-62-00-coherence.md`) — trou de couverture pur, toutes briques amont/aval livrées. Le présent doc cadre l'impact écran.

## Parcours écran réel de l'avocat (ouverture dossier → état terminal)

> Source : `docs/business/parcours-ecran-dossier.md` + écrans codés (`CaseFileDetailComponent`, `ConclusionsSectionComponent`) + observation prod (dossier STANOJEVIC).

1. Ouvre le dossier `/case-files/:id`, onglet **Dossier**.
2. Onglet **Dossier** : upload des pièces ; **renseigne juridiction + stade + position** (F-243) — peut choisir **BCO**.
3. Onglet **Analyse** : lance l'analyse (asynchrone) → synthèse.
4. Onglet **Décision** : renseigne/calcule les outils décisionnels, lit le tableau de bord.
5. Section **« Projet de conclusions »** : clique « Générer ».
6. **Aujourd'hui si stade = BCO** : blocage `409 COMBINATION_NOT_SUPPORTED` → snackbar fugace (6 s) ou rien de perceptible → **cul-de-sac muet**.
7. **[CORRIGÉ]** : génération asynchrone → **état terminal « Projet de conclusions généré »**.
8. Relit / édite (SF-98-49), bordereau (SF-98-57), exporte Word/PDF (SF-98-50/51), versionne (SF-98-52).

## État terminal du processus (explicite)

Inchangé : **« Projet de conclusions généré »** (`app-conclusions-section`). La feature ne crée pas d'état terminal nouveau — elle **rend atteignable** l'état terminal existant depuis un stade (BCO) où il était inaccessible.

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 2. Réglage juridiction/stade/position (dont BCO) | Onglet Dossier (F-243) | ✅ existant — propose BCO |
| 3. Analyse | Onglet Analyse | ✅ existant |
| 4. Outils décisionnels + dashboard | Onglet Décision | ✅ existant |
| 5/7. Génération conclusions | `ConclusionsSectionComponent` | ✅ existant (résout désormais une cellule BCO) |
| 6. Message si combinaison non couverte | snackbar 409 (fugace) | ⚠️ à rendre explicite (ajustement) |
| 8. Relire / bordereau / exporter / versionner | SF-98-49/57/50/51/52 | ✅ existants |

## Position candidate de la feature

- **Écran** : détail dossier, onglet **Décision**, section « Projet de conclusions » — **inchangé**.
- **Aucun nouvel écran, aucun nouveau panneau, aucun nouveau bouton.** La cellule BCO est résolue côté serveur ; l'UI existante en bénéficie sans modification structurelle.

## Challenge placement

✅ **Cohérent.** Le point d'action (bouton « Générer ») est déjà à sa place. On ne déplace ni n'ajoute rien : on fait fonctionner ce qui était proposé puis refusé.

## Challenge lisibilité de la séquence

✅ **Améliorée** (on supprime une incohérence). Avant : l'écran proposait le stade BCO mais refusait silencieusement la génération → l'avocat ne comprenait pas. Après : la séquence « régler le stade → générer » aboutit. Aucune nouvelle séquence inter-écrans à apprendre.

## Challenge charge écran

✅ **Aucune surcharge** — zéro élément visible ajouté en cas nominal.

## Challenge état final / continuité

✅ **Cul-de-sac supprimé.** Le maillon faible était précisément un **dead-end muet** (étape 6). La feature le remplace par la continuité normale vers l'état terminal. Pour les combinaisons qui resteraient non couvertes à l'avenir, l'ajustement ci-dessous garantit qu'il n'y aura **plus jamais de blocage muet**.

## Ajustements écran requis (à intégrer dans la mini-spec)

1. **Message explicite, non fugace, en cas de combinaison non couverte** : remplacer le snackbar 6 s par un **encart d'état dans la section conclusions** (« La génération n'est pas disponible pour cette combinaison procédurale (… / … / …). ») — afin qu'aucun stade sélectionnable ne mène à un refus invisible. *(Bénéficie à toute la matrice, pas seulement au BCO.)*
2. Aucun autre changement d'UI nécessaire pour le cas nominal BCO.

## Invariants anti-surcharge pour la mini-spec

1. **Zéro nouvel élément visible** en cas nominal : pas de panneau, pas de bouton, pas d'onglet.
2. **Aucun blocage muet** : tout refus de génération doit être affiché de façon persistante et lisible (ajustement 1).
3. **Pas de régression** du parcours existant (génération sur Fond/Référé/Départage/Appel/Cassation intacte).
4. **Pas de message vide** : l'encart d'indisponibilité n'apparaît que lorsqu'une génération est effectivement refusée.

## Décision finale

**GO avec ajustements.** Impact écran quasi nul (on rend atteignable un état terminal existant) ; le seul ajustement utile est de transformer le refus muet en **message explicite** pour toute combinaison non couverte. La mini-spec peut démarrer.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` : le stade **BCO** devient un stade pleinement génératif (requête valant conclusions / observations en défense) ; tout refus de génération pour combinaison non couverte est désormais **affiché explicitement** dans la section conclusions (fin des culs-de-sac muets).
