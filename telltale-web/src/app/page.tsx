"use client";

import dynamic from 'next/dynamic';
import { useState, useEffect } from 'react';
import { Waypoint, RouteForecast } from '@/types';

// Import dynamique pour Leaflet (Client-side only)
const Map = dynamic(() => import('@/components/Map'), { 
  ssr: false, 
  loading: () => <div className="h-full w-full bg-gray-200 animate-pulse flex items-center justify-center">Chargement de la carte...</div>
});

export default function Home() {
  const [waypoints, setWaypoints] = useState<Waypoint[]>([]);
  const [selectedRoute, setSelectedRoute] = useState<Waypoint[]>([]);
  const [forecast, setForecast] = useState<RouteForecast | null>(null);
  const [departureDate, setDepartureDate] = useState("");

  // Charger les waypoints au démarrage
  useEffect(() => {
    fetch('http://localhost:8080/api/waypoints')
      .then(res => res.json())
      .then(data => setWaypoints(data))
      .catch(console.error);
  }, []);

  // Gestion du clic sur la carte : Ajout / Suppression
  const handleWaypointClick = (wp: Waypoint) => {
    if (selectedRoute.some(p => p.id === wp.id)) {
      setSelectedRoute(selectedRoute.filter(p => p.id !== wp.id));
    } else {
      setSelectedRoute([...selectedRoute, wp]);
    }
    setForecast(null); // On efface le calcul si on modifie la route
  };

  // Appel API de calcul
  const calculateRoute = async () => {
    if (selectedRoute.length < 2) {
      alert("Il faut au moins un départ et une arrivée !");
      return;
    }

    const startId = selectedRoute[0].id;
    const endId = selectedRoute[selectedRoute.length - 1].id;
    
    // Les étapes intermédiaires
    const steps = selectedRoute.slice(1, -1).map(wp => wp.id);
    const stepIdsParam = steps.length > 0 ? `&stepIds=${steps.join(',')}` : '';
    const dateParam = departureDate ? `&departureTime=${departureDate}` : '';

    try {
      // On appelle le backend
      const res = await fetch(`http://localhost:8080/api/routing?startId=${startId}&endId=${endId}${stepIdsParam}${dateParam}&speed=5.0`);
      const data = await res.json();
      setForecast(data);
    } catch (error) {
      console.error(error);
      alert("Erreur lors du calcul de la route.");
    }
  };

  return (
    <div className="flex h-screen bg-gray-100 font-sans">
      
      {/* --- PANNEAU GAUCHE (Contrôles & Résultats) --- */}
      <div className="w-1/3 min-w-[400px] bg-white shadow-xl z-10 flex flex-col border-r border-gray-200">
        
        {/* Header */}
        <div className="p-6 bg-blue-600 text-white">
          <h1 className="text-2xl font-bold">TellTale ⚓️</h1>
          <p className="text-blue-100 text-sm">Planificateur de navigation intelligent</p>
        </div>

        {/* Corps Scrollable */}
        <div className="flex-1 overflow-y-auto p-6 space-y-6">
          
          {/* Section 1 : Itinéraire */}
          <div>
            <h2 className="font-semibold text-gray-700 mb-3 flex items-center gap-2">
              📍 Votre Itinéraire
            </h2>
            
            {selectedRoute.length === 0 ? (
              <div className="p-4 bg-blue-50 border border-blue-100 rounded text-blue-600 text-sm italic text-center">
                Cliquez sur les points bleus sur la carte pour tracer votre route.
              </div>
            ) : (
              <ul className="space-y-2">
                {selectedRoute.map((wp, index) => (
                  <li key={wp.id} className="flex items-center justify-between p-3 bg-white border rounded shadow-sm hover:border-blue-400 transition-colors">
                    <div className="flex items-center gap-3">
                      <span className={`text-xs font-bold px-2 py-1 rounded text-white ${
                        index === 0 ? "bg-green-500" : index === selectedRoute.length - 1 ? "bg-red-500" : "bg-orange-400"
                      }`}>
                        {index === 0 ? "DEPART" : index === selectedRoute.length - 1 ? "ARRIVÉE" : "ÉTAPE"}
                      </span>
                      <span className="font-medium text-gray-800">{wp.name}</span>
                    </div>
                    <button 
                      onClick={() => handleWaypointClick(wp)} 
                      className="text-gray-400 hover:text-red-500 px-2 font-bold"
                      title="Retirer ce point"
                    >
                      ✕
                    </button>
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Section 2 : Date & Action */}
          <div className="bg-gray-50 p-4 rounded border">
            <label className="block text-sm font-semibold text-gray-700 mb-1">Date de départ estimée :</label>
            <input 
              type="datetime-local" 
              className="w-full p-2 border border-gray-300 rounded focus:ring-2 focus:ring-blue-500 outline-none"
              value={departureDate}
              onChange={(e) => setDepartureDate(e.target.value)}
            />
            
            <button 
              onClick={calculateRoute}
              disabled={selectedRoute.length < 2}
              className="w-full mt-4 bg-blue-600 text-white py-3 rounded font-bold hover:bg-blue-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all shadow-md"
            >
              Calculer la Route 🧭
            </button>
          </div>

          {/* Section 3 : Résultats Détaillés */}
          {forecast && (
            <div className="animate-in fade-in slide-in-from-bottom-4 duration-500">
              <h3 className="font-bold text-lg mb-4 text-gray-800 border-b pb-2">Conditions sur le parcours</h3>
              
              {/* Résumé Global */}
              <div className="flex justify-between mb-4 text-sm bg-blue-50 p-3 rounded text-blue-800 font-medium">
                <span>⏱ Durée : {Math.floor(forecast.estimatedDurationHours)}h {Math.round((forecast.estimatedDurationHours % 1) * 60)}min</span>
                <span>📏 Distance : {forecast.totalDistanceNm.toFixed(1)} NM</span>
              </div>

              {/* Liste des points */}
              <div className="space-y-3">
                {forecast.points.map((point, index) => (
                  <div key={index} className="flex flex-col bg-white border rounded shadow-sm overflow-hidden">
                    {/* Header Point : Heure & Position */}
                    <div className="bg-gray-100 px-3 py-1 text-xs text-gray-500 flex justify-between">
                      <span>{new Date(point.estimatedTime).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                      <span>{point.latitude.toFixed(3)}, {point.longitude.toFixed(3)}</span>
                    </div>

                    {/* Contenu Point */}
                    <div className="p-3 grid grid-cols-4 gap-2 text-center text-sm">
                      
                      {/* Vent */}
                      <div className="flex flex-col">
                        <span className="text-[10px] uppercase text-gray-400 font-bold">Vent</span>
                        <span className="font-bold text-blue-600">{point.windSpeedKnots ?? '-'} nds</span>
                        <span className="text-xs text-gray-500">{point.windDirection}</span>
                      </div>

                      {/* Vagues */}
                      <div className="flex flex-col border-l border-gray-100">
                        <span className="text-[10px] uppercase text-gray-400 font-bold">Vagues</span>
                        <span className="font-bold text-indigo-600">{point.waveHeight ? point.waveHeight.toFixed(1) + 'm' : '-'}</span>
                        <span className="text-xs text-gray-500">{point.waveDirection}</span>
                      </div>

                      {/* Courant */}
                      <div className="flex flex-col border-l border-gray-100">
                        <span className="text-[10px] uppercase text-gray-400 font-bold">Courant</span>
                        <span className="font-bold text-teal-600">{point.currentSpeedKnots ? point.currentSpeedKnots.toFixed(1) + ' nds' : '-'}</span>
                        <span className="text-xs text-gray-500">{point.currentDirection}</span>
                      </div>

                      {/* Vitesse Fond */}
                      <div className="flex flex-col border-l border-gray-100 bg-blue-50/50 rounded">
                        <span className="text-[10px] uppercase text-gray-400 font-bold">Vitesse</span>
                        <span className="font-bold text-black text-lg">{point.boatSpeedKnots ? point.boatSpeedKnots.toFixed(1) : '-'}</span>
                        <span className="text-[10px] text-gray-500">SOG</span>
                      </div>

                    </div>
                    {/* Météo texte */}
                    <div className="px-3 pb-2 text-xs text-gray-500 italic text-center border-t border-gray-50 pt-1">
                      {point.weatherSummary}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* --- PANNEAU DROIT (Carte) --- */}
      <div className="flex-1 relative h-full">
        <Map 
          waypoints={waypoints} 
          selectedRoute={selectedRoute} 
          onWaypointClick={handleWaypointClick}
          calculatedPath={forecast?.points.map(p => [p.latitude, p.longitude])}
        />
      </div>
    </div>
  );
}