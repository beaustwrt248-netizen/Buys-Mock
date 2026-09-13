import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorkspaceStore, WORKSPACE_KEY } from '../src/workspace-store.mjs';

class MemoryStorage {
  constructor() { this.map = new Map(); }
  getItem(key) { return this.map.has(key) ? this.map.get(key) : null; }
  setItem(key, value) { this.map.set(key, String(value)); }
  removeItem(key) { this.map.delete(key); }
}

function makeStore({ storage = new MemoryStorage(), ids = ['t1', 'p1', 't2'], now = '2026-09-13T01:00:00.000Z' } = {}) {
  let index = 0;
  return {
    storage,
    store: createWorkspaceStore({
      storage,
      now: () => new Date(now),
      idFactory: () => ids[index++] || `id-${index}`
    })
  };
}

test('task CRUD persists only in Nova Next namespace', () => {
  const { storage, store } = makeStore();
  const task = store.createTask({ title: ' Ship workspace ', priority: 'high' });
  assert.equal(task.id, 't1');
  assert.equal(task.title, 'Ship workspace');
  assert.equal(task.completed, false);
  assert.ok(storage.getItem(WORKSPACE_KEY));
  assert.equal(storage.map.size, 1);

  const updated = store.updateTask('t1', { notes: 'Ready', dueDate: '2026-09-14' });
  assert.equal(updated.notes, 'Ready');
  assert.equal(updated.createdAt, task.createdAt);
  assert.equal(updated.updatedAt, '2026-09-13T01:00:00.000Z');

  store.toggleTask('t1');
  assert.equal(store.snapshot().tasks[0].completed, true);
  store.deleteTask('t1');
  assert.equal(store.snapshot().tasks.length, 0);
});

test('task validation rejects invalid title priority and date', () => {
  const { store } = makeStore();
  assert.throws(() => store.createTask({ title: '   ' }), /TASK_TITLE_REQUIRED/);
  assert.throws(() => store.createTask({ title: 'x', priority: 'urgent' }), /TASK_PRIORITY_INVALID/);
  assert.throws(() => store.createTask({ title: 'x', dueDate: '13-09-2026' }), /TASK_DUE_DATE_INVALID/);
});

test('project CRUD normalizes values and deletion unlinks tasks without deleting them', () => {
  const { store } = makeStore({ ids: ['p1', 't1'] });
  const project = store.createProject({ name: ' Launch ', status: 'in_progress', progress: 40, targetDate: '2026-09-20' });
  assert.equal(project.name, 'Launch');
  const task = store.createTask({ title: 'Linked', projectId: 'p1' });
  assert.equal(task.projectId, 'p1');

  const updated = store.updateProject('p1', { progress: 75 });
  assert.equal(updated.progress, 75);
  store.deleteProject('p1');

  const state = store.snapshot();
  assert.equal(state.projects.length, 0);
  assert.equal(state.tasks.length, 1);
  assert.equal(state.tasks[0].projectId, null);
});

test('project validation rejects invalid name status progress and date', () => {
  const { store } = makeStore();
  assert.throws(() => store.createProject({ name: '' }), /PROJECT_NAME_REQUIRED/);
  assert.throws(() => store.createProject({ name: 'x', status: 'active' }), /PROJECT_STATUS_INVALID/);
  assert.throws(() => store.createProject({ name: 'x', progress: 101 }), /PROJECT_PROGRESS_INVALID/);
  assert.throws(() => store.createProject({ name: 'x', targetDate: '2026/09/20' }), /PROJECT_TARGET_DATE_INVALID/);
});

test('corrupt data clears only Nova Next workspace key and surfaces recovery once', () => {
  const storage = new MemoryStorage();
  storage.setItem('other.key', 'keep');
  storage.setItem(WORKSPACE_KEY, '{bad json');
  const store = createWorkspaceStore({ storage });
  assert.deepEqual(store.snapshot(), { version: 1, tasks: [], projects: [] });
  assert.equal(storage.getItem('other.key'), 'keep');
  assert.equal(storage.getItem(WORKSPACE_KEY), null);
  assert.match(store.consumeRecoveryNotice(), /reset/i);
  assert.equal(store.consumeRecoveryNotice(), '');
});

test('failed persistence leaves canonical state unchanged', () => {
  const storage = new MemoryStorage();
  const store = createWorkspaceStore({ storage, idFactory: () => 't1' });
  storage.setItem = () => { throw new Error('quota'); };
  assert.throws(() => store.createTask({ title: 'Will fail' }), /quota/);
  assert.equal(store.snapshot().tasks.length, 0);
});

test('duplicate generated task ids are rejected without mutating state', () => {
  const { store } = makeStore({ ids: ['dup', 'dup'] });
  store.createTask({ title: 'First' });
  assert.throws(() => store.createTask({ title: 'Second' }), /TASK_ID_INVALID/);
  assert.deepEqual(store.snapshot().tasks.map(task => task.title), ['First']);
});

test('duplicate generated project ids are rejected without mutating state', () => {
  const { store } = makeStore({ ids: ['dup', 'dup'] });
  store.createProject({ name: 'First' });
  assert.throws(() => store.createProject({ name: 'Second' }), /PROJECT_ID_INVALID/);
  assert.deepEqual(store.snapshot().projects.map(project => project.name), ['First']);
});

test('duplicate persisted workspace ids are treated as corrupt data', () => {
  const storage = new MemoryStorage();
  const stamp = '2026-09-13T01:00:00.000Z';
  storage.setItem(WORKSPACE_KEY, JSON.stringify({
    version: 1,
    projects: [],
    tasks: [
      { id: 'dup', title: 'One', notes: '', dueDate: null, priority: 'medium', projectId: null, completed: false, createdAt: stamp, updatedAt: stamp },
      { id: 'dup', title: 'Two', notes: '', dueDate: null, priority: 'medium', projectId: null, completed: false, createdAt: stamp, updatedAt: stamp }
    ]
  }));
  const store = createWorkspaceStore({ storage });
  assert.deepEqual(store.snapshot(), { version: 1, tasks: [], projects: [] });
  assert.equal(storage.getItem(WORKSPACE_KEY), null);
  assert.match(store.consumeRecoveryNotice(), /reset/i);
});
