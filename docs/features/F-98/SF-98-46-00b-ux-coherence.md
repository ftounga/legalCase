# F-98 — Style learning — Cadrage cohérence écran (étape 0 bis)

**Date** : 2026-05-18
**Skill appliquée** : `ai-skills/screen-coherence-challenger.md`
**Périmètre** : impact écran du bloc style learning — **SF-98-46** (ingestion : point d'entrée d'upload) et **SF-98-48** (UI cabinet de gestion du corpus). SF-98-47 (style mimicking) est **backend pur** → exemptée de cadrage écran.

---

## Verdict

**GO avec ajustements**

Le corpus de style est une configuration **de niveau cabinet** (workspace), pas de niveau dossier. Il mérite un **écran dédié** sous la rubrique GESTION plutôt qu'une section greffée sur un écran existant. Deux ajustements : (a) un lien de découvrabilité depuis la section « Conclusions » du dossier, (b) la section « Conclusions » doit signaler si l'adaptation de style est active.

---

## Intention métier + comportement visible attendu

**Intention** : l'avocat constitue, au niveau de son cabinet, un corpus de ses conclusions passées pour que la génération adopte son style.

**Comportement visible** : un écran cabinet « Corpus de style » où l'avocat téléverse des conclusions de référence, voit la liste de son corpus, et active/désactive l'apprentissage. La section « Conclusions » du dossier indique si la génération courante s'appuie sur ce style.

---

## Rappel verdict étape 0
`SF-98-46-00-coherence.md` (2026-05-18) — **GO avec ajustements** (ajustement structurant : extraction du profil de style, pas de conservation du contenu client). Verdict fonctionnel levé : le cadrage écran est légitime.

---

## Parcours écran réel de l'avocat (configuration cabinet → effet)

Source : écrans réellement codés (`frontend/src/app/workspace/*`, `shell.component`) + pratique avocat. Référentiel : nouveau `docs/business/parcours-ecran-cabinet.md` (créé par ce passage).

1. L'avocat travaille au quotidien dans ses **dossiers** (rubrique DOSSIERS du menu).
2. Ponctuellement, il configure son **cabinet** via la rubrique GESTION : Membres, Abonnement, Administration (plan, consommation, taux de facturation).
3. La rubrique OUTILS porte les ressources transverses : Guides & barèmes, Simulateurs, Rapport de temps.
4. **Configuration du corpus de style** : tâche cabinet, faite une fois puis ajustée — l'avocat téléverse quelques conclusions de référence. ⬅ **SF-98-46 / SF-98-48**
5. De retour dans un dossier, à l'étape 8 du parcours dossier (génération de conclusions, F-98), la génération **adopte le style appris**.
6. État terminal cabinet : corpus constitué et apprentissage actif → toutes les générations futures du cabinet sont stylées.

---

## État terminal du processus
La configuration du corpus est **terminée** quand au moins une conclusion de référence est ingérée et l'apprentissage actif. C'est un état de configuration **persistant et cabinet-large** — distinct de l'état terminal d'un dossier (« projet de conclusions généré », cf. `parcours-ecran-dossier.md`).

---

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours | Écran / zone LegalCase | Statut |
|---|---|---|
| 2 Config cabinet | `workspace-admin`, `workspace-members`, `workspace-billing` (rubrique GESTION) | ✅ existant |
| 3 Ressources transverses | `referentials`, `simulators`, `time-report` (rubrique OUTILS) | ✅ existant |
| 4 **Gestion du corpus de style** | — | ❌ **manquant → SF-98-48** |
| 4 **Upload de conclusions de référence** | pipeline d'upload F-43 (réutilisable) mais rattaché à un dossier | 🟡 à étendre au niveau workspace (SF-98-46) |
| 5 Génération stylée | section `app-conclusions-section` (onglet Décision du dossier) | ✅ existant — à enrichir d'un indicateur |

---

## Position candidate de la feature
- **Écran** : nouvel écran dédié, route `/workspace/style-learning`, composant `StyleCorpusComponent`.
- **Point d'entrée** : nouvelle entrée de menu **« Corpus de style »** dans la rubrique **GESTION** du `shell` (après « Administration »).
- **Zone** : l'écran héberge l'upload (SF-98-46, zone de dépôt), la liste du corpus + le profil de style appris, et le contrôle d'activation (SF-98-48).
- **Point d'entrée secondaire** : un lien discret depuis la section « Conclusions » du dossier (« Configurez votre corpus de style »).

---

## Challenge placement
**Question** : un écran dédié sous GESTION est-il le bon endroit ?

**Oui.** Le corpus est une donnée **cabinet** (workspace), pas dossier — il ne peut pas vivre dans `case-file-detail`. Parmi les écrans cabinet, `workspace-admin` (« Administration ») porte le **plan / la consommation / la facturation** : y greffer une 5ᵉ section « Corpus de style » mélangerait une **configuration produit interactive** (upload, gestion) avec de l'**administration de compte** en lecture seule — registres différents. Un **écran dédié** sous GESTION est plus juste : le corpus est un objet de gestion à part entière (liste, ajout, suppression, activation). **Placement validé — écran dédié, pas section dans `workspace-admin`.**

---

## Challenge lisibilité de la séquence
**Question** : l'avocat voit-il le lien entre « configurer le corpus » et « conclusions stylées » ?

**Partiellement — ajustement requis.** Une entrée de menu isolée sous GESTION ne relie pas le corpus à F-98. **Ajustement b1** : depuis la section « Conclusions » du dossier, afficher un lien de découvrabilité vers `/workspace/style-learning` quand le corpus est vide (« La génération utilise un style générique — constituez votre corpus de style »). **Ajustement b2** : la section « Conclusions » indique, lors d'une génération, si l'adaptation de style est active (cohérent avec le bandeau de transparence SF-98-01).

---

## Challenge charge écran
**Question** : densité ?

L'écran `/workspace/style-learning` est **neuf et dédié** — aucune surcharge d'un écran existant. Le menu GESTION passe de 3 à 4 entrées (Membres, Abonnement, Administration, + Corpus de style) — acceptable. **Pas de surcharge.**

---

## Challenge état final / continuité
**Question** : après avoir constitué le corpus, que fait l'avocat ?

Il retourne à ses dossiers ; l'effet du corpus est **différé et automatique** (les générations suivantes sont stylées). L'écran corpus est un point de configuration, pas une impasse : son « output » se matérialise ailleurs (dans la section Conclusions du dossier). L'ajustement b2 (indicateur de style actif dans la section Conclusions) **ferme la boucle** — sans lui, l'avocat ne saurait pas que son corpus produit un effet.

---

## Ajustements IA requis (pour les mini-specs)
- **b1 — Découvrabilité** : lien depuis la section « Conclusions » du dossier vers `/workspace/style-learning` (affiché surtout quand le corpus est vide).
- **b2 — Boucle de feedback** : la section « Conclusions » signale si la génération s'appuie sur le style appris.
- **b3 — Écran dédié** : `/workspace/style-learning` + entrée de menu GESTION « Corpus de style » — pas de section dans `workspace-admin`.

---

## Invariants anti-surcharge pour les mini-specs
1. Le corpus de style vit sur un **écran cabinet dédié**, jamais dans `case-file-detail` (donnée workspace, pas dossier).
2. L'écran `/workspace/style-learning` reste **un seul écran** : upload + liste + profil + activation y cohabitent sans éclatement (volume attendu faible — quelques documents de référence).
3. Tout effet du corpus a un **point de visibilité** dans le parcours dossier (indicateur de style actif, ajustement b2).
4. La rubrique GESTION ne dépasse pas ~5 entrées ; « Corpus de style » est la 4ᵉ.

---

## Décision finale
**GO avec ajustements.** Écran dédié `/workspace/style-learning` + entrée de menu GESTION. SF-98-46 livre l'ingestion (backend + zone d'upload de l'écran), SF-98-48 livre l'écran de gestion du corpus, SF-98-47 (backend pur) est exemptée. Les 3 ajustements b1–b3 et les 4 invariants sont à reprendre dans les mini-specs. Prochaine étape : mini-specs SF-98-46 / 47 / 48.

---

## MAJ du parcours écran de référence
Création de `docs/business/parcours-ecran-cabinet.md` — référentiel du parcours écran de niveau cabinet (rubriques GESTION / OUTILS), avec l'écran « Corpus de style » et son état terminal de configuration.

---

## Liens
- `SF-98-46-00-coherence.md` — étape 0 du style learning
- `ai-skills/screen-coherence-challenger.md` — skill appliquée
- `docs/business/parcours-ecran-dossier.md` — parcours dossier (l'effet du style s'y matérialise)
- `docs/business/parcours-ecran-cabinet.md` — référentiel parcours cabinet (créé par ce passage)
