# Mini-spec — F-JU-01 / SF-JU-01-07 Upload fichier CSV dans le bootstrap

## Identifiant

`F-JU-01 / SF-JU-01-07`

## Feature parente

`F-JU-01` — Citations jurisprudentielles dans les outils décisionnels (FR + BE)

## Statut

`draft`

## Date de création

2026-05-25

## Branche Git

`feat/SF-JU-01-07-bootstrap-csv-file-upload`

---

## Objectif

Ajouter un bouton « Charger depuis un fichier .csv » dans l'onglet Bootstrap de
`/super-admin/jurisprudence-watch` qui lit le fichier sélectionné via `FileReader` et alimente
le textarea CSV existant (parsing, compteur, validation inchangés).

---

## Comportement attendu

### Cas nominal

1. Super-admin ouvre `/super-admin/jurisprudence-watch` → onglet « Bootstrap »
2. Clique sur le bouton stroked **« Charger depuis un fichier .csv »** dans le header de
   section
3. Sélecteur fichier OS s'ouvre, filtré sur `.csv` (`accept=".csv,text/csv"`)
4. Sélectionne `bootstrap-batch-1.csv` (≤ 1 Mo, UTF-8)
5. `FileReader.readAsText(file, 'UTF-8')` lit le contenu
6. Le textarea `csvInput` est remplacé par le contenu lu
7. `onCsvInputChange()` est invoqué → re-parse → compteur live mis à jour
8. Snackbar info : `Fichier "bootstrap-batch-1.csv" chargé (N entrées détectées)`
9. L'admin vérifie le compteur, clique sur `[Lancer le bootstrap]` (logique inchangée)

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Aucun fichier sélectionné (dialog annulé) | Aucun changement, pas de snackbar |
| Fichier vide (`size === 0`) | Snackbar `Fichier vide`, textarea inchangé |
| Fichier > 1 Mo (`size > 1_048_576`) | Snackbar `Fichier trop volumineux (max 1 Mo)`, textarea inchangé |
| Échec lecture `FileReader.onerror` | Snackbar `Erreur de lecture du fichier`, textarea inchangé |
| Fichier > 200 lignes parsées | Le contenu est chargé, snackbar info `Fichier chargé : N entrées (> 200, découpez en batches)`, le bouton Lancer reste désactivé par `canLaunchBootstrap()` (logique existante via `parseResult.errors` + count) — l'admin peut éditer manuellement pour réduire |
| Extension fichier non `.csv` | Filtrée par `accept`, mais en double-check JS : snackbar `Format attendu : .csv` si MIME ≠ csv/text — pas de blocage dur |
| Multi-upload simultané (re-clic pendant lecture) | Le bouton fichier déclenche un nouveau `FileReader` ; le `<input type="file">` est reset après chaque lecture pour permettre re-sélectionner le même fichier |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — Non applicable : SF d'outillage ops sur écran super-admin
  existant, pas un outil décisionnel.
- [x] **Autres pays** — Non applicable : écran ops global, pas de gate FR/BE.
- [x] **Autres domaines** — Non applicable.
- [x] **Autres UI patterns** — Le pattern « bouton trigger + `<input type="file" hidden>` »
  est standard Material Design. Aucune réutilisation transversale projetée.
- [x] **Autres flows transversaux** — Touche uniquement `JurisprudenceWatchComponent`. Pas
  d'impact auth, workspace, plans, navigation. Aucun service partagé modifié.

### Niveaux de vérification à couvrir

- [x] **Modèle TypeScript / API exposée** — aucun changement (réutilise types
  `JurisprudenceBootstrapEntry` existants).
- [x] **Record / DTO backend** — aucun changement, frontend pur.
- [x] **Service / logique métier** — aucun changement service. Méthode locale
  `onFileSelected(event)` ajoutée au composant.
- [x] **Entité JPA + schéma DB** — aucun changement.
- [x] **Tests existants** — `jurisprudence-watch.component.spec.ts` (10 tests T-01 à T-10),
  à étendre avec 3 tests dédiés au nouveau path (lecture OK, fichier vide, fichier > 1 Mo).

### Cas spécifique : nouvelle feature d'outil décisionnel

**Non applicable** — outil ops super-admin, pas un outil décisionnel avocat (pas
d'`app-XXX-section`, pas de `TOOL_REGISTRY`, pas de pré-fill IA).

### Cas spécifique : nouveau pattern UI ou service partagé

**Non applicable** — bouton + `<input type="file">` est un pattern Material standard, pas un
composant partagé introduit.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Outils décisionnels avocat (F-DT/F-FA/F-IM) | Non | Écran ops |
| Pays FR/BE | Non | Endpoint backend déjà global |
| Domaines métier | Non | Idem |
| Pattern UI shared | Non | Pattern Material standard, local au composant |
| Auth/Workspace/Plans/Nav | Non | Réutilise `SuperAdminGuard` + route existants |

### Décision

- [x] Non applicable aux autres cibles (justification : SF d'outillage UX additif sur écran
  super-admin existant, aucune chaîne de propagation).

---

## Conformité F-IA-04 (SF frontend décisionnelle)

- [x] **Non applicable** — pas une section décisionnelle (pas d'`app-XXX-section`, pas de
  `TOOL_REGISTRY`).

---

## Champs IA à extraire (pré-remplissage)

- [x] **Aucun pré-remplissage IA** — saisie manuelle / fichier fourni par le super-admin.

---

## Critères d'acceptation

- [ ] **CA-01** : un bouton `mat-stroked-button` portant le label « Charger depuis un fichier
  .csv » et l'attribut `data-test="bootstrap-file-upload"` est présent dans le header de
  section Bootstrap, à côté du bouton « Exemple »
- [ ] **CA-02** : le bouton est associé à un `<input type="file" accept=".csv,text/csv"
  hidden #fileInput>` dans le template ; le clic sur le bouton déclenche `fileInput.click()`
- [ ] **CA-03** : à la sélection d'un fichier `.csv` valide, `FileReader.readAsText(file,
  'UTF-8')` lit le contenu et appelle `onCsvInputChange(text)` qui met à jour `csvInput` et
  `parseResult`
- [ ] **CA-04** : un snackbar info `Fichier "<nom>" chargé (N entrées détectées)` s'affiche
  après lecture réussie
- [ ] **CA-05** : fichier vide → snackbar `Fichier vide`, textarea inchangé, pas d'appel
  `onCsvInputChange`
- [ ] **CA-06** : fichier > 1 Mo → snackbar `Fichier trop volumineux (max 1 Mo)`, textarea
  inchangé
- [ ] **CA-07** : erreur de lecture (`FileReader.onerror`) → snackbar `Erreur de lecture du
  fichier`, textarea inchangé
- [ ] **CA-08** : le `<input type="file">` est reset après chaque tentative (réussie ou
  échouée) pour permettre de re-sélectionner le même fichier
- [ ] **CA-09** : la séquence existante (compteur, erreurs, bouton Lancer) fonctionne à
  l'identique après chargement de fichier — aucune régression sur les tests T-01 à T-10
- [ ] **CA-10** : `ChangeDetectorRef.markForCheck()` est invoqué après mutation de `csvInput`
  / `parseResult` dans le callback `FileReader.onload` (cf. mémoire
  `feedback_onpush_subscribe_markforcheck` — OnPush + mutation async)

---

## Périmètre

### Hors scope (explicite)

- Pas de support `.xlsx`, `.ods`, `.tsv` (CSV UTF-8 uniquement)
- Pas de modal de prévisualisation (le textarea fait office de preview)
- Pas d'auto-lancement du bootstrap après upload (l'admin doit cliquer Lancer)
- Pas de découpage automatique en batches > 200 (V2 si la friction émerge — le hors-scope est
  documenté dans le snackbar info quand le fichier dépasse 200 lignes)
- Pas de drag & drop (V2 éventuelle — la souris à un bouton trigger reste suffisante pour
  un usage ponctuel)
- Pas de modification backend (l'endpoint `POST /bootstrap` reste tel quel)
- Pas de mémorisation du dernier fichier chargé (pas de localStorage)

---

## Valeurs initiales

Pas de nouvelle entité. Le fichier est lu en mémoire et son contenu pousse vers `csvInput`.

---

## Contraintes de validation

| Champ | Obligatoire | Limite | Format | Normalisation |
|-------|-------------|--------|--------|---------------|
| File.size | Oui | > 0 et ≤ 1 048 576 octets (1 Mo) | — | — |
| File.name | Oui | — | extension `.csv` (filtré par `accept`) | — |
| Contenu | Oui | — | texte UTF-8 multi-lignes au format CSV | `readAsText(file, 'UTF-8')` |

Les contraintes ligne par ligne (toolId regex, motCleRecherche non vide, etc.) sont déjà
appliquées par `parseBootstrapCsv()` (SF-JU-01-06) et restent inchangées.

---

## Technique

### Endpoint(s)

Aucun nouveau endpoint backend. SF frontend pure.

### Tables impactées

Aucune table directement. Bootstrap downstream peuple `tool_jurisprudence_mappings` et
`jurisprudence_audit_log` (inchangé SF-JU-01-05).

### Migration Liquibase

- [x] Non applicable.

### Composants Angular

- `JurisprudenceWatchComponent` étendu avec :
  - propriété `loadingFile = false` (verrou pendant lecture)
  - constante `BOOTSTRAP_MAX_FILE_SIZE_BYTES = 1_048_576`
  - méthode `onFileSelected(event: Event)` qui :
    - récupère le `File` via `input.files[0]`
    - check `size === 0` → snackbar erreur, return
    - check `size > MAX` → snackbar erreur, return
    - instancie `FileReader`, branche `onload` (push vers `onCsvInputChange` + snackbar +
      `markForCheck`) et `onerror` (snackbar + `markForCheck`)
    - appelle `readAsText(file, 'UTF-8')`
    - reset `input.value = ''` après pour permettre re-upload du même fichier
  - méthode `triggerFileSelector()` qui appelle `this.fileInput.nativeElement.click()`
  - `@ViewChild('fileInput', { static: false })` sur l'input file
- Template HTML : ajout du bouton + input hidden dans le header de section Bootstrap
- SCSS : aucun nouveau style requis (réutilise les classes existantes du header)

---

## Plan de test

### Tests unitaires / composant Jest

Tests ajoutés à `jurisprudence-watch.component.spec.ts` :

- [ ] **T-11** — `onFileSelected` avec un Blob CSV valide remplit `csvInput` + `parseResult`
  et déclenche le snackbar info `Fichier ... chargé`
- [ ] **T-12** — `onFileSelected` avec un fichier vide (`size === 0`) déclenche le snackbar
  `Fichier vide` et ne modifie pas `csvInput`
- [ ] **T-13** — `onFileSelected` avec un fichier > 1 Mo déclenche le snackbar `Fichier trop
  volumineux` et ne modifie pas `csvInput`

(Les 10 tests existants T-01 à T-10 doivent rester 100 % verts — non-régression obligatoire.)

### Tests d'intégration

Aucun test d'intégration backend (frontend pur). Validation manuelle staging post-deploy :
charger les 2 fichiers `docs/operations/jurisprudence-watch/bootstrap-batch-{1,2}.csv` et
vérifier l'exécution de bout en bout.

### Isolation workspace

- [x] **Non applicable** — endpoint super-admin global, pas de workspace_id.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Aucune** — pas d'auth modifiée (réutilise `SuperAdminGuard`), pas de workspace, pas
  de plan, pas de routing. SF additive 100 % UX.

### Composants / endpoints existants potentiellement impactés

Aucun. Le composant `JurisprudenceWatchComponent` est l'unique consommateur des endpoints
`/super-admin/jurisprudence-watch/*`. Le bouton fichier est additif sur la section Bootstrap.

### Smoke tests E2E concernés

- [x] **Aucun** — pas de smoke E2E sur `/super-admin/*` à date. Test manuel post-deploy
  staging suffit.

---

## Dépendances

### Subfeatures bloquantes

- `SF-JU-01-06` — **done** (livré PR #1293 — fournit l'onglet Bootstrap, le textarea, le
  bouton Exemple, la logique `onCsvInputChange`, `parseBootstrapCsv`, `runBootstrap`)
- `SF-JU-01-05` — **done** (endpoint backend `POST /bootstrap`)

### Questions ouvertes impactées

- [x] Aucune.

---

## Notes et décisions

- Le pattern « bouton stylé + `<input type="file" hidden>` couplé via `@ViewChild` +
  `click()` » est standard Material Design (`mat-stroked-button` n'a pas de natif file
  trigger). Préféré au natif `<input type="file">` brut qui rend très mal côté UX.
- L'encoding `UTF-8` est forcé explicitement dans `readAsText(file, 'UTF-8')` car les CSV
  exportés depuis LibreOffice / Excel peuvent par défaut être en latin-1 / Windows-1252 ;
  l'utilisateur est responsable de sauvegarder en UTF-8 côté LibreOffice (option à la
  sauvegarde). Si caractères mal interprétés → ils apparaîtront dans le textarea et l'admin
  pourra corriger manuellement ou re-exporter en UTF-8.
- La limite 1 Mo est volontairement laxiste vs taille réelle attendue : un CSV de 200 lignes
  ~ 60 caractères/ligne = ~12 Ko. Marge x80 pour absorber tout cas anormal sans surprise.
- Le reset `input.value = ''` après chaque lecture est nécessaire car le navigateur ne
  re-trigger pas l'event `change` si on re-sélectionne le même fichier (comportement
  standard HTML).
- Le `markForCheck()` dans `FileReader.onload` est obligatoire (OnPush + mutation async hors
  de la zone Angular = vue figée — mémoire `feedback_onpush_subscribe_markforcheck` cas
  F-120 /blog staging 2026-05-04).
