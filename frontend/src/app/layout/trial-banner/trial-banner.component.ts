import { Component, Input, OnChanges, signal } from '@angular/core';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { BillingService } from '../../core/services/billing.service';
import { Workspace } from '../../core/models/workspace.model';

@Component({
  selector: 'app-trial-banner',
  standalone: true,
  imports: [MatButtonModule, MatIconModule],
  templateUrl: './trial-banner.component.html',
  styleUrl: './trial-banner.component.scss'
})
export class TrialBannerComponent implements OnChanges {
  @Input() workspace: Workspace | null = null;
  visible = signal(false);

  constructor(
    private billingService: BillingService,
    private router: Router
  ) {}

  ngOnChanges(): void {
    if (this.workspace && this.billingService.shouldShowTrialBanner(this.workspace)) {
      this.visible.set(true);
    }
  }

  goToPlans(): void {
    this.router.navigate(['/workspace/billing']);
  }

  dismiss(): void {
    this.visible.set(false);
  }

  isExpired(): boolean {
    if (!this.workspace?.expiresAt) return false;
    return new Date(this.workspace.expiresAt) < new Date();
  }

  expiresAt(): string | null {
    if (!this.workspace?.expiresAt) return null;
    return new Date(this.workspace.expiresAt).toLocaleDateString('fr-FR', {
      day: 'numeric', month: 'long', year: 'numeric'
    });
  }
}
