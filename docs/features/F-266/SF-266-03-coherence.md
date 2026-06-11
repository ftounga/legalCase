# SF-266-03 — Cadrage cohérence (étape 0) + cohérence écran (étape 0 bis)

> Feature : **Garde « acte déposable » — alerte des emplacements à compléter avant export PDF/Word**. Signal PO 2026-06-11 (« avant de générer en PDF, alerter s'il reste des choses à modifier : la date, le nom et la qualité de l'avocat… non remplis »). Extension de F-266.

## Verdict : **GO**

## Intention
Avant l'export « déposable » (PDF/Word), détecter les **placeholders restants** dans l'acte (emplacements `[ … ]` posés par le générateur : `[Nom et qualité de l'avocat]`, `[à compléter]`, `[Date]`, `[Lieu]`…) et **alerter l'avocat** pour éviter de déposer un acte incomplet.

## Étape 0 — cohérence fonctionnelle
- **Amont** : le générateur (`CaseConclusionPromptBuilder`) pose des placeholders **déterministes** entre crochets — point 7 du `REDACTION_QUALITY_GUARD` (« termine par un emplacement de signature neutre `[Nom et qualité de l'avocat]` ») et « à défaut mets `[à compléter]`, n'invente pas ». Ces marqueurs existent **dans le `content` stocké** → détection fiable sans IA, par simple scan `[ … ]`.
- **Aval** : l'alerte renvoie vers le **mode édition** (F-264, déjà livré) où l'avocat complète, puis exporte (F-266). Boucle fermée, sortie exploitable.
- **Anti-gadget** : ce n'est PAS un gadget — la garde empêche une erreur concrète et embarrassante (déposer un acte contenant `[Nom et qualité de l'avocat]`). Aucun doublon (aucune autre garde ne couvre l'export).
- **Verdict** : GO.

## Étape 0 bis — cohérence écran
- **Parcours** : page conclusions (F-267) → état `DONE` → zone d'export (boutons Word/PDF). C'est **là** que se place l'alerte, au plus près du geste d'export.
- **Placement** : **bandeau d'alerte** juste **au-dessus des boutons d'export**, listant les emplacements restants + bouton **« Compléter »** (entre en mode édition). **ET** **confirmation** au clic Export s'il reste des placeholders (« Exporter quand même ? ») — décision PO (option « Bandeau + confirmation »).
- **Charge écran** : bandeau compact, n'apparaît **que** s'il reste des placeholders ; sinon l'écran est inchangé.
- **Continuité** : non bloquant — l'avocat peut exporter un brouillon volontairement (confirme). Cohérent avec le pattern non bloquant F-258.
- **Verdict** : GO.

## Invariants
1. **Détection sans IA** : scan déterministe `[ … ]` du `content` stocké (pas l'export). Exclut les liens markdown `[libellé](url)` et les renvois numériques `[1]`.
2. **Non bloquant** : l'export reste possible après confirmation (« Exporter quand même »).
3. **Frontend-only** : aucun backend (le content est déjà côté client).
4. **Réutilise** `ConfirmDialogComponent` partagé (pas de nouveau dialog).
5. Le bandeau n'apparaît que sur un acte `DONE` contenant ≥ 1 placeholder.

## Fichiers
- `conclusions-section.component.ts` : `extractPlaceholders()` + signal `placeholdersToFill` + garde `guardThenExport`.
- `conclusions-section.component.html` : bandeau d'alerte.
- `conclusions-section.component.scss` : style bandeau.

## Décision finale
**GO.** Bandeau « N éléments à compléter avant dépôt » + confirmation au clic export, détection déterministe des placeholders, non bloquant. Frontend-only, extension de F-266.
