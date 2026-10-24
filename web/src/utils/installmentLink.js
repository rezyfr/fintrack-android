import { addTransaction, getInstallments, incrementInstallmentStep, updateTransaction } from '../api/client';
import { advanceInstallment, matchInstallment } from './installmentMatcher';

// ac: installment-auto-link — links a card transaction to its installment and moves the installment to its
// new step, like the Android app. The transaction is sent with its installment_id. Once it is saved, the
// step update is sent. ac: installment-link-step-retry — a step update that fails is kept in a queue and
// sent again on the next add, edit, or visit to the Installments screen.
const QUEUE_KEY = 'pending_installment_steps';

const memory = new Map();
const memoryStorage = {
  getItem: (k) => (memory.has(k) ? memory.get(k) : null),
  setItem: (k, v) => { memory.set(k, v); },
};

// localStorage can throw or be missing in private windows and tests. The queue then lives in memory.
function defaultStorage() {
  try {
    const s = window.localStorage;
    s.getItem(QUEUE_KEY);
    return s;
  } catch {
    return memoryStorage;
  }
}

function readQueue(storage) {
  try { return JSON.parse(storage.getItem(QUEUE_KEY) || '{}'); } catch { return {}; }
}

export async function flushPendingSteps(storage = defaultStorage()) {
  const queue = readQueue(storage);
  for (const [id, row] of Object.entries(queue)) {
    try {
      await incrementInstallmentStep(Number(id), row.newStep, row.newStatus);
      delete queue[id];
    } catch {
      // stays in the queue for the next attempt
    }
  }
  storage.setItem(QUEUE_KEY, JSON.stringify(queue));
}

// Sends the step update, or queues it when the send fails. Returns true when nothing is left waiting.
async function applyStep(match, storage) {
  if (match.newStep === match.installment.current_step) return true;
  try {
    await incrementInstallmentStep(match.installment.id, match.newStep, match.newStatus);
    return true;
  } catch {
    const queue = readQueue(storage);
    const waiting = queue[match.installment.id];
    if (!waiting || waiting.newStep < match.newStep) {
      queue[match.installment.id] = { newStep: match.newStep, newStatus: match.newStatus };
    }
    storage.setItem(QUEUE_KEY, JSON.stringify(queue));
    return false;
  }
}

async function loadPlans() {
  try {
    const plans = await getInstallments();
    return Array.isArray(plans) ? plans : null;
  } catch {
    return null;
  }
}

function isCardExpense(payload) {
  return payload.tx_type === 'expense' && Boolean(payload.wallet);
}

// Saves a new transaction. Returns { match, stepSent }. match is the linked installment, or null.
export async function addWithLink(payload, storage = defaultStorage()) {
  await flushPendingSteps(storage);
  const plans = isCardExpense(payload) ? await loadPlans() : null;
  const match = plans ? matchInstallment(payload.item, payload.wallet, payload.amount, plans) : null;
  await addTransaction(match ? { ...payload, installment_id: match.installment.id } : payload);
  const stepSent = match ? await applyStep(match, storage) : true;
  return { match, stepSent };
}

// ac: installment-link-edit — saves an edit. installmentId is a plan picked by hand. unlink removes the
// link. autoLink matches an unlinked card expense. Returns { match, stepSent, linkedId }, where linkedId
// is undefined when the link did not change.
export async function updateWithLink(id, payload, { installmentId = null, unlink = false, autoLink = false } = {}, storage = defaultStorage()) {
  let match = null;
  if (installmentId != null) {
    const plan = (await loadPlans())?.find((p) => p.id === installmentId);
    match = plan ? advanceInstallment(plan, payload.item, false) : null;
  } else if (autoLink && isCardExpense(payload)) {
    const plans = await loadPlans();
    match = plans ? matchInstallment(payload.item, payload.wallet, payload.amount, plans) : null;
  }
  let linkedId;
  if (installmentId != null) linkedId = installmentId;
  else if (match) linkedId = match.installment.id;
  else if (unlink) linkedId = null;
  await updateTransaction(id, linkedId === undefined ? payload : { ...payload, installment_id: linkedId });
  const stepSent = match ? await applyStep(match, storage) : true;
  return { match, stepSent, linkedId };
}
