import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { VpfLiensPersonnelsSectionComponent } from './vpf-liens-personnels-section.component';
import { VpfLiensPersonnelsResponse } from '../../core/models/vpf-liens-personnels.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('VpfLiensPersonnelsSectionComponent', () => {
  let component: VpfLiensPersonnelsSectionComponent;
  let fixture: ComponentFixture<VpfLiensPersonnelsSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/vpf-liens-personnels-analysis';

  function frResponse(overrides: Partial<VpfLiensPersonnelsResponse> = {}): VpfLiensPersonnelsResponse {
    return {
      caseFileId: 'case-1',
      dureeResidenceFranceMois: 120,
      entreeEnFranceMineur: true,
      enfantsEnFrance: true,
      conjointEnFrance: false,
      parentsEnFrance: false,
      situationFamilialeAlEtranger: null,
      niveauIntegration: 'FORT',
      ancienneConvictionPenale: false,
      country: 'FRANCE',
      verdict: 'ELIGIBLE_PROBABLE',
      score: 78,
      chipsCriteresNonRemplis: [],
      recommandations: ['Joindre justificatifs'],
      baseJuridique: 'CESEDA L.423-23',
      ...overrides,
    };
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.dureeResidenceFranceMois.set(120);
    component.niveauIntegration.set('FORT');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [VpfLiensPersonnelsSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(VpfLiensPersonnelsSectionComponent);
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
    expect(VpfLiensPersonnelsSectionComponent.TOOL_LABEL).toContain('L.423-23');
    expect(VpfLiensPersonnelsSectionComponent.TOOL_ICON).toBe('diversity_3');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(VpfLiensPersonnelsSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present', () => {
    expect(VpfLiensPersonnelsSectionComponent.getPrefillCount({
      aiData: {
        aesDureePresenceMois: 120,
        clientMineurDetecte: true,
        aesDureeScolaritePlusAncienEnfantAnnees: 5,
        vpfNiveauIntegration: 'FORT',
      },
      workspaceCountry: 'FRANCE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(VpfLiensPersonnelsSectionComponent.getPrefillCount({
      aiData: { aesDureePresenceMois: 120 },
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
    expect(component.dureeResidenceFranceMois()).toBe(120);
    expect(component.entreeEnFranceMineur()).toBe(true);
    expect(component.niveauIntegration()).toBe('FORT');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false until duree + niveauIntegration set', () => {
    expect(component.formValid()).toBe(false);
    component.dureeResidenceFranceMois.set(120);
    expect(component.formValid()).toBe(false);
    component.niveauIntegration.set('MOYEN');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false on negative duree', () => {
    fillValidForm();
    component.dureeResidenceFranceMois.set(-1);
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack + score', () => {
    component.ngOnInit();
    flush404();
    component.dureeResidenceFranceMois.set(120);
    component.entreeEnFranceMineur.set(true);
    component.enfantsEnFrance.set(true);
    component.niveauIntegration.set('FORT');
    component.situationFamilialeAlEtranger.set('  famille au pays  ');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dureeResidenceFranceMois: 120,
      entreeEnFranceMineur: true,
      enfantsEnFrance: true,
      conjointEnFrance: false,
      parentsEnFrance: false,
      situationFamilialeAlEtranger: 'famille au pays',
      niveauIntegration: 'FORT',
      ancienneConvictionPenale: false,
    });
    req.flush(frResponse());
    expect(component.result()!.score).toBe(78);
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() sends null situation when blank', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.situationFamilialeAlEtranger.set('   ');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.situationFamilialeAlEtranger).toBeNull();
    req.flush(frResponse());
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

  it('aiData with all 4 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      aesDureePresenceMois: 130,
      clientMineurDetecte: true,
      aesDureeScolaritePlusAncienEnfantAnnees: 4,
      vpfNiveauIntegration: 'MOYEN',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dureeResidenceFranceMois()).toBe(130);
    expect(component.provenanceDuree()).toBe('IA');
    expect(component.entreeEnFranceMineur()).toBe(true);
    expect(component.provenanceMineur()).toBe('IA');
    expect(component.enfantsEnFrance()).toBe(true);
    expect(component.provenanceEnfants()).toBe('IA');
    expect(component.niveauIntegration()).toBe('MOYEN');
    expect(component.provenanceNiveau()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { aesDureePresenceMois: 999 } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dureeResidenceFranceMois: 120 }));
    expect(component.dureeResidenceFranceMois()).toBe(120);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('onDureeChange / onMineurChange / onEnfantsChange / onNiveauChange clear provenance', () => {
    component.aiData = {
      aesDureePresenceMois: 130,
      clientMineurDetecte: true,
      aesDureeScolaritePlusAncienEnfantAnnees: 4,
      vpfNiveauIntegration: 'FORT',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDuree()).toBe('IA');
    component.onDureeChange(140);
    expect(component.provenanceDuree()).toBeNull();
    component.onMineurChange(false);
    expect(component.provenanceMineur()).toBeNull();
    component.onEnfantsChange(false);
    expect(component.provenanceEnfants()).toBeNull();
    component.onNiveauChange('FAIBLE');
    expect(component.provenanceNiveau()).toBeNull();
  });

  it('bannerClass / bannerIcon / verdictLabel cover all verdicts', () => {
    expect(component.bannerClass('ELIGIBLE_PROBABLE')).toContain('vpf-banner--success');
    expect(component.bannerClass('ELIGIBLE_SOUS_RESERVE')).toContain('vpf-banner--warning');
    expect(component.bannerClass('DOSSIER_A_CONSOLIDER')).toContain('vpf-banner--warning');
    expect(component.bannerClass('NON_ELIGIBLE')).toContain('vpf-banner--danger');
    expect(component.bannerIcon('ELIGIBLE_PROBABLE')).toBe('check_circle');
    expect(component.bannerIcon('NON_ELIGIBLE')).toBe('error');
    expect(component.bannerIcon('DOSSIER_A_CONSOLIDER')).toBe('warning');
    expect(component.verdictLabel('ELIGIBLE_PROBABLE')).toContain('probable');
    expect(component.verdictLabel('NON_ELIGIBLE')).toContain('Non éligible');
    expect(component.verdictLabel('DOSSIER_A_CONSOLIDER')).toContain('consolider');
  });

  it('scoreBarColor maps verdicts to material colors', () => {
    expect(component.scoreBarColor('ELIGIBLE_PROBABLE')).toBe('primary');
    expect(component.scoreBarColor('NON_ELIGIBLE')).toBe('warn');
    expect(component.scoreBarColor('ELIGIBLE_SOUS_RESERVE')).toBe('accent');
    expect(component.scoreBarColor('DOSSIER_A_CONSOLIDER')).toBe('accent');
  });

  it('niveauLabel maps each code to FR label', () => {
    expect(component.niveauLabel('FORT')).toBe('Fort');
    expect(component.niveauLabel('MOYEN')).toBe('Moyen');
    expect(component.niveauLabel('FAIBLE')).toBe('Faible');
    expect(component.niveauLabel(null)).toBe('');
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
    expect(component.dureeResidenceFranceMois()).toBeNull();
    component.aiData = { aesDureePresenceMois: 96 } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeResidenceFranceMois()).toBe(96);
    expect(component.provenanceDuree()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(frResponse({ dureeResidenceFranceMois: 120 }));
    component.aiData = { aesDureePresenceMois: 999 } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dureeResidenceFranceMois()).toBe(120);
    expect(component.provenanceDuree()).toBeNull();
  });

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.vpf-banner--info');
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

  it('result view renders score progress bar and recommandations', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(frResponse({ score: 65, recommandations: ['Reco A', 'Reco B'] }));
    fixture.detectChanges();
    const bar = fixture.nativeElement.querySelector('mat-progress-bar');
    expect(bar).not.toBeNull();
    const recos = fixture.nativeElement.querySelectorAll('.vpf-reco-list li');
    expect(recos.length).toBe(2);
  });
});
