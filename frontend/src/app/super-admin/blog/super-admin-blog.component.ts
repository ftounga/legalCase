import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';

import { SuperAdminBlogService, CostSummary } from './super-admin-blog.service';
import { BlogArticleSummary, LEGAL_DOMAIN_LABELS, COUNTRY_LABELS } from '../../blog/blog.model';

@Component({
  selector: 'app-super-admin-blog',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './super-admin-blog.component.html',
  styleUrl: './super-admin-blog.component.scss',
})
export class SuperAdminBlogComponent implements OnInit {

  private readonly service = inject(SuperAdminBlogService);
  private readonly snackBar = inject(MatSnackBar);

  protected readonly legalDomainLabels = LEGAL_DOMAIN_LABELS;
  protected readonly countryLabels = COUNTRY_LABELS;

  protected loadingNeedsReview = true;
  protected needsReview: BlogArticleSummary[] = [];
  protected loadingCost = true;
  protected cost: CostSummary | null = null;
  protected runNowInFlight = false;

  ngOnInit(): void {
    this.loadNeedsReview();
    this.loadCost();
  }

  protected loadNeedsReview(): void {
    this.loadingNeedsReview = true;
    this.service.listArticlesByStatus('NEEDS_REVIEW').subscribe({
      next: items => {
        this.needsReview = items;
        this.loadingNeedsReview = false;
      },
      error: () => {
        this.loadingNeedsReview = false;
        this.snackBar.open('Impossible de charger les articles à relire', 'Fermer', { duration: 5000 });
      },
    });
  }

  protected loadCost(): void {
    this.loadingCost = true;
    this.service.getCostSummary().subscribe({
      next: c => {
        this.cost = c;
        this.loadingCost = false;
      },
      error: () => {
        this.loadingCost = false;
      },
    });
  }

  protected approve(article: BlogArticleSummary): void {
    this.service.approve(article.id).subscribe({
      next: () => {
        this.needsReview = this.needsReview.filter(a => a.id !== article.id);
        this.snackBar.open(`Article approuvé — passera en publication au prochain run`, 'Fermer', { duration: 4000 });
      },
      error: () => this.snackBar.open('Erreur lors de l\'approbation', 'Fermer', { duration: 5000 }),
    });
  }

  protected reject(article: BlogArticleSummary): void {
    if (!confirm(`Rejeter et supprimer définitivement l'article "${article.title}" ?`)) {
      return;
    }
    this.service.reject(article.id).subscribe({
      next: () => {
        this.needsReview = this.needsReview.filter(a => a.id !== article.id);
        this.snackBar.open('Article rejeté et supprimé. Le sujet est de nouveau disponible.', 'Fermer', { duration: 4000 });
      },
      error: () => this.snackBar.open('Erreur lors du rejet', 'Fermer', { duration: 5000 }),
    });
  }

  protected runNow(): void {
    this.runNowInFlight = true;
    this.service.runNow().subscribe({
      next: r => {
        this.runNowInFlight = false;
        this.snackBar.open(r.success ? 'Run déclenché' : `Run échoué : ${r.message}`, 'Fermer',
          { duration: 5000 });
        this.loadNeedsReview();
        this.loadCost();
      },
      error: err => {
        this.runNowInFlight = false;
        const msg = err?.status === 429
          ? 'Limite quotidienne atteinte (5 runs/jour)'
          : 'Erreur lors du lancement du run';
        this.snackBar.open(msg, 'Fermer', { duration: 5000 });
      },
    });
  }
}
