import { Injectable, inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatDialog } from '@angular/material/dialog';
import {
  SynthesisShortBlockDialogComponent,
  SynthesisShortBlockDialogData,
} from '../synthesis-short-block-dialog/synthesis-short-block-dialog.component';

/**
 * F-229 SF-229-01 — clés de navigation supportées par le service.
 *
 * <p>Huit clés correspondant aux blocs canoniques de la grille de badges
 * F-162 qui possèdent une cible déterministe (page dédiée, popup ou scroll
 * vers une ancre dans la page synthèse) :
 * `pistes`, `pieces`, `risques`, `questions`, `timeline`, `faits`,
 * `points-juridiques`, `checklist`.</p>
 *
 * <p>F-229 SF-229-03 (2026-05-09) — la clé `checklist` est désormais exposée
 * pour la tile résumé `F-193-procedure-checks-summary` (procedure checks F-96
 * matérialisés). Le commentaire d'origine indiquait qu'il n'y avait pas de
 * tile équivalente — F-193 SF-193-01 a précisément ajouté cette tile, donc
 * il faut une cible canonique partagée avec le badge F-162 `checklist`.</p>
 *
 * <p>La clé `type-litige` (F-197, dialog override sur synthesis uniquement)
 * reste volontairement non exposée — elle est gérée localement par
 * {@link SynthesisComponent}.</p>
 */
export type BadgeKey =
  | 'pistes'
  | 'pieces'
  | 'risques'
  | 'questions'
  | 'timeline'
  | 'faits'
  | 'points-juridiques'
  | 'checklist';

/**
 * F-229 SF-229-01 — données contextuelles fournies par l'appelant pour les
 * destinations qui ouvrent un popup (`pieces`, `questions-ouvertes`).
 *
 * <p>Le service ne possède pas de référence directe à la synthèse en cours :
 * c'est l'appelant (case-dashboard ou synthesis) qui connaît
 * `synthesis().piecesManquantes` / `synthesis().questionsOuvertes` au moment
 * du clic et les passe ici. Pour les destinations route (timeline, faits,
 * points-juridiques, risques), pistes et questions, `contextData` est ignoré.</p>
 */
export interface BadgeContextData {
  /** Items affichés dans le popup `SynthesisShortBlockDialogComponent`. */
  popupItems?: string[];
  /** Titre du popup (par ex. "Pièces manquantes"). */
  popupTitle?: string;
  /** Icône Material affichée à gauche du titre du popup. */
  popupIcon?: string;
}

/**
 * F-229 SF-229-01 — service partagé qui aligne les tiles "résumé" du
 * dashboard décisionnel sur les cibles canoniques de la grille de badges
 * F-162. Source de vérité unique pour la résolution {@link BadgeKey} →
 * destination (route, popup, fragment).
 *
 * <p>Consommateurs initiaux :
 * <ol>
 *   <li>{@link CaseDashboardComponent#openGenericTool} — pour les 5 tiles
 *   "résumé" (`F-192-retained-pistes-summary`, `F-193-procedure-checks-summary`,
 *   `F-194-pieces-summary`, `F-195-risques-summary`,
 *   `F-196-questions-summary`).</li>
 *   <li>{@link SynthesisComponent} — pour les badges de la grille F-162 qui
 *   correspondent aux clés exposées (les badges restent visuellement
 *   identiques, seul le handler de clic passe par le service).</li>
 * </ol></p>
 *
 * <p>Toute évolution future (notification email, breadcrumb, page d'accueil)
 * doit passer par ce service afin d'éviter une dette de convergence — cf.
 * historique F-194/F-195/F-196 où 3 mismatches sont apparus en 30 jours
 * faute d'un point unique de résolution.</p>
 */
@Injectable({ providedIn: 'root' })
export class BadgeNavigationService {
  private readonly router = inject(Router);
  private readonly dialog = inject(MatDialog);

  /**
   * F-229 SF-229-01 — résout `key` vers la destination canonique F-162.
   *
   * @param key clé de badge supportée ({@link BadgeKey})
   * @param caseFileId identifiant du dossier en cours (utilisé pour les
   *   `router.navigate` vers `/case-files/:caseFileId/synthesis/...`)
   * @param contextData données contextuelles pour les destinations popup
   *   (cf. {@link BadgeContextData}). Ignoré pour les destinations route.
   */
  go(key: BadgeKey, caseFileId: string, contextData?: BadgeContextData): void {
    switch (key) {
      case 'pistes':
        this.router.navigate(
          ['/case-files', caseFileId, 'synthesis'],
          { fragment: 'section-pistes' },
        );
        return;
      case 'pieces':
        this.openShortBlockDialog(contextData, 'Pièces manquantes', 'report_problem');
        return;
      case 'risques':
        this.router.navigate(['/case-files', caseFileId, 'synthesis', 'risques']);
        return;
      case 'questions':
        this.router.navigate(
          ['/case-files', caseFileId, 'synthesis'],
          { fragment: 'section-questions' },
        );
        return;
      case 'timeline':
        this.router.navigate(['/case-files', caseFileId, 'synthesis', 'timeline']);
        return;
      case 'faits':
        this.router.navigate(['/case-files', caseFileId, 'synthesis', 'faits']);
        return;
      case 'points-juridiques':
        this.router.navigate(['/case-files', caseFileId, 'synthesis', 'points-juridiques']);
        return;
      case 'checklist':
        // F-229 SF-229-03 — anchor cohérent avec badge F-162 `checklist`
        // (cf. `synthesis.component.ts:362` anchor `section-checklist`).
        this.router.navigate(
          ['/case-files', caseFileId, 'synthesis'],
          { fragment: 'section-checklist' },
        );
        return;
    }
  }

  /**
   * F-229 SF-229-01 — ouvre le popup `SynthesisShortBlockDialogComponent`
   * pour une destination popup (`pieces`, `questions-ouvertes`).
   *
   * <p>Le service applique des valeurs par défaut prudentes si l'appelant ne
   * fournit pas tous les champs : items vides, titre/icône fallback. Cela
   * évite tout crash runtime, le popup s'ouvre simplement sur un état vide
   * — comportement aligné sur la version actuelle de
   * {@link SynthesisComponent#openPopup} (qui n'ouvre rien si `synthesis`
   * est null mais ouvre un popup vide si la liste l'est).</p>
   */
  private openShortBlockDialog(
    contextData: BadgeContextData | undefined,
    fallbackTitle: string,
    fallbackIcon: string,
  ): void {
    const data: SynthesisShortBlockDialogData = {
      title: contextData?.popupTitle ?? fallbackTitle,
      icon: contextData?.popupIcon ?? fallbackIcon,
      items: contextData?.popupItems ?? [],
    };
    this.dialog.open(SynthesisShortBlockDialogComponent, {
      data,
      width: '520px',
      maxWidth: '92vw',
    });
  }
}
