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
| Abonnement | `/workspace/billing` | Plan et facturation |
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

## Historique des passages

| Date | Feature | Apport au parcours |
|---|---|---|
| 2026-05-18 | F-98 style learning (cadrage écran SF-98-46-00b) | Création du référentiel parcours cabinet. Ajout de l'écran « Corpus de style » (`/workspace/style-learning`) dans la rubrique GESTION. Verdict GO avec ajustements. |
