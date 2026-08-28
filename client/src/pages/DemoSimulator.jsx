import React, { useState, useEffect, useRef } from 'react';
import { simulateRecovery } from '../services/api';

export default function DemoSimulator() {
  const [merchant, setMerchant] = useState('TechCorp Subscriptions');
  const [custId, setCustId] = useState('CUST-88392-AB');
  const [amount, setAmount] = useState('1499.00');
  const [retryCount, setRetryCount] = useState('0');
  const [failureReason, setFailureReason] = useState('INSUFFICIENT_BALANCE');
  
  // Simulation State
  const [isRunning, setIsRunning] = useState(false);
  const [simStarted, setSimStarted] = useState(false);
  const [activeNode, setActiveNode] = useState(null); // 1, 2, 3, 4
  const [probScore, setProbScore] = useState('--%');
  const [selectedStrategy, setSelectedStrategy] = useState('--');
  const [estimatedTat, setEstimatedTat] = useState('--');
  const [logs, setLogs] = useState([]);
  
  const consoleEndRef = useRef(null);

  useEffect(() => {
    if (consoleEndRef.current) {
      consoleEndRef.current.scrollIntoView({ behavior: 'smooth' });
    }
  }, [logs]);

  const runSimulation = async () => {
    setIsRunning(true);
    setSimStarted(true);
    setLogs([]);
    setActiveNode(1);
    
    // Step 1: Failure Detected
    addLog('System', `Failure alert received: ${failureReason} from Gateway Connector.`);
    addLog('System', `Processing payload - Customer: ${custId}, Amount: INR ${amount}, Retries: ${retryCount}`);

    const mandateId = `SIM-${Date.now()}`;

    try {
      await new Promise((resolve) => setTimeout(resolve, 1000));
      setActiveNode(2);
      addLog('AI Engine', `Analyzing payment history profile for ${custId}...`);

      const response = await simulateRecovery({
        merchantId: merchant.replace(/\s+/g, '_').toUpperCase(),
        customerId: custId,
        mandateId,
        amount,
        failureReason,
        failureCode: failureReason,
        retryCount: Number(retryCount || 0),
      });
      const result = response.result;
      addLog('AI Engine', `Running predictive scoring. Confidence profile: ${result.recoverabilityScore}% success rate.`);
      setProbScore(`${result.recoverabilityScore}%`);
      setSelectedStrategy(result.action);
      setEstimatedTat(result.action === 'RETRY' ? 'Queued' : 'N/A');

      await new Promise((resolve) => setTimeout(resolve, 1000));
      setActiveNode(3);
      addLog('AI Engine', `Routing decision finalized: ${result.action}`);
      addLog('System', `Configuring automated execution webhook target in queue.`);

      await new Promise((resolve) => setTimeout(resolve, 1000));
      setActiveNode(4);
      addLog('Gateway', `Backend recovery outcome: ${result.outcome}.`);
      addLog('System', `Simulation completed successfully.`);
    } catch {
      addLog('System', 'Backend simulation failed. Check API server, DB, and environment variables.');
    } finally {
      setIsRunning(false);
    }
  };

  const addLog = (source, message) => {
    const time = new Date().toLocaleTimeString();
    setLogs(prev => [...prev, { time, source, message }]);
  };

  return (
    <div className="flex-1 p-container-padding overflow-y-auto bg-surface-container-lowest pb-20">
      <div className="max-w-6xl mx-auto space-y-stack-lg">
        
        {/* Page Header */}
        <div>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">Demo Simulator</h2>
          <p className="font-body-md text-body-md text-on-surface-variant mt-2">Simulate UPI AutoPay failures and observe real-time AI-driven recovery workflows.</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-12 gap-stack-lg">
          {/* Simulator Form (Left) */}
          <div className="lg:col-span-4 bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-6">
            <h3 className="font-title-lg text-title-lg text-on-surface mb-6 flex items-center gap-2">
              <span className="material-symbols-outlined text-primary fill">bolt</span>
              Simulate UPI AutoPay Failure
            </h3>
            
            <div className="space-y-4">
              <div>
                <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Merchant</label>
                <select 
                  value={merchant}
                  onChange={(e) => setMerchant(e.target.value)}
                  className="w-full bg-surface border border-outline-variant rounded-lg px-3 py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary pr-8 appearance-none"
                >
                  <option>TechCorp Subscriptions</option>
                  <option>StreamFlix Media</option>
                  <option>Global SaaS Ltd.</option>
                </select>
              </div>
              
              <div>
                <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Customer Identifier</label>
                <input 
                  value={custId}
                  onChange={(e) => setCustId(e.target.value)}
                  className="w-full bg-surface border border-outline-variant rounded-lg px-3 py-2 font-code text-code text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary" 
                  type="text"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Amount (INR)</label>
                  <input 
                    value={amount}
                    onChange={(e) => setAmount(e.target.value)}
                    className="w-full bg-surface border border-outline-variant rounded-lg px-3 py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary" 
                    type="number"
                  />
                </div>
                <div>
                  <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Retry Count</label>
                  <input 
                    value={retryCount}
                    onChange={(e) => setRetryCount(e.target.value)}
                    className="w-full bg-surface border border-outline-variant rounded-lg px-3 py-2 font-body-md text-body-md text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary" 
                    type="number"
                  />
                </div>
              </div>

              <div>
                <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Failure Reason Code</label>
                <select 
                  value={failureReason}
                  onChange={(e) => setFailureReason(e.target.value)}
                  className="w-full bg-surface border border-outline-variant rounded-lg px-3 py-2 font-code text-code text-on-surface focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary pr-8 appearance-none"
                >
                  <option value="INSUFFICIENT_BALANCE">INSUFFICIENT_BALANCE</option>
                  <option value="BANK_SERVER_DOWN">BANK_SERVER_DOWN</option>
                  <option value="MANDATE_EXPIRED">MANDATE_EXPIRED</option>
                  <option value="LIMIT_EXCEEDED">LIMIT_EXCEEDED</option>
                </select>
              </div>

              <button 
                onClick={runSimulation}
                disabled={isRunning}
                className="w-full mt-6 bg-primary text-on-primary font-title-md text-title-md py-3 rounded-lg hover:bg-on-primary-fixed-variant transition-colors flex items-center justify-center gap-2 shadow-sm disabled:opacity-50" 
                type="button"
              >
                <span className="material-symbols-outlined">{isRunning ? 'sync' : 'play_arrow'}</span>
                {isRunning ? 'Simulating...' : 'Simulate Recovery'}
              </button>
            </div>
          </div>

          {/* Visual Flow & Metrics (Right) */}
          <div className="lg:col-span-8 space-y-stack-lg">
            {/* Top Metrics */}
            <div className="grid grid-cols-3 gap-4">
              <div className="bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-4 flex flex-col justify-between">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase font-semibold">Probability Score</span>
                <div className="mt-2 flex items-baseline gap-2">
                  <span className="font-headline-md text-headline-md text-primary font-bold">{probScore}</span>
                  {simStarted && <span className="font-body-sm text-body-sm text-outline">Confidence</span>}
                </div>
              </div>
              
              <div className="bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-4 flex flex-col justify-between">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase font-semibold">Selected Strategy</span>
                <div className="mt-2 flex items-baseline">
                  <span className="font-title-md text-title-md text-on-surface font-bold truncate max-w-full" title={selectedStrategy}>
                    {selectedStrategy}
                  </span>
                </div>
              </div>
              
              <div className="bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-4 flex flex-col justify-between">
                <span className="font-label-md text-label-md text-on-surface-variant uppercase font-semibold">Estimated TAT</span>
                <div className="mt-2 flex items-baseline gap-1">
                  <span className="font-headline-md text-headline-md text-on-surface font-bold">{estimatedTat}</span>
                  {simStarted && !['N/A', 'Manual Review Required', 'Queued'].includes(estimatedTat) && (
                    <span className="font-body-sm text-body-sm text-outline">mins</span>
                  )}
                </div>
              </div>
            </div>

            {/* Workflow Canvas */}
            <div className="bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-8 min-h-[220px] relative overflow-hidden flex flex-col justify-center">
              {/* Connecting Line SVG */}
              <svg className="absolute inset-0 w-full h-full pointer-events-none" style={{ zIndex: 0 }}>
                <path 
                  className={`stroke-[#c4c5d8] ${isRunning ? 'flow-line' : ''}`}
                  d="M 60 110 L 180 110 L 300 110 L 420 110 L 540 110" 
                  fill="none" 
                  strokeWidth="2"
                  strokeDasharray={isRunning ? '5' : 'none'}
                />
              </svg>
              
              <div className="relative z-10 flex justify-between items-center w-full flex-wrap gap-4">
                {/* Node 1 */}
                <div className="flex flex-col items-center gap-2 w-24">
                  <div className={`w-12 h-12 rounded-full flex items-center justify-center border transition-all ${
                    activeNode === 1 ? 'bg-error-container text-on-error-container border-error shadow-[0_0_15px_#ba1a1a]' : 'bg-surface-container-high text-outline border-outline-variant/50'
                  }`}>
                    <span className="material-symbols-outlined">cancel</span>
                  </div>
                  <span className="font-label-md text-label-md text-center text-on-surface-variant leading-tight">Failure<br/>Detected</span>
                </div>

                {/* Node 2 */}
                <div className="flex flex-col items-center gap-2 w-24">
                  <div className={`w-12 h-12 rounded-full flex items-center justify-center border transition-all ${
                    activeNode === 2 ? 'ai-sparkle text-[#8a2be2] shadow-[0_0_15px_#8a2be2]' : 'bg-surface-container-high text-outline border-outline-variant/50'
                  }`}>
                    <span className="material-symbols-outlined">memory</span>
                  </div>
                  <span className="font-label-md text-label-md text-center text-on-surface-variant leading-tight">AI Analysis<br/>&amp; Scoring</span>
                </div>

                {/* Node 3 */}
                <div className="flex flex-col items-center gap-2 w-24">
                  <div className={`w-12 h-12 rounded-full flex items-center justify-center border transition-all ${
                    activeNode === 3 ? 'bg-secondary-container text-on-secondary-container border-secondary shadow-[0_0_15px_#4d6077]' : 'bg-surface-container-high text-outline border-outline-variant/50'
                  }`}>
                    <span className="material-symbols-outlined">call_split</span>
                  </div>
                  <span className="font-label-md text-label-md text-center text-on-surface-variant leading-tight">Routing<br/>Decision</span>
                </div>

                {/* Node 4 */}
                <div className="flex flex-col items-center gap-2 w-24">
                  <div className={`w-12 h-12 rounded-full flex items-center justify-center border transition-all ${
                    activeNode === 4 ? 'bg-green-100 text-green-700 border-green-500 shadow-[0_0_15px_green]' : 'bg-surface-container-high text-outline border-outline-variant/50'
                  }`}>
                    <span className="material-symbols-outlined">account_balance</span>
                  </div>
                  <span className="font-label-md text-label-md text-center text-on-surface-variant leading-tight">Payment<br/>Gateway</span>
                </div>
              </div>

              {/* Placeholder Overlay */}
              {!simStarted && (
                <div className="absolute inset-0 bg-surface/90 backdrop-blur-[2px] flex items-center justify-center z-20">
                  <p className="font-title-md text-title-md text-outline text-center p-4">Configure parameters and press 'Simulate Recovery' to visualize flow.</p>
                </div>
              )}
            </div>

            {/* Console Log Terminal */}
            {simStarted && (
              <div className="bg-inverse-surface text-inverse-on-surface rounded-xl border border-outline-variant shadow-level2 overflow-hidden flex flex-col h-64 font-code text-code">
                <div className="px-4 py-2 bg-on-surface-variant/20 border-b border-outline/30 flex justify-between items-center text-xs text-outline-variant select-none">
                  <span>SYSTEM PROCESS CONSOLE (LIVE)</span>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-red-500"></span>
                    <span className="w-2.5 h-2.5 rounded-full bg-yellow-500"></span>
                    <span className="w-2.5 h-2.5 rounded-full bg-green-500"></span>
                  </div>
                </div>
                <div className="p-4 flex-grow overflow-y-auto space-y-2 select-text chat-scroll">
                  {logs.map((log, i) => (
                    <div key={i} className="flex gap-2">
                      <span className="text-[#a855f7] select-none">[{log.time}]</span>
                      <span className={`${log.source === 'AI Engine' ? 'text-primary-fixed-dim' : log.source === 'Gateway' ? 'text-green-400' : 'text-outline-variant'} font-bold`}>
                        [{log.source.toUpperCase()}]
                      </span>
                      <span>{log.message}</span>
                    </div>
                  ))}
                  {isRunning && (
                    <div className="text-primary-fixed-dim animate-pulse">Running process threads...</div>
                  )}
                  <div ref={consoleEndRef} />
                </div>
              </div>
            )}

          </div>
        </div>

      </div>
    </div>
  );
}
