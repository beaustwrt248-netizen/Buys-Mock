function el(documentObj, tag, className = '', text = '') {
  const node = documentObj.createElement(tag);
  if (className) node.className = className;
  if (text) node.textContent = text;
  return node;
}

function clear(node) {
  while (node?.firstChild) node.firstChild.remove();
}

function titleCase(value) {
  return String(value || '').replaceAll('_', ' ').replace(/\b\w/g, character => character.toUpperCase());
}

function formatDate(value) {
  if (!value) return 'No date';
  const date = new Date(`${value}T00:00:00`);
  return Number.isNaN(date.getTime()) ? value : new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' }).format(date);
}

export function createWorkspaceUi({
  workspaceRuntime,
  documentObj = globalThis.document,
  windowObj = globalThis.window,
  onToast = () => {},
  onNavigate = () => {}
} = {}) {
  if (!workspaceRuntime) throw new TypeError('WORKSPACE_RUNTIME_REQUIRED');
  if (!documentObj) throw new TypeError('DOCUMENT_REQUIRED');

  let bound = false;
  let activeTaskFilter = 'all';

  function showSheet(title, contentNode) {
    documentObj.querySelector('.workspace-sheet-backdrop')?.remove();
    const backdrop = el(documentObj, 'div', 'feature-sheet-backdrop workspace-sheet-backdrop');
    const sheet = el(documentObj, 'section', 'feature-sheet');
    sheet.setAttribute('role', 'dialog');
    sheet.setAttribute('aria-modal', 'true');
    const head = el(documentObj, 'div', 'feature-sheet-head');
    head.append(el(documentObj, 'h2', '', title));
    const close = el(documentObj, 'button', 'icon-button', '×');
    close.type = 'button';
    close.setAttribute('aria-label', 'Close');
    close.addEventListener('click', () => backdrop.remove());
    head.append(close);
    sheet.append(head, contentNode);
    backdrop.append(sheet);
    backdrop.addEventListener('click', event => { if (event.target === backdrop) backdrop.remove(); });
    documentObj.body.append(backdrop);
    return backdrop;
  }

  function persistenceFailure(error) {
    onToast('Workspace changes could not be saved.', 'error');
    console.error('nova-next workspace persistence', error);
  }

  function refreshWorkspaceViews() {
    renderTasks();
    renderProjects();
    renderCalendar();
  }

  function renderTaskFilters() {
    const container = documentObj.getElementById('novaNextTaskFilters');
    if (!container) return;
    clear(container);
    for (const filter of ['all', 'today', 'upcoming', 'done']) {
      const button = el(documentObj, 'button', filter === activeTaskFilter ? 'is-selected' : '', titleCase(filter));
      button.type = 'button';
      button.dataset.taskFilter = filter;
      button.setAttribute('aria-pressed', String(filter === activeTaskFilter));
      button.addEventListener('click', () => {
        activeTaskFilter = filter;
        renderTaskFilters();
        renderTasks();
      });
      container.append(button);
    }
  }

  function taskMeta(task) {
    const parts = [];
    if (task.dueDate) parts.push(formatDate(task.dueDate));
    const project = workspaceRuntime.listProjects().find(item => item.id === task.projectId);
    if (project) parts.push(project.name);
    return parts.join(' · ') || 'No due date';
  }

  function renderTasks() {
    renderTaskFilters();
    const list = documentObj.getElementById('novaNextTaskList');
    if (!list) return;
    clear(list);
    const tasks = workspaceRuntime.listTasks(activeTaskFilter);
    if (!tasks.length) {
      const empty = el(documentObj, 'div', 'workspace-empty');
      empty.append(el(documentObj, 'strong', '', 'Nothing here yet'), el(documentObj, 'p', '', activeTaskFilter === 'all' ? 'Add your first task to start organising work.' : `No ${activeTaskFilter} tasks.`));
      list.append(empty);
      return;
    }

    for (const task of tasks) {
      const row = el(documentObj, 'article', `workspace-task${task.completed ? ' is-complete' : ''}`);
      const check = documentObj.createElement('input');
      check.type = 'checkbox';
      check.checked = task.completed;
      check.setAttribute('aria-label', `${task.completed ? 'Reopen' : 'Complete'} ${task.title}`);
      check.addEventListener('change', () => {
        try {
          workspaceRuntime.toggleTask(task.id);
          refreshWorkspaceViews();
        } catch (error) {
          check.checked = task.completed;
          persistenceFailure(error);
        }
      });
      const copy = el(documentObj, 'div', 'workspace-task-copy');
      copy.append(el(documentObj, 'strong', '', task.title), el(documentObj, 'small', 'workspace-meta', taskMeta(task)));
      if (task.notes) copy.append(el(documentObj, 'p', '', task.notes));
      const priority = el(documentObj, 'em', `priority ${task.priority}`, titleCase(task.priority));
      const actions = el(documentObj, 'div', 'workspace-actions');
      const edit = el(documentObj, 'button', 'link-button', 'Edit');
      edit.type = 'button';
      edit.addEventListener('click', () => openTaskForm(task));
      const remove = el(documentObj, 'button', 'link-button', 'Delete');
      remove.type = 'button';
      remove.addEventListener('click', () => {
        if (!windowObj.confirm('Delete this task?')) return;
        try {
          workspaceRuntime.deleteTask(task.id);
          refreshWorkspaceViews();
          onToast('Task deleted.');
        } catch (error) {
          persistenceFailure(error);
        }
      });
      actions.append(edit, remove);
      row.append(check, copy, priority, actions);
      list.append(row);
    }
  }

  function fieldLabel(text, control) {
    const label = el(documentObj, 'label', 'workspace-field');
    label.append(el(documentObj, 'span', '', text), control);
    return label;
  }

  function openTaskForm(task = null) {
    const form = el(documentObj, 'form', 'workspace-form');
    const title = documentObj.createElement('input');
    title.name = 'title';
    title.required = true;
    title.maxLength = 180;
    title.value = task?.title || '';
    const notes = documentObj.createElement('textarea');
    notes.name = 'notes';
    notes.rows = 4;
    notes.value = task?.notes || '';
    const dueDate = documentObj.createElement('input');
    dueDate.name = 'dueDate';
    dueDate.type = 'date';
    dueDate.value = task?.dueDate || '';
    const priority = documentObj.createElement('select');
    for (const value of ['low', 'medium', 'high']) {
      const option = documentObj.createElement('option');
      option.value = value;
      option.textContent = titleCase(value);
      option.selected = value === (task?.priority || 'medium');
      priority.append(option);
    }
    const project = documentObj.createElement('select');
    const noProject = documentObj.createElement('option');
    noProject.value = '';
    noProject.textContent = 'No project';
    project.append(noProject);
    for (const item of workspaceRuntime.listProjects()) {
      const option = documentObj.createElement('option');
      option.value = item.id;
      option.textContent = item.name;
      option.selected = item.id === task?.projectId;
      project.append(option);
    }
    const actions = el(documentObj, 'div', 'workspace-actions');
    const save = el(documentObj, 'button', 'primary-button', task ? 'Save Task' : 'Add Task');
    save.type = 'submit';
    actions.append(save);
    form.append(fieldLabel('Title', title), fieldLabel('Notes', notes), fieldLabel('Due date', dueDate), fieldLabel('Priority', priority), fieldLabel('Project', project), actions);
    const backdrop = showSheet(task ? 'Edit Task' : 'New Task', form);
    form.addEventListener('submit', event => {
      event.preventDefault();
      save.disabled = true;
      try {
        const input = { title: title.value, notes: notes.value, dueDate: dueDate.value || null, priority: priority.value, projectId: project.value || null };
        if (task) workspaceRuntime.updateTask(task.id, input);
        else workspaceRuntime.createTask(input);
        backdrop.remove();
        refreshWorkspaceViews();
        onToast(task ? 'Task updated.' : 'Task added.');
      } catch (error) {
        save.disabled = false;
        if (String(error?.message || error).includes('REQUIRED') || String(error?.message || error).includes('INVALID')) {
          onToast('Check the task details and try again.', 'error');
        } else {
          persistenceFailure(error);
        }
      }
    });
    title.focus();
  }

  function linkedTaskContext(projectId) {
    const linked = workspaceRuntime.listTasks('all').filter(task => task.projectId === projectId);
    if (!linked.length) return 'No linked tasks';
    return `${linked.filter(task => task.completed).length}/${linked.length} tasks complete`;
  }

  function renderProjects() {
    const list = documentObj.getElementById('novaNextProjectList');
    if (!list) return;
    clear(list);
    const projects = workspaceRuntime.listProjects();
    if (!projects.length) {
      const empty = el(documentObj, 'div', 'workspace-empty');
      empty.append(el(documentObj, 'strong', '', 'No projects yet'), el(documentObj, 'p', '', 'Create a project to group related work.'));
      list.append(empty);
      return;
    }
    for (const project of projects) {
      const card = el(documentObj, 'article', 'workspace-project');
      const head = el(documentObj, 'div', 'project-head');
      const icon = el(documentObj, 'span', 'project-icon blue', '▣');
      const copy = el(documentObj, 'div');
      copy.append(el(documentObj, 'strong', '', project.name), el(documentObj, 'small', '', project.description || linkedTaskContext(project.id)));
      head.append(icon, copy);
      const meta = el(documentObj, 'div', 'workspace-meta', `${titleCase(project.status)}${project.targetDate ? ` · ${formatDate(project.targetDate)}` : ''} · ${linkedTaskContext(project.id)}`);
      const track = el(documentObj, 'div', 'progress-track');
      const progress = documentObj.createElement('span');
      progress.style.width = `${project.progress}%`;
      track.append(progress);
      const actions = el(documentObj, 'div', 'workspace-actions');
      const edit = el(documentObj, 'button', 'link-button', 'Edit');
      edit.type = 'button';
      edit.addEventListener('click', () => openProjectForm(project));
      const remove = el(documentObj, 'button', 'link-button', 'Delete');
      remove.type = 'button';
      remove.addEventListener('click', () => {
        if (!windowObj.confirm('Delete this project? Linked tasks will be kept and unlinked.')) return;
        try {
          workspaceRuntime.deleteProject(project.id);
          refreshWorkspaceViews();
          onToast('Project deleted.');
        } catch (error) {
          persistenceFailure(error);
        }
      });
      actions.append(edit, remove);
      card.append(head, meta, track, el(documentObj, 'em', '', `${project.progress}%`), actions);
      list.append(card);
    }
  }

  function openProjectForm(project = null) {
    const form = el(documentObj, 'form', 'workspace-form');
    const name = documentObj.createElement('input');
    name.required = true;
    name.maxLength = 180;
    name.value = project?.name || '';
    const description = documentObj.createElement('textarea');
    description.rows = 4;
    description.value = project?.description || '';
    const status = documentObj.createElement('select');
    for (const value of ['planned', 'in_progress', 'blocked', 'done']) {
      const option = documentObj.createElement('option');
      option.value = value;
      option.textContent = titleCase(value);
      option.selected = value === (project?.status || 'planned');
      status.append(option);
    }
    const targetDate = documentObj.createElement('input');
    targetDate.type = 'date';
    targetDate.value = project?.targetDate || '';
    const progress = documentObj.createElement('input');
    progress.type = 'number';
    progress.min = '0';
    progress.max = '100';
    progress.step = '1';
    progress.value = String(project?.progress ?? 0);
    const actions = el(documentObj, 'div', 'workspace-actions');
    const save = el(documentObj, 'button', 'primary-button', project ? 'Save Project' : 'Create Project');
    save.type = 'submit';
    actions.append(save);
    form.append(fieldLabel('Name', name), fieldLabel('Description', description), fieldLabel('Status', status), fieldLabel('Target date', targetDate), fieldLabel('Progress %', progress), actions);
    const backdrop = showSheet(project ? 'Edit Project' : 'New Project', form);
    form.addEventListener('submit', event => {
      event.preventDefault();
      save.disabled = true;
      try {
        const input = { name: name.value, description: description.value, status: status.value, targetDate: targetDate.value || null, progress: Number(progress.value) };
        if (project) workspaceRuntime.updateProject(project.id, input);
        else workspaceRuntime.createProject(input);
        backdrop.remove();
        refreshWorkspaceViews();
        onToast(project ? 'Project updated.' : 'Project created.');
      } catch (error) {
        save.disabled = false;
        if (String(error?.message || error).includes('REQUIRED') || String(error?.message || error).includes('INVALID')) {
          onToast('Check the project details and try again.', 'error');
        } else {
          persistenceFailure(error);
        }
      }
    });
    name.focus();
  }

  function renderCalendar() {
    const list = documentObj.getElementById('novaNextCalendarList');
    if (!list) return;
    clear(list);
    const items = workspaceRuntime.calendarItems();
    if (!items.length) {
      const empty = el(documentObj, 'div', 'workspace-empty');
      empty.append(el(documentObj, 'strong', '', 'No dated work yet'), el(documentObj, 'p', '', 'Add dates to tasks or projects and they will appear here.'));
      const actions = el(documentObj, 'div', 'workspace-actions');
      const tasks = el(documentObj, 'button', 'small-primary', 'Open Tasks');
      tasks.type = 'button';
      tasks.addEventListener('click', () => onNavigate('tasks'));
      const projects = el(documentObj, 'button', 'secondary-button', 'Open Projects');
      projects.type = 'button';
      projects.addEventListener('click', () => onNavigate('projects'));
      actions.append(tasks, projects);
      empty.append(actions);
      list.append(empty);
      return;
    }

    const groups = new Map();
    for (const item of items) {
      if (!groups.has(item.date)) groups.set(item.date, []);
      groups.get(item.date).push(item);
    }
    for (const [date, dayItems] of groups) {
      const group = el(documentObj, 'section', 'calendar-date-group');
      group.append(el(documentObj, 'h2', '', formatDate(date)));
      for (const item of dayItems) {
        const card = el(documentObj, 'article', `calendar-item${item.completed ? ' is-complete' : ''}`);
        card.append(el(documentObj, 'span', 'calendar-kind', item.kind === 'task' ? '☑' : '▣'), el(documentObj, 'strong', '', item.label), el(documentObj, 'small', 'workspace-meta', titleCase(item.kind)));
        group.append(card);
      }
      list.append(group);
    }
  }

  function bind() {
    if (bound) return;
    bound = true;
    documentObj.getElementById('novaNextAddTask')?.addEventListener('click', () => openTaskForm());
    documentObj.getElementById('novaNextAddProject')?.addEventListener('click', () => openProjectForm());
    const recovery = workspaceRuntime.consumeRecoveryNotice();
    if (recovery) onToast(recovery, 'error');
    refreshWorkspaceViews();
  }

  function routeChanged(route) {
    if (route === 'tasks') renderTasks();
    if (route === 'projects') renderProjects();
    if (route === 'calendar') renderCalendar();
  }

  return Object.freeze({ bind, routeChanged, renderTasks, renderProjects, renderCalendar, openTaskForm, openProjectForm });
}
