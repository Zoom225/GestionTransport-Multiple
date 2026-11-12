import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { RouterLink } from '@angular/router';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { AgenceRequest, AgenceResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-agences-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <!-- En-tête agences -->
    <div class="adm-agence-header">
      <div class="adm-agence-header-inner">
        <div class="adm-agence-title-block">
          <span class="adm-agence-icon" aria-hidden="true"
            ><svg class="ui-icon"><use href="images/ui-icons.svg#agency" /></svg
          ></span>
          <div>
            <h1 class="adm-agence-title">{{ language.t('admin.agences.title') }}</h1>
            <p class="adm-agence-sub">{{ language.t('admin.agences.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-agence-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      <!-- Formulaire -->
      <div class="adm-agence-form-card">
        <div class="adm-agence-form-header">
          <span>{{ editingId() ? '✏️' : '➕' }}</span>
          <h2 class="adm-agence-form-title">
            {{ editingId() ? language.t('common.edit') : language.t('common.create') }}
          </h2>
        </div>
        <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="save()">
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.name') }}</mat-label
            ><input matInput formControlName="nom"
          /></mat-form-field>
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.address') }}</mat-label
            ><input matInput formControlName="adresse"
          /></mat-form-field>
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.opening') }}</mat-label
            ><input matInput type="time" formControlName="heureOuverture"
          /></mat-form-field>
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.closing') }}</mat-label
            ><input matInput type="time" formControlName="heureFermeture"
          /></mat-form-field>
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.trajetDuration') }}</mat-label
            ><input matInput type="number" formControlName="dureeTrajetMinutes"
          /></mat-form-field>
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.breakDuration') }}</mat-label
            ><input matInput type="number" formControlName="dureeEntreTrajetMinutes"
          /></mat-form-field>
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.civilYear') }}</mat-label
            ><input matInput type="number" formControlName="anneeCivile"
          /></mat-form-field>

          @if (message()) {
            <div class="status-success md:col-span-2">{{ message() }}</div>
          }
          @if (errorMessage()) {
            <div class="status-error md:col-span-2">{{ errorMessage() }}</div>
          }

          <div class="flex flex-wrap items-center gap-3 md:col-span-2">
            <button
              class="adm-agence-btn-primary"
              type="submit"
              [disabled]="loading() || form.invalid"
            >
              {{ editingId() ? language.t('common.save') : language.t('common.create') }}
            </button>
            <button class="adm-agence-btn-secondary" type="button" (click)="resetForm()">
              {{ language.t('common.reset') }}
            </button>
            @if (loading()) {
              <mat-spinner
                [attr.aria-label]="language.t('transport.searching')"
                diameter="24"
              ></mat-spinner>
            }
          </div>
        </form>
      </div>

      <!-- Liste des agences -->
      <div class="grid gap-5 md:grid-cols-2">
        @for (agence of agences(); track agence.id) {
          <div class="adm-agence-card">
            <div class="adm-agence-card-header">
              <div>
                <div class="adm-agence-card-name">{{ agence.nom }}</div>
                <div class="adm-agence-card-addr">{{ agence.adresse }}</div>
              </div>
            </div>
            <div class="adm-agence-info-grid">
              <div class="adm-agence-info-item adm-agence-info-teal">
                <span class="adm-agence-info-label">{{ language.t('admin.agences.hours') }}</span>
                <span class="adm-agence-info-val"
                  >{{ agence.heureOuverture }} – {{ agence.heureFermeture }}</span
                >
              </div>
              <div class="adm-agence-info-item adm-agence-info-violet">
                <span class="adm-agence-info-label">{{
                  language.t('admin.agences.duration')
                }}</span>
                <span class="adm-agence-info-val">{{ agence.dureeTrajetMinutes }} min</span>
              </div>
              <div class="adm-agence-info-item adm-agence-info-amber">
                <span class="adm-agence-info-label">{{ language.t('admin.agences.break') }}</span>
                <span class="adm-agence-info-val">{{ agence.dureeEntreTrajetMinutes }} min</span>
              </div>
              <div class="adm-agence-info-item adm-agence-info-blue">
                <span class="adm-agence-info-label">{{ language.t('admin.agences.year') }}</span>
                <span class="adm-agence-info-val">{{ agence.anneeCivile }}</span>
              </div>
            </div>
            <div class="adm-agence-card-actions">
              <button class="adm-agence-action-edit" type="button" (click)="edit(agence)">
                {{ language.t('common.edit') }}
              </button>
              <button class="adm-agence-action-delete" type="button" (click)="remove(agence.id)">
                {{ language.t('common.delete') }}
              </button>
            </div>
          </div>
        } @empty {
          <div class="adm-agence-empty md:col-span-2">
            <span></span>
            <p>{{ language.t('admin.agences.empty') }}</p>
          </div>
        }
      </div>
    </section>

    <style>
      .adm-agence-header {
        background: linear-gradient(120deg, var(--ui-navy), var(--ui-primary-strong));
        padding: 1.5rem 2rem;
        box-shadow: var(--ui-shadow);
      }
      .adm-agence-header-inner {
        display: flex;
        align-items: center;
        justify-content: space-between;
        max-width: 1200px;
        margin: 0 auto;
        gap: 1rem;
        flex-wrap: wrap;
      }
      .adm-agence-title-block {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .adm-agence-icon {
        font-size: 2.5rem;
      }
      .adm-agence-title {
        font-size: 1.6rem;
        font-weight: 800;
        color: var(--ui-surface);
        margin: 0;
      }
      .adm-agence-sub {
        color: color-mix(in srgb, var(--ui-surface) 75%, transparent);
        font-size: 0.85rem;
        margin: 0;
      }
      .adm-agence-back-btn {
        background: color-mix(in srgb, var(--ui-surface) 15%, transparent);
        color: var(--ui-surface);
        border: 2px solid color-mix(in srgb, var(--ui-surface) 35%, transparent);
        border-radius: 9999px;
        padding: 0.45rem 1.1rem;
        font-weight: 700;
        font-size: 0.85rem;
        text-decoration: none;
        transition: background 0.15s;
        white-space: nowrap;
      }
      .adm-agence-back-btn:hover {
        background: color-mix(in srgb, var(--ui-surface) 25%, transparent);
      }

      .adm-agence-form-card {
        background: var(--ui-surface);
        border-radius: 1.25rem;
        box-shadow: var(--ui-shadow);
        padding: 1.5rem;
        border-left: 5px solid var(--ui-primary-strong);
      }
      .adm-agence-form-header {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.5rem;
        font-size: 1.5rem;
      }
      .adm-agence-form-title {
        font-size: 1.2rem;
        font-weight: 700;
        color: var(--ui-navy);
        margin: 0;
      }

      .adm-agence-btn-primary {
        background: linear-gradient(120deg, var(--ui-primary-soft), var(--ui-surface));
        color: var(--ui-surface);
        border: none;
        border-radius: 0.6rem;
        padding: 0.65rem 1.5rem;
        font-weight: 700;
        cursor: pointer;
        box-shadow: var(--ui-shadow);
        transition: transform 0.15s;
      }
      .adm-agence-btn-primary:hover:not(:disabled) {
        transform: translateY(-1px);
      }
      .adm-agence-btn-primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .adm-agence-btn-secondary {
        background: var(--ui-surface);
        color: var(--ui-primary-strong);
        border: 1.5px solid var(--ui-primary-soft);
        border-radius: 0.6rem;
        padding: 0.65rem 1.25rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-agence-btn-secondary:hover {
        background: var(--ui-surface-subtle);
      }

      .adm-agence-card {
        background: var(--ui-surface);
        border-radius: 1.25rem;
        box-shadow: var(--ui-shadow);
        overflow: hidden;
        transition:
          transform 0.15s,
          box-shadow 0.15s;
        border-top: 4px solid var(--ui-primary-strong);
      }
      .adm-agence-card:hover {
        transform: translateY(-3px);
        box-shadow: var(--ui-shadow);
      }
      .adm-agence-card-header {
        padding: 1rem 1.25rem 0.5rem;
      }
      .adm-agence-card-name {
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--ui-navy);
      }
      .adm-agence-card-addr {
        font-size: 0.85rem;
        color: var(--ui-text-secondary);
        margin-top: 0.15rem;
      }

      .adm-agence-info-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 0.6rem;
        padding: 0.5rem 1.25rem 0.75rem;
      }
      .adm-agence-info-item {
        border-radius: 0.6rem;
        padding: 0.5rem 0.75rem;
        display: flex;
        flex-direction: column;
        gap: 0.1rem;
      }
      .adm-agence-info-label {
        font-size: 0.72rem;
        font-weight: 600;
        color: var(--ui-text-secondary);
      }
      .adm-agence-info-val {
        font-size: 0.9rem;
        font-weight: 700;
      }
      .adm-agence-info-teal {
        background: var(--ui-surface-subtle);
        color: var(--ui-primary-strong);
      }
      .adm-agence-info-violet {
        background: var(--ui-surface);
        color: var(--ui-primary-strong);
      }
      .adm-agence-info-amber {
        background: var(--ui-surface-subtle);
        color: var(--ui-warning);
      }
      .adm-agence-info-blue {
        background: var(--ui-surface-subtle);
        color: var(--ui-primary-strong);
      }

      .adm-agence-card-actions {
        display: flex;
        gap: 0.75rem;
        padding: 0.75rem 1.25rem;
        border-top: 1px solid var(--ui-surface-subtle);
        flex-wrap: wrap;
      }
      .adm-agence-action-edit {
        flex: 1;
        min-width: 100px;
        background: var(--ui-surface-subtle);
        color: var(--ui-primary-strong);
        border: 1.5px solid var(--ui-primary-soft);
        border-radius: 0.5rem;
        padding: 0.45rem 0.75rem;
        font-size: 0.82rem;
        font-weight: 700;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-agence-action-edit:hover {
        background: var(--ui-primary-soft);
      }
      .adm-agence-action-delete {
        flex: 1;
        min-width: 100px;
        background: var(--ui-surface);
        color: var(--ui-danger);
        border: 1.5px solid var(--ui-danger-soft);
        border-radius: 0.5rem;
        padding: 0.45rem 0.75rem;
        font-size: 0.82rem;
        font-weight: 700;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-agence-action-delete:hover {
        background: var(--ui-surface-subtle);
      }

      .adm-agence-empty {
        text-align: center;
        padding: 3rem;
        color: var(--ui-text-secondary);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        font-size: 1.1rem;
      }
      .adm-agence-empty span {
        font-size: 3rem;
      }
    </style>
  `,
})
export class AdminAgencesPage {
  private readonly agencesApi = inject(AgencesApiService);
  private readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly editingId = signal<number | null>(null);
  readonly agences = signal<AgenceResponse[]>([]);

  readonly form = new FormGroup({
    nom: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    adresse: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    heureOuverture: new FormControl('08:00', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    heureFermeture: new FormControl('22:00', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    dureeTrajetMinutes: new FormControl(90, {
      nonNullable: true,
      validators: [Validators.required],
    }),
    dureeEntreTrajetMinutes: new FormControl(15, {
      nonNullable: true,
      validators: [Validators.required],
    }),
    anneeCivile: new FormControl(new Date().getFullYear(), {
      nonNullable: true,
      validators: [Validators.required],
    }),
  });

  constructor() {
    this.loadAgences();
  }

  loadAgences(): void {
    this.agencesApi.getAll().subscribe({
      next: (agences) => {
        this.agences.set(
          this.adminSession.isGlobalAdmin()
            ? agences
            : agences.filter((agence) => agence.id === this.adminSession.agenceId()),
        );
      },
      error: (error) => {
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.agences.loadError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  edit(agence: AgenceResponse): void {
    this.editingId.set(agence.id);
    this.form.patchValue({ ...agence });
  }

  resetForm(): void {
    this.editingId.set(null);
    this.form.reset({
      nom: '',
      adresse: '',
      heureOuverture: '08:00',
      heureFermeture: '22:00',
      dureeTrajetMinutes: 90,
      dureeEntreTrajetMinutes: 15,
      anneeCivile: new Date().getFullYear(),
    });
  }

  save(): void {
    if (this.form.invalid || this.loading() || this.adminSession.isAgenceAdmin()) {
      if (this.adminSession.isAgenceAdmin()) {
        this.errorMessage.set(this.language.t('admin.agences.agenceAdminEditBlocked'));
      }
      return;
    }

    this.loading.set(true);
    const payload = this.form.getRawValue() as AgenceRequest;
    const request$ = this.editingId()
      ? this.agencesApi.update(this.editingId()!, payload)
      : this.agencesApi.create(payload);
    request$.subscribe({
      next: () => {
        const wasEditing = this.editingId() !== null;
        this.loading.set(false);
        this.resetForm();
        this.message.set(
          wasEditing
            ? this.language.t('admin.agences.updateSuccess')
            : this.language.t('admin.agences.createSuccess'),
        );
        this.loadAgences();
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.agences.saveError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }

  remove(id: number): void {
    if (this.adminSession.isAgenceAdmin()) {
      this.errorMessage.set(this.language.t('admin.agences.agenceAdminDeleteBlocked'));
      return;
    }

    if (!confirm(this.language.t('admin.agences.deleteConfirm'))) {
      return;
    }

    this.agencesApi.delete(id).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.agences.deleteSuccess'));
        this.loadAgences();
      },
      error: (error) => {
        this.errorMessage.set(
          extractApiErrorMessage(error, this.language.t('admin.agences.deleteError'), (key) =>
            this.language.t(key),
          ),
        );
      },
    });
  }
}
