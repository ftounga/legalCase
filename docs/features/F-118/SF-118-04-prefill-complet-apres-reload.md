# Mini-spec — F-118 / SF-118-04 Pré-remplissage complet des formulaires après rechargement

## Identifiant

`F-118 / SF-118-04`

## Feature parente

`F-118` — Refonte visuelle des écrans principaux / UX polish

## Statut

`draft`

## Date de création

2026-04-15

## Branche Git

`feat/SF-118-04-prefill-complet-apres-reload`

---

## Objectif

Corriger un bug transversal observé pendant le test 2 Martin : après un rechargement de page (hard refresh), l'avocat qui clique sur "Modifier" dans certains outils décisionnels voit un formulaire avec des champs vides ou aux valeurs par défaut, au lieu du contenu sauvegardé. Concerne F-DT-07 Ancienneté (1 champ sur 5 restauré), F-FA-05 Partage immobilier et F-FA-06 Calendrier garde (aucun champ restauré).

Cause racine : les signals de champs du formulaire conservent leurs valeurs par défaut (chaînes vides, 0) après un reload. Le `editForm()` et parfois le `loadExisting().next` n'appellent pas un `prefillForm()` complet qui restaure tous les champs depuis la réponse sauvegardée.

---

## Comportement attendu

### Cas nominal (après fix)

1. L'avocat ouvre un dossier qui a déjà un résultat sauvegardé pour F-DT-07, F-FA-05 ou F-FA-06.
2. La section charge le résultat (GET /…/xxx → 200). Elle affiche le bloc résultat, formulaire masqué (`showForm=false`).
3. L'avocat clique **Modifier**.
4. Le formulaire apparaît **entièrement pré-rempli avec les valeurs sauvegardées** — identique au comportement après un POST en session continue, sans reload.
5. L'avocat peut modifier n'importe quel champ ; les alertes de cohérence IA (SF-IA-03-12) fonctionnent immédiatement.

### Règle générale retenue

- **Un outil qui persiste un état par dossier DOIT avoir un `prefillForm(resp)` qui restaure *tous* les champs du formulaire** à partir de la réponse.
- **Ce `prefillForm` DOIT être appelé à la fois dans `loadExisting().next` ET dans `editForm()`** — ou `editForm()` restaure les valeurs signal par signal depuis `this.result()`.

Pattern de référence : `indemnite-comparatif-section` (F-DT-09), `immigration-title-decision-section` (F-IM-05), `immigration-recours-section` (F-IM-06) — corrects.

### Périmètre exact du fix

| Outil | Fichier | Problème | Correction |
|-------|---------|----------|-----------|
| F-FA-05 Partage immobilier | `partage-immobilier-section.component.ts` | `editForm()` ne restaure rien, `loadExisting` non plus | Ajouter `prefillForm(resp)` restaurant tous les champs signal du formulaire ; appeler depuis `loadExisting.next` ET `editForm()` |
| F-FA-06 Calendrier garde | `calendrier-garde-section.component.ts` | `editForm()` ne restaure rien | Idem F-FA-05 |

**F-DT-07 retiré du scope pendant l'implémentation** : découverte que `AncienneteResponse` (backend) ne contient que `conventionCode`, pas les autres inputs. La table `anciennete_analyses` ne stocke pas non plus `salaireBase` / `congesContrat` / `primeContrat`. Un vrai fix demande une extension backend (nouvelles colonnes ou JSON `request_data`, migration, extension response) qui excède le scope d'un patch frontend ciblé. Une SF follow-up **SF-DT-07-04 — Persister et exposer les inputs d'ancienneté** sera créée avant d'appliquer le même prefillForm complet côté front.

### Outils **non modifiés**

- F-DT-08 Licenciement : formulaire persistant, pas de cycle form → résultat → modifier (pattern différent).
- F-DT-09 Comparateur : déjà correct.
- F-DT-10 Rupture conventionnelle : déjà correct (`applyReponsesFromResult` complet sur les 6 critères).
- F-FA-07 Checklist divorce : pattern toggle inline (pas de formulaire → résultat).
- F-IM-05 / F-IM-06 / F-IM-07 : déjà corrects.

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Pas de résultat sauvegardé (GET 404) | Aucun prefill depuis résultat — `prefillFromAi()` prend le relais comme aujourd'hui |
| Réponse incomplète (champ null en base) | Le signal prend `null` ou la valeur par défaut — pas de crash |
| Résultat partiel (legacy) | Chaque `set` est conditionnel (`if (resp.field != null)`) sur F-FA-05 et F-FA-06 si les champs peuvent être null en base |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** : 10 outils porteurs d'un formulaire persistant (F-DT-07/08/09/10, F-FA-05/06/07, F-IM-05/06/07)
- [x] **Autres pays** : non applicable — le pattern est structurel, pas dépendant du pays
- [x] **Autres domaines** : applicable aux 3 domaines V1 (couvert par le scan par outil)
- [x] **Autres UI patterns** : le pattern "formulaire → résultat sauvegardé → Modifier" est spécifique à ces outils ; F-DT-08 (formulaire persistant), F-DT-10 (checklist radio) et F-FA-07 (toggle inline) utilisent d'autres patterns
- [x] **Autres flows transversaux** : non applicable (auth/workspace/plans/navigation intactes)

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| F-DT-07 Ancienneté | Partiel | Blocage backend découvert pendant le dev (réponse n'expose pas les inputs). Retiré du scope. Follow-up SF-DT-07-04 à créer. |
| F-DT-08 Validité licenciement | Non | Formulaire persistant — pas de cycle Modifier depuis résultat |
| F-DT-09 Comparateur | Non | `prefillForm` déjà complet (5 champs) |
| F-DT-10 Rupture conventionnelle | Non | `applyReponsesFromResult` déjà complet (6 critères) |
| F-FA-05 Partage immobilier | Oui | Intégré dans cette SF |
| F-FA-06 Calendrier garde | Oui | Intégré dans cette SF |
| F-FA-07 Checklist divorce | Non | Pattern toggle inline (chaque clic persiste) |
| F-IM-05 Titre séjour | Non | `prefillForm` déjà complet (5 champs) |
| F-IM-06 Recours | Non | `prefillForm` déjà complet (10 champs) |
| F-IM-07 Droit au travail | Non | `editForm` restaure country + titreType (2 champs du formulaire) |

### Décision

- [x] Étendu à 2 cibles applicables dans cette subfeature (F-FA-05, F-FA-06)
- [x] Subfeature parallèle à créer : SF-DT-07-04 (persister et exposer les inputs d'ancienneté en backend, puis appliquer le prefillForm complet côté front)
- [ ] Subfeature(s) parallèle(s)
- [ ] Backlog VN
- [x] Non applicable pour 7 autres outils (justification explicite ci-dessus)

---

## Critères d'acceptation

- [ ] `PartageImmobilierSectionComponent` : ajout d'un `prefillForm(resp)` couvrant tous les signals du formulaire ; appelé dans `loadExisting.next` et dans `editForm()`.
- [ ] `CalendrierGardeSectionComponent` : idem.
- [ ] `AncienneteSectionComponent.prefillForm` : commentaire TODO pointant vers SF-DT-07-04 (follow-up backend nécessaire avant complétion).
- [ ] Aucun autre composant modifié.
- [ ] Tests Jest unitaires (≥ 3) : scénario "GET 200 → showForm=false → editForm → form montre toutes les valeurs sauvegardées" sur les 3 outils concernés.
- [ ] Tests existants restent verts (non-régression sur F-DT-09, F-IM-05/06/07).
- [ ] Build Angular OK, ≥ 936 tests frontend verts.

---

## Périmètre

### Hors scope (explicite)

- Toucher les 7 outils déjà corrects (F-DT-08, F-DT-09, F-DT-10, F-FA-07, F-IM-05/06/07).
- Refactorer le pattern en classe abstraite partagée — intéressant à terme mais trop de churn pour ce fix.
- Corriger le timing F5 de masquage F-DT-08 / F-DT-10 (sujet distinct).
- Ajouter des champs qui n'existent pas déjà dans les formulaires.
- Modifier l'API de `AncienneteResponse`, `PartageImmobilierResponse`, `CalendrierGardeResponse`.

---

## Valeurs initiales

Sans objet — correction du comportement d'affichage, pas de valeurs stockées.

---

## Contraintes de validation

Sans objet — pas de nouvelle saisie utilisateur.

---

## Technique

### Endpoints

Aucun.

### Tables impactées

Aucune.

### Migration Liquibase

Non applicable.

### Composants Angular

- `anciennete-section.component.ts` — enrichir `prefillForm(resp)`.
- `partage-immobilier-section.component.ts` — ajouter `prefillForm(resp)`, brancher dans `loadExisting` et `editForm`.
- `calendrier-garde-section.component.ts` — idem.
- 3 specs Jest enrichis avec le scénario "GET 200 → editForm → champs restaurés".

### Backend

Aucun impact.

---

## Plan de test

### Tests unitaires Jest

- [ ] `AncienneteSectionComponent` : `GET 200 → editForm → dateEntree/salaire/congés/prime restaurés` (en plus de conventionCode déjà testé).
- [ ] `PartageImmobilierSectionComponent` : `GET 200 → editForm → tous les signals du formulaire pré-remplis`.
- [ ] `CalendrierGardeSectionComponent` : `GET 200 → editForm → mode de garde + tous les champs pré-remplis`.
- [ ] Non-régression : les specs existants des 3 composants restent verts.

### Tests d'intégration

- [x] N/A — frontend pur.

### Isolation workspace

- [x] N/A.

### Validation manuelle

- [ ] Staging : dossier Martin → reload → F-DT-07 bloc résultat → Modifier → les 5 champs sont pré-remplis avec les valeurs sauvegardées.
- [ ] Idem sur un dossier divorce pour F-FA-05 et F-FA-06.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [ ] Auth / Principal
- [ ] Workspace context
- [ ] Plans / limites
- [ ] Navigation / routing frontend
- [x] **Aucune** — correction d'un comportement local à 3 composants.

### Composants / endpoints existants potentiellement impactés

| Composant | Impact | Test de non-régression |
|-----------|--------|-----------------------|
| F-DT-07 anciennete | Ajoute 4 lignes dans `prefillForm` | Specs Jest existants |
| F-FA-05 partage | Ajoute `prefillForm` + 2 appels | Specs Jest existants |
| F-FA-06 calendrier-garde | Idem | Specs Jest existants |
| 7 autres outils | Aucun | — |

### Smoke tests E2E concernés

- [ ] Aucun — comportement purement client.

---

## Dépendances

### Subfeatures bloquantes

- Aucune bloquante. `SF-IA-03-12` Done — les alertes de cohérence s'afficheront correctement une fois le formulaire pré-rempli.

### Questions ouvertes impactées

- [ ] Aucune.

---

## Notes et décisions

- **Pourquoi rattacher à F-118** : la feature parente regroupe les polish UX / refontes visuelles. Le fix relève de la qualité UX transversale. Pas besoin d'une nouvelle feature racine.
- **Pourquoi ne pas extraire une classe abstraite `BasePersistedToolComponent`** : refactoriser 3 composants pour extraire un pattern commun est tentant mais augmente la surface de changement et le risque de régression. On corrige localement pour cette passe ; si un 4e outil émerge plus tard on reconsidérera l'abstraction.
- **Pourquoi pas un test E2E** : le bug est observable via Jest + DOM inspection, et la validation manuelle staging suffit pour confirmer en réel. Ajouter un E2E dédié aurait un coût disproportionné vs la valeur.
- **Pourquoi 2 outils sans `prefillForm` du tout** : historiquement F-FA-05 et F-FA-06 ont été implémentés avec un cycle form → résultat court où l'utilisateur n'effaçait jamais son travail en session, donc les signals gardaient naturellement les valeurs. Le comportement casse au reload, d'où le fix.
