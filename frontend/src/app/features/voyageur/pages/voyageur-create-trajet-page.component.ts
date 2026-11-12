import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormControl, FormGroup, ReactiveFormsModule, ValidationErrors, ValidatorFn, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { VehiculesApiService } from '../../../core/api/vehicules-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { LanguageService } from '../../../core/i18n/language.service';
import { TypeVoyageur } from '../../../shared/models/enums.model';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { AgenceResponse, VehiculeResponse } from '../../../shared/models/agence-vehicule.model';
import { extractApiErrorMessage } from '../../../shared/utils/api-error.util';

const BOOKING_MAX_DAYS: Record<TypeVoyageur, number> = {
  VIP: 21,
  LOCAL: 14,
  LIBRE: 5
};

const formatDateForInput = (date: Date): string => {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, '0');
  const day = `${date.getDate()}`.padStart(2, '0');
  return `${year}-${month}-${day}`;
};

const getMaxBookingDays = (voyageurType?: TypeVoyageur | null): number => BOOKING_MAX_DAYS[voyageurType ?? 'LIBRE'];

const createMaxBookingDateValidator = (getMaxDate: () => string): ValidatorFn => {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = `${control.value ?? ''}`.trim();
    if (!value) {
      return null;
    }

    const maxDate = getMaxDate();
    return value > maxDate ? { maxBookingDate: { maxDate, actualDate: value } } : null;
  };
};

@Component({
  selector: 'app-voyageur-create-trajet-page',
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
    <section class="page-shell max-w-6xl">
      <div class="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 class="title-gradient ds-section-title">{{ language.t('voyageur.create.title') }}</h1>
          <p class="ds-subtitle">{{ language.t('voyageur.create.subtitle') }}</p>
          <span class="ds-badge"
                [class]="form.controls.typeTrajet.value === 'PRIVE' ? 'ds-badge-info' : 'ds-badge-success'">
            {{ language.t('voyageur.create.mode') }} : {{ form.controls.typeTrajet.value }}
          </span>
        </div>
        <div class="toolbar-actions">
          <a mat-stroked-button routerLink="/voyageur/trajets">{{ language.t('voyageur.public.title') }}</a>
        </div>
      </div>

      <mat-card class="card-soft panel-gradient">
        <mat-card-content>
          <form [formGroup]="form" class="grid gap-4 pt-4 md:grid-cols-2" (ngSubmit)="submit()">
            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('voyageur.create.agence') }}</mat-label>
              <mat-select formControlName="agenceId" (valueChange)="onAgenceChange($event)" [disabled]="isAgenceVoyageur()">
                @for (agence of agences(); track agence.id) {
                  <mat-option [value]="agence.id">{{ agence.nom }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('voyageur.create.vehicule') }}</mat-label>
              <mat-select formControlName="vehiculeId" [disabled]="!vehicules().length">
                @for (vehicule of vehicules(); track vehicule.id) {
                  <mat-option [value]="vehicule.id">{{ vehicule.nom }}{{ vehicule.prix != null ? ' - ' + vehicule.prix + ' EUR' : '' }}</mat-option>
                }
              </mat-select>
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('voyageur.create.date') }}</mat-label>
              <input matInput type="date" formControlName="date" [min]="todayDate()" [max]="maxBookingDate()" />
              <mat-hint>{{ language.t('voyageur.create.dateHint') }} {{ todayDate() }} - {{ maxBookingDate() }}</mat-hint>
              @if (form.controls.date.hasError('maxBookingDate') && (form.controls.date.dirty || form.controls.date.touched)) {
                <mat-error>{{ bookingDelayErrorMessage() }}</mat-error>
              }
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('voyageur.create.time') }}</mat-label>
              <input matInput type="time" formControlName="heureDebut" />
            </mat-form-field>

            <mat-form-field appearance="outline">
              <mat-label>{{ language.t('voyageur.create.trajetType') }}</mat-label>
              <mat-select formControlName="typeTrajet">
                <mat-option value="PUBLIC">{{ language.t('voyageur.create.public') }}</mat-option>
                <mat-option value="PRIVE">{{ language.t('voyageur.create.private') }}</mat-option>
              </mat-select>
            </mat-form-field>

            <div class="rounded-lg border border-slate-200 bg-slate-50 p-4 text-sm text-slate-700 md:col-span-2">
              <p class="font-semibold text-slate-800">{{ language.t('voyageur.create.rappel') }}</p>
              <ul class="ml-4 list-disc space-y-1">
                <li>{{ language.t('voyageur.create.ruleGlobal') }}</li>
                <li>{{ language.t('voyageur.create.ruleAgence') }}</li>
                <li>{{ language.t('voyageur.create.ruleLibre') }}</li>
              </ul>
              <p class="mt-2 text-slate-500">{{ language.t('voyageur.create.bookingLimit') }} {{ maxBookingDate() }}</p>
            </div>

            @if (errorMessage()) {
              <p class="status-error md:col-span-2">{{ errorMessage() }}</p>
            } @else if (!agences().length && !loading()) {
              <p class="status-info md:col-span-2">{{ language.t('voyageur.create.noAgence') }}</p>
            }

            @if (form.controls.agenceId.value && !vehicules().length && !loading()) {
              <p class="status-info md:col-span-2">{{ language.t('voyageur.create.noVehicule') }}</p>
            }

            @if (form.controls.typeTrajet.value === 'PRIVE') {
              <div class="md:col-span-2">
                <p class="mb-2 text-sm font-medium text-slate-800">{{ language.t('voyageur.create.privatePassagers') }}</p>
                <div class="grid gap-3 md:grid-cols-3">
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('voyageur.create.passager2') }}</mat-label>
                    <input matInput formControlName="passager1" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('voyageur.create.passager3') }}</mat-label>
                    <input matInput formControlName="passager2" />
                  </mat-form-field>
                  <mat-form-field appearance="outline">
                    <mat-label>{{ language.t('voyageur.create.passager4') }}</mat-label>
                    <input matInput formControlName="passager3" />
                  </mat-form-field>
                </div>
              </div>
            }

            @if (message()) {
              <p class="status-success md:col-span-2">{{ message() }}</p>
            }

            <div class="toolbar-actions justify-start md:col-span-2">
              <button mat-flat-button color="primary" type="submit" [disabled]="loading() || form.invalid || !vehicules().length">
                {{ language.t('voyageur.create.submit') }}
              </button>
              <a mat-stroked-button routerLink="/voyageur/profile">{{ language.t('voyageur.create.back') }}</a>
              @if (loading()) {
                <mat-spinner diameter="24"></mat-spinner>
              }
            </div>
          </form>
        </mat-card-content>
      </mat-card>
    </section>
  `
})
export class VoyageurCreateTrajetPage {
  private readonly agencesApi = inject(AgencesApiService);
  private readonly vehiculesApi = inject(VehiculesApiService);
  private readonly trajetsApi = inject(TrajetsApiService);
  private readonly reservationsApi = inject(ReservationsApiService);
  private readonly voyageurSession = inject(VoyageurSessionService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  readonly language = inject(LanguageService);

  readonly loading = signal(false);
  readonly message = signal('');
  readonly errorMessage = signal('');
  readonly agences = signal<AgenceResponse[]>([]);
  readonly vehicules = signal<VehiculeResponse[]>([]);
  readonly voyageur = computed(() => this.voyageurSession.voyageur());
  readonly isAgenceVoyageur = computed(() => this.voyageur()?.typeVoyageur === 'LOCAL');
  readonly todayDate = computed(() => {
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    return formatDateForInput(today);
  });
  readonly maxBookingDate = computed(() => {
    const today = new Date();
    today.setHours(12, 0, 0, 0);
    today.setDate(today.getDate() + getMaxBookingDays(this.voyageur()?.typeVoyageur));
    return formatDateForInput(today);
  });
  readonly bookingDelayErrorMessage = computed(() => {
    return this.language.t('voyageur.create.bookingLimitError', {
      date: this.maxBookingDate()
    });
  });

  readonly bookingDelayProfileErrorMessage = computed(() => {
    const voyageurType = this.voyageur()?.typeVoyageur ?? 'LIBRE';
    return this.language.t('voyageur.create.bookingLimitProfileError', {
      type: voyageurType,
      days: getMaxBookingDays(voyageurType),
      date: this.maxBookingDate()
    });
  });

  readonly form = new FormGroup({
    agenceId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    vehiculeId: new FormControl<number | null>(null, { validators: [Validators.required] }),
    date: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, createMaxBookingDateValidator(() => this.maxBookingDate())]
    }),
    heureDebut: new FormControl('', { nonNullable: true, validators: [Validators.required] }),
    typeTrajet: new FormControl<'PUBLIC' | 'PRIVE'>('PUBLIC', {
      nonNullable: true,
      validators: [Validators.required]
    }),
    passager1: new FormControl('', { nonNullable: true }),
    passager2: new FormControl('', { nonNullable: true }),
    passager3: new FormControl('', { nonNullable: true })
  });

  constructor() {
    if (!this.voyageurSession.isAuthenticated()) {
      this.router.navigateByUrl('/voyageur');
      return;
    }

    this.applyPreferredTypeFromQuery();
    this.applyDefaultStartTime();
    this.applyDateBounds();
    this.loadAgences();
  }

  private applyDefaultStartTime(): void {
    if (!this.form.controls.heureDebut.value) {
      this.form.controls.heureDebut.setValue('09:00');
    }
  }

  private applyPreferredTypeFromQuery(): void {
    const preferredType = this.route.snapshot.queryParamMap.get('type')?.toUpperCase();
    if (preferredType === 'PUBLIC' || preferredType === 'PRIVE') {
      this.form.controls.typeTrajet.setValue(preferredType);
    }
  }

  loadAgences(): void {
    this.loading.set(true);
    this.errorMessage.set('');
    this.agencesApi.getAll().subscribe({
      next: (agences) => {
        this.agences.set(agences);
        const voyageur = this.voyageur();
        if (voyageur?.typeVoyageur === 'LOCAL' && voyageur.agenceId) {
          this.form.controls.agenceId.setValue(voyageur.agenceId);
          this.onAgenceChange(voyageur.agenceId);
          return;
        }

        if (agences.length) {
          const defaultAgenceId = agences[0].id;
          this.form.controls.agenceId.setValue(defaultAgenceId);
          this.onAgenceChange(defaultAgenceId);
        }
        this.loading.set(false);
      },
      error: (error) => {
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('voyageur.create.noAgenceLoaded')));
        this.loading.set(false);
      }
    });
  }

  onAgenceChange(agenceId: number | null): void {
    this.form.controls.vehiculeId.setValue(null);
    this.vehicules.set([]);

    if (!agenceId) {
      return;
    }

    this.loading.set(true);
    this.vehiculesApi.getByAgence(agenceId).subscribe({
      next: (vehicules) => {
        this.vehicules.set(vehicules);
        if (vehicules.length) {
          this.form.controls.vehiculeId.setValue(vehicules[0].id);
        }
        this.loading.set(false);
      },
      error: (error) => {
        const status = error?.status;
        if (status === 404) {
          this.errorMessage.set(this.language.t('voyageur.create.noAgenceLoaded'));
        } else if (status === 500) {
          this.errorMessage.set(this.language.t('voyageur.create.noVehiculeLoaded'));
        } else {
          this.errorMessage.set(extractApiErrorMessage(error, this.language.t('voyageur.create.noVehiculeLoaded')));
        }
        this.vehicules.set([]);
        this.loading.set(false);
      }
    });
  }

  submit(): void {
    this.form.markAllAsTouched();

    if (this.form.invalid || this.loading()) {
      if (this.form.controls.date.hasError('maxBookingDate')) {
        this.errorMessage.set(this.bookingDelayErrorMessage());
      }
      return;
    }

    const { vehiculeId, date, heureDebut, typeTrajet } = this.form.getRawValue();

    if (!vehiculeId) {
      this.errorMessage.set(this.language.t('voyageur.create.noVehicule'));
      return;
    }

    if (typeTrajet !== 'PUBLIC' && typeTrajet !== 'PRIVE') {
      this.errorMessage.set(this.language.t('voyageur.create.invalidType'));
      return;
    }

    this.loading.set(true);
    this.message.set('');
    this.errorMessage.set('');

    const trajetDate = `${date}T${heureDebut}:00`;

    const payload: import('../../../shared/models/trajet.model').CreateTrajetRequest = {
      vehiculeId,
      trajetDate,
      trajetType: typeTrajet
    };

    this.trajetsApi.create(payload).subscribe({
      next: (createdTrajet) => {
        this.handleInvites(createdTrajet);
      },
      error: (error) => {
        this.loading.set(false);
        this.errorMessage.set(this.toFriendlyCreationErrorMessage(error));
      }
    });
  }

  private applyDateBounds(): void {
    const dateControl = this.form.controls.date;
    const minDate = this.todayDate();
    const maxDate = this.maxBookingDate();
    const selectedDate = dateControl.getRawValue();

    if (!selectedDate || selectedDate < minDate) {
      dateControl.setValue(minDate);
    } else if (selectedDate > maxDate) {
      dateControl.setValue(maxDate);
    }

    dateControl.updateValueAndValidity({ emitEvent: false });
  }

  private toFriendlyCreationErrorMessage(error: unknown): string {
    const apiMessage = extractApiErrorMessage(error, this.language.t('voyageur.create.createError'));
    const status = (error as any)?.status || (error as any)?.error?.status;

    const errorMappings: Record<string, string> = {
      'outstanding balance': this.language.t('voyageur.create.outstandingBalance'),
      'active restriction': this.language.t('voyageur.create.activeRestriction'),
      'ne peut pas reserver plus de': this.bookingDelayProfileErrorMessage(),
      'ce créneau est déjà réservé': this.language.t('voyageur.create.slotTaken'),
      'agence est fermé': this.language.t('voyageur.create.agenceClosed'),
      'en dehors des heures d\'ouverture': this.language.t('voyageur.create.outOfHours'),
      'ne peut créer un trajet que sur sa propre agence': this.language.t('voyageur.create.agenceOnly')
    };

    const normalizedMessage = apiMessage
      .toLowerCase()
      .normalize('NFD')
      .replace(/\p{Diacritic}/gu, '')
      .replace(/ã©/g, 'e')
      .replace(/ã¨/g, 'e')
      .replace(/ãª/g, 'e')
      .replace(/ã /g, 'a');
    for (const [key, message] of Object.entries(errorMappings)) {
      const normalizedKey = key
        .toLowerCase()
        .normalize('NFD')
        .replace(/\p{Diacritic}/gu, '');
      if (normalizedMessage.includes(normalizedKey)) {
        return message;
      }
    }

    if (status === 500) {
      return this.language.t('voyageur.create.internalError');
    }

    return apiMessage;
  }

  private handleInvites(createdTrajet: TrajetResponse): void {
    const invitees = [
      this.form.controls.passager1.getRawValue(),
      this.form.controls.passager2.getRawValue(),
      this.form.controls.passager3.getRawValue()
    ]
      .map((value) => value.trim().toUpperCase())
      .filter(Boolean);

    if (this.form.controls.typeTrajet.getRawValue() !== 'PRIVE' || !invitees.length) {
      this.loading.set(false);
      this.message.set(this.language.t('voyageur.create.success'));
      setTimeout(() => this.router.navigateByUrl('/voyageur/reservations'), 1000);
      return;
    }

    this.reservationsApi.createPrivateInvites({
      trajetId: createdTrajet.id,
      inviteeMatricules: invitees
    }).subscribe({
      next: () => {
        this.loading.set(false);
        this.message.set(this.language.t('voyageur.create.successPrivate'));
        setTimeout(() => this.router.navigateByUrl('/voyageur/reservations'), 1000);
      },
      error: (error) => {
        this.loading.set(false);
        this.message.set(this.language.t('voyageur.create.partialSuccess'));
        this.errorMessage.set(extractApiErrorMessage(error, this.language.t('voyageur.create.addPassagersError')));
        setTimeout(() => this.router.navigateByUrl('/voyageur/reservations'), 2000);
      }
    });
  }
}
