import { ChangeDetectionStrategy, Component, OnInit, PLATFORM_ID, inject } from '@angular/core';
import { CommonModule, isPlatformBrowser } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';

import { BlogService } from '../blog.service';
import {
  BlogArticleSummary,
  BlogCountry,
  BlogFilters,
  BlogLegalDomain,
  BlogPage,
  COUNTRY_LABELS,
  LEGAL_DOMAIN_LABELS,
} from '../blog.model';

@Component({
  selector: 'app-blog-list-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './blog-list-page.component.html',
  styleUrl: './blog-list-page.component.scss',
})
export class BlogListPageComponent implements OnInit {

  private readonly blogService = inject(BlogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly isBrowser = isPlatformBrowser(inject(PLATFORM_ID));

  protected readonly legalDomainLabels = LEGAL_DOMAIN_LABELS;
  protected readonly countryLabels = COUNTRY_LABELS;

  protected loading = this.isBrowser;
  protected error: string | null = null;
  protected articles: BlogArticleSummary[] = [];
  protected page: BlogPage<BlogArticleSummary> | null = null;
  protected filters: BlogFilters = { page: 0, size: 20 };

  ngOnInit(): void {
    if (!this.isBrowser) {
      return;
    }
    this.route.queryParamMap.subscribe(params => {
      this.filters = {
        country: (params.get('country') as BlogCountry) ?? null,
        legalDomain: (params.get('legalDomain') as BlogLegalDomain) ?? null,
        page: Number(params.get('page') ?? 0),
        size: 20,
      };
      this.load();
    });
  }

  protected load(): void {
    this.loading = true;
    this.error = null;
    this.blogService.listArticles(this.filters).subscribe({
      next: page => {
        this.page = page;
        this.articles = page.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Impossible de charger les articles.';
        this.loading = false;
      },
    });
  }

  protected setCountry(country: BlogCountry): void {
    this.applyFilters({ ...this.filters, country, page: 0 });
  }

  protected setDomain(legalDomain: BlogLegalDomain): void {
    this.applyFilters({ ...this.filters, legalDomain, page: 0 });
  }

  protected goToPage(page: number): void {
    this.applyFilters({ ...this.filters, page });
  }

  private applyFilters(filters: BlogFilters): void {
    const queryParams: Record<string, string | number> = { page: filters.page ?? 0 };
    if (filters.country) queryParams['country'] = filters.country;
    if (filters.legalDomain) queryParams['legalDomain'] = filters.legalDomain;
    this.router.navigate(['/blog'], { queryParams });
  }

  protected heroPlaceholderClass(article: BlogArticleSummary): string {
    return article.heroImageUrl ? '' : 'hero-placeholder';
  }
}
