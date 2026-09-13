import test from 'node:test';
import assert from 'node:assert/strict';
import { createWorkspaceRuntime } from '../src/workspace-runtime.mjs';

function makeState() {
  return {
    version: 1,
    tasks: [
      { id:'a', title:'Today', dueDate:'2026-09-13', completed:false, priority:'high', projectId:null, notes:'', createdAt:'x', updatedAt:'x' },
      { id:'b', title:'Later', dueDate:'2026-09-15', completed:false, priority:'low', projectId:null, notes:'', createdAt:'x', updatedAt:'x' },
      { id:'c', title:'Done', dueDate:'2026-09-12', completed:true, priority:'medium', projectId:null, notes:'', createdAt:'x', updatedAt:'x' },
      { id:'d', title:'No date', dueDate:null, completed:false, priority:'medium', projectId:null, notes:'', createdAt:'x', updatedAt:'x' }
    ],
    projects: [
      { id:'p1', name:'Launch', description:'', status:'in_progress', targetDate:'2026-09-20', progress:50, createdAt:'x', updatedAt:'x' },
      { id:'p2', name:'No date project', description:'', status:'planned', targetDate:null, progress:0, createdAt:'x', updatedAt:'x' }
    ]
  };
}

function makeStore() {
  let state = makeState();
  const calls = [];
  const store = {
    snapshot: () => structuredClone(state),
    createTask: input => (calls.push(['createTask', input]), { id:'new-task', ...input }),
    updateTask: (id, patch) => (calls.push(['updateTask', id, patch]), { id, ...patch }),
    toggleTask: id => (calls.push(['toggleTask', id]), true),
    deleteTask: id => (calls.push(['deleteTask', id]), true),
    createProject: input => (calls.push(['createProject', input]), { id:'new-project', ...input }),
    updateProject: (id, patch) => (calls.push(['updateProject', id, patch]), { id, ...patch }),
    deleteProject: id => (calls.push(['deleteProject', id]), true),
    consumeRecoveryNotice: () => 'Recovered'
  };
  return { store, calls, setState: next => { state = next; } };
}

test('filters tasks deterministically', () => {
  const { store } = makeStore();
  const runtime = createWorkspaceRuntime({ store, today: () => '2026-09-13' });
  assert.deepEqual(runtime.listTasks('all').map(x => x.id), ['a','b','c','d']);
  assert.deepEqual(runtime.listTasks('today').map(x => x.id), ['a']);
  assert.deepEqual(runtime.listTasks('upcoming').map(x => x.id), ['b']);
  assert.deepEqual(runtime.listTasks('done').map(x => x.id), ['c']);
});

test('rejects unknown task filter', () => {
  const { store } = makeStore();
  const runtime = createWorkspaceRuntime({ store, today: () => '2026-09-13' });
  assert.throws(() => runtime.listTasks('later-ish'), /TASK_FILTER_INVALID/);
});

test('calendar combines dated tasks and projects sorted by date and label', () => {
  const { store } = makeStore();
  const runtime = createWorkspaceRuntime({ store, today: () => '2026-09-13' });
  assert.deepEqual(runtime.calendarItems().map(x => [x.kind, x.id, x.date, x.completed]), [
    ['task','c','2026-09-12',true],
    ['task','a','2026-09-13',false],
    ['task','b','2026-09-15',false],
    ['project','p1','2026-09-20',false]
  ]);
});

test('workspace runtime passes CRUD through to the store', () => {
  const { store, calls } = makeStore();
  const runtime = createWorkspaceRuntime({ store, today: () => '2026-09-13' });
  runtime.createTask({ title:'x' });
  runtime.updateTask('a', { title:'y' });
  runtime.toggleTask('a');
  runtime.deleteTask('a');
  runtime.createProject({ name:'p' });
  runtime.updateProject('p1', { progress:90 });
  runtime.deleteProject('p1');
  assert.deepEqual(calls.map(x => x[0]), ['createTask','updateTask','toggleTask','deleteTask','createProject','updateProject','deleteProject']);
  assert.equal(runtime.consumeRecoveryNotice(), 'Recovered');
  assert.equal(runtime.listProjects().length, 2);
});
