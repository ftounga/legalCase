import { ChangeDetectionStrategy, Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

import {
  LANDING_TOOLS_CATALOG,
  LandingTool,
  LandingToolCountry,
  LandingToolDomain,
} from '../landing-tools-catalog';

const DOMAIN_LABELS: Record<LandingToolDomain, string> = {
  TRAVAIL: 'Droit du travail',
  IMMIGRATION: 'Droit de l\'immigration',
  FAMILLE: 'Droit de la famille',
};

const COUNTRY_LABELS: Record<LandingToolCountry, string> = {
  FR: 'France',
  BE: 'Belgique',
};

@Component({
  selector: 'app-landing-tools-showcase',
  standalone: true,
  imports: [CommonModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './landing-tools-showcase.component.html',
  styleUrl: './landing-tools-showcase.component.scss',
})
export class LandingToolsShowcaseComponent {

  protected readonly catalog: readonly LandingTool[] = LANDING_TOOLS_CATALOG;
  protected readonly domainLabels = DOMAIN_LABELS;
  protected readonly countryLabels = COUNTRY_LABELS;

  protected readonly selectedDomain = signal<LandingToolDomain | null>(null);
  protected readonly selectedCountry = signal<LandingToolCountry | null>(null);

  protected readonly filteredTools = computed<LandingTool[]>(() => {
    const dom = this.selectedDomain();
    const ctry = this.selectedCountry();
    return this.catalog.filter(t =>
      (!dom || t.domain === dom) &&
      (!ctry || t.country === ctry)
    );
  });

  protected readonly totalCount = this.catalog.length;

  protected setDomain(d: LandingToolDomain | null): void {
    this.selectedDomain.set(d);
  }

  protected setCountry(c: LandingToolCountry | null): void {
    this.selectedCountry.set(c);
  }
}
