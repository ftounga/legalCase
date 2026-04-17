import { Component, Input } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { SourceExplanation, SourceType, ActionType } from '../../core/models/source-explanation.model';

@Component({
  selector: 'app-coherence-popover',
  standalone: true,
  imports: [MatIconModule],
  template: `
    <div class="popover-card" [class.blocker]="blocker">
      <section class="zone zone-motif">
        <p class="zone-title"><mat-icon class="zone-icon">auto_awesome</mat-icon> Motif détecté</p>
        <p class="reason">{{ reason }}</p>
        @if (firstSentence()) {
          <p class="explanation">{{ firstSentence() }}</p>
        }
      </section>

      @if (explanations.length > 0) {
        @for (exp of explanations; track $index) {
          <section class="zone zone-source"
                   [class.clickable]="hasActionFor(exp)"
                   (click)="hasActionFor(exp) && onClickSource(exp)"
                   [attr.role]="hasActionFor(exp) ? 'button' : null"
                   [attr.tabindex]="hasActionFor(exp) ? 0 : null"
                   (keydown.enter)="hasActionFor(exp) && onClickSource(exp)"
                   (keydown.space)="hasActionFor(exp) && onClickSource(exp); $event.preventDefault()">
            <p class="zone-title"><mat-icon class="zone-icon">{{ sourceIcon(exp) }}</mat-icon> {{ sourceKindLabel(exp) }}</p>
            <p class="source-label">{{ exp.label }}</p>
            @if (exp.secondaryText) {
              <p class="source-secondary">« {{ exp.secondaryText }} »</p>
            }
            @if (hasActionFor(exp)) {
              <p class="source-link">{{ actionLabelFor(exp) }} <mat-icon class="arrow">arrow_forward</mat-icon></p>
            }
          </section>
        }
      } @else {
        <section class="zone zone-source zone-fallback clickable"
                 (click)="onClickSource(null)"
                 role="button" tabindex="0"
                 (keydown.enter)="onClickSource(null)"
                 (keydown.space)="onClickSource(null); $event.preventDefault()">
          <p class="source-link">Voir la synthèse <mat-icon class="arrow">arrow_forward</mat-icon></p>
        </section>
      }
    </div>
  `,
  styles: [`
    :host { display: block; }
    .popover-card {
      max-width: 320px;
      min-width: 180px;
      width: max-content;
      background: #FFFFFF;
      border-radius: 8px;
      box-shadow: 0 6px 20px rgba(0, 30, 60, 0.12);
      border-left: 3px solid #F59E0B;
      padding: 6px 8px;
      font-family: 'Inter', sans-serif;
      color: #0B2340;
    }
    .popover-card.blocker { border-left-color: #DC2626; }
    .zone {
      padding-bottom: 4px;
      margin-bottom: 4px;
      border-bottom: 1px solid #F3F4F6;
    }
    .zone:last-child { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
    .zone-source.clickable {
      cursor: pointer;
      border-radius: 6px;
      transition: background 0.15s ease;
    }
    .zone-source.clickable:hover { background: #FAFBFC; }
    .zone-source.clickable:focus-visible { outline: 2px solid #C9A646; outline-offset: 2px; }
    .zone-title {
      display: flex;
      align-items: center;
      gap: 3px;
      font-size: 9px;
      text-transform: uppercase;
      letter-spacing: 0.3px;
      color: #6B7A8F;
      font-weight: 600;
      margin: 0 0 2px 0;
    }
    .zone-icon { font-size: 12px; width: 12px; height: 12px; color: #C9A646; }
    .reason {
      font-size: 12px;
      font-weight: 600;
      margin: 0;
      line-height: 1.3;
      color: #0B2340;
    }
    .explanation {
      font-size: 11px;
      font-style: italic;
      color: #4B5563;
      margin: 2px 0 0 0;
      line-height: 1.3;
    }
    .source-label {
      font-size: 12px;
      font-weight: 600;
      color: #0B2340;
      margin: 0;
      line-height: 1.3;
      word-break: break-word;
    }
    .source-secondary {
      font-size: 10px;
      color: #4B5563;
      font-style: italic;
      margin: 1px 0 0 0;
      line-height: 1.3;
    }
    .source-link {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      gap: 2px;
      color: #C9A646;
      font-size: 11px;
      font-weight: 600;
      margin: 2px 0 0 0;
    }
    .source-link .arrow { font-size: 12px; width: 12px; height: 12px; }
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

  sourceIcon(exp: SourceExplanation): string {
    switch (exp.sourceType) {
      case 'DOCUMENT': return 'description';
      case 'QUESTION_AI': return 'help_outline';
      case 'CHECKLIST_F96': return 'checklist';
      case 'CHAT': return 'chat_bubble_outline';
      case 'MISSING_PIECE': return 'report_problem';
      case 'MULTI': return 'layers';
      default: return 'insights';
    }
  }

  sourceKindLabel(exp: SourceExplanation): string {
    switch (exp.sourceType) {
      case 'DOCUMENT': return 'Document du dossier';
      case 'QUESTION_AI': return 'Question complémentaire';
      case 'CHECKLIST_F96': return 'Checklist procédurale';
      case 'CHAT': return 'Message du chat';
      case 'MISSING_PIECE': return 'Pièce manquante';
      case 'MULTI': return 'Sources multiples';
      default: return 'Analyse du dossier';
    }
  }

  hasActionFor(exp: SourceExplanation): boolean {
    return exp.actionType !== 'NONE';
  }

  actionLabelFor(exp: SourceExplanation): string {
    switch (exp.actionType) {
      case 'OPEN_DOCUMENT': return `Ouvrir ${exp.label}`;
      case 'OPEN_DOCUMENTS_LIST': return 'Voir les documents';
      case 'SCROLL_QA': return 'Voir la question';
      case 'OPEN_QUESTIONS': return 'Voir les questions';
      case 'SCROLL_F96': return 'Voir le point procédural';
      case 'OPEN_F96_LIST': return 'Voir la checklist procédurale';
      case 'OPEN_CHAT': return 'Ouvrir le chat';
      case 'OPEN_MISSING_PIECES': return 'Voir les pièces manquantes';
      default: return 'Voir la synthèse';
    }
  }

  onClickSource(exp: SourceExplanation | null): void {
    this.onActionFor(exp);
  }
}
