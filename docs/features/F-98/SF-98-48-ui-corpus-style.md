# Mini-spec — F-98 / SF-98-48 — Écran cabinet de gestion du corpus de style

> Cadrages amont : `SF-98-46-00-coherence.md` (étape 0) + `SF-98-46-00b-ux-coherence.md` (étape 0 bis — écran dédié `/workspace/style-learning`, ajustements b1/b2/b3).

## Identifiant
`F-98 / SF-98-48`

## Feature parente
`F-98` — Génération de courrier / conclusions (bloc style learning)

## Statut
`ready`

## Date de création
2026-05-18

## Branche Git
- `feat/SF-98-48-frontend-corpus-style` — **SF frontend pure**. Code contre le contrat API figé de `SF-98-46-ingestion-corpus-style.md`.

---

## Objectif
Fournir à l'avocat un écran cabinet pour **constituer et gérer son corpus de style** : téléverser des conclusions de référence, voir leur état, les activer/désactiver, les supprimer.

---

## Comportement attendu

### Cas nominal
1. Une entrée **« Corpus de style »** apparaît dans la rubrique **GESTION** du menu latéral → route `/workspace/style-learning`.
2. L'écran `StyleCorpusComponent` affiche :
   - une **zone de dépôt / bouton d'upload** de conclusions de référence ;
   - la **liste des documents** du corpus (nom, statut `PENDING/PROCESSING/DONE/FAILED`, date) avec, par document, un **toggle actif/inactif** et une action **supprimer** ;
   - un texte expliquant l'usage (le style est appris, le contenu client n'est pas conservé — cohérent RGPD) ;
   - un état vide explicite (« Aucune conclusion de référence — téléversez-en pour que la génération adopte votre style »).
3. L'upload appelle `POST .../style-corpus/documents` ; la liste est rechargée ; un **polling léger** suit les documents `PENDING/PROCESSING` jusqu'à `DONE/FAILED`.
4. Toggle → `PATCH .../{id}` `{active}` ; supprimer → `DELETE .../{id}` (avec confirmation `MatDialog`).
5. **Découvrabilité (ajustement b1)** : la section « Conclusions » du dossier affiche un lien vers `/workspace/style-learning` quand le corpus est vide.
6. **Boucle de feedback (ajustement b2)** : la section « Conclusions » indique, sur une version générée, si l'adaptation de style est active (`ConclusionResponse.styleApplied`, fourni par SF-98-47).

### Cas d'erreur
| Situation | Comportement |
|---|---|
| Upload d'un type/taille non supporté | Message `MatSnackBar` (le backend renvoie `400`) |
| Échec d'un appel (`GET`/`PATCH`/`DELETE`) | `MatSnackBar` d'erreur, écran non cassé |
| Document au statut `FAILED` | Affiché avec son `errorMessage`, action « supprimer » disponible |

---

## Analyse de cohérence transversale
- [x] **Navigation / routing** coché — nouvelle route `/workspace/style-learning` + entrée de menu GESTION. Composants impactés : `app.routes.ts` (ajout route), `shell.component` (ajout entrée). Aucune route/guard existant modifié. Pas de garde d'auth nouvelle (route workspace standard).
- [x] **Réutilisation upload** : pattern d'upload de fichiers (`document.service` côté dossier) comme référence ; ici cible workspace.
- [x] **Modifie `conclusions-section.component`** (ajustements b1/b2) — ajout d'un lien + d'un indicateur ; additif.

### Décision
- [x] Étendu aux cibles du cadrage écran (nouvel écran + lien/indicateur dans la section conclusions).

## Conformité F-IA-04
- [x] **Non applicable** — écran de gestion de corpus, pas un outil décisionnel ; pas d'entrée `TOOL_REGISTRY`.

---

## Critères d'acceptation
- [ ] **CA1** — Entrée « Corpus de style » dans la rubrique GESTION → route `/workspace/style-learning` affiche `StyleCorpusComponent`.
- [ ] **CA2** — L'upload d'un document appelle `POST .../style-corpus/documents` et la liste se met à jour.
- [ ] **CA3** — La liste affiche nom, statut, date ; les documents `PENDING/PROCESSING` sont suivis par polling jusqu'à l'état terminal.
- [ ] **CA4** — Le toggle actif/inactif appelle `PATCH` ; la suppression appelle `DELETE` après confirmation `MatDialog`.
- [ ] **CA5** — État vide explicite quand le corpus est vide.
- [ ] **CA6** — La section « Conclusions » du dossier affiche un lien vers `/workspace/style-learning` quand le corpus est vide (ajustement b1).
- [ ] **CA7** — La section « Conclusions » signale, sur une version `DONE`, si `styleApplied = true` (ajustement b2).
- [ ] **CA8** — Erreurs via `MatSnackBar`, aucun `alert()/confirm()` ; palette navy/or, espacements multiples de 4px.

---

## Périmètre
### Hors scope
- Backend du corpus — SF-98-46.
- Logique d'injection du style — SF-98-47.
- Visualisation du « profil de style » agrégé (la signature n'est pas exposée par l'API — cf. SF-98-46).

---

## Technique

### Endpoints consommés (contrat figé SF-98-46)
`POST` / `GET` / `PATCH` / `DELETE` `/api/v1/workspaces/{workspaceId}/style-corpus/documents[/{id}]` — cf. `SF-98-46-ingestion-corpus-style.md`.

### Composants Angular
- `StyleCorpusComponent` (`/workspace/style-learning`), standalone, `OnPush` + signals + `markForCheck()`.
- `StyleCorpusService` — 4 méthodes HTTP.
- `style-corpus.model.ts` — `StyleCorpusDocumentSummary`, `StyleCorpusDocumentStatus`.
- `app.routes.ts` — route `workspace/style-learning`.
- `shell.component` — entrée de menu « Corpus de style » (rubrique GESTION).
- `conclusions-section.component` — lien de découvrabilité (b1) + indicateur `styleApplied` (b2).

### Workspace courant
Le `workspaceId` est résolu depuis le contexte workspace courant du frontend (pattern des écrans `workspace/*` existants — `workspace-admin`, etc.).

---

## Plan de test
### Frontend (Jest)
- [ ] `style-corpus.component.spec.ts` : montage → `GET` ; upload → `POST` + recharge ; polling `PENDING`→`DONE` ; toggle → `PATCH` ; suppression → `MatDialog` + `DELETE` ; état vide ; `FAILED` affiché.
- [ ] `style-corpus.service.spec.ts` : URLs des 4 méthodes.
- [ ] `conclusions-section.component.spec.ts` : lien découvrabilité si corpus vide (b1) ; indicateur `styleApplied` (b2).
### Isolation workspace
- [x] Non applicable côté frontend — l'isolation est garantie backend (SF-98-46) ; le frontend envoie le `workspaceId` courant.

---

## Analyse d'impact
- [x] **Navigation / routing** coché — nouvelle route + entrée de menu. Guards : la route `/workspace/style-learning` réutilise le guard d'authentification des routes `workspace/*` existantes (aucun guard nouveau). Redirections : aucune.
- [x] Smoke test E2E : `e2e/smoke/navigation.spec.ts` doit passer sans régression (nouvelle route ajoutée). À exécuter avant push.

## Dépendances
- **SF-98-46** — contrat API figé (parallélisable) ; doit être mergée avant le merge de SF-98-48 (les endpoints consommés doivent exister — cf. règle pré-merge endpoint check).
- **SF-98-47** — fournit `ConclusionResponse.styleApplied` (ajustement b2) ; si SF-98-47 n'est pas encore mergée au moment du dev, l'indicateur b2 se base sur le champ optionnel `styleApplied` (absent ⇒ indicateur masqué, dégradation propre).

## Notes et décisions
- L'écran reste **un seul écran** (invariant anti-surcharge 2) : upload + liste + activation cohabitent, volume attendu faible.
- Smoke test navigation à lancer avant push (préoccupation transversale navigation cochée).
