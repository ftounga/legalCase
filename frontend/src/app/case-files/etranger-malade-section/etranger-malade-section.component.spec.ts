import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { EtrangerMaladeSectionComponent } from './etranger-malade-section.component';
import { EtrangerMaladeResponse } from '../../core/models/etranger-malade.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('EtrangerMaladeSectionComponent', () => {
  let component: EtrangerMaladeSectionComponent;
  let fixture: ComponentFixture<EtrangerMaladeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/etranger-malade-analysis';

  function frResponse(overrides: Partial<EtrangerMaladeResponse> = {}): EtrangerMaladeResponse {
    return {
      caseFileId: 'case-1',
      pathologiePrincipale: 'Cancer hépatique stade 4',
      paysOrigine: 'Comores',
      traitementDisponiblePaysOrigine: false,
      avisOFII: 'FAVORABLE',
      dateAvisOFII: '2026-04-15',
      country: 'FRANCE',
      verdict: 'ELIGIBLE_PROBABLE',
      delaiRecoursTA: null,
      motifRecours: null,
      chipsCriteresNonRemplis: [],
      baseJuridique: 'CESEDA L.425-9 ; R.425-9 à R.425-16 ; CE 7 avril 2010 n° 301640',
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
      imports: [EtrangerMaladeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(EtrangerMaladeSectionComponent);
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
    expect(EtrangerMaladeSectionComponent.TOOL_LABEL).toContain('L.425-9');
    expect(EtrangerMaladeSectionComponent.TOOL_ICON).toBe('medical_services');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(EtrangerMaladeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present', () => {
    expect(EtrangerMaladeSectionComponent.getPrefillCount({
      aiData: {
        etrangerMaladePathologie: 'VIH stade C',
        etrangerMaladeTraitementDisponible: false,
        etrangerMaladeAvisOFII: 'DEFAVORABLE',
        etrangerMalaDateAvisOFII: '2026-04-01',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(EtrangerMaladeSectionComponent.getPrefillCount({
      aiData: { etrangerMaladePathologie: 'Diabète' },
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
    expect(component.result()!.verdict).toBe('ELIGIBLE_PROBABLE');
    expect(component.showForm()).toBe(false);
    expect(component.pathologiePrincipale()).toBe('Cancer hépatique stade 4');
    expect(component.paysOrigine()).toBe('Comores');
    expect(component.avisOFIIRendu()).toBe(true);
    expect(component.avisOFII()).toBe('FAVORABLE');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false if pathologie or paysOrigine missing', () => {
    component.pathologiePrincipale.set('');
    component.paysOrigine.set('Comores');
    expect(component.formValid()).toBe(false);
    component.pathologiePrincipale.set('VIH');
    component.paysOrigine.set('');
    expect(component.formValid()).toBe(false);
    component.paysOrigine.set('Comores');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : avisOFIIRendu=true requires avisOFII set', () => {
    component.pathologiePrincipale.set('VIH');
    component.paysOrigine.set('Comores');
    component.avisOFIIRendu.set(true);
    component.avisOFII.set(null);
    expect(component.formValid()).toBe(false);
    component.avisOFII.set('FAVORABLE');
    expect(component.formValid()).toBe(true);
  });

  it('formValid : avisOFII=DEFAVORABLE requires dateAvisOFII', () => {
    component.pathologiePrincipale.set('VIH');
    component.paysOrigine.set('Comores');
    component.avisOFIIRendu.set(true);
    component.avisOFII.set('DEFAVORABLE');
    component.dateAvisOFII.set(null);
    expect(component.formValid()).toBe(false);
    component.dateAvisOFII.set('2026-04-15');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if dateAvisOFII in future', () => {
    component.pathologiePrincipale.set('VIH');
    component.paysOrigine.set('Comores');
    component.avisOFIIRendu.set(true);
    component.avisOFII.set('FAVORABLE');
    const future = new Date();
    future.setDate(future.getDate() + 10);
    component.dateAvisOFII.set(future.toISOString().slice(0, 10));
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack + dashboard refresh', () => {
    component.ngOnInit();
    flush404();
    component.pathologiePrincipale.set('Cancer hépatique');
    component.paysOrigine.set('Comores');
    component.traitementDisponiblePaysOrigine.set(false);
    component.avisOFIIRendu.set(true);
    component.avisOFII.set('FAVORABLE');
    component.dateAvisOFII.set('2026-04-15');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      pathologiePrincipale: 'Cancer hépatique',
      paysOrigine: 'Comores',
      traitementDisponiblePaysOrigine: false,
      avisOFIIRendu: true,
      avisOFII: 'FAVORABLE',
      dateAvisOFII: '2026-04-15',
    });
    req.flush(frResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE_PROBABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() avisOFIIRendu=false -> avisOFII + dateAvisOFII null in body', () => {
    component.ngOnInit();
    flush404();
    component.pathologiePrincipale.set('VIH');
    component.paysOrigine.set('Algérie');
    component.avisOFIIRendu.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.avisOFIIRendu).toBe(false);
    expect(req.request.body.avisOFII).toBeNull();
    expect(req.request.body.dateAvisOFII).toBeNull();
    req.flush(frResponse({ verdict: 'EN_ATTENTE_AVIS_OFII' }));
    expect(component.result()!.verdict).toBe('EN_ATTENTE_AVIS_OFII');
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.pathologiePrincipale.set('VIH');
    component.paysOrigine.set('Comores');
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
      etrangerMaladePathologie: 'VIH stade C',
      etrangerMaladeTraitementDisponible: true,
      etrangerMaladeAvisOFII: 'DEFAVORABLE',
      etrangerMalaDateAvisOFII: '2026-04-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.pathologiePrincipale()).toBe('VIH stade C');
    expect(component.provenancePathologie()).toBe('IA');
    expect(component.traitementDisponiblePaysOrigine()).toBe(true);
    expect(component.provenanceTraitement()).toBe('IA');
    expect(component.avisOFII()).toBe('DEFAVORABLE');
    expect(component.avisOFIIRendu()).toBe(true);
    expect(component.provenanceAvisOFII()).toBe('IA');
    expect(component.dateAvisOFII()).toBe('2026-04-01');
    expect(component.provenanceDateAvisOFII()).toBe('IA');
  });

  it('GET 200 -> no pre-fill', () => {
    component.aiData = {
      etrangerMaladePathologie: 'VIH ignored',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ pathologiePrincipale: 'From backend' }));
    expect(component.pathologiePrincipale()).toBe('From backend');
    expect(component.provenancePathologie()).toBeNull();
  });

  it('onPathologieChange clears provenance', () => {
    component.aiData = {
      etrangerMaladePathologie: 'VIH',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenancePathologie()).toBe('IA');
    component.onPathologieChange('Cancer');
    expect(component.provenancePathologie()).toBeNull();
  });

  it('onAvisOFIIRenduChange(false) resets avis + date + provenance', () => {
    component.aiData = {
      etrangerMaladeAvisOFII: 'FAVORABLE',
      etrangerMalaDateAvisOFII: '2026-04-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.avisOFII()).toBe('FAVORABLE');
    component.onAvisOFIIRenduChange(false);
    expect(component.avisOFII()).toBeNull();
    expect(component.dateAvisOFII()).toBeNull();
    expect(component.provenanceAvisOFII()).toBeNull();
    expect(component.provenanceDateAvisOFII()).toBeNull();
  });

  it('bannerClass / bannerIcon / verdictLabel cover all verdicts', () => {
    expect(component.bannerClass('ELIGIBLE_PROBABLE')).toContain('em-banner--success');
    expect(component.bannerClass('ELIGIBLE_SOUS_RESERVE')).toContain('em-banner--warning');
    expect(component.bannerClass('NON_ELIGIBLE')).toContain('em-banner--danger');
    expect(component.bannerClass('EN_ATTENTE_AVIS_OFII')).toContain('em-banner--info');
    expect(component.bannerIcon('ELIGIBLE_PROBABLE')).toBe('check_circle');
    expect(component.bannerIcon('NON_ELIGIBLE')).toBe('error');
    expect(component.verdictLabel('NON_ELIGIBLE')).toContain('Non éligible');
    expect(component.verdictLabel('EN_ATTENTE_AVIS_OFII')).toContain('OFII');
  });

  it('chipLabelCritere maps known codes to FR labels', () => {
    expect(component.chipLabelCritere('PATHOLOGIE_NON_RENSEIGNEE')).toContain('Pathologie');
    expect(component.chipLabelCritere('TRAITEMENT_DISPONIBLE_PAYS_ORIGINE')).toContain('Traitement');
    expect(component.chipLabelCritere('AVIS_OFII_DEFAVORABLE')).toContain('défavorable');
    // Unknown code -> echo
    expect(component.chipLabelCritere('UNKNOWN_X')).toBe('UNKNOWN_X');
  });

  it('showDelaiRecoursTA true only when result has delaiRecoursTA', () => {
    expect(component.showDelaiRecoursTA(null)).toBe(false);
    expect(component.showDelaiRecoursTA(frResponse({ delaiRecoursTA: null }))).toBe(false);
    expect(component.showDelaiRecoursTA(frResponse({ delaiRecoursTA: '2026-06-15' }))).toBe(true);
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
    expect(component.pathologiePrincipale()).toBe('');
    component.aiData = {
      etrangerMaladePathologie: 'Hépatite C',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.pathologiePrincipale()).toBe('Hépatite C');
    expect(component.provenancePathologie()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ pathologiePrincipale: 'From backend' }));
    component.aiData = {
      etrangerMaladePathologie: 'New IA value',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.pathologiePrincipale()).toBe('From backend');
    expect(component.provenancePathologie()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.em-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });
});
