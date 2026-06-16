# SF-289-07 — Réponse inline aux questions IA même en présence de plusieurs

> Extension de **F-289 V1.1 / SF-289-02** (actions inline du bloc « ce qui requiert ton attention »). Exempte étape 0/0bis (ajustement de comportement d'une action existante, pas de nouvel écran).

## Objectif (une phrase)

Permettre de **répondre inline** à une question IA depuis la Vue d'ensemble **même quand plusieurs questions sont sans réponse** (une à la fois), au lieu de router vers l'onglet Analyse.

## Constat (vérifié en base)

SF-289-02 ne portait l'`questionId` de l'item d'attention QUESTION_IA **que s'il y avait exactement 1 question sans réponse** (sinon `null` → routage). Or au test, le dossier avait **3 questions sans réponse** → « Répondre » **redirigeait** vers Analyse, contredisant la promesse « Vue d'ensemble actionnable ». L'avocat ne pouvait traiter **aucune** question sur place dès qu'il y en avait ≥ 2.

## Comportement nominal

- Le backend porte **toujours** sur l'item QUESTION_IA l'`questionId` **et le texte** de la **1ʳᵉ question sans réponse** (ordre d'`order_index`), quel que soit le nombre.
- Le `label` reste le **compteur** (« 3 questions sans réponse ») pour signaler combien il en reste.
- Front : « Répondre » ouvre le **champ inline** + affiche **le texte de la 1ʳᵉ question** (l'avocat sait à quoi il répond). À l'envoi (`POST /ai-questions/{id}/answer`), l'overview se recharge → la **question suivante** devient la 1ʳᵉ → son texte s'affiche, et ainsi de suite jusqu'à épuisement (l'item disparaît).

## Cas d'erreur / limites

1. **0 question sans réponse** → pas d'item QUESTION_IA (inchangé).
2. **Échec du POST** → snackbar non bloquant, item conservé (pattern SF-289-02).
3. **Réponse vide** → bouton désactivé (inchangé).

## Contrat API (backend → frontend)

`OverviewResponse.AttentionItem` gagne un 7ᵉ champ **`questionText: String | null`** (non null uniquement pour QUESTION_IA). `questionId` devient non null dès qu'il y a ≥ 1 question sans réponse (avant : seulement si exactement 1).

## Critères d'acceptation vérifiables

- [ ] Dossier avec **3 questions sans réponse** → l'item QUESTION_IA porte `questionId` (1ʳᵉ) + `questionText` (1ʳᵉ) ; label « 3 questions sans réponse ».
- [ ] Front : « Répondre » ouvre l'inline + affiche le texte de la 1ʳᵉ question (pas de routage).
- [ ] Après réponse → recharge → la 2ᵉ question devient courante (texte affiché) ; après la dernière → item disparu.
- [ ] Dossier avec **1 question** → comportement inchangé (inline).
- [ ] Pièces / échéances / analyse obsolète inchangés (`questionText` = null).

## Plan de test minimal

- **Backend** (`OverviewServiceTest`) : 3 questions → `questionId` + `questionText` = 1ʳᵉ ; 1 question → idem ; mise à jour du test `multipleUnansweredQuestions...` (n'attend plus `null`).
- **Frontend** (`case-overview.component.spec.ts`) : `isInlineActionable` vrai pour QUESTION_IA multi ; le texte de la question s'affiche ; submit → reload.
- **Isolation workspace** : N/A (lecture via overview déjà résolu par workspace ; POST réponse déjà gaté).

## Composants impactés

- `OverviewResponse.java` (champ `questionText`), `OverviewService.java` (buildAttention QUESTION_IA).
- `overview.model.ts`, `case-overview.component.{ts,html}`.
- Specs backend + frontend.

**Aucune** : migration, nouvel endpoint (réutilise `POST /ai-questions/{id}/answer`).

## Hors périmètre

- Répondre à plusieurs questions en une fois (batch) — on traite une à une.
- Afficher toutes les questions d'un coup dans la Vue d'ensemble (le compteur + 1 à la fois suffit ; l'onglet Analyse reste la vue exhaustive).

## Analyse transversale

- **Auth/workspace** : inchangés (overview résolu par workspace ; POST réponse F-94 déjà gaté). **Navigation** : on **supprime** un routage (moins de navigation), pas d'ajout de route. **Smoke E2E** : N/A.
