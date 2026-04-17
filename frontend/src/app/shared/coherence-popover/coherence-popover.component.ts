import { Component, Input } from '@angular/core';
import { SourceExplanation, ActionType } from '../../core/models/source-explanation.model';

/**
 * Popover d'incohérence — version compacte type tooltip (SF-118-07 polish).
 * Plus de zones séparées. 2-3 lignes serrées : raison + source inline + lien.
 */
@Component({
  selector: 'app-coherence-popover',
  standalone: true,
  imports: [],
  template: `
    <div class="pop" [class.blocker]="blocker">
      <p class="reason">{{ reason }}</p>
      @for (exp of explanations; track $index) {
        <div class="src-row"
             [class.clickable]="exp.actionType !== 'NONE'"
             (click)="exp.actionType !== 'NONE' && onClickSource(exp)"
             [attr.role]="exp.actionType !== 'NONE' ? 'button' : null"
             [attr.tabindex]="exp.actionType !== 'NONE' ? 0 : null">
          <span class="src-icon">{{ iconFor(exp) }}</span>
          <span class="src-label">{{ exp.label }}</span>
          @if (exp.secondaryText) {
            <span class="src-detail"> — {{ exp.secondaryText }}</span>
          }
          @if (exp.actionType !== 'NONE') {
            <span class="src-go">→</span>
          }
        </div>
      }
      @if (explanations.length === 0) {
        <div class="src-row clickable" (click)="onClickSource(null)" role="button" tabindex="0">
          <span class="src-go">Voir la synthese →</span>
        </div>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .pop {
      max-width: 300px;
      min-width: 160px;
      width: max-content;
      background: #fff;
      border-radius: 6px;
      box-shadow: 0 4px 16px rgba(0,30,60,0.14);
      border-left: 3px solid #F59E0B;
      padding: 6px 8px;
      font-family: 'Inter', sans-serif;
      color: #0B2340;
      font-size: 12px;
      line-height: 1.35;
    }
    .pop.blocker { border-left-color: #DC2626; }
    .reason {
      font-weight: 600;
      margin: 0 0 3px 0;
    }
    .src-row {
      display: flex;
      align-items: baseline;
      gap: 3px;
      padding: 2px 0;
      font-size: 11px;
      color: #4B5563;
      flex-wrap: wrap;
    }
    .src-row.clickable {
      cursor: pointer;
      border-radius: 3px;
      padding: 2px 3px;
      margin: 0 -3px;
    }
    .src-row.clickable:hover { background: #F3F4F6; }
    .src-row.clickable:focus-visible { outline: 1.5px solid #C9A646; outline-offset: 1px; }
    .src-icon { flex-shrink: 0; }
    .src-label { font-weight: 600; color: #0B2340; }
    .src-detail { color: #6B7A8F; font-style: italic; }
    .src-go { color: #C9A646; font-weight: 600; margin-left: auto; flex-shrink: 0; }
  `],
})
export class CoherencePopoverComponent {
  @Input() explanations: SourceExplanation[] = [];
  @Input() reason = '';
  @Input() blocker = false;

  onActionFor: (exp: SourceExplanation | null) => void = () => {};

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

  onClickSource(exp: SourceExplanation | null): void {
    this.onActionFor(exp);
  }
}
