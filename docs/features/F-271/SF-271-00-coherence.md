# Étape 0 — Cadrage cohérence — F-271 (+ F-278 couplée)

> Skill : `ai-skills/feature-coherence-challenger.md`. Programme « Conclusions V4 », audit conclusions 2026-06-12.
> **Périmètre du couple** : F-271 (conclusions récapitulatives & reprise des éditions, art. 768 CPC) **et** F-278 (garde anti-écrasement à la régénération) sont traitées **ensemble** par directive PO — une seule confirmation, pas deux redondantes.

## 1. Workflow métier réel de l'avocat cible

En procédure écrite (TJ / CA, et par usage devant le CPH au fond), l'avocat dépose **plusieurs jeux de conclusions successifs**. L'article **768 CPC** impose que les **dernières** conclusions soient **récapitulatives** : la juridiction ne statue que sur les prétentions/moyens **repris** dans le dernier jeu ; tout ce qui n'est pas repris est **réputé abandonné**. En pratique, l'avocat part de son jeu précédent, l'**enrichit** (réplique aux écritures adverses, actualise les montants) et le **consolide**, il ne réécrit jamais tout depuis zéro.

Parcours dans LegalCase :
1. Analyse de dossier (F-3/4/5, Terminée) → synthèse.
2. Outils décisionnels remplis, pistes stratégiques retenues, pièces numérotées (F-260/SF-98-57).
3. **Génération d'un projet de conclusions** (F-98) → version DRAFT.
4. **Édition manuelle** du `content` (F-264 markdown enrichi, livré) → l'avocat raffine l'acte.
5. **Régénération** (ex. après réception des écritures adverses, F-261) → **aujourd'hui : repart de zéro (`version_number = max+1`), content vierge, les éditions de l'étape 4 sont perdues** et les demandes du jeu précédent ne sont pas garanties reprises.

## 2. Cartographie des features existantes sur ce workflow

| Brique | Feature | Statut | Rôle pour F-271/F-278 |
|---|---|---|---|
| Versions de conclusions (1:N, version_number) | SF-98-52 | Livrée | Substrat : on lit la dernière version comme base |
| Édition manuelle du content (DRAFT) | SF-98-49 / F-264 | Livrée | Le travail à NE PAS perdre = `content` édité |
| Moyens / jurisprudence adverses dans le prompt | F-261 / F-179 | Livrée | Déclencheur typique de régénération (réplique) |
| Alerte placeholders `[à compléter]` | SF-266-03 | Livrée | Coexiste ; F-271 ne la remplace pas |
| Prompt builder (user message structuré) | CaseConclusionPromptBuilder | Livrée | Point d'injection de la base récapitulative |
| ConfirmDialogComponent | shared | Livré | Réutilisé tel quel par F-278 (déjà importé dans la section) |

**Amont** : tous les pré-requis existent (versions, content éditable, prompt builder). OK
**Aval** : la sortie reste une version DRAFT éditable/exportable — strictement compatible avec l'existant (export, cycle de vie, diff futur F-280). OK

## 3. Challenge de cohérence — anti-gadget

- **Le besoin est-il réel ?** Oui — c'est le **manque métier #1** de l'audit (le plus grave) : perte de travail + risque d'abandon implicite de demandes (768 CPC). Non gadget.
- **Doublon ?** Non. Aucune feature ne reprend le content précédent. SF-98-52 crée des versions mais **indépendantes**. F-280 (diff) lit les versions mais ne les chaîne pas.
- **F-271 vs F-278, redondance ?** C'est précisément le piège signalé par le PO. **Insight de couplage** : une fois F-271 livrée, « régénérer » **ne perd plus le travail** — la nouvelle version **repart du content de la dernière version (éditions incluses)**. Donc F-278 **ne doit pas** afficher un avertissement « vous allez perdre vos modifications » (faux après F-271). F-278 devient une **confirmation informative unique** : « La régénération crée une nouvelle version qui **repart de vos conclusions actuelles (vos modifications incluses)** et les consolide. Continuer ? ». Une seule confirmation, cohérente avec le nouveau comportement.

## 4. Arbitrage de conception (gate 🟠 réversible — décidé par défaut, tracé)

**Décision : reprise par injection de la base récapitulative dans le PROMPT (Option Prompt), pas copie brute du content (Option Copy).**

| Option | Description | Verdict |
|---|---|---|
| **Copy** | Pré-remplir `content` de la nouvelle version = copie du content précédent, sans IA | Rejetée : la régénération existe justement pour **réintégrer** les nouveaux éléments (analyse mise à jour, moyens adverses F-261). Une copie brute n'est pas « récapitulative », c'est un doublon figé. |
| **Prompt** | Charger le content de la **dernière version** et l'injecter comme **« BASE À CONSOLIDER »** dans le user message, avec consigne explicite : reprendre tous les chefs de demande/moyens de la base (ne rien abandonner — 768 CPC), les enrichir des nouveaux éléments, produire un jeu récapitulatif. | **Retenue** : conforme au métier (récapitulatif = reprise + enrichissement), réversible (no migration, no schéma), construit une fois pour les 3 domaines (uniforme, pas par cellule). |

**Réversibilité** : aucune migration, aucune table, aucun champ DB. C'est une section de prompt + une garde de prompt + une confirmation frontend. 100 % réversible → **décision par défaut conforme à la règle GATE**.

**Portée** : uniforme (toutes cellules), construit une seule fois dans le prompt builder + la garde rédactionnelle. Aucune déclinaison par domaine nécessaire (le content précédent est domaine-agnostique).

## 5. Invariants anti-gadget que la mini-spec doit respecter

1. **Pas de migration / pas de table** — reprise = lecture de la dernière version + injection prompt.
2. **Première génération inchangée** — s'il n'existe aucune version DONE antérieure, comportement actuel (from-scratch), aucune section « base à consolider ».
3. **Base = dernière version DONE** (avec content non vide), éditions de l'avocat incluses ; ignorer les versions PENDING/PROCESSING/FAILED ou content vide.
4. **Garde 768 dans le prompt** : interdiction d'abandonner un chef de demande présent dans la base sans raison ; consolidation, pas troncature.
5. **F-278 = UNE confirmation, informative** (pas « perte de travail »), réutilise `ConfirmDialogComponent`, message aligné sur le comportement récapitulatif.
6. **Pas de régression** sur les gardes existantes de `triggerGeneration` (stade, combinaison, analyse, already-generating).

## 6. Verdict

**GO** — couple F-271 + F-278. Besoin métier #1 réel, briques amont/aval présentes, conception réversible (zéro DB), coordination explicite des deux confirmations en une seule. Étape 0 bis (cohérence écran) requise pour F-278 (élément visible : confirmation à la régénération) — traitée dans `SF-271-00b-ux-coherence.md`.
