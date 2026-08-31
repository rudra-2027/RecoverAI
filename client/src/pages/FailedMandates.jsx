import React, { useEffect, useState, useMemo } from 'react';
import { TableSkeletonRows } from '../components/LoadingSkeleton';
import { downloadFailedMandatesCsv, fetchFailedMandates, runAgent, runAllAgents } from '../services/api';

export default function FailedMandates() {
  const normalizeMandate = (item) => ({
    id: item.mandateId || item.id,
    merchant: item.merchantId || 'Unknown Merchant',
    merchantColor: 'bg-primary text-white',
    merchantInitial: (item.merchantId || 'M').charAt(0).toUpperCase(),
    customerName: item.customerId || 'Unknown Customer',
    customerEmail: item.customerId ? `${item.customerId}@recoverai.local` : '',
    amount: Number(item.amount || 0),
    failureReason: item.failureReason || 'Unknown',
    failureType: item.stopReason ? 'Hard Failure' : 'Soft Failure',
    failureTypeColor: item.stopReason ? 'bg-error-container text-on-error-container' : 'bg-secondary-container text-on-secondary-container',
    createdDate: item.failureTimestamp ? new Date(item.failureTimestamp).toLocaleString() : 'Unknown',
    status: item.status || 'FAILED',
    cardDetails: item.failureCode || 'Gateway failure',
    phone: 'Not available',
    retries: item.retryCount || 0,
  });

  const [mandates, setMandates] = useState([]);
  const [isLoading, setIsLoading] = useState(true);

  // Filters State
  const [search, setSearch] = useState('');
  const [selectedMerchant, setSelectedMerchant] = useState('All Merchants');
  const [selectedFailure, setSelectedFailure] = useState('All Reasons');
  const [selectedDate, setSelectedDate] = useState('');
  const [selectedRows, setSelectedRows] = useState([]);
  
  // Modal / Toast State
  const [detailModalItem, setDetailModalItem] = useState(null);
  const [toastMessage, setToastMessage] = useState('');

  useEffect(() => {
    fetchFailedMandates()
      .then((data) => setMandates(data.map(normalizeMandate)))
      .catch(() => showToast('Could not load failed mandates from backend.'))
      .finally(() => setIsLoading(false));
  }, []);

  const merchantOptions = useMemo(
    () => Array.from(new Set(mandates.map((item) => item.merchant).filter(Boolean))).sort(),
    [mandates],
  );

  const failureOptions = useMemo(
    () => Array.from(new Set(mandates.map((item) => item.failureReason).filter(Boolean))).sort(),
    [mandates],
  );

  // Filtering Logic
  const filteredMandates = useMemo(() => {
    return mandates.filter(item => {
      const matchesSearch = 
        item.id.toLowerCase().includes(search.toLowerCase()) ||
        item.merchant.toLowerCase().includes(search.toLowerCase()) ||
        item.customerName.toLowerCase().includes(search.toLowerCase()) ||
        item.customerEmail.toLowerCase().includes(search.toLowerCase());
      
      const matchesMerchant = selectedMerchant === 'All Merchants' || item.merchant === selectedMerchant;
      const matchesFailure = selectedFailure === 'All Reasons' || item.failureReason.toLowerCase().includes(selectedFailure.toLowerCase());
      
      // Simple date matching if needed, else ignore
      const matchesDate = !selectedDate || item.createdDate.includes(selectedDate);

      return matchesSearch && matchesMerchant && matchesFailure && matchesDate;
    });
  }, [mandates, search, selectedMerchant, selectedFailure, selectedDate]);

  // Checkbox select handlers
  const handleSelectAll = (e) => {
    if (e.target.checked) {
      setSelectedRows(filteredMandates.map(item => item.id));
    } else {
      setSelectedRows([]);
    }
  };

  const handleSelectRow = (id) => {
    if (selectedRows.includes(id)) {
      setSelectedRows(selectedRows.filter(rowId => rowId !== id));
    } else {
      setSelectedRows([...selectedRows, id]);
    }
  };

  const showToast = (message) => {
    setToastMessage(message);
    setTimeout(() => setToastMessage(''), 3000);
  };

  const triggerAIAgent = async (id) => {
    try {
      const result = await runAgent(id);
      showToast(`AI agent completed for ${id}: ${result.action} / ${result.outcome}.`);
    } catch {
      showToast(`AI agent could not run for ${id}.`);
    }
  };

  const handleBulkAction = async () => {
    if (selectedRows.length === 0) return;
    try {
      await Promise.all(selectedRows.map((id) => runAgent(id)));
      showToast(`Bulk processing completed for ${selectedRows.length} mandates.`);
      setSelectedRows([]);
    } catch {
      showToast('Bulk processing failed for one or more mandates.');
    }
  };

  const handleRunAll = async () => {
    try {
      const result = await runAllAgents();
      showToast(`Batch completed: ${result.successfulRecoveries}/${result.totalProcessed} recovered.`);
    } catch {
      showToast('Batch agent could not run.');
    }
  };

  const handleExport = async () => {
    try {
      await downloadFailedMandatesCsv();
      showToast('Failed mandates CSV downloaded.');
    } catch {
      showToast('Could not export failed mandates.');
    }
  };

  const clearFilters = () => {
    setSearch('');
    setSelectedMerchant('All Merchants');
    setSelectedFailure('All Reasons');
    setSelectedDate('');
    setSelectedRows([]);
  };

  return (
    <div className="flex-1 p-container-padding flex flex-col gap-stack-lg overflow-y-auto pb-20">
      {/* Toast Alert */}
      {toastMessage && (
        <div className="fixed bottom-6 right-6 z-50 bg-inverse-surface text-inverse-on-surface px-6 py-3 rounded-lg shadow-level2 flex items-center gap-2 border border-outline-variant animate-fade-in">
          <span className="material-symbols-outlined fill text-[#8a2be2]">auto_awesome</span>
          <span className="font-semibold text-body-md">{toastMessage}</span>
        </div>
      )}

      {/* Page Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
        <div>
          <h2 className="font-headline-lg text-headline-lg text-on-surface m-0 mb-1">Failed Mandates</h2>
          <p className="font-body-md text-body-md text-on-surface-variant m-0">Monitor and triage unsuccessful recurring payment attempts in real-time.</p>
        </div>
        <div className="flex gap-3 w-full sm:w-auto">
          <button 
            onClick={handleExport}
            className="flex-1 sm:flex-none px-4 py-2 bg-surface text-primary border border-outline-variant rounded font-label-md text-label-md hover:bg-surface-container transition-colors shadow-sm"
          >
            Export CSV
          </button>
          <button 
            onClick={handleRunAll}
            className="flex-1 sm:flex-none px-4 py-2 bg-primary text-on-primary rounded font-label-md text-label-md hover:bg-primary-container transition-colors shadow-sm flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-[18px]">play_arrow</span>
            Run Batch Agent
          </button>
        </div>
      </div>

      {/* Filters Section (Bento Style) */}
      <div className="bg-surface p-4 rounded-lg border border-outline-variant/50 shadow-level1 flex flex-wrap gap-4 items-end">
        {/* Search */}
        <div className="flex-1 min-w-[240px]">
          <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Search Mandates</label>
          <div className="flex items-center bg-surface border border-outline-variant rounded px-3 py-1.5 focus-within:border-primary focus-within:ring-1 focus-within:ring-primary">
            <span className="material-symbols-outlined text-on-surface-variant mr-2 text-sm">search</span>
            <input 
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="bg-transparent border-none outline-none w-full font-body-sm text-body-sm text-on-surface placeholder-on-surface-variant focus:ring-0 p-0" 
              placeholder="ID, customer name, email..." 
              type="text"
            />
          </div>
        </div>

        {/* Merchant Select */}
        <div className="min-w-[180px] w-full sm:w-auto">
          <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Merchant</label>
          <div className="relative">
            <select 
              value={selectedMerchant}
              onChange={(e) => setSelectedMerchant(e.target.value)}
              className="w-full bg-surface border border-outline-variant rounded px-3 py-2 font-body-sm text-body-sm text-on-surface appearance-none focus:border-primary focus:ring-1 focus:ring-primary pr-8"
            >
              <option>All Merchants</option>
              {merchantOptions.map((merchant) => (
                <option key={merchant}>{merchant}</option>
              ))}
            </select>
            <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">expand_more</span>
          </div>
        </div>

        {/* Failure Reason Select */}
        <div className="min-w-[180px] w-full sm:w-auto">
          <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Failure Reason</label>
          <div className="relative">
            <select 
              value={selectedFailure}
              onChange={(e) => setSelectedFailure(e.target.value)}
              className="w-full bg-surface border border-outline-variant rounded px-3 py-2 font-body-sm text-body-sm text-on-surface appearance-none focus:border-primary focus:ring-1 focus:ring-primary pr-8"
            >
              <option>All Reasons</option>
              {failureOptions.map((reason) => (
                <option key={reason}>{reason}</option>
              ))}
            </select>
            <span className="material-symbols-outlined absolute right-2 top-1/2 -translate-y-1/2 text-on-surface-variant pointer-events-none">expand_more</span>
          </div>
        </div>

        {/* Date Selector */}
        <div className="min-w-[160px] w-full sm:w-auto">
          <label className="block font-label-md text-label-md text-on-surface-variant mb-1">Date Segment</label>
          <input 
            value={selectedDate}
            onChange={(e) => setSelectedDate(e.target.value)}
            className="w-full bg-surface border border-outline-variant rounded px-3 py-1.5 font-body-sm text-body-sm text-on-surface focus:border-primary focus:ring-1 focus:ring-primary" 
            type="text"
            placeholder="e.g. Oct 24"
          />
        </div>

        {/* Action buttons */}
        <div className="flex gap-2 w-full sm:w-auto self-stretch items-end">
          <button 
            onClick={clearFilters}
            className="flex-1 sm:flex-none h-[38px] px-4 bg-surface-container-high text-on-surface rounded font-label-md text-label-md hover:bg-surface-variant border border-outline-variant/50 transition-colors"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Bulk Action Panel */}
      {selectedRows.length > 0 && (
        <div className="bg-primary-container/20 border border-primary/20 rounded-lg p-3 flex justify-between items-center animate-fade-in">
          <span className="font-label-md text-label-md text-on-primary-container">
            {selectedRows.length} mandates selected
          </span>
          <button 
            onClick={handleBulkAction}
            className="px-4 py-1.5 bg-primary text-on-primary rounded text-xs font-bold hover:bg-primary-container transition-colors shadow-sm"
          >
            Bulk Run AI Agent
          </button>
        </div>
      )}

      {/* Data Table Container */}
      <div className="bg-surface rounded-lg border border-outline-variant/40 shadow-level1 overflow-hidden flex flex-col">
        <div className="overflow-x-auto">
          <table className="w-full border-collapse text-left">
            <thead>
              <tr className="border-b border-outline-variant/40 bg-surface-container-low">
                <th className="p-4 w-12">
                  <input 
                    type="checkbox" 
                    onChange={handleSelectAll} 
                    checked={filteredMandates.length > 0 && selectedRows.length === filteredMandates.length}
                    className="rounded border-outline-variant text-primary focus:ring-primary w-4 h-4"
                  />
                </th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Mandate ID</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Merchant</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Customer</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider text-right">Amount</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Failure Details</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Created</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Status</th>
                <th className="p-4 font-label-md text-label-md text-on-surface-variant uppercase tracking-wider text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="font-body-sm text-body-sm divide-y divide-outline-variant/20">
              {isLoading ? (
                <TableSkeletonRows rows={6} columns={9} />
              ) : filteredMandates.length === 0 ? (
                <tr>
                  <td colSpan="9" className="p-8 text-center text-on-surface-variant">
                    No failed mandates match the active filter criteria.
                  </td>
                </tr>
              ) : (
                filteredMandates.map((item) => (
                  <tr key={item.id} className="hover:bg-surface-container-lowest transition-colors group">
                    <td className="p-4">
                      <input 
                        type="checkbox"
                        checked={selectedRows.includes(item.id)}
                        onChange={() => handleSelectRow(item.id)}
                        className="rounded border-outline-variant text-primary focus:ring-primary w-4 h-4"
                      />
                    </td>
                    <td className="p-4 font-code text-code text-on-surface-variant">{item.id}</td>
                    <td className="p-4">
                      <div className="flex items-center gap-2">
                        <div className={`w-6 h-6 rounded flex items-center justify-center font-title-md text-title-md ${item.merchantColor}`}>
                          {item.merchantInitial}
                        </div>
                        <span className="font-title-md text-title-md text-on-surface">{item.merchant}</span>
                      </div>
                    </td>
                    <td className="p-4">
                      <div className="flex flex-col">
                        <span className="font-title-md text-title-md text-on-surface">{item.customerName}</span>
                        <span className="text-on-surface-variant">{item.customerEmail}</span>
                      </div>
                    </td>
                    <td className="p-4 text-right font-code text-code text-on-surface">${item.amount.toFixed(2)}</td>
                    <td className="p-4">
                      <div className="flex flex-col gap-1 items-start">
                        <span className="bg-surface-container text-on-surface px-2 py-0.5 rounded text-[11px] font-semibold">
                          {item.failureReason}
                        </span>
                        <span className={`px-2 py-0.5 rounded text-[10px] font-semibold uppercase tracking-wider ${item.failureTypeColor}`}>
                          {item.failureType}
                        </span>
                      </div>
                    </td>
                    <td className="p-4 text-on-surface-variant">{item.createdDate}</td>
                    <td className="p-4">
                      <div className="flex items-center gap-1 text-error">
                        <span className="material-symbols-outlined text-[16px]">cancel</span>
                        <span className="font-label-md text-label-md">Failed</span>
                      </div>
                    </td>
                    <td className="p-4 text-right">
                      <div className="flex justify-end gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                        <button 
                          onClick={() => setDetailModalItem(item)}
                          className="p-1.5 text-on-surface-variant hover:text-primary hover:bg-primary-fixed rounded transition-colors" 
                          title="View Details"
                        >
                          <span className="material-symbols-outlined text-[18px]">visibility</span>
                        </button>
                        <button 
                          onClick={() => triggerAIAgent(item.id)}
                          className="p-1.5 text-primary hover:bg-primary-fixed rounded transition-colors flex items-center" 
                          title="Run AI Agent"
                        >
                          <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Footer */}
        <div className="flex items-center justify-between px-4 py-3 border-t border-outline-variant/30 bg-surface">
          <span className="font-body-sm text-body-sm text-on-surface-variant">
            {isLoading ? 'Loading mandates...' : `Showing ${filteredMandates.length} of ${mandates.length} mandates`}
          </span>
          <div className="flex gap-1">
            <button className="p-1 text-outline-variant hover:text-on-surface disabled:opacity-50" disabled>
              <span className="material-symbols-outlined">chevron_left</span>
            </button>
            <button className="w-8 h-8 rounded bg-primary-fixed text-primary font-label-md text-label-md flex items-center justify-center">1</button>
            <button className="p-1 text-outline-variant hover:text-on-surface" disabled>
              <span className="material-symbols-outlined">chevron_right</span>
            </button>
          </div>
        </div>
      </div>

      {/* Details Modal */}
      {detailModalItem && (
        <div className="fixed inset-0 bg-inverse-surface/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-xl border border-outline-variant/40 shadow-level2 w-full max-w-lg overflow-hidden animate-zoom-in">
            <div className="px-6 py-4 bg-surface-container-high border-b border-outline-variant/40 flex justify-between items-center">
              <h3 className="font-title-lg text-title-lg text-on-surface">Mandate Details</h3>
              <button 
                onClick={() => setDetailModalItem(null)}
                className="text-on-surface-variant hover:text-on-surface p-1 rounded hover:bg-surface-container"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-6 space-y-4 font-body-md text-body-md text-on-surface">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">MANDATE ID</span>
                  <span className="font-code text-code">{detailModalItem.id}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">MERCHANT</span>
                  <span className="font-semibold">{detailModalItem.merchant}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">CUSTOMER</span>
                  <span className="font-semibold">{detailModalItem.customerName}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">EMAIL</span>
                  <span>{detailModalItem.customerEmail}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">PHONE</span>
                  <span>{detailModalItem.phone}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">AMOUNT</span>
                  <span className="font-bold text-primary">${detailModalItem.amount.toFixed(2)}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">METHOD</span>
                  <span>{detailModalItem.cardDetails}</span>
                </div>
                <div>
                  <span className="block font-label-md text-label-md text-on-surface-variant">RETRIES ATTEMPTED</span>
                  <span>{detailModalItem.retries} times</span>
                </div>
              </div>
              <div className="border-t border-outline-variant/30 pt-4">
                <span className="block font-label-md text-label-md text-on-surface-variant">FAILURE SUMMARY</span>
                <div className="mt-2 p-3 bg-surface-container rounded border border-outline-variant/20 flex flex-col gap-1">
                  <span className="font-semibold text-error">{detailModalItem.failureReason}</span>
                  <span className="text-body-sm text-on-surface-variant">
                    Failure category: <strong>{detailModalItem.failureType}</strong>. AI recovery engine suggests retry optimization based on merchant rules.
                  </span>
                </div>
              </div>
            </div>
            <div className="px-6 py-4 bg-surface-container border-t border-outline-variant/40 flex justify-end gap-2">
              <button 
                onClick={() => setDetailModalItem(null)}
                className="px-4 py-2 border border-outline-variant/50 text-on-surface rounded font-label-md text-label-md hover:bg-surface-container transition-colors"
              >
                Close
              </button>
              <button 
                onClick={() => {
                  triggerAIAgent(detailModalItem.id);
                  setDetailModalItem(null);
                }}
                className="px-4 py-2 bg-primary text-on-primary rounded font-label-md text-label-md hover:bg-primary-container transition-colors flex items-center gap-1"
              >
                <span className="material-symbols-outlined text-[16px]">auto_awesome</span>
                Run AI Agent
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
