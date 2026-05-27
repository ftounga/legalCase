import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { Regroupement10terBeSectionComponent } from './regroupement-10ter-be-section.component';
import { Regroupement10terBeResponse } from '../../core/models/regroupement-10ter-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('Regroupement10terBeSectionComponent', () => {
  let component: Regroupement10terBeSectionComponent;
  let fixture: ComponentFixture<Regroupement10terBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/regroupement-10ter-be-analysis';

  function beResponse(
    overrides: Partial<Regroupement10terBeResponse> = {},
  ): Regroupement10terBeResponse {
    return {
      caseFileId: 'case-1',
      lienFamilial: 'CONJOINT',
      typeCarteRegroupant: 'CARTE_B',
      revenusMensuelsNetsRegroupant: 1950,
      dureeSejour: 24,
      logementConforme: true,
      assuranceMaladie: true,
      menaceOrdrePublic: false,
      seuilRessources: 1500,
      differentielRevenus: 450,
      scoreEligibilite: 85,
      verdict: 'ELIGIBLE',
      criteresNonRemplis: [],
      baseJuridique:
        'Loi 15/12/1980 art. 10ter ; AR 17/05/2007 ; AR exécution Loi 15/12/1980',
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
      imports: [Regroupement10terBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(Regroupement10terBeSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'BELGIQUE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match((r) => r.url.includes('/jurisprudence-citations')).forEach((r) => r.flush({ items: [] }));
    httpMock.verify();
  });

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(Regroupement10terBeSectionComponent.TOOL_LABEL).toContain('REGROUPEMENT 10TER');
    expect(Regroupement10terBeSectionComponent.TOOL_ICON).toBe('family_restroom');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(Regroupement10terBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 4 when all 4 IA signals present (BELGIQUE)', () => {
    expect(Regroupement10terBeSectionComponent.getPrefillCount({
      aiData: {
        be10terLienFamilial: 'CONJOINT',
        be10terTypeCarte: 'CARTE_B',
        be10terRevenusMensuels: 1950,
        be10terDureeSejour: 24,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(4);
  });

  it('static getPrefillCount returns 2 on partial pré-fill', () => {
    expect(Regroupement10terBeSectionComponent.getPrefillCount({
      aiData: {
        be10terLienFamilial: 'CONJOINT',
        be10terDureeSejour: 12,
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(Regroupement10terBeSectionComponent.getPrefillCount({
      aiData: { be10terLienFamilial: 'CONJOINT' },
      workspaceCountry: 'FRANCE',
    })).toBe(0);
  });

  it('BELGIQUE -> GET called on ngOnInit', () => {
    expect(component.isBelgique()).toBe(true);
    component.ngOnInit();
    const req = httpMock.expectOne(BASE_URL);
    expect(req.request.method).toBe('GET');
    req.flush({ message: 'NF' }, { status: 404, statusText: 'NF' });
  });

  it('FRANCE -> no HTTP on ngOnInit', () => {
    component.workspaceCountry = 'FRANCE';
    component.ngOnInit();
    httpMock.expectNone(BASE_URL);
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(component.lienFamilial()).toBe('CONJOINT');
    expect(component.typeCarteRegroupant()).toBe('CARTE_B');
    expect(component.revenusMensuelsNetsRegroupant()).toBe(1950);
    expect(component.dureeSejour()).toBe(24);
    expect(component.logementConforme()).toBe(true);
    expect(component.assuranceMaladie()).toBe(true);
    expect(component.menaceOrdrePublic()).toBe(false);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false if any required field is missing', () => {
    expect(component.formValid()).toBe(false);
    component.lienFamilial.set('CONJOINT');
    expect(component.formValid()).toBe(false);
    component.typeCarteRegroupant.set('CARTE_B');
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsRegroupant.set(1950);
    expect(component.formValid()).toBe(false);
    component.dureeSejour.set(24);
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if revenus or dureeSejour out of bounds', () => {
    component.lienFamilial.set('CONJOINT');
    component.typeCarteRegroupant.set('CARTE_B');
    component.revenusMensuelsNetsRegroupant.set(150_000);
    component.dureeSejour.set(24);
    expect(component.formValid()).toBe(false);
    component.revenusMensuelsNetsRegroupant.set(1950);
    component.dureeSejour.set(1000);
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack + dashboard refresh', () => {
    component.ngOnInit();
    flush404();
    component.lienFamilial.set('CONJOINT');
    component.typeCarteRegroupant.set('CARTE_B');
    component.revenusMensuelsNetsRegroupant.set(1950);
    component.dureeSejour.set(24);
    component.logementConforme.set(true);
    component.assuranceMaladie.set(true);
    component.menaceOrdrePublic.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      lienFamilial: 'CONJOINT',
      typeCarteRegroupant: 'CARTE_B',
      revenusMensuelsNetsRegroupant: 1950,
      dureeSejour: 24,
      logementConforme: true,
      assuranceMaladie: true,
      menaceOrdrePublic: false,
    });
    req.flush(beResponse());
    expect(component.result()!.verdict).toBe('ELIGIBLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.lienFamilial.set('PARTENAIRE_ENREGISTRE');
    component.typeCarteRegroupant.set('CARTE_C');
    component.revenusMensuelsNetsRegroupant.set(1200);
    component.dureeSejour.set(6);
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
      be10terLienFamilial: 'PARTENAIRE_ENREGISTRE',
      be10terTypeCarte: 'CARTE_C',
      be10terRevenusMensuels: 2200,
      be10terDureeSejour: 48,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.lienFamilial()).toBe('PARTENAIRE_ENREGISTRE');
    expect(component.provenanceLienFamilial()).toBe('IA');
    expect(component.typeCarteRegroupant()).toBe('CARTE_C');
    expect(component.provenanceTypeCarte()).toBe('IA');
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2200);
    expect(component.provenanceRevenus()).toBe('IA');
    expect(component.dureeSejour()).toBe(48);
    expect(component.provenanceDureeSejour()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (server values win)', () => {
    component.aiData = {
      be10terLienFamilial: 'ASCENDANT_CHARGE',
      be10terRevenusMensuels: 9999,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ revenusMensuelsNetsRegroupant: 2000 }));
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2000);
    expect(component.provenanceRevenus()).toBeNull();
  });

  it('onRevenusChange clears provenance', () => {
    component.aiData = {
      be10terRevenusMensuels: 1950,
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceRevenus()).toBe('IA');
    component.onRevenusChange(2200);
    expect(component.provenanceRevenus()).toBeNull();
  });

  it('onLienFamilialChange clears provenance', () => {
    component.aiData = {
      be10terLienFamilial: 'CONJOINT',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceLienFamilial()).toBe('IA');
    component.onLienFamilialChange('ENFANT_MOINS_21');
    expect(component.provenanceLienFamilial()).toBeNull();
  });

  it('verdictClass covers all 3 verdict colors', () => {
    expect(component.verdictClass('ELIGIBLE')).toContain('re10ter-banner--success');
    expect(component.verdictClass('SOUS_RESERVE')).toContain('re10ter-banner--warning');
    expect(component.verdictClass('INELIGIBLE')).toContain('re10ter-banner--danger');
  });

  it('verdictChipClass covers all 3 verdicts', () => {
    expect(component.verdictChipClass('ELIGIBLE')).toContain('re10ter-chip--success');
    expect(component.verdictChipClass('SOUS_RESERVE')).toContain('re10ter-chip--warning');
    expect(component.verdictChipClass('INELIGIBLE')).toContain('re10ter-chip--danger');
  });

  it('verdictIcon covers all 3 verdicts', () => {
    expect(component.verdictIcon('ELIGIBLE')).toBe('check_circle');
    expect(component.verdictIcon('SOUS_RESERVE')).toBe('warning');
    expect(component.verdictIcon('INELIGIBLE')).toBe('block');
  });

  it('verdictLabel covers all 3 verdicts', () => {
    expect(component.verdictLabel('ELIGIBLE')).toContain('Éligible');
    expect(component.verdictLabel('SOUS_RESERVE')).toContain('réserve');
    expect(component.verdictLabel('INELIGIBLE')).toContain('Inéligible');
  });

  it('scoreBarColor maps verdicts to Material colors', () => {
    expect(component.scoreBarColor('ELIGIBLE')).toBe('primary');
    expect(component.scoreBarColor('SOUS_RESERVE')).toBe('accent');
    expect(component.scoreBarColor('INELIGIBLE')).toBe('warn');
  });

  it('differentielText is signed: positive (+) and negative', () => {
    expect(component.differentielText(450)).toContain('+450');
    expect(component.differentielText(450)).toContain('au-dessus');
    expect(component.differentielText(-200)).toContain('-200');
    expect(component.differentielText(-200)).toContain('en-dessous');
    expect(component.differentielText(0)).toContain('+0');
  });

  it('differentielBadgeClass is positive/negative coloured', () => {
    expect(component.differentielBadgeClass(450)).toContain('re10ter-diff--positive');
    expect(component.differentielBadgeClass(0)).toContain('re10ter-diff--positive');
    expect(component.differentielBadgeClass(-200)).toContain('re10ter-diff--negative');
    expect(component.differentielBadgeClass(null)).toBe('re10ter-diff');
  });

  it('lienFamilialLabel resolves the 5 codes', () => {
    expect(component.lienFamilialLabel('CONJOINT')).toBe('Conjoint');
    expect(component.lienFamilialLabel('PARTENAIRE_ENREGISTRE')).toContain('Partenaire');
    expect(component.lienFamilialLabel('ENFANT_MOINS_21')).toContain('21');
    expect(component.lienFamilialLabel('ENFANT_21_PLUS_CHARGE')).toContain('majeur');
    expect(component.lienFamilialLabel('ASCENDANT_CHARGE')).toContain('Ascendant');
    expect(component.lienFamilialLabel(null)).toBe('');
  });

  it('typeCarteLabel resolves the 2 codes', () => {
    expect(component.typeCarteLabel('CARTE_B')).toContain('Carte B');
    expect(component.typeCarteLabel('CARTE_C')).toContain('Carte C');
    expect(component.typeCarteLabel(null)).toBe('');
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
    expect(component.lienFamilial()).toBeNull();
    component.aiData = {
      be10terLienFamilial: 'ENFANT_MOINS_21',
      be10terRevenusMensuels: 2400,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.lienFamilial()).toBe('ENFANT_MOINS_21');
    expect(component.provenanceLienFamilial()).toBe('IA');
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2400);
    expect(component.provenanceRevenus()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ revenusMensuelsNetsRegroupant: 2000 }));
    component.aiData = {
      be10terRevenusMensuels: 9999,
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.revenusMensuelsNetsRegroupant()).toBe(2000);
    expect(component.provenanceRevenus()).toBeNull();
  });

  it('FRANCE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.re10ter-banner--info');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Belgique uniquement');
  });

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });

  it('renders verdict chip in result header (INELIGIBLE)', () => {
    component.standaloneMode = true; // skip auto-GET
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ verdict: 'INELIGIBLE', scoreEligibilite: 25 }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.re10ter-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('re10ter-chip--danger')).toBe(true);
  });

  it('renders criteresNonRemplis list when result is SOUS_RESERVE', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({
      verdict: 'SOUS_RESERVE',
      scoreEligibilite: 60,
      differentielRevenus: -150,
      criteresNonRemplis: ['Revenus insuffisants', 'Logement non documenté'],
    }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const items = fixture.nativeElement.querySelectorAll('.re10ter-criteres-list mat-list-item');
    expect(items.length).toBe(2);
  });
});
