export interface AgenceRequest {
  nom: string;
  adresse: string;
  heureOuverture: string;
  heureFermeture: string;
  dureeTrajetMinutes: number;
  dureeEntreTrajetMinutes: number;
  anneeCivile: number;
}

export interface AgenceResponse {
  id: number;
  nom: string;
  adresse: string;
  heureOuverture: string;
  heureFermeture: string;
  dureeTrajetMinutes: number;
  dureeEntreTrajetMinutes: number;
  anneeCivile: number;
}

export interface VehiculeRequest {
  nom: string;
  agenceId: number;
}

export interface VehiculeResponse {
  id: number;
  nom: string;
  agenceId: number;
  agenceNom: string;
  prix: number | null;
}

export interface IndisponibiliteServiceRequest {
  date: string;
  raison: string;
  global: boolean;
  agenceId: number | null;
}

export interface IndisponibiliteServiceResponse {
  id: number;
  date: string;
  raison: string | null;
  global: boolean;
  agenceId: number | null;
  agenceNom: string | null;
}
