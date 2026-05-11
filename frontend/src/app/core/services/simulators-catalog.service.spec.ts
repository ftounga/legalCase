import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { SimulatorsCatalogService } from './simulators-catalog.service';
import { SimulatorsCatalogResponse } from '../models/simulators-catalog.model';

describe('SimulatorsCatalogService', () => {
  let service: SimulatorsCatalogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SimulatorsCatalogService, provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(SimulatorsCatalogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getCatalog() appelle GET /api/v1/simulators', () => {
    const mockResponse: SimulatorsCatalogResponse = {
      legalDomain: 'DROIT_IMMIGRATION',
      country: 'FRANCE',
      toolIds: ['F-IM-08-oqtf-avec-delai-fr', 'F-IM-09-oqtf-sans-delai-fr'],
    };

    service.getCatalog().subscribe((res) => {
      expect(res.legalDomain).toBe('DROIT_IMMIGRATION');
      expect(res.country).toBe('FRANCE');
      expect(res.toolIds).toHaveLength(2);
    });

    const req = httpMock.expectOne('/api/v1/simulators');
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('getCatalog() propage une erreur 401 si non authentifié', () => {
    let observedStatus = 0;
    service.getCatalog().subscribe({
      next: () => fail('should not emit on 401'),
      error: (err) => {
        observedStatus = err.status;
      },
    });

    const req = httpMock.expectOne('/api/v1/simulators');
    req.flush('Unauthorized', { status: 401, statusText: 'Unauthorized' });
    expect(observedStatus).toBe(401);
  });

  it('getCatalog() retourne toolIds=[] pour workspace sans legalDomain', () => {
    service.getCatalog().subscribe((res) => {
      expect(res.legalDomain).toBeNull();
      expect(res.toolIds).toEqual([]);
    });

    const req = httpMock.expectOne('/api/v1/simulators');
    req.flush({ legalDomain: null, country: null, toolIds: [] } satisfies SimulatorsCatalogResponse);
  });
});
