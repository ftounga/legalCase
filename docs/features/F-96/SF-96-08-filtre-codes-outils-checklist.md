# SF-96-08 — Filtrer les critères d'outils décisionnels (DT*) hors de la checklist procédurale

> Prolonge SF-96-07. Backend pur (parse), aucun écran, aucune migration. Exempt étape 0/0bis (correction de comportement). Issu d'une **vérification en base** (dossier Dupont-4, 2026-06-16).

## Constat (mesuré en base)

La checklist procédurale (`points_procedure` → entités `ProcedureCheck`) contenait, sur un dossier réel, **5 vrais checks (`FR_*`) + 7 intrus (`DT09_`/`DT36_`)** :
- **doublons** : `DT36_ENTRETIEN_TENU` ≈ `FR_ENTRETIEN`, `DT36_MOTIVATION` ≈ `FR_MOTIVATION`, `DT36_PRESCRIPTION_FAUTE` ≈ `FR_PROCEDURE_DISCIPLINAIRE` ;
- **fond / cadrage** : `DT36_QUALIFICATION_FAUTE`, `DT36_INTENTION_NUIRE`, `DT36_DATE_ENTRETIEN`, `DT09_TYPE_RUPTURE`.

Les `DT*` sont des **critères d'outils décisionnels** (F-DT-09 comparateur, F-DT-36 nullité de procédure) qui fuient dans la checklist. **SF-96-07 (durcissement prompt) ne filtre QUE les items à `critere_code = null`** → il ne pouvait pas les viser (ils ont un code). L'analyse Dupont-4 a tourné **8 h après** le déploiement de SF-96-07 et montrait toujours les intrus → preuve que l'approche prompt est insuffisante ici.

## Décision

**Filtre déterministe** (≠ pari sur le LLM) : les codes de **formalisme** sont `FR_*` / `BE_*` / `RC_*` ; les `DT*` sont des critères d'outils. À la création des checks (`ProcedureCheckService.parsePointsProcedure`), **exclure tout point dont `critere_code` commence par `DT`**. Les outils décisionnels conservent leurs propres panneaux ; la checklist ne garde que les vraies étapes de formalisme + les points free-form (déjà bornés par SF-96-06/07).

## Comportement nominal

- Point `critere_code` ∈ `FR_*`/`BE_*`/`RC_*` → **conservé**.
- Point `critere_code = null` (free-form) → **conservé** (contrainte SF-96-06/07 inchangée).
- Point `critere_code` commençant par `DT` (insensible à la casse, normalisé en MAJ) → **exclu**.

## Cas d'erreur / limites

1. **Format legacy string** (sans code) → conservé (inchangé).
2. **JSON invalide / absent** → fail-open (aucun check), inchangé.
3. **Faux négatif théorique** : un futur code formalisme préfixé `DT` — impossible par convention (`DT` = decision tool ; formalisme = `FR_`/`BE_`/`RC_`).

## Critères d'acceptation vérifiables

- [ ] points_procedure mêlant `FR_ENTRETIEN`, `DT36_*`, `DT09_*`, code null → seuls `FR_ENTRETIEN` + le free-form sont persistés.
- [ ] Aucun `ProcedureCheck` persisté avec un `critere_code` commençant par `DT`.
- [ ] Les checks `FR_*`/`BE_*`/`RC_*` et free-form restent inchangés (non-régression SF-96-06/07).

## Plan de test minimal

- **Unitaire** (`ProcedureCheckServiceTest`) : `createChecks_dropsDecisionToolCodes_keepsFormalismAndFreeForm` (5 entrées → 2 sauvés). Non-régression des tests existants.
- **Isolation workspace** : N/A (le check hérite du workspace de l'analyse, inchangé).

## Composants impactés

- `ProcedureCheckService.parsePointsProcedure` (filtre + commentaire).
- `ProcedureCheckServiceTest`.

**Aucun** : migration, endpoint, frontend, prompt (on n'y touche plus — c'est justement la leçon de SF-96-07).

## Hors périmètre / suivi

- **Nettoyage des `ProcedureCheck` DT* déjà persistés** (analyses passées) : ils disparaîtront à la prochaine ré-analyse (les checks sont remplacés par analyse). Purge ponctuelle optionnelle.
- **Pourquoi le LLM émet ces codes** : la liste des « codes surveillés » du prompt inclut des critères d'outils → ré-analyse du prompt possible (réduire à la source), mais le filtre déterministe suffit et est plus sûr.

## Analyse transversale

- **Outil décisionnel** : on **retire** des critères d'outils de la checklist (ils restent dans leurs panneaux) → cohérent avec « 1 outil = 1 situation ». Le `ProcedureCheckToolMatcher` (F-193) est **inchangé** (mapping intact ; il n'est simplement plus alimenté par des entrées DT* parasites).
- **Auth/workspace** : inchangés. **Smoke E2E** : N/A.
