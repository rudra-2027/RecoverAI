import React, { useEffect, useState, useMemo } from 'react';
import { ButtonLoader, MetricCardSkeleton, SkeletonBlock, TableSkeletonRows } from '../components/LoadingSkeleton';
import { downloadBatchReport, fetchBatches, runAllAgents, uploadBatch } from '../services/api';

export default function BatchProcessing() {
  const [batches, setBatches] = useState([]);
  const [selectedBatchId, setSelectedBatchId] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadStep, setUploadStep] = useState('idle'); // idle, uploading, processing, done
  const [toast, setToast] = useState('');
  const [isLoading, setIsLoading] = useState(true);
  const [isRunningAll, setIsRunningAll] = useState(false);
  const [isExporting, setIsExporting] = useState(false);

  const percentChange = (current, previous) => {
    if (!previous && !current) return 0;
    if (!previous) return current > 0 ? 100 : 0;
    return ((current - previous) / previous) * 100;
  };

  const normalizeBatch = (item) => {
    const total = item.totalMandates || 0;
    const recovered = item.successfulRecoveries || 0;
    const failed = item.failedRecoveries || 0;
    const percentNum = total > 0 ? (recovered * 100) / total : 0;
    const revenueNum = Number(item.recoveredRevenue || 0);

    return {
      id: `BAT-${item.id}`,
      createdDate: item.startedAt ? new Date(item.startedAt).toLocaleString() : 'Unknown',
      startedAt: item.startedAt || null,
      mandatesCount: total,
      recoveredCount: recovered,
      failedCount: failed,
      percentage: total > 0 ? `${percentNum.toFixed(1)}%` : '--',
      percentNum,
      revenue: revenueNum > 0 ? `$${revenueNum.toLocaleString()}` : '--',
      revenueNum,
      avgTicket: recovered > 0 ? `$${(revenueNum / recovered).toFixed(2)}` : '--',
      time: item.completedAt ? 'Completed' : 'In Progress',
      status: item.completedAt ? 'Completed' : 'Processing',
      statusClass: item.completedAt ? 'text-primary bg-primary-container/20' : 'text-secondary bg-secondary-container/50',
      dotClass: item.completedAt ? 'bg-primary' : 'bg-secondary animate-pulse',
      aiSummary: `Backend batch processed ${total} mandates with ${recovered} successful recoveries and ${failed} failed recoveries.`,
    };
  };

  const loadBatches = () => {
    setIsLoading(true);
    fetchBatches()
      .then((data) => {
        if (data.length > 0) {
          const normalized = data.map(normalizeBatch);
          setBatches(normalized);
          setSelectedBatchId(normalized[0].id);
        } else {
          setBatches([]);
          setSelectedBatchId('');
        }
      })
      .catch(() => showToast('Could not load backend batches.'))
      .finally(() => setIsLoading(false));
  };

  const backendBatchId = (id) => {
    const value = String(id || '').replace(/^BAT-/, '');
    return /^\d+$/.test(value) ? value : null;
  };

  useEffect(() => {
    loadBatches();
  }, []);

  // Find active details item
  const activeBatch = useMemo(() => {
    return batches.find(b => b.id === selectedBatchId) || batches[0];
  }, [batches, selectedBatchId]);

  // Filter batches
  const filteredBatches = useMemo(() => {
    return batches.filter(b => b.id.toLowerCase().includes(searchQuery.toLowerCase()));
  }, [batches, searchQuery]);

  const totals = useMemo(() => {
    const mandates = batches.reduce((sum, batch) => sum + batch.mandatesCount, 0);
    const recovered = batches.reduce((sum, batch) => sum + batch.recoveredCount, 0);
    const revenue = batches.reduce((sum, batch) => sum + batch.revenueNum, 0);
    return {
      batches: batches.length,
      mandates,
      recoveryRate: mandates > 0 ? (recovered * 100) / mandates : 0,
      revenue,
    };
  }, [batches]);

  const monthlyBatchTrend = useMemo(() => {
    const monthKey = (dateValue) => {
      const date = dateValue ? new Date(dateValue) : null;
      if (!date || Number.isNaN(date.getTime())) return null;
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    };

    const months = batches.reduce((acc, batch) => {
      const key = monthKey(batch.startedAt);
      if (!key) return acc;

      acc[key] = acc[key] || { batches: 0, mandates: 0, recovered: 0, revenue: 0 };
      acc[key].batches += 1;
      acc[key].mandates += batch.mandatesCount;
      acc[key].recovered += batch.recoveredCount;
      acc[key].revenue += batch.revenueNum;
      return acc;
    }, {});

    const keys = Object.keys(months).sort();
    const current = months[keys[keys.length - 1]] || { batches: 0, mandates: 0, recovered: 0, revenue: 0 };
    const previous = months[keys[keys.length - 2]] || { batches: 0, mandates: 0, recovered: 0, revenue: 0 };
    const currentRate = current.mandates > 0 ? (current.recovered * 100) / current.mandates : 0;
    const previousRate = previous.mandates > 0 ? (previous.recovered * 100) / previous.mandates : 0;

    return {
      batches: percentChange(current.batches, previous.batches),
      mandates: percentChange(current.mandates, previous.mandates),
      recoveryRate: currentRate - previousRate,
      revenue: current.revenue - previous.revenue,
    };
  }, [batches]);

  const TrendIndicator = ({ value, type = 'percent' }) => {
    const isUp = value > 0;
    const isDown = value < 0;
    const color = isUp ? 'text-primary' : isDown ? 'text-error' : 'text-on-surface-variant';
    const icon = isUp ? 'trending_up' : isDown ? 'trending_down' : 'trending_flat';
    const formattedValue = type === 'money'
      ? `${isUp ? '+' : isDown ? '-' : ''}$${Math.abs(Number(value || 0)).toLocaleString()}`
      : `${value > 0 ? '+' : ''}${Number(value || 0).toFixed(1)}%`;

    return (
      <div className={`font-body-sm text-body-sm ${color} flex items-center gap-1`}>
        <span className="material-symbols-outlined text-[14px]">{icon}</span>
        <span className="font-bold">{formattedValue}</span> vs last month
      </div>
    );
  };

  const showToast = (msg) => {
    setToast(msg);
    setTimeout(() => setToast(''), 3000);
  };

  const handleRunAll = async () => {
    setIsRunningAll(true);
    try {
      const result = await runAllAgents();
      showToast(`Backend batch finished: ${result.successfulRecoveries}/${result.totalProcessed} recovered.`);
      loadBatches();
    } catch {
      showToast('Backend batch could not run.');
    } finally {
      setIsRunningAll(false);
    }
  };

  const handleExportReport = async () => {
    const id = backendBatchId(activeBatch?.id);
    if (!id) {
      showToast('Report export is available for backend batches only.');
      return;
    }

    setIsExporting(true);
    try {
      await downloadBatchReport(id);
      showToast(`Batch ${activeBatch.id} report downloaded.`);
    } catch {
      showToast(`Could not export report for ${activeBatch.id}.`);
    } finally {
      setIsExporting(false);
    }
  };

  const handleFileDrop = (e) => {
    e.preventDefault();
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setUploadFile(e.dataTransfer.files[0]);
    }
  };

  const handleFileChange = (e) => {
    if (e.target.files && e.target.files[0]) {
      setUploadFile(e.target.files[0]);
    }
  };

  const startUploadSimulation = async () => {
    if (!uploadFile) return;
    setUploadStep('uploading');
    setUploadProgress(0);

    try {
      setUploadProgress(35);
      const result = await uploadBatch(uploadFile, true);
      setUploadProgress(100);
      setUploadStep('processing');
      await new Promise((resolve) => setTimeout(resolve, 600));
      setUploadStep('done');
      showToast(`Imported ${result.importedMandates} mandates from ${result.fileName}.`);
      loadBatches();
    } catch {
      setUploadStep('idle');
      showToast('Batch upload failed.');
    }
  };

  const resetModal = () => {
    setIsModalOpen(false);
    setUploadFile(null);
    setUploadProgress(0);
    setUploadStep('idle');
  };

  return (
    <div className="flex-1 p-container-padding overflow-y-auto pb-20">
      {/* Toast */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 bg-inverse-surface text-inverse-on-surface px-6 py-3 rounded-lg shadow-level2 border border-outline-variant flex items-center gap-2 border-primary/20 animate-fade-in">
          <span className="material-symbols-outlined fill text-green-500">check_circle</span>
          <span className="font-semibold">{toast}</span>
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4 mb-8">
        <div>
          <h2 className="font-headline-lg text-headline-lg text-on-surface">Batch Processing</h2>
          <p className="font-body-md text-body-md text-on-surface-variant mt-1">Manage and monitor bulk mandate recovery operations.</p>
        </div>
        <div className="flex gap-3 w-full sm:w-auto">
          <button 
            onClick={handleExportReport}
            disabled={isExporting || !activeBatch}
            className="flex-1 sm:flex-none px-4 py-2 border border-outline-variant text-on-surface rounded-md font-title-md text-title-md hover:bg-surface-container-low transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {isExporting ? <ButtonLoader /> : <span className="material-symbols-outlined text-[18px]">download</span>}
            {isExporting ? 'Exporting...' : 'Export Report'}
          </button>
          <button 
            onClick={handleRunAll}
            disabled={isRunningAll}
            className="flex-1 sm:flex-none px-4 py-2 border border-outline-variant text-on-surface rounded-md font-title-md text-title-md hover:bg-surface-container-low transition-colors flex items-center justify-center gap-2 disabled:opacity-50"
          >
            {isRunningAll ? <ButtonLoader /> : <span className="material-symbols-outlined text-[18px]">play_arrow</span>}
            {isRunningAll ? 'Running...' : 'Run All'}
          </button>
          <button 
            onClick={() => setIsModalOpen(true)}
            className="flex-1 sm:flex-none px-4 py-2 bg-primary text-on-primary rounded-md font-title-md text-title-md hover:bg-primary-container transition-colors shadow-sm flex items-center justify-center gap-2"
          >
            <span className="material-symbols-outlined text-[18px]">add</span> New Batch
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-gutter mb-8">
        {isLoading ? (
          Array.from({ length: 4 }).map((_, index) => <MetricCardSkeleton key={`batch-metric-skeleton-${index}`} />)
        ) : (
          <>
        {/* Card 1 */}
        <div className="bg-surface-container-lowest rounded-lg p-5 border border-outline-variant/30 shadow-level1">
          <div className="flex items-center justify-between mb-2">
            <span className="font-body-sm text-body-sm text-on-surface-variant font-semibold">Total Batches</span>
            <span className="material-symbols-outlined text-outline-variant text-[20px]">layers</span>
          </div>
          <div className="font-headline-md text-headline-md text-on-surface mb-1">{totals.batches.toLocaleString()}</div>
          <TrendIndicator value={monthlyBatchTrend.batches} />
        </div>

        {/* Card 2 */}
        <div className="bg-surface-container-lowest rounded-lg p-5 border border-outline-variant/30 shadow-level1">
          <div className="flex items-center justify-between mb-2">
            <span className="font-body-sm text-body-sm text-on-surface-variant font-semibold">Mandates Processed</span>
            <span className="material-symbols-outlined text-outline-variant text-[20px]">receipt_long</span>
          </div>
          <div className="font-headline-md text-headline-md text-on-surface mb-1">{totals.mandates.toLocaleString()}</div>
          <TrendIndicator value={monthlyBatchTrend.mandates} />
        </div>

        {/* Card 3 */}
        <div className="bg-surface-container-lowest rounded-lg p-5 border border-outline-variant/30 shadow-level1">
          <div className="flex items-center justify-between mb-2">
            <span className="font-body-sm text-body-sm text-on-surface-variant font-semibold">Avg. Recovery Rate</span>
            <span className="material-symbols-outlined text-outline-variant text-[20px]">query_stats</span>
          </div>
          <div className="font-headline-md text-headline-md text-on-surface mb-1">{totals.recoveryRate.toFixed(1)}%</div>
          <TrendIndicator value={monthlyBatchTrend.recoveryRate} />
        </div>

        {/* Card 4 */}
        <div className="bg-surface-container-lowest rounded-lg p-5 border border-[#e0caff] bg-gradient-to-br from-[#f9f0ff] to-white shadow-level1 relative overflow-hidden">
          <div className="flex items-center justify-between mb-2 relative z-10">
            <span className="font-body-sm text-body-sm text-on-surface-variant font-semibold">Revenue Recovered</span>
            <span className="material-symbols-outlined text-primary text-[20px]">payments</span>
          </div>
          <div className="font-headline-md text-headline-md text-on-surface mb-1 relative z-10">${totals.revenue.toLocaleString()}</div>
          <div className="relative z-10">
            <TrendIndicator value={monthlyBatchTrend.revenue} type="money" />
          </div>
        </div>
          </>
        )}
      </div>

      {/* Complex Layout Area */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-gutter min-h-0">
        
        {/* Main Table Section */}
        <div className="lg:col-span-2 bg-surface-container-lowest rounded-xl border border-outline-variant/30 shadow-level1 flex flex-col overflow-hidden min-h-0">
          <div className="p-5 border-b border-outline-variant/30 flex justify-between items-center bg-surface-container-low/30">
            <h3 className="font-title-lg text-title-lg text-on-surface">Recent Batches</h3>
            <div className="relative">
              <span className="material-symbols-outlined absolute left-3 top-1/2 transform -translate-y-1/2 text-outline-variant text-[18px]">search</span>
              <input 
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="pl-9 pr-4 py-1.5 border border-outline-variant rounded-lg font-body-sm text-body-sm focus:outline-none focus:border-primary w-64 bg-surface-container-lowest" 
                placeholder="Search batch ID..." 
                type="text"
              />
            </div>
          </div>
          <div className="overflow-y-auto flex-grow">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-outline-variant/30 bg-surface-container-low text-on-surface-variant font-label-md text-label-md uppercase tracking-wider sticky top-0 z-10">
                  <th className="py-3 px-5">Batch ID</th>
                  <th className="py-3 px-5">Created Date</th>
                  <th className="py-3 px-5 text-right">Mandates</th>
                  <th className="py-3 px-5 text-right">Recovered</th>
                  <th className="py-3 px-5 text-right">Recovery %</th>
                  <th className="py-3 px-5 text-right">Revenue</th>
                  <th className="py-3 px-5 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="font-body-md text-body-md divide-y divide-outline-variant/10">
                {isLoading ? (
                  <TableSkeletonRows rows={6} columns={7} />
                ) : filteredBatches.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="py-8 px-5 text-center text-on-surface-variant">
                      No backend batches match this view.
                    </td>
                  </tr>
                ) : (
                  filteredBatches.map((item) => (
                  <tr 
                    key={item.id}
                    onClick={() => setSelectedBatchId(item.id)}
                    className={`hover:bg-surface-container-low transition-colors cursor-pointer ${
                      selectedBatchId === item.id 
                        ? 'bg-surface-container-low border-l-4 border-l-primary font-semibold' 
                        : ''
                    }`}
                  >
                    <td className="py-4 px-5 font-code text-code text-primary">{item.id}</td>
                    <td className="py-4 px-5 text-on-surface-variant">{item.createdDate}</td>
                    <td className="py-4 px-5 text-right text-on-surface">{item.mandatesCount.toLocaleString()}</td>
                    <td className="py-4 px-5 text-right text-on-surface">{item.recoveredCount > 0 ? item.recoveredCount.toLocaleString() : '--'}</td>
                    <td className="py-4 px-5 text-right font-code text-code">
                      <span className={`${item.percentNum > 0 ? 'text-primary bg-primary-container/20' : 'text-on-surface-variant bg-surface-container-low'} px-2 py-0.5 rounded text-xs font-bold`}>
                        {item.percentage}
                      </span>
                    </td>
                    <td className="py-4 px-5 text-right text-on-surface">{item.recoveredCount > 0 ? item.revenue : '--'}</td>
                    <td className="py-4 px-5 text-center">
                      <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-[11px] font-bold ${item.statusClass}`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${item.dotClass}`}></span> {item.status}
                      </span>
                    </td>
                  </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
          
          <div className="p-4 border-t border-outline-variant/30 flex justify-between items-center text-body-sm text-on-surface-variant bg-surface-container-low/20">
            <span>{isLoading ? 'Loading batches...' : `Showing ${filteredBatches.length} of ${batches.length} batches`}</span>
            <div className="flex gap-2">
              <button className="px-2 py-1 border border-outline-variant rounded hover:bg-surface-container transition-colors disabled:opacity-50" disabled>Previous</button>
              <button className="px-2 py-1 border border-outline-variant rounded hover:bg-surface-container transition-colors" disabled>Next</button>
            </div>
          </div>
        </div>

        {/* Detail Panel Section */}
        {isLoading ? (
          <div className="flex flex-col gap-gutter shrink-0">
            <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/30 shadow-level1 p-5 space-y-6">
              <div className="flex justify-between items-center border-b border-outline-variant/20 pb-4">
                <SkeletonBlock className="h-5 w-32" />
                <SkeletonBlock className="h-6 w-20" />
              </div>
              <SkeletonBlock className="h-20 w-full" />
              <div className="grid grid-cols-2 gap-4">
                <SkeletonBlock className="h-20 w-full" />
                <SkeletonBlock className="h-20 w-full" />
              </div>
              <SkeletonBlock className="h-28 w-full" />
            </div>
          </div>
        ) : activeBatch && (
          <div className="flex flex-col gap-gutter shrink-0">
            <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/30 shadow-level1 p-5 space-y-6">
              <div className="flex justify-between items-center border-b border-outline-variant/20 pb-4">
                <h3 className="font-title-lg text-title-lg text-on-surface">Batch Details</h3>
                <span className="font-code text-code text-primary bg-primary-container/10 px-2.5 py-1 rounded font-semibold">
                  {activeBatch.id}
                </span>
              </div>

              {/* Distribution Bar */}
              {activeBatch.mandatesCount > 0 && activeBatch.recoveredCount > 0 && (
                <div>
                  <div className="flex justify-between text-body-sm mb-2">
                    <span className="text-on-surface-variant">Outcome Distribution</span>
                    <span className="font-title-md text-primary font-bold">{activeBatch.percentage} Recovered</span>
                  </div>
                  <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden flex">
                    <div className="h-full bg-primary animate-width-fill" style={{ width: activeBatch.percentage }}></div>
                    <div className="h-full bg-error" style={{ width: `${100 - activeBatch.percentNum}%` }}></div>
                  </div>
                  <div className="flex justify-between text-body-sm mt-2 text-on-surface-variant">
                    <div className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-primary"></span> {activeBatch.recoveredCount} Recovered</div>
                    <div className="flex items-center gap-1"><span className="w-2 h-2 rounded-full bg-error"></span> {activeBatch.failedCount} Failed</div>
                  </div>
                </div>
              )}

              {/* Mini Stats Grid */}
              <div className="grid grid-cols-2 gap-4">
                <div className="p-3 bg-surface-container-low rounded-lg border border-outline-variant/30">
                  <div className="text-label-md text-on-surface-variant mb-1 font-semibold uppercase tracking-wider">Avg Ticket</div>
                  <div className="font-title-md text-on-surface text-lg">{activeBatch.avgTicket}</div>
                </div>
                <div className="p-3 bg-surface-container-low rounded-lg border border-outline-variant/30">
                  <div className="text-label-md text-on-surface-variant mb-1 font-semibold uppercase tracking-wider">Processing Time</div>
                  <div className="font-title-md text-on-surface text-lg">{activeBatch.time}</div>
                </div>
              </div>

              {/* AI Insight Component */}
              <div className="bg-[#f9f0ff] border border-[#e6ccff] rounded-lg p-4 relative overflow-hidden ai-sparkle">
                <div className="flex items-center gap-2 mb-2 relative z-10">
                  <span className="material-symbols-outlined text-[18px] text-[#9933ff] fill">auto_awesome</span>
                  <span className="font-title-md text-title-md text-[#4d0099] font-bold">AI Recovery Summary</span>
                </div>
                <p className="font-body-sm text-body-sm text-[#330066] relative z-10 leading-relaxed">
                  {activeBatch.aiSummary}
                </p>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* New Batch Upload Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-inverse-surface/40 backdrop-blur-xs flex items-center justify-center z-50 p-4 animate-fade-in">
          <div className="bg-surface rounded-xl border border-outline-variant/40 shadow-level2 w-full max-w-md overflow-hidden animate-zoom-in">
            <div className="px-6 py-4 bg-surface-container-high border-b border-outline-variant/40 flex justify-between items-center">
              <h3 className="font-title-lg text-title-lg text-on-surface">Upload Mandate Batch</h3>
              <button 
                onClick={resetModal}
                className="text-on-surface-variant hover:text-on-surface p-1 rounded hover:bg-surface-container"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            
            <div className="p-6 space-y-6">
              {uploadStep === 'idle' && (
                <div 
                  onDragOver={(e) => e.preventDefault()}
                  onDrop={handleFileDrop}
                  className="border-2 border-dashed border-outline-variant/60 rounded-xl p-8 text-center bg-surface-container-low hover:bg-surface-container-high hover:border-primary transition-all cursor-pointer relative"
                >
                  <input 
                    type="file" 
                    onChange={handleFileChange}
                    className="absolute inset-0 opacity-0 cursor-pointer"
                    accept=".csv,.xlsx"
                  />
                  <span className="material-symbols-outlined text-4xl text-outline-variant mb-3 block">cloud_upload</span>
                  {uploadFile ? (
                    <div>
                      <p className="font-title-md text-title-md text-primary font-bold mb-1 truncate">{uploadFile.name}</p>
                      <p className="text-xs text-on-surface-variant">{(uploadFile.size / 1024).toFixed(1)} KB - Click to replace</p>
                    </div>
                  ) : (
                    <div>
                      <p className="font-title-md text-title-md text-on-surface mb-1">Drag and drop file here</p>
                      <p className="text-xs text-on-surface-variant">Supports CSV, XLS (Max 10MB)</p>
                    </div>
                  )}
                </div>
              )}

              {uploadStep === 'uploading' && (
                <div className="space-y-4">
                  <div className="flex justify-between text-body-sm font-semibold">
                    <span>Uploading CSV file...</span>
                    <span>{uploadProgress}%</span>
                  </div>
                  <div className="h-2 w-full bg-surface-container rounded-full overflow-hidden">
                    <div className="h-full bg-primary transition-all duration-150" style={{ width: `${uploadProgress}%` }}></div>
                  </div>
                </div>
              )}

              {uploadStep === 'processing' && (
                <div className="space-y-4 text-center py-4">
                  <div className="w-12 h-12 rounded-full border-4 border-primary/20 border-t-primary animate-spin mx-auto mb-4"></div>
                  <h4 className="font-title-md text-[#8a2be2] font-bold flex items-center justify-center gap-1">
                    <span className="material-symbols-outlined text-[18px] fill animate-pulse">auto_awesome</span>
                    AI Model Synthesizing
                  </h4>
                  <p className="text-xs text-on-surface-variant leading-relaxed">
                    Analyzing accounts, validating bank codes, and predicting optimal retry timing...
                  </p>
                </div>
              )}

              {uploadStep === 'done' && (
                <div className="text-center py-6 space-y-3">
                  <span className="material-symbols-outlined text-5xl text-green-500 fill mb-2">check_circle</span>
                  <h4 className="font-title-lg text-title-lg text-on-surface font-bold">Successfully Registered!</h4>
                  <p className="text-body-sm text-on-surface-variant">
                    Your batch upload is complete. The mandate queues are executing automatically.
                  </p>
                </div>
              )}
            </div>

            <div className="px-6 py-4 bg-surface-container border-t border-outline-variant/40 flex justify-end gap-2">
              <button 
                onClick={resetModal}
                className="px-4 py-2 border border-outline-variant/50 text-on-surface rounded font-label-md text-label-md hover:bg-surface-container transition-colors"
              >
                {uploadStep === 'done' ? 'Done' : 'Cancel'}
              </button>
              {uploadStep === 'idle' && (
                <button 
                  disabled={!uploadFile}
                  onClick={startUploadSimulation}
                  className="px-4 py-2 bg-primary text-on-primary rounded font-label-md text-label-md hover:bg-primary-container transition-colors shadow-sm disabled:opacity-50 flex items-center gap-2"
                >
                  <span className="material-symbols-outlined text-[16px]">upload_file</span>
                  Upload &amp; Analyze
                </button>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
