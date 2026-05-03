export interface BlogArticleSummary {
  id: string;
  slug: string;
  title: string;
  subtitle: string | null;
  metaDescription: string;
  heroImageUrl: string | null;
  heroImageAlt: string | null;
  country: string;
  legalDomain: string;
  authorName: string;
  publishedAt: string | null;
  readingTimeMinutes: number;
}

export interface BlogArticleDetail extends BlogArticleSummary {
  bodyMarkdown: string;
  metaTitle: string;
}

export interface BlogPage<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export type BlogCountry = 'FR' | 'BE' | null;
export type BlogLegalDomain =
  | 'DROIT_DU_TRAVAIL'
  | 'DROIT_IMMIGRATION'
  | 'DROIT_FAMILLE'
  | null;

export interface BlogFilters {
  country?: BlogCountry;
  legalDomain?: BlogLegalDomain;
  page?: number;
  size?: number;
}

export const LEGAL_DOMAIN_LABELS: Record<string, string> = {
  DROIT_DU_TRAVAIL: 'Droit du travail',
  DROIT_IMMIGRATION: 'Droit de l\'immigration',
  DROIT_FAMILLE: 'Droit de la famille',
};

export const COUNTRY_LABELS: Record<string, string> = {
  FR: 'France',
  BE: 'Belgique',
};
