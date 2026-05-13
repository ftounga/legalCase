# Pièce 04 — Capture d'écran SMS du conjoint

## Spécifications techniques

- **Fichier final** : `04-sms-conjoint.png`
- **Format** : PNG (capture d'écran iPhone native)
- **Dimensions** : 1170 × 2532 pixels (écran iPhone 14/15)
- **Qualité** : nette (capture d'écran système)
- **Routage attendu dans la vidéo** : **Legal Vision**

## Rôle dans la vidéo

Cette pièce ouvre le **bloc 6 (48–62s) — Legal Vision : 3 supports impossibles**. C'est le premier des trois supports que Legal Vision traite et qu'aucun OCR classique ne sait reconstruire correctement (qui parle, à quelle heure, dans quel ordre, avec quel ton).

À l'écran : Legal Vision en quelques secondes produit une **synthèse structurée** de la conversation — nombre de messages, période, escalade détectée, menaces explicites datées et citées.

## Contenu de la conversation à reproduire

Capture d'écran de l'app Messages d'iPhone (iOS), conversation avec le contact "**Julien (mari)**".

Format à reproduire :
- En-tête : nom du contact "Julien (mari)", icône de profil avec initiale "J"
- Bulles **bleues à droite** = messages envoyés par Sophie (l'utilisatrice du téléphone)
- Bulles **grises à gauche** = messages reçus de Julien
- Indicateurs de date / horaire au-dessus des groupes de messages
- Quelques émojis et accusés de réception "Lu"

### Fil complet de la conversation (du plus ancien au plus récent)

```
─────── Lundi 27 avril ───────

[Julien — gris, 19:42]
Tu m'avais dit qu'on parlerait ce soir.
T'es où ?

[Sophie — bleu, 19:58]
Je suis chez ma sœur. Je rentre demain.
On parlera ce week-end, je t'ai dit.

[Julien — gris, 20:01]
Tu fais chier Sophie.

─────── Vendredi 1er mai ───────

[Julien — gris, 22:14]
Sérieux tu réponds plus ?
Tu joues à quoi là.

[Julien — gris, 22:17]
Réponds. Maintenant.

[Sophie — bleu, 22:35]
Julien stp arrête. Je t'ai demandé du temps.

[Julien — gris, 22:36]
Du temps pour quoi.
Du temps pour qui ?

[Julien — gris, 22:39]
T'as intérêt à me répondre.

─────── Dimanche 3 mai ───────

[Julien — gris, 18:22]
Je passe ce soir. Faut qu'on se voie.

[Sophie — bleu, 18:45]
Non. S'il te plaît ne viens pas.
J'ai pas envie ce soir.

[Julien — gris, 18:46]
Je passe. C'est chez moi aussi je te rappelle.

[Sophie — bleu, 18:50]
Si tu viens je t'ouvre pas.

[Julien — gris, 18:51]
Tu vas voir si tu m'ouvres pas.

[Julien — gris, 22:58]
Je suis en bas.

[Julien — gris, 23:04]
Ouvre cette putain de porte.

[Julien — gris, 23:08]
Je te jure que tu vas le regretter.

[Julien — gris, 23:11]
Si je rentre je te casse la gueule.

─────── Lundi 4 mai ───────

[Julien — gris, 08:14]
Sophie réponds-moi.
J'étais bourré hier. Faut qu'on parle.

[Julien — gris, 09:30]
Tu fais quoi là.

[Julien — gris, 11:02]
T'es au commissariat c'est ça ?
Tu vas vraiment me faire ça ?
```

## Démonstration bloc 6 — Legal Vision sur la conversation

Voix off (rappel) : *"Là où d'autres outils s'arrêtent, Legal Vision lit la conversation, décrit la scène, identifie l'événement filmé."*

À l'écran, sur la vignette SMS (3 vignettes côte à côte avec photo porte + vidéo), apparaît une **carte de synthèse Legal Vision** générée :

```
┌──────────────────────────────────────────────────┐
│ 💬 Conversation SMS — Julien VASSEUR            │
├──────────────────────────────────────────────────┤
│ Période : 27 avril → 4 mai 2026 (8 jours)       │
│ Messages : 23 messages (15 lui / 8 elle)         │
│                                                  │
│ ⚠ Escalade détectée                              │
│ Ton du déclarant qualifié : pression, reproches, │
│ injonctions, menaces explicites                  │
│                                                  │
│ Menaces explicites identifiées (3) :             │
│ • 03/05 18:51 — "Tu vas voir si tu m'ouvres pas"│
│ • 03/05 23:08 — "Tu vas le regretter"           │
│ • 03/05 23:11 — "Si je rentre je te casse la    │
│   gueule"                                        │
│                                                  │
│ Aveu post-faits : 04/05 08:14 —                  │
│ "J'étais bourré hier. Faut qu'on parle."         │
│                                                  │
│ Source : 04-sms-conjoint.png                     │
└──────────────────────────────────────────────────┘

Badge : Legal Vision (violet électrique)
```

## Conseils de rendu

- **Authenticité avant esthétique** : la conversation doit avoir l'air d'une vraie capture iPhone, pas d'une maquette UI parfaite. Garder les imperfections naturelles : barre de statut iOS en haut (heure 11:23, batterie 67%, signal 5G), bouton "i" en haut à droite, zone de saisie en bas avec icône micro.
- **Émojis** : volontairement absents dans cette conversation (registre de menace, pas de détente).
- **Police** : SF Pro (police système iOS).
- **Lecture confort** : le motion designer prendra une partie haute du fil pour l'animation initiale (vignette small) puis zoom sur les messages du 3 mai 22:58–23:11 quand la carte de synthèse pointe les menaces.

---

## Génération automatique de la pièce

### Méthode : script HTML/CSS qui imite iMessage iOS, capture en PNG

Cette pièce se génère par **HTML/CSS** (pas par image generation — DALL-E 3 produit des captures iMessage approximatives avec des artefacts texte). On rend dans Chrome en mode mobile (390 × 844 = iPhone 14 Pro), puis on screenshot.

### Code HTML à enregistrer comme `04-sms-conjoint.html`

```html
<!DOCTYPE html>
<html lang="fr">
<head>
<meta charset="UTF-8">
<title>SMS Julien</title>
<style>
  * { box-sizing: border-box; margin: 0; padding: 0; }
  body {
    background: #fff;
    font-family: -apple-system, "SF Pro Text", "Helvetica Neue", sans-serif;
    color: #000;
    width: 390px;
    margin: 0 auto;
    padding-top: 0;
  }
  /* Status bar iOS */
  .status-bar {
    height: 47px;
    background: #fff;
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 30px;
    font-weight: 600;
    font-size: 17px;
  }
  .status-bar .icons { display: flex; gap: 6px; align-items: center; font-size: 15px; }
  /* Header conversation */
  .convo-header {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding: 8px 0 12px;
    border-bottom: 0.5px solid #d1d1d6;
    background: rgba(245,245,247,0.85);
    backdrop-filter: blur(10px);
    position: relative;
  }
  .back { position: absolute; left: 14px; top: 14px; color: #007aff; font-size: 17px; }
  .info-icon { position: absolute; right: 18px; top: 14px; color: #007aff; font-size: 15px; }
  .avatar {
    width: 50px; height: 50px; border-radius: 50%;
    background: linear-gradient(135deg, #6c7480, #4a5260);
    color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 22px; font-weight: 500;
    margin-bottom: 4px;
  }
  .contact-name { font-size: 13px; font-weight: 500; }
  .contact-name .chevron { color: #8e8e93; font-size: 11px; margin-left: 3px; }
  /* Messages */
  .messages { padding: 12px 14px 80px; background: #fff; }
  .day-divider { text-align: center; color: #8e8e93; font-size: 11px; font-weight: 600; margin: 18px 0 8px; }
  .day-divider time { font-weight: 400; margin-left: 4px; }
  .msg-row { display: flex; margin-bottom: 3px; }
  .msg-row.them { justify-content: flex-start; }
  .msg-row.me { justify-content: flex-end; }
  .bubble {
    max-width: 75%;
    padding: 8px 13px;
    border-radius: 18px;
    font-size: 16px;
    line-height: 1.25;
    word-wrap: break-word;
  }
  .them .bubble { background: #e9e9eb; color: #000; border-bottom-left-radius: 4px; }
  .me .bubble { background: #007aff; color: #fff; border-bottom-right-radius: 4px; }
  .timestamp-line { text-align: center; color: #8e8e93; font-size: 11px; margin: 6px 0 2px; }
  .read-receipt { text-align: right; color: #8e8e93; font-size: 11px; margin-top: 2px; padding-right: 4px; }
  /* Zone de saisie en bas */
  .input-bar {
    position: fixed; bottom: 0; left: 0; right: 0;
    width: 390px; margin: 0 auto;
    height: 60px;
    background: rgba(245,245,247,0.95);
    backdrop-filter: blur(10px);
    display: flex; align-items: center;
    padding: 0 12px;
    border-top: 0.5px solid #d1d1d6;
  }
  .plus-btn { width: 32px; height: 32px; border-radius: 50%; background: #e9e9eb; color: #8e8e93; display: flex; align-items: center; justify-content: center; font-size: 22px; }
  .input-field { flex: 1; margin: 0 8px; background: #fff; border: 0.5px solid #d1d1d6; border-radius: 18px; height: 36px; display: flex; align-items: center; padding: 0 14px; color: #c7c7cc; font-size: 15px; }
  .mic-btn { color: #8e8e93; font-size: 20px; padding-right: 4px; }
</style>
</head>
<body>
<div class="status-bar">
  <span>11:23</span>
  <span class="icons">5G ●●● 67%</span>
</div>
<div class="convo-header">
  <span class="back">‹</span>
  <div class="avatar">J</div>
  <div class="contact-name">Julien (mari) <span class="chevron">›</span></div>
  <span class="info-icon">ⓘ</span>
</div>

<div class="messages">

  <div class="day-divider">lun. 27 avr.<time>19:42</time></div>
  <div class="msg-row them"><div class="bubble">Tu m'avais dit qu'on parlerait ce soir.<br>T'es où ?</div></div>
  <div class="timestamp-line">19:58</div>
  <div class="msg-row me"><div class="bubble">Je suis chez ma sœur. Je rentre demain. On parlera ce week-end, je t'ai dit.</div></div>
  <div class="msg-row them"><div class="bubble">Tu fais chier Sophie.</div></div>

  <div class="day-divider">ven. 1 mai<time>22:14</time></div>
  <div class="msg-row them"><div class="bubble">Sérieux tu réponds plus ?<br>Tu joues à quoi là.</div></div>
  <div class="msg-row them"><div class="bubble">Réponds. Maintenant.</div></div>
  <div class="msg-row me"><div class="bubble">Julien stp arrête. Je t'ai demandé du temps.</div></div>
  <div class="msg-row them"><div class="bubble">Du temps pour quoi.<br>Du temps pour qui ?</div></div>
  <div class="msg-row them"><div class="bubble">T'as intérêt à me répondre.</div></div>

  <div class="day-divider">dim. 3 mai<time>18:22</time></div>
  <div class="msg-row them"><div class="bubble">Je passe ce soir. Faut qu'on se voie.</div></div>
  <div class="msg-row me"><div class="bubble">Non. S'il te plaît ne viens pas. J'ai pas envie ce soir.</div></div>
  <div class="msg-row them"><div class="bubble">Je passe. C'est chez moi aussi je te rappelle.</div></div>
  <div class="msg-row me"><div class="bubble">Si tu viens je t'ouvre pas.</div></div>
  <div class="msg-row them"><div class="bubble">Tu vas voir si tu m'ouvres pas.</div></div>
  <div class="timestamp-line">22:58</div>
  <div class="msg-row them"><div class="bubble">Je suis en bas.</div></div>
  <div class="msg-row them"><div class="bubble">Ouvre cette putain de porte.</div></div>
  <div class="msg-row them"><div class="bubble">Je te jure que tu vas le regretter.</div></div>
  <div class="msg-row them"><div class="bubble">Si je rentre je te casse la gueule.</div></div>

  <div class="day-divider">lun. 4 mai<time>08:14</time></div>
  <div class="msg-row them"><div class="bubble">Sophie réponds-moi.<br>J'étais bourré hier. Faut qu'on parle.</div></div>
  <div class="msg-row them"><div class="bubble">Tu fais quoi là.</div></div>
  <div class="timestamp-line">11:02</div>
  <div class="msg-row them"><div class="bubble">T'es au commissariat c'est ça ?<br>Tu vas vraiment me faire ça ?</div></div>
</div>

<div class="input-bar">
  <div class="plus-btn">+</div>
  <div class="input-field">iMessage</div>
  <div class="mic-btn">●</div>
</div>
</body>
</html>
```

### Méthode 1 — Capture via Chrome DevTools (manuelle, rapide)

1. Ouvrir `04-sms-conjoint.html` dans Chrome
2. Ouvrir les DevTools (F12), activer le mode "Toggle device toolbar" (Ctrl+Shift+M)
3. Sélectionner "iPhone 14 Pro" dans la liste des appareils (ou définir manuellement 390 × 844)
4. Faire défiler la page pour s'assurer que tout le fil de conversation est lisible
5. Dans le menu kebab des DevTools (les 3 points verticaux) → **Capture full size screenshot**
6. Renommer le PNG produit en `04-sms-conjoint.png`

### Méthode 2 — Capture automatique via Puppeteer (si scriptée)

```bash
npm install puppeteer
node -e "
const puppeteer = require('puppeteer');
(async () => {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  await page.setViewport({ width: 390, height: 844, deviceScaleFactor: 3 });
  await page.goto('file://' + __dirname + '/04-sms-conjoint.html');
  await page.screenshot({ path: '04-sms-conjoint.png', fullPage: true });
  await browser.close();
})();
"
```

### Vérifications post-génération

- Le rendu doit être à **3× la résolution** (deviceScaleFactor: 3) pour avoir une qualité Retina iPhone — sinon le motion designer va voir du flou en zoom.
- La barre de statut iOS (11:23, 5G, 67 %) doit être visible en haut.
- La zone de saisie iMessage doit être visible en bas.
- Le fil doit être complet, du 27 avril au 4 mai 11:02.
