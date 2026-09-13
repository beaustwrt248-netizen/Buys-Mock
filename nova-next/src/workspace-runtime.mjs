const FILTERS = new Set(['all', 'today', 'upcoming', 'done']);

export function createWorkspaceRuntime({
  store,
  today = () => new Date().toISOString().slice(0, 10)
} = {}) {
  if (!store || typeof store.snapshot !== 'function') throw new TypeError('WORKSPACE_STORE_REQUIRED');
  if (typeof today !== 'function') throw new TypeError('WORKSPACE_TODAY_REQUIRED');

  const current = () => store.snapshot();

  function listTasks(filter = 'all') {
    if (!FILTERS.has(filter)) throw new Error('TASK_FILTER_INVALID');
    const day = today();
    return current().tasks.filter(task => {
      if (filter === 'today') return !task.completed && task.dueDate === day;
      if (filter === 'upcoming') return !task.completed && Boolean(task.dueDate) && task.dueDate > day;
      if (filter === 'done') return task.completed;
      return true;
    });
  }

  function listProjects() {
    return current().projects;
  }

  function calendarItems() {
    const state = current();
    const day = today();
    return [
      ...state.tasks
        .filter(task => Boolean(task.dueDate) && task.dueDate >= day)
        .map(task => ({
          kind: 'task',
          id: task.id,
          label: task.title,
          date: task.dueDate,
          completed: task.completed
        })),
      ...state.projects
        .filter(project => Boolean(project.targetDate) && project.targetDate >= day)
        .map(project => ({
          kind: 'project',
          id: project.id,
          label: project.name,
          date: project.targetDate,
          completed: project.status === 'done'
        }))
    ].sort((a, b) => a.date.localeCompare(b.date) || a.label.localeCompare(b.label));
  }

  return Object.freeze({
    listTasks,
    listProjects,
    calendarItems,
    createTask: input => store.createTask(input),
    updateTask: (id, patch) => store.updateTask(id, patch),
    toggleTask: id => store.toggleTask(id),
    deleteTask: id => store.deleteTask(id),
    createProject: input => store.createProject(input),
    updateProject: (id, patch) => store.updateProject(id, patch),
    deleteProject: id => store.deleteProject(id),
    consumeRecoveryNotice: () => store.consumeRecoveryNotice()
  });
}
