# Factory: opt-in agent harness

This folder holds an optional workflow for AI coding agents.
It traces every change from a product manifest to code.
You do not need it to contribute. CI does not run it.

## What is inside

| Path | Purpose |
|---|---|
| `product/manifest/` | Capabilities, stories (with acceptance criteria) and flows. |
| `product/registry.md` | A generated index of the manifest. Never edit it by hand. |
| `tasks/task.json` | The current task: feature, story id and acceptance criteria. |
| `check_*.py` | Manifest gates: G0, G7, G16, G31, G-NAME and G-REG. |
| `check_all.sh` | Runs the contributor gates from `governance/`, then the manifest gates. |
| `gate_retry_loop.sh` | Runs `check_all.sh` with a 3-attempt limit. |
| `session_resume.sh`, `session_checkpoint.sh` | Save and restore where a session stopped. |
| `worklog.sh`, `worklog.jsonl` | An append-only log of tasks and retrospectives. |
| `docs/conventions.md` | The manifest gates in detail. |
| `blueprint.md`, `cheat-sheet.md` | Background on the pattern and a short recipe. |
| `claude-settings.example.json` | Claude Code hooks that enforce the workflow. |

## Turn it on

1. Copy the `hooks` block from `factory/claude-settings.example.json` into `.claude/settings.local.json`.
   That file is for your machine only.
2. The `git commit` hook now runs `factory/check_all.sh` before every commit.
3. The prompt hook reminds the agent to follow the workflow below.

## Workflow for a feature

1. Run `bash factory/session_resume.sh`. It prints the last checkpoint, the current task and the last gate runs.
2. Write or update the manifest under `product/manifest/`. Add the capability, story and flow files.
3. Write `tasks/task.json` with `feature`, `storyId` and `acceptanceCriteria`.
4. Implement the feature under the layers in `CLAUDE.md`.
   Mark each acceptance criterion in code with a `// ac: <story-id>` comment.
5. Run `bash factory/gate_retry_loop.sh` until all gates pass.
6. Regenerate the registry with `python3 factory/check_registry.py --write`.
7. Append the session entries:
   `bash factory/worklog.sh task` and `bash factory/worklog.sh retro <attempts> <escalations> "<notes>"`.

## Pause in the middle of a feature

```bash
bash factory/session_checkpoint.sh "where I left off"
```

The checkpoint is saved in `factory/.session-checkpoint.json`. Git ignores it.

## Turn it off

Remove the `hooks` block from `.claude/settings.local.json`.
The contributor gates in `governance/` keep working without this folder.
