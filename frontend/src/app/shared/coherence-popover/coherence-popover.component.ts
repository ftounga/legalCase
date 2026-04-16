import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SourceExplanation, SourceType, ActionType } from '../../core/models/source-explanation.model';

/**
 * Popover d'incohérence enrichi (SF-IA-03-15a, refonte 17, compact 18).
 * Design compact navy/gold, 3 zones MOTIF / SOURCE / ACTION.
 * Toute la carte est cliquable quand une action est disponible.
 */
@Component({
  selector: 'app-coherence-popover',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="popover-card"
         [class.blocker]="blocker"
         [class.clickable]="hasAction()"
         [attr.role]="hasAction() ? 'button' : null"
         [attr.tabindex]="hasAction() ? 0 : null"
         (click)="hasAction() && onSourceClicked()"
         (keydown.enter)="hasAction() && onSourceClicked()"
         (keydown.space)="hasAction() && onSourceClicked(); $event.preventDefault()">
      <section class="zone zone-motif">
        <p class="zone-title"><mat-icon class="zone-icon">auto_awesome</mat-icon> Motif détecté</p>
        <p class="reason">{{ reason }}</p>
        @if (explanation?.sentence) {
          <p class="explanation">{{ explanation?.sentence }}</p>
        }
      </section>

      @if (explanation) {
        <section class="zone zone-source">
          <p class="zone-title"><mat-icon class="zone-icon">{{ sourceIcon() }}</mat-icon> {{ sourceKindLabel() }}</p>
          <p class="source-label">{{ explanation?.label }}</p>
          @if (explanation?.secondaryText) {
            <p class="source-secondary">« {{ explanation?.secondaryText }} »</p>
          }
        </section>
      }

      @if (hasAction()) {
        <p class="source-link">
          {{ actionLabel() }}
          <mat-icon class="arrow">arrow_forward</mat-icon>
        </p>
      }
    </div>
  `,
  styles: [`
    :host {
      display: block;
    }
    .popover-card {
      max-width: 320px;
      min-width: 200px;
      width: max-content;
      background: #FFFFFF;
      border-radius: 10px;
      box-shadow: 0 8px 24px rgba(0, 30, 60, 0.12);
      border-left: 3px solid #F59E0B;
      padding: 10px 12px;
      font-family: 'Inter', sans-serif;
      color: #0B2340;
    }
    .popover-card.blocker {
      border-left-color: #DC2626;
    }
    .popover-card.clickable {
      cursor: pointer;
      transition: box-shadow 0.15s ease, transform 0.15s ease;
    }
    .popover-card.clickable:hover {
      box-shadow: 0 10px 28px rgba(0, 30, 60, 0.16);
    }
    .popover-card.clickable:focus-visible {
      outline: 2px solid #C9A646;
      outline-offset: 2px;
    }
    .zone {
      padding-bottom: 8px;
      margin-bottom: 8px;
      border-bottom: 1px solid #F3F4F6;
    }
    .zone:last-child,
    .zone + .source-link {
      border-bottom: none;
    }
    .zone + .source-link {
      margin-top: 4px;
      padding-top: 4px;
    }
    .zone-title {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 10px;
      text-transform: uppercase;
      letter-spacing: 0.4px;
      color: #6B7A8F;
      font-weight: 600;
      margin: 0 0 4px 0;
    }
    .zone-icon {
      font-size: 14px;
      width: 14px;
      height: 14px;
      color: #C9A646;
    }
    .reason {
      font-size: 13px;
      font-weight: 600;
      margin: 0;
      line-height: 1.35;
      color: #0B2340;
    }
    .explanation {
      font-size: 12px;
      font-style: italic;
      color: #4B5563;
      margin: 4px 0 0 0;
      line-height: 1.4;
    }
    .source-label {
      font-size: 13px;
      font-weight: 600;
      color: #0B2340;
      margin: 0;
      line-height: 1.35;
      word-break: break-word;
    }
    .source-secondary {
      font-size: 11px;
      color: #4B5563;
      font-style: italic;
      margin: 2px 0 0 0;
      line-height: 1.35;
    }
    .source-link {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 3px;
      color: #C9A646;
      font-size: 12px;
      font-weight: 600;
      margin: 0;
    }
    .source-link .arrow {
      font-size: 14px;
      width: 14px;
      height: 14px;
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

  /**
   * Action disponible si on a une explanation avec actionType ≠ NONE
   * OU aucune explanation (fallback vers synthèse).
   */
  hasAction(): boolean {
    if (!this.explanation) return true;
    return this.explanation.actionType !== 'NONE';
  }

  actionLabel(): string {
    if (!this.explanation) return 'Voir dans la synthèse';
    const a: ActionType = this.explanation.actionType;
    const label = this.explanation.label ?? '';
    switch (a) {
      case 'OPEN_DOCUMENT': return `Ouvrir ${label}`;
      case 'OPEN_DOCUMENTS_LIST': return 'Voir les documents';
      case 'SCROLL_QA': return 'Voir la question';
      case 'OPEN_QUESTIONS': return 'Voir les questions';
      case 'SCROLL_F96': return 'Voir le point procédural';
      case 'OPEN_F96_LIST': return 'Voir la checklist procédurale';
      case 'OPEN_CHAT': return 'Ouvrir le chat';
      case 'OPEN_MISSING_PIECES': return 'Voir les pièces manquantes';
      default: return 'Voir dans la synthèse';
    }
  }

  onSourceClicked(): void {
    this.sourceAction();
  }
}
