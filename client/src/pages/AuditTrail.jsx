import React, { useEffect, useState } from 'react';
import { SkeletonBlock } from '../components/LoadingSkeleton';
import { downloadAuditCsv, fetchAuditLogs, fetchFailedMandates } from '../services/api';

export default function AuditTrail() {
  const [selectedMandate, setSelectedMandate] = useState('');
  const [expandedEvents, setExpandedEvents] = useState({});
  const [filterSource, setFilterSource] = useState('ALL'); // ALL, AI, GW
  const [toast, setToast] = useState('');
  const [backendMandates, setBackendMandates] = useState([]);
  const [backendLogs, setBackendLogs] = useState([]);
  const [isLoadingMandates, setIsLoadingMandates] = useState(true);
  const [isLoadingLogs, setIsLoadingLogs] = useState(false);

  const normalizeLog = (log) => ({
    id: String(log.id),
    time: log.createdAt ? new Date(log.createdAt).toLocaleTimeString() : '',
    date: log.createdAt ? new Date(log.createdAt).toLocaleDateString() : '',
    title: log.stage || 'AUDIT_EVENT',
    icon: log.stage === 'PROBABILITY' ? 'auto_awesome' : log.stage === 'INGESTION' ? 'error' : 'analytics',
    colorClass: log.stage === 'PROBABILITY' ? 'border-primary text-primary' : 'border-outline text-outline',
    dotColor: log.stage === 'PROBABILITY' ? 'border-primary' : 'border-outline',
    isAi: ['ANALYSIS', 'PROBABILITY', 'DECISION'].includes(log.stage),
    description: log.message || 'Backend audit event',
    payload: log,
  });

  useEffect(() => {
    fetchFailedMandates()
      .then((data) => {
        const ids = data.map((item) => item.mandateId).filter(Boolean);
        setBackendMandates(ids);
        if (ids.length > 0) {
          setSelectedMandate(ids[0]);
        }
      })
      .catch(() => {})
      .finally(() => setIsLoadingMandates(false));
  }, []);

  useEffect(() => {
    if (!selectedMandate) return;
    setIsLoadingLogs(true);
    fetchAuditLogs(selectedMandate)
      .then((data) => setBackendLogs(data.map(normalizeLog)))
      .catch(() => setBackendLogs([]))
      .finally(() => setIsLoadingLogs(false));
  }, [selectedMandate]);

  const logs = backendLogs;
  const mandateOptions = backendMandates;

  const filteredLogs = logs.filter(log => {
    if (filterSource === 'ALL') return true;
    if (filterSource === 'AI') return log.isAi;
    if (filterSource === 'GW') return log.title === 'FAILURE_RECEIVED';
    return true;
  });

  const toggleEvent = (id) => {
    setExpandedEvents(prev => ({
      ...prev,
      [id]: !prev[id]
    }));
  };

  const triggerExport = async () => {
    try {
      await downloadAuditCsv(selectedMandate);
      setToast(`Audit log downloaded for ${selectedMandate}.`);
    } catch {
      setToast(`Could not export audit log for ${selectedMandate}.`);
    }
    setTimeout(() => setToast(''), 3000);
  };

  return (
    <div className="flex-1 p-container-padding overflow-y-auto pb-20">
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 bg-inverse-surface text-inverse-on-surface px-6 py-3 rounded-lg shadow-level2 border border-outline-variant flex items-center gap-2 border-primary/20 animate-fade-in">
          <span className="material-symbols-outlined fill text-green-500">check_circle</span>
          <span className="font-semibold">{toast}</span>
        </div>
      )}

      <div className="max-w-4xl mx-auto">
        {/* Page Header */}
        <div className="mb-stack-lg flex flex-col sm:flex-row items-start sm:items-end justify-between gap-4">
          <div>
            <h2 className="font-headline-lg text-headline-lg text-on-surface mb-unit">Mandate Audit Trail</h2>
            <div className="flex items-center gap-2 mt-1">
              <span className="material-symbols-outlined text-sm text-secondary">receipt_long</span>
              <span className="font-body-md text-body-md text-on-surface-variant">Active Mandate:</span>
              <select 
                value={selectedMandate}
                onChange={(e) => setSelectedMandate(e.target.value)}
                className="bg-surface-container-highest border border-outline-variant rounded px-2 py-0.5 font-code text-code text-on-surface font-semibold focus:outline-none focus:border-primary cursor-pointer"
              >
                {isLoadingMandates && <option value="">Loading mandates</option>}
                {!isLoadingMandates && mandateOptions.length === 0 && <option value="">No mandates</option>}
                {mandateOptions.map((mandateId) => (
                  <option key={mandateId} value={mandateId}>{mandateId}</option>
                ))}
              </select>
            </div>
          </div>
          
          <div className="flex gap-2 w-full sm:w-auto">
            {/* Filter */}
            <select 
              value={filterSource} 
              onChange={(e) => setFilterSource(e.target.value)}
              className="px-3 py-2 border border-outline-variant rounded-lg font-label-md text-label-md bg-surface text-on-surface focus:outline-none"
            >
              <option value="ALL">All Events</option>
              <option value="AI">AI Engines Only</option>
              <option value="GW">Gateway Alerts Only</option>
            </select>
            
            <button 
              onClick={triggerExport}
              className="px-4 py-2 bg-surface border border-outline-variant text-on-surface hover:bg-surface-container-highest font-label-md text-label-md rounded-lg transition-colors flex items-center justify-center gap-2 shadow-sm whitespace-nowrap"
            >
              <span className="material-symbols-outlined text-sm">download</span>
              Export Log
            </button>
          </div>
        </div>

        {/* Timeline Surface */}
        <div className="bg-surface border border-outline-variant/40 rounded-xl shadow-level1 p-stack-lg relative">
          
          {isLoadingLogs ? (
            <div className="relative pl-6">
              <div className="absolute left-[7px] top-4 bottom-4 w-px bg-outline-variant opacity-50"></div>
              {Array.from({ length: 4 }).map((_, index) => (
                <div key={`timeline-skeleton-${index}`} className="relative mb-stack-lg last:mb-0">
                  <div className="absolute -left-[30px] top-4 w-[14px] h-[14px] rounded-full bg-surface border-2 border-outline-variant z-10 ring-4 ring-surface"></div>
                  <div className="flex flex-col md:flex-row items-start gap-4">
                    <div className="w-24 pt-3 space-y-2">
                      <SkeletonBlock className="h-4 w-16" />
                      <SkeletonBlock className="h-3 w-20" />
                    </div>
                    <div className="flex-grow w-full border rounded-lg bg-surface-container-lowest border-outline-variant/30 p-4 space-y-3">
                      <SkeletonBlock className="h-5 w-44" />
                      <SkeletonBlock className="h-4 w-full" />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : filteredLogs.length === 0 ? (
            <div className="text-center py-8 text-on-surface-variant">
              No timeline items match the selected event source filters.
            </div>
          ) : (
            <div className="relative pl-6">
              {/* Vertical Line */}
              <div className="absolute left-[7px] top-4 bottom-4 w-px bg-outline-variant opacity-50"></div>

              {filteredLogs.map((log) => {
                const isExpanded = expandedEvents[log.id];
                return (
                  <div key={log.id} className="relative mb-stack-lg last:mb-0">
                    
                    {/* Circle Node */}
                    <div className={`absolute -left-[30px] top-4 w-[14px] h-[14px] rounded-full bg-surface border-2 ${log.dotColor} z-10 ring-4 ring-surface`}></div>
                    
                    <div className="flex flex-col md:flex-row items-start gap-4">
                      {/* Time and Date */}
                      <div className="w-24 pt-3 flex-shrink-0 text-left md:text-right">
                        <p className="font-label-md text-label-md text-on-surface">{log.time}</p>
                        <p className="font-body-sm text-body-sm text-outline mt-0.5">{log.date}</p>
                      </div>

                      {/* Content Card */}
                      <div className={`flex-grow w-full border rounded-lg shadow-sm overflow-hidden bg-surface-container-lowest ${log.isAi ? 'border-primary-container/30' : 'border-outline-variant/30'}`}>
                        
                        {/* Card Header (clickable) */}
                        <div 
                          onClick={() => toggleEvent(log.id)}
                          className="px-4 py-3 cursor-pointer flex justify-between items-center hover:bg-surface-container-low/40 transition-colors"
                        >
                          <div className="flex items-center gap-2 flex-wrap">
                            <span className={`material-symbols-outlined ${log.colorClass} text-lg`}>
                              {log.icon}
                            </span>
                            <h3 className={`font-title-md text-title-md ${log.isAi ? 'text-primary' : 'text-on-surface'}`}>
                              {log.title}
                            </h3>
                            {log.isAi && (
                              <span className="bg-primary-container text-on-primary font-label-md px-2 py-0.5 rounded-full text-[10px] uppercase font-bold tracking-wider">
                                ML Engine
                              </span>
                            )}
                          </div>
                          <span className={`material-symbols-outlined text-outline-variant transition-transform duration-200 ${isExpanded ? 'rotate-180' : ''}`}>
                            expand_more
                          </span>
                        </div>

                        {/* Card Body (expandable) */}
                        {isExpanded && (
                          <div className="px-4 pb-4 border-t border-outline-variant/20 bg-surface-container-low/20">
                            <p className="font-body-sm text-body-sm text-on-surface-variant mb-2 mt-2">
                              {log.description}
                            </p>
                            <pre className="bg-inverse-surface text-inverse-on-surface p-3 rounded font-code text-code overflow-x-auto">
                              <code>{JSON.stringify(log.payload, null, 2)}</code>
                            </pre>
                          </div>
                        )}

                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}

        </div>
      </div>
    </div>
  );
}
