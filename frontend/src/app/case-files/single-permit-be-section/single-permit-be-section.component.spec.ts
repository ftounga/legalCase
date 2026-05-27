import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatSnackBar } from '@angular/material/snack-bar';
import { SimpleChange } from '@angular/core';

import { SinglePermitBeSectionComponent } from './single-permit-be-section.component';
import { SinglePermitBeResponse } from '../../core/models/single-permit-be.model';
import { ImmigrationExtractedData } from '../../core/models/case-analysis.model';

describe('SinglePermitBeSectionComponent', () => {
  let component: SinglePermitBeSectionComponent;
  let fixture: ComponentFixture<SinglePermitBeSectionComponent>;
  let httpMock: HttpTestingController;
  let snackSpy: jasmine.SpyObj<MatSnackBar>;

  const BASE_URL = '/api/v1/case-files/case-1/single-permit-be-analysis';

  function beResponse(overrides: Partial<SinglePermitBeResponse> = {}): SinglePermitBeResponse {
    return {
      caseFileId: 'case-1',
      dateDebutPermit: '2026-01-01',
      dateFinPermit: '2026-12-31',
      regionInstruction: 'WALLONIE',
      typeActivite: 'SALARIE',
      motifDemande: 'RENOUVELLEMENT',
      dateLimiteDemande: '2026-10-31',
      joursAvantExpiration: 90,
      statutRenouvellement: 'DANS_DELAI',
      regionCompetente: 'Wallonie (FOREM)',
      etapesProchaines: [
        'Déposer la demande auprès du FOREM 2 mois avant expiration',
        "Joindre les pièces de l'employeur",
      ],
      baseJuridique:
        'Loi 15/12/1980 art. 61/25-2 ; Accord coopération 02/02/2018 ; AGW 02/05/2019',
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
      imports: [SinglePermitBeSectionComponent, HttpClientTestingModule, NoopAnimationsModule],
      providers: [{ provide: MatSnackBar, useValue: snackSpy }],
    }).compileComponents();

    fixture = TestBed.createComponent(SinglePermitBeSectionComponent);
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
    expect(SinglePermitBeSectionComponent.TOOL_LABEL).toContain('PERMIS UNIQUE');
    expect(SinglePermitBeSectionComponent.TOOL_ICON).toBe('badge');
  });

  it('static getPrefillCount returns 0 when no aiData', () => {
    expect(SinglePermitBeSectionComponent.getPrefillCount({})).toBe(0);
  });

  it('static getPrefillCount returns 5 when all 5 IA signals present (BELGIQUE)', () => {
    expect(SinglePermitBeSectionComponent.getPrefillCount({
      aiData: {
        singlePermitDateDebut: '2026-01-01',
        singlePermitDateFin: '2026-12-31',
        singlePermitRegion: 'WALLONIE',
        singlePermitTypeActivite: 'SALARIE',
        singlePermitMotif: 'NOUVEAU',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(5);
  });

  it('static getPrefillCount returns 2 on partial pré-fill', () => {
    expect(SinglePermitBeSectionComponent.getPrefillCount({
      aiData: {
        singlePermitDateDebut: '2026-01-01',
        singlePermitRegion: 'BRUXELLES',
      },
      workspaceCountry: 'BELGIQUE',
    })).toBe(2);
  });

  it('static getPrefillCount returns 0 when workspaceCountry=FRANCE', () => {
    expect(SinglePermitBeSectionComponent.getPrefillCount({
      aiData: { singlePermitDateDebut: '2026-01-01' },
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
    expect(component.result()!.statutRenouvellement).toBe('DANS_DELAI');
    expect(component.showForm()).toBe(false);
    expect(component.dateDebutPermit()).toBe('2026-01-01');
    expect(component.dateFinPermit()).toBe('2026-12-31');
    expect(component.regionInstruction()).toBe('WALLONIE');
    expect(component.typeActivite()).toBe('SALARIE');
    expect(component.motifDemande()).toBe('RENOUVELLEMENT');
  });

  it('stays in form mode on GET 404', () => {
    component.ngOnInit();
    flush404();
    expect(component.showForm()).toBe(true);
    expect(component.result()).toBeNull();
  });

  it('formValid false if any required field is missing', () => {
    expect(component.formValid()).toBe(false);
    component.dateDebutPermit.set('2026-01-01');
    expect(component.formValid()).toBe(false);
    component.dateFinPermit.set('2026-12-31');
    expect(component.formValid()).toBe(false);
    component.regionInstruction.set('WALLONIE');
    expect(component.formValid()).toBe(false);
    component.typeActivite.set('SALARIE');
    expect(component.formValid()).toBe(false);
    component.motifDemande.set('NOUVEAU');
    expect(component.formValid()).toBe(true);
  });

  it('formValid false if dateDebut > dateFin', () => {
    component.dateDebutPermit.set('2026-12-31');
    component.dateFinPermit.set('2026-01-01');
    component.regionInstruction.set('WALLONIE');
    component.typeActivite.set('SALARIE');
    component.motifDemande.set('NOUVEAU');
    expect(component.formValid()).toBe(false);
  });

  it('analyze() POST nominal -> result + snack + dashboard refresh', () => {
    component.ngOnInit();
    flush404();
    component.dateDebutPermit.set('2026-01-01');
    component.dateFinPermit.set('2026-12-31');
    component.regionInstruction.set('WALLONIE');
    component.typeActivite.set('SALARIE');
    component.motifDemande.set('RENOUVELLEMENT');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST' && r.url === BASE_URL);
    expect(req.request.body).toEqual({
      dateDebutPermit: '2026-01-01',
      dateFinPermit: '2026-12-31',
      regionInstruction: 'WALLONIE',
      typeActivite: 'SALARIE',
      motifDemande: 'RENOUVELLEMENT',
    });
    req.flush(beResponse());
    expect(component.result()!.statutRenouvellement).toBe('DANS_DELAI');
    expect(component.showForm()).toBe(false);
    expect(snackSpy.open).toHaveBeenCalled();
  });

  it('analyze() backend 400 -> snack-error', () => {
    component.ngOnInit();
    flush404();
    component.dateDebutPermit.set('2026-01-01');
    component.dateFinPermit.set('2026-12-31');
    component.regionInstruction.set('FLANDRE');
    component.typeActivite.set('CHERCHEUR');
    component.motifDemande.set('NOUVEAU');
    component.analyze();
    const req = httpMock.expectOne((r) => r.method === 'POST');
    req.flush({ message: 'Bad request' }, { status: 400, statusText: 'Bad' });
    expect(snackSpy.open).toHaveBeenCalledWith(
      jasmine.any(String),
      'Fermer',
      jasmine.objectContaining({ panelClass: 'snack-error' }),
    );
  });

  it('aiData with all 5 IA signals -> pre-fills + provenance IA', () => {
    component.aiData = {
      singlePermitDateDebut: '2026-01-01',
      singlePermitDateFin: '2026-12-31',
      singlePermitRegion: 'BRUXELLES',
      singlePermitTypeActivite: 'CHERCHEUR',
      singlePermitMotif: 'NOUVEAU',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.dateDebutPermit()).toBe('2026-01-01');
    expect(component.provenanceDateDebut()).toBe('IA');
    expect(component.dateFinPermit()).toBe('2026-12-31');
    expect(component.provenanceDateFin()).toBe('IA');
    expect(component.regionInstruction()).toBe('BRUXELLES');
    expect(component.provenanceRegion()).toBe('IA');
    expect(component.typeActivite()).toBe('CHERCHEUR');
    expect(component.provenanceTypeActivite()).toBe('IA');
    expect(component.motifDemande()).toBe('NOUVEAU');
    expect(component.provenanceMotif()).toBe('IA');
  });

  it('GET 200 -> no pre-fill', () => {
    component.aiData = {
      singlePermitDateDebut: '2099-12-31',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ dateDebutPermit: '2026-02-15' }));
    expect(component.dateDebutPermit()).toBe('2026-02-15');
    expect(component.provenanceDateDebut()).toBeNull();
  });

  it('onDateDebutChange clears provenance', () => {
    component.aiData = {
      singlePermitDateDebut: '2026-01-01',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceDateDebut()).toBe('IA');
    component.onDateDebutChange('2026-02-01');
    expect(component.provenanceDateDebut()).toBeNull();
  });

  it('onRegionChange clears provenance', () => {
    component.aiData = {
      singlePermitRegion: 'WALLONIE',
    } as ImmigrationExtractedData;
    component.ngOnInit();
    flush404();
    expect(component.provenanceRegion()).toBe('IA');
    component.onRegionChange('FLANDRE');
    expect(component.provenanceRegion()).toBeNull();
  });

  it('bannerClass covers all 4 status colors', () => {
    expect(component.bannerClass('DEPOSE_EN_TEMPS')).toContain('sp-banner--success');
    expect(component.bannerClass('DANS_DELAI')).toContain('sp-banner--warning');
    expect(component.bannerClass('URGENT')).toContain('sp-banner--danger');
    expect(component.bannerClass('EXPIRE')).toContain('sp-banner--danger-strong');
  });

  it('bannerIcon covers all 4 status icons', () => {
    expect(component.bannerIcon('DEPOSE_EN_TEMPS')).toBe('check_circle');
    expect(component.bannerIcon('DANS_DELAI')).toBe('schedule');
    expect(component.bannerIcon('URGENT')).toBe('warning');
    expect(component.bannerIcon('EXPIRE')).toBe('block');
  });

  it('statutLabel covers all 4 statuses', () => {
    expect(component.statutLabel('DEPOSE_EN_TEMPS')).toContain('temps');
    expect(component.statutLabel('DANS_DELAI')).toContain('délai');
    expect(component.statutLabel('URGENT')).toContain('urgent');
    expect(component.statutLabel('EXPIRE')).toContain('expiré');
  });

  it('regionOptionLabel resolves the 3 region codes', () => {
    expect(component.regionOptionLabel('WALLONIE')).toContain('Wallonie');
    expect(component.regionOptionLabel('FLANDRE')).toContain('Flandre');
    expect(component.regionOptionLabel('BRUXELLES')).toContain('Bruxelles');
    // Unknown -> echo
    expect(component.regionOptionLabel(null)).toBe('');
  });

  it('typeActiviteOptionLabel resolves the 5 type codes', () => {
    expect(component.typeActiviteOptionLabel('SALARIE')).toContain('salarié');
    expect(component.typeActiviteOptionLabel('STAGIAIRE')).toContain('Stagiaire');
    expect(component.typeActiviteOptionLabel('DETACHE')).toContain('détaché');
    expect(component.typeActiviteOptionLabel('CHERCHEUR')).toContain('Chercheur');
    expect(component.typeActiviteOptionLabel('ETUDIANT')).toContain('Étudiant');
  });

  it('motifOptionLabel resolves the 2 motif codes', () => {
    expect(component.motifOptionLabel('NOUVEAU')).toContain('Nouvelle');
    expect(component.motifOptionLabel('RENOUVELLEMENT')).toContain('Renouvellement');
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
    expect(component.dateDebutPermit()).toBeNull();
    component.aiData = {
      singlePermitDateDebut: '2026-03-15',
      singlePermitRegion: 'BRUXELLES',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateDebutPermit()).toBe('2026-03-15');
    expect(component.provenanceDateDebut()).toBe('IA');
    expect(component.regionInstruction()).toBe('BRUXELLES');
    expect(component.provenanceRegion()).toBe('IA');
  });

  it('ngOnChanges does NOT re-prefill when result already loaded', () => {
    component.ngOnInit();
    httpMock.expectOne(BASE_URL).flush(beResponse({ dateDebutPermit: '2026-02-15' }));
    component.aiData = {
      singlePermitDateDebut: '2099-12-31',
    } as ImmigrationExtractedData;
    component.ngOnChanges({ aiData: new SimpleChange(null, component.aiData, false) });
    expect(component.dateDebutPermit()).toBe('2026-02-15');
    expect(component.provenanceDateDebut()).toBeNull();
  });

  it('FRANCE workspace shows info banner instead of form', () => {
    component.workspaceCountry = 'FRANCE';
    component.collapsed.set(false);
    fixture.detectChanges();
    const banner = fixture.nativeElement.querySelector('.sp-banner--info');
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

  it('renders status chip in result header (BELGIQUE)', () => {
    component.standaloneMode = true; // skip auto-GET in ngOnInit
    fixture.detectChanges(); // triggers Angular ngOnInit
    httpMock.expectNone(BASE_URL);
    component.result.set(beResponse({ statutRenouvellement: 'URGENT' }));
    component.showForm.set(false);
    component.collapsed.set(false);
    fixture.detectChanges();
    const chip = fixture.nativeElement.querySelector('.sp-chip');
    expect(chip).not.toBeNull();
    expect(chip.classList.contains('sp-chip--danger')).toBe(true);
  });
});
