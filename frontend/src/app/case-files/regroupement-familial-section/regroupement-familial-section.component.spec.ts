import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { RegroupementFamilialSectionComponent } from './regroupement-familial-section.component';
import { RegroupementFamilialResponse } from '../../core/models/regroupement-familial.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('RegroupementFamilialSectionComponent', () => {
  let component: RegroupementFamilialSectionComponent;
  let fixture: ComponentFixture<RegroupementFamilialSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/regroupement-familial-analysis';

  function frResponse(overrides: Partial<RegroupementFamilialResponse> = {}): RegroupementFamilialResponse {
    return {
      caseFileId: 'case-1',
      dureeSejourRegulierMois: 24,
      ressourcesMensuellesNettes: 1800,
      tailleLogementM2: 60,
      nombrePersonnesFoyer: 2,
      typeRegroupement: 'CONJOINT',
      membresFamilleARegrouper: 2,
      country: 'FRANCE',
      verdict: 'ELIGIBLE',
      ressourcesRequises: 1766.92,
      surfaceRequise: 32,
      chipsCriteresNonRemplis: [],
      baseJuridique: 'CESEDA L.434-1 à L.434-10',
      ...overrides,
    };
  }

  function flush404(): void {
    const req = httpMock.expectOne(BASE_URL);
    req.flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dureeSejourRegulierMois.set(24);
    component.ressourcesMensuellesNettes.set(1800);
    component.tailleLogementM2.set(60);
    component.nombrePersonnesFoyer.set(2);
    component.typeRegroupement.set('CONJOINT');
    component.membresFamilleARegrouper.set(2);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [RegroupementFamilialSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(RegroupementFamilialSectionComponent);
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
    expect(RegroupementFamilialSectionComponent.TOOL_LABEL).toContain('L.434-1');
    expect(RegroupementFamilialSectionComponent.TOOL_ICON).toBe('family_restroom');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(RegroupementFamilialSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 when all 3 IA signals present', () => {
    expect(RegroupementFamilialSectionComponent.getPrefillCount({
      aiData: {
        aesDureePresenceMois: 24,
        regroupementRessourcesMensuelles: 1800,
        regroupementType: 'CONJOINT',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(RegroupementFamilialSectionComponent.getPrefillCount({
      aiData: { aesDureePresenceMois: 24 },
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
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.dureeSejourRegulierMois()).toBe(24);
    expect(component.ressourcesMensuellesNettes()).toBe(1800);
    expect(component.typeRegroupement()).toBe('CONJOINT');
    expect(component.membresFamilleARegrouper()).toBe(2);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false until all 6 fields valid', () => {
    expect(component.formValid()).toBe(false);
    fillValidForm();
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if membresFamilleARegrouper out of 1-6 range', () => {
    fillValidForm();
    component.membresFamilleARegrouper.set(0);
    expect(component.formValid()).toBe(false);
    component.membresFamilleARegrouper.set(7);
    expect(component.formValid()).toBe(false);
    component.membresFamilleARegrouper.set(6);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if logement / foyer / type missing', () => {
    fillValidForm();
    component.tailleLogementM2.set(0);
    expect(component.formValid()).toBe(false);
    component.tailleLogementM2.set(60);
    component.nombrePersonnesFoyer.set(0);
    expect(component.formValid()).toBe(false);
    component.nombrePersonnesFoyer.set(2);
    component.typeRegroupement.set(null);
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dureeSejourRegulierMois: 24,
      ressourcesMensuellesNettes: 1800,
      tailleLogementM2: 60,
      nombrePersonnesFoyer: 2,
      typeRegroupement: 'CONJOINT',
      membresFamilleARegrouper: 2,
    });
    req.flush(frResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.result()!.ressourcesRequises).toBe(1766.92);
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
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 3 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      aesDureePresenceMois: 30,
      regroupementRessourcesMensuelles: 2100,
      regroupementType: 'ENFANT_MINEUR',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dureeSejourRegulierMois()).toBe(30);
    expect(component.provenanceDuree()).toBe('IA');
    expect(component.ressourcesMensuellesNettes()).toBe(2100);
    expect(component.provenanceRessources()).toBe('IA');
    expect(component.typeRegroupement()).toBe('ENFANT_MINEUR');
    expect(component.provenanceType()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = {
      aesDureePresenceMois: 99,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dureeSejourRegulierMois: 24 }));
    expect(component.dureeSejourRegulierMois()).toBe(24);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('onDureeChange / onRessourcesChange / onTypeChange clear provenance', () => {
    component.aiData = {
      aesDureePresenceMois: 30,
      regroupementRessourcesMensuelles: 2100,
      regroupementType: 'CONJOINT',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDuree()).toBe('IA');
    component.onDureeChange(40);
    expect(component.provenanceDuree()).toBeNull();
    component.onRessourcesChange(2500);
    expect(component.provenanceRessources()).toBeNull();
    component.onTypeChange('AUTRE');
    expect(component.provenanceType()).toBeNull();
  });

  it('bannerClass / bannerIcon / verdictLabel cover all verdicts', () => {
    expect(component.bannerClass('ELIGIBLE')).toContain('rf-banner--success');
    expect(component.bannerClass('ELIGIBLE_SOUS_RESERVE')).toContain('rf-banner--warning');
    expect(component.bannerClass('NON_ELIGIBLE_DELAI')).toContain('rf-banner--danger');
    expect(component.bannerClass('NON_ELIGIBLE_RESSOURCES')).toContain('rf-banner--danger');
    expect(component.bannerClass('NON_ELIGIBLE_LOGEMENT')).toContain('rf-banner--danger');
    expect(component.bannerIcon('ELIGIBLE')).toBe('check_circle');
    expect(component.bannerIcon('NON_ELIGIBLE_RESSOURCES')).toBe('error');
    expect(component.verdictLabel('NON_ELIGIBLE_DELAI')).toContain('durée');
    expect(component.verdictLabel('NON_ELIGIBLE_RESSOURCES')).toContain('essources');
    expect(component.verdictLabel('NON_ELIGIBLE_LOGEMENT')).toContain('ogement');
  });

  it('chipLabelCritere maps known codes to FR labels', () => {
    expect(component.chipLabelCritere('DUREE_SEJOUR_INSUFFISANTE')).toContain('Durée');
    expect(component.chipLabelCritere('RESSOURCES_INSUFFISANTES')).toContain('Ressources');
    expect(component.chipLabelCritere('LOGEMENT_INSUFFISANT')).toContain('Surface');
    expect(component.chipLabelCritere('UNKNOWN_X')).toBe('UNKNOWN_X');
  });

  it('typeLabel maps each type code to FR label', () => {
    expect(component.typeLabel('CONJOINT')).toBe('Conjoint');
    expect(component.typeLabel('ENFANT_MINEUR')).toBe('Enfant mineur');
    expect(component.typeLabel('AUTRE')).toBe('Autre');
    expect(component.typeLabel(null)).toBe('');
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
    expect(component.dureeSejourRegulierMois()).toBeNull();
    component.aiData = {
      aesDureePresenceMois: 36,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeSejourRegulierMois()).toBe(36);
    expect(component.provenanceDuree()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dureeSejourRegulierMois: 24 }));
    component.aiData = {
      aesDureePresenceMois: 99,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeSejourRegulierMois()).toBe(24);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.rf-banner--info');
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
