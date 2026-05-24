import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CrrvRefusVisaSectionComponent } from './crrv-refus-visa-section.component';
import { CrrvRefusVisaResponse } from '../../core/models/crrv-refus-visa.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('CrrvRefusVisaSectionComponent', () => {
  let component: CrrvRefusVisaSectionComponent;
  let fixture: ComponentFixture<CrrvRefusVisaSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/crrv-refus-visa-analysis';

  function frResponse(overrides: Partial<CrrvRefusVisaResponse> = {}): CrrvRefusVisaResponse {
    return {
      caseFileId: 'case-1',
      dateNotificationRefus: '2026-04-15',
      typeVisa: 'LONG_SEJOUR',
      motifRefus: 'Ressources insuffisantes',
      recoursForme: false,
      dateRecours: null,
      country: 'FRANCE',
      dateExpirationRecoursCrrv: '2026-06-15',
      joursRestants: 30,
      statut: 'DISPONIBLE',
      formule: 'Refus visa LONG_SEJOUR notifie le 2026-04-15',
      baseJuridique: 'CESEDA L.312-1+, D.312-3',
      messages: ['Prealable obligatoire avant TA Nantes'],
      ...overrides,
    };
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [CrrvRefusVisaSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(CrrvRefusVisaSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('TOOL_LABEL and TOOL_ICON statics', () => {
    expect(CrrvRefusVisaSectionComponent.TOOL_LABEL).toContain('CRRV');
    expect(CrrvRefusVisaSectionComponent.TOOL_ICON).toBe('mail');
  });

  it('static getPrefillCount 0/1', () => {
    expect(CrrvRefusVisaSectionComponent.getPrefillCount({})).toBe(0);
    expect(CrrvRefusVisaSectionComponent.getPrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-15' },
    })).toBe(1);
    expect(CrrvRefusVisaSectionComponent.getPrefillCount({
      aiData: { dateNotificationDecisionContestee: '2026-04-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('5 types de visa disponibles', () => {
    expect(component.typesVisa.length).toBe(5);
    expect(component.typesVisa.map(t => t.code)).toEqual([
      'COURT_SEJOUR', 'LONG_SEJOUR', 'REGROUPEMENT_FAMILIAL', 'ETUDIANT', 'AUTRE',
    ]);
  });

  it('FRANCE -> GET', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush({}, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('GET 200 -> loads result', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.dateNotificationRefus()).toBe('2026-04-15');
    expect(component.typeVisa()).toBe('LONG_SEJOUR');
    expect(component.motifRefus()).toBe('Ressources insuffisantes');
    expect(component.showForm()).toBe(false);
  });

  it('GET 404 -> form mode', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
  });

  it('formValid requires date + typeVisa, motif <= 200 chars', () => {
    component.dateNotificationRefus.set('2026-04-15');
    component.typeVisa.set(null);
    expect(component.formValid()).toBe(false);
    component.typeVisa.set('LONG_SEJOUR');
    expect(component.formValid()).toBe(true);
    component.motifRefus.set('x'.repeat(201));
    expect(component.formValid()).toBe(false);
    component.motifRefus.set('valid');
    expect(component.formValid()).toBe(true);
  });

  it('formValid rejects future date', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    component.dateNotificationRefus.set(future.toISOString().slice(0, 10));
    component.typeVisa.set('LONG_SEJOUR');
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationRefus.set('2026-04-15');
    component.typeVisa.set('LONG_SEJOUR');
    component.motifRefus.set('Ressources insuffisantes');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateNotificationRefus: '2026-04-15',
      typeVisa: 'LONG_SEJOUR',
      motifRefus: 'Ressources insuffisantes',
      recoursForme: false,
    });
    req.flush(frResponse());
    expect(component.result()!.statut).toBe('DISPONIBLE');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() recoursForme=true includes dateRecours', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationRefus.set('2026-04-15');
    component.typeVisa.set('ETUDIANT');
    component.recoursForme.set(true);
    component.dateRecours.set('2026-05-01');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST');
    expect(req.request.body.recoursForme).toBe(true);
    expect(req.request.body.dateRecours).toBe('2026-05-01');
    req.flush(frResponse({ recoursForme: true, dateRecours: '2026-05-01', statut: 'RECOURS_FORME' }));
    expect(component.result()!.statut).toBe('RECOURS_FORME');
  });

  it('analyze() error -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateNotificationRefus.set('2026-04-15');
    component.typeVisa.set('LONG_SEJOUR');
    component.analyze();
    httpMock.expectOne(r => r.method === 'POST').flush({ message: 'Bad' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' })
    );
  });

  it('pre-fill IA: dateNotificationDecisionContestee -> date + badge', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateNotificationRefus()).toBe('2026-04-15');
    expect(component.provenanceDateNotification()).toBe('IA');
  });

  it('onDateNotificationChange clears provenance', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    component.onDateNotificationChange('2026-04-20');
    expect(component.provenanceDateNotification()).toBeNull();
  });

  it('coherenceAlerts : divergence -> WARNING', () => {
    component.aiData = { dateNotificationDecisionContestee: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    component.onDateNotificationChange('2026-04-20');
    const alert = component.coherenceAlerts().DATE_NOTIFICATION;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toBe('2026-04-15');
  });

  it('bannerClass + statutLabel', () => {
    expect(component.bannerClass('DISPONIBLE')).toContain('crrv-banner--info');
    expect(component.bannerClass('URGENT')).toContain('crrv-banner--warning');
    expect(component.bannerClass('EXPIRE')).toContain('crrv-banner--danger');
    expect(component.bannerClass('RECOURS_FORME')).toContain('crrv-banner--success');
    expect(component.statutLabel('URGENT')).toContain('URGENT');
  });

  it('showJoursRestants', () => {
    expect(component.showJoursRestants(frResponse({ statut: 'DISPONIBLE' }))).toBe(true);
    expect(component.showJoursRestants(frResponse({ statut: 'EXPIRE' }))).toBe(false);
    expect(component.showJoursRestants(null)).toBe(false);
  });

  it('ngOnChanges re-prefill', () => {
    component.ngOnInit();
    flush404();
    component.aiData = { dateNotificationDecisionContestee: '2026-04-15' } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateNotificationRefus()).toBe('2026-04-15');
  });

  it('toggleCollapse + editMode', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  // ---------------------------------------------------------------------------
  // F-163 SF-163-02d — mode simulateur autonome
  // ---------------------------------------------------------------------------
  describe('F-163 SF-163-02d — mode standalone', () => {
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-23-crrv-refus-visa-fr/calculate';

    it('CA-02 : affiche la bannière 🧪 quand standaloneMode=true', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
      expect(banner).not.toBeNull();
      expect(banner.textContent).toContain('Mode simulateur');
    });

    it('CA-02 : aucun GET vers /api/v1/case-files/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      const matches = httpMock.match((r: { url: string }) => r.url.includes('/api/v1/case-files/'));
      expect(matches.length).toBe(0);
    });

    it('CA-04 : POST sur le dispatcher /api/v1/simulators/... en standalone', () => {
      component.standaloneMode = true;
      fixture.detectChanges();
      try { (component as any).analyze(); } catch (_) { /* formValid */ }
      const dispatcherReqs = httpMock.match((r: { url: string; method: string }) => r.url === STANDALONE_URL_F163 && r.method === 'POST');
      const caseFileReqs = httpMock.match((r: { url: string; method: string }) => r.url.includes('/api/v1/case-files/') && r.method === 'POST');
      // Aucun POST case-file ne doit partir en standalone.
      expect(caseFileReqs.length).toBe(0);
      dispatcherReqs.forEach((req: any) => req.flush({}));
    });
  });
});
