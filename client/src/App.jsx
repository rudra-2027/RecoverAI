import React from 'react';
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import Layout from './components/Layout';
import Dashboard from './pages/Dashboard';
import FailedMandates from './pages/FailedMandates';
import RecoveryDecisions from './pages/RecoveryDecisions';
import RecoveryOutcomes from './pages/RecoveryOutcomes';
import AuditTrail from './pages/AuditTrail';
import BatchProcessing from './pages/BatchProcessing';
import AIInsights from './pages/AIInsights';
import DemoSimulator from './pages/DemoSimulator';
import Settings from './pages/Settings';

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<Dashboard />} />
          <Route path="failed-mandates" element={<FailedMandates />} />
          <Route path="recovery-decisions" element={<RecoveryDecisions />} />
          <Route path="recovery-outcomes" element={<RecoveryOutcomes />} />
          <Route path="audit-trail" element={<AuditTrail />} />
          <Route path="batch-processing" element={<BatchProcessing />} />
          <Route path="ai-insights" element={<AIInsights />} />
          <Route path="demo-simulator" element={<DemoSimulator />} />
          <Route path="settings" element={<Settings />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
