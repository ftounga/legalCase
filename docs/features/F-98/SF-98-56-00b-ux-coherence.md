# SF-98-56 — Cadrage cohérence écran (étape 0 bis)

> Feature : **Réfutation de la jurisprudence adverse dans les conclusions** — couche **marquage** (Option A, sélection avocat).
> Skill : `ai-skills/screen-coherence-challenger.md`. Date : 2026-06-09.

## Verdict : **GO avec ajustements**

## Intention métier + comportement visible attendu (1-2 phrases)

Sur la liste des jurisprudences citées détectées par F-179 (écran Synthèse), l'avocat peut **marquer** une citation **suspecte/introuvable** comme « citation adverse à réfuter ». À la génération des conclusions (onglet Décision), les citations marquées alimentent une **réfutation** rédigée dans l'acte.

## Rappel verdict étape 0 (feature-coherence-challenger)

**GO avec ajustements** (`SF-98-56-00-coherence.md`) — chaîne amont complète sauf la distinction du camp ; **décision PO : Option A (sélection avocat)**. Le présent doc cadre l'impact écran de cette Option A.

## Parcours écran réel de l'avocat (ouverture dossier → état terminal)

> Source : `docs/business/parcours-ecran-dossier.md` + écrans réellement codés (`SynthesisComponent`, `JurisprudenceCitationsSectionComponent`, `ConclusionsSectionComponent`).

1. Ouvre le dossier → détail dossier `/case-files/:id`, onglet **Dossier**.
2. Onglet **Dossier** : upload des pièces (dont les écritures adverses).
3. Onglet **Analyse** : lance l'analyse (asynchrone), puis « Voir la synthèse ».
4. Écran **Synthèse** `/case-files/:id/synthesis` : lit faits, points juridiques, **risques**.
5. **Section « Jurisprudences citées » (F-179)** : voit chaque arrêt cité avec son badge (Vérifiée / **Suspecte** / **Non trouvée** / Incertaine), explication, lien source. *Aujourd'hui : lecture seule, aucune action par citation.*
6. **[NOUVEAU] Marque** les citations suspectes/introuvables issues de l'adversaire comme « adverse à réfuter ».
7. Revient au détail dossier, onglet **Décision** (CTA « Outils décisionnels » existant depuis la synthèse).
8. Renseigne/calcule les outils décisionnels (colonne gauche), lit le tableau de bord (colonne droite).
9. Section **« Projet de conclusions »** : génère / régénère les conclusions.
10. **État terminal = « Projet de conclusions généré »** — désormais enrichi d'une **réfutation** des citations adverses marquées.
11. Relit / édite (SF-98-49), exporte Word/PDF (SF-98-50/51), versionne (SF-98-52).

## État terminal du processus (explicite)

Inchangé : **« Projet de conclusions généré »** (`app-conclusions-section`, onglet Décision). SF-98-56 n'ajoute pas d'état terminal — elle **enrichit le contenu** de cet état terminal. Le marquage (étape 6) est une **pré-étape** qui reboucle dans la génération (étape 9).

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours écran | Écran / zone LegalCase | Statut |
|---|---|---|
| 2. Upload pièces | Onglet Dossier | ✅ existant |
| 3. Analyse | Onglet Analyse | ✅ existant |
| 4-5. Synthèse + F-179 | `SynthesisComponent` / `JurisprudenceCitationsSectionComponent` (accordéon) | ✅ existant |
| **6. Marquage adverse** | `JurisprudenceCitationsSectionComponent` — **action inline par citation** | 🆕 à ajouter |
| 7. Navigation vers Décision | CTA « Outils décisionnels » (synthèse → `?section=decision`) | ✅ existant |
| 9-10. Génération conclusions + réfutation | `ConclusionsSectionComponent` (onglet Décision) | ✅ existant (contenu enrichi) |
| 11. Relire / exporter / versionner | SF-98-49/50/51/52 | ✅ existants |

## Position candidate de la feature

- **Écran** : Synthèse (`/case-files/:id/synthesis`).
- **Zone** : section « Jurisprudences citées » (F-179), **dans la ligne de chaque citation**, à côté du badge de statut.
- **Point d'entrée** : action inline (toggle / bouton « Marquer comme adverse à réfuter ») affichée **uniquement sur les citations SUSPECT / NOT_FOUND** (les seules exploitables en réfutation ; VERIFIED = arrêt valable, UNCERTAIN = silence > erreur).

## Challenge placement

✅ **Cohérent.** L'avocat est déjà sur la section F-179 à l'étape 5 pour juger les citations ; lui permettre de marquer là où il lit le badge « Suspecte » est l'endroit naturel — pas de nouvel écran, pas de détour. Marquer ailleurs (ex. onglet Décision) l'obligerait à mémoriser les références hors contexte.

## Challenge lisibilité de la séquence

⚠️ **Ajustement requis (point principal).** La séquence « marquer sur **Synthèse** → réfutation produite dans les conclusions sur **Décision** » traverse deux écrans et n'est pas explicite : rien n'indique à l'avocat que son marquage aura un effet ailleurs.

Ajustements (légers, sans surcharge) :
- **a.** Sous la section F-179, une **mention discrète** : « Les citations marquées comme adverses alimenteront la réfutation dans le projet de conclusions. » (apparaît seulement s'il existe ≥ 1 citation SUSPECT/NOT_FOUND).
- **b.** Dans `ConclusionsSectionComponent`, après génération, **signaler factuellement** que la réfutation s'appuie sur N citation(s) adverse(s) marquée(s) — sans surpromesse (si N = 0, rien). Réutilise l'esprit du bandeau de transparence existant.
- *(Pas de CTA lourd ni de boucle de retour forcée : on garde l'aller simple synthèse → Décision déjà en place.)*

## Challenge charge écran

✅ **Aucune surcharge.** L'écran Synthèse est un **accordéon extensible** (≈ 13 panneaux conditionnels, chacun replié et affiché seulement s'il a du contenu). Le marquage est une **action locale dans une ligne existante**, pas un nouveau panneau primaire. La mention (a) est une ligne de texte conditionnelle au sein du panneau F-179 déjà présent. Densité globale inchangée.

## Challenge état final / continuité

✅ **Continuité assurée.** Le marquage reboucle dans la génération (déjà existante) ; l'état terminal « conclusions généré » est inchangé, simplement enrichi. Pas de dead-end ni de ping-pong subi : l'avocat marque (Synthèse) → génère (Décision) → relit/exporte (état terminal). Le seul maillon faible (lisibilité inter-écrans) est traité par les ajustements a/b.

## Ajustements IA requis (à intégrer dans la mini-spec)

1. Action de marquage **inline** dans `JurisprudenceCitationsSectionComponent`, **uniquement sur SUSPECT / NOT_FOUND**.
2. Mention de continuité (a) sous la section F-179 (conditionnelle à la présence de citations éligibles).
3. Signalement factuel (b) dans `ConclusionsSectionComponent` après génération (N citations adverses prises en compte ; rien si N = 0).
4. Persistance du marquage : **différée à la mini-spec** (flag sur `jurisprudence_checks` ou petite table de liaison — choix technique étape 1).

## Invariants anti-surcharge pour la mini-spec

1. **Pas de nouveau panneau primaire** sur la Synthèse : le marquage reste une action dans la ligne de citation existante.
2. **Contrôle visible seulement quand il sert** : action de marquage affichée uniquement sur les citations SUSPECT/NOT_FOUND ; mention de continuité affichée seulement s'il en existe ≥ 1.
3. **Aucune section vide** : si aucune citation marquée, le bloc conclusions n'affiche ni rubrique « réfutation » ni « néant ».
4. **Lisibilité de la séquence sans surpromesse** : la mention (a/b) informe, n'exige pas d'action et ne garantit pas un résultat (le LLM reste seul juge de la formulation de la réfutation).
5. **Pas de régression du parcours existant** : l'aller simple Synthèse → Décision et l'état terminal « conclusions généré » restent intacts.

## Décision finale

**GO avec ajustements.** Placement naturel (inline F-179), aucune surcharge écran, continuité assurée ; le seul point à corriger est la **lisibilité de la séquence inter-écrans**, traitée par deux mentions légères (a/b). La mini-spec (étape 1) peut démarrer en intégrant les 4 ajustements et les 5 invariants ci-dessus.

## MAJ apportée au parcours écran de référence

`docs/business/parcours-ecran-dossier.md` enrichi : la section « Jurisprudences citées » (F-179) gagne une **action de marquage « adverse à réfuter »** (sur SUSPECT/NOT_FOUND) reliant la Synthèse à la réfutation dans les conclusions générées (onglet Décision) ; l'état terminal « conclusions généré » est désormais explicitement **enrichi de la réfutation des citations adverses marquées**.
