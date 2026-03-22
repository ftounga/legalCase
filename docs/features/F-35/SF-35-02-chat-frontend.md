# Mini-spec — F-35 / SF-35-02 — Chat libre sur dossier — Frontend

## Identifiant

`F-35 / SF-35-02`

## Feature parente

`F-35` — Chat libre sur dossier

## Statut

`in-progress`

## Date de création

2026-03-22

## Branche Git

`feat/SF-35-02-chat-frontend`

---

## Objectif

Ajouter un panneau de chat interactif sur la page synthèse permettant à l'avocat de poser des questions libres sur le dossier et de voir l'historique des réponses IA.

---

## Comportement attendu

### Cas nominal

1. L'avocat est sur `/case-files/:id/synthesis` avec une synthèse DONE
2. En bas de la page, un panneau "Chat avec le dossier" affiche l'historique (GET au chargement)
3. L'avocat saisit une question, coche optionnellement "Analyse approfondie" (Sonnet), clique Envoyer
4. Spinner pendant l'appel POST, bouton désactivé
5. La réponse s'affiche dans la liste avec le modèle utilisé
6. Le champ est vidé après envoi réussi

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| 402 — limite atteinte | Snackbar rouge "Limite de messages atteinte pour ce mois" |
| 424 — synthèse absente | Panneau désactivé, message "Synthèse requise pour utiliser le chat" |
| 500 / réseau | Snackbar rouge "Erreur lors de l'envoi du message" |

---

## Critères d'acceptation

- [ ] Panneau chat affiché en bas de SynthesisComponent
- [ ] Historique chargé au ngOnInit via GET
- [ ] Question à droite, réponse IA à gauche, badge modèle
- [ ] Champ vidé après envoi réussi
- [ ] Spinner pendant envoi, bouton désactivé
- [ ] Checkbox "Analyse approfondie"
- [ ] 402 → snackbar rouge
- [ ] 424 → message dans le panneau, formulaire désactivé
- [ ] Conformité design system

---

## Périmètre

### Hors scope

- Pas de route dédiée
- Pas de temps réel (SSE/WebSocket)
- Pas d'affichage du quota restant

---

## Technique

### Endpoints consommés

| Méthode | URL |
|---------|-----|
| GET | `/api/v1/case-files/{id}/chat` |
| POST | `/api/v1/case-files/{id}/chat` |

### Composants Angular

- `ChatService` — service HTTP (core/services/)
- `ChatMessage` model (core/models/)
- `SynthesisComponent` — étendu avec panneau chat

### Migration Liquibase

- Non applicable (backend déjà mergé)

---

## Plan de test

### Tests unitaires

- [ ] `ChatService.sendMessage()` — appel POST correct
- [ ] `ChatService.getHistory()` — appel GET correct

### Isolation workspace

- Non applicable — portée par le backend

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — SynthesisComponent modifié (pas de nouvelle route)

### Composants impactés

| Composant | Impact | Non-régression |
|-----------|--------|----------------|
| SynthesisComponent | Ajout panneau chat en bas | Vérification manuelle sections existantes |

### Smoke tests E2E concernés

- Aucun smoke test concerné (pas de changement routing/guard)

---

## Dépendances

- [SF-35-01] — statut : done
