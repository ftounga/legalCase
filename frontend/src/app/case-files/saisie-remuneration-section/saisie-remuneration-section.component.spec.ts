import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { SaisieRemunerationSectionComponent } from './saisie-remuneration-section.component';
import { SaisieRemunerationResponse } from '../../core/models/saisie-remuneration.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';

describe('SaisieRemunerationSectionComponent', () => {
  let component: SaisieRemunerationSectionComponent;
  let fixture: ComponentFixture<SaisieRemunerationSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/saisie-remuneration-analysis';

  function saisissableResponse(overrides: Partial<SaisieRemunerationResponse> = {}): SaisieRemunerationResponse {
    return {
      caseFileId: 'case-1',
      remunerationNetteMensuelle: 2000,
      nombrePersonnesACharge: 0,
      creanceTotale: 5000,
      creanceAlimentaire: false,
      verdict: 'SAISISSABLE',
      quotiteSaisissableMensuelle: 320,
      montantLaisseAuSalarie: 1680,
      fractionInsaisissable: 635,
      nombreMoisRecouvrement: 16,
      tranches: [
        { rang: 1, borneInferieure: 0, borneSuperieure: 350, quotite: '1/20', montantTranche: 350, montantSaisissable: 17.5 },
        { rang: 2, borneInferieure: 350, borneSuperieure: 690, quotite: '1/10', montantTranche: 340, montantSaisissable: 34 },
        { rang: 6, borneInferieure: 1900, borneSuperieure: null, quotite: 'totalité', montantTranche: 100, montantSaisissable: 100 },
      ],
      anneeBareme: 2026,
      country: 'FRANCE',
      baseJuridique: 'R. 3252-1 à R. 3252-5 ; L. 3252-2 et L. 3252-3 Code travail',
      ...overrides,
    };
  }

  function insaisissableResponse(): SaisieRemunerationResponse {
    return saisissableResponse({
      remunerationNetteMensuelle: 500,
      verdict: 'INSAISISSABLE',
      quotiteSaisissableMensuelle: 0,
      montantLaisseAuSalarie: 500,
      nombreMoisRecouvrement: null,
      tranches: [],
    });
  }

  function alimentaireResponse(): SaisieRemunerationResponse {
    return saisissableResponse({
      creanceAlimentaire: true,
      verdict: 'ALIMENTAIRE_PAIEMENT_DIRECT',
      quotiteSaisissableMensuelle: 1365,
      montantLaisseAuSalarie: 635,
      tranches: [],
      nombreMoisRecouvrement: 4,
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  function fillValidForm(): void {
    component.remunerationNetteMensuelle.set(2000);
    component.nombrePersonnesACharge.set(0);
    component.creanceTotale.set(5000);
    component.creanceAlimentaire.set(false);
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    await TestBed.configureTestingModule({
      imports: [SaisieRemunerationSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(SaisieRemunerationSectionComponent);
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
    expect(SaisieRemunerationSectionComponent.TOOL_LABEL).toContain('SAISIE SUR RÉMUNÉRATION');
    expect(SaisieRemunerationSectionComponent.TOOL_ICON).toBe('savings');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(SaisieRemunerationSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 1 (partiel) with only salaire', () => {
    expect(SaisieRemunerationSectionComponent.getPrefillCount({
      aiData: { salaireBrutMensuel: 1800 },
      workspaceCountry: 'FRANCE',
    })).toBe(1);
  });

  it('static getPrefillCount returns 2 (nominal) with salaire + nbPersonnes', () => {
    expect(SaisieRemunerationSectionComponent.getPrefillCount({
      aiData: { salaireBrutMensuel: 1800, nombrePersonnesACharge: 2 },
      workspaceCountry: 'FRANCE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(SaisieRemunerationSectionComponent.getPrefillCount({
      aiData: { salaireBrutMensuel: 1800, nombrePersonnesACharge: 2 },
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
    httpMock.expectOne(BASE_URL).flush(saisissableResponse({ nombrePersonnesACharge: 2 }));
    expect(component.result()!.verdict).toBe('SAISISSABLE');
    expect(component.showForm()).toBe(false);
    expect(component.remunerationNetteMensuelle()).toBe(2000);
    expect(component.nombrePersonnesACharge()).toBe(2);
    expect(component.creanceTotale()).toBe(5000);
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validation ---

  it('formValid requires positive remuneration + positive creance', () => {
    component.remunerationNetteMensuelle.set(null);
    component.creanceTotale.set(5000);
    expect(component.formValid()).toBe(false);
    component.remunerationNetteMensuelle.set(2000);
    component.creanceTotale.set(0);
    expect(component.formValid()).toBe(false);
    component.creanceTotale.set(5000);
    expect(component.formValid()).toBe(true);
  });

  it('onNombrePersonnesAChargeChange floors negatives to 0 and truncates decimals', () => {
    component.onNombrePersonnesAChargeChange(-3);
    expect(component.nombrePersonnesACharge()).toBe(0);
    component.onNombrePersonnesAChargeChange(2.9);
    expect(component.nombrePersonnesACharge()).toBe(2);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + exact body shape', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      remunerationNetteMensuelle: 2000,
      nombrePersonnesACharge: 0,
      creanceTotale: 5000,
      creanceAlimentaire: false,
    });
    req.flush(saisissableResponse());
    expect(component.result()!.verdict).toBe('SAISISSABLE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() sends creanceAlimentaire=true when checked', () => {
    component.ngOnInit();
    flush404();
    fillValidForm();
    component.creanceAlimentaire.set(true);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    expect(req.request.body.creanceAlimentaire).toBe(true);
    req.flush(alimentaireResponse());
  });

  it('analyze() does nothing when form invalid', () => {
    component.ngOnInit();
    flush404();
    component.remunerationNetteMensuelle.set(null);
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

  // --- result rendering : quotité + tranches + verdicts ---

  it('verdict SAISISSABLE -> navy chip + quotité + tranche table + bareme note', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(saisissableResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.srm-chip--navy');
    expect(chip).not.toBeNull();
    const quotite = fixture.nativeElement.querySelector('[data-testid="quotite-saisissable"]');
    expect(quotite.textContent).toContain('320');
    const fraction = fixture.nativeElement.querySelector('[data-testid="fraction-insaisissable"]');
    expect(fraction.textContent).toContain('635');
    const mois = fixture.nativeElement.querySelector('[data-testid="mois-recouvrement"]');
    expect(mois.textContent).toContain('16');
    const block = fixture.nativeElement.querySelector('[data-testid="tranches-block"]');
    expect(block).not.toBeNull();
    const rows = fixture.nativeElement.querySelectorAll('[data-testid="tranche-row"]');
    expect(rows.length).toBe(3);
    const note = fixture.nativeElement.querySelector('[data-testid="bareme-note"]');
    expect(note).not.toBeNull();
    expect(note.textContent).toContain('actualiser annuellement');
  });

  it('verdict INSAISISSABLE -> warning chip, no tranche table, no mois recouvrement', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    component.remunerationNetteMensuelle.set(500);
    component.creanceTotale.set(5000);
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(insaisissableResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.srm-chip--warning');
    expect(chip).not.toBeNull();
    const block = fixture.nativeElement.querySelector('[data-testid="tranches-block"]');
    expect(block).toBeNull();
    const mois = fixture.nativeElement.querySelector('[data-testid="mois-recouvrement"]');
    expect(mois).toBeNull();
  });

  it('verdict ALIMENTAIRE_PAIEMENT_DIRECT -> navy chip', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fillValidForm();
    component.creanceAlimentaire.set(true);
    component.analyze();
    httpMock.expectOne((r) => r.method === 'POST').flush(alimentaireResponse());
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.srm-chip--navy');
    expect(chip).not.toBeNull();
    expect(chip.textContent).toContain('alimentaire');
  });

  // --- prefill IA ---

  it('aiData -> pre-fills remuneration (from salaireBrutMensuel) + nbPersonnes with provenance IA', () => {
    component.aiData = { salaireBrutMensuel: 1900, nombrePersonnesACharge: 2 } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.remunerationNetteMensuelle()).toBe(1900);
    expect(component.provenanceRemuneration()).toBe('IA');
    expect(component.nombrePersonnesACharge()).toBe(2);
    expect(component.provenanceNbPersonnesACharge()).toBe('IA');
  });

  it('GET 200 -> no pre-fill (backend wins)', () => {
    component.aiData = { salaireBrutMensuel: 9999, nombrePersonnesACharge: 5 } as TravailExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(saisissableResponse({ remunerationNetteMensuelle: 2000, nombrePersonnesACharge: 0 }));
    expect(component.remunerationNetteMensuelle()).toBe(2000);
    expect(component.provenanceRemuneration()).toBeNull();
    expect(component.nombrePersonnesACharge()).toBe(0);
  });

  it('onRemunerationChange clears remuneration provenance', () => {
    component.aiData = { salaireBrutMensuel: 1900 } as TravailExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceRemuneration()).toBe('IA');
    component.onRemunerationChange(1700);
    expect(component.provenanceRemuneration()).toBeNull();
  });

  it('ngOnChanges with new aiData in form mode -> re-prefill', () => {
    component.ngOnInit();
    flush404();
    expect(component.remunerationNetteMensuelle()).toBeNull();
    component.aiData = { salaireBrutMensuel: 2200, nombrePersonnesACharge: 1 } as TravailExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.remunerationNetteMensuelle()).toBe(2200);
    expect(component.nombrePersonnesACharge()).toBe(1);
  });

  // --- F-IA-03 coherence ---

  it('coherenceAlerts warns that remuneration was pre-filled from BRUT salary', () => {
    component.aiData = { salaireBrutMensuel: 2000 } as TravailExtractedData;
    component.remunerationNetteMensuelle.set(2000);
    component.provenanceRemuneration.set('IA');
    expect(component.coherenceAlerts().some((a) => a.includes('BRUT'))).toBe(true);
  });

  it('coherenceAlerts empty once user edits remuneration (provenance cleared)', () => {
    component.aiData = { salaireBrutMensuel: 2000 } as TravailExtractedData;
    component.onRemunerationChange(1700);
    expect(component.coherenceAlerts().length).toBe(0);
  });

  // --- helpers / labels ---

  it('verdictLabel / bannerClass / bannerIcon cover all 3 verdicts', () => {
    expect(component.verdictLabel('SAISISSABLE')).toContain('saisissable');
    expect(component.verdictLabel('INSAISISSABLE')).toContain('insaisissable');
    expect(component.verdictLabel('ALIMENTAIRE_PAIEMENT_DIRECT')).toContain('alimentaire');
    expect(component.bannerClass('SAISISSABLE')).toContain('srm-banner--navy');
    expect(component.bannerClass('INSAISISSABLE')).toContain('srm-banner--warning');
    expect(component.bannerClass('ALIMENTAIRE_PAIEMENT_DIRECT')).toContain('srm-banner--navy');
    expect(component.bannerIcon('SAISISSABLE')).toBe('savings');
    expect(component.bannerIcon('INSAISISSABLE')).toBe('shield');
    expect(component.bannerIcon('ALIMENTAIRE_PAIEMENT_DIRECT')).toBe('family_restroom');
  });

  it('showTranches true only when tranches present', () => {
    expect(component.showTranches(saisissableResponse())).toBe(true);
    expect(component.showTranches(insaisissableResponse())).toBe(false);
    expect(component.showTranches(null)).toBe(false);
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
