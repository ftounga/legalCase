import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { ImmigrationTitleDecisionSectionComponent } from './immigration-title-decision-section.component';

describe('ImmigrationTitleDecisionSectionComponent', () => {
  let component: ImmigrationTitleDecisionSectionComponent;
  let fixture: ComponentFixture<ImmigrationTitleDecisionSectionComponent>;
  let httpMock: HttpTestingController;

  const CASE_FILE_ID = '11111111-1111-1111-1111-111111111111';
  const API_URL = `/api/v1/case-files/${CASE_FILE_ID}/immigration/title-decision`;

  const MOCK_RESPONSE = {
    caseFileId: CASE_FILE_ID,
    country: 'FRANCE',
    nationaliteUe: false,
    motif: 'TRAVAIL',
    duree: 'LONG_SEJOUR',
    situationFamiliale: null,
    recommendations: [{
      code: 'VLS_TS_SALARIE', label: 'VLS-TS Salarié', country: 'FRANCE',
      motif: 'TRAVAIL', conditions: 'Test', pieces: ['Passeport'], delaiMoyenJours: 120
    }]
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ImmigrationTitleDecisionSectionComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideAnimationsAsync(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ImmigrationTitleDecisionSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = CASE_FILE_ID;
  });

  afterEach(() => {
    httpMock.verify();
  });

  function initWithNoExistingDecision(): void {
    fixture.detectChanges(); // triggers ngOnInit → GET
    httpMock.expectOne(API_URL).flush(null, { status: 404, statusText: 'Not Found' });
  }

  function initWithExistingDecision(resp = MOCK_RESPONSE): void {
    fixture.detectChanges();
    httpMock.expectOne(API_URL).flush(resp);
  }

  it('should create', () => {
    initWithNoExistingDecision();
    expect(component).toBeTruthy();
  });

  it('should call GET on init', () => {
    fixture.detectChanges();
    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('GET');
    req.flush(null, { status: 404, statusText: 'Not Found' });
  });

  it('should show form when no existing decision', () => {
    initWithNoExistingDecision();
    expect(component.showForm()).toBe(true);
    expect(component.decision()).toBeNull();
  });

  it('should call POST when resolve() is called', () => {
    initWithNoExistingDecision();

    component.country.set('FRANCE');
    component.nationaliteUe.set(false);
    component.motif.set('TRAVAIL');
    component.duree.set('LONG_SEJOUR');
    component.resolve();

    const req = httpMock.expectOne(API_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.country).toBe('FRANCE');
    expect(req.request.body.motif).toBe('TRAVAIL');

    req.flush(MOCK_RESPONSE);

    expect(component.decision()).toBeTruthy();
    expect(component.decision()!.recommendations.length).toBe(1);
    expect(component.showForm()).toBe(false);
  });

  it('should display existing decision from GET', () => {
    initWithExistingDecision({
      ...MOCK_RESPONSE,
      country: 'BELGIQUE',
      motif: 'ETUDES',
      duree: 'COURT_SEJOUR',
      recommendations: [{
        code: 'CARTE_A_ETUDES', label: 'Carte A Études', country: 'BELGIQUE',
        motif: 'ETUDES', conditions: 'Test', pieces: ['Passeport'], delaiMoyenJours: 90
      }]
    });

    expect(component.decision()).toBeTruthy();
    expect(component.showForm()).toBe(false);
    expect(component.country()).toBe('BELGIQUE');
  });
});
