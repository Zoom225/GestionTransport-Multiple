import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { IndisponibilitesServiceApiService } from '../../../core/api/indisponibilites-service-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { AdminSessionService } from '../../../core/auth/admin-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import {
  IndisponibiliteServiceResponse,
  AgenceResponse,
} from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

@Component({
  selector: 'app-admin-indisponibilites-service-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatCheckboxModule,
    MatButtonModule,
    MatProgressSpinnerModule,
  ],
  template: `
    <!-- En-tête fermetures -->
    <div class="adm-fer-header">
      <div class="adm-fer-header-inner">
        <div class="adm-fer-title-block">
          <span class="adm-fer-icon" aria-hidden="true"
            ><svg class="ui-icon"><use href="images/ui-icons.svg#lock" /></svg
          ></span>
          <div>
            <h1 class="adm-fer-title">{{ language.t('admin.indisponibilites.title') }}</h1>
            <p class="adm-fer-sub">{{ language.t('admin.indisponibilites.subtitle') }}</p>
          </div>
        </div>
        <a routerLink="/admin" class="adm-fer-back-btn">{{ language.t('common.back') }}</a>
      </div>
    </div>

    <section class="page-shell">
      <!-- Formulaire -->
      <div class="adm-fer-form-card">
        <div class="adm-fer-form-header">
          <span></span>
          <h2 class="adm-fer-form-title">{{ language.t('common.create') }}</h2>
        </div>
        <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="save()">
          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.indisponibilites.date') }}</mat-label>
            <input matInput type="date" formControlName="date" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.indisponibilites.reason') }}</mat-label>
            <input matInput formControlName="raison" />
          </mat-form-field>

          <mat-form-field appearance="outline">
            <mat-label>{{ language.t('admin.indisponibilites.agence') }}</mat-label>
            <mat-select formControlName="agenceId" [disabled]="form.controls.global.value">
              <mat-option [value]="null">{{
                language.t('admin.indisponibilites.noneGlobal')
              }}</mat-option>
              @for (agence of agences(); track agence.id) {
                <mat-option [value]="agence.id">{{ agence.nom }}</mat-option>
              }
            </mat-select>
          </mat-form-field>

          <div class="flex items-center">
            <mat-checkbox formControlName="global" (change)="onGlobalChange($event.checked)">
              {{ language.t('admin.indisponibilites.global') }}
            </mat-checkbox>
          </div>

          @if (message()) {
            <div class="status-success md:col-span-2">{{ message() }}</div>
          }
          @if (errorMessage()) {
            <div class="status-error md:col-span-2">{{ errorMessage() }}</div>
          }

          <div class="flex flex-wrap items-center gap-3 md:col-span-2">
            <button
              class="adm-fer-btn-primary"
              type="submit"
              [disabled]="loading() || form.invalid"
            >
              {{ language.t('admin.indisponibilites.add') }}
            </button>
            <button class="adm-fer-btn-secondary" type="button" (click)="resetForm()">
              {{ language.t('admin.indisponibilites.reset') }}
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

      <!-- Liste des jours -->
      <div class="grid gap-5 md:grid-cols-2 lg:grid-cols-3">
        @for (jour of filteredJours(); track jour.id) {
          <div class="adm-fer-card" [class.adm-fer-card-global]="jour.global">
            <div class="adm-fer-card-top">
              <div class="adm-fer-date">{{ jour.date }}</div>
              <span
                class="adm-fer-badge"
                [class.adm-fer-badge-global]="jour.global"
                [class.adm-fer-badge-agence]="!jour.global"
              >
                {{
                  jour.global
                    ? language.t('admin.indisponibilites.global')
                    : language.t('admin.voyageurs.agence')
                }}
              </span>
            </div>
            @if (!jour.global) {
              <div class="adm-fer-agence">{{ jour.agenceNom }}</div>
            }
            <div class="adm-fer-raison">
              {{ jour.raison || language.t('admin.indisponibilites.reasonEmpty') }}
            </div>
            <div class="adm-fer-card-actions">
              <button class="adm-fer-action-delete" type="button" (click)="remove(jour.id)">
                {{ language.t('common.delete') }}
              </button>
            </div>
          </div>
        } @empty {
          @if (!loading()) {
            <div class="adm-fer-empty md:col-span-2 lg:col-span-3">
              <span></span>
              <p>{{ language.t('admin.indisponibilites.empty') }}</p>
            </div>
          }
        }
      </div>
    </section>

    <style>
      .adm-fer-header {
        background: linear-gradient(120deg, var(--ui-primary-soft), var(--ui-surface));
        padding: 1.5rem 2rem;
        box-shadow: var(--ui-shadow);
      }
      .adm-fer-header-inner {
        display: flex;
        align-items: center;
        justify-content: space-between;
        max-width: 1200px;
        margin: 0 auto;
        gap: 1rem;
        flex-wrap: wrap;
      }
      .adm-fer-title-block {
        display: flex;
        align-items: center;
        gap: 1rem;
      }
      .adm-fer-icon {
        font-size: 2.5rem;
      }
      .adm-fer-title {
        font-size: 1.6rem;
        font-weight: 800;
        color: var(--ui-surface);
        margin: 0;
      }
      .adm-fer-sub {
        color: color-mix(in srgb, var(--ui-surface) 75%, transparent);
        font-size: 0.85rem;
        margin: 0;
      }
      .adm-fer-back-btn {
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
      .adm-fer-back-btn:hover {
        background: color-mix(in srgb, var(--ui-surface) 25%, transparent);
      }

      .adm-fer-form-card {
        background: var(--ui-surface);
        border-radius: 1.25rem;
        box-shadow: var(--ui-shadow);
        padding: 1.5rem;
        border-left: 5px solid var(--ui-warning);
      }
      .adm-fer-form-header {
        display: flex;
        align-items: center;
        gap: 0.75rem;
        margin-bottom: 0.5rem;
        font-size: 1.5rem;
      }
      .adm-fer-form-title {
        font-size: 1.2rem;
        font-weight: 700;
        color: var(--ui-warning);
        margin: 0;
      }

      .adm-fer-btn-primary {
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
      .adm-fer-btn-primary:hover:not(:disabled) {
        transform: translateY(-1px);
      }
      .adm-fer-btn-primary:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
      .adm-fer-btn-secondary {
        background: var(--ui-surface-subtle);
        color: var(--ui-warning);
        border: 1.5px solid var(--ui-warning);
        border-radius: 0.6rem;
        padding: 0.65rem 1.25rem;
        font-weight: 600;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-fer-btn-secondary:hover {
        background: var(--ui-warning-soft);
      }

      .adm-fer-card {
        background: var(--ui-surface);
        border-radius: 1.25rem;
        box-shadow: var(--ui-shadow);
        overflow: hidden;
        transition:
          transform 0.15s,
          box-shadow 0.15s;
        border-top: 4px solid var(--ui-warning);
      }
      .adm-fer-card-global {
        border-top-color: var(--ui-danger);
      }
      .adm-fer-card:hover {
        transform: translateY(-3px);
        box-shadow: var(--ui-shadow);
      }
      .adm-fer-card-top {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 1rem 1.25rem 0.4rem;
        gap: 0.5rem;
      }
      .adm-fer-date {
        font-size: 1.05rem;
        font-weight: 700;
        color: var(--ui-navy);
      }
      .adm-fer-badge {
        font-size: 0.72rem;
        font-weight: 800;
        padding: 0.2rem 0.7rem;
        border-radius: 9999px;
        white-space: nowrap;
      }
      .adm-fer-badge-global {
        background: var(--ui-surface-subtle);
        color: var(--ui-danger);
      }
      .adm-fer-badge-agence {
        background: var(--ui-warning-soft);
        color: var(--ui-warning);
      }
      .adm-fer-agence {
        padding: 0 1.25rem 0.2rem;
        font-size: 0.85rem;
        color: var(--ui-text-secondary);
      }
      .adm-fer-raison {
        padding: 0.4rem 1.25rem 0.75rem;
        font-size: 0.88rem;
        color: var(--ui-text-secondary);
        border-bottom: 1px solid var(--ui-warning-soft);
      }
      .adm-fer-card-actions {
        padding: 0.75rem 1.25rem;
      }
      .adm-fer-action-delete {
        width: 100%;
        background: var(--ui-surface);
        color: var(--ui-danger);
        border: 1.5px solid var(--ui-danger-soft);
        border-radius: 0.5rem;
        padding: 0.5rem;
        font-size: 0.85rem;
        font-weight: 700;
        cursor: pointer;
        transition: background 0.15s;
      }
      .adm-fer-action-delete:hover {
        background: var(--ui-surface-subtle);
      }

      .adm-fer-empty {
        text-align: center;
        padding: 3rem;
        color: var(--ui-text-secondary);
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        font-size: 1.1rem;
      }
      .adm-fer-empty span {
        font-size: 3rem;
      }
    </style>
  `,
})
export class AdminIndisponibilitesServicePage {
  private readonly indisponibilitesServiceApi = inject(IndisponibilitesServiceApiService);
  private readonly agencesApi = inject(AgencesApiService);
  private readonly adminSession = inject(AdminSessionService);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly jours = signal<IndisponibiliteServiceResponse[]>([]);
  readonly agences = signal<AgenceResponse[]>([]);

  readonly filteredJours = computed(() => {
    if (this.adminSession.isGlobalAdmin()) {
      return this.jours();
    }
    const agenceId = this.adminSession.agenceId();
    return this.jours().filter((j) => j.global || j.agenceId === agenceId);
  });

  readonly form = new FormGroup({
    date: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    raison: new FormControl('', { nonNullable: true }),
    global: new FormControl(false, { nonNullable: true }),
    agenceId: new FormControl<number | null>(null),
  });

  constructor() {
    this.loadData();
    if (this.adminSession.isAgenceAdmin()) {
      this.form.controls.agenceId.setValue(this.adminSession.agenceId());
    }
  }

  loadData(): void {
    this.loading.set(true);
    forkJoin({
      jours: this.indisponibilitesServiceApi.getAll(),
      agences: this.agencesApi.getAll(),
    }).subscribe({
      next: ({ jours, agences }) => {
        this.jours.set(jours);
        this.agences.set(
          this.adminSession.isGlobalAdmin()
            ? agences
            : agences.filter((s) => s.id === this.adminSession.agenceId()),
        );
        this.loading.set(false);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('admin.indisponibilites.loadError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }

  onGlobalChange(isGlobal: boolean): void {
    if (isGlobal) {
      this.form.controls.agenceId.setValue(null);
    }
  }

  resetForm(): void {
    this.message.set('');
    this.errorMessage.set('');
    this.form.reset({
      date: '',
      raison: '',
      global: false,
      agenceId: this.adminSession.isAgenceAdmin() ? this.adminSession.agenceId() : null,
    });
  }

  save(): void {
    if (this.form.invalid || this.loading()) {
      return;
    }

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    const raw = this.form.getRawValue();
    this.indisponibilitesServiceApi
      .create({
        date: raw.date,
        raison: raw.raison,
        global: raw.global,
        agenceId: raw.global ? null : raw.agenceId,
      })
      .subscribe({
        next: () => {
          this.loading.set(false);
          this.resetForm();
          this.message.set(this.language.t('admin.indisponibilites.addSuccess'));
          this.loadData();
        },
        error: (error) => {
          this.loading.set(false);
          this.errorMessage.set(
            extractApiErrorMessage(
              error,
              this.language.t('admin.indisponibilites.addError'),
              (key) => this.language.t(key),
            ),
          );
        },
      });
  }

  remove(id: number): void {
    this.message.set('');
    this.errorMessage.set('');

    this.indisponibilitesServiceApi.delete(id).subscribe({
      next: () => {
        this.message.set(this.language.t('admin.indisponibilites.deleteSuccess'));
        this.loadData();
      },
      error: (error) => {
        this.errorMessage.set(
          extractApiErrorMessage(
            error,
            this.language.t('admin.indisponibilites.deleteError'),
            (key) => this.language.t(key),
          ),
        );
      },
    });
  }
}
