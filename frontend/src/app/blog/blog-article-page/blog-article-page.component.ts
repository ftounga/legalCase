import { ChangeDetectionStrategy, Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DomSanitizer, SafeHtml, Title, Meta } from '@angular/platform-browser';

import { BlogService } from '../blog.service';
import { MarkdownRendererService } from '../markdown-renderer.service';
import { BlogArticleDetail, COUNTRY_LABELS, LEGAL_DOMAIN_LABELS } from '../blog.model';

@Component({
  selector: 'app-blog-article-page',
  standalone: true,
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './blog-article-page.component.html',
  styleUrl: './blog-article-page.component.scss',
})
export class BlogArticlePageComponent implements OnInit {

  private readonly route = inject(ActivatedRoute);
  private readonly blogService = inject(BlogService);
  private readonly markdownRenderer = inject(MarkdownRendererService);
  private readonly sanitizer = inject(DomSanitizer);
  private readonly titleService = inject(Title);
  private readonly metaService = inject(Meta);

  protected readonly legalDomainLabels = LEGAL_DOMAIN_LABELS;
  protected readonly countryLabels = COUNTRY_LABELS;

  protected loading = true;
  protected error: string | null = null;
  protected article: BlogArticleDetail | null = null;
  protected renderedHtml: SafeHtml = '';

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const slug = params.get('slug');
      if (!slug) {
        this.error = 'Article introuvable';
        this.loading = false;
        return;
      }
      this.load(slug);
    });
  }

  private load(slug: string): void {
    this.loading = true;
    this.error = null;
    this.blogService.getArticleBySlug(slug).subscribe({
      next: article => {
        this.article = article;
        const cta = '/contact';
        const bodyWithCta = (article.bodyMarkdown ?? '').replace(/\{\{LEGALCASE_CTA\}\}/g, cta);
        this.renderedHtml = this.sanitizer.bypassSecurityTrustHtml(
          this.markdownRenderer.render(bodyWithCta));
        this.applySeoTags(article);
        this.loading = false;
      },
      error: () => {
        this.error = 'Article introuvable';
        this.loading = false;
      },
    });
  }

  private applySeoTags(article: BlogArticleDetail): void {
    this.titleService.setTitle(article.metaTitle || article.title);
    this.metaService.updateTag({ name: 'description', content: article.metaDescription });
    this.metaService.updateTag({ property: 'og:title', content: article.metaTitle || article.title });
    this.metaService.updateTag({ property: 'og:description', content: article.metaDescription });
    this.metaService.updateTag({ property: 'og:type', content: 'article' });
    if (article.heroImageUrl) {
      this.metaService.updateTag({ property: 'og:image', content: article.heroImageUrl });
    }
    this.metaService.updateTag({ name: 'twitter:card', content: 'summary_large_image' });
  }
}
