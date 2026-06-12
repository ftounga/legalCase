# Audit du workflow décisionnel — fidélité métier & leviers d'adoption

> Audit demandé par le PO le 2026-06-12. Objectif : « monter d'un cran dans l'adoption » — l'avocat doit retrouver ses étapes du quotidien et **rester**. Grounded sur : `parcours-ecran-dossier.md`, capacités vérifiées du code, dossier réel STANOJEVIC (Renversez), signaux terrain (Renversez/Mengue). ⚠ Le workflow métier est une hypothèse à valider auprès d'avocats.

## TL;DR
Le cœur **synthèse → outils pré-remplis → jurisprudence → conclusions est excellent et différenciant — à garder.** Mais il modélise le dossier comme un **instantané one-shot, en ligne droite**. Or un dossier réel est **vivant, multi-phases, contradictoire, étalé sur des mois**. Ce décalage est la **cause racine du churn** (Renversez 1 usage puis stop ; Mengue jamais activé) : du « WOW » à l'usage unique, **aucune habitude** ne se crée. Correctif = **ajouter la couche cycle-de-vie**, pas réécrire.

## Workflow réel de l'avocat plaidant (⚠ hypothèse)
0. **Qualification/intake** — récit client + pièces partielles → y a-t-il un dossier ? prescription ? compétence ? type ? valeur ?
1. **Constitution itérative** — réclame les pièces ; elles arrivent **par vagues** sur des semaines (dossier incomplet→complet).
2. **Analyse & stratégie** — faits, chefs de demande, risques, chiffrage ; référé+fond ? concilier/transiger ?
3. **Rédaction & saisine** — requête/conclusions, jurisprudence, principal/subsidiaire, bordereau, dépôt.
4. **Contradictoire (mois, plusieurs rounds)** — BCO→jugement ; l'adversaire conclut → **tu répliques** → il répond → tu re-répliques (2-4 échanges) ; délais de mise en état.
5. **Audience / délibéré** (hors produit).
6. **Post-jugement** — appel ? exécution ?

## Fidélité LegalCase ↔ réalité (4 onglets : Dossier / Analyse / Décision / Suivi)
| Phase réelle | État produit | Verdict |
|---|---|---|
| 0. Qualification | ❌ absent (on uploade direct ; stade saisi à la main) | **Trou** |
| 1. Constitution itérative (vagues) | ⚠️ upload + pièces manquantes, mais modèle snapshot, pas de vagues/phases | **Trou** |
| 2. Analyse & stratégie | ✅ fort, mais snapshot + stratégie silotée par outil | OK, améliorable |
| 3. Rédaction | ✅ fort (conclusions versionnées, lifecycle, juris, subsidiaire, bordereau) + **programme Conclusions V4** (récap, éditeur, diff, beauté) | OK / en cours |
| 4. Contradictoire multi-rounds | ⚠️ **réfutation one-shot** (F-261, `adverse_pleadings` = booléen) + récap (F-271) — **mais aucun modèle de ROUND/échange** | **Trou (majeur)** |
| 5/6 | calendrier/délais (Suivi, F-69) **passif**, périphérique | sous-exploité |

## Réconciliation avec le backlog existant (vérifié dans le code 2026-06-12)
- **Couche RÉDACTION = déjà traitée** par le programme **Conclusions V4 (F-271→F-281)** + **F-261** (réfutation des moyens adverses, 3 domaines FR). La beauté écran des conclusions y est déjà adressée (F-276→281 : sommaire, WYSIWYG, anti-écrasement, autosave, diff, aperçu export).
- **NON couvert (apport unique de cet audit) = la couche CYCLE DE VIE** : le cycle contradictoire **multi-rounds** (≠ réfutation one-shot de F-261 et ≠ récap de F-271), le dossier vivant par phases, l'échéancier proactif, la qualification d'entrée, la stratégie unifiée.

## Recommandations → features (priorisées par impact rétention)
| Prio | Feature | Essence | Distinct de |
|---|---|---|---|
| 🥇 P1 | **F-282 Cycle contradictoire (rounds d'échange)** | modéliser la séquence d'échanges (à qui le tour, délai, « réplique au jeu adverse N ») = moteur de rétention | F-261 (contenu réfutation), F-271 (ta récap), F-274 (pièces) |
| 🥈 P2 | **F-283 Dossier vivant — phases procédurales + vagues de pièces** | structurer par phase courante ; pièces par vagues + « impact des nouvelles pièces » | F-243 (stade statique) |
| 🥉 P3 | **F-284 Échéancier procédural proactif + alertes** | rendre F-69 proactif (prescription, recours, mise en état) = raison de revenir | F-69 (échéances passives) |
| P4 | **F-285 Qualification d'entrée (intake)** | porte d'entrée guidée (type/prescription/recevabilité/valeur) | — |
| P5 | **F-286 Stratégie de dossier unifiée** | reco consolidée (référé vs fond, concilier vs plaider, quels chefs) | « pistes » silotées |

## Invariant transverse pour ces 5 features
**Exigence design impérative** : qualité visuelle de premier ordre (charte `DESIGN_SYSTEM.md` : navy `#1A3A5C` / or `#C9973A`, Inter/Merriweather/JetBrains Mono, espacements 4px ; production-grade, zéro « AI-generic »). Chacune passe obligatoirement par l'**étape 0 bis (cohérence écran)**. Motivation : retour PO 2026-06 « visuellement extrêmement moche, tout serré » (origine refonte F-267/F-268). L'adoption se joue autant sur la beauté que sur la fonction.

## Thèse d'adoption
Le « WOW » (pré-remplissage + juris + conclusions) crée l'émerveillement ; **le cycle de vie crée l'habitude**. La rétention vient de ce que l'outil vit dans le **rythme quotidien** de l'avocat — délais, échanges adverses, vagues de pièces — pas d'une analyse unique.
