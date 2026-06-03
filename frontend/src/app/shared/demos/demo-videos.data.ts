import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

/**
 * Source partagée des démos vidéo LegalCase (SF-254-01).
 * Consommée par le carousel landing (LandingComponent) et la galerie /demos (DemosPageComponent).
 * Éditer ici pour ajouter / réordonner / remplacer une vidéo — single source of truth.
 */
export interface DemoVideo {
  videoId: string;
  title: string;
  subtitle: string;
}

export const DEMO_VIDEOS: DemoVideo[] = [
  {
    videoId: 'NGTRMWQKPEA',
    title: 'Votre dossier analysé en 3 min',
    subtitle: "De l'upload des pièces à la synthèse structurée",
  },
  {
    videoId: '8C-h43fyanY',
    title: 'Legal OCR + Legal Vision : tous vos formats',
    subtitle: 'Scans dégradés, fax administratifs, photos de SMS et manuscrits — lus et intégrés à l’analyse',
  },
  {
    videoId: 'I5EemkFR8NE',
    title: 'Synthèse enrichie et diff sourcé',
    subtitle: "Comparez les versions d'analyse, chaque point relié à sa pièce",
  },
  {
    videoId: 'HVGXeUnrbks',
    title: "Validité prud'homale — pièces, délais, score",
    subtitle: 'Pièces manquantes identifiées, délais légaux vérifiés, score calculé',
  },
  {
    videoId: 'rKJXppVe2SA',
    title: 'Démo droit du travail',
    subtitle: 'Un dossier analysé de bout en bout : synthèse, procédure, indemnités',
  },
  {
    videoId: 'Qh3hAO75xMk',
    title: "Démo droit de l'immigration",
    subtitle: 'Un dossier analysé de bout en bout : titre de séjour, recours, pièces',
  },
  {
    videoId: '78hEuoD_L_4',
    title: 'Démo droit de la famille',
    subtitle: 'Un dossier analysé de bout en bout : divorce, étapes, pièces',
  },
];

/**
 * hqdefault.jpg (480x360) existe pour 100% des vidéos YouTube.
 * maxresdefault peut renvoyer 404 selon la résolution d'upload — évité.
 */
export function getDemoThumbnailUrl(videoId: string): string {
  return `https://img.youtube.com/vi/${videoId}/hqdefault.jpg`;
}

export function getDemoEmbedUrl(videoId: string, sanitizer: DomSanitizer): SafeResourceUrl {
  return sanitizer.bypassSecurityTrustResourceUrl(
    `https://www.youtube-nocookie.com/embed/${videoId}?rel=0`,
  );
}
