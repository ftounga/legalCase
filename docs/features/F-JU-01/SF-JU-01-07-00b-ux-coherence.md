# F-JU-01 / SF-JU-01-07 — Cadrage cohérence écran (étape 0 bis)

## Verdict : GO

## Intention métier + comportement visible attendu

Permettre au super-admin de **charger un fichier `.csv`** dans l'onglet « Bootstrap » de
`/super-admin/jurisprudence-watch` plutôt que de copier-coller 200+ lignes manuellement depuis
LibreOffice. Le textarea actuel reste présent (édition manuelle ponctuelle), un bouton
« Charger depuis un fichier .csv » est ajouté juste au-dessus pour pré-remplir le textarea via
`FileReader.readAsText()`. La validation existante (compteur, parse line-by-line, erreurs)
reste inchangée et s'applique immédiatement au contenu chargé.

## Rappel verdict feature-coherence-challenger

SF-JU-01-07 = **étape 0 exempte** (incrément UX additif strict, le workflow super-admin reste
identique : alimenter `tool_jurisprudence_mappings` via `POST /bootstrap`). Pas de nouvelle
capacité métier, pas de nouvel endpoint, pas d'effet sur la chaîne IA.

## Parcours écran réel — super-admin

Identique à SF-JU-01-06, on insère juste un point d'entrée alternatif à l'étape 5 :

1. Super-admin se connecte (OAuth)
2. Ouvre `/super-admin/jurisprudence-watch`
3. Onglet « Bootstrap » (1er, livré SF-JU-01-06)
4. **(nouveau)** Clique sur « Charger depuis un fichier .csv » → sélectionne
   `bootstrap-batch-1.csv` → le textarea se remplit, le compteur affiche `200 / 200`
5. Vérifie visuellement le contenu si nécessaire (édition optionnelle dans le textarea)
6. Clique sur `[Lancer le bootstrap]` → workflow inchangé (POST, snackbar succès, audit log
   rechargé)
7. État terminal : mêmes mappings peuplés que SF-JU-01-06

## État terminal du processus

Identique SF-JU-01-06. Aucune modification de l'état terminal métier. La SF supprime juste un
point de friction d'entrée (copier-coller lourd remplacé par upload fichier).

## Cartographie écrans / zones existants ↔ parcours

| Étape parcours admin | Écran / zone LegalCase | Statut |
|---|---|---|
| 1. Auth super-admin | `/auth` + `SuperAdminGuard` | ✅ existant |
| 2. Ouverture dashboard | `/super-admin/jurisprudence-watch` | ✅ existant (SF-JU-01-05) |
| 3. Onglet Bootstrap | `mat-tab label="Bootstrap"` | ✅ existant (SF-JU-01-06) |
| 4. **Charger fichier .csv** | ❌ **manquant** — copier-coller manuel uniquement | ❌ — sujet de cette SF |
| 5. Vérification compteur | `bootstrap-counter` | ✅ existant |
| 6. Lancement bootstrap | `bootstrap-launch` | ✅ existant |
| 7. État terminal (mappings peuplés) | Onglet « Audit log » | ✅ existant |

## Position candidate de la feature

- **Écran cible** : `/super-admin/jurisprudence-watch`, onglet « Bootstrap »
- **Zone** : juste au-dessus du textarea (et à droite ou à gauche du bouton « Exemple » dans
  le même header de section)
- **Élément** : `mat-stroked-button` accent « Charger depuis un fichier .csv » +
  `<input type="file" accept=".csv,text/csv" hidden>` couplé via `@ViewChild` (pattern standard
  Material — bouton stylé visible qui déclenche `fileInput.click()`)

## Challenge placement

> *« L'écran candidat correspond-il à l'étape du parcours où l'admin a besoin de la feature ? »*

OUI. La sélection de fichier doit être colocalisée avec le textarea cible (pas d'écran ni de
dialog supplémentaire). C'est un raccourci pour pré-remplir un input texte existant.

## Challenge lisibilité de la séquence

> *« L'UI rend-elle visible l'ordre des étapes ? »*

OUI. La séquence visuelle de l'onglet devient :

1. Header « Bootstrap initial des mappings… » + boutons d'action (Exemple + Charger fichier)
2. Aide CSV
3. Textarea (pré-rempli si fichier chargé, sinon vide)
4. Compteur + erreurs
5. Bouton Lancer
6. Résultat dernier batch

L'utilisateur lit naturellement « j'ai un fichier → je le charge → je vérifie → je lance ».

## Challenge charge écran

> *« Quelle est la densité TOTALE de l'écran cible APRÈS ajout ? »*

+1 bouton stroked dans le header de section (déjà 1 bouton « Exemple »). Total = 2 boutons
dans le header. Aucune restructuration. Pas de nouvelle zone, pas de modal.

## Challenge état final / continuité

> *« Après l'output, que fait l'admin ? »*

Identique au cas SF-JU-01-06. Après chargement fichier → vérifie le compteur (le snackbar
informe « X entrées chargées depuis nom-fichier.csv ») → corrige éventuellement dans le
textarea → clique Lancer. Continuité préservée, pas de ping-pong subi.

## Cas d'erreur fichier — UX

| Cas | Comportement |
|---|---|
| Fichier vide | Snackbar `Fichier vide` + textarea inchangé |
| Fichier > 1 Mo | Snackbar `Fichier trop volumineux (max 1 Mo)` + textarea inchangé |
| Fichier > 200 lignes parsées | Le contenu est chargé, snackbar avertit `Fichier > 200 lignes : découpez en plusieurs batches`, le bouton Lancer reste désactivé via `canLaunchBootstrap()` (logique existante) — pas de blocage dur, l'admin peut éditer manuellement |
| Échec lecture (corruption / encoding non UTF-8 non détectable) | Snackbar `Erreur de lecture du fichier` |
| Extension non `.csv` | Filtrée par `accept=".csv,text/csv"` côté navigateur ; double-check JS au `change` (warning soft) |

## Invariants anti-surcharge pour la mini-spec

1. **Ne pas ouvrir de modal de prévisualisation** — le textarea fait déjà office de preview
   éditable.
2. **Ne pas supprimer le textarea** — copier-coller manuel reste un cas d'usage légitime
   (modif rapide ponctuelle de 2-3 entrées).
3. **Ne pas auto-lancer le bootstrap après chargement** — l'admin doit avoir un acte de
   confirmation explicite (clic sur Lancer).
4. **Ne pas accepter `.xlsx` ou `.ods`** — uniquement `.csv` UTF-8. LibreOffice / Excel
   exportent en CSV en 1 clic, pas besoin de parser des binaires.
5. **Aucun stockage côté frontend** du fichier après lecture — `FileReader` lit en mémoire,
   pousse vers `csvInput`, fin.

## Décision finale

**GO** — incrément UX additif, pas de chaîne fonctionnelle nouvelle, pas de risque
transversal. Mise en œuvre frontend pure (~50 lignes TS + bouton template).

## MAJ apportée au parcours écran de référence

Pas de `docs/business/parcours-ecran-*.md` dédié au parcours super-admin — décision SF-JU-01-06
maintenue (un seul écran ops, traçabilité dans cadrages 0 bis suffit).
