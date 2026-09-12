# Nova Next Design Specification

## Purpose

Nova Next is a brand-new standalone successor candidate for Nova AI. It is built from scratch and must remain operationally isolated from the existing `nova/` application until an explicit future promotion. The current Nova is a functional reference only; Nova Next must not import or execute its frontend/runtime source files.

## Reference visual contract

The approved source of truth is `Nova AI Mobile App UI Mockup.png`.

The product uses a premium dark mobile aesthetic:

- deep navy-to-black backgrounds;
- white primary text with muted blue-gray secondary copy;
- electric blue and violet gradient accents;
- rounded, glass-like cards with subtle blue borders and glow;
- clean sans-serif typography;
- compact, minimalist line icons;
- soft depth rather than heavy shadows;
- fast, restrained motion; and
- safe-area aware, one-handed mobile layouts.

## Required reference screens

### Splash

Centered Nova mark, `NOVA AI`, tagline `Smarter Tools / Brighter Results`, progress bar and `Loading your workspace...` over the blue wave treatment.

### Login

Nova mark, `Welcome Back`, `Sign in to continue`, Email/Username, Password, Remember me, Forgot password, Sign In, Google/Apple continuation controls and signup copy. The production implementation must preserve Nova's actual protected authentication contract rather than treating these visual controls as authorization.

### Home

Header: hamburger, `NOVA AI`, `Your AI-Powered Assistant`, avatar. Large `Ask Nova anything...` input with microphone. Quick actions: `Chat`, `Scan`, `Create`, `Research`. Greeting: `Good morning, Beau` and `Let's make today productive.` Quick Access: `My Tasks`, `Projects`, `Knowledge`, `Tools`.

### Side drawer

Brand header `NOVA AI` / `Smarter Tools. Brighter Results.` Drawer destinations, in order: Home, Chat, Tools, Tasks, Projects, Knowledge Base, Files, Automation, Calendar, Integrations, Settings, Help & Support. Profile footer and logout affordance.

### Chat

`Nova` with online state and glowing orb. Welcome card and suggestion actions: Summarise information, Help me write something, Analyse an image, Find the best price, Plan or organise a task, Other. Bottom composer remains visible.

### Tools

Title and `Powerful AI tools at your fingertips`. Filters: All, Productivity, Content, Analysis. Reference list: AI Chat, Image Analysis, Document Assistant, Price & Product Search, Translate, Code Assistant, Idea Generator, Data Analysis. Real Nova-specific tools may extend this list through the capability registry without changing the visual grammar.

### Tasks

Title `My Tasks`, tabs All / Today / Upcoming / Done, task rows with priority chips and `+ Add Task` primary action.

### Projects

Title `Projects`, `Manage and build your ideas`, `+ New Project`, progress cards.

### Settings

Title `Settings`, `Customise your Nova experience`, rows for Account, Appearance, Notifications, Privacy & Data, Integrations, Automation, About.

### Completion state

Glowing checkmark, `All Set!`, `Smooth. Simple. Powerful.`, `Nova AI is ready when you are.`, and `Let's Go`.

## Navigation contract

Primary bottom navigation is always: Home, Chat, Tools, Tasks, More. The selected item uses the electric-blue active treatment. Secondary and advanced features are grouped into the side drawer and More destination rather than crowding the bottom bar.

## Motion contract

Page changes use subtle slide/fade transitions. Drawer opens laterally over a dim/blurred surface. Interactions feel continuous and must not force full-page reloads. Honor `prefers-reduced-motion` by removing nonessential transitions.

## Functional parity contract

Nova Next must eventually provide the existing Nova capability surface: conversation, live web research, camera/image intelligence, catalogue intelligence, pricing intelligence, support intelligence, Guardian analysis, release readiness, bug triage, business intelligence, memory/learning, voice, scenario planning, multi-step jobs, proactive alerts and evidence/explain modes. Capability parity means the real adapter, permissions, evidence, failure handling and tests exist; a visual placeholder alone does not count.

## Security and promotion boundaries

Nova Next may use the same approved server-side contracts as Nova but cannot weaken them. Guardian remains independently authoritative. Protected operations stay human-gated. Development web/PWA/Android identity remains distinct so both apps can coexist. Promotion to main Nova is a deliberate release event with parity, security, package/signing, routing, rollback and human-approval checks.
