import { Component, Input } from '@angular/core';
import { SourceExplanation, ActionType } from '../../core/models/source-explanation.model';

/**
 * Popover d'incohérence — 2 blocs titrés (MOTIF + SOURCE) compacts.
 */
@Component({
  selector: 'app-coherence-popover',
  standalone: true,
  imports: [],
  template: `
    <div class="pop" [class.blocker]="blocker">
      <div class="block">
        <div class="block-title">Motif détecté</div>
        <p class="reason">{{ reason }}</p>
        @if (firstSentence()) {
          <p class="sentence">{{ firstSentence() }}</p>
        }
      </div>

      @for (exp of explanations; track $index) {
        <div class="block src"
             [class.clickable]="exp.actionType !== 'NONE'"
             (click)="exp.actionType !== 'NONE' && onClickSource(exp)"
             [attr.role]="exp.actionType !== 'NONE' ? 'button' : null"
             [attr.tabindex]="exp.actionType !== 'NONE' ? 0 : null">
          <div class="block-title">{{ iconFor(exp) }} Source</div>
          <p class="src-label">{{ exp.label }}</p>
          @if (exp.secondaryText) {
            <p class="src-detail">« {{ exp.secondaryText }} »</p>
          }
          @if (exp.actionType !== 'NONE') {
            <p class="src-action">{{ actionLabel(exp) }} →</p>
          }
        </div>
      }

      @if (explanations.length === 0) {
        <div class="block src clickable" (click)="onClickSource(null)" role="button" tabindex="0">
          <p class="src-action">Voir la synthèse →</p>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .pop {
      max-width: 300px;
      min-width: 180px;
      width: max-content;
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 4px 16px rgba(0,30,60,0.14);
      border-left: 3px solid #F59E0B;
      padding: 8px 10px;
      font-family: 'Inter', sans-serif;
      color: #0B2340;
    }
    .pop.blocker { border-left-color: #DC2626; }

    .block {
      padding-bottom: 6px;
      margin-bottom: 6px;
      border-bottom: 1px solid #F0F1F3;
    }
    .block:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }

    .block-title {
      font-size: 9px;
      text-transform: uppercase;
      letter-spacing: 0.4px;
      color: #8896A6;
      font-weight: 700;
      margin-bottom: 2px;
    }

    .reason {
      font-size: 12px;
      font-weight: 600;
      margin: 0;
      line-height: 1.35;
    }
    .sentence {
      font-size: 11px;
      font-style: italic;
      color: #4B5563;
      margin: 2px 0 0 0;
      line-height: 1.35;
    }

    .src { transition: background 0.12s; }
    .src.clickable { cursor: pointer; border-radius: 4px; padding: 4px 6px; margin: 0 -6px 6px; }
    .src.clickable:hover { background: #F7F8FA; }
    .src.clickable:focus-visible { outline: 1.5px solid #C9A646; outline-offset: 1px; }

    .src-label {
      font-size: 12px;
      font-weight: 600;
      margin: 0;
      line-height: 1.35;
    }
    .src-detail {
      font-size: 11px;
      color: #6B7A8F;
      font-style: italic;
      margin: 1px 0 0 0;
      line-height: 1.3;
    }
    .src-action {
      font-size: 11px;
      font-weight: 600;
      color: #C9A646;
      margin: 3px 0 0 0;
      text-align: right;
    }
  `],
})
export class CoherencePopoverComponent {
  @Input() explanations: SourceExplanation[] = [];
  @Input() reason = '';
  @Input() blocker = false;

  onActionFor: (exp: SourceExplanation | null) => void = () => {};

  firstSentence(): string | null {
    return this.explanations.find(e => e.sentence)?.sentence ?? null;
  }

  iconFor(exp: SourceExplanation): string {
    switch (exp.sourceType) {
      case 'DOCUMENT': return '📄';
      case 'QUESTION_AI': return '❓';
      case 'CHECKLIST_F96': return '☑️';
      case 'CHAT': return '💬';
      case 'MISSING_PIECE': return '⚠️';
      default: return '✨';
    }
  }

  actionLabel(exp: SourceExplanation): string {
    switch (exp.actionType) {
      case 'OPEN_DOCUMENT': return `Ouvrir ${exp.label}`;
      case 'OPEN_DOCUMENTS_LIST': return 'Voir les documents';
      case 'SCROLL_QA': return 'Voir la question';
      case 'OPEN_QUESTIONS': return 'Voir les questions';
      case 'SCROLL_F96': return 'Voir le point procédural';
      case 'OPEN_F96_LIST': return 'Voir la checklist';
      case 'OPEN_CHAT': return 'Ouvrir le chat';
      case 'OPEN_MISSING_PIECES': return 'Voir les pièces manquantes';
      default: return 'Voir la synthèse';
    }
  }

  onClickSource(exp: SourceExplanation | null): void {
    this.onActionFor(exp);
  }
}
