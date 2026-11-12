import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { VehiculesApiService } from '../../../core/api/vehicules-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AgenceResponse, VehiculeRequest, VehiculeResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-vehicules-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  template: `
    <!-- En-tête vehicules -->
    <div class="adm-ter-header">
      <div class="adm-ter-header-inner">
        <div class="adm-ter-title-block">
          <span class="adm-ter-icon">📍</span>
          <div>
            <h1 class="adm-ter-title">{{ language.t('admin.vehicules.title') }}</h1>
            <p class="adm-ter-sub">{{ language.t('admin.vehicules.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-ter-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      <!-- Formulaire -->
      <div class="adm-ter-form-card">
        <div class="adm-ter-form-header">
          <span>{{ editingId() ? '✏️' : '➕' }}</span>
          <h2 class="adm-ter-form-title">{{ editingId() ? language.t('common.edit') : language.t('common.create') }}</h2>
        </div>
        <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="save()">
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.vehicules.name') }}</mat-label>
            <input matInput formControlName="nom" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.vehicules.agence') }}</mat-label>
            <mat-select formControlName="agenceId" [disabled]="adminSession.isAgenceAdmin()">
              @for (agence of agences(); track agence.id) {
                <mat-option [value]="agence.id">🏢 {{ agence.nom }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          @if (message()) {
            <div class="status-success md:col-span-2">✅ {{ message() }}</div>
          }
          @if (errorMessage()) {
            <div class="status-error md:col-span-2">❌ {{ errorMessage() }}</div>
          }

          <div class="flex flex-wrap items-center gap-3 md:col-span-2">
            <button class="adm-ter-btn-primary" type="submit" [disabled]="loading() || form.invalid">
              {{ editingId() ? language.t('common.save') : language.t('common.create') }}
            </button>
            <button class="adm-ter-btn-secondary" type="button" (click)="resetForm()">{{ language.t('common.reset') }}</button>
            @if (loading()) { <mat-spinner diameter="24"></mat-spinner> }
          </div>
        </form>
      </div>

      <!-- Liste des vehicules -->
      <div class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        @for (vehicule of filteredVehicules(); track vehicule.id) {
          <div class="adm-ter-card">
            <div class="adm-ter-card-header">
              <span class="adm-ter-card-num">#{{ vehicule.id }}</span>
              <div class="adm-ter-card-name">📍 {{ vehicule.nom }}</div>
              <div class="adm-ter-card-agence">🏢 {{ vehicule.agenceNom }}</div>
            </div>
            <div class="adm-ter-card-actions">
              <button class="adm-ter-action-edit" type="button" (click)="edit(vehicule)">✏️ {{ language.t('common.edit') }}</button>
              <button class="adm-ter-action-delete" type="button" (click)="remove(vehicule.id)">🗑️ {{ language.t('common.delete') }}</button>
            </div>
          </div>
        } @empty {
          <div class="adm-ter-empty md:col-span-2 lg:col-span-3">
            <span>📍</span>
            <p>{{ language.t('admin.vehicules.empty') }}</p>
          </div>
        }
      </div>
    </section>

    <style>
      .adm-ter-header {
        background: linear-gradient(135deg, #134e4a 0%, #0f766e 50%, #0d9488 100%);
        padding: 1.5rem 2rem;
        box-shadow: 0 4px 16px rgba(15,118,110,0.3);
      }
      .adm-ter-header-inner {
        display: flex; align-items: center; justify-content: space-between;
        max-width: 1200px; margin: 0 auto; gap: 1rem; flex-wrap: wrap;
      }
      .adm-ter-title-block { display: flex; align-items: center; gap: 1rem; }
      .adm-ter-icon { font-size: 2.5rem; }
      .adm-ter-title { font-size: 1.6rem; font-weight: 800; color: #fff; margin: 0; }
      .adm-ter-sub { color: rgba(255,255,255,0.75); font-size: 0.85rem; margin: 0; }
      .adm-ter-back-btn {
        background: rgba(255,255,255,0.15); color: #fff; border: 2px solid rgba(255,255,255,0.35);
        border-radius: 9999px; padding: 0.45rem 1.1rem; font-weight: 700;
        font-size: 0.85rem; text-decoration: none; transition: background 0.15s; white-space: nowrap;
      }
      .adm-ter-back-btn:hover { background: rgba(255,255,255,0.25); }

      .adm-ter-form-card {
        background: #fff; border-radius: 1.25rem;
        box-shadow: 0 4px 20px rgba(15,118,110,0.1);
        padding: 1.5rem; border-left: 5px solid #0d9488;
      }
      .adm-ter-form-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; font-size: 1.5rem; }
      .adm-ter-form-title { font-size: 1.2rem; font-weight: 700; color: #134e4a; margin: 0; }

      .adm-ter-btn-primary {
        background: linear-gradient(135deg, #0f766e, #0d9488); color: #fff; border: none;
        border-radius: 0.6rem; padding: 0.65rem 1.5rem; font-weight: 700; cursor: pointer;
        box-shadow: 0 3px 10px rgba(15,118,110,0.3); transition: transform 0.15s;
      }
      .adm-ter-btn-primary:hover:not(:disabled) { transform: translateY(-1px); }
      .adm-ter-btn-primary:disabled { opacity: 0.5; cursor: not-allowed; }
      .adm-ter-btn-secondary {
        background: #f0fdfa; color: #0f766e; border: 1.5px solid #99f6e4;
        border-radius: 0.6rem; padding: 0.65rem 1.25rem; font-weight: 600; cursor: pointer;
        transition: background 0.15s;
      }
      .adm-ter-btn-secondary:hover { background: #ccfbf1; }

      .adm-ter-card {
        background: #fff; border-radius: 1.25rem;
        box-shadow: 0 4px 16px rgba(0,0,0,0.07); overflow: hidden;
        transition: transform 0.15s, box-shadow 0.15s;
        border-top: 4px solid #0d9488;
      }
      .adm-ter-card:hover { transform: translateY(-3px); box-shadow: 0 10px 28px rgba(0,0,0,0.12); }
      .adm-ter-card-header {
        padding: 1rem 1.25rem 0.75rem;
        background: linear-gradient(135deg, #f0fdfa, #ccfbf1);
      }
      .adm-ter-card-num {
        display: inline-block; font-size: 0.72rem; font-weight: 800; color: #0f766e;
        background: #99f6e4; border-radius: 9999px; padding: 0.15rem 0.6rem; margin-bottom: 0.4rem;
      }
      .adm-ter-card-name { font-size: 1.05rem; font-weight: 700; color: #134e4a; }
      .adm-ter-card-agence { font-size: 0.85rem; color: #0f766e; margin-top: 0.2rem; }

      .adm-ter-card-actions {
        display: flex; gap: 0.75rem; padding: 0.75rem 1.25rem;
        border-top: 1px solid #f0fdfa; flex-wrap: wrap;
      }
      .adm-ter-action-edit {
        flex: 1; min-width: 100px; background: #eff6ff; color: #1d4ed8;
        border: 1.5px solid #bfdbfe; border-radius: 0.5rem; padding: 0.45rem 0.75rem;
        font-size: 0.82rem; font-weight: 700; cursor: pointer; transition: background 0.15s;
      }
      .adm-ter-action-edit:hover { background: #dbeafe; }
      .adm-ter-action-delete {
        flex: 1; min-width: 100px; background: #fef2f2; color: #dc2626;
        border: 1.5px solid #fecaca; border-radius: 0.5rem; padding: 0.45rem 0.75rem;
        font-size: 0.82rem; font-weight: 700; cursor: pointer; transition: background 0.15s;
      }
      .adm-ter-action-delete:hover { background: #fee2e2; }

      .adm-ter-empty {
        text-align: center; padding: 3rem; color: #94a3b8;
        display: flex; flex-direction: column; align-items: center; gap: 0.5rem; font-size: 1.1rem;
      }
      .adm-ter-empty span { font-size: 3rem; }
    </style>
  `
})
export class AdminVehiculesPage {
  private readonly vehiculesApi = inject(VehiculesApiService);
  private readonly agencesApi = inject(AgencesApiService);
  readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly editingId = signal<number | null>(null);
  readonly vehicules = signal<VehiculeResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);

  readonly filteredVehicules = computed(() => {
    if (this.adminSession.isGlobalAdmin()) {
      return this.vehicules();
    }
    return this.vehicules().filter((vehicule) => vehicule.agenceId === this.adminSession.agenceId());
  });

  readonly form = new FormGroup({
    nom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    agenceId: new FormControl<number | null>(null, { validators: [Validators.required] })
  });

  constructor() {
    this.loadData();
  }

  loadData(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    forkJoin({
      vehicules: this.vehiculesApi.getAll(),
      agences: this.agencesApi.getAll()
    }).subscribe({
      next: ({ vehicules, agences }) => {
        this.vehicules.set(vehicules);
        const filteredAgences = this.adminSession.isGlobalAdmin() ? agences : agences.filter((agence) => agence.id === this.adminSession.agenceId());
        this.agences.set(filteredAgences);
        if (this.adminSession.isAgenceAdmin() && this.adminSession.agenceId()) {
          this.form.controls.agenceId.setValue(this.adminSession.agenceId());
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.vehicules.loadError')));
      }
    });
  }

  edit(vehicule: VehiculeResponse): void {
    this.editingId.set(vehicule.id);
    this.form.patchValue({ nom: vehicule.nom, agenceId: vehicule.agenceId });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.message.set('');
    this.errorMessage.set('');
    this.form.reset({ nom: '', agenceId: this.adminSession.isAgenceAdmin() ? this.adminSession.agenceId() : null });
  }

  save(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');
    const payload = this.form.getRawValue() as VehiculeRequest;
    if (this.adminSession.isAgenceAdmin()) {
      payload.agenceId = this.adminSession.agenceId()!;
    }

    const request$ = this.editingId() ? this.vehiculesApi.update(this.editingId()!, payload) : this.vehiculesApi.create(payload);
    request$.subscribe({
      next: () => {
        const wasEditing = this.editingId() !== null;
        this.loading.set(false);
        this.resetForm();
        this.message.set(wasEditing ? this.language.t('admin.vehicules.updateSuccess') : this.language.t('admin.vehicules.createSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.vehicules.saveError')));
      }
    });
  }

  remove(id: number): void {
    this.message.set('');
    this.errorMessage.set('');

    if (!confirm(this.language.t('admin.vehicules.deleteConfirm'))) {
      return;
    }

    this.vehiculesApi.delete(id).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.vehicules.deleteSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('admin.vehicules.deleteError')));
      }
    });
  }
}
