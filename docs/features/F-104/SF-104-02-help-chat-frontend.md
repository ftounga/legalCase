# Mini-spec — F-104 / SF-104-02 Chatbot d'aide produit — frontend

## Identifiant
`F-104 / SF-104-02`

## Feature parente
`F-104` — Chatbot d'aide produit intégré

## Statut
`in-progress`

## Date de création
2026-04-02

## Branche Git
`feat/SF-104-02-help-chat-frontend`

---

## Objectif

Ajouter un widget flottant (bulle + panneau de chat) visible sur toutes les pages authentifiées, permettant à l'utilisateur de poser une question sur l'utilisation du produit et d'obtenir une réponse de l'IA.

---

## Comportement attendu

### Cas nominal

1. Un utilisateur authentifié voit une bulle flottante en bas à droite de l'écran (icône `help_outline`).
2. Il clique sur la bulle → un panneau de chat s'ouvre au-dessus de la bulle.
3. Le panneau affiche un titre "Aide", un bouton fermer, 3 questions suggérées et un champ de saisie.
4. L'utilisateur clique sur une question suggérée ou saisit sa propre question (max 500 chars) et clique sur Envoyer.
5. Le champ est désactivé, un indicateur de chargement apparaît.
6. La réponse de l'IA s'affiche dans le panneau.
7. L'utilisateur peut poser une nouvelle question (le champ se réinitialise).
8. Cliquer à nouveau sur la bulle ou sur le bouton fermer ferme le panneau.

### Questions suggérées (3 fixes)

- "Comment créer un dossier ?"
- "Comment ajouter un document ?"
- "Comment lancer une analyse IA ?"

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Réponse API 503 | Message d'erreur inline dans le panneau : "Service temporairement indisponible." |
| Erreur réseau | Message d'erreur inline dans le panneau : "Impossible de joindre le service." |
| Message vide | Bouton Envoyer désactivé (pas d'appel API) |
| Message > 500 chars | Compteur de caractères rouge, bouton Envoyer désactivé |

---

## Critères d'acceptation

- [ ] La bulle flottante est visible sur toutes les pages authentifiées (position: fixed, bottom-right)
- [ ] La bulle est masquée quand l'utilisateur n'est pas authentifié (`authService.currentUser() === null`)
- [ ] Clic sur la bulle → panneau ouvert ; reclic ou clic sur fermer → panneau fermé
- [ ] 3 questions suggérées cliquables pré-remplissent le champ et envoient directement
- [ ] Message vide → bouton Envoyer désactivé
- [ ] Message > 500 chars → compteur rouge + bouton désactivé
- [ ] Pendant le chargement : champ et bouton désactivés, spinner visible
- [ ] Réponse affichée dans le panneau après succès
- [ ] Erreur 503 ou réseau → message d'erreur inline (pas de snackbar)
- [ ] `HelpService.chat()` appelle `POST /api/v1/help/chat`

---

## Périmètre

### Hors scope
- Historique de conversation (chaque question est indépendante)
- Persistance des échanges entre sessions
- Streaming de la réponse
- Markdown rendering de la réponse (texte brut suffisant en V1)
- Modale plein écran (panneau compact uniquement)

---

## Technique

### Endpoint consommé

| Méthode | URL | Auth |
|---------|-----|------|
| POST | `/api/v1/help/chat` | Oui (session cookie) |

### Composants Angular

- `HelpChatWidgetComponent` — composant standalone, ajouté dans `AppComponent`. Gère la bulle, le panneau (toggle), la saisie, l'appel service, l'affichage de la réponse et les états (loading, error).
- `HelpService` — service injectable `providedIn: 'root'`, méthode `chat(message: string): Observable<HelpChatResponse>`

### Modèles

```typescript
interface HelpChatResponse { answer: string; }
```

### Intégration dans AppComponent

- Ajouter `<app-help-chat-widget />` dans `app.component.html`
- Conditionné par `authService.currentUser() !== null`

### Tables impactées
Aucune.

### Migration Liquibase
- [x] Non applicable

---

## Plan de test

### Tests unitaires (`HelpChatWidgetComponent` spec)

- [ ] T-01 : bulle non visible quand `currentUser` est null
- [ ] T-02 : bulle visible quand `currentUser` est non null
- [ ] T-03 : clic sur bulle → `panelOpen` passe à true
- [ ] T-04 : clic sur fermer → `panelOpen` passe à false
- [ ] T-05 : bouton Envoyer désactivé si message vide
- [ ] T-06 : bouton Envoyer désactivé si message > 500 chars
- [ ] T-07 : clic question suggérée → appel `chat()` avec la question
- [ ] T-08 : pendant chargement → `isLoading` true, champ désactivé
- [ ] T-09 : succès → `answer` affiché dans le panneau
- [ ] T-10 : erreur service → message d'erreur inline affiché

---

## Analyse d'impact

### Préoccupations transversales touchées
- [x] **Aucune préoccupation transversale** — nouveau composant ajouté en fin de `AppComponent`, aucun composant existant modifié structurellement.

### Smoke tests E2E concernés
- [x] Aucun smoke test concerné — widget overlay, aucune route modifiée.

---

## Dépendances

### Subfeatures bloquantes
- SF-104-01 — statut : done (PR #211 mergée)

---

## Notes et décisions

- **Position fixed** : le widget est positionné en `position: fixed; bottom: 24px; right: 24px` pour ne jamais interférer avec le layout.
- **Panel compact** : le panneau est un `div` positionné au-dessus de la bulle (bottom: 80px, right: 24px, width: 360px, height: 480px max), pas un MatDialog.
- **Pas de MatDialog** : éviter l'overhead d'une modale pour un widget conversationnel simple.
- **Inline error** : l'erreur s'affiche dans le panneau, pas en snackbar, pour ne pas interrompre le flux.
- **Conditionnement auth** : `@if (authService.currentUser())` dans app.component.html — le widget n'est pas rendu du tout si non authentifié.
