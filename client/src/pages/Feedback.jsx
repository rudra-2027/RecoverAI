import React, { useEffect, useMemo, useState } from 'react';
import { ButtonLoader } from '../components/LoadingSkeleton';
import { createFeedback, fetchFeedback } from '../services/api';

const initialForm = {
  reviewerName: '',
  reviewerEmail: '',
  reviewerRole: 'Student Reviewer',
  rating: 4,
  issueCategory: 'Usability',
  severity: 'Medium',
  failedArea: '',
  reviewTitle: '',
  feedback: '',
  stepsToReproduce: '',
  expectedBehavior: '',
  suggestedImprovement: '',
  wouldRecommend: true,
};

const issueCategories = ['Usability', 'Bug', 'Performance', 'AI Accuracy', 'UI Design', 'Data/API', 'Documentation', 'Other'];
const severities = ['Low', 'Medium', 'High', 'Critical'];

export default function Feedback() {
  const [form, setForm] = useState(initialForm);
  const [feedbackItems, setFeedbackItems] = useState([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [toast, setToast] = useState('');

  const averageRating = useMemo(() => {
    if (feedbackItems.length === 0) {
      return '0.0';
    }
    const total = feedbackItems.reduce((sum, item) => sum + Number(item.rating || 0), 0);
    return (total / feedbackItems.length).toFixed(1);
  }, [feedbackItems]);

  function showToast(message) {
    setToast(message);
    setTimeout(() => setToast(''), 3000);
  }

  function loadFeedback() {
    fetchFeedback()
      .then(setFeedbackItems)
      .catch(() => showToast('Could not load previous feedback.'))
      .finally(() => setIsLoading(false));
  }

  useEffect(() => {
    loadFeedback();
  }, []);

  const updateField = (field, value) => {
    setForm((current) => ({ ...current, [field]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      const saved = await createFeedback({
        ...form,
        rating: Number(form.rating),
      });
      setFeedbackItems((current) => [saved, ...current]);
      setForm(initialForm);
      showToast('Thanks, your feedback was submitted.');
    } catch {
      showToast('Could not submit feedback. Please check required fields.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="flex-1 p-container-padding overflow-y-auto bg-surface-container-lowest pb-20">
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 bg-inverse-surface text-inverse-on-surface px-6 py-3 rounded-lg shadow-level2 border border-outline-variant flex items-center gap-2">
          <span className="material-symbols-outlined fill text-green-500">check_circle</span>
          <span className="font-semibold">{toast}</span>
        </div>
      )}

      <div className="max-w-6xl mx-auto space-y-stack-lg">
        <div className="flex flex-col lg:flex-row lg:items-end lg:justify-between gap-4">
          <div>
            <h2 className="font-headline-lg text-headline-lg text-on-surface">Project Feedback</h2>
            <p className="font-body-md text-body-md text-on-surface-variant mt-2">
              Collect reviews, failure points, issues, and improvement ideas from people testing RecoverAI.
            </p>
          </div>
          <div className="grid grid-cols-2 gap-3 min-w-[260px]">
            <div className="bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-4">
              <span className="font-label-md text-label-md text-on-surface-variant uppercase font-semibold">Reviews</span>
              <p className="font-headline-md text-headline-md text-primary font-bold mt-1">{feedbackItems.length}</p>
            </div>
            <div className="bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-4">
              <span className="font-label-md text-label-md text-on-surface-variant uppercase font-semibold">Avg Rating</span>
              <p className="font-headline-md text-headline-md text-primary font-bold mt-1">{averageRating}/5</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 xl:grid-cols-12 gap-stack-lg items-start">
          <form onSubmit={handleSubmit} className="xl:col-span-7 bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-6 space-y-5">
            <div className="flex items-center gap-2 border-b border-outline-variant/30 pb-4">
              <span className="material-symbols-outlined text-primary fill">rate_review</span>
              <h3 className="font-title-lg text-title-lg text-on-surface">Share Review</h3>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Field label="Name">
                <input value={form.reviewerName} onChange={(e) => updateField('reviewerName', e.target.value)} className="form-input" placeholder="Optional" />
              </Field>
              <Field label="Email">
                <input value={form.reviewerEmail} onChange={(e) => updateField('reviewerEmail', e.target.value)} className="form-input" type="email" placeholder="Optional" />
              </Field>
              <Field label="Role">
                <input value={form.reviewerRole} onChange={(e) => updateField('reviewerRole', e.target.value)} className="form-input" />
              </Field>
              <Field label="Rating">
                <select value={form.rating} onChange={(e) => updateField('rating', e.target.value)} className="form-input">
                  {[5, 4, 3, 2, 1].map((rating) => <option key={rating} value={rating}>{rating} out of 5</option>)}
                </select>
              </Field>
              <Field label="Issue Category">
                <select value={form.issueCategory} onChange={(e) => updateField('issueCategory', e.target.value)} className="form-input">
                  {issueCategories.map((category) => <option key={category}>{category}</option>)}
                </select>
              </Field>
              <Field label="Severity">
                <select value={form.severity} onChange={(e) => updateField('severity', e.target.value)} className="form-input">
                  {severities.map((severity) => <option key={severity}>{severity}</option>)}
                </select>
              </Field>
            </div>

            <Field label="Where did you fail or get stuck?" required>
              <input value={form.failedArea} onChange={(e) => updateField('failedArea', e.target.value)} className="form-input" required placeholder="Example: uploading batch file, AI insight page, simulator result" />
            </Field>

            <Field label="Review Title" required>
              <input value={form.reviewTitle} onChange={(e) => updateField('reviewTitle', e.target.value)} className="form-input" required placeholder="Short summary of your experience" />
            </Field>

            <Field label="Detailed Review" required>
              <textarea value={form.feedback} onChange={(e) => updateField('feedback', e.target.value)} className="form-input min-h-28 resize-y" required placeholder="Tell us what worked, what failed, and what felt confusing." />
            </Field>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <Field label="Steps to Reproduce">
                <textarea value={form.stepsToReproduce} onChange={(e) => updateField('stepsToReproduce', e.target.value)} className="form-input min-h-24 resize-y" placeholder="What did you click or enter before the issue happened?" />
              </Field>
              <Field label="Expected Behavior">
                <textarea value={form.expectedBehavior} onChange={(e) => updateField('expectedBehavior', e.target.value)} className="form-input min-h-24 resize-y" placeholder="What should RecoverAI have done instead?" />
              </Field>
            </div>

            <Field label="Suggested Improvement">
              <textarea value={form.suggestedImprovement} onChange={(e) => updateField('suggestedImprovement', e.target.value)} className="form-input min-h-24 resize-y" placeholder="Feature ideas, design fixes, missing fields, or clearer messages." />
            </Field>

            <label className="flex items-center justify-between gap-4 rounded-lg border border-outline-variant/30 bg-surface-container-lowest p-4">
              <span>
                <span className="block font-title-md text-title-md text-on-surface">Would recommend this project</span>
                <span className="block font-body-sm text-body-sm text-on-surface-variant mt-1">Useful for demos, reviews, or production-style evaluation.</span>
              </span>
              <button
                type="button"
                onClick={() => updateField('wouldRecommend', !form.wouldRecommend)}
                className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors focus:outline-none flex-shrink-0 ${form.wouldRecommend ? 'bg-primary' : 'bg-outline'}`}
              >
                <span className={`inline-block h-4 w-4 rounded-full bg-white transition-transform ${form.wouldRecommend ? 'translate-x-6' : 'translate-x-1'}`}></span>
              </button>
            </label>

            <div className="flex justify-end pt-2">
              <button type="submit" disabled={isSubmitting} className="bg-primary text-on-primary font-title-md text-title-md px-6 py-3 rounded-lg hover:bg-on-primary-fixed-variant transition-colors flex items-center gap-2 shadow-sm disabled:opacity-50">
                {isSubmitting ? <ButtonLoader /> : <span className="material-symbols-outlined">send</span>}
                {isSubmitting ? 'Submitting...' : 'Submit Feedback'}
              </button>
            </div>
          </form>

          <div className="xl:col-span-5 bg-surface rounded-xl border border-outline-variant/30 shadow-level1 p-6 space-y-4">
            <div className="flex items-center gap-2 border-b border-outline-variant/30 pb-4">
              <span className="material-symbols-outlined text-secondary">forum</span>
              <h3 className="font-title-lg text-title-lg text-on-surface">Recent Reviews</h3>
            </div>

            {isLoading ? (
              <div className="text-on-surface-variant">Loading feedback...</div>
            ) : feedbackItems.length === 0 ? (
              <div className="text-center text-on-surface-variant py-12 border border-dashed border-outline-variant rounded-lg">
                No feedback submitted yet.
              </div>
            ) : (
              <div className="space-y-3 max-h-[760px] overflow-y-auto pr-1 chat-scroll">
                {feedbackItems.map((item) => (
                  <article key={item.id} className="border border-outline-variant/30 rounded-lg p-4 bg-surface-container-lowest">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <h4 className="font-title-md text-title-md text-on-surface font-bold truncate">{item.reviewTitle}</h4>
                        <p className="font-body-sm text-body-sm text-on-surface-variant mt-1">
                          {item.issueCategory} | {item.severity} | {item.failedArea}
                        </p>
                      </div>
                      <span className="font-label-md text-label-md bg-primary-fixed text-on-primary-fixed-variant px-2 py-1 rounded-md flex-shrink-0">{item.rating}/5</span>
                    </div>
                    <p className="font-body-md text-body-md text-on-surface-variant mt-3 line-clamp-4">{item.feedback}</p>
                    <div className="flex items-center justify-between gap-3 mt-4 text-outline font-body-sm text-body-sm">
                      <span className="truncate">{item.reviewerName || 'Anonymous'}{item.reviewerRole ? `, ${item.reviewerRole}` : ''}</span>
                      <span>{item.wouldRecommend ? 'Recommended' : 'Not recommended'}</span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function Field({ label, required = false, children }) {
  return (
    <label className="block">
      <span className="block font-label-md text-label-md text-on-surface-variant mb-1">
        {label}{required ? ' *' : ''}
      </span>
      {children}
    </label>
  );
}
