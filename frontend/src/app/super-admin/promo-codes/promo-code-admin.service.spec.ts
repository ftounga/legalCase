import { TestBed } from '@angular/core/testing';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';

import { PromoCodeAdminService } from './promo-code-admin.service';
import {
  PromoCodeCreateRequest,
  PromoCodeDto,
} from './promo-code.model';

const mockCode: PromoCodeDto = {
  id: '11111111-1111-1111-1111-111111111111',
  code: 'BARREAU2026',
  type: 'TRIAL_EXTENSION',
  valueDays: 30,
  stripeCouponId: null,
  partnerLabel: 'Barreau de Bordeaux',
  maxUses: 100,
  usesCount: 0,
  expiresAt: '2027-01-01T23:59:59Z',
  active: true,
  createdAt: '2026-05-23T10:00:00Z',
  createdByUserId: '22222222-2222-2222-2222-222222222222',
};

describe('PromoCodeAdminService', () => {
  let service: PromoCodeAdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        PromoCodeAdminService,
        provideHttpClient(),
        provideHttpClientTesting(),
      ],
    });
    service = TestBed.inject(PromoCodeAdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('createCode POST /api/v1/super-admin/promo-codes avec le body req', () => {
    const req: PromoCodeCreateRequest = {
      code: 'BARREAU2026',
      type: 'TRIAL_EXTENSION',
      valueDays: 30,
      partnerLabel: 'Barreau de Bordeaux',
      maxUses: 100,
      expiresAt: '2027-01-01T23:59:59Z',
    };

    let received: PromoCodeDto | null = null;
    service.createCode(req).subscribe(r => (received = r));

    const httpReq = httpMock.expectOne('/api/v1/super-admin/promo-codes');
    expect(httpReq.request.method).toBe('POST');
    expect(httpReq.request.body).toEqual(req);
    httpReq.flush(mockCode);

    expect(received).toEqual(mockCode);
  });

  it('listCodes GET /api/v1/super-admin/promo-codes retourne la liste', () => {
    let received: PromoCodeDto[] | null = null;
    service.listCodes().subscribe(r => (received = r));

    const httpReq = httpMock.expectOne('/api/v1/super-admin/promo-codes');
    expect(httpReq.request.method).toBe('GET');
    httpReq.flush([mockCode]);

    expect(received).toEqual([mockCode]);
  });

  it('deactivateCode POST /api/v1/super-admin/promo-codes/{id}/deactivate', () => {
    const updated: PromoCodeDto = { ...mockCode, active: false };

    let received: PromoCodeDto | null = null;
    service.deactivateCode(mockCode.id).subscribe(r => (received = r));

    const httpReq = httpMock.expectOne(
      `/api/v1/super-admin/promo-codes/${mockCode.id}/deactivate`
    );
    expect(httpReq.request.method).toBe('POST');
    expect(httpReq.request.body).toEqual({});
    httpReq.flush(updated);

    expect(received).toEqual(updated);
    expect(received!.active).toBe(false);
  });
});
