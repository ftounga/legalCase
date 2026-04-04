import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ReferentialService } from './referential.service';
import { ReferentialResponse } from '../models/referential.model';

describe('ReferentialService', () => {
  let service: ReferentialService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReferentialService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ReferentialService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('getReferentials appelle GET /api/v1/referentials?domain=DROIT_DU_TRAVAIL', () => {
    const mockResponse: ReferentialResponse = {
      domain: 'DROIT_DU_TRAVAIL',
      sections: {
        LITIGATION_TYPE: [
          { key: 'DISCRIMINATION', label: 'Discrimination', valueJson: '{"years":5,"article":"Art. L1132-1"}', isSystem: true }
        ]
      }
    };

    service.getReferentials('DROIT_DU_TRAVAIL').subscribe(res => {
      expect(res.domain).toBe('DROIT_DU_TRAVAIL');
      expect(res.sections['LITIGATION_TYPE']).toHaveLength(1);
    });

    const req = httpMock.expectOne(r =>
      r.url === '/api/v1/referentials' && r.params.get('domain') === 'DROIT_DU_TRAVAIL'
    );
    expect(req.request.method).toBe('GET');
    req.flush(mockResponse);
  });

  it('getReferentials appelle GET /api/v1/referentials?domain=DROIT_IMMIGRATION', () => {
    service.getReferentials('DROIT_IMMIGRATION').subscribe();
    const req = httpMock.expectOne(r =>
      r.url === '/api/v1/referentials' && r.params.get('domain') === 'DROIT_IMMIGRATION'
    );
    expect(req.request.method).toBe('GET');
    req.flush({ domain: 'DROIT_IMMIGRATION', sections: {} });
  });
});
