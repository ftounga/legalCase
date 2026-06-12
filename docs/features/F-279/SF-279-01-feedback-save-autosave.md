# SF-279-01 — Feedback de sauvegarde explicite + autosave brouillon (local)

**Feature parente** : F-279 — Conclusions V4 ⑨ (UX)
**Type** : Frontend-only. Aucune migration, aucun endpoint, aucune dépendance nouvelle.
**Date** : 2026-06-12

## Objectif (une phrase)

Dans l'éditeur de conclusions, rendre la sauvegarde **visible** (état `✓ Enregistré` /
`Modifié` + confirmation au save) et **protéger le brouillon de la perte** via un autosave
**local** (localStorage) restitué — sur proposition — à la ré-entrée en édition.

## Comportement nominal

1. **État de sauvegarde (dirty/clean)**
   - Un signal `dirty` calculé : `draftContent() !== savedContent()` où `savedContent` =
     contenu serveur de la version courante.
   - En mode édition, un chip dans la barre d'actions :
     - `Modifié` (couleur attention) quand `dirty`.
     - `✓ Enregistré` (couleur succès) quand `!dirty`.
2. **Confirmation au save**
   - `saveContent()` succès → en plus de fermer l'édition : snackbar `Modifications enregistrées.`
     (panel succès, 3 s) ; `savedContent` mis à jour ; brouillon local purgé.
3. **Autosave local**
   - À chaque modification du brouillon (`onDraftInput`, `applyMarkdown`, `regenerateSection`),
     un debounce (800 ms) écrit `{ versionId, content, savedAt }` dans
     `localStorage["lc.conclusions.draft.<caseFileId>.<versionId>"]`.
   - L'autosave **n'écrit jamais** côté serveur.
   - Si `content === savedContent` (revenu à l'état serveur), l'entrée locale est **supprimée**
     (pas de faux brouillon).
4. **Restauration au démarrage de l'édition**
   - `startEditing()` : si une entrée locale existe pour `(caseFileId, versionId)` ET que son
     `content` diffère du contenu serveur → afficher le bandeau « Brouillon récupéré » avec
     **Restaurer** (charge le contenu local dans `draftContent`) / **Ignorer** (supprime
     l'entrée locale, garde le contenu serveur). Sinon, édition normale.
5. **Purge du brouillon local**
   - Après un `saveContent` réussi ; après `cancelEditing` ; après un changement de cycle de
     vie sortant de `DRAFT` (`changeLifecycle` → VALIDATED/DEPOSITED) ; après « Ignorer ».

## Cas d'erreur

1. **`localStorage` indisponible** (mode privé, quota plein, exception `setItem`) → toutes les
   opérations de stockage sont encapsulées en `try/catch` ; l'autosave est **désactivé
   silencieusement**, l'éditeur et l'enregistrement serveur restent pleinement fonctionnels.
2. **Échec de l'enregistrement serveur** (`409`/`400`/réseau) → comportement existant inchangé
   (snackbar d'erreur) ; le brouillon local **n'est pas** purgé (le travail reste protégé) ;
   l'édition reste ouverte ; chip reste `Modifié`.
3. **Entrée locale corrompue** (JSON invalide) → `try/catch` au parse → traitée comme absente,
   et l'entrée est supprimée.

## Critères d'acceptation (vérifiables)

- [ ] En édition, modifier le texte affiche le chip `Modifié`.
- [ ] Après `Enregistrer` réussi : snackbar « Modifications enregistrées. », retour lecture,
      entrée localStorage de la version supprimée.
- [ ] Une frappe déclenche, après debounce, une écriture localStorage contenant le contenu courant.
- [ ] Rouvrir l'édition avec un brouillon local divergent affiche le bandeau « Brouillon récupéré ».
- [ ] « Restaurer » charge le contenu local ; « Ignorer » garde le contenu serveur et supprime l'entrée.
- [ ] `localStorage.setItem` qui jette n'empêche ni l'édition ni l'enregistrement (pas d'exception remontée).
- [ ] Brouillon local revenu identique au serveur → l'entrée locale est supprimée (pas de bandeau).
- [ ] Passer la version en `VALIDATED` purge l'entrée locale.

## Plan de test minimal (Jest, jasmine-style — fichier spec existant)

**Unitaires (helpers purs)** — `draftStorageKey(caseId, versionId)`,
`readDraft`/`writeDraft`/`clearDraft` (avec mock `localStorage` qui jette) :
- clé déterministe ; round-trip write→read ; parse JSON invalide → null + clear ;
  `setItem` qui jette → pas d'exception (no-op).

**Composant (intégration légère, HttpTestingController)** :
- `dirty` vrai après input, faux après save.
- save réussi → snackbar succès + `clearDraft` appelé.
- input → après `tick(800)` (fakeAsync) → `writeDraft` appelé.
- `startEditing` avec brouillon divergent → `draftRecovered()` vrai (bandeau).
- `restoreDraft()` → `draftContent` = contenu local ; `discardDraft()` → contenu serveur + clear.
- `changeLifecycle('VALIDATED')` réussi → `clearDraft` appelé.

**Isolation workspace** : non applicable (frontend-only, pas d'appel serveur nouveau ; la clé
localStorage est scopée par `caseFileId` + `versionId`, identifiants déjà résolus côté workspace
par les guards existants).

## Tables / endpoints / composants impactés

- **Tables** : aucune.
- **Endpoints** : aucun (réutilise `PATCH .../content` existant pour le seul save explicite).
- **Composants** :
  - `frontend/src/app/case-files/conclusions-section/conclusions-section.component.ts`
    (signaux `dirty`, `savedContent`, `draftRecovered` ; debounce autosave ; restore/discard ;
    purge ; snackbar de succès).
  - `…/conclusions-section.component.html` (chip d'état dans `cs-actions` édition ; bandeau
    « Brouillon récupéré »).
  - `…/conclusions-section.component.scss` (chip + bandeau, réutilisent les variables existantes).
  - Nouveau module pur `…/conclusion-draft-storage.util.ts` (clé + read/write/clear, testable
    sans Angular).

## Analyse de cohérence transversale

- **Autres outils décisionnels** : N/A — `conclusions-section` n'est pas un outil décisionnel
  (pas de TOOL_REGISTRY, pas de panel F-IA-04).
- **Autres pays / domaines** : aucune logique métier — uniforme aux 3 domaines, BE inclus.
- **Autres UI patterns (autosave/localStorage)** : pattern localStorage déjà utilisé
  (`tour.service`, `consent.service`, `onboarding-wizard.service`). On reste cohérent (préfixe
  `lc.`), mais le besoin (brouillon par version de conclusions) est spécifique → util dédié,
  pas de service global réutilisable à harmoniser ailleurs aujourd'hui (aucun autre éditeur
  long de document dans le produit). Classement : **non applicable ailleurs** pour l'instant.
- **Nouveau service/DTO réutilisable** : le util reste local au dossier `conclusions-section`
  (scope minimal) ; si un 2ᵉ éditeur long apparaît, promotion en service partagé (backlog).

## Analyse d'impact — préoccupations transversales

- Auth / Principal : ❌ non touché.
- Workspace context : ❌ non touché (pas d'appel serveur nouveau).
- Plans / limites : ❌ non touché.
- Navigation / routing : ❌ non touché.
- Outil décisionnel métier : ❌ non touché.
- **Smoke tests E2E** : non requis (aucune préoccupation transversale cochée ; frontend-only,
  pas d'auth/workspace/navigation).

## Hors périmètre

- Autosave **serveur** (PATCH throttlé) — écarté (arbitrage SF-279-00, réversible).
- Reprise récapitulative à la génération (**F-271**).
- Confirmation avant régénération (**F-278**).
- Diff de versions (**F-280**).
- Synchronisation multi-onglets / multi-appareils du brouillon (le local est mono-navigateur).
