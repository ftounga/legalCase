import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ImmigrationWorkRightSectionComponent } from './immigration-work-right-section.component';

describe('ImmigrationWorkRightSectionComponent', () => {
  let component: ImmigrationWorkRightSectionComponent;
  let fixture: ComponentFixture<ImmigrationWorkRightSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '33333333-3333-3333-3333-333333333333';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/work-right`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    titreType: 'VLS_TS_SALARIE',
    titreLabel: 'VLS-TS Salarié',
    country: 'FRANCE',
    droitTravail: 'OUI',
    conditions: 'Droit au travail inclus',
    obligationsEmployeur: ['Vérification préfecture', 'DPAE'],
    baseJuridique: 'Articles L. 421-1 du CESEDA'
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationWorkRightSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationWorkRightSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function initNoExisting(): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExisting(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  it('should create', () => {
    initNoExisting();
    expect(component).toBeTruthy();
  });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });

  it('should show form when no existing result', () => {
    initNoExisting();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('should call POST when resolve() is called', () => {
    initNoExisting();
    component.titreType.set('VLS_TS_SALARIE');
    component.country.set('FRANCE');
    component.resolve();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.titreType).toBe('VLS_TS_SALARIE');
    req.flush(MOCK_RESPONSE);

    expect(component.result()).toBeTruthy();
    expect(component.result()!.droitTravail).toBe('OUI');
    expect(component.showForm()).toBe(false);
  });

  it('should display existing result from GET', () => {
    initWithExisting();
    expect(component.result()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.titreType()).toBe('VLS_TS_SALARIE');
  });
});
