import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';
import { MAT_DIALOG_DATA, MatDialogRef } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import {
  BacklogFeatureDetailDialogComponent,
  expectedSubfeatureMdPath,
} from './backlog-feature-detail-dialog.component';
import { BacklogAdminService } from '../../../core/services/backlog-admin.service';
import { BacklogFeatureDetail } from '../../../core/models/backlog.model';

const mockDetail: BacklogFeatureDetail = {
  id: 'fid-178',
  code: 'F-178',
  title: 'Visualiseur de backlog dans super-admin',
  targetVersion: 'V8+',
  status: 'IN_PROGRESS',
  description: 'Constat 2026-05-01 : les deux backlogs sont riches mais purement Markdown.',
  domain: 'TRANSVERSAL',
  priority: 'MEDIUM',
  sourceFile: 'docs/PRODUCT_SPEC.md',
  sourceLine: 335,
  parsedAt: '2026-05-02T00:55:00Z',
  updatedAt: '2026-05-02T00:55:00Z',
  subfeatures: [
    {
      id: 's1',
      code: 'SF-178-01',
      title: 'Backend infra',
      status: 'DONE',
      description: 'Migration 199 + entités JPA + parser MD + endpoints super-admin.',
      sourceLine: 100,
      updatedAt: '2026-05-01T00:00:00Z',
    },
    {
      id: 's2',
      code: 'SF-178-02',
      title: 'Sync auto',
      status: 'DONE',
      description: 'Cron 5 min + ApplicationReadyEvent.',
      sourceLine: 120,
      updatedAt: '2026-05-02T00:00:00Z',
    },
  ],
};

describe('BacklogFeatureDetailDialogComponent', () => {
  let component: BacklogFeatureDetailDialogComponent;
  let fixture: ComponentFixture<BacklogFeatureDetailDialogComponent>;
  let backlogService: any;
  let dialogRef: any;
  let snackBar: any;

  function setup(data: { code: string }, detailReturn: any) {
    backlogService = jasmine.createSpyObj('BacklogAdminService', [
      'searchFeatures', 'searchMarketingTasks', 'getFreshness',
      'triggerSync', 'getFeatureDetail',
    ]);
    backlogService.getFeatureDetail.mockReturnValue(detailReturn);
    dialogRef = jasmine.createSpyObj('MatDialogRef', ['close']);
    snackBar = jasmine.createSpyObj('MatSnackBar', ['open']);

    TestBed.configureTestingModule({
      imports: [BacklogFeatureDetailDialogComponent, NoopAnimationsModule],
      providers: [
        { provide: BacklogAdminService, useValue: backlogService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
        { provide: MatSnackBar, useValue: snackBar },
      ],
    });

    fixture = TestBed.createComponent(BacklogFeatureDetailDialogComponent);
    component = fixture.componentInstance;
  }

  it('calls getFeatureDetail on init with the code from MAT_DIALOG_DATA', () => {
    setup({ code: 'F-178' }, of(mockDetail));
    fixture.detectChanges();
    expect(backlogService.getFeatureDetail).toHaveBeenCalledWith('F-178');
    expect(component.detail()).toEqual(mockDetail);
    expect(component.loading()).toBe(false);
  });

  it('renders header + badges + description after load', () => {
    setup({ code: 'F-178' }, of(mockDetail));
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    expect(html.querySelector('.code')?.textContent?.trim()).toBe('F-178');
    expect(html.querySelector('.title')?.textContent).toContain('Visualiseur de backlog');
    expect(html.querySelector('pre.description')?.textContent).toContain('Constat');
  });

  it('renders the subfeatures list with badge and truncated description', () => {
    setup({ code: 'F-178' }, of(mockDetail));
    fixture.detectChanges();

    const html = fixture.nativeElement as HTMLElement;
    const subs = html.querySelectorAll('.subfeature');
    expect(subs.length).toBe(2);
    expect(subs[0].textContent).toContain('SF-178-01');
    expect(subs[0].textContent).toContain('Backend infra');
    expect(html.querySelector('.subfeature-md-path')?.textContent?.trim())
      .toBe('docs/features/F-178/SF-178-01-*.md');
  });

  it('flags notFound on 404 without crashing', () => {
    setup({ code: 'F-NOPE' }, throwError(() => ({ status: 404 })));
    fixture.detectChanges();
    expect(component.notFound()).toBe(true);
    expect(component.loading()).toBe(false);
    expect(dialogRef.close).not.toHaveBeenCalled();
  });

  it('closes dialog and shows snack on 500', () => {
    setup({ code: 'F-178' }, throwError(() => ({ status: 500 })));
    fixture.detectChanges();
    expect(snackBar.open).toHaveBeenCalledWith(
      expect.stringContaining('Erreur'),
      'Fermer',
      expect.objectContaining({ panelClass: ['snack-error'] }),
    );
    expect(dialogRef.close).toHaveBeenCalled();
  });

  it('truncate caps strings at 240 chars with ellipsis', () => {
    setup({ code: 'F-178' }, of(mockDetail));
    fixture.detectChanges();
    const long = 'a'.repeat(300);
    expect(component.truncate(long).length).toBe(241);
    expect(component.truncate(long).endsWith('…')).toBe(true);
    expect(component.truncate('short')).toBe('short');
    expect(component.truncate(null)).toBe('');
  });
});

describe('expectedSubfeatureMdPath', () => {
  it('builds the expected glob path for a valid feature/subfeature pair', () => {
    expect(expectedSubfeatureMdPath('F-178', 'SF-178-01'))
      .toBe('docs/features/F-178/SF-178-01-*.md');
    expect(expectedSubfeatureMdPath('F-DT-08', 'SF-DT-08-02'))
      .toBe('docs/features/F-DT-08/SF-DT-08-02-*.md');
  });

  it('returns null when parent code does not match the F-XX pattern', () => {
    expect(expectedSubfeatureMdPath('xxx', 'SF-178-01')).toBeNull();
    expect(expectedSubfeatureMdPath('', 'SF-178-01')).toBeNull();
    expect(expectedSubfeatureMdPath(null, 'SF-178-01')).toBeNull();
  });

  it('returns null when subfeature code does not start with SF-', () => {
    expect(expectedSubfeatureMdPath('F-178', 'X-178-01')).toBeNull();
    expect(expectedSubfeatureMdPath('F-178', null)).toBeNull();
  });
});
