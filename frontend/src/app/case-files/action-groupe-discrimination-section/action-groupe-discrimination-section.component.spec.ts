import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { ActionGroupeDiscriminationSectionComponent } from './action-groupe-discrimination-section.component';
import { ActionGroupeDiscriminationResponse } from '../../core/models/action-groupe-discrimination.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('ActionGroupeDiscriminationSectionComponent', () => {
  let component: ActionGroupeDiscriminationSectionComponent;
  let fixture: ComponentFixture<ActionGroupeDiscriminationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/action-groupe-discrimination-analysis';

  function recevableResponse(overrides: Partial<ActionGroupeDiscriminationResponse> = {}): ActionGroupeDiscriminationResponse {
    return {
      caseFileId: 'case-1',
      typeOrganisation: 'SYNDICAT_REPRESENTATIF',
      dateMiseEnDemeure: '2025-06-01',
      motifDiscrimination: 'SEXE',
      nombrePersonnesConcernees: 5,
      objetAction: 'LES_DEUX',
      verdict: 'RECEVABLE',
      qualiteAAgir: true,
      pluraliteEtablie: true,
      dateRecevabiliteSaisine: '2025-12-01',
      delaiCarenceRespecte: true,
      checklist: [
        { libelle: 'Mise en demeure écrite de l\'employeur', obligatoire: true, bloquant: false, baseJuridique: 'L. 1134-9' },
        { libelle: 'Délai de 6 mois écoulé', obligatoire: true, bloquant: false, baseJuridique: 'L. 1134-9' },
        { libelle: 'Identité de situation', obligatoire: true, bloquant: false, baseJuridique: 'L. 1134-7' },
      ],
      country: 'FRANCE',
      baseJuridique: 'L. 1134-7 à L. 1134-10 ; L. 1132-1 Code travail',
      ...overrides,
    };
  }

  function prematureResponse(): ActionGroupeDiscriminationResponse {
    return recevableResponse({
      dateMiseEnDemeure: '2026-05-01',
      verdict: 'PREMATURE',
      dateRecevabiliteSaisine: '2026-11-01',
      delaiCarenceRespecte: false,
    });
  }

  function irrecevableQualiteResponse(): ActionGroupeDiscriminationResponse {
    return recevableResponse({
      typeOrganisation: 'AUTRE',
      verdict: 'IRRECEVABLE_QUALITE',
      qualiteAAgir: false,
    });
  }

  function infoManquanteResponse(): ActionGroupeDiscriminationResponse {
    return recevableResponse({
      dateMiseEnDemeure: null,
      verdict: 'INFO_MANQUANTE',
      dateRecevabiliteSaisine: null,
      delaiCarenceRespecte: false,
      checklist: [
        { libelle: 'Mise en demeure écrite de l\'employeur', obligatoire: true, bloquant: true, baseJuridique: 'L. 1134-9' },
      ],
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.typeOrganisation.set('SYNDICAT_REPRESENTATIF');
    component.motifDiscrimination.set('SEXE');
    component.dateMiseEnDemeure.set('2025-06-01');
    component.nombrePersonnesConcernees.set(5);
    component.objetAction.set('LES_DEUX');
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [ActionGroupeDiscriminationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(ActionGroupeDiscriminationSectionComponent);
    component = fixture.componentInstance;
    component.caseFileId = 'case-1';
    component.workspaceCountry = 'FRANCE';
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.match(r => r.url.includes('/jurisprudence-citations')).forEach(r => r.flush({ items: [] }));
    httpMock.verify();
  });

  // --- statics / contract ---

  it('exposes TOOL_LABEL and TOOL_ICON statics', () => {
    expect(ActionGroupeDiscriminationSectionComponent.TOOL_LABEL).toContain('ACTION DE GROUPE');
    expect(ActionGroupeDiscriminationSectionComponent.TOOL_ICON).toBe('groups');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(ActionGroupeDiscriminationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 (partiel) with only motif', () => {
    expect(ActionGroupeDiscriminationSectionComponent.getPrefillCount({
      aiData: { motifDiscrimination: 'HANDICAP' },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) with motif + date', () => {
    expect(ActionGroupeDiscriminationSectionComponent.getPrefillCount({
      aiData: { motifDiscrimination: 'ORIGINE', dateMiseEnDemeureDiscrimination: '2025-06-01' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(ActionGroupeDiscriminationSectionComponent.getPrefillCount({
      aiData: { motifDiscrimination: 'ORIGINE', dateMiseEnDemeureDiscrimination: '2025-06-01' },
      workspaceCountry: 'BELGIQUE',
    })).toBe(0);
  });

  // --- gate pays / lifecycle ---

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

  it('BELGIQUE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('française uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(recevableResponse());
    expect(component.result()!.verdict).toBe('RECEVABLE');
    expect(component.showForm()).toBe(false);
    expect(component.typeOrganisation()).toBe('SYNDICAT_REPRESENTATIF');
    expect(component.motifDiscrimination()).toBe('SEXE');
    expect(component.nombrePersonnesConcernees()).toBe(5);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validation ---

  it('formValid requires type, motif and nombrePersonnes >= 1', () => {
    component.typeOrganisation.set(null);
    expect(component.formValid()).toBe(false);
    component.typeOrganisation.set('SYNDICAT_REPRESENTATIF');
    component.motifDiscrimination.set(null);
    expect(component.formValid()).toBe(false);
    component.motifDiscrimination.set('SEXE');
    component.nombrePersonnesConcernees.set(0);
    expect(component.formValid()).toBe(false);
    component.nombrePersonnesConcernees.set(3);
    expect(component.formValid()).toBe(true);
  });

  it('onNombrePersonnesChange floors negatives and truncates decimals', () => {
    component.onNombrePersonnesChange(-2);
    expect(component.nombrePersonnesConcernees()).toBe(0);
    component.onNombrePersonnesChange(4.8);
    expect(component.nombrePersonnesConcernees()).toBe(4);
  });

  it('onDateMiseEnDemeureChange normalizes empty string to null', () => {
    component.onDateMiseEnDemeureChange('');
    expect(component.dateMiseEnDemeure()).toBeNull();
    component.onDateMiseEnDemeureChange('2025-06-01');
    expect(component.dateMiseEnDemeure()).toBe('2025-06-01');
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + exact body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      typeOrganisation: 'SYNDICAT_REPRESENTATIF',
      dateMiseEnDemeure: '2025-06-01',
      motifDiscrimination: 'SEXE',
      nombrePersonnesConcernees: 5,
      objetAction: 'LES_DEUX',
    });
    req.flush(recevableResponse());
    expect(component.result()!.verdict).toBe('RECEVABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() sends dateMiseEnDemeure=null when absent', () => {
    component.ngOnInit();
    flush404();
    component.typeOrganisation.set('SYNDICAT_REPRESENTATIF');
    component.motifDiscrimination.set('SEXE');
    component.nombrePersonnesConcernees.set(3);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.dateMiseEnDemeure).toBeNull();
    req.flush(infoManquanteResponse());
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.typeOrganisation.set(null);
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

  // --- result rendering : verdicts + chip + checklist + date ---

  it('verdict RECEVABLE -> success chip + recevabilité date + checklist rows', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(recevableResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.agd-chip--success');
    expect(chip).not.toBeNull();
    const date = fixture.nativeElement.querySelector('[data-testid="date-recevabilite"]');
    expect(date.textContent).toContain('2025-12-01');
    const carence = fixture.nativeElement.querySelector('[data-testid="delai-carence"]');
    expect(carence.textContent).toContain('Oui');
    const block = fixture.nativeElement.querySelector('[data-testid="checklist-block"]');
    expect(block).not.toBeNull();
    const rows = fixture.nativeElement.querySelectorAll('[data-testid="checklist-item"]');
    expect(rows.length).toBe(3);
  });

  it('verdict PREMATURE -> warning chip + delaiCarenceRespecte=false', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(prematureResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.agd-chip--warning');
    expect(chip).not.toBeNull();
    const carence = fixture.nativeElement.querySelector('[data-testid="delai-carence"]');
    expect(carence.textContent).toContain('Non');
  });

  it('verdict IRRECEVABLE_QUALITE -> danger chip', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.typeOrganisation.set('AUTRE');
    component.motifDiscrimination.set('SEXE');
    component.nombrePersonnesConcernees.set(5);
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(irrecevableQualiteResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.agd-chip--danger');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('non habilitée');
  });

  it('verdict INFO_MANQUANTE -> neutral chip + bloquant checklist item', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.typeOrganisation.set('SYNDICAT_REPRESENTATIF');
    component.motifDiscrimination.set('SEXE');
    component.nombrePersonnesConcernees.set(5);
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(infoManquanteResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.agd-chip--neutral');
    expect(chip).not.toBeNull();
    const bloquant = fixture.nativeElement.querySelector('.agd-checklist-item--bloquant');
    expect(bloquant).not.toBeNull();
  });

  // --- prefill IA ---

  it('aiData -> pre-fills motif + date with provenance IA', () => {
    component.aiData = { motifDiscrimination: 'ORIGINE', dateMiseEnDemeureDiscrimination: '2025-06-01' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.motifDiscrimination()).toBe('ORIGINE');
    expect(component.provenanceMotifDiscrimination()).toBe('IA');
    expect(component.dateMiseEnDemeure()).toBe('2025-06-01');
    expect(component.provenanceDateMiseEnDemeure()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { motifDiscrimination: 'AGE', dateMiseEnDemeureDiscrimination: '2024-01-01' } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(recevableResponse({ motifDiscrimination: 'SEXE', dateMiseEnDemeure: '2025-06-01' }));
    expect(component.motifDiscrimination()).toBe('SEXE');
    expect(component.provenanceMotifDiscrimination()).toBeNull();
  });

  it('onMotifChange clears motif provenance', () => {
    component.aiData = { motifDiscrimination: 'ORIGINE' } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceMotifDiscrimination()).toBe('IA');
    component.onMotifChange('SEXE');
    expect(component.provenanceMotifDiscrimination()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.motifDiscrimination()).toBeNull();
    component.aiData = { motifDiscrimination: 'HANDICAP', dateMiseEnDemeureDiscrimination: '2025-07-01' } as TravailExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.motifDiscrimination()).toBe('HANDICAP');
    expect(component.dateMiseEnDemeure()).toBe('2025-07-01');
  });

  // --- F-IA-03 coherence ---

  it('coherenceAlerts warns when mise en demeure is missing', () => {
    component.dateMiseEnDemeure.set(null);
    expect(component.coherenceAlerts().some((a) => a.includes('mise en demeure'))).toBe(true);
  });

  it('coherenceAlerts warns when pluralité < 2', () => {
    component.dateMiseEnDemeure.set('2025-06-01');
    component.nombrePersonnesConcernees.set(1);
    expect(component.coherenceAlerts().some((a) => a.includes('pluralité'))).toBe(true);
  });

  // --- helpers / labels ---

  it('verdictLabel / chipClass / bannerClass / bannerIcon cover all 4 verdicts', () => {
    expect(component.verdictLabel('RECEVABLE')).toContain('recevable');
    expect(component.verdictLabel('PREMATURE')).toContain('prématurée');
    expect(component.verdictLabel('IRRECEVABLE_QUALITE')).toContain('non habilitée');
    expect(component.verdictLabel('INFO_MANQUANTE')).toContain('manquante');
    expect(component.chipClass('RECEVABLE')).toContain('agd-chip--success');
    expect(component.chipClass('PREMATURE')).toContain('agd-chip--warning');
    expect(component.chipClass('IRRECEVABLE_QUALITE')).toContain('agd-chip--danger');
    expect(component.chipClass('INFO_MANQUANTE')).toContain('agd-chip--neutral');
    expect(component.bannerClass('RECEVABLE')).toContain('agd-banner--success');
    expect(component.bannerIcon('RECEVABLE')).toBe('verified');
    expect(component.bannerIcon('PREMATURE')).toBe('hourglass_top');
    expect(component.bannerIcon('IRRECEVABLE_QUALITE')).toBe('block');
    expect(component.bannerIcon('INFO_MANQUANTE')).toBe('help_outline');
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

  it('standaloneMode -> no GET, form visible, banner displayed', () => {
    component.standaloneMode = true;
    fixture.detectChanges();
    httpMock.expectNone(BASE_URL);
    const banner = fixture.nativeElement.querySelector('[data-testid="standalone-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('Mode simulateur');
  });
});
