import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { HttpTestingController } from '@angular/common/http/testing';
import { SynthesisSearchService } from './synthesis-search.service';

describe('SynthesisSearchService', () => {
  let service: SynthesisSearchService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        SynthesisSearchService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(SynthesisSearchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should call GET /api/v1/search with q param', () => {
    service.search('licenciement').subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/v1/search' && r.params.get('q') === 'licenciement');
    expect(req.request.method).toBe('GET');
    req.flush({ query: 'licenciement', totalResults: 0, results: [] });
  });
});
