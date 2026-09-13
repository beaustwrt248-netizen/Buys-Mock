import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const html = await readFile(new URL('../index.html', import.meta.url), 'utf8');
const source = await readFile(new URL('../src/workspace-ui.mjs', import.meta.url), 'utf8').catch(() => '');
const app = await readFile(new URL('../app.js', import.meta.url), 'utf8');

test('hard-coded demo task and project data is removed', () => {
  for (const token of ['Update Morley Buys app', 'Review new device data', 'Device Database', 'Admin Tools']) {
    assert.equal(html.includes(token), false, token);
  }
});

test('workspace routes expose stable runtime-owned containers', () => {
  for (const token of ['id="novaNextTaskFilters"', 'id="novaNextTaskList"', 'id="novaNextAddTask"', 'id="novaNextProjectList"', 'id="novaNextAddProject"', 'id="novaNextCalendarList"']) {
    assert.ok(html.includes(token), token);
  }
});

test('workspace UI owns task project and calendar behavior', () => {
  for (const token of ['createWorkspaceUi', 'renderTasks', 'renderProjects', 'renderCalendar', 'openTaskForm', 'openProjectForm', "Delete this task?", 'Delete this project? Linked tasks will be kept and unlinked.']) {
    assert.ok(source.includes(token), token);
  }
});

test('workspace UI reports persistence failure without fake success', () => {
  assert.ok(source.includes("Workspace changes could not be saved."));
  assert.ok(source.includes("onToast"));
});

test('app wires the local workspace store runtime and UI', () => {
  assert.ok(app.includes('createWorkspaceStore'));
  assert.ok(app.includes('createWorkspaceRuntime'));
  assert.ok(app.includes('createWorkspaceUi'));
  assert.ok(app.includes('workspaceUi.bind()'));
  assert.ok(app.includes('workspaceUi?.routeChanged(currentRoute)'));
});
