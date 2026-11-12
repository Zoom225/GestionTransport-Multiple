import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { TrajetsApiService } from '../../../core/api/trajets-api.service';
import { ReservationsApiService } from '../../../core/api/reservations-api.service';
import { AgencesApiService } from '../../../core/api/agences-api.service';
import { VoyageurSessionService } from '../../../core/auth/voyageur-session.service';
import { TrajetResponse } from '../../../shared/models/trajet.model';
import { VoyageurPublicTrajetsPage } from './voyageur-public-trajets-page.component';

describe('VoyageurPublicTrajetsPage', () => {
  const farFuture = '2099-05-20';
  const trajets: TrajetResponse[] = [
    {
      id: 1,
      vehiculeId: 1,
      vehiculeNom: 'Vehicule A',
      agenceNom: 'Lyon',
      organisateurId: 1,
      organisateurNom: 'Tom Bernard',
      date: farFuture,
      heureDebut: '11:00:00',
      heureFin: '12:30:00',
      typeTrajet: 'PUBLIC',
      statut: 'PLANIFIE',
      nbPassagersActuels: 2,
      prixParPassager: 15,
      dateConversionPublic: null
    },
    {
      id: 2,
      vehiculeId: 2,
      vehiculeNom: 'Vehicule B',
      agenceNom: 'Paris',
      organisateurId: 9,
      organisateurNom: 'Emma Dubois',
      date: farFuture,
      heureDebut: '13:00:00',
      heureFin: '14:30:00',
      typeTrajet: 'PUBLIC',
      statut: 'PLANIFIE',
      nbPassagersActuels: 1,
      prixParPassager: 15,
      dateConversionPublic: null
    }
  ];

  let trajetsApiMock: {
    getVilles: ReturnType<typeof vi.fn>;
    getPublic: ReturnType<typeof vi.fn>;
    update: ReturnType<typeof vi.fn>;
    cancel: ReturnType<typeof vi.fn>;
  };
  let reservationsApiMock: { create: ReturnType<typeof vi.fn> };

  beforeEach(async () => {
    trajetsApiMock = {
      getVilles: vi.fn().mockReturnValue(of([])),
      getPublic: vi.fn().mockReturnValue(of(trajets)),
      update: vi.fn().mockImplementation((id: number, payload: Partial<TrajetResponse>) =>
        of({ ...trajets.find((m) => m.id === id)!, ...payload } as TrajetResponse)
      ),
      cancel: vi.fn().mockReturnValue(of(void 0))
    };

    reservationsApiMock = {
      create: vi.fn().mockReturnValue(of({ id: 10 }))
    };

    await TestBed.configureTestingModule({
      imports: [VoyageurPublicTrajetsPage],
      providers: [
        provideRouter([]),
        provideNoopAnimations(),
        { provide: TrajetsApiService, useValue: trajetsApiMock },
        { provide: AgencesApiService, useValue: { getAll: vi.fn().mockReturnValue(of([{ id: 1, nom: 'Lyon' }, { id: 2, nom: 'Paris' }])) } },
        { provide: ReservationsApiService, useValue: reservationsApiMock },
        { provide: VoyageurSessionService, useValue: { voyageurId: vi.fn().mockReturnValue(1) } }
      ]
    }).compileComponents();
  });

  afterEach(() => {
    vi.restoreAllMocks();
    TestBed.resetTestingModule();
  });

  it('identifie correctement les trajets de l’organisateur connecté', () => {
    const fixture = TestBed.createComponent(VoyageurPublicTrajetsPage);
    const component = fixture.componentInstance;

    expect(component.isOrganizer(trajets[0])).toBe(true);
    expect(component.isOrganizer(trajets[1])).toBe(false);
  });

  it('affiche les trajets de l utilisateur en premier dans la liste filtree', () => {
    const fixture = TestBed.createComponent(VoyageurPublicTrajetsPage);
    const component = fixture.componentInstance;

    const ordered = component.filteredTrajets();

    expect(ordered[0].id).toBe(1);
    expect(component.isOrganizer(ordered[0])).toBe(true);
  });

  it('affiche le badge Mon trajet pour le trajet organisé par le voyageur connecté', () => {
    const fixture = TestBed.createComponent(VoyageurPublicTrajetsPage);
    fixture.detectChanges();

    const text = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(text).toContain('Mon trajet');
  });

  it('met à jour un trajet organisé depuis la liste publique', () => {
    const fixture = TestBed.createComponent(VoyageurPublicTrajetsPage);
    const component = fixture.componentInstance;

    component.startEdit(trajets[0]);
    component.editForm.patchValue({ date: '2099-05-22', heureDebut: '10:30', typeTrajet: 'PRIVE' });
    component.saveEdit(trajets[0]);

    expect(trajetsApiMock.update).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        vehiculeId: 1,
        organisateurId: 1,
        date: '2099-05-22',
        heureDebut: '10:30',
        typeTrajet: 'PRIVE'
      })
    );
  });

  it('supprime (annule) un trajet organisé depuis la liste publique', () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    const fixture = TestBed.createComponent(VoyageurPublicTrajetsPage);
    const component = fixture.componentInstance;

    component.cancelOwnTrajet(trajets[0]);

    expect(trajetsApiMock.cancel).toHaveBeenCalledWith(1, 1);
  });

  it('affiche un message clair après une demande de participation', () => {
    const fixture = TestBed.createComponent(VoyageurPublicTrajetsPage);
    const component = fixture.componentInstance;

    component.joinTrajet(trajets[1]);

    expect(component.message()).toContain("Votre place ne sera confirmée qu'après paiement");
  });
});
