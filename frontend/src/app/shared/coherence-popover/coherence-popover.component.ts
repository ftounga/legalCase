import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SourceExplanation, SourceType, ActionType } from '../../core/models/source-explanation.model';

/**
 * Popover d'incohérence enrichi (SF-IA-03-15a, refonte SF-IA-03-17).
 * Design navy/gold conforme DESIGN_SYSTEM.md, 3 zones distinctes :
 * - MOTIF DÉTECTÉ : reason + sentence (phrase Haiku pédagogique).
 * - SOURCE : type typé + label + extrait court (secondaryText).
 * - ACTION : bouton contextualisé vers la source précise ou générique.
 */
@Component({
  selector: 'app-coherence-popover',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="popover-card" [class.blocker]="blocker">
      <section class="zone zone-motif">
        <div class="zone-header">
          <mat-icon class="zone-icon">auto_awesome</mat-icon>
          <span class="zone-title">Motif détecté</span>
        </div>
        <p class="reason">{{ reason }}</p>
        @if (explanation?.sentence) {
          <p class="explanation">{{ explanation?.sentence }}</p>
        }
      </section>

      @if (explanation) {
        <section class="zone zone-source">
          <div class="zone-header">
            <mat-icon class="zone-icon source-icon">{{ sourceIcon() }}</mat-icon>
            <span class="zone-title">Source</span>
          </div>
          <p class="source-kind">{{ sourceKindLabel() }}</p>
          <p class="source-label">{{ explanation?.label }}</p>
          @if (explanation?.secondaryText) {
            <p class="source-secondary">« {{ explanation?.secondaryText }} »</p>
          }
        </section>
      } @else {
        <section class="zone zone-source zone-source--muted">
          <div class="zone-header">
            <mat-icon class="zone-icon source-icon">insights</mat-icon>
            <span class="zone-title">Source</span>
          </div>
          <p class="source-secondary">Détectée à partir de l'analyse du dossier. Ré-analysez le dossier pour enrichir ce popover.</p>
        </section>
      }

      @if (hasAction()) {
        <button class="source-link" type="button" (click)="onSourceClicked()">
          {{ actionLabel() }}
          <mat-icon class="arrow">arrow_forward</mat-icon>
        </button>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
    .popover-card {
      width: 360px;
      background: #FFFFFF;
      border-radius: 12px;
      box-shadow: 0 10px 30px rgba(0, 30, 60, 0.12);
      border-left: 3px solid #F59E0B;
      padding: 16px;
      font-family: 'Inter', sans-serif;
      color: #0B2340;
    }
    .popover-card.blocker {
      border-left-color: #DC2626;
    }
    .zone {
      padding-bottom: 12px;
      margin-bottom: 12px;
      border-bottom: 1px solid #E5E7EB;
    }
    .zone:last-of-type {
      border-bottom: none;
      margin-bottom: 0;
      padding-bottom: 0;
    }
    .zone-header {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 6px;
    }
    .zone-icon {
      font-size: 16px;
      width: 16px;
      height: 16px;
      color: #C9A646;
    }
    .zone-title {
      font-size: 11px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #6B7A8F;
      font-weight: 600;
    }
    .reason {
      font-size: 14px;
      font-weight: 600;
      margin: 0 0 4px 0;
      line-height: 1.4;
      color: #0B2340;
    }
    .explanation {
      font-size: 13px;
      font-style: italic;
      color: #4B5563;
      margin: 4px 0 0 0;
      line-height: 1.5;
    }
    .source-kind {
      font-size: 12px;
      color: #6B7A8F;
      margin: 0 0 2px 0;
    }
    .source-label {
      font-size: 14px;
      font-weight: 600;
      color: #0B2340;
      margin: 0;
      line-height: 1.4;
      word-break: break-word;
    }
    .source-secondary {
      font-size: 12px;
      color: #4B5563;
      font-style: italic;
      margin: 6px 0 0 0;
      line-height: 1.4;
    }
    .zone-source--muted .source-secondary {
      color: #9CA3AF;
    }
    .source-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: none;
      border: none;
      padding: 12px 0 0 0;
      margin-top: 4px;
      border-top: 1px solid #E5E7EB;
      width: 100%;
      justify-content: flex-end;
      color: #C9A646;
      font-family: 'Inter', sans-serif;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      transition: color 0.15s ease;
    }
    .source-link:hover {
      color: #B08E38;
    }
    .source-link .arrow {
      font-size: 16px;
      width: 16px;
      height: 16px;
    }
  `],
})
export class CoherencePopoverComponent {
  @Input() explanation: SourceExplanation | null = null;
  @Input() reason = '';
  @Input() blocker = false;

  sourceAction = () => {};
  @Input() set onAction(fn: () => void) {
    this.sourceAction = fn;
  }

  sourceIcon(): string {
    const t: SourceType | undefined = this.explanation?.sourceType;
    switch (t) {
      case 'DOCUMENT': return 'description';
      case 'QUESTION_AI': return 'help_outline';
      case 'CHECKLIST_F96': return 'checklist';
      case 'CHAT': return 'chat_bubble_outline';
      case 'MISSING_PIECE': return 'report_problem';
      case 'MULTI': return 'layers';
      default: return 'insights';
    }
  }

  sourceKindLabel(): string {
    const t: SourceType | undefined = this.explanation?.sourceType;
    switch (t) {
      case 'DOCUMENT': return 'Document du dossier';
      case 'QUESTION_AI': return 'Question complémentaire';
      case 'CHECKLIST_F96': return 'Checklist procédurale';
      case 'CHAT': return 'Message du chat';
      case 'MISSING_PIECE': return 'Pièce manquante';
      case 'MULTI': return 'Sources multiples';
      default: return 'Analyse du dossier';
    }
  }

  hasAction(): boolean {
    return !!this.explanation && this.explanation.actionType !== 'NONE';
  }

  actionLabel(): string {
    const a: ActionType | undefined = this.explanation?.actionType;
    const label = this.explanation?.label ?? '';
    switch (a) {
      case 'OPEN_DOCUMENT': return `Ouvrir ${label}`;
      case 'OPEN_DOCUMENTS_LIST': return 'Voir les documents';
      case 'SCROLL_QA': return 'Voir la question';
      case 'OPEN_QUESTIONS': return 'Voir les questions';
      case 'SCROLL_F96': return 'Voir le point procédural';
      case 'OPEN_F96_LIST': return 'Voir la checklist procédurale';
      case 'OPEN_CHAT': return 'Ouvrir le chat';
      case 'OPEN_MISSING_PIECES': return 'Voir les pièces manquantes';
      default: return 'Voir la source';
    }
  }

  onSourceClicked(): void {
    this.sourceAction();
  }
}
