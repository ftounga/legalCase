import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';

import { BlogService } from './blog.service';

describe('BlogService', () => {
  let service: BlogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(BlogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('listArticles uses default page=0,size=20 when no filters', () => {
    service.listArticles().subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/v1/blog/articles');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    expect(req.request.params.get('country')).toBeNull();
    expect(req.request.params.get('legalDomain')).toBeNull();
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });
  });

  it('listArticles propagates country and legalDomain filters', () => {
    service.listArticles({ country: 'FR', legalDomain: 'DROIT_DU_TRAVAIL', page: 2, size: 10 }).subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/v1/blog/articles');
    expect(req.request.params.get('country')).toBe('FR');
    expect(req.request.params.get('legalDomain')).toBe('DROIT_DU_TRAVAIL');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 10 });
  });

  it('getArticleBySlug encodes the slug', () => {
    service.getArticleBySlug('un slug avec espaces').subscribe();
    const req = httpMock.expectOne(`/api/v1/blog/articles/${encodeURIComponent('un slug avec espaces')}`);
    expect(req.request.method).toBe('GET');
    req.flush({});
  });
});
