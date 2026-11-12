import { TypeVoyageur } from './enums.model';

export interface LoginRequest {
  matricule: string;
  password: string; // Added password field
}

export interface VoyageurRequest {
  peutProposerTrajet?: boolean;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeVoyageur: TypeVoyageur;
  agenceId?: number;
  password?: string;
}

export interface VoyageurResponse {
  peutProposerTrajet?: boolean;
  id: number;
  matricule: string;
  nom: string;
  prenom: string;
  email: string;
  typeVoyageur: TypeVoyageur;
  agenceId: number | null;
  agenceNom: string | null;
  solde: number;
  token?: string;
}

export interface LoginResponse extends VoyageurResponse {
  token: string;
}
export interface RestrictionResponse {
  id: number;
  motif: string;
  sourceEvenement: string | null;
  dateDebut: string | null;
  dateFin: string;
  active: boolean;
}
