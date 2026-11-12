import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { PaiementsApiService } from '../../../core/api/paiements-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { VoyageurReservationsPage } from './voyageur-reservations-page.component';

describe('VoyageurReservationsPage', () => {
  const organizedTrajets: TrajetResponse[] = [
    {
      id: 1,
      vehiculeId: 1,
      vehiculeNom: 'Vehicule A',
      agenceNom: 'Lyon',
      organisateurId: 1,
      organisateurNom: 'Tom Bernard',
      date: '2026-05-14',
      heureDebut: '09:00:00',
      heureFin: '10:30:00',
      typeTrajet: 'PUBLIC',
      statut: 'PLANIFIE',
      nbPassagersActuels: 2,
      prixParPassager: 15,
      dateConversionPublic: null
    },
    {
      id: 2,
      vehiculeId: 1,
      vehiculeNom: 'Vehicule A',
      agenceNom: 'Lyon',
      organisateurId: 1,
      organisateurNom: 'Tom Bernard',
      date: '2026-05-20',
      heureDebut: '11:00:00',
      heureFin: '12:30:00',
      typeTrajet: 'PRIVE',
      statut: 'PLANIFIE',
      nbPassagersActuels: 1,
      prixParPassager: 15,
      dateConversionPublic: null
    }
  ];

  let trajetsApiMock: {
    getByOrganisateur: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };
  let reservationsApiMock: {
    getByVoyageur: ReturnType<typeof vi.fn>;
    getByTrajet: ReturnType<typeof vi.fn>;
    create: ReturnType<typeof vi.fn>;
    createPrivateInvites: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };

  beforeEach(async () => {
    trajetsApiMock = {
      getByOrganisateur: vi.fn().mockReturnValue(of(organizedTrajets)),
      update: vi.fn().mockImplementation((id: number, payload: Partial<TrajetResponse>) =>
        of({ ...organizedTrajets.find((m) => m.id === id)!, ...payload } as TrajetResponse)
      ),
      cancel: vi.fn().mockReturnValue(of(void 0))
    };

    reservationsApiMock = {
      getByVoyageur: vi.fn().mockReturnValue(of([])),
      getByTrajet: vi.fn().mockReturnValue(of([])),
      create: vi.fn().mockReturnValue(of({ id: 1 })),
      createPrivateInvites: vi.fn().mockReturnValue(of([])),
      cancel: vi.fn().mockReturnValue(of(void 0))
    };

    await TestBed.configureTestingModule({
      imports: [VoyageurReservationsPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: TrajetsApiService, useValue: trajetsApiMock },
        { provide: ReservationsApiService, useValue: reservationsApiMock },
        { provide: PaiementsApiService, useValue: { pay: vi.fn() } },
        { provide: VoyageurSessionService, useValue: { voyageurId: vi.fn().mockReturnValue(1) } }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('n expose que les trajets PRIVE dans la gestion des passagers', () => {
    const fixture = TestBed.createComponent(VoyageurReservationsPage);
    const component = fixture.componentInstance;

    expect(component.organisedTrajets()).toHaveLength(2);
    expect(component.managedTrajets()).toHaveLength(1);
    expect(component.managedTrajets()[0].id).toBe(2);
  });

  it('bloque la selection d un trajet PUBLIC dans la gestion des passagers', () => {
    const fixture = TestBed.createComponent(VoyageurReservationsPage);
    const component = fixture.componentInstance;

    component.onManagedTrajetChange(1);

    expect(component.managedTrajetId()).toBeNull();
    expect(component.errorMessage()).toContain('réservée aux trajets PRIVÉS');
    expect(reservationsApiMock.getByTrajet).not.toHaveBeenCalled();
  });

  it('modifie un trajet PRIVÉ organisé', () => {
    const fixture = TestBed.createComponent(VoyageurReservationsPage);
    const component = fixture.componentInstance;

    component.onManagedTrajetChange(2);
    component.managedTrajetForm.patchValue({ date: '2026-05-22', heureDebut: '13:30', typeTrajet: 'PRIVE' });
    component.updateManagedTrajet();

    expect(trajetsApiMock.update).toHaveBeenCalledWith(
      2,
      expect.objectContaining({
        vehiculeId: 1,
        organisateurId: 1,
        date: '2026-05-22',
        heureDebut: '13:30',
        typeTrajet: 'PRIVE'
      })
    );
    expect(component.message()).toContain('Trajet mis à jour');
  });

  it('supprime (annule) un trajet PRIVÉ organisé', () => {
    const fixture = TestBed.createComponent(VoyageurReservationsPage);
    const component = fixture.componentInstance;

    component.onManagedTrajetChange(2);
    component.requestDeleteManagedTrajet();
    component.deleteManagedTrajet();

    expect(trajetsApiMock.cancel).toHaveBeenCalledWith(2, 1);
    expect(component.managedTrajetId()).toBeNull();
    expect(trajetsApiMock.getByOrganisateur).toHaveBeenCalled();
  });
});
