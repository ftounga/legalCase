# Pièce 06 — Vidéo de la caméra de sonnette du voisin

## Spécifications techniques

- **Fichier final** : `06-camera-voisin.mp4`
- **Format** : MP4, codec H.264, résolution 720p (1280 × 720)
- **Durée** : **12 secondes**
- **Qualité** : basse définition typique d'une caméra de sonnette grand public (Ring, Nest, etc.) — granulosité visible, compression vidéo perceptible, framerate 15 fps
- **Audio** : **muet** (la caméra de sonnette du voisin n'enregistrait pas l'audio dans ce scénario)
- **Routage attendu dans la vidéo** : **Legal Vision** avec extraction de frames clés

## Rôle dans la vidéo

Troisième et dernière vignette du **bloc 6 (48–62s)**. C'est le cas le plus extrême — **une vidéo en pièce de dossier**. Aucun OCR ne traite la vidéo. Très peu d'outils d'IA juridique savent qualifier ce qu'il s'y passe. Legal Vision extrait les frames clés, décrit l'événement, le date.

## Storyboard — 12 secondes, plan unique fixe

La caméra est fixée sur la porte de l'appartement du voisin du dessus (M. ROCHE, mentionné dans la sous-pièce B de la pièce 02). Elle filme **le palier commun**, avec la porte de Sophie MARTIN visible à droite du cadre. Plan fixe, sans mouvement de caméra.

### Découpage seconde par seconde

| Temps | Action visible |
|---|---|
| **00:00** | Plan fixe du palier vide. Lumière tamisée d'éclairage de couloir. Horodatage incrusté en bas à droite : `03/05/2026 23:11:45` |
| **00:00–00:03** | Une silhouette masculine entre dans le cadre par la gauche, monte les dernières marches d'escalier. Allure pressée. Visage **flouté ou non identifiable** (à cause de la basse résolution, pas par retouche graphique). |
| **00:03–00:05** | L'individu s'approche de la porte de droite (porte de Sophie). Frappe avec le poing droit. Trois coups secs visibles. |
| **00:05–00:07** | Pause. L'individu fait un pas en arrière. |
| **00:07–00:09** | Coup d'épaule contre la porte. La porte vibre visiblement. Le mouvement de l'individu suggère un effort important. |
| **00:09–00:11** | Coup de pied bas de porte. |
| **00:11–00:12** | L'individu sort du cadre par la gauche, descendant les escaliers rapidement. |

### Indications visuelles

- **Horodatage incrusté** dans le coin bas-droit de la vidéo, format `JJ/MM/AAAA HH:MM:SS`, qui s'incrémente en temps réel
- **Logo discret de la caméra** dans le coin haut-gauche (créer un logo fictif type "EZView Cam" — pas de marque réelle Ring/Nest)
- **Granulation et compression** : appliquer un filtre qui simule une caméra de sonnette grand public — pas de la 4K, pas du cinéma. Bruit numérique visible.
- **Pas d'audio** — vidéo entièrement muette

### Ce qu'on ne veut PAS

- Pas de gros plan sur le visage de l'agresseur
- Pas de violence sur personne (la porte est l'objet visible des coups, pas Sophie qui n'apparaît jamais à l'écran)
- Pas d'audio (cohérent avec la fiche technique de la caméra de sonnette)

## Démonstration bloc 6 — Legal Vision sur la vidéo

Voix off (rappel global du bloc) : *"Là où d'autres outils s'arrêtent, Legal Vision lit la conversation, décrit la scène, identifie l'événement filmé."*

Sur la vignette vidéo (3ᵉ position dans les 3 vignettes côte à côte), animation :
- Lecture des 12s en accéléré (~3s à l'écran)
- Pause sur 3 frames clés extraites automatiquement (à 00:04 — coup de poing, 00:08 — coup d'épaule, 00:10 — coup de pied)
- Apparition de la **carte de synthèse Legal Vision** :

```
┌──────────────────────────────────────────────────┐
│ 🎥 Vidéo — Caméra palière voisin                 │
├──────────────────────────────────────────────────┤
│ Durée : 12 s — sans audio                        │
│ Horodatage incrusté : 03/05/2026 23:11–23:12     │
│                                                  │
│ Événement filmé (Legal Vision) :                 │
│ Individu masculin (visage non identifiable —     │
│ basse résolution caméra) se présente devant      │
│ la porte palière, frappe à coups de poing,       │
│ donne un coup d'épaule, puis un coup de pied     │
│ bas de porte, avant de quitter le cadre.         │
│                                                  │
│ Frames clés extraites : 3 (00:04, 00:08, 00:10)  │
│                                                  │
│ Source : 06-camera-voisin.mp4                    │
└──────────────────────────────────────────────────┘

Badge : Legal Vision (violet électrique)
```

Note importante : Legal Vision **ne nomme pas l'agresseur**. Il décrit "individu masculin non identifiable". L'identification reste à la charge de l'avocat et des forces de l'ordre. LegalCase ne tire pas de conclusion à la place du professionnel.

## Considérations éthiques pour le rendu

- **Aucune représentation explicite de violence physique sur personne**. Seuls les coups portés à la porte sont visibles. Sophie n'apparaît jamais à l'écran.
- **Aucune identification de l'agresseur**. Le visage doit être indistinct par effet caméra (basse résolution, contre-jour léger), pas par flou intentionnel.
- **Pas de musique dramatisante** dans la vidéo finale (vidéo source muette ; la vidéo marketing peut avoir sa propre bande-son mais doit éviter le pathos sur ce passage).

---

## Génération automatique de la pièce

### Méthode : prompt vidéo (Sora / Runway Gen-3 / Veo / Pika) + prompts frames de fallback (DALL-E 3)

C'est la pièce la plus difficile à générer automatiquement. Deux approches selon les outils accessibles.

### Approche 1 — Génération vidéo directe

Si tu as accès à un générateur vidéo (Sora, Runway Gen-3 Alpha, Google Veo, Pika 1.5+), utilise le prompt suivant :

#### Prompt vidéo à coller (Sora / Runway / Veo / Pika)

```
12-second video, fixed CCTV-style camera angle, low resolution 720p
look (visible compression artifacts, grain, 15 fps), no audio.

Scene: a Parisian apartment building hallway/landing, dim warm yellow
overhead lighting, light oak wood doors visible. The camera is
mounted on a doorbell-cam unit on the upper-left door, filming the
landing in front of an opposite door (right side of frame).

Timecode overlay in bottom-right corner: "03/05/2026 23:11:45"
incrementing in real-time.

Action sequence (timed):
- 0-3s: empty landing, dim yellow lighting
- 3s: a male silhouette enters from the left, climbing the last
  stairs, walking with hurried determined pace toward the right
  door. Face NOT clearly visible due to low camera resolution and
  slight backlight from hallway lamp.
- 3-5s: man approaches the right door, knocks three times with
  closed fist
- 5-7s: man steps back one step, brief pause
- 7-9s: man delivers a shoulder-strike against the door, door
  visibly vibrates
- 9-11s: man delivers a low kick to the bottom of the door
- 11-12s: man exits frame to the left, descending stairs quickly

Visual treatment: low-res surveillance camera quality, slight fish-eye,
grainy compression, warm color cast from incandescent hallway bulb,
no smooth motion (15 fps look). Absolutely no close-up of the man's
face. No blood, no violence on any person — only impacts against the
door visible. No audio.

Top-left corner: small fictional camera logo "EZView Cam" (do not
use any real brand).

Style reference: residential doorbell camera footage, security cam
aesthetic, 720p compressed.
```

### Approche 2 — Fallback : 3 frames clés en image generation, puis assemblage en motion design

Si tu n'as pas accès à un générateur vidéo, génère **3 images statiques** via DALL-E 3 et le motion designer assemble la vidéo (12s avec horodatage incrusté qui défile + transitions entre les 3 frames + intercalages d'images statiques du palier vide).

#### Prompt FRAME 1 — coup de poing (à 00:04)

```
Surveillance camera still frame, 720p compressed look with visible
grain and noise, fish-eye lens, fixed top-down angle from a doorbell
camera mounted on an apartment door. The camera films a Parisian
apartment landing with warm yellow incandescent lighting and worn
oak wood doors. A male figure stands in front of the right-side door,
right arm raised in mid-motion of knocking with closed fist on the
door. Face NOT identifiable — backlit, low resolution, slight motion
blur on the head area. Dark jacket. The figure is shown from a 3/4
back angle. Timecode overlay bottom-right: "03/05/2026 23:11:49".
Small "EZView Cam" logo top-left. No blood, no violence on person,
no audio implied. Surveillance cam aesthetic, not cinematic.
--ar 16:9 --v 6
```

#### Prompt FRAME 2 — coup d'épaule (à 00:08)

```
Same surveillance camera setup as previous (Parisian apartment landing,
fish-eye low-res 720p, warm yellow light, oak wood doors). The same
male figure is now mid-action of delivering a shoulder strike against
the right-side door, body angled into the door, right shoulder
making contact. Face NOT identifiable — blurred by motion and low
resolution, 3/4 back view. The door visibly vibrating (subtle motion
blur on the door edge). Timecode bottom-right: "03/05/2026 23:11:53".
Surveillance cam aesthetic, no close-up, no blood, no violence on
person. --ar 16:9 --v 6
```

#### Prompt FRAME 3 — coup de pied (à 00:10)

```
Same surveillance setup (Parisian landing, fish-eye low-res 720p,
warm yellow light). The male figure is mid-action of a low kick
to the bottom of the right-side door, right leg extended forward,
foot making contact at lower part of the door. Face NOT identifiable
— blurred by motion and low resolution, 3/4 back view, dark jacket.
Timecode bottom-right: "03/05/2026 23:11:55". Surveillance cam
aesthetic, no close-up, no blood, no violence on person.
--ar 16:9 --v 6
```

### Assemblage motion design (si fallback)

À partir des 3 frames + 1 frame "palier vide" supplémentaire :

1. Frame palier vide statique de 0 à 3s (avec horodatage qui défile de 23:11:45 à 23:11:48)
2. Transition 3-4s : interpolation entre frame vide et frame 1 (coup de poing)
3. Frame 1 figée + micro-mouvements 4-6s
4. Transition 6-7s : interpolation vers frame 2 (coup d'épaule)
5. Frame 2 + micro-mouvements 7-9s
6. Transition 9-10s : interpolation vers frame 3 (coup de pied)
7. Frame 3 + micro-mouvements 10-11s
8. Frame palier vide retour 11-12s
9. Toute la vidéo : appliquer un filtre "low-res surveillance" (grain, compression, 15 fps target, fish-eye léger), sans audio

### Recommandation

Si l'option vidéo native est disponible (Sora notamment) → privilégier l'approche 1, qui donne le meilleur réalisme.
Sinon, l'approche 2 est tout à fait acceptable pour une vidéo marketing — c'est même le standard de la production motion design.
