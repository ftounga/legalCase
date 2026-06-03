import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { PacsVpfSectionComponent } from './pacs-vpf-section.component';
import { PacsVpfResponse } from '../../core/models/pacs-vpf.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('PacsVpfSectionComponent', () => {
  let component: PacsVpfSectionComponent;
  let fixture: ComponentFixture<PacsVpfSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/pacs-vpf-analysis';

  function frResponse(overrides: Partial<PacsVpfResponse> = {}): PacsVpfResponse {
    return {
      caseFileId: 'case-1',
      pacsConclu: true,
      datePacs: '2022-01-15',
      partenaireStatut: 'FRANCAIS',
      dureeVieCommuneMois: 24,
      intensiteCommunauteVie: 'FORTE',
      autresLiensPrivesFamiliaux: true,
      country: 'FRANCE',
      eligibilite: 'FAISCEAU_FAVORABLE',
      elementsFavorables: ['PACS conclu'],
      elementsManquants: [],
      basesJuridiques: ['CESEDA L.423-23'],
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
      imports: [PacsVpfSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(PacsVpfSectionComponent);
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
    expect(PacsVpfSectionComponent.TOOL_LABEL).toContain('PACS');
    expect(PacsVpfSectionComponent.TOOL_ICON).toBe('favorite');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(PacsVpfSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present', () => {
    expect(PacsVpfSectionComponent.getPrefillCount({
      aiData: {
        pacsConclu: true,
        pacsDate: '2022-01-15',
        pacsDureeVieCommune: 24,
        pacsIntensiteCommunauteVie: 'FORTE',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(PacsVpfSectionComponent.getPrefillCount({
      aiData: { pacsConclu: true },
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
    expect(component.result()!.eligibilite).toBe('FAISCEAU_FAVORABLE');
    expect(component.showForm()).toBe(false);
    expect(component.pacsConclu()).toBe(true);
    expect(component.intensiteCommunauteVie()).toBe('FORTE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid always true (NON_ELIGIBLE couvre l\'absence de PACS)', () => {
    expect(component.formValid()).toBe(true);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    component.pacsConclu.set(true);
    component.partenaireStatut.set('FRANCAIS');
    component.dureeVieCommuneMois.set(24);
    component.intensiteCommunauteVie.set('FORTE');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body.pacsConclu).toBe(true);
    expect(req.request.body.intensiteCommunauteVie).toBe('FORTE');
    req.flush(frResponse());
    expect(component.result()!.eligibilite).toBe('FAISCEAU_FAVORABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
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
      pacsConclu: true,
      pacsDate: '2021-06-01',
      pacsDureeVieCommune: 18,
      pacsIntensiteCommunauteVie: 'MOYENNE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.pacsConclu()).toBe(true);
    expect(component.provenancePacsConclu()).toBe('IA');
    expect(component.datePacs()).toBe('2021-06-01');
    expect(component.provenanceDatePacs()).toBe('IA');
    expect(component.dureeVieCommuneMois()).toBe(18);
    expect(component.provenanceDuree()).toBe('IA');
    expect(component.intensiteCommunauteVie()).toBe('MOYENNE');
    expect(component.provenanceIntensite()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      pacsDureeVieCommune: 5,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dureeVieCommuneMois: 24 }));
    expect(component.dureeVieCommuneMois()).toBe(24);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('onPacsConcluChange / onDureeChange / onIntensiteChange clear provenance', () => {
    component.aiData = {
      pacsConclu: true,
      pacsDureeVieCommune: 18,
      pacsIntensiteCommunauteVie: 'FORTE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenancePacsConclu()).toBe('IA');
    component.onPacsConcluChange(false);
    expect(component.provenancePacsConclu()).toBeNull();
    component.onDureeChange(30);
    expect(component.provenanceDuree()).toBeNull();
    component.onIntensiteChange('FAIBLE');
    expect(component.provenanceIntensite()).toBeNull();
  });

  it('bannerClass / bannerIcon cover the four verdict states', () => {
    expect(component.bannerClass(frResponse({ eligibilite: 'FAISCEAU_FAVORABLE' }))).toContain('pvp-banner--success');
    expect(component.bannerClass(frResponse({ eligibilite: 'FAISCEAU_INSUFFISANT' }))).toContain('pvp-banner--warning');
    expect(component.bannerClass(frResponse({ eligibilite: 'A_CONSOLIDER' }))).toContain('pvp-banner--warning');
    expect(component.bannerClass(frResponse({ eligibilite: 'NON_ELIGIBLE' }))).toContain('pvp-banner--neutral');
    expect(component.bannerIcon(frResponse({ eligibilite: 'FAISCEAU_FAVORABLE' }))).toBe('verified');
    expect(component.bannerIcon(frResponse({ eligibilite: 'FAISCEAU_INSUFFISANT' }))).toBe('report_problem');
    expect(component.bannerIcon(frResponse({ eligibilite: 'NON_ELIGIBLE' }))).toBe('cancel');
  });

  it('eligibiliteLabel maps the four codes to FR labels', () => {
    expect(component.eligibiliteLabel('FAISCEAU_FAVORABLE')).toContain('favorable');
    expect(component.eligibiliteLabel('FAISCEAU_INSUFFISANT')).toContain('insuffisant');
    expect(component.eligibiliteLabel('A_CONSOLIDER')).toContain('consolider');
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
    expect(component.dureeVieCommuneMois()).toBeNull();
    component.aiData = {
      pacsDureeVieCommune: 20,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeVieCommuneMois()).toBe(20);
    expect(component.provenanceDuree()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dureeVieCommuneMois: 24 }));
    component.aiData = {
      pacsDureeVieCommune: 12,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeVieCommuneMois()).toBe(24);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.pvp-banner--info');
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
