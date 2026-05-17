# Mini-spec — F-98 / SF-98-49 — Éditeur de relecture des conclusions

> Cadrages amont : `SF-98-00-coherence.md` (étape 0, invariant 6 « éditeur de relecture ») + `SF-98-00b-ux-coherence.md` (étape 0 bis — l'éditeur s'intègre dans la section conclusions, anticipé explicitement). Pas de nouveau cadrage écran.

## Identifiant
`F-98 / SF-98-49`

## Feature parente
`F-98` — Génération de courrier / conclusions

## Statut
`ready`

## Date de création
2026-05-18

## Branches Git (dev parallélisé)
- `feat/SF-98-49-backend-editeur`
- `feat/SF-98-49-frontend-editeur`

---

## Objectif
Permettre à l'avocat de **modifier le texte** d'une version de conclusions en brouillon, avant de la valider.

---

## Comportement attendu

### Cas nominal
1. Dans la section « Conclusions » (onglet Décision), quand la version affichée est au statut de génération `DONE` et au cycle de vie `DRAFT`, un bouton **« Modifier »** est disponible.
2. L'avocat passe en mode édition : le contenu devient un champ texte éditable. Il modifie, puis **« Enregistrer »** → `PATCH .../conclusions/versions/{versionId}/content`.
3. Le contenu de la version est mis à jour ; la vue repasse en lecture. **« Annuler »** restaure le texte sans appel serveur.
4. Une version `VALIDATED` ou `DEPOSITED` est en **lecture seule** — pour la rééditer, l'avocat la repasse d'abord en `DRAFT` (cycle de vie, SF-98-52).

### Cas d'erreur
| Situation | Comportement | Code |
|---|---|---|
| `PATCH content` sur une version dont la génération n'est pas `DONE` | Rejet — « La version n'est pas encore générée. » | 409 |
| `PATCH content` sur une version `VALIDATED`/`DEPOSITED` | Rejet — « Seul un brouillon peut être modifié. » | 409 |
| `content` vide ou absent | Rejet | 400 |
| Version / dossier inexistant ou autre workspace | Accès refusé | 404 |
| Non authentifié | Rejet | 401 |

---

## Analyse de cohérence transversale
- [x] **Outils décisionnels / F-IA-04** : non applicable (générateur de document).
- [x] **Autres pays / domaines** : l'éditeur est transversal — bénéficiera à toutes les cellules F-98.
- [x] **Modification SF existante** : ajoute un endpoint à `CaseConclusionController` et un bouton à `ConclusionsSectionComponent` — additif, ne modifie aucun contrat existant.

### Décision
- [x] Étendu à la seule cible applicable (la section conclusions) ; rien à dupliquer.

## Conformité F-IA-04
- [x] **Non applicable** — générateur de document, pas un outil décisionnel.

---

## Critères d'acceptation
- [ ] **CA1** — `PATCH .../conclusions/versions/{versionId}/content` met à jour le `content` d'une version `DONE` + `DRAFT` → `200 ConclusionResponse`.
- [ ] **CA2** — `409` si la version n'est pas au statut de génération `DONE`.
- [ ] **CA3** — `409` si la version est `VALIDATED` ou `DEPOSITED` (lecture seule).
- [ ] **CA4** — `400` si `content` est vide/absent.
- [ ] **CA5** — Isolation workspace : `404` pour une version/dossier d'un autre workspace.
- [ ] **CA6** — Frontend : un bouton « Modifier » est visible uniquement pour une version `DONE` + `DRAFT` ; le mode édition propose Enregistrer / Annuler ; Enregistrer appelle le `PATCH` et rafraîchit l'affichage.
- [ ] **CA7** — Frontend : une version `VALIDATED`/`DEPOSITED` n'affiche pas le bouton « Modifier ».

---

## Périmètre
### Hors scope
- **Éditeur WYSIWYG** : V1 = champ texte éditable (`textarea` redimensionnable). Arbitrage : les conclusions sont du texte structuré, la mise en forme finale se fait à l'export Word (SF-98-50) / dans le traitement de texte de l'avocat — un WYSIWYG (dépendance lourde) serait surdimensionné.
- Suivi « modifié manuellement depuis la génération » (badge) — non tracé en V1 (pas de colonne dédiée).
- Diff entre le texte généré et le texte édité.

---

## Technique

### Contrat API (FIGÉ — parallélisation back/front)
| Méthode | URL | Réponses |
|---|---|---|
| PATCH | `/api/v1/case-files/{caseFileId}/conclusions/versions/{versionId}/content` | body `{"content":"..."}` → `200 ConclusionResponse` ; `400` (content vide) ; `409` (version pas `DONE`, ou `VALIDATED`/`DEPOSITED`) ; `404` ; `401` |

### Tables impactées
Aucune — `content` est une colonne existante de `case_conclusions`. **Pas de migration.**

### Composants
- Backend : `CaseConclusionCommandService.updateContent(...)`, endpoint dans `CaseConclusionController`, `ContentUpdateRequest` DTO, garde `409` (réutilise `CaseConclusionGuardException` ou `ResponseStatusException`).
- Frontend : `ConclusionsService.updateContent(...)`, mode édition dans `ConclusionsSectionComponent` (signal `editing`, `textarea`, boutons Modifier / Enregistrer / Annuler).

---

## Plan de test
### Backend
- [ ] UT `CaseConclusionCommandServiceTest` : `updateContent` nominal ; `409` non-`DONE` ; `409` `VALIDATED`/`DEPOSITED` ; `400` content vide.
- [ ] IT `CaseConclusionControllerIT` : `PATCH content` → `200` ; `409` ; `400` ; `404` isolation workspace ; `401`.
### Frontend (Jest)
- [ ] `conclusions-section.component.spec.ts` : bouton « Modifier » visible si `DONE`+`DRAFT` / masqué sinon ; Enregistrer → `PATCH` + rafraîchissement ; Annuler restaure.
- [ ] `conclusions.service.spec.ts` : URL `PATCH .../content`.
### Isolation workspace
- [x] Applicable — testée dans `CaseConclusionControllerIT`.

---

## Analyse d'impact
- [x] **Aucune préoccupation transversale** — SF additive, pas d'impact auth/workspace/plans/navigation.
- [x] Aucun smoke test E2E concerné.

## Dépendances
- `F-98 / SF-98-52` (versions) — **done** : l'éditeur édite le `content` d'une version `DRAFT`.

## Notes et décisions
- Réutilise le contrôle d'isolation workspace (`resolveCaseFileInWorkspace` + `resolveVersion`) de SF-98-52.
- Éditable ⇔ génération `DONE` **et** cycle de vie `DRAFT` — cohérent avec SF-98-52 (une version validée/déposée est figée).
