# SF-206-06 — Frontend : section « prise d'acte de la rupture »

> Feature F-206. Outil : `F-DT-39-prise-acte-rupture`. Contrat API : `SF-206-05-backend-prise-acte.md` (figé).

## Objectif

Afficher dans le panneau d'outils décisionnels (onglet Décision) la section permettant à l'avocat de scorer les chances de succès d'une prise d'acte de la rupture, **avant** que le salarié ne prenne acte.

## Comportement nominal

Composant standalone `PriseActeRuptureSectionComponent` (`frontend/src/app/case-files/prise-acte-rupture-section/`), enregistré dans le `TOOL_REGISTRY` sous `F-DT-39-prise-acte-rupture`. Affiché en `CONTEXTUAL` (flag `prise_acte_envisagee`).

Formulaire (12 champs du contrat API) : 8 griefs (booléens), montant des impayés, persistance des griefs, grief rendant impossible la poursuite, commentaire libre. Bouton « Analyser » → `POST` → affichage : verdict 3 niveaux (`PRISE_ACTE_FAVORABLE` vert / `PRISE_ACTE_RISQUEE` or / `PRISE_ACTE_DEFAVORABLE` rouge), score 0-100, liste des griefs retenus avec libellé + fondement + explication, effet probable (`LICENCIEMENT_SANS_CAUSE` / `LICENCIEMENT_NUL` / `DEMISSION`).

Avertissement visuel persistant : « La prise d'acte est une rupture **immédiate** ; un verdict défavorable signifie effets démission. À comparer avec l'outil **résiliation judiciaire**. »

**Pré-remplissage IA** : champs pré-remplis depuis `aiData` (`travailExtractedData.priseActeDetail`) via `prise-acte-rupture-section-prefill-rules.ts` ; badge `auto_awesome`.

**Cohérence F-IA-03** : champs `DT39_DEFAUT_PAIEMENT`, `DT39_HARCELEMENT`, `DT39_MANQUEMENT_SECURITE`, `DT39_MODIFICATION_CONTRAT`, `DT39_GRIEF_IMPOSSIBLE_POURSUITE` équipés de `CoherencePopoverTriggerDirective`.

## Cas d'erreur

- Backend 422 → message « outil réservé au droit du travail français », formulaire masqué.
- Backend 4xx/5xx → message non bloquant, saisie conservée.
- Pays ≠ FRANCE → section non rendue.

## Critères d'acceptation

1. La section apparaît uniquement sur visibilité backend (flag `prise_acte_envisagee`).
2. Les 12 champs sont pré-remplis depuis `aiData` quand l'information existe ; badges `auto_awesome`.
3. Verdict 3 niveaux avec légende couleur ; griefs retenus listés ; effet probable affiché.
4. Avertissement « rupture immédiate / comparer avec résiliation judiciaire » visible.
5. `getPrefillCount()` correct.
6. Self-check grep `tool_id`.

## Plan de test

- **Jest** : rendu formulaire, pré-remplissage, badges, appel POST, affichage des 3 verdicts + 3 effets probables (dont bascule `LICENCIEMENT_NUL` sur harcèlement/discrimination), gate `isFrance`.
- Self-check grep `tool_id`.

## Tables / endpoints / composants impactés

- **Nouveaux fichiers** : `prise-acte-rupture-section.component.{ts,html,scss}`, `prise-acte-rupture-section-prefill-rules.ts`, service `prise-acte-rupture.service.ts`.
- **Modifié** : `decisional-tools-panel.component.ts` (`TOOL_REGISTRY`).

## Préoccupations transversales

**Outil décisionnel métier** : groupe thématique F-169 « Rupture — initiative salarié / torts employeur ». Self-check grep `tool_id`. Vérifier merge backend SF-206-05 avant merge frontend.

## Hors périmètre

- Backend (→ SF-206-05).
- Chiffrage des indemnités consécutives (F-DT-09).
- Rédaction de la lettre de prise d'acte (F-98).
