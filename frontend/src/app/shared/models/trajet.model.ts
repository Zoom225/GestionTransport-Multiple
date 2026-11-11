import { StatutTrajet, TypeTrajet } from './enums.model';

export interface TrajetRequest {
  vehiculeId: number;
  organisateurId: number;
  date: string;
  heureDebut: string;
  typeTrajet: TypeTrajet;
}

export interface TrajetResponse {
  id: number;
  vehiculeId: number;
  vehiculeNom: string;
  agenceNom: string;
  organisateurId: number;
  organisateurNom: string;
  date: string;
  heureDebut: string;
  heureFin: string;
  typeTrajet: TypeTrajet;
  statut: StatutTrajet;
  nbPassagersActuels: number;
  prixParPassager: number;
  dateConversionPublic: string | null;
}

export interface CreateTrajetRequest {
  vehiculeId: number;
  trajetDate: string;
  trajetType: TypeTrajet;
}
