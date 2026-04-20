import { TestBed, ComponentFixture } from '@angular/core/testing';
import { ReferentialsComponent } from './referentials.component';
import { ReferentialService } from '../core/services/referential.service';
import { WorkspaceService } from '../core/services/workspace.service';
import { WorkspaceMemberService } from '../core/services/workspace-member.service';
import { AuthService } from '../core/services/auth.service';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { of, throwError } from 'rxjs';
import { ReferentialResponse } from '../core/models/referential.model';

const mockWorkspace = {
  id: 'ws1', name: 'Cabinet Test', slug: 'cabinet-test',
  planCode: 'STARTER', status: 'ACTIVE', legalDomain: 'DROIT_DU_TRAVAIL'
};

const mockResponse: ReferentialResponse = {
  domain: 'DROIT_DU_TRAVAIL',
  sections: {
    LITIGATION_TYPE: [
      { key: 'DISCRIMINATION', label: 'Discrimination', valueJson: '{"years":5,"article":"Art. L1132-1"}', isSystem: true, sourceRef: 'Art. L1132-1' }
    ],
    BAREME_MACRON: [
      { key: 'LICENCIEMENT', label: 'Licenciement', valueJson: '{"supported":true}', isSystem: true }
    ]
  }
};

function buildTestBed(referentialReturn: any, memberRole = 'OWNER') {
  const referentialServiceSpy = {
    getReferentials: jest.fn().mockReturnValue(referentialReturn),
    updateReferential: jest.fn(),
    getPendingAlertsCount: jest.fn().mockReturnValue(of({ count: 0 })),
    reportAnomaly: jest.fn().mockReturnValue(of(undefined)),
  };
  const workspaceServiceSpy = { getCurrentWorkspace: jest.fn().mockReturnValue(of(mockWorkspace)) };
  const workspaceMemberServiceSpy = {
    getMembers: jest.fn().mockReturnValue(of([{ userId: 'u1', memberRole }]))
  };
  const authServiceSpy = { currentUser: jest.fn().mockReturnValue({ id: 'u1' }) };
  const dialogSpy = { open: jest.fn() };
  const snackBarSpy = { open: jest.fn() };

  return TestBed.configureTestingModule({
    imports: [ReferentialsComponent],
    providers: [
      provideAnimationsAsync(),
      { provide: ReferentialService, useValue: referentialServiceSpy },
      { provide: WorkspaceService, useValue: workspaceServiceSpy },
      { provide: WorkspaceMemberService, useValue: workspaceMemberServiceSpy },
      { provide: AuthService, useValue: authServiceSpy },
      { provide: MatDialog, useValue: dialogSpy },
      { provide: MatSnackBar, useValue: snackBarSpy },
    ]
  });
}

describe('ReferentialsComponent — cas nominal (OWNER)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'OWNER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // REF-UI-01 : panels affichés pour chaque section
  it('REF-UI-01: affiche un panel pour chaque section retournée', () => {
    const panels = fixture.nativeElement.querySelectorAll('mat-expansion-panel');
    expect(panels.length).toBe(2);
  });

  // REF-UI-02 : titre lisible pour LITIGATION_TYPE
  it('REF-UI-02: affiche "Types de litiges" pour la section LITIGATION_TYPE', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Types de litiges');
  });

  // REF-UI-03 : titre lisible pour BAREME_MACRON
  it('REF-UI-03: affiche "Barème Macron" pour la section BAREME_MACRON', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Barème Macron');
  });

  // REF-UI-06 : formatValue LITIGATION_TYPE
  it('REF-UI-06: formatValue pour LITIGATION_TYPE retourne "X an(s) — Art. XXX"', () => {
    const entry = { key: 'DISCRIMINATION', label: 'Discrimination', valueJson: '{"years":5,"article":"Art. L1132-1"}', isSystem: true };
    expect(component.formatValue(entry, 'LITIGATION_TYPE')).toBe('5 ans — Art. L1132-1');
  });

  // REF-UI-07 : formatValue PRESTATION_COEFF
  it('REF-UI-07: formatValue pour PRESTATION_COEFF retourne coefficient formaté', () => {
    const entry = { key: 'FRANCE', label: 'Coeff PC', valueJson: '{"coeff":0.30,"dureeReferenceAns":8}', isSystem: true };
    const result = component.formatValue(entry, 'PRESTATION_COEFF');
    expect(result).toContain('30 %');
    expect(result).toContain('8 ans');
  });

  // SF-110-11 tests — 9 types ajoutés par SF-REF-01-03
  // REF-UI-12 : IMMIGRATION_TITLES
  it('REF-UI-12: formatValue IMMIGRATION_TITLES → phrase lisible, pas de JSON brut', () => {
    const entry = { key: 'VPF', label: 'Titre VPF', isSystem: true,
      valueJson: '{"motif":"Vie privée et familiale","conditions":"Mariage + 3 ans","pieces":["acte","contrat","fiche"],"delaiMoyenJours":90}' };
    const result = component.formatValue(entry, 'IMMIGRATION_TITLES');
    expect(result).toContain('Vie privée et familiale');
    expect(result).toContain('Mariage');
    expect(result).toContain('90 jours');
    expect(result).toContain('3 pièces');
    expect(result).not.toContain('{');
  });

  // REF-UI-13 : IMMIGRATION_RECOURS
  it('REF-UI-13: formatValue IMMIGRATION_RECOURS → phrase lisible', () => {
    const entry = { key: 'OQTF', label: 'OQTF', isSystem: true,
      valueJson: '{"delaiJours":30,"juridiction":"Tribunal administratif","textesApplicables":["L511-1"],"piecesStandard":["decision","recours","id"]}' };
    const result = component.formatValue(entry, 'IMMIGRATION_RECOURS');
    expect(result).toContain('Tribunal administratif');
    expect(result).toContain('30 jours');
    expect(result).toContain('1 texte');
    expect(result).toContain('3 pièces');
    expect(result).not.toContain('{');
  });

  // REF-UI-14 : IMMIGRATION_WORK_RIGHTS
  it('REF-UI-14: formatValue IMMIGRATION_WORK_RIGHTS → mentionne droit au travail', () => {
    const entry = { key: 'ETUDIANT', label: 'Étudiant', isSystem: true,
      valueJson: '{"droitTravail":"CONDITIONNEL","conditions":"max 964h/an","obligationsEmployeur":["declaration","autorisation"]}' };
    const result = component.formatValue(entry, 'IMMIGRATION_WORK_RIGHTS');
    expect(result).toContain('CONDITIONNEL');
    expect(result).toContain('964h/an');
    expect(result).toContain('2 obligations');
    expect(result).not.toContain('{');
  });

  // REF-UI-15 : CONVENTION_BAREMES
  it('REF-UI-15: formatValue CONVENTION_BAREMES → congés légaux affichés', () => {
    const entry = { key: 'SYNTEC', label: 'Syntec', isSystem: true,
      valueJson: '{"congesLegauxJours":25,"primeAnciennetePourcentage":3}' };
    const result = component.formatValue(entry, 'CONVENTION_BAREMES');
    expect(result).toContain('Congés légaux');
    expect(result).toContain('25');
    expect(result).not.toContain('{');
  });

  // REF-UI-16 : LICENCIEMENT_CRITERES (bloquant = true)
  it('REF-UI-16: formatValue LICENCIEMENT_CRITERES bloquant=true → contient "Bloquant"', () => {
    const entry = { key: 'FR_MOTIF_REEL', label: 'Motif réel', isSystem: true,
      valueJson: '{"poids":30,"bloquant":true,"description":"Cause réelle et sérieuse requise"}' };
    const result = component.formatValue(entry, 'LICENCIEMENT_CRITERES');
    expect(result).toContain('30');
    expect(result).toContain('Bloquant');
    expect(result).not.toContain('Non bloquant');
    expect(result).toContain('réelle');
    expect(result).not.toContain('{');
  });

  // REF-UI-17 : INDEMNITE_BAREMES variante MACRON
  it('REF-UI-17: formatValue INDEMNITE_BAREMES MACRON → "Barème sur N années"', () => {
    const entry = { key: 'MACRON', label: 'Barème Macron', isSystem: true,
      valueJson: '{"entries":[{"an":0,"min":0,"max":1},{"an":1,"min":1,"max":2},{"an":10,"min":3,"max":10}]}' };
    const result = component.formatValue(entry, 'INDEMNITE_BAREMES');
    expect(result).toContain('Barème sur 3 années');
    expect(result).toContain('0');
    expect(result).toContain('10');
    expect(result).toContain('mois de salaire');
    expect(result).not.toContain('{');
  });

  // REF-UI-18 : INDEMNITE_BAREMES variante CCT109
  it('REF-UI-18: formatValue INDEMNITE_BAREMES CCT109 → "semaines"', () => {
    const entry = { key: 'CCT109', label: 'CCT 109', isSystem: true,
      valueJson: '{"minSemaines":3,"maxSemaines":17}' };
    const result = component.formatValue(entry, 'INDEMNITE_BAREMES');
    expect(result).toContain('3');
    expect(result).toContain('17');
    expect(result).toContain('semaines');
    expect(result).not.toContain('{');
  });

  // REF-UI-19 : GARDE_MODES → répartition et jours
  it('REF-UI-19: formatValue GARDE_MODES → Parent A/B et jours', () => {
    const entry = { key: 'ALTERNEE_FR', label: 'Alternée', isSystem: true,
      valueJson: '{"repartitionType":"ALTERNEE_1_SUR_2","periodesA":["Semaine A"],"periodesB":["Semaine B"],"vacances":"Partagées","joursA":182,"joursB":183}' };
    const result = component.formatValue(entry, 'GARDE_MODES');
    expect(result).toContain('ALTERNEE_1_SUR_2');
    expect(result).toContain('Parent A');
    expect(result).toContain('182');
    expect(result).toContain('183');
    expect(result).toContain('Partagées');
    expect(result).not.toContain('{');
  });

  // REF-UI-20 : DIVORCE_ETAPES obligatoire=true → préfixe ⚠
  it('REF-UI-20: formatValue DIVORCE_ETAPES obligatoire=true → préfixe ⚠ + description', () => {
    const entry = { key: 'FR_CHOIX_AVOCATS', label: 'Choix avocats', isSystem: true,
      valueJson: '{"ordre":1,"description":"Chaque époux doit avoir son avocat","delai":"—","obligatoire":true}' };
    const result = component.formatValue(entry, 'DIVORCE_ETAPES');
    expect(result).toContain('⚠');
    expect(result).toContain('Étape 1');
    expect(result).toContain('Chaque époux');
    expect(result).not.toContain('{');
  });

  // REF-UI-21 : DIVORCE_PIECES obligatoire=false → pas de préfixe ⚠
  it('REF-UI-21: formatValue DIVORCE_PIECES obligatoire=false → pas de ⚠', () => {
    const entry = { key: 'FR_ATTESTATION', label: 'Attestation', isSystem: true,
      valueJson: '{"description":"Attestation optionnelle","obligatoire":false}' };
    const result = component.formatValue(entry, 'DIVORCE_PIECES');
    expect(result).toContain('Attestation optionnelle');
    expect(result).not.toContain('⚠');
    expect(result).not.toContain('{');
  });

  // REF-UI-22 : JSON malformé → fallback sur la chaîne brute (try/catch)
  it('REF-UI-22: formatValue JSON malformé → fallback sur valueJson brut', () => {
    const entry = { key: 'BROKEN', label: 'Broken', isSystem: true, valueJson: 'not-json' };
    const result = component.formatValue(entry, 'IMMIGRATION_TITLES');
    expect(result).toBe('not-json');
  });
});

describe('ReferentialsComponent — erreur API', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(throwError(() => new Error('500'))).compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-04 : message d'erreur si API échoue
  it('REF-UI-04: affiche un message d\'erreur si l\'API échoue', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Impossible de charger');
  });
});

describe('ReferentialsComponent — MEMBER (pas de bouton Modifier)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'MEMBER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-08 : bouton Modifier absent pour MEMBER
  it('REF-UI-08: bouton "Modifier" absent pour MEMBER', () => {
    const editBtns = fixture.nativeElement.querySelectorAll('.edit-btn');
    expect(editBtns.length).toBe(0);
  });
});

describe('ReferentialsComponent — OWNER (pas de bouton Signaler)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'OWNER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-09 : bouton Signaler absent pour OWNER
  it('REF-UI-09: bouton "Signaler" absent pour OWNER', () => {
    const reportBtns = fixture.nativeElement.querySelectorAll('.report-btn');
    expect(reportBtns.length).toBe(0);
  });
});

describe('ReferentialsComponent — MEMBER (bouton Signaler visible)', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;

  beforeEach(async () => {
    await buildTestBed(of(mockResponse), 'MEMBER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  // REF-UI-10 : bouton Signaler présent pour MEMBER
  it('REF-UI-10: bouton "Signaler" présent pour MEMBER', () => {
    const reportBtns = fixture.nativeElement.querySelectorAll('.report-btn');
    expect(reportBtns.length).toBeGreaterThan(0);
  });

  // REF-UI-11 : canReport = true pour MEMBER
  it('REF-UI-11: canReport = true pour MEMBER', () => {
    expect(component.canReport()).toBe(true);
  });
});

describe('ReferentialsComponent — SF-137-02/03/04 recherche + filtres', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;

  const richResponse: ReferentialResponse = {
    domain: 'DROIT_DU_TRAVAIL',
    sections: {
      LITIGATION_TYPE: [
        { key: 'DISCRIMINATION', label: 'Discrimination', valueJson: '{"years":5,"article":"Art. L1132-1"}', isSystem: true, sourceRef: 'Art. L1132-1' },
        { key: 'HARCELEMENT', label: 'Harcèlement moral', valueJson: '{"years":5,"article":"Art. L1152-1"}', isSystem: true, sourceRef: 'Art. L1152-1' },
      ],
      CONVENTION_BAREMES: [
        { key: 'IDCC_3248', label: 'Métallurgie', valueJson: '{"congesLegauxJours":25}', country: 'FRANCE', isSystem: true, sourceRef: 'IDCC 3248' },
        { key: 'MY_CUSTOM', label: 'Barème workspace custom', valueJson: '{"congesLegauxJours":25}', country: 'FRANCE', isSystem: false, sourceRef: 'custom' },
      ],
    }
  };

  beforeEach(async () => {
    await buildTestBed(of(richResponse), 'OWNER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('SF-137-02: recherche filtre les entries par key/label/sourceRef', () => {
    component.searchQuery.set('harcel');
    const filtered = component.filteredSections();
    expect(filtered).toHaveLength(1);
    expect(filtered[0].type).toBe('LITIGATION_TYPE');
    expect(filtered[0].entries.map(e => e.key)).toEqual(['HARCELEMENT']);
  });

  it('SF-137-02: recherche case-insensitive sur sourceRef', () => {
    component.searchQuery.set('IDCC 3248');
    const filtered = component.filteredSections();
    expect(filtered).toHaveLength(1);
    expect(filtered[0].type).toBe('CONVENTION_BAREMES');
  });

  it('SF-137-03: typeFilter restreint aux sections sélectionnées', () => {
    component.typeFilter.set('LITIGATION_TYPE');
    expect(component.filteredSections().map(s => s.type)).toEqual(['LITIGATION_TYPE']);
  });

  it('SF-137-03: scope SYSTEM masque les entries personnalisées', () => {
    component.scopeFilter.set('SYSTEM');
    const conv = component.filteredSections().find(s => s.type === 'CONVENTION_BAREMES');
    expect(conv?.entries.map(e => e.key)).toEqual(['IDCC_3248']);
  });

  it('SF-137-03: scope CUSTOM garde uniquement les workspace entries', () => {
    component.scopeFilter.set('CUSTOM');
    const conv = component.filteredSections().find(s => s.type === 'CONVENTION_BAREMES');
    expect(conv?.entries.map(e => e.key)).toEqual(['MY_CUSTOM']);
    // Section LITIGATION_TYPE est vide en CUSTOM → doit disparaître
    expect(component.filteredSections().map(s => s.type)).not.toContain('LITIGATION_TYPE');
  });

  it('SF-137-04: compteurs reflètent filtre actif', () => {
    expect(component.totalEntryCount()).toBe(4);
    component.searchQuery.set('harcel');
    expect(component.filteredEntryCount()).toBe(1);
    expect(component.hasActiveFilters()).toBe(true);
  });

  it('SF-137-03: clearFilters réinitialise tout', () => {
    component.searchQuery.set('xyz');
    component.typeFilter.set('LITIGATION_TYPE');
    component.scopeFilter.set('SYSTEM');
    expect(component.hasActiveFilters()).toBe(true);
    component.clearFilters();
    expect(component.hasActiveFilters()).toBe(false);
    expect(component.filteredEntryCount()).toBe(4);
  });
});

describe('ReferentialsComponent — SF-140-01 aide contextuelle', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;
  let component: ReferentialsComponent;
  let dialogSpy: { open: jest.Mock };

  const richResponse: ReferentialResponse = {
    domain: 'DROIT_DU_TRAVAIL',
    sections: {
      LICENCIEMENT_CRITERES: [
        { key: 'FR_MOTIVATION', label: 'Motivation lettre licenciement',
          valueJson: '{"poids":20,"bloquant":true,"description":"Motifs précis et vérifiables"}',
          country: 'FRANCE', isSystem: true, sourceRef: 'Code travail L. 1232-1' },
      ],
      CONVENTION_BAREMES: [
        { key: 'IDCC_3248', label: 'Métallurgie', valueJson: '{"congesLegauxJours":25,"congesSupp":[],"primes":[]}',
          country: 'FRANCE', isSystem: true, sourceRef: 'CCN IDCC 3248' },
      ],
    }
  };

  beforeEach(async () => {
    await buildTestBed(of(richResponse), 'OWNER').compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    component = fixture.componentInstance;
    dialogSpy = TestBed.inject(MatDialog) as unknown as { open: jest.Mock };
    fixture.detectChanges();
  });

  it('SF-140-01: hasSectionDoc renvoie true pour un type documenté', () => {
    expect(component.hasSectionDoc('CONVENTION_BAREMES')).toBe(true);
    expect(component.hasSectionDoc('LICENCIEMENT_CRITERES')).toBe(true);
    expect(component.hasSectionDoc('TYPE_INEXISTANT')).toBe(false);
  });

  it('SF-140-01: openSectionHelp ouvre le dialog avec le type seul', () => {
    component.openSectionHelp('CONVENTION_BAREMES');
    expect(dialogSpy.open).toHaveBeenCalled();
    const callArgs = dialogSpy.open.mock.calls[0][1];
    expect(callArgs.data).toEqual({ sectionType: 'CONVENTION_BAREMES' });
  });

  it('SF-140-01: openEntryHelp ouvre le dialog avec description métier extraite (LICENCIEMENT_CRITERES)', () => {
    const entry = richResponse.sections.LICENCIEMENT_CRITERES[0];
    component.openEntryHelp(entry as any, 'LICENCIEMENT_CRITERES');
    const callArgs = dialogSpy.open.mock.calls[0][1];
    expect(callArgs.data.entry.key).toBe('FR_MOTIVATION');
    expect(callArgs.data.entry.metierDescription).toContain('Motifs précis');
    expect(callArgs.data.entry.sourceRef).toBe('Code travail L. 1232-1');
    expect(callArgs.data.entry.rawJson).toContain('"poids":20');
  });

  it('SF-140-01: openEntryHelp retourne undefined metierDescription pour CONVENTION_BAREMES (pas de description native)', () => {
    const entry = richResponse.sections.CONVENTION_BAREMES[0];
    component.openEntryHelp(entry as any, 'CONVENTION_BAREMES');
    const callArgs = dialogSpy.open.mock.calls[0][1];
    expect(callArgs.data.entry.metierDescription).toBeUndefined();
  });
});

describe('ReferentialsComponent — sections vides', () => {
  let fixture: ComponentFixture<ReferentialsComponent>;

  beforeEach(async () => {
    await buildTestBed(of({ domain: 'DROIT_DU_TRAVAIL', sections: {} })).compileComponents();
    fixture = TestBed.createComponent(ReferentialsComponent);
    fixture.detectChanges();
  });

  // REF-UI-05 : "Aucun référentiel disponible" si sections vides
  it('REF-UI-05: affiche "Aucun référentiel disponible" si sections vides', () => {
    const text = fixture.nativeElement.textContent as string;
    expect(text).toContain('Aucun référentiel disponible');
  });
});
