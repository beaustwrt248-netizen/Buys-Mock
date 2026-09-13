export const WORKSPACE_KEY = 'nova-next.workspace.v1';

const PRIORITIES = new Set(['low', 'medium', 'high']);
const PROJECT_STATUSES = new Set(['planned', 'in_progress', 'blocked', 'done']);
const EMPTY = () => ({ version: 1, tasks: [], projects: [] });

function clone(value) {
  return globalThis.structuredClone ? globalThis.structuredClone(value) : JSON.parse(JSON.stringify(value));
}

function timestamp(now) {
  const value = now();
  const date = value instanceof Date ? value : new Date(value);
  if (Number.isNaN(date.getTime())) throw new Error('WORKSPACE_CLOCK_INVALID');
  return date.toISOString();
}

function validDate(value) {
  if (value === null) return true;
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const [year, month, day] = value.split('-').map(Number);
  const date = new Date(Date.UTC(year, month - 1, day));
  return date.getUTCFullYear() === year && date.getUTCMonth() === month - 1 && date.getUTCDate() === day;
}

function requiredText(value, code) {
  const result = typeof value === 'string' ? value.trim() : '';
  if (!result) throw new Error(code);
  return result;
}

function optionalText(value) {
  return typeof value === 'string' ? value.trim() : '';
}

function requireUniqueIds(items) {
  const ids = new Set();
  for (const item of items) {
    if (ids.has(item.id)) throw new Error('WORKSPACE_SCHEMA_INVALID');
    ids.add(item.id);
  }
}

function normalizeTask(input, { existing = null, nowIso, projectIds = null } = {}) {
  const title = requiredText(input?.title ?? existing?.title, 'TASK_TITLE_REQUIRED');
  const notes = optionalText(input?.notes ?? existing?.notes ?? '');
  const dueDate = input?.dueDate === undefined ? (existing?.dueDate ?? null) : (input.dueDate || null);
  const priority = input?.priority ?? existing?.priority ?? 'medium';
  const projectId = input?.projectId === undefined ? (existing?.projectId ?? null) : (input.projectId || null);
  const completed = input?.completed === undefined ? (existing?.completed ?? false) : input.completed;

  if (!validDate(dueDate)) throw new Error('TASK_DUE_DATE_INVALID');
  if (!PRIORITIES.has(priority)) throw new Error('TASK_PRIORITY_INVALID');
  if (typeof completed !== 'boolean') throw new Error('TASK_COMPLETED_INVALID');
  if (projectId !== null && typeof projectId !== 'string') throw new Error('TASK_PROJECT_INVALID');
  if (projectIds && projectId !== null && !projectIds.has(projectId)) throw new Error('TASK_PROJECT_INVALID');

  return {
    id: existing?.id ?? input.id,
    title,
    notes,
    dueDate,
    priority,
    projectId,
    completed,
    createdAt: existing?.createdAt ?? input.createdAt ?? nowIso,
    updatedAt: nowIso
  };
}

function normalizeProject(input, { existing = null, nowIso } = {}) {
  const name = requiredText(input?.name ?? existing?.name, 'PROJECT_NAME_REQUIRED');
  const description = optionalText(input?.description ?? existing?.description ?? '');
  const status = input?.status ?? existing?.status ?? 'planned';
  const targetDate = input?.targetDate === undefined ? (existing?.targetDate ?? null) : (input.targetDate || null);
  const progress = input?.progress === undefined ? (existing?.progress ?? 0) : input.progress;

  if (!PROJECT_STATUSES.has(status)) throw new Error('PROJECT_STATUS_INVALID');
  if (!validDate(targetDate)) throw new Error('PROJECT_TARGET_DATE_INVALID');
  if (!Number.isInteger(progress) || progress < 0 || progress > 100) throw new Error('PROJECT_PROGRESS_INVALID');

  return {
    id: existing?.id ?? input.id,
    name,
    description,
    status,
    targetDate,
    progress,
    createdAt: existing?.createdAt ?? input.createdAt ?? nowIso,
    updatedAt: nowIso
  };
}

function validateLoadedState(parsed) {
  if (parsed?.version !== 1 || !Array.isArray(parsed.tasks) || !Array.isArray(parsed.projects)) {
    throw new Error('WORKSPACE_SCHEMA_INVALID');
  }

  const projects = parsed.projects.map(project => {
    if (!project || typeof project.id !== 'string' || !project.id || typeof project.createdAt !== 'string' || typeof project.updatedAt !== 'string') {
      throw new Error('WORKSPACE_SCHEMA_INVALID');
    }
    return normalizeProject(project, { existing: project, nowIso: project.updatedAt });
  });
  requireUniqueIds(projects);
  const projectIds = new Set(projects.map(project => project.id));

  const tasks = parsed.tasks.map(task => {
    if (!task || typeof task.id !== 'string' || !task.id || typeof task.createdAt !== 'string' || typeof task.updatedAt !== 'string') {
      throw new Error('WORKSPACE_SCHEMA_INVALID');
    }
    return normalizeTask(task, { existing: task, nowIso: task.updatedAt, projectIds });
  });
  requireUniqueIds(tasks);

  return { version: 1, tasks, projects };
}

export function createWorkspaceStore({
  storage = globalThis.localStorage,
  now = () => new Date(),
  idFactory = () => globalThis.crypto?.randomUUID?.() || `nova-next-${Date.now()}-${Math.random().toString(16).slice(2)}`
} = {}) {
  if (!storage || typeof storage.getItem !== 'function' || typeof storage.setItem !== 'function' || typeof storage.removeItem !== 'function') {
    throw new TypeError('WORKSPACE_STORAGE_REQUIRED');
  }
  if (typeof now !== 'function') throw new TypeError('WORKSPACE_CLOCK_REQUIRED');
  if (typeof idFactory !== 'function') throw new TypeError('WORKSPACE_ID_FACTORY_REQUIRED');

  let recoveryNotice = '';
  let state = load();

  function load() {
    const raw = storage.getItem(WORKSPACE_KEY);
    if (!raw) return EMPTY();
    try {
      return validateLoadedState(JSON.parse(raw));
    } catch {
      storage.removeItem(WORKSPACE_KEY);
      recoveryNotice = 'Workspace data was invalid and has been reset.';
      return EMPTY();
    }
  }

  function commit(next) {
    const canonical = clone(next);
    storage.setItem(WORKSPACE_KEY, JSON.stringify(canonical));
    state = canonical;
    return clone(state);
  }

  function snapshot() {
    return clone(state);
  }

  function projectIdSet() {
    return new Set(state.projects.map(project => project.id));
  }

  function createTask(input = {}) {
    const nowIso = timestamp(now);
    const id = String(idFactory() || '').trim();
    if (!id || state.tasks.some(task => task.id === id)) throw new Error('TASK_ID_INVALID');
    const task = normalizeTask({ ...input, id }, { nowIso, projectIds: projectIdSet() });
    commit({ ...state, tasks: [...state.tasks, task] });
    return clone(task);
  }

  function updateTask(id, patch = {}) {
    const index = state.tasks.findIndex(task => task.id === id);
    if (index < 0) throw new Error('TASK_NOT_FOUND');
    const nowIso = timestamp(now);
    const current = state.tasks[index];
    const updated = normalizeTask({ ...current, ...patch }, { existing: current, nowIso, projectIds: projectIdSet() });
    const tasks = state.tasks.map((task, taskIndex) => taskIndex === index ? updated : task);
    commit({ ...state, tasks });
    return clone(updated);
  }

  function toggleTask(id) {
    const current = state.tasks.find(task => task.id === id);
    if (!current) throw new Error('TASK_NOT_FOUND');
    return updateTask(id, { completed: !current.completed });
  }

  function deleteTask(id) {
    if (!state.tasks.some(task => task.id === id)) throw new Error('TASK_NOT_FOUND');
    commit({ ...state, tasks: state.tasks.filter(task => task.id !== id) });
    return true;
  }

  function createProject(input = {}) {
    const nowIso = timestamp(now);
    const id = String(idFactory() || '').trim();
    if (!id || state.projects.some(project => project.id === id)) throw new Error('PROJECT_ID_INVALID');
    const project = normalizeProject({ ...input, id }, { nowIso });
    commit({ ...state, projects: [...state.projects, project] });
    return clone(project);
  }

  function updateProject(id, patch = {}) {
    const index = state.projects.findIndex(project => project.id === id);
    if (index < 0) throw new Error('PROJECT_NOT_FOUND');
    const nowIso = timestamp(now);
    const current = state.projects[index];
    const updated = normalizeProject({ ...current, ...patch }, { existing: current, nowIso });
    const projects = state.projects.map((project, projectIndex) => projectIndex === index ? updated : project);
    commit({ ...state, projects });
    return clone(updated);
  }

  function deleteProject(id) {
    if (!state.projects.some(project => project.id === id)) throw new Error('PROJECT_NOT_FOUND');
    const nowIso = timestamp(now);
    const projects = state.projects.filter(project => project.id !== id);
    const tasks = state.tasks.map(task => task.projectId === id ? { ...task, projectId: null, updatedAt: nowIso } : task);
    commit({ version: 1, tasks, projects });
    return true;
  }

  function consumeRecoveryNotice() {
    const notice = recoveryNotice;
    recoveryNotice = '';
    return notice;
  }

  return Object.freeze({
    snapshot,
    createTask,
    updateTask,
    toggleTask,
    deleteTask,
    createProject,
    updateProject,
    deleteProject,
    consumeRecoveryNotice
  });
}
