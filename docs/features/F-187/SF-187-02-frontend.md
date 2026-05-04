# Mini-spec — F-187 / SF-187-02 — Frontend Export traction commerciale (page super-admin + formulaire)

## Identifiant

`F-187 / SF-187-02-frontend`

## Feature parente

`F-187` — Export traction commerciale (one-pager PDF auto) — super-admin

## Statut

`ready`

## Date de création

2026-05-04

## Branche Git

`feat/SF-187-02-frontend`

---

## Objectif

Exposer une page super-admin `/super-admin/traction-onepager` avec un formulaire qui permet de saisir verbatims, partenaires, accroche, contact, choisir les KPIs à inclure (lus depuis F-76 en lecture seule) et déclencher la génération + téléchargement du PDF via SF-187-01.

---

## Comportement attendu

### Cas nominal

1. Le super-admin navigue vers `/super-admin/traction-onepager` (lien depuis le menu super-admin existant).
2. La page charge automatiquement les KPIs courants via le service existant qui consomme `GET /api/v1/super-admin/metrics` (F-76).
3. Les KPIs sont affichés en lecture seule, chacun avec une checkbox "Inclure dans le PDF" cochée par défaut.
4. Le formulaire propose : 0-2 verbatims (avec bouton "Ajouter un verbatim" / "Supprimer"), 0-10 partenaires (texte libre, séparation par chips ou liste de champs), accroche (input simple), contact (pré-rempli depuis le user connecté : `name`, `email`, `url`).
5. À la soumission, le frontend POST sur `/api/v1/super-admin/traction-onepager/pdf` avec le body conforme au contrat (cf. SF-187-01).
6. La réponse blob est téléchargée en tant que fichier `legalcase-traction-YYYY-MM-DD.pdf` (le nom est lu depuis `Content-Disposition` ou recomposé côté frontend si plus simple).

### Cas d'erreur

| Situation | Comportement attendu |
|-----------|---------------------|
| Utilisateur non super-admin (route protégée) | Redirection vers `/` ou écran 403 selon pattern existant des autres routes super-admin |
| KPIs F-76 indisponibles (erreur réseau) | MatSnackBar erreur, formulaire reste utilisable (les KPIs côté serveur seront recalculés au moment du POST PDF) |
| Validation client : `headline` vide | Bouton "Générer PDF" désactivé tant que invalid |
| Validation client : `verbatims.length > 2` | Le bouton "Ajouter un verbatim" est masqué dès 2 verbatims présents |
| Validation client : `partners.length > 10` | Idem (bouton "Ajouter un partenaire" masqué dès 10) |
| POST PDF retourne 400 | MatSnackBar "Données invalides : <message>" |
| POST PDF retourne 403 | MatSnackBar "Accès refusé" + redirection (rare car gate déjà en route) |
| POST PDF retourne 500 | MatSnackBar "Erreur de génération PDF, réessayez" |

---

## Analyse de cohérence transversale

### Périmètres à scanner

- [x] **Autres outils métier** — non applicable (outil interne super-admin, pas un outil décisionnel métier)
- [x] **Autres pays** — non applicable
- [x] **Autres domaines** — non applicable
- [x] **Autres UI patterns** — formulaires reactive : pattern existant `SuperAdminBacklogComponent` (F-178), `SuperAdminMetricsComponent` (F-76). Réutiliser `MatFormField`, `MatInput`, `MatCheckbox`, `MatButton`, `MatChipList` (pour partenaires si pertinent), `MatSnackBar` pour erreurs. Téléchargement blob : pattern existant dans `CaseAnalysisPdfExportService` côté frontend (à vérifier au démarrage).
- [x] **Autres flows transversaux** — gate route super-admin : pattern existant via `SuperAdminGuard` ou équivalent, à utiliser tel quel.

### Niveaux de vérification

- [x] **Modèle TypeScript / API exposée** — interface `TractionOnePagerInput` miroir du DTO backend
- [x] **Service / logique métier** — `TractionOnePagerService` côté front (HttpClient.post + responseType: 'blob' + saveAs)
- [x] **Composant Angular** — `TractionOnePagerComponent` standalone
- [x] **Tests existants** — pattern Jest pour les composants super-admin (`SuperAdminBacklogComponent`)

### Cas spécifique : nouvelle feature d'outil décisionnel

Non applicable — outil interne, pas un outil décisionnel métier (pas de pré-fill IA, pas de F-IA-03, pas de TOOL_REGISTRY).

### Cas spécifique : nouveau pattern UI ou service partagé

- [x] **Où le nouveau pattern UI pourrait-il être réutilisé ?** — le pattern "formulaire super-admin → POST → téléchargement blob" est nouveau. Il pourra servir à de futurs exports (CSV super-admin, JSON, etc.). Mais sans demande concrète identifiée, pas d'extraction prématurée — laisser le code dans le composant.
- [x] **Y a-t-il des patterns concurrents ?** — il existe déjà des téléchargements blob (export PDF synthèse côté case-file). Vérifier au démarrage du dev qu'on utilise le même pattern (responseType blob + saveAs ou createObjectURL) pour cohérence.
- [x] **Le nouveau service / endpoint peut-il servir à d'autres features ?** — non.
- [x] **Le nouveau composant a-t-il un équivalent design ?** — non.

### Résultat du scan

| Cible | Applicable ? | Traitement |
|-------|-------------|------------|
| Pattern formulaire super-admin | Oui | Réutilise les composants Material existants |
| Pattern téléchargement blob | Oui | Réutiliser le pattern existant (à identifier via grep au début du dev) |
| Gate route super-admin | Oui | Réutilise `SuperAdminGuard` ou équivalent existant |

### Décision

- [x] Étendu à toutes les cibles applicables dans cette subfeature

---

## Impact par domaine métier

Cette SF est-elle sensible au domaine (droit du travail / immigration / famille) ?
**Non — transversal**. F-187 est un outil interne super-admin pour le pilotage commercial. Aucune adaptation par domaine ni par pays.

---

## Critères d'acceptation

- [ ] Route `/super-admin/traction-onepager` accessible uniquement au super-admin (gate vérifiée)
- [ ] Au chargement, les 7 KPIs F-76 sont affichés en lecture seule avec checkbox "Inclure" cochée par défaut
- [ ] Le formulaire reactive valide :
  - `headline` requis, ≤ 200 chars (bouton générer désactivé sinon)
  - `verbatims` : 0 à 2 max (bouton "Ajouter" masqué à 2)
  - `verbatims[i].quote` ≤ 500 chars + non vide
  - `verbatims[i].author` ≤ 200 chars + non vide
  - `partners` : 0 à 10 max
  - `contact.name`, `contact.email`, `contact.url` requis (pré-remplis depuis user connecté)
- [ ] Bouton "Générer PDF" déclenche `POST /api/v1/super-admin/traction-onepager/pdf` avec le body conforme au contrat
- [ ] Le blob retourné est téléchargé sous le nom `legalcase-traction-YYYY-MM-DD.pdf`
- [ ] Décocher un KPI puis générer → le KPI est exclu du body envoyé
- [ ] Erreur 400 / 500 → MatSnackBar avec message
- [ ] Charte respectée : `MatFormField`, `MatInput`, navy / or selon DESIGN_SYSTEM.md, JetBrains Mono pour les chiffres lus en lecture seule

---

## Périmètre

### Hors scope (explicite)

- Preview PDF live dans le navigateur
- Sauvegarde des inputs du formulaire (pas de localStorage, pas de backend persistance — chaque session redémarre vide hormis le contact pré-rempli)
- Historique des PDFs générés
- Édition WYSIWYG
- Personnalisation des couleurs / template
- Export en autres formats (PowerPoint, Word)

---

## Contrat API consommé

> **Contrat importé de SF-187-01-backend.** Toute modification doit être synchrone avec SF-187-01.

### Endpoint consommé

`POST /api/v1/super-admin/traction-onepager/pdf`

### Body envoyé

```typescript
interface TractionOnePagerInput {
  includeKpis: {
    totalWorkspaces: boolean;
    trialWorkspaces: boolean;
    paidWorkspaces: boolean;
    conversionRatePct: boolean;
    activeWorkspaces30d: boolean;
    analysesLast7Days: boolean;
    analysesLast30Days: boolean;
  };
  verbatims: Array<{ quote: string; author: string }>;  // 0-2
  partners: string[];  // 0-10
  headline: string;  // ≤ 200, requis
  contact: { name: string; email: string; url: string };
}
```

### Réponse attendue

- Succès : `200 OK`, `Content-Type: application/pdf`, body = blob PDF, header `Content-Disposition: attachment; filename="..."`.
- Erreur : voir SF-187-01 (codes 400 / 401 / 403 / 500).

### Lecture des KPIs courants (read-only)

`GET /api/v1/super-admin/metrics` (existant F-76). Réutiliser le service Angular existant qui consomme cet endpoint (probablement `SuperAdminMetricsService` — à confirmer via grep `super-admin/metrics` au démarrage).

---

## Valeurs initiales

| Champ formulaire | Valeur initiale | Règle |
|-------|----------------|-------|
| `includeKpis.*` | `true` | toutes les checkboxes cochées par défaut |
| `verbatims` | `[]` | aucun verbatim au départ |
| `partners` | `[]` | aucun partenaire au départ |
| `headline` | `''` | vide, requis pour activer le bouton |
| `contact.name` | nom du user connecté | lecture depuis `AuthService.currentUser().fullName` ou équivalent |
| `contact.email` | email du user connecté | lecture depuis `AuthService.currentUser().email` |
| `contact.url` | `'https://legalcase.ng-itconsulting.com'` | constante front (URL canonique du site) |

---

## Contraintes de validation

Validation Reactive Forms côté frontend, miroir des validations backend. Le backend reste l'autorité finale (cf. SF-187-01) — le frontend valide pour l'UX, pas pour la sécurité.

---

## Technique

### Route

| Path | Composant | Guard | Lazy |
|------|-----------|-------|------|
| `/super-admin/traction-onepager` | `TractionOnePagerComponent` | `SuperAdminGuard` (ou équivalent existant) | Oui |

### Composant Angular

- `TractionOnePagerComponent` (standalone) :
  - Injection : `SuperAdminMetricsService` (existant), `TractionOnePagerService` (nouveau), `AuthService` (pour pré-remplir contact), `MatSnackBar`, `FormBuilder`.
  - Template : section KPIs lecture seule + formulaire reactive + bouton "Générer PDF".
  - Styles : SCSS standalone, charte navy `#1A3A5C` / or `#C9973A` / Inter / JetBrains Mono pour les chiffres.

### Service Angular

- `TractionOnePagerService` (nouveau) :
  - Méthode `generatePdf(input: TractionOnePagerInput): Observable<Blob>` qui fait `HttpClient.post(url, input, { responseType: 'blob' })`.
  - Helper `triggerDownload(blob: Blob, filename: string)` qui crée un `<a download>` et clique programmatiquement (pattern standard).

### Fichiers à modifier

- Ajouter la route dans le routing super-admin (probablement `super-admin-routing.module.ts` ou équivalent — à confirmer via grep au démarrage).
- Ajouter le lien menu dans le composant menu super-admin (si menu visible).

---

## Plan de test

### Tests unitaires (Jest)

- [ ] `TractionOnePagerComponent` — rendu : KPIs affichés au chargement (mock `SuperAdminMetricsService`)
- [ ] `TractionOnePagerComponent` — formulaire invalide si `headline` vide → bouton désactivé
- [ ] `TractionOnePagerComponent` — décocher un KPI → champ correspondant `false` dans le body envoyé
- [ ] `TractionOnePagerComponent` — ajouter 2 verbatims → bouton "Ajouter" masqué
- [ ] `TractionOnePagerComponent` — soumission valide → appel `TractionOnePagerService.generatePdf` avec body conforme + déclenchement `triggerDownload`
- [ ] `TractionOnePagerComponent` — erreur 400 → MatSnackBar avec message
- [ ] `TractionOnePagerService.generatePdf` — POST appelé avec `responseType: 'blob'` + URL correcte
- [ ] `TractionOnePagerService.triggerDownload` — crée un blob URL, clique l'anchor, libère l'URL

### Tests d'intégration

- [x] Non applicable côté front (les tests Jest couvrent le composant + service unitairement avec mocks). L'intégration end-to-end est validée post-merge en staging via test manuel.

### Isolation workspace

- [x] Non applicable — pas de workspace context utilisateur, route super-admin only.

---

## Analyse d'impact

### Préoccupations transversales touchées

- [x] **Navigation / routing frontend** — ajout d'une nouvelle route `/super-admin/traction-onepager`. Vérifier que le routing module super-admin l'inclut bien.
- [x] **Aucune autre préoccupation** : pas d'auth modifiée, pas de workspace context modifié, pas de plan modifié.

### Composants / endpoints existants potentiellement impactés

| Composant / Endpoint | Impact potentiel | Test de non-régression prévu |
|----------------------|-----------------|------------------------------|
| Routing super-admin | Ajout d'une route — vérifier qu'aucune route existante n'est cassée | Smoke test navigation existant + Jest test routing |
| Menu super-admin (si présent) | Ajout d'un lien — vérifier qu'aucun lien existant n'est cassé | Visualisation manuelle staging |

### Smoke tests E2E concernés

- [ ] `e2e/smoke/navigation.spec.ts` — vérifier qu'aucune redirection / guard existante ne casse
- [x] `e2e/smoke/auth.spec.ts` — non concerné directement, mais à exécuter par défaut

---

## Dépendances

### Subfeatures bloquantes

- F-76 ✅ Terminée — `GET /api/v1/super-admin/metrics` doit retourner les KPIs.
- **SF-187-01 backend** — doit être mergée AVANT le merge de SF-187-02 (sinon 404 runtime sur staging). En revanche, le **dev** SF-187-02 peut partir en parallèle puisque le contrat est figé et les tests Jest utilisent un mock du service.

### Questions ouvertes impactées

- [x] Aucune.

### Subfeatures parallèles

- **SF-187-01 backend** — démarrée en parallèle (skill `parallel-frontback-delivery.md`). Tests frontend basés sur mock du `TractionOnePagerService`.

---

## Notes et décisions

- **Pas de localStorage** pour persister les inputs : risque de stocker des verbatims sensibles côté client. Le user re-saisit à chaque session (sauf le contact qui vient du user connecté).
- **Pas de preview PDF live** : ouverture du PDF dans le viewer du navigateur après téléchargement suffisant en V1.
- **Le contact est pré-rempli mais éditable** : permet au super-admin d'envoyer un PDF avec le contact d'un commercial (qui ne soit pas lui-même).
- **Le bouton "Générer PDF" est le seul submit** : pas de sauvegarde brouillon, pas d'auto-save.
- **Style** : reprendre les classes utilitaires existantes (`super-admin-page`, `super-admin-form` si elles existent — sinon créer des classes locales scopées au composant).
