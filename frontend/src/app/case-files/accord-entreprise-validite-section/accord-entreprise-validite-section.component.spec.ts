import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { AccordEntrepriseValiditeSectionComponent } from './accord-entreprise-validite-section.component';
import { AccordEntrepriseValiditeResponse } from '../../core/models/accord-entreprise-validite.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('AccordEntrepriseValiditeSectionComponent', () => {
  let component: AccordEntrepriseValiditeSectionComponent;
  let fixture: ComponentFixture<AccordEntrepriseValiditeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/accord-entreprise-validite-analysis';

  function valideResponse(overrides: Partial<AccordEntrepriseValiditeResponse> = {}): AccordEntrepriseValiditeResponse {
    return {
      caseFileId: 'case-1',
      pourcentageSuffragesSignataires: 55,
      typeOperation: 'CONCLUSION',
      referendumOrganise: false,
      referendumApprouve: false,
      conditionMajorite: 'MAJORITE_50',
      dateDenonciation: null,
      dateFinSurvie: null,
      checklist: [
        { item: 'Majorité des suffrages exprimés (> 50 %)', conforme: true, commentaire: 'OK' },
      ],
      itemsNonConformes: 0,
      statut: 'VALIDE',
      consequences: [],
      country: 'FRANCE',
      baseJuridique: 'Art. L.2232-12 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function sousReserveResponse(): AccordEntrepriseValiditeResponse {
    return valideResponse({
      pourcentageSuffragesSignataires: 35,
      referendumOrganise: true,
      referendumApprouve: true,
      conditionMajorite: 'REFERENDUM_30',
      statut: 'VALIDE_SOUS_RESERVE',
      checklist: [
        { item: 'Majorité par référendum (≥ 30 %)', conforme: true, commentaire: '' },
      ],
    });
  }

  function nonValideResponse(): AccordEntrepriseValiditeResponse {
    return valideResponse({
      pourcentageSuffragesSignataires: 35,
      conditionMajorite: 'INSUFFISANTE',
      itemsNonConformes: 1,
      statut: 'NON_VALIDE',
      checklist: [
        { item: 'Majorité des suffrages exprimés', conforme: false, commentaire: 'Signataires < 30 %' },
      ],
      consequences: ['Accord non valide en l\'état.'],
    });
  }

  function denonciationResponse(): AccordEntrepriseValiditeResponse {
    return valideResponse({
      typeOperation: 'DENONCIATION',
      dateDenonciation: '2026-01-01',
      dateFinSurvie: '2027-04-01',
      checklist: [
        { item: 'Préavis de dénonciation (3 mois)', conforme: true, commentaire: '' },
      ],
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [AccordEntrepriseValiditeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(AccordEntrepriseValiditeSectionComponent);
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
    expect(AccordEntrepriseValiditeSectionComponent.TOOL_LABEL).toContain('ACCORD');
    expect(AccordEntrepriseValiditeSectionComponent.TOOL_ICON).toBe('gavel');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(AccordEntrepriseValiditeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 2 (nominal) with both fields', () => {
    expect(AccordEntrepriseValiditeSectionComponent.getPrefillCount({
      aiData: { accordPourcentageSignataires: 55, accordTypeOperation: 'CONCLUSION' },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 1 (partiel) with only pourcentage', () => {
    expect(AccordEntrepriseValiditeSectionComponent.getPrefillCount({
      aiData: { accordPourcentageSignataires: 55 },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(AccordEntrepriseValiditeSectionComponent.getPrefillCount({
      aiData: { accordPourcentageSignataires: 55, accordTypeOperation: 'CONCLUSION' },
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

  it('BELGIQUE workspace shows FR gate banner instead of form', () => {
    component.workspaceCountry = 'BELGIQUE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('[data-testid="country-gate-banner"]');
    expect(banner).not.toBeNull();
    expect(banner.textContent).toContain('français uniquement');
  });

  it('loads existing analysis on GET 200', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(valideResponse());
    expect(component.result()!.statut).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(component.pourcentageSuffragesSignataires()).toBe(55);
    expect(component.typeOperation()).toBe('CONCLUSION');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires a pourcentage in [0;100]', () => {
    expect(component.formValid()).toBe(false);
    component.pourcentageSuffragesSignataires.set(-1);
    expect(component.formValid()).toBe(false);
    component.pourcentageSuffragesSignataires.set(101);
    expect(component.formValid()).toBe(false);
    component.pourcentageSuffragesSignataires.set(55);
    expect(component.formValid()).toBe(true);
    component.pourcentageSuffragesSignataires.set(null);
    expect(component.formValid()).toBe(false);
  });

  // --- champs conditionnels révision / dénonciation ---

  it('REVISION -> signePartiesHabilitees field visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.onTypeOperationChange('REVISION');
    fixture.detectChanges();
    expect(component.isRevision()).toBe(true);
    const block = fixture.nativeElement.querySelector('[data-testid="revision-block"]');
    expect(block).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="denonciation-date-block"]')).toBeNull();
  });

  it('DENONCIATION -> préavis + date fields visible', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.onTypeOperationChange('DENONCIATION');
    fixture.detectChanges();
    expect(component.isDenonciation()).toBe(true);
    expect(fixture.nativeElement.querySelector('[data-testid="denonciation-preavis-block"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="denonciation-date-block"]')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('[data-testid="revision-block"]')).toBeNull();
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when référendum approuvé sans référendum organisé', () => {
    component.referendumApprouve.set(true);
    component.referendumOrganise.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('aucun référendum'))).toBe(true);
  });

  it('raises a coherence alert when révision sans parties habilitées', () => {
    component.onTypeOperationChange('REVISION');
    component.signePartiesHabilitees.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('parties habilitées'))).toBe(true);
  });

  it('raises a coherence alert when dénonciation sans date de dénonciation', () => {
    component.onTypeOperationChange('DENONCIATION');
    component.dateDenonciation.set(null);
    expect(component.coherenceAlerts().some(a => a.includes('fin de survie'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.pourcentageSuffragesSignataires.set(55);
    component.typeOperation.set('CONCLUSION');
    component.referendumOrganise.set(false);
    component.referendumApprouve.set(false);
    component.signePartiesHabilitees.set(false);
    component.preavisDenonciationRespecte.set(true);
    component.dateDenonciation.set(null);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      pourcentageSuffragesSignataires: 55,
      referendumOrganise: false,
      referendumApprouve: false,
      typeOperation: 'CONCLUSION',
      signePartiesHabilitees: false,
      preavisDenonciationRespecte: true,
      dateDenonciation: null,
    });
    req.flush(valideResponse());
    expect(component.result()!.statut).toBe('VALIDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() does nothing when form invalid (no pourcentage)', () => {
    component.ngOnInit();
    flush404();
    component.pourcentageSuffragesSignataires.set(null);
    component.analyze();
    httpMock.expectNone((r) => r.method === 'POST');
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.pourcentageSuffragesSignataires.set(55);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : 3 conditionMajorite / 3 statut states ---

  it('VALIDE -> success statut chip + MAJORITE_50 chip + pourcentage + checklist ✓', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(valideResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('valide');
    expect(chip.className).toContain('is-chip--success');
    const condChip = el.querySelector('[data-testid="condition-majorite-chip"]')!;
    expect(condChip.textContent).toContain('50');
    expect(condChip.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="pourcentage-value"]')!.textContent).toContain('55');
    expect(el.querySelectorAll('[data-testid="checklist"] .is-critere').length).toBe(1);
    expect(el.querySelector('[data-testid="fin-survie-figure"]')).toBeNull();
  });

  it('VALIDE_SOUS_RESERVE -> warning statut chip + REFERENDUM_30 chip', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(sousReserveResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('réserve');
    expect(chip.className).toContain('is-chip--warning');
    const condChip = el.querySelector('[data-testid="condition-majorite-chip"]')!;
    expect(condChip.textContent).toContain('Référendum');
    expect(condChip.className).toContain('is-chip--warning');
  });

  it('NON_VALIDE -> danger statut chip + INSUFFISANTE chip + item ✗', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(nonValideResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="statut-chip"]')!;
    expect(chip.textContent).toContain('non valide');
    expect(chip.className).toContain('is-chip--danger');
    const condChip = el.querySelector('[data-testid="condition-majorite-chip"]')!;
    expect(condChip.textContent).toContain('insuffisante');
    expect(condChip.className).toContain('is-chip--danger');
    expect(el.querySelector('[data-testid="items-non-conformes"]')!.textContent).toContain('1');
    expect(el.querySelectorAll('[data-testid="checklist"] .is-critere--ko').length).toBe(1);
    expect(el.querySelector('[data-testid="consequences"]')).not.toBeNull();
  });

  it('DENONCIATION result -> dateFinSurvie displayed + survie note', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(denonciationResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.querySelector('[data-testid="fin-survie-figure"]')).not.toBeNull();
    expect(el.querySelector('[data-testid="date-fin-survie"]')!.textContent).toContain('2027-04-01');
    expect(el.querySelector('[data-testid="survie-note"]')).not.toBeNull();
  });

  it('chip classes map their states', () => {
    expect(component.statutChipClass('VALIDE')).toContain('success');
    expect(component.statutChipClass('VALIDE_SOUS_RESERVE')).toContain('warning');
    expect(component.statutChipClass('NON_VALIDE')).toContain('danger');
    expect(component.conditionMajoriteChipClass('MAJORITE_50')).toContain('success');
    expect(component.conditionMajoriteChipClass('REFERENDUM_30')).toContain('warning');
    expect(component.conditionMajoriteChipClass('INSUFFISANTE')).toContain('danger');
  });

  // --- pré-fill IA ---

  it('pré-fills pourcentage and typeOperation from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      accordPourcentageSignataires: 42,
      accordTypeOperation: 'REVISION',
    };
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.pourcentageSuffragesSignataires()).toBe(42);
    expect(component.typeOperation()).toBe('REVISION');
    expect(component.provenancePourcentage()).toBe('IA');
    expect(component.provenanceTypeOperation()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData: TravailExtractedData = { accordPourcentageSignataires: 60 };
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.pourcentageSuffragesSignataires()).toBe(60);
    expect(component.provenancePourcentage()).toBe('IA');
  });

  it('onPourcentageChange clears provenance', () => {
    component.provenancePourcentage.set('IA');
    component.onPourcentageChange(33);
    expect(component.pourcentageSuffragesSignataires()).toBe(33);
    expect(component.provenancePourcentage()).toBeNull();
  });

  it('onTypeOperationChange clears provenance', () => {
    component.provenanceTypeOperation.set('IA');
    component.onTypeOperationChange('DENONCIATION');
    expect(component.typeOperation()).toBe('DENONCIATION');
    expect(component.provenanceTypeOperation()).toBeNull();
  });
});
