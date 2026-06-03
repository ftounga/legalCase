import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { VpfJeuneMajeurSectionComponent } from './vpf-jeune-majeur-section.component';
import { VpfJeuneMajeurResponse } from '../../core/models/vpf-jeune-majeur.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('VpfJeuneMajeurSectionComponent', () => {
  let component: VpfJeuneMajeurSectionComponent;
  let fixture: ComponentFixture<VpfJeuneMajeurSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/vpf-jeune-majeur-analysis';

  function frResponse(overrides: Partial<VpfJeuneMajeurResponse> = {}): VpfJeuneMajeurResponse {
    return {
      caseFileId: 'case-1',
      age: 18,
      entreMineur: true,
      dateEntreeFrance: '2020-09-01',
      ageEntreeAse: 14,
      priseEnChargeAse: true,
      dateDebutPriseEnCharge: '2020-09-15',
      ancienneteMoisPriseEnCharge: 24,
      scolariseOuFormation: true,
      caractereReelEtSerieuxFormation: true,
      country: 'FRANCE',
      eligibilite: 'ELIGIBLE_L42322',
      ancienneteRequiseMois: 0,
      criteresManquants: [],
      basesJuridiques: ['CESEDA L.423-22'],
      messages: [],
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [VpfJeuneMajeurSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(VpfJeuneMajeurSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(VpfJeuneMajeurSectionComponent.TOOL_LABEL).toContain('JEUNE MAJEUR');
    expect(VpfJeuneMajeurSectionComponent.TOOL_ICON).toBe('school');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(VpfJeuneMajeurSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present', () => {
    expect(VpfJeuneMajeurSectionComponent.getPrefillCount({
      aiData: {
        jeuneMajeurAge: 18,
        jeuneMajeurEntreMineur: true,
        jeuneMajeurPriseEnChargeAse: true,
        jeuneMajeurScolarise: false,
      },
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(VpfJeuneMajeurSectionComponent.getPrefillCount({
      aiData: { jeuneMajeurAge: 18 },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  it('FRANCE -> GET called on ngOnInit', () => {
    expect(component.isFrance()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('BELGIQUE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse());
    expect(component.result()!.eligibilite).toBe('ELIGIBLE_L42322');
    expect(component.showForm()).toBe(false);
    expect(component.age()).toBe(18);
    expect(component.entreMineur()).toBe(true);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false until valid age set', () => {
    expect(component.formValid()).toBe(false);
    component.age.set(18);
    expect(component.formValid()).toBe(true);
    component.age.set(45);
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.age.set(18);
    component.entreMineur.set(true);
    component.priseEnChargeAse.set(true);
    component.scolariseOuFormation.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.age).toBe(18);
    expect(req.request.body.entreMineur).toBe(true);
    req.flush(frResponse());
    expect(component.result()!.eligibilite).toBe('ELIGIBLE_L42322');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.age.set(18);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 4 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      jeuneMajeurAge: 17,
      jeuneMajeurEntreMineur: true,
      jeuneMajeurPriseEnChargeAse: true,
      jeuneMajeurScolarise: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.age()).toBe(17);
    expect(component.provenanceAge()).toBe('IA');
    expect(component.entreMineur()).toBe(true);
    expect(component.provenanceEntreMineur()).toBe('IA');
    expect(component.priseEnChargeAse()).toBe(true);
    expect(component.provenancePriseEnCharge()).toBe('IA');
    expect(component.scolariseOuFormation()).toBe(true);
    expect(component.provenanceScolarise()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      jeuneMajeurAge: 19,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ age: 18 }));
    expect(component.age()).toBe(18);
    expect(component.provenanceAge()).toBeNull();
  });

  it('onAgeChange / onEntreMineurChange / onScolariseChange clear provenance', () => {
    component.aiData = {
      jeuneMajeurAge: 18,
      jeuneMajeurEntreMineur: true,
      jeuneMajeurScolarise: true,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceAge()).toBe('IA');
    component.onAgeChange(19);
    expect(component.provenanceAge()).toBeNull();
    component.onEntreMineurChange(false);
    expect(component.provenanceEntreMineur()).toBeNull();
    component.onScolariseChange(false);
    expect(component.provenanceScolarise()).toBeNull();
  });

  it('bannerClass / bannerIcon cover the four verdict states', () => {
    expect(component.bannerClass(frResponse({ eligibilite: 'ELIGIBLE_L42322' }))).toContain('vjm-banner--success');
    expect(component.bannerClass(frResponse({ eligibilite: 'ELIGIBLE_SOUS_RESERVE' }))).toContain('vjm-banner--warning');
    expect(component.bannerClass(frResponse({ eligibilite: 'ORIENTER_AES' }))).toContain('vjm-banner--warning');
    expect(component.bannerClass(frResponse({ eligibilite: 'NON_ELIGIBLE' }))).toContain('vjm-banner--neutral');
    expect(component.bannerIcon(frResponse({ eligibilite: 'ELIGIBLE_L42322' }))).toBe('verified');
    expect(component.bannerIcon(frResponse({ eligibilite: 'ORIENTER_AES' }))).toBe('alt_route');
    expect(component.bannerIcon(frResponse({ eligibilite: 'NON_ELIGIBLE' }))).toBe('cancel');
  });

  it('eligibiliteLabel maps the four codes to FR labels', () => {
    expect(component.eligibiliteLabel('ELIGIBLE_L42322')).toContain('L.423-22');
    expect(component.eligibiliteLabel('ELIGIBLE_SOUS_RESERVE')).toContain('sous réserve');
    expect(component.eligibiliteLabel('ORIENTER_AES')).toContain('L.435-3');
    expect(component.eligibiliteLabel('NON_ELIGIBLE')).toContain('Non éligible');
    expect(component.eligibiliteLabel(null)).toBe('');
  });

  it('toggleCollapse inverts collapsed state', () => {
    expect(component.collapsed()).toBe(true);
    component.toggleCollapse();
    expect(component.collapsed()).toBe(false);
  });

  it('editMode resets showForm to true', () => {
    component.showForm.set(false);
    component.editMode();
    expect(component.showForm()).toBe(true);
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.age()).toBeNull();
    component.aiData = {
      jeuneMajeurAge: 20,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.age()).toBe(20);
    expect(component.provenanceAge()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ age: 18 }));
    component.aiData = {
      jeuneMajeurAge: 21,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.age()).toBe(18);
    expect(component.provenanceAge()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.vjm-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible', () => {
    component.standaloneMode = true;
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
    expect(component.showForm()).toBe(true);
    expect(component.collapsed()).toBe(false);
  });
});
