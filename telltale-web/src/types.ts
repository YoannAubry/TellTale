export interface Waypoint {
  id: number;
  name: string;
  latitude: number;
  longitude: number;
  type: "PORT" | "ANCHORAGE" | "POI";
}

export interface RouteForecast {
  totalDistanceNm: number;
  estimatedDurationHours: number;
  points: RoutePoint[];
}

export interface RoutePoint {
  latitude: number;
  longitude: number;
  estimatedTime: string;
  windSpeedKnots: number;
  windDirection: string;
  weatherSummary: string;
  currentSpeedKnots: number;
  currentDirection: string;
  waveHeight: number;
  waveDirection: string;
  boatSpeedKnots: number;
}