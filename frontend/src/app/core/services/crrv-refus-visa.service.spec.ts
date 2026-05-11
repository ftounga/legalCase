import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { CrrvRefusVisaService } from './crrv-refus-visa.service';
import { CrrvRefusVisaRequest, CrrvRefusVisaResponse } from '../models/crrv-refus-visa.model';

describe('CrrvRefusVisaService', () => {
  let service: CrrvRefusVisaService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/crrv-refus-visa-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [CrrvRefusVisaService],
    });
    service = TestBed.inject(CrrvRefusVisaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: CrrvRefusVisaRequest = {
      dateNotificationRefus: '2026-04-15',
      typeVisa: 'LONG_SEJOUR',
      motifRefus: 'Ressources insuffisantes',
      recoursForme: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as CrrvRefusVisaResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as CrrvRefusVisaResponse);
  });
});
