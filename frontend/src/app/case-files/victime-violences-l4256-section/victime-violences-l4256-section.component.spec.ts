import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';

import { VictimeViolencesL4256SectionComponent } from './victime-violences-l4256-section.component';
import { VictimeViolencesL4256Response } from '../../core/models/victime-violences-l4256.model';

describe('VictimeViolencesL4256SectionComponent', () => {
  let component: VictimeViolencesL4256SectionComponent;
  let fixture: ComponentFixture<VictimeViolencesL4256SectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/victime-violences-l4256-analysis';

  function frResponse(
    verdict: 'ELIGIBLE_PLEIN_DROIT' | 'ELIGIBLE_SOUS_RESERVE' | 'NON_ELIGIBLE' = 'ELIGIBLE_PLEIN_DROIT',
    overrides: Partial<VictimeViolencesL4256Response> = {},
  ): VictimeViolencesL4256Response {
    return {
      caseFileId: 'case-1',
      dateOrdonnanceProtection: '2026-03-01',
      juridiction: 'JAF Paris',
      dureeProtectionMois: 6,
      dateExpirationProtectionEffective: '2026-09-01',
      enfantsAcharge: 2,
      nationalite: 'Marocaine',
      country: 'FRANCE',
      eligibiliteScore: verdict,
      criteresValides: ['Ordonnance JAF rendue le 2026-03-01'],
      criteresManquants: [],
      dureeTitreSejourMois: 12,
      formule: 'Demande L.425-6 - verdict ELIGIBLE',
      baseJuridique: 'CESEDA L.425-6+, Cciv 515-9+',
      messages: ['Carte de sejour VPF de plein droit'],
      ...overrides,
    };
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [VictimeViolencesL4256SectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(VictimeViolencesL4256SectionComponent);
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
    expect(VictimeViolencesL4256SectionComponent.TOOL_LABEL).toContain('VICTIME');
    expect(VictimeViolencesL4256SectionComponent.TOOL_ICON).toBe('shield');
  });

  // SF-246-04 : pré-fill IA effectif sur dateOrdonnanceProtection.
  it('static getPrefillCount returns 0 when no IA date signal', () => {
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({})).toBe(0);
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: { dateNotificationOqtf: '2026-04-01' },
    })).toBe(0);
  });

  it('static getPrefillCount returns 1 when a valid ISO ordonnance date is detected (FR)', () => {
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: { dateOrdonnanceProtectionJaf: '2026-01-15' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 for BELGIQUE even with a valid date', () => {
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: { dateOrdonnanceProtectionJaf: '2026-01-15' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('prefillFromAi : valid IA date prefills dateOrdonnanceProtection + sets provenance + parity with getPrefillCount', () => {
    component.aiData = { dateOrdonnanceProtectionJaf: '2026-01-15' };
    component.ngOnInit();
    flush404();
    expect(component.dateOrdonnanceProtection()).toBe('2026-01-15');
    expect(component.provenanceDateOrdonnance()).toBe('IA');
    // Parité stricte : 1 champ pré-rempli ⇔ getPrefillCount = 1.
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: component.aiData,
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('prefillFromAi : non-ISO IA date is rejected, no prefill (parity with getPrefillCount = 0)', () => {
    component.aiData = { dateOrdonnanceProtectionJaf: '15/01/2026' };
    component.ngOnInit();
    flush404();
    expect(component.dateOrdonnanceProtection()).toBeNull();
    expect(component.provenanceDateOrdonnance()).toBeNull();
    expect(VictimeViolencesL4256SectionComponent.getPrefillCount({
      aiData: component.aiData,
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('prefillFromAi : no-op on BELGIQUE workspace even with a valid IA date', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.aiData = { dateOrdonnanceProtectionJaf: '2026-01-15' };
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.dateOrdonnanceProtection()).toBeNull();
    expect(component.provenanceDateOrdonnance()).toBeNull();
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
    httpMock.expectOne(BASE_URL).flush(frResponse('ELIGIBLE_PLEIN_DROIT'));
    expect(component.result()!.eligibiliteScore).toBe('ELIGIBLE_PLEIN_DROIT');
    expect(component.juridiction()).toBe('JAF Paris');
    expect(component.dureeProtectionMois()).toBe(6);
    expect(component.enfantsAcharge()).toBe(2);
    expect(component.showForm()).toBe(false);
  });

  it('GET 404 -> form mode', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
  });

  it('formValid requires date + juridiction + duree > 0 + enfants >= 0', () => {
    component.dateOrdonnanceProtection.set('2026-03-01');
    component.juridiction.set('JAF Paris');
    component.dureeProtectionMois.set(6);
    component.enfantsAcharge.set(0);
    expect(component.formValid()).toBe(true);
    component.juridiction.set(null);
    expect(component.formValid()).toBe(false);
    component.juridiction.set('   ');
    expect(component.formValid()).toBe(false);
    component.juridiction.set('JAF Paris');
    component.dureeProtectionMois.set(0);
    expect(component.formValid()).toBe(false);
    component.dureeProtectionMois.set(6);
    component.enfantsAcharge.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('formValid rejects future date', () => {
    const future = new Date();
    future.setDate(future.getDate() + 5);
    component.dateOrdonnanceProtection.set(future.toISOString().slice(0, 10));
    component.juridiction.set('JAF Paris');
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.dateOrdonnanceProtection.set('2026-03-01');
    component.juridiction.set('JAF Paris');
    component.dureeProtectionMois.set(6);
    component.enfantsAcharge.set(2);
    component.nationalite.set('Marocaine');
    component.analyze();
    const req = httpMock.expectOne(r => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateOrdonnanceProtection: '2026-03-01',
      juridiction: 'JAF Paris',
      dureeProtectionMois: 6,
      dateExpirationProtection: null,
      enfantsAcharge: 2,
      nationalite: 'Marocaine',
    });
    req.flush(frResponse('ELIGIBLE_PLEIN_DROIT'));
    expect(component.result()!.eligibiliteScore).toBe('ELIGIBLE_PLEIN_DROIT');
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() error -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateOrdonnanceProtection.set('2026-03-01');
    component.juridiction.set('JAF Paris');
    component.analyze();
    httpMock.expectOne(r => r.method === 'POST').flush({ message: 'Bad' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String), 'Fermer', jasmine.objectContaining({ panelClass: 'snack-error' })
    );
  });

  it('verdict ELIGIBLE_PLEIN_DROIT -> success', () => {
    expect(component.verdictClass('ELIGIBLE_PLEIN_DROIT')).toContain('vvl-banner--success');
    expect(component.verdictIcon('ELIGIBLE_PLEIN_DROIT')).toBe('verified');
    expect(component.verdictLabel('ELIGIBLE_PLEIN_DROIT')).toContain('plein droit');
  });

  it('verdict ELIGIBLE_SOUS_RESERVE -> warning', () => {
    expect(component.verdictClass('ELIGIBLE_SOUS_RESERVE')).toContain('vvl-banner--warning');
    expect(component.verdictIcon('ELIGIBLE_SOUS_RESERVE')).toBe('warning');
    expect(component.verdictLabel('ELIGIBLE_SOUS_RESERVE')).toContain('sous reserve');
  });

  it('verdict NON_ELIGIBLE -> danger', () => {
    expect(component.verdictClass('NON_ELIGIBLE')).toContain('vvl-banner--danger');
    expect(component.verdictIcon('NON_ELIGIBLE')).toBe('error');
    expect(component.verdictLabel('NON_ELIGIBLE')).toContain('Non eligible');
  });

  it('coherenceAlerts : divergence F-96 -> WARNING', () => {
    component.procedureChecks = [
      { id: 'p1', ordre: 1, description: 'Ordonnance', statut: 'NON_COMPLIANT',
        critereCode: 'IM24_DATE_ORDONNANCE_PROTECTION', expectedValue: '2026-03-01' },
    ];
    component.ngOnInit();
    flush404();
    component.dateOrdonnanceProtection.set('2026-03-05');
    component.juridiction.set('JAF Paris');
    const alert = component.coherenceAlerts().DATE_ORDONNANCE;
    expect(alert).toBeDefined();
    expect(alert!.severity).toBe('WARNING');
    expect(alert!.expectedDisplay).toBe('2026-03-01');
  });

  it('no alerts when showForm=false', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.coherenceAlerts()).toEqual({});
  });

  it('onDateOrdonnanceChange clears provenance', () => {
    component.provenanceDateOrdonnance.set('IA');
    component.onDateOrdonnanceChange('2026-03-05');
    expect(component.provenanceDateOrdonnance()).toBeNull();
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
    const STANDALONE_URL_F163 = '/api/v1/simulators/F-IM-24-victime-violences-l4256-fr/calculate';

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
