import React, { useState, useEffect } from 'react';
import { Train, ShieldAlert, ShieldCheck, Database, Zap, RefreshCw } from 'lucide-react';

const API_ROOT = 'http://localhost:8000';

export default function App() {
  const [gridState, setGridState] = useState({ track_occupied: false, current_train: 'NONE', history: [] });
  const [activeTrain, setActiveTrain] = useState('TRN-BULLET-01');
  const [terminalFeed, setTerminalFeed] = useState(["HelixRail Operational Grid Terminal: ONLINE. Synchronizers active."]);

  const fetchSystemStatus = async () => {
    try {
      const res = await fetch(`${API_ROOT}/rail/status`);
      const data = await res.json();
      setGridState(data);
    } catch (err) {
      console.error("Java Concurrency Engine Unavailable:", err);
    }
  };

  useEffect(() => {
    fetchSystemStatus();
    const interval = setInterval(fetchSystemStatus, 1500);
    return () => clearInterval(interval);
  }, []);

  const requestTrackAccess = async () => {
    setTerminalFeed(prev => [`[WAITING] ${activeTrain} entering wait loop for synchronized monitor lock pool...`, ...prev]);
    try {
      const res = await fetch(`${API_ROOT}/rail/request`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ train_id: activeTrain })
      });
      
      if (res.status === 200) {
        setTerminalFeed(prev => [`[🔒 LOCK ACQUIRED] ${activeTrain} entered the critical track block section safely.`, ...prev]);
      } else if (res.status === 429) {
        setTerminalFeed(prev => [`[🚨 TIMEOUT] ${activeTrain} failed to acquire monitor block before threshold timeout expired.`, ...prev]);
      }
      fetchSystemStatus();
    } catch (err) {
      console.error(err);
    }
  };

  const releaseTrackAccess = async () => {
    try {
      const res = await fetch(`${API_ROOT}/rail/release`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ train_id: activeTrain })
      });
      setTerminalFeed(prev => [`[🔓 LOCK RELEASED] ${activeTrain} exited track block. Monitor notified waiting threads.`, ...prev]);
      fetchSystemStatus();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="min-h-screen bg-neutral-950 text-neutral-100 font-mono p-6 selection:bg-neutral-800">
      {/* HUD Header Bar */}
      <div className="max-w-6xl mx-auto border-b border-neutral-800 pb-4 flex justify-between items-center">
        <div className="flex items-center gap-3">
          <Train className="text-amber-500 w-8 h-8 animate-pulse" />
          <div>
            <h1 className="text-xl font-black tracking-wider">HELIX_RAIL.CORE</h1>
            <p className="text-xs text-neutral-500">Java Thread Synchronization Monitor & Relational Fact Ledger</p>
          </div>
        </div>
        <div className="text-xs border border-neutral-800 rounded-lg px-3 py-1.5 bg-neutral-900/40 flex items-center gap-2">
          <Database className="text-amber-500 w-4 h-4" /> Persistence Warehouse: <span className="text-white font-bold">SQLITE_TRANSIT_LEDGER</span>
        </div>
      </div>

      <div className="max-w-6xl mx-auto grid grid-cols-1 lg:grid-cols-3 gap-6 mt-6">
        
        {/* Core Control Simulation Parameters Input Area */}
        <div className="bg-neutral-900/50 border border-neutral-800 rounded-xl p-5 space-y-4">
          <h2 className="text-sm font-bold tracking-widest text-neutral-400 uppercase flex items-center gap-2">
            <Zap className="w-4 h-4 text-amber-500" /> Dispatch Controller
          </h2>
          <div className="space-y-3 text-xs">
            <div>
              <label className="block text-neutral-500 font-bold mb-1">SELECT SIMULATED TRAIN THREAD</label>
              <input 
                type="text" value={activeTrain} onChange={(e) => setActiveTrain(e.target.value)}
                className="w-full bg-neutral-950 border border-neutral-800 rounded p-2 focus:outline-none focus:border-amber-500 text-neutral-300"
              />
            </div>
            
            <div className="grid grid-cols-1 gap-2 pt-2">
              <button 
                onClick={requestTrackAccess}
                className="w-full bg-amber-500 hover:bg-amber-400 text-black py-2.5 rounded font-black transition tracking-wide text-xs"
              >
                REQUEST CRITICAL SECTION LOCK
              </button>
              <button 
                onClick={releaseTrackAccess}
                className="w-full bg-neutral-800 hover:bg-neutral-700 text-neutral-200 border border-neutral-700 py-2.5 rounded font-bold transition text-xs"
              >
                RELEASE SYNCHRONIZED TRACK
              </button>
            </div>
          </div>
        </div>

        {/* Database Fact Table Display & Monitor State Block */}
        <div className="lg:col-span-2 bg-neutral-900/20 border border-neutral-800 rounded-xl p-5 flex flex-col justify-between space-y-4">
          
          {/* Active Monitor Status Display Layout */}
          <div className="p-4 border border-neutral-800 bg-neutral-950/40 rounded-lg flex items-center justify-between">
            <div>
              <span className="text-[10px] block font-bold text-neutral-500 uppercase tracking-wider">Current Global Critical Section State</span>
              <h3 className="text-md font-bold mt-1">
                Active Node Allocation: <span className="text-white">{gridState.current_train}</span>
              </h3>
            </div>
            <span className={`px-3 py-1 rounded text-xs font-bold tracking-wide uppercase flex items-center gap-1.5 ${
              gridState.track_occupied ? 'bg-red-950 text-red-400 border border-red-900' : 'bg-emerald-950 text-emerald-400 border border-emerald-900'
            }`}>
              {gridState.track_occupied ? <ShieldAlert className="w-4 h-4"/> : <ShieldCheck className="w-4 h-4"/>}
              {gridState.track_occupied ? 'Track Block Locked' : 'Track Block Free'}
            </span>
          </div>

          <div className="space-y-2">
            <h2 className="text-[11px] font-bold tracking-widest text-neutral-500 uppercase flex items-center gap-2">
              <Database className="w-4 h-4 text-amber-500" /> Relational Database Audit Snapshot History
            </h2>
            <div className="overflow-x-auto">
              <table className="w-full text-left text-[11px] text-neutral-400 border-collapse">
                <thead>
                  <tr className="border-b border-neutral-800 text-neutral-600 font-bold uppercase tracking-wider">
                    <th className="pb-2">Train Identity</th>
                    <th className="pb-2">Concurrency Action</th>
                    <th className="pb-2">Resource State</th>
                    <th className="pb-2">Timestamp Log</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-neutral-900">
                  {gridState.history.map((log, i) => (
                    <tr key={i} className="hover:bg-neutral-900/40 transition">
                      <td className="py-2 text-neutral-200 font-bold">{log.train_id}</td>
                      <td className="py-2 text-amber-400">{log.action}</td>
                      <td className="py-2 font-mono text-neutral-500">{log.state}</td>
                      <td className="py-2 text-neutral-600">{log.time.slice(11, 19)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </div>

      {/* Live Logging Terminal Feed Footer Component */}
      <div className="max-w-6xl mx-auto mt-6 bg-black border border-neutral-800 rounded-xl p-4 h-36 overflow-y-auto text-xs space-y-1 shadow-inner">
        <span className="text-[10px] text-neutral-600 uppercase tracking-widest block font-bold border-b border-neutral-900 pb-1 mb-1">Live Thread Mutex Monitor Inspection Stream</span>
        {terminalFeed.map((log, i) => (
          <p key={i} className={i === 0 ? "text-amber-400 font-bold" : "text-neutral-500"}>
            &gt; {log}
          </p>
        ))}
      </div>
    </div>
  );
}