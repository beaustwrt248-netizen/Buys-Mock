# Nova Next Workspace Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace hard-coded Nova Next Tasks/Projects placeholders with a local-first workspace and derive Calendar from the same canonical data.

**Architecture:** Add an isolated persistence module at `nova-next/src/workspace-store.mjs`, a pure runtime facade at `nova-next/src/workspace-runtime.mjs`, and a dedicated DOM controller at `nova-next/src/workspace-ui.mjs`. `app.js` owns construction/wiring; no backend mutation surface is added.

**Tech Stack:** Browser ES modules, `localStorage`, Node 22 built-in `node:test`/`assert`, existing Nova Next HTML/CSS/runtime patterns.

**Spec:** `docs/superpowers/specs/2026-09-13-nova-next-functionality-first-design.md`

## Global Constraints

- Persist only under storage key `nova-next.workspace.v1`.
- Task fields: title, notes, due date, priority, project link, completed state, created/updated timestamps.
- Project fields: name, description, status, optional target date, progress, created/updated timestamps.
- Project deletion clears linked task `projectId` values but does not delete those tasks.
- Corrupt workspace payloads clear only `nova-next.workspace.v1` and return an empty valid workspace plus recovery notice.
- Storage write failure must not be reported as persisted.
- Current production `nova/**` remains untouched.
- No network mutation, Guardian, pricing, release, OTA, signing, role-management, or destructive backend authority is added.

---

### Task 1: Local workspace store

**Files:**
- Create: `nova-next/src/workspace-store.mjs`
- Create: `nova-next/tests/workspace-store.test.mjs`

**Interfaces:**
- Produces: `createWorkspaceStore({ storage, now, idFactory })`
- Produces store methods: `snapshot()`, `createTask(input)`, `updateTask(id, patch)`, `toggleTask(id)`, `deleteTask(id)`, `createProject(input)`, `updateProject(id, patch)`, `deleteProject(id)`, `consumeRecoveryNotice()`.
- Canonical state: `{ version: 1, tasks: Task[], projects: Project[] }`.

- [ ] **Step 1: Write failing CRUD/isolation tests**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorkspaceStore, WORKSPACE_KEY } from '../src/workspace-store.mjs';

class MemoryStorage {
  map = new Map();
  getItem(key) { return this.map.has(key) ? this.map.get(key) : null; }
  setItem(key, value) { this.map.set(key, String(value)); }
  removeItem(key) { this.map.delete(key); }
}

test('task CRUD persists only in Nova Next namespace', () => {
  const storage = new MemoryStorage();
  const store = createWorkspaceStore({ storage, now: () => new Date('2026-09-13T01:00:00Z'), idFactory: () => 't1' });
  const task = store.createTask({ title: 'Ship workspace', priority: 'high' });
  assert.equal(task.id, 't1');
  assert.equal(task.completed, false);
  assert.ok(storage.getItem(WORKSPACE_KEY));
  assert.equal(storage.map.size, 1);
  store.toggleTask('t1');
  assert.equal(store.snapshot().tasks[0].completed, true);
  store.deleteTask('t1');
  assert.equal(store.snapshot().tasks.length, 0);
});
```

- [ ] **Step 2: Run test and verify RED**

Run: `node --test nova-next/tests/workspace-store.test.mjs`

Expected: FAIL because `workspace-store.mjs` and exports do not exist.

- [ ] **Step 3: Implement validated canonical store**

```js
export const WORKSPACE_KEY = 'nova-next.workspace.v1';
const EMPTY = () => ({ version: 1, tasks: [], projects: [] });
const PRIORITIES = new Set(['low', 'medium', 'high']);
const PROJECT_STATUSES = new Set(['planned', 'in_progress', 'blocked', 'done']);

export function createWorkspaceStore({ storage = globalThis.localStorage, now = () => new Date(), idFactory = () => crypto.randomUUID() } = {}) {
  let recoveryNotice = '';
  let state = load();

  function load() {
    const raw = storage?.getItem?.(WORKSPACE_KEY);
    if (!raw) return EMPTY();
    try {
      const parsed = JSON.parse(raw);
      if (parsed?.version !== 1 || !Array.isArray(parsed.tasks) || !Array.isArray(parsed.projects)) throw new Error('WORKSPACE_SCHEMA_INVALID');
      return parsed;
    } catch {
      storage?.removeItem?.(WORKSPACE_KEY);
      recoveryNotice = 'Workspace data was invalid and has been reset.';
      return EMPTY();
    }
  }

  function commit(next) {
    storage.setItem(WORKSPACE_KEY, JSON.stringify(next));
    state = next;
    return structuredClone(state);
  }

  function snapshot() { return structuredClone(state); }
  // Implement create/update/toggle/delete using immutable arrays and commit only after validation.
  return Object.freeze({ snapshot, createTask, updateTask, toggleTask, deleteTask, createProject, updateProject, deleteProject, consumeRecoveryNotice });
}
```

Implementation requirements inside this file:
- Trim titles/names; reject empty values with `TASK_TITLE_REQUIRED` / `PROJECT_NAME_REQUIRED`.
- Normalize missing task fields to `notes:''`, `dueDate:null`, `priority:'medium'`, `projectId:null`, `completed:false`.
- Normalize missing project fields to `description:''`, `status:'planned'`, `targetDate:null`, `progress:0`.
- Validate ISO `YYYY-MM-DD` date strings when present.
- Validate project progress as integer `0..100`.
- Preserve `createdAt`; replace `updatedAt` on updates/toggles.
- `deleteProject(id)` clears `projectId` on linked tasks in the same committed transaction.

- [ ] **Step 4: Add failure/recovery tests**

```js
test('corrupt data clears only Nova Next workspace key', () => {
  const storage = new MemoryStorage();
  storage.setItem('other.key', 'keep');
  storage.setItem(WORKSPACE_KEY, '{bad json');
  const store = createWorkspaceStore({ storage });
  assert.deepEqual(store.snapshot(), { version: 1, tasks: [], projects: [] });
  assert.equal(storage.getItem('other.key'), 'keep');
  assert.match(store.consumeRecoveryNotice(), /reset/i);
});

test('failed persistence leaves canonical state unchanged', () => {
  const storage = new MemoryStorage();
  const store = createWorkspaceStore({ storage, idFactory: () => 't1' });
  storage.setItem = () => { throw new Error('quota'); };
  assert.throws(() => store.createTask({ title: 'Will fail' }), /quota/);
  assert.equal(store.snapshot().tasks.length, 0);
});
```

- [ ] **Step 5: Run store tests to GREEN**

Run: `node --test nova-next/tests/workspace-store.test.mjs`

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add nova-next/src/workspace-store.mjs nova-next/tests/workspace-store.test.mjs
git commit -m "feat(nova-next): add local workspace store"
```

---

### Task 2: Workspace runtime and derived calendar

**Files:**
- Create: `nova-next/src/workspace-runtime.mjs`
- Create: `nova-next/tests/workspace-runtime.test.mjs`

**Interfaces:**
- Consumes: workspace store from Task 1.
- Produces: `createWorkspaceRuntime({ store, today })`.
- Produces methods: `listTasks(filter)`, `listProjects()`, all store CRUD passthroughs, `calendarItems()`.

- [ ] **Step 1: Write failing filter/calendar tests**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorkspaceRuntime } from '../src/workspace-runtime.mjs';

const state = {
  version: 1,
  tasks: [
    { id:'a', title:'Today', dueDate:'2026-09-13', completed:false, priority:'high', projectId:null, notes:'', createdAt:'x', updatedAt:'x' },
    { id:'b', title:'Later', dueDate:'2026-09-15', completed:false, priority:'low', projectId:null, notes:'', createdAt:'x', updatedAt:'x' },
    { id:'c', title:'Done', dueDate:'2026-09-12', completed:true, priority:'medium', projectId:null, notes:'', createdAt:'x', updatedAt:'x' }
  ],
  projects: [{ id:'p1', name:'Launch', description:'', status:'in_progress', targetDate:'2026-09-20', progress:50, createdAt:'x', updatedAt:'x' }]
};
const store = { snapshot: () => structuredClone(state) };
const runtime = createWorkspaceRuntime({ store, today: () => '2026-09-13' });

test('filters tasks deterministically', () => {
  assert.deepEqual(runtime.listTasks('today').map(x => x.id), ['a']);
  assert.deepEqual(runtime.listTasks('upcoming').map(x => x.id), ['b']);
  assert.deepEqual(runtime.listTasks('done').map(x => x.id), ['c']);
});

test('calendar combines dated tasks and projects sorted by date', () => {
  assert.deepEqual(runtime.calendarItems().map(x => [x.kind, x.date]), [
    ['task','2026-09-12'], ['task','2026-09-13'], ['task','2026-09-15'], ['project','2026-09-20']
  ]);
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test nova-next/tests/workspace-runtime.test.mjs`

Expected: FAIL because module does not exist.

- [ ] **Step 3: Implement runtime**

```js
export function createWorkspaceRuntime({ store, today = () => new Date().toISOString().slice(0, 10) } = {}) {
  if (!store) throw new TypeError('WORKSPACE_STORE_REQUIRED');
  const current = () => store.snapshot();
  function listTasks(filter = 'all') {
    const day = today();
    return current().tasks.filter(task => {
      if (filter === 'today') return !task.completed && task.dueDate === day;
      if (filter === 'upcoming') return !task.completed && task.dueDate && task.dueDate > day;
      if (filter === 'done') return task.completed;
      return true;
    });
  }
  function calendarItems() {
    const state = current();
    return [
      ...state.tasks.filter(x => x.dueDate).map(x => ({ kind:'task', id:x.id, label:x.title, date:x.dueDate, completed:x.completed })),
      ...state.projects.filter(x => x.targetDate).map(x => ({ kind:'project', id:x.id, label:x.name, date:x.targetDate, completed:x.status === 'done' }))
    ].sort((a,b) => a.date.localeCompare(b.date) || a.label.localeCompare(b.label));
  }
  return Object.freeze({ listTasks, listProjects: () => current().projects, calendarItems,
    createTask: store.createTask, updateTask: store.updateTask, toggleTask: store.toggleTask, deleteTask: store.deleteTask,
    createProject: store.createProject, updateProject: store.updateProject, deleteProject: store.deleteProject,
    consumeRecoveryNotice: store.consumeRecoveryNotice });
}
```

- [ ] **Step 4: Run runtime tests to GREEN**

Run: `node --test nova-next/tests/workspace-runtime.test.mjs`

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add nova-next/src/workspace-runtime.mjs nova-next/tests/workspace-runtime.test.mjs
git commit -m "feat(nova-next): add workspace runtime and calendar model"
```

---

### Task 3: Tasks, Projects and Calendar UI

**Files:**
- Create: `nova-next/src/workspace-ui.mjs`
- Create: `nova-next/tests/workspace-ui-contract.test.mjs`
- Modify: `nova-next/index.html`
- Modify: `nova-next/live.css`

**Interfaces:**
- Consumes: `workspaceRuntime`, `documentObj`, `onToast`, `onNavigate`.
- Produces: `createWorkspaceUi(...).bind()` and `.routeChanged(route)`.

- [ ] **Step 1: Write failing UI contract**

```js
import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const source = await readFile(new URL('../src/workspace-ui.mjs', import.meta.url), 'utf8').catch(() => '');

test('hard-coded demo task/project data is removed', () => {
  assert.equal(html.includes('Update Morley Buys app'), false);
  assert.equal(html.includes('Device Database'), false);
});

test('workspace UI owns tasks projects and calendar routes', () => {
  for (const token of ['renderTasks', 'renderProjects', 'renderCalendar', 'openTaskForm', 'openProjectForm']) {
    assert.ok(source.includes(token), token);
  }
});
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test nova-next/tests/workspace-ui-contract.test.mjs`

Expected: FAIL because demo rows still exist and `workspace-ui.mjs` does not exist.

- [ ] **Step 3: Replace static route contents with runtime-owned shells**

In `index.html`, keep each `<section class="page" data-route="tasks|projects|calendar">` and headings, but remove all hard-coded task/project cards. Add stable containers:

```html
<div id="novaNextTaskFilters" class="filter-tabs" role="group" aria-label="Task filters"></div>
<div id="novaNextTaskList" class="task-list"></div>
<button id="novaNextAddTask" class="primary-button sticky-action" type="button">＋ Add Task</button>
```

```html
<div id="novaNextProjectList" class="project-list"></div>
```

```html
<div id="novaNextCalendarList" class="feature-stack"></div>
```

- [ ] **Step 4: Implement dedicated workspace UI controller**

`workspace-ui.mjs` must:
- render filters `all/today/upcoming/done` with `aria-pressed` state;
- render empty state when a filter has no tasks;
- open task form sheet with title, notes, due date, priority, project selection;
- edit existing tasks via the same form;
- toggle completion from checkbox;
- require `window.confirm('Delete this task?')` before local deletion;
- render projects from runtime, with task completion context `completed/total` when linked tasks exist;
- open project form with name, description, status, target date, progress;
- require `window.confirm('Delete this project? Linked tasks will be kept and unlinked.')` before project deletion;
- render calendar items grouped by date, with empty-state buttons navigating to Tasks and Projects;
- catch persistence errors and call `onToast('Workspace changes could not be saved.', 'error')` without falsely re-rendering an unpersisted state;
- consume and show the recovery notice once after bind.

- [ ] **Step 5: Add focused styles**

Add to `live.css` only classes used by this controller: `.workspace-empty`, `.workspace-form`, `.workspace-actions`, `.workspace-meta`, `.calendar-date-group`, `.calendar-item`. Reuse existing button/card tokens; do not introduce a second visual system.

- [ ] **Step 6: Run UI contract to GREEN**

Run: `node --test nova-next/tests/workspace-ui-contract.test.mjs`

Expected: PASS.

- [ ] **Step 7: Commit**

```bash
git add nova-next/src/workspace-ui.mjs nova-next/tests/workspace-ui-contract.test.mjs nova-next/index.html nova-next/live.css
git commit -m "feat(nova-next): make tasks projects and calendar interactive"
```

---

### Task 4: App wiring and regression verification

**Files:**
- Modify: `nova-next/app.js`
- Modify: `nova-next/tests/isolation.test.mjs` only if a new explicit namespace assertion is needed.

**Interfaces:**
- Consumes: `createWorkspaceStore`, `createWorkspaceRuntime`, `createWorkspaceUi`.
- Produces: one workspace controller wired into existing route changes without changing current Auth/Chat/Feature UI behavior.

- [ ] **Step 1: Write failing wiring assertion**

Add to `workspace-ui-contract.test.mjs`:

```js
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');
assert.ok(app.includes('createWorkspaceStore'));
assert.ok(app.includes('createWorkspaceRuntime'));
assert.ok(app.includes('createWorkspaceUi'));
```

- [ ] **Step 2: Run and verify RED**

Run: `node --test nova-next/tests/workspace-ui-contract.test.mjs`

Expected: FAIL on missing app wiring.

- [ ] **Step 3: Wire workspace construction in `app.js`**

Use this ownership pattern:

```js
import { createWorkspaceStore } from './src/workspace-store.mjs';
import { createWorkspaceRuntime } from './src/workspace-runtime.mjs';
import { createWorkspaceUi } from './src/workspace-ui.mjs';

const workspaceStore = createWorkspaceStore();
const workspaceRuntime = createWorkspaceRuntime({ store: workspaceStore });
const workspaceUi = createWorkspaceUi({
  workspaceRuntime,
  documentObj: document,
  windowObj: window,
  onNavigate: route => setRoute(route),
  onToast: showToast
});
```

Call `workspaceUi.bind()` once after shell setup. In `setRoute`, call `workspaceUi.routeChanged(currentRoute)` alongside existing `featureUi.routeChanged`.

- [ ] **Step 4: Run all Nova Next contracts and syntax checks**

Run:

```bash
node --test nova-next/tests/*.test.mjs
node --check nova-next/app.js
find nova-next/src -type f -name '*.mjs' -print0 | sort -z | xargs -0 -n1 node --check
```

Expected: all tests PASS; all syntax checks exit 0.

- [ ] **Step 5: Verify current Nova isolation**

Run:

```bash
git diff --name-only main...HEAD | grep '^nova/' && exit 1 || true
```

Expected: no output.

- [ ] **Step 6: Commit**

```bash
git add nova-next/app.js nova-next/tests/workspace-ui-contract.test.mjs
git commit -m "feat(nova-next): wire local workspace runtime"
```
