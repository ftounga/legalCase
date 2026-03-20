import { Component, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatSnackBar } from '@angular/material/snack-bar';
import { DatePipe } from '@angular/common';
import { forkJoin } from 'rxjs';
import { WorkspaceService } from '../../core/services/workspace.service';
import { WorkspaceMemberService } from '../../core/services/workspace-member.service';
import { Workspace } from '../../core/models/workspace.model';
import { WorkspaceMember } from '../../core/models/workspace-member.model';

const PLAN_QUOTA: Record<string, number | null> = {
  FREE: null,
  STARTER: 3,
  PRO: 20
};

@Component({
  selector: 'app-workspace-admin',
  standalone: true,
  imports: [
    DatePipe, RouterLink,
    MatCardModule, MatTableModule, MatProgressSpinnerModule,
    MatIconModule, MatButtonModule, MatChipsModule
  ],
  templateUrl: './workspace-admin.component.html',
  styleUrl: './workspace-admin.component.scss'
})
export class WorkspaceAdminComponent implements OnInit {
  workspace = signal<Workspace | null>(null);
  members = signal<WorkspaceMember[]>([]);
  loading = signal(true);
  accessDenied = signal(false);

  readonly memberColumns = ['email', 'role'];

  constructor(
    private workspaceService: WorkspaceService,
    private memberService: WorkspaceMemberService,
    private snackBar: MatSnackBar
  ) {}

  ngOnInit(): void {
    forkJoin({
      workspace: this.workspaceService.getCurrentWorkspace(),
      members: this.memberService.getMembers()
    }).subscribe({
      next: ({ workspace, members }) => {
        this.workspace.set(workspace);
        this.members.set(members);
        this.loading.set(false);
      },
      error: (err: any) => {
        this.loading.set(false);
        if (err.status === 403) {
          this.accessDenied.set(true);
        } else {
          this.snackBar.open('Erreur lors du chargement des données', 'Fermer', {
            duration: 4000, panelClass: ['snack-error']
          });
        }
      }
    });
  }

  getPlanQuota(planCode: string): string {
    const quota = PLAN_QUOTA[planCode];
    return quota !== null && quota !== undefined ? `${quota} dossiers` : 'Essai gratuit';
  }

  isTrial(workspace: Workspace): boolean {
    return workspace.planCode === 'FREE' && !!workspace.expiresAt;
  }
}
