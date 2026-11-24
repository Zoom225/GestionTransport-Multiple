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
  styleUrl: './admin-agences-page.component.scss',
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

    <section class="page-shell adm-agence-layout">
      <!-- Liste des agences -->
      <div class="adm-agence-directory">
        <div class="adm-agence-directory-heading">
          <h2>{{ language.t('nav.agences') }}</h2>
          <span class="adm-agence-count"
            >{{ agences().length }} {{ language.t('admin.home.visible') }}</span
          >
        </div>
        <div class="adm-agence-list">
          @for (agence of agences(); track agence.id) {
            <article class="adm-agence-card" [class.is-selected]="editingId() === agence.id">
              <div class="adm-agence-card-header">
                <span class="adm-agence-card-icon" aria-hidden="true"
                  ><svg class="ui-icon"><use href="images/ui-icons.svg#agency" /></svg
                ></span>
                <div>
                  <h3 class="adm-agence-card-name">{{ agence.nom }}</h3>
                  <div class="adm-agence-card-addr">{{ agence.adresse }}</div>
                </div>
              </div>
              <div class="adm-agence-info-grid">
                <div class="adm-agence-info-item adm-agence-info-teal">
                  <span class="adm-agence-info-label">{{ language.t('admin.agences.hours') }}</span>
                  <span class="adm-agence-info-val"
                    >{{ agence.heureOuverture.slice(0, 5) }} –
                    {{ agence.heureFermeture.slice(0, 5) }}</span
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
                <button
                  class="adm-agence-action-edit"
                  type="button"
                  (click)="edit(agence); agencyName.focus()"
                  [attr.aria-label]="language.t('common.edit') + ' : ' + agence.nom"
                >
                  {{ language.t('common.edit') }}
                </button>
                <button
                  class="adm-agence-action-delete"
                  type="button"
                  (click)="remove(agence.id)"
                  [attr.aria-label]="language.t('common.delete') + ' : ' + agence.nom"
                >
                  {{ language.t('common.delete') }}
                </button>
              </div>
            </article>
          } @empty {
            <div class="adm-agence-empty">
              <span class="adm-agence-card-icon" aria-hidden="true"
                ><svg class="ui-icon"><use href="images/ui-icons.svg#agency" /></svg
              ></span>
              <p>{{ language.t('admin.agences.empty') }}</p>
            </div>
          }
        </div>
      </div>

      <!-- Formulaire -->
      <div class="adm-agence-form-card" id="agence-form" [class.is-editing]="editingId() !== null">
        <div class="adm-agence-form-header">
          <span class="adm-agence-form-icon" aria-hidden="true">
            <svg class="ui-icon" viewBox="0 0 24 24">
              <path
                [attr.d]="
                  editingId() ? 'm4 16 12-12 4 4-12 12H4v-4Z M14 6l4 4' : 'M12 5v14M5 12h14'
                "
              />
            </svg>
          </span>
          <h2 class="adm-agence-form-title">
            {{ editingId() ? language.t('common.edit') : language.t('common.create') }}
          </h2>
        </div>
        <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="save()">
          <mat-form-field appearance="outline"
            ><mat-label>{{ language.t('admin.agences.name') }}</mat-label
            ><input #agencyName matInput formControlName="nom"
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
            <div role="status" class="status-success md:col-span-2">{{ message() }}</div>
          }
          @if (errorMessage()) {
            <div role="alert" class="status-error md:col-span-2">{{ errorMessage() }}</div>
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
    </section>
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
