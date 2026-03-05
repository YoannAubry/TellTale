"use client";

import { MapContainer, TileLayer, CircleMarker, Popup, Polyline, useMap } from 'react-leaflet';
import 'leaflet/dist/leaflet.css';
import { Waypoint } from '@/types';

// Composant pour recentrer la carte quand on ajoute un point
function MapUpdater({ center }: { center: [number, number] }) {
  const map = useMap();
  map.setView(center, map.getZoom());
  return null;
}

interface MapProps {
  waypoints: Waypoint[];       // Tous les points disponibles (base de données)
  selectedRoute: Waypoint[];   // La route en cours de construction
  onWaypointClick: (wp: Waypoint) => void;
  calculatedPath?: [number, number][]; // Le tracé météo précis (optionnel)
}

export default function Map({ waypoints, selectedRoute, onWaypointClick, calculatedPath }: MapProps) {
  
  // Centre par défaut (St Malo) ou dernier point sélectionné
  const center: [number, number] = selectedRoute.length > 0 
    ? [selectedRoute[selectedRoute.length - 1].latitude, selectedRoute[selectedRoute.length - 1].longitude]
    : [48.65, -2.0];

  return (
    <MapContainer center={center} zoom={11} style={{ height: "100%", width: "100%" }}>
      <TileLayer
        attribution='&copy; OpenStreetMap'
        url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
      />

      {/* 2. Couche Marine (Balises/Phares) - OpenSeaMap */}
      <TileLayer
        url="https://tiles.openseamap.org/seamark/{z}/{x}/{y}.png"
      />
      <MapUpdater center={center} />

      {/* 1. Afficher tous les Waypoints */}
      {waypoints.map((wp) => {
        // Est-ce que ce point fait partie de la route ?
        const isSelected = selectedRoute.some(p => p.id === wp.id);
        const isStart = selectedRoute.length > 0 && selectedRoute[0].id === wp.id;
        const isEnd = selectedRoute.length > 1 && selectedRoute[selectedRoute.length - 1].id === wp.id;

        let color = "blue"; // Par défaut
        if (isSelected) color = "orange"; // Étape
        if (isStart) color = "green";     // Départ
        if (isEnd) color = "red";         // Arrivée

        return (
          <CircleMarker 
            key={wp.id} 
            center={[wp.latitude, wp.longitude]}
            radius={isSelected ? 8 : 5} // Plus gros si sélectionné
            pathOptions={{ color: color, fillColor: color, fillOpacity: 0.7 }}
            eventHandlers={{
              click: () => onWaypointClick(wp), // Clic interactif
            }}
          >
            <Popup>{wp.name} ({wp.type})</Popup>
          </CircleMarker>
        );
      })}

      {/* 2. Tracer la ligne droite entre les points sélectionnés (Aperçu) */}
      {selectedRoute.length > 1 && (
        <Polyline 
          positions={selectedRoute.map(wp => [wp.latitude, wp.longitude])} 
          pathOptions={{ color: 'gray', dashArray: '5, 10', weight: 2 }} 
        />
      )}

      {/* 3. Tracer la route calculée météo (si dispo) */}
      {calculatedPath && calculatedPath.length > 0 && (
        <Polyline 
          positions={calculatedPath} 
          pathOptions={{ color: 'blue', weight: 4 }} 
        />
      )}
    </MapContainer>
  );
}