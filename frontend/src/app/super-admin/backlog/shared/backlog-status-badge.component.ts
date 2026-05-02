import { ChangeDetectionStrategy, Component, Input } from '@angular/core';

type FeatureStatus =
  | 'PLANNED' | 'READY' | 'IN_PROGRESS' | 'BLOCKED'
  | 'DONE' | 'PARTIAL' | 'ABSORBED' | 'UNKNOWN';

type MarketingStatus =
  | 'A_FAIRE' | 'REDIGE' | 'EN_COURS'
  | 'TERMINE' | 'BLOQUE' | 'UNKNOWN';

const FEATURE_LABELS: Record<FeatureStatus, string> = {
  PLANNED: 'À planifier',
  READY: 'Ready to dev',
  IN_PROGRESS: 'En cours',
  BLOCKED: 'Bloqué',
  DONE: 'Terminée',
  PARTIAL: 'Partielle',
  ABSORBED: 'Absorbée',
  UNKNOWN: 'Inconnu',
};

const MARKETING_LABELS: Record<MarketingStatus, string> = {
  A_FAIRE: 'À faire',
  REDIGE: 'Rédigé',
  EN_COURS: 'En cours',
  TERMINE: 'Terminé',
  BLOQUE: 'Bloqué',
  UNKNOWN: 'Inconnu',
};

const FEATURE_TONE: Record<FeatureStatus, string> = {
  PLANNED: 'planned',
  READY: 'ready',
  IN_PROGRESS: 'in-progress',
  BLOCKED: 'blocked',
  DONE: 'done',
  PARTIAL: 'partial',
  ABSORBED: 'absorbed',
  UNKNOWN: 'unknown',
};

const MARKETING_TONE: Record<MarketingStatus, string> = {
  A_FAIRE: 'planned',
  REDIGE: 'partial',
  EN_COURS: 'in-progress',
  TERMINE: 'done',
  BLOQUE: 'blocked',
  UNKNOWN: 'unknown',
};

@Component({
  selector: 'app-backlog-status-badge',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span class="badge badge--{{ tone }}">{{ label }}</span>`,
  styles: [`
    .badge {
      display: inline-block;
      padding: 2px 10px;
      border-radius: 999px;
      font-family: 'Inter', sans-serif;
      font-size: 12px;
      font-weight: 500;
      line-height: 1.4;
      letter-spacing: 0.2px;
      white-space: nowrap;
    }
    .badge--planned { background: #eef2f7; color: #6b7a8d; }
    .badge--ready { background: #fdf3dd; color: #8a6618; }
    .badge--in-progress { background: #e3eaf3; color: #1a3a5c; }
    .badge--blocked { background: #f9e2df; color: #c0392b; }
    .badge--done { background: #dcefe1; color: #1f7a3f; }
    .badge--partial { background: #e3eaf3; color: #1a3a5c; opacity: .85; }
    .badge--absorbed { background: #ece1f5; color: #5b3a89; }
    .badge--unknown { background: #f5f6fa; color: #6b7a8d; }
  `],
})
export class BacklogStatusBadgeComponent {
  @Input({ required: true }) status!: string;
  @Input() kind: 'feature' | 'marketing' = 'feature';

  get label(): string {
    if (this.kind === 'marketing') {
      return MARKETING_LABELS[this.status as MarketingStatus] ?? this.status;
    }
    return FEATURE_LABELS[this.status as FeatureStatus] ?? this.status;
  }

  get tone(): string {
    if (this.kind === 'marketing') {
      return MARKETING_TONE[this.status as MarketingStatus] ?? 'unknown';
    }
    return FEATURE_TONE[this.status as FeatureStatus] ?? 'unknown';
  }
}
