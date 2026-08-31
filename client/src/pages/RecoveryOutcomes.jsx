import React, { useEffect, useState, useMemo } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from 'recharts';
import { MetricCardSkeleton, SkeletonBlock, TableSkeletonRows } from '../components/LoadingSkeleton';
import { fetchMetrics, fetchOutcomes } from '../services/api';

export default function RecoveryOutcomes() {
  const percentChange = (current, previous) => {
    if (!previous && !current) return 0;
    if (!previous) return current > 0 ? 100 : 0;
    return ((current - previous) / previous) * 100;
  };

  const normalizeOutcome = (item) => ({
    id: item.mandateId || item.id,
    action: item.actionTaken || 'RECOVERY_ACTION',
    isAi: true,
    outcome: item.outcome || 'PENDING',
    outcomeStyle: item.outcome === 'SUCCESS'
      ? 'bg-[#e6f4ea] text-[#137333] border-[#ceead6]'
      : item.outcome === 'FAILED'
        ? 'bg-[#fce8e6] text-[#c5221f] border-[#fad2cf]'
        : 'bg-[#fef7e0] text-[#b06000] border-[#fde293]',
    dotStyle: item.outcome === 'SUCCESS' ? 'bg-[#137333]' : item.outcome === 'FAILED' ? 'bg-[#c5221f]' : 'bg-[#b06000]',
    amount: Number(item.recoveredAmount || 0),
    txRef: item.transactionId || 'N/A',
    date: item.outcomeTimestamp ? new Date(item.outcomeTimestamp).toLocaleString() : 'Unknown',
    outcomeTimestamp: item.outcomeTimestamp || null,
    details: item.simulationReason || 'Backend recovery outcome record.',
  });

  // States
  const [search, setSearch] = useState('');
  const [outcomeFilter, setOutcomeFilter] = useState('ALL');
  const [selectedItem, setSelectedItem] = useState(null);
  const [refreshing, setRefreshing] = useState(false);
  const [outcomes, setOutcomes] = useState([]);
  const [metrics, setMetrics] = useState(null);
  const [isLoading, setIsLoading] = useState(true);

  const loadOutcomes = async () => {
    setIsLoading(true);
    const [outcomeData, metricData] = await Promise.all([fetchOutcomes(), fetchMetrics()]);
    setOutcomes(outcomeData.map(normalizeOutcome));
    setMetrics(metricData);
    setIsLoading(false);
  };

  useEffect(() => {
    loadOutcomes().catch(() => setIsLoading(false));
  }, []);

  const filteredOutcomes = useMemo(() => {
    return outcomes.filter(item => {
      const matchesSearch = 
        item.id.toLowerCase().includes(search.toLowerCase()) ||
        item.action.toLowerCase().includes(search.toLowerCase()) ||
        item.txRef.toLowerCase().includes(search.toLowerCase());
      
      const matchesOutcome = outcomeFilter === 'ALL' || item.outcome === outcomeFilter;
      return matchesSearch && matchesOutcome;
    });
  }, [outcomes, search, outcomeFilter]);

  const chartData = useMemo(() => {
    const colors = ['#0038d1', '#8a2be2', '#4d6077', '#c4c5d8', '#137333'];
    const totals = outcomes.reduce((acc, item) => {
      acc[item.action] = (acc[item.action] || 0) + item.amount;
      return acc;
    }, {});

    return Object.entries(totals).map(([name, Amount], index) => ({
      name,
      Amount,
      color: colors[index % colors.length],
    }));
  }, [outcomes]);

  const outcomeDistribution = useMemo(() => {
    const total = outcomes.length || 1;
    const count = (status) => outcomes.filter((item) => item.outcome === status).length;
    const successes = count('SUCCESS');
    const failures = count('FAILED');
    const pendings = count('PENDING');

    return { total, successes, failures, pendings };
  }, [outcomes]);

  const monthlyOutcomeTrend = useMemo(() => {
    const monthKey = (dateValue) => {
      const date = dateValue ? new Date(dateValue) : null;
      if (!date || Number.isNaN(date.getTime())) return null;
      return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    };

    const months = outcomes.reduce((acc, item) => {
      const key = monthKey(item.outcomeTimestamp);
      if (!key) return acc;

      acc[key] = acc[key] || { total: 0, successes: 0 };
      acc[key].total += 1;
      if (item.outcome === 'SUCCESS') {
        acc[key].successes += 1;
      }
      return acc;
    }, {});

    const keys = Object.keys(months).sort();
    const current = months[keys[keys.length - 1]] || { total: 0, successes: 0 };
    const previous = months[keys[keys.length - 2]] || { total: 0, successes: 0 };
    const currentRate = current.total > 0 ? (current.successes * 100) / current.total : 0;
    const previousRate = previous.total > 0 ? (previous.successes * 100) / previous.total : 0;

    return {
      recoveries: percentChange(current.successes, previous.successes),
      successRate: currentRate - previousRate,
    };
  }, [outcomes]);

  const handleRefresh = () => {
    setRefreshing(true);
    loadOutcomes().finally(() => setRefreshing(false));
  };

  const money = (value) => new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
    maximumFractionDigits: 0,
  }).format(Number(value || 0));

  const TrendIndicator = ({ value, suffix = '%' }) => {
    const isUp = value > 0;
    const isDown = value < 0;
    const color = isUp ? 'text-green-600' : isDown ? 'text-error' : 'text-on-surface-variant';
    const icon = isUp ? 'arrow_upward' : isDown ? 'arrow_downward' : 'trending_flat';

    return (
      <span className={`text-sm ${color} flex items-center font-bold`}>
        <span className="material-symbols-outlined text-[16px]">{icon}</span>
        {value > 0 ? '+' : ''}{Number(value || 0).toFixed(1)}{suffix}
      </span>
    );
  };

  const handleExport = () => {
    const header = ['Mandate ID', 'Action Taken', 'Outcome', 'Amount Recovered', 'Transaction Ref', 'Recovery Date'];
    const rows = filteredOutcomes.map((item) => [
      item.id,
      item.action,
      item.outcome,
      item.amount,
      item.txRef,
      item.date,
    ]);
    const csv = [header, ...rows]
      .map((row) => row.map((cell) => `"${String(cell ?? '').replaceAll('"', '""')}"`).join(','))
      .join('\n');
    const href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
    const link = document.createElement('a');
    link.href = href;
    link.download = 'recovery-outcomes.csv';
    document.body.appendChild(link);
    link.click();
    link.remove();
    URL.revokeObjectURL(href);
  };

  return (
    <div className="flex-1 p-container-padding overflow-y-auto pb-20">
      <div className="max-w-7xl mx-auto space-y-stack-lg">
        {/* Header */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-end gap-4">
          <div>
            <h2 className="font-headline-lg text-headline-lg text-on-surface mb-2">Recovery Outcomes</h2>
            <p className="font-body-md text-body-md text-on-surface-variant">Overview of processed mandates and their resolution status.</p>
          </div>
          <div className="flex gap-3 w-full sm:w-auto">
            <button onClick={handleExport} className="flex-1 sm:flex-none px-4 py-2 border border-outline-variant/60 rounded-lg text-on-surface font-title-md text-title-md hover:bg-surface-container transition-colors flex items-center justify-center gap-2">
              <span className="material-symbols-outlined text-[18px]">download</span> Export
            </button>
            <button 
              onClick={handleRefresh}
              className={`flex-1 sm:flex-none px-4 py-2 bg-primary text-on-primary rounded-lg font-title-md text-title-md hover:opacity-90 transition-opacity shadow-sm flex items-center justify-center gap-2 ${refreshing ? 'opacity-50 pointer-events-none' : ''}`}
            >
              {refreshing ? 'Refreshing...' : 'Refresh Data'}
            </button>
          </div>
        </div>

        {/* Metrics Bento */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-gutter">
          {isLoading ? (
            Array.from({ length: 4 }).map((_, index) => <MetricCardSkeleton key={`outcome-metric-skeleton-${index}`} />)
          ) : (
            <>
          {/* Metric 1 */}
          <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/30 shadow-level1 flex flex-col justify-between h-32 relative overflow-hidden group">
            <div className="absolute -right-4 -top-4 w-24 h-24 bg-primary-container opacity-10 rounded-full group-hover:scale-150 transition-transform duration-500"></div>
            <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Total Recoveries</p>
            <div className="flex items-baseline gap-2">
              <h3 className="font-display text-headline-lg text-on-surface">{metrics?.recoveredMandatesCount ?? outcomes.length}</h3>
              <TrendIndicator value={monthlyOutcomeTrend.recoveries} />
            </div>
          </div>

          {/* Metric 2 */}
          <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/30 shadow-level1 flex flex-col justify-between h-32 relative overflow-hidden group">
            <div className="absolute -right-4 -top-4 w-24 h-24 bg-green-600 opacity-10 rounded-full group-hover:scale-150 transition-transform duration-500"></div>
            <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Success Rate</p>
            <div className="flex items-baseline gap-2">
              <h3 className="font-display text-headline-lg text-on-surface">{Number(metrics?.retrySuccessRate || 0).toFixed(1)}%</h3>
              <TrendIndicator value={monthlyOutcomeTrend.successRate} />
            </div>
          </div>

          {/* Metric 3 */}
          <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/30 shadow-level1 flex flex-col justify-between h-32 relative overflow-hidden group">
            <div className="absolute -right-4 -top-4 w-24 h-24 bg-tertiary-container opacity-10 rounded-full group-hover:scale-150 transition-transform duration-500"></div>
            <p className="font-label-md text-label-md text-on-surface-variant uppercase tracking-wider">Revenue Recovered</p>
            <div className="flex items-baseline gap-2">
              <h3 className="font-display text-headline-lg text-on-surface">{money(metrics?.recoveredRevenue)}</h3>
              <span className="text-sm text-outline flex items-center font-semibold">This Month</span>
            </div>
          </div>

          {/* Metric 4 (AI Sparkle) */}
          <div className="bg-[#f9f0ff] rounded-xl p-6 border border-[#e0c6fd] shadow-level1 flex flex-col justify-between h-32 relative overflow-hidden group">
            <div className="absolute right-4 top-4 text-[#a855f7]">
              <span className="material-symbols-outlined fill text-[#8a2be2]">auto_awesome</span>
            </div>
            <p className="font-label-md text-label-md text-[#6b21a8] uppercase tracking-wider font-bold">Avg Recovery Time</p>
            <div className="flex items-baseline gap-2">
              <h3 className="font-display text-headline-lg text-[#4c1d95]">{Number(metrics?.averageRecoveryProbability || 0).toFixed(1)}%</h3>
              <span className="text-sm text-[#a855f7] flex items-center font-semibold">Avg Probability</span>
            </div>
          </div>
            </>
          )}
        </div>

        {/* Charts & Breakdown */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-stack-lg">
          <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/30 shadow-level1 lg:col-span-2">
            <h3 className="font-title-lg text-title-lg text-on-surface mb-6">Recovered Volume by Strategy ($)</h3>
            <div className="h-64 w-full">
              {isLoading ? (
                <div className="grid grid-cols-6 gap-3 items-end h-full">
                  {[38, 70, 50, 82, 58, 66].map((height, index) => (
                    <SkeletonBlock key={`outcome-chart-skeleton-${index}`} className="w-full rounded-t-lg" style={{ height: `${height}%` }} />
                  ))}
                </div>
              ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#c4c5d8" opacity={0.3} />
                  <XAxis dataKey="name" stroke="#747687" fontSize={12} />
                  <YAxis stroke="#747687" fontSize={12} />
                  <Tooltip 
                    cursor={{ fill: 'rgba(238, 237, 249, 0.3)' }}
                    contentStyle={{ 
                      backgroundColor: '#ffffff', 
                      borderRadius: '8px', 
                      border: '1px solid #c4c5d8',
                      boxShadow: '0px 2px 8px rgba(0,0,0,0.04)' 
                    }}
                  />
                  <Bar dataKey="Amount" radius={[4, 4, 0, 0]}>
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={entry.color} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
              )}
            </div>
          </div>

          <div className="bg-surface-container-lowest rounded-xl p-6 border border-outline-variant/30 shadow-level1 flex flex-col justify-between">
            {isLoading ? (
              <div className="space-y-4">
                <SkeletonBlock className="h-5 w-40" />
                <SkeletonBlock className="h-4 w-full" />
                <SkeletonBlock className="h-4 w-full" />
                <SkeletonBlock className="h-4 w-3/4" />
              </div>
            ) : (
            <>
            <div>
              <h3 className="font-title-lg text-title-lg text-on-surface mb-4">Outcome Distribution</h3>
              <div className="space-y-4">
                <div className="flex justify-between items-center">
                  <span className="text-body-sm text-secondary flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full bg-green-600"></span> Successes
                  </span>
                  <span className="font-title-md text-title-md text-on-surface">
                    {outcomeDistribution.successes} ({((outcomeDistribution.successes * 100) / outcomeDistribution.total).toFixed(0)}%)
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-body-sm text-secondary flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full bg-red-600"></span> Failures
                  </span>
                  <span className="font-title-md text-title-md text-on-surface">
                    {outcomeDistribution.failures} ({((outcomeDistribution.failures * 100) / outcomeDistribution.total).toFixed(0)}%)
                  </span>
                </div>
                <div className="flex justify-between items-center">
                  <span className="text-body-sm text-secondary flex items-center gap-2">
                    <span className="w-3 h-3 rounded-full bg-orange-600"></span> Pendings
                  </span>
                  <span className="font-title-md text-title-md text-on-surface">
                    {outcomeDistribution.pendings} ({((outcomeDistribution.pendings * 100) / outcomeDistribution.total).toFixed(0)}%)
                  </span>
                </div>
              </div>
            </div>
            <div className="border-t border-outline-variant/30 pt-4 mt-4">
              <p className="text-body-sm text-on-surface-variant">
                Backend success rate is currently <strong>{Number(metrics?.retrySuccessRate || 0).toFixed(1)}%</strong>.
              </p>
            </div>
            </>
            )}
          </div>
        </div>

        {/* Main Data Table */}
        <div className="bg-surface-container-lowest rounded-xl border border-outline-variant/30 shadow-level1 overflow-hidden">
          <div className="px-6 py-4 border-b border-outline-variant/30 flex flex-col sm:flex-row justify-between items-stretch sm:items-center bg-surface-container-low gap-3">
            <h3 className="font-title-lg text-title-lg text-on-surface">Recent Outcomes Log</h3>
            <div className="flex flex-wrap items-center gap-2">
              {/* Search input */}
              <input 
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Search..."
                className="px-3 py-1.5 bg-surface border border-outline-variant rounded-lg text-body-sm font-body-sm focus:outline-none focus:border-primary"
              />
              {/* Filter pills */}
              <button 
                onClick={() => setOutcomeFilter('ALL')}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${outcomeFilter === 'ALL' ? 'bg-primary text-on-primary' : 'bg-surface-container-high text-on-surface-variant hover:bg-outline-variant/30'}`}
              >
                All
              </button>
              <button 
                onClick={() => setOutcomeFilter('SUCCESS')}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${outcomeFilter === 'SUCCESS' ? 'bg-green-600 text-white' : 'bg-surface-container-high text-on-surface-variant hover:bg-outline-variant/30'}`}
              >
                Success
              </button>
              <button 
                onClick={() => setOutcomeFilter('FAILED')}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${outcomeFilter === 'FAILED' ? 'bg-red-600 text-white' : 'bg-surface-container-high text-on-surface-variant hover:bg-outline-variant/30'}`}
              >
                Failed
              </button>
              <button 
                onClick={() => setOutcomeFilter('PENDING')}
                className={`px-3 py-1.5 rounded-full text-xs font-bold transition-all ${outcomeFilter === 'PENDING' ? 'bg-orange-600 text-white' : 'bg-surface-container-high text-on-surface-variant hover:bg-outline-variant/30'}`}
              >
                Pending
              </button>
            </div>
          </div>
          
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-outline-variant/30 bg-surface-container-low text-on-surface-variant font-label-md text-label-md uppercase tracking-wider">
                  <th className="px-6 py-4 font-semibold">Mandate ID</th>
                  <th className="px-6 py-4 font-semibold">Action Taken</th>
                  <th className="px-6 py-4 font-semibold">Outcome</th>
                  <th className="px-6 py-4 font-semibold text-right">Amount Recovered</th>
                  <th className="px-6 py-4 font-semibold">Transaction Ref</th>
                  <th className="px-6 py-4 font-semibold">Recovery Date</th>
                  <th className="px-6 py-4"></th>
                </tr>
              </thead>
              <tbody className="font-body-md text-body-md text-on-surface divide-y divide-outline-variant/10">
                {isLoading ? (
                  <TableSkeletonRows rows={6} columns={7} />
                ) : filteredOutcomes.length === 0 ? (
                  <tr>
                    <td colSpan="7" className="px-6 py-8 text-center text-on-surface-variant">
                      No records match the active filter options.
                    </td>
                  </tr>
                ) : (
                  filteredOutcomes.map((item) => (
                    <tr 
                      key={item.id} 
                      className={`hover:bg-surface-container-low transition-colors group ${item.isAi ? 'bg-[#f9f0ff]/50' : ''}`}
                    >
                      <td className="px-6 py-4 font-code text-code text-on-surface-variant">{item.id}</td>
                      <td className="px-6 py-4 flex items-center gap-2">
                        {item.isAi && (
                          <span className="material-symbols-outlined text-[16px] text-[#a855f7] fill">auto_awesome</span>
                        )}
                        <span>{item.action}</span>
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold border ${item.outcomeStyle}`}>
                          <span className={`w-1.5 h-1.5 rounded-full ${item.dotStyle}`}></span> {item.outcome}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right font-code text-code">${item.amount.toFixed(2)}</td>
                      <td className="px-6 py-4 font-code text-code text-outline">{item.txRef}</td>
                      <td className="px-6 py-4 text-on-surface-variant">{item.date}</td>
                      <td className="px-6 py-4 text-right opacity-0 group-hover:opacity-100 transition-opacity">
                        <button 
                          onClick={() => setSelectedItem(item)}
                          className="text-primary hover:underline text-sm font-semibold"
                        >
                          View
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Outcome Detail Modal */}
      {selectedItem && (
        <div className="fixed inset-0 bg-inverse-surface/40 backdrop-blur-xs flex items-center justify-center z-50 p-4">
          <div className="bg-surface rounded-xl border border-outline-variant/40 shadow-level2 w-full max-w-md overflow-hidden animate-zoom-in">
            <div className="px-6 py-4 bg-surface-container-high border-b border-outline-variant/40 flex justify-between items-center">
              <h3 className="font-title-lg text-title-lg text-on-surface">Outcome Details</h3>
              <button onClick={() => setSelectedItem(null)} className="text-on-surface-variant hover:text-on-surface p-1 rounded hover:bg-surface-container">
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>
            <div className="p-6 space-y-4 font-body-md text-body-md">
              <div>
                <span className="block font-label-md text-label-md text-on-surface-variant">MANDATE ID</span>
                <span className="font-code text-code">{selectedItem.id}</span>
              </div>
              <div>
                <span className="block font-label-md text-label-md text-on-surface-variant">STRATEGY EXECUTED</span>
                <span>{selectedItem.action}</span>
              </div>
              <div>
                <span className="block font-label-md text-label-md text-on-surface-variant">AMOUNT RECOVERED</span>
                <span className="font-bold text-primary">${selectedItem.amount.toFixed(2)}</span>
              </div>
              <div>
                <span className="block font-label-md text-label-md text-on-surface-variant">TRANSACTION REFERENCE</span>
                <span className="font-code text-code">{selectedItem.txRef}</span>
              </div>
              <div>
                <span className="block font-label-md text-label-md text-on-surface-variant">DATE RESOLVED</span>
                <span>{selectedItem.date}</span>
              </div>
              <div className="border-t border-outline-variant/20 pt-4">
                <span className="block font-label-md text-label-md text-on-surface-variant">EXECUTION LOGS SUMMARY</span>
                <p className="mt-1 text-body-sm text-on-surface-variant leading-relaxed bg-surface-container p-3 rounded border border-outline-variant/10">
                  {selectedItem.details}
                </p>
              </div>
            </div>
            <div className="px-6 py-4 bg-surface-container border-t border-outline-variant/40 flex justify-end">
              <button 
                onClick={() => setSelectedItem(null)}
                className="px-4 py-2 bg-primary text-on-primary rounded font-label-md text-label-md hover:bg-primary-container transition-colors shadow-sm"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
