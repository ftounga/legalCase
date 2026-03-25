# SF-52-01 — Upload multi-documents : sélection batch et soumission différée

## Objectif

Permettre à l'avocat de sélectionner plusieurs documents en une fois et de les uploader tous d'un coup via un bouton dédié, au lieu d'un upload immédiat fichier par fichier.

---

## Comportement nominal

**Avant (actuel) :**
- Clic "Ajouter un document" → sélection 1 fichier → upload immédiat

**Après :**
1. Clic "Ajouter des documents" → sélecteur multi-fichiers (`multiple` sur l'`<input>`)
2. Les fichiers sélectionnés s'ajoutent à une **liste en attente** (panier local, pas encore uploadés)
3. La liste affiche : nom du fichier, taille, bouton ✕ pour retirer un fichier du panier
4. Un bouton "**Uploader les documents (N)**" devient actif dès qu'au moins 1 fichier est en attente
5. Clic "Uploader" → N appels parallèles `DocumentService.upload()` vers l'endpoint existant
6. Pendant l'upload : spinner global, boutons désactivés
7. À la fin : snackbar succès/erreur par fichier, panier vidé, liste de documents rechargée

---

## Cas d'erreur

- Fichier > 50 Mo → rejeté à la sélection, snackbar erreur, les autres sont conservés dans le panier
- Upload individuel échoue (erreur 4xx/5xx) → snackbar erreur pour ce fichier, les autres sont uploadés quand même
- Erreur 402 (gate billing) → snackbar "Limite de documents atteinte", upload stoppé

---

## Critères d'acceptation

- [ ] Le sélecteur accepte plusieurs fichiers simultanément (attribut `multiple`)
- [ ] Les fichiers sélectionnés s'affichent dans un panier avant upload
- [ ] On peut retirer un fichier du panier avant upload
- [ ] On peut ajouter plusieurs fois (clics multiples sur "Ajouter") pour compléter le panier
- [ ] Le bouton "Uploader" indique le nombre de fichiers en attente
- [ ] L'upload est déclenché uniquement sur clic "Uploader"
- [ ] Fichier > 50 Mo → rejeté à la sélection, snackbar, reste du panier intact
- [ ] Les gates `canUpload()` existantes s'appliquent au bouton "Uploader"
- [ ] Après upload complet : panier vidé, liste de documents mise à jour

---

## Plan de test

| ID | Scénario | Attendu |
|----|----------|---------|
| U-01 | Sélection 3 fichiers valides | Affichés dans le panier, aucun appel `upload()` |
| U-02 | Clic ✕ sur un fichier du panier | Fichier retiré du panier |
| U-03 | Clic "Ajouter" une seconde fois | Fichiers ajoutés au panier existant (cumul) |
| U-04 | Clic "Uploader" avec 3 fichiers | 3 appels `upload()` lancés en parallèle via `forkJoin` |
| U-05 | Fichier > 50 Mo sélectionné | Rejeté à la sélection, snackbar erreur, panier intact |
| U-06 | Un upload échoue (mock erreur 500) | Snackbar erreur pour ce fichier, les autres passent |
| U-07 | `canUpload() = false` | Bouton "Uploader" désactivé |

---

## Composants impactés

- `case-file-detail.component.ts` — logique panier + upload parallèle
- `case-file-detail.component.html` — template panier + nouveaux boutons
- Aucun changement backend
- Aucune migration de schéma

---

## Hors périmètre

- Barre de progression par fichier
- Drag & drop
- Preview des fichiers
- Changement d'endpoint backend
