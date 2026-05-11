import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { DublinRecoursService } from './dublin-recours.service';
import { DublinRecoursRequest, DublinRecoursResponse } from '../models/dublin-recours.model';

describe('DublinRecoursService', () => {
  let service: DublinRecoursService;
  let httpMock: HttpTestingController;
  const BASE = '/api/v1/case-files/case-1/dublin-recours-analysis';

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [DublinRecoursService],
    });
    service = TestBed.inject(DublinRecoursService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('analyze() POSTs request body', () => {
    const req: DublinRecoursRequest = {
      dateNotificationDecisionTransfert: '2026-05-01',
      etatMembreResponsable: 'ITALIE',
      motifTransfert: 'DEMANDE_ASILE_AUTRE_ETAT',
      recoursForme: false,
    };
    service.analyze('case-1', req).subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('POST');
    expect(r.request.body).toEqual(req);
    r.flush({} as DublinRecoursResponse);
  });

  it('get() GETs the analysis', () => {
    service.get('case-1').subscribe();
    const r = httpMock.expectOne(BASE);
    expect(r.request.method).toBe('GET');
    r.flush({} as DublinRecoursResponse);
  });
});
