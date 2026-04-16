import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SourceExplanation, SourceType } from '../../core/models/source-explanation.model';

/**
 * Popover d'incohérence enrichi (SF-IA-03-15a).
 * Design navy/gold conforme DESIGN_SYSTEM.md. Affiché dans un CdkConnectedOverlay
 * déclaré dans le composant parent (ex. AncienneteSectionComponent).
 */
@Component({
  selector: 'app-coherence-popover',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="popover-card" [class.blocker]="blocker">
      <div class="header">
        <mat-icon class="source-icon" [attr.aria-hidden]="true">{{ sourceIcon() }}</mat-icon>
        <span class="source-type-label">{{ sourceTypeLabel() }}</span>
      </div>

      <p class="reason">{{ reason }}</p>

      @if (explanation?.sentence) {
        <p class="explanation">{{ explanation?.sentence }}</p>
      } @else {
        <p class="explanation explanation-muted">Détectée à partir de l'analyse du dossier.</p>
      }

      @if (hasAction()) {
        <button class="source-link" type="button" (click)="onSourceClicked()">
          Voir la source
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
      width: 340px;
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
    .header {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 12px;
      font-size: 12px;
      text-transform: uppercase;
      letter-spacing: 0.5px;
      color: #6B7A8F;
    }
    .source-icon {
      font-size: 18px;
      width: 18px;
      height: 18px;
      color: #C9A646;
    }
    .source-type-label {
      font-weight: 600;
    }
    .reason {
      font-size: 14px;
      font-weight: 600;
      margin: 0 0 8px 0;
      line-height: 1.4;
    }
    .explanation {
      font-size: 13px;
      font-style: italic;
      color: #4B5563;
      margin: 0 0 12px 0;
      line-height: 1.5;
    }
    .explanation-muted {
      color: #9CA3AF;
    }
    .source-link {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      background: none;
      border: none;
      padding: 8px 0 0 0;
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
      default: return 'auto_awesome';
    }
  }

  sourceTypeLabel(): string {
    const t: SourceType | undefined = this.explanation?.sourceType;
    switch (t) {
      case 'DOCUMENT': return this.explanation?.label ?? 'Document';
      case 'QUESTION_AI': return 'Question — ' + (this.explanation?.label ?? '');
      case 'CHECKLIST_F96': return 'Checklist conformité';
      case 'CHAT': return 'Message du chat';
      case 'MISSING_PIECE': return 'Pièce manquante';
      case 'MULTI': return 'Sources multiples';
      default: return 'Analyse du dossier';
    }
  }

  hasAction(): boolean {
    return !!this.explanation && this.explanation.actionType !== 'NONE';
  }

  onSourceClicked(): void {
    this.sourceAction();
  }
}
