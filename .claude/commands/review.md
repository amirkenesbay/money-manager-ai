Perform a thorough code review and post comments directly to the GitHub PR.

## Instructions

### Step 1 — Find the PR

If `$ARGUMENTS` contains a PR number, use it directly.
Otherwise:
1. Run `git branch --show-current` to get the current branch name.
2. Run `gh pr list` and find the PR for this branch.
3. If no PR exists, run `git diff HEAD` and output the review as text only (no GitHub posting).

### Step 2 — Get the diff

Run `gh pr diff <PR_NUMBER>` to get the full diff.
Also run `gh pr view <PR_NUMBER>` for PR context.

### Step 3 — Analyze the code

Review the diff across these dimensions:

#### Correctness
- Logic errors, off-by-one errors, null pointer risks
- Edge cases not handled
- Incorrect assumptions

#### Kotlin / Spring Boot Idioms
- Use of `val` vs `var` (prefer immutability)
- Null-safety (`?.`, `?:`, `!!` abuse)
- Data classes, extension functions, sealed classes used appropriately
- Idiomatic Kotlin (avoid Java-style code)
- Spring annotations used correctly

#### Architecture (project-specific)
- Business logic should NOT be in Dialog/Reply builders — delegate to Services
- Context fields properly populated in `action {}` blocks before state transitions
- `UserInfo.groupIds` and `MoneyGroup.memberIds` kept in sync
- Back button transitions specify explicit `from` states
- New states registered in: enum, reply config, dialog transitions

#### Security
- No hardcoded secrets or tokens
- Input validation at system boundaries
- No SQL/NoSQL injection risks

#### Performance
- Unnecessary DB calls in loops
- Missing indexes for frequent queries
- Blocking calls on coroutine threads

#### Code Quality
- Dead code or unused imports
- Overly complex methods (consider splitting)
- Magic numbers/strings (use constants)
- Misleading names

### Step 4 — Post comments to GitHub

For each issue found, post an **inline comment** on the relevant line using:

```bash
gh api repos/{owner}/{repo}/pulls/<PR_NUMBER>/comments \
  --method POST \
  --field body="<comment>" \
  --field commit_id="<latest commit sha from gh pr view>" \
  --field path="<file path>" \
  --field line=<line number> \
  --field side="RIGHT"
```

Get the repo owner/name from: `gh repo view --json nameWithOwner -q .nameWithOwner`
Get the commit SHA from: `gh pr view <PR_NUMBER> --json headRefOid -q .headRefOid`

**Comment format for each issue:**
```
🔴 Critical / 🟡 Warning / 🔵 Suggestion

<Short description of the issue>

**Fix:**
<Concrete suggestion or corrected code snippet>
```

### Step 5 — Post a summary review

After all inline comments, post an overall review summary using:

```bash
gh pr review <PR_NUMBER> --comment --body "<summary>"
```

Summary format:
```
## Code Review Summary

| Severity | Count |
|----------|-------|
| 🔴 Critical | N |
| 🟡 Warning  | N |
| 🔵 Suggestion | N |

### Key issues
- <bullet points of most important findings>

### Overall
<1-2 sentence assessment>
```

If no issues found, post a single approval comment.

$ARGUMENTS