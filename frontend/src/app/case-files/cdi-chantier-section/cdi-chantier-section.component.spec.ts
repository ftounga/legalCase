import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { CdiChantierSectionComponent } from './cdi-chantier-section.component';
import { CdiChantierResponse } from '../../core/models/cdi-chantier.model';
import { TravailExtractedData } from '../../core/models/case-analysis.model';
import { CaseDashboardRefreshService } from '../case-dashboard/case-dashboard-refresh.service';

describe('CdiChantierSectionComponent', () => {
  let component: CdiChantierSectionComponent;
  let fixture: ComponentFixture<CdiChantierSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;
  let refreshSpy: jasmine.SpyObj<CaseDashboardRefreshService>;

  const BASE_URL = '/api/v1/case-files/case-1/cdi-chantier-analysis';

  function fondeResponse(overrides: Partial<CdiChantierResponse> = {}): CdiChantierResponse {
    return {
      caseFileId: 'case-1',
      dateEntree: '2023-01-06',
      dateRupture: '2026-04-06',
      fondementRecours: 'ACCORD_BRANCHE_ETENDU',
      secteur: 'BTP',
      chantierAcheve: true,
      salaireMensuelMoyen: 3000,
      reclassementAutreChantierPropose: false,
      recoursValide: true,
      motifRecours: 'Recours valide (accord de branche étendu).',
      motifLicenciement: 'FIN_CHANTIER_CRS',
      indemniteLicenciement: 2250,
      procedureRequise: true,
      verdictGlobal: 'LICENCIEMENT_FONDE',
      country: 'FRANCE',
      baseJuridique: 'Art. L.1223-8 CT (à vérifier par avocat)',
      ...overrides,
    };
  }

  function recoursInvalideResponse(): CdiChantierResponse {
    return fondeResponse({
      fondementRecours: 'AUCUN',
      recoursValide: false,
      motifRecours: 'Aucun accord ni usage : requalification probable en CDI de droit commun.',
      motifLicenciement: 'MOTIF_NON_FONDE',
      verdictGlobal: 'RECOURS_INVALIDE',
    });
  }

  function aSecuriserResponse(): CdiChantierResponse {
    return fondeResponse({
      reclassementAutreChantierPropose: false,
      verdictGlobal: 'LICENCIEMENT_A_SECURISER',
      motifRecours: 'Recours valide mais reclassement non tracé.',
    });
  }

  function motifNonFondeResponse(): CdiChantierResponse {
    return fondeResponse({
      chantierAcheve: false,
      motifLicenciement: 'MOTIF_NON_FONDE',
      verdictGlobal: 'LICENCIEMENT_A_SECURISER',
      motifRecours: 'Le chantier n\'est pas achevé : motif non caractérisé.',
    });
  }

  function flush404(): void {
    httpMock.expectOne(BASE_URL).flush({ message: 'Not found' }, { status: 404, statusText: 'Not Found' });
  }

  beforeEach(async () => {
    snackSpy = jasmine.createSpyObj('MatSnackBar', ['open']);
    refreshSpy = jasmine.createSpyObj('CaseDashboardRefreshService', ['triggerRefresh']);
    await TestBed.configureTestingModule({
      imports: [CdiChantierSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [
        { provide: MatSnackBar, useValue: snackSpy },
        { provide: CaseDashboardRefreshService, useValue: refreshSpy },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(CdiChantierSectionComponent);
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
    expect(CdiChantierSectionComponent.TOOL_LABEL).toContain('CHANTIER');
    expect(CdiChantierSectionComponent.TOOL_ICON).toBe('engineering');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(CdiChantierSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 3 (nominal) with both dates + secteur', () => {
    expect(CdiChantierSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2023-01-06', dateLicenciement: '2026-04-06', cdiChantierSecteur: 'BTP' },
      workspaceCountry: 'FRANCE',
    })).toBe(3);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=BELGIQUE', () => {
    expect(CdiChantierSectionComponent.getPrefillCount({
      aiData: { dateEntree: '2023-01-06', dateLicenciement: '2026-04-06', cdiChantierSecteur: 'BTP' },
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
    httpMock.expectOne(BASE_URL).flush(fondeResponse());
    expect(component.result()!.verdictGlobal).toBe('LICENCIEMENT_FONDE');
    expect(component.showForm()).toBe(false);
    expect(component.dateEntree()).toBe('2023-01-06');
    expect(component.fondementRecours()).toBe('ACCORD_BRANCHE_ETENDU');
    expect(component.secteur()).toBe('BTP');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  // --- form validity ---

  it('formValid requires dates + fondement + secteur + salaire>0, rejects rupture < entrée', () => {
    expect(component.formValid()).toBe(false);
    component.dateEntree.set('2023-01-06');
    component.dateRupture.set('2026-04-06');
    component.fondementRecours.set('ACCORD_BRANCHE_ETENDU');
    component.secteur.set('BTP');
    expect(component.formValid()).toBe(false); // salaire manquant
    component.salaireMensuelMoyen.set(3000);
    expect(component.formValid()).toBe(true);
    component.salaireMensuelMoyen.set(0);
    expect(component.formValid()).toBe(false);
    component.salaireMensuelMoyen.set(3000);
    component.dateRupture.set('2022-01-01'); // antérieure
    expect(component.formValid()).toBe(false);
  });

  // --- coherence (F-IA-03) ---

  it('raises a coherence alert when dateRupture < dateEntree', () => {
    component.dateEntree.set('2026-05-06');
    component.dateRupture.set('2023-01-06');
    expect(component.coherenceAlerts().some(a => a.includes('antérieure'))).toBe(true);
  });

  it('raises a coherence alert when fondementRecours=AUCUN (requalification)', () => {
    component.fondementRecours.set('AUCUN');
    expect(component.coherenceAlerts().some(a => a.includes('requalification'))).toBe(true);
  });

  it('raises a coherence alert when chantier non achevé', () => {
    component.fondementRecours.set('ACCORD_BRANCHE_ETENDU');
    component.chantierAcheve.set(false);
    expect(component.coherenceAlerts().some(a => a.includes('achevé'))).toBe(true);
  });

  // --- analyze ---

  it('analyze() POST nominal -> result + snack + refresh + exact body', () => {
    component.ngOnInit();
    flush404();
    component.dateEntree.set('2023-01-06');
    component.dateRupture.set('2026-04-06');
    component.fondementRecours.set('ACCORD_BRANCHE_ETENDU');
    component.secteur.set('BTP');
    component.chantierAcheve.set(true);
    component.salaireMensuelMoyen.set(3000);
    component.reclassementAutreChantierPropose.set(false);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateEntree: '2023-01-06',
      dateRupture: '2026-04-06',
      fondementRecours: 'ACCORD_BRANCHE_ETENDU',
      secteur: 'BTP',
      chantierAcheve: true,
      salaireMensuelMoyen: 3000,
      reclassementAutreChantierPropose: false,
    });
    req.flush(fondeResponse());
    expect(component.result()!.verdictGlobal).toBe('LICENCIEMENT_FONDE');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
    expect(refreshSpy.triggerRefresh).toHaveBeenCalled();
  });

  it('analyze() error -> snack error, stays in form', () => {
    component.ngOnInit();
    flush404();
    component.dateEntree.set('2023-01-06');
    component.dateRupture.set('2026-04-06');
    component.fondementRecours.set('ACCORD_BRANCHE_ETENDU');
    component.secteur.set('BTP');
    component.salaireMensuelMoyen.set(3000);
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    req.flush({ message: 'Boom' }, { status: 400, statusText: 'Bad Request' });
    expect(component.analyzing()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  // --- result rendering : verdict + recours + motif + indemnité ---

  it('LICENCIEMENT_FONDE -> success chip + recours valide + FIN_CHANTIER_CRS + indemnité', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(fondeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('fondé');
    expect(chip.className).toContain('is-chip--success');
    const recours = el.querySelector('[data-testid="recours-chip"]')!;
    expect(recours.textContent).toContain('valide');
    expect(recours.className).toContain('is-chip--success');
    const motif = el.querySelector('[data-testid="motif-chip"]')!;
    expect(motif.textContent).toContain('cause réelle');
    expect(motif.className).toContain('is-chip--success');
    expect(el.querySelector('[data-testid="indemnite-value"]')!.textContent).toContain('2,250');
    expect(el.querySelector('[data-testid="indemnite-value"]')!.textContent).toContain('€');
    expect(el.querySelector('[data-testid="requalification-box"]')).toBeNull();
  });

  it('RECOURS_INVALIDE -> danger chip + recours invalide + requalification box', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(recoursInvalideResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('invalide');
    expect(chip.className).toContain('is-chip--danger');
    const recours = el.querySelector('[data-testid="recours-chip"]')!;
    expect(recours.className).toContain('is-chip--danger');
    const box = el.querySelector('[data-testid="requalification-box"]');
    expect(box).not.toBeNull();
    expect(box!.textContent).toContain('requalification');
  });

  it('LICENCIEMENT_A_SECURISER -> warning chip + no requalification box', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(aSecuriserResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const chip = el.querySelector('[data-testid="verdict-chip"]')!;
    expect(chip.textContent).toContain('sécuriser');
    expect(chip.className).toContain('is-chip--warning');
    expect(el.querySelector('[data-testid="requalification-box"]')).toBeNull();
  });

  it('MOTIF_NON_FONDE (chantier non achevé) -> motif chip danger', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    httpMock.expectOne(BASE_URL).flush(motifNonFondeResponse());
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    const motif = el.querySelector('[data-testid="motif-chip"]')!;
    expect(motif.textContent).toContain('non fondé');
    expect(motif.className).toContain('is-chip--danger');
  });

  it('verdict / motif chip classes map their states', () => {
    expect(component.verdictChipClass('LICENCIEMENT_FONDE')).toContain('success');
    expect(component.verdictChipClass('LICENCIEMENT_A_SECURISER')).toContain('warning');
    expect(component.verdictChipClass('RECOURS_INVALIDE')).toContain('danger');
    expect(component.motifChipClass('FIN_CHANTIER_CRS')).toContain('success');
    expect(component.motifChipClass('MOTIF_NON_FONDE')).toContain('danger');
    expect(component.recoursChipClass(true)).toContain('success');
    expect(component.recoursChipClass(false)).toContain('danger');
  });

  it('CCN note ("plus favorable") present in the form', () => {
    component.forceExpanded = true;
    fixture.detectChanges();
    flush404();
    fixture.detectChanges();
    const note = fixture.nativeElement.querySelector('[data-testid="ccn-note"]');
    expect(note).not.toBeNull();
    expect(note.textContent).toContain('plus favorable');
  });

  // --- pré-fill IA ---

  it('pré-fills dateEntree, dateRupture and secteur from aiData (with provenance)', () => {
    const aiData: TravailExtractedData = {
      dateEntree: '2023-01-06',
      dateLicenciement: '2026-04-06',
      cdiChantierSecteur: 'INGENIERIE',
    } as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnInit();
    flush404();
    expect(component.dateEntree()).toBe('2023-01-06');
    expect(component.dateRupture()).toBe('2026-04-06');
    expect(component.secteur()).toBe('INGENIERIE');
    expect(component.provenanceDateEntree()).toBe('IA');
    expect(component.provenanceDateRupture()).toBe('IA');
    expect(component.provenanceSecteur()).toBe('IA');
  });

  it('ngOnChanges aiData triggers pré-fill while in form mode', () => {
    const aiData = { dateEntree: '2023-02-01' } as TravailExtractedData;
    component.aiData = aiData;
    component.ngOnChanges({ aiData: new SimpleChange(null, aiData, true) });
    expect(component.dateEntree()).toBe('2023-02-01');
    expect(component.provenanceDateEntree()).toBe('IA');
  });

  it('onDateEntreeChange clears provenance', () => {
    component.provenanceDateEntree.set('IA');
    component.onDateEntreeChange('2023-03-01');
    expect(component.dateEntree()).toBe('2023-03-01');
    expect(component.provenanceDateEntree()).toBeNull();
  });

  it('onSecteurChange clears provenance', () => {
    component.provenanceSecteur.set('IA');
    component.onSecteurChange('AUTRE');
    expect(component.secteur()).toBe('AUTRE');
    expect(component.provenanceSecteur()).toBeNull();
  });
});
