# CI/CD Pipeline — Full Guide

This document explains how the Money Manager AI bot gets from your laptop to the live server automatically, written so anyone can follow along — no prior knowledge of SSH, Docker, or GitHub Actions required.

---

## Table of Contents

1. [What is CI/CD and why we have it](#1-what-is-cicd-and-why-we-have-it)
2. [Bird's-eye view — the full journey](#2-birds-eye-view--the-full-journey)
3. [The cast of characters (who is who)](#3-the-cast-of-characters-who-is-who)
4. [What we built — infrastructure that existed before a push](#4-what-we-built--infrastructure-that-existed-before-a-push)
5. [Step-by-step: what happens on every `git push`](#5-step-by-step-what-happens-on-every-git-push)
6. [The two workflow files, explained line by line](#6-the-two-workflow-files-explained-line-by-line)
7. [Secrets — what is stored where and why](#7-secrets--what-is-stored-where-and-why)
8. [How to verify a deploy worked](#8-how-to-verify-a-deploy-worked)
9. [How to roll back](#9-how-to-roll-back)
10. [When things go wrong — common failures and fixes](#10-when-things-go-wrong--common-failures-and-fixes)
11. [Security notes](#11-security-notes)
12. [Future recommendations](#12-future-recommendations)
13. [Glossary](#13-glossary)

---

## 1. What is CI/CD and why we have it

Imagine you change one word in the bot's greeting. Without automation, getting that change live on the server means: compile the code, build a container image, copy it to the server, stop the old container, start the new one. That is 4–5 manual steps. Miss one, the bot stays broken.

**CI/CD** is the robot that does those steps for you.

- **CI** = Continuous Integration. When you push code, the robot builds it and tests it. If it is broken, you find out right away instead of two weeks later.
- **CD** = Continuous Deployment. Once the build is healthy, the robot also puts the new version on the live server by itself.

Our pipeline turns one command — `git push` — into a working, updated bot on the server a few minutes later.

---

## 2. Bird's-eye view — the full journey

```
     ┌──────────────┐
     │  Your laptop │
     │  git push    │
     └──────┬───────┘
            │
            ▼
  ┌─────────────────────┐
  │ GitHub: money-       │   Workflow file: .github/workflows/build.yml
  │ manager-ai repo      │   Job: build-push
  │                      │
  │ 1. Build Docker image│
  │ 2. Push to GHCR      │───► ghcr.io/amirkenesbay/money-manager-ai:latest
  │ 3. Ping infra repo   │           (public container registry)
  └──────┬──────────────┘
         │ HTTP POST (repository_dispatch)
         ▼
  ┌─────────────────────┐
  │ GitHub: money-       │   Workflow file: .github/workflows/deploy.yml
  │ manager-infra repo   │   Job: deploy
  │                      │
  │ 1. Load SSH key      │
  │ 2. SSH into VPS      │
  │ 3. git pull          │
  │ 4. docker compose    │
  │    pull + up -d      │
  └──────┬──────────────┘
         │ SSH
         ▼
  ┌─────────────────────┐
  │   VPS (rented        │   Server: vds31199.vpsza500.kz
  │   Ubuntu server)     │   IP:     93.170.72.240
  │                      │   User:   deploy
  │ /opt/money-manager:  │
  │   docker-compose.yml │
  │   volumes/…          │
  │                      │
  │ Containers running:  │
  │   - app              │◄── pulls new image here
  │   - mongo            │    (mongo never restarts on deploy)
  └─────────────────────┘
```

Two GitHub repos, two workflow files, one server. That is the whole system.

---

## 3. The cast of characters (who is who)

| Name | What it is | Where it lives |
|---|---|---|
| **money-manager-ai** | The repo with the bot's Kotlin source code | github.com/amirkenesbay/money-manager-ai |
| **money-manager-infra** | The repo that knows how to run the bot (`docker-compose.yml` + configs) | github.com/amirkenesbay/money-manager-infra |
| **GHCR** | GitHub Container Registry — an online warehouse that stores built Docker images | ghcr.io/amirkenesbay/money-manager-ai |
| **Docker image** | A frozen snapshot of "the bot + everything it needs to run" in one file | Lives in GHCR, runs on the VPS |
| **VPS** | A rented Linux server, always on, with a public IP | 93.170.72.240 (Ubuntu 24.04) |
| **`deploy` user** | A Linux account on the VPS with just enough rights to run Docker | /home/deploy on the VPS |
| **GitHub Actions** | GitHub's built-in CI/CD robots — free VMs that run your workflows | github.com (runs on GitHub-hosted runners) |
| **Workflow file** | A YAML file that tells GitHub Actions "when X happens, run these steps" | `.github/workflows/*.yml` in each repo |

---

## 4. What we built — infrastructure that existed before a push

Before any of the automation could run, we set up several one-time things. This is the groundwork; it does not repeat per deploy.

### 4.1 The VPS

A Linux server was rented (Ubuntu 24.04). On it we:

- Created a dedicated Linux user called `deploy` (so nothing runs as root).
- Turned off password-based SSH login — you can only get in with an SSH key.
- Turned on `ufw` (firewall) allowing ports 22 (SSH), 80, 443, and 27017 (MongoDB).
- Installed `fail2ban` (bans IPs that try brute-forcing SSH).
- Installed Docker and Docker Compose.
- Created the directory `/opt/money-manager` owned by `deploy`.

### 4.2 The infra repo on the VPS

We cloned `money-manager-infra` into `/opt/money-manager`. That directory now contains:

- `docker-compose.yml` — the recipe that says "run these containers with these settings"
- `.env` — real secrets (bot token, Gemini API key, DB credentials), **not** committed to Git
- `volumes/mongo/mongo-init.sh` — script that creates the MongoDB user on first boot

The `.env` file lives only on the server. If you ever rebuild the server, you must re-create it there from a safe backup.

### 4.3 The Docker image factory

We created a `Dockerfile` in the bot's repo. It is a two-stage recipe:

1. Start from an image with Java 21 + build tools. Copy source. Run `./gradlew bootJar` to produce a `.jar`.
2. Throw away the build stage. Start from a smaller image with only Java runtime. Copy the `.jar` over. Set the entrypoint to run it.

The final image is what runs on the server. It has the bot and nothing else — no Gradle, no source code, no build tools. Smaller and safer.

### 4.4 The SSH key for the robot

We generated a brand-new SSH key pair just for the CI robot — `~/.ssh/money-manager-deploy-ci` (private) and `.pub` (public).

- Public part was appended to `/home/deploy/.ssh/authorized_keys` on the VPS — meaning "whoever holds the private half can log in as `deploy`."
- Private part was copied into GitHub Actions as a secret called `SSH_PRIVATE_KEY` in the infra repo.

Why a separate key and not your personal one? Principle of **least privilege**. If this key ever leaks, you delete one line from `authorized_keys` and the robot is locked out. Your personal keys keep working. Every automation gets its own key.

### 4.5 The GHCR package made public

The compiled Docker image is stored in GHCR (ghcr.io). We marked it **public** so the VPS can pull it without authenticating. There are no secrets inside the image (the `.env` is on the server, never baked in), so making it public is safe and eliminates a whole category of "why did it fail to log in?" problems.

### 4.6 The PAT (Personal Access Token)

The build workflow needs to "ring the doorbell" at the infra repo to trigger deployment. That API call needs authentication. We created a fine-grained PAT scoped to **only** the infra repo, with **only** `Contents: Read and Write` + `Metadata: Read`. That token lives in the bot repo as secret `INFRA_DISPATCH_TOKEN`.

---

## 5. Step-by-step: what happens on every `git push`

You type:

```bash
git push
```

The next 2–4 minutes look like this:

### Phase 1 — Build (in the bot repo, ~90 seconds)

1. GitHub notices a push on `master`. It reads `.github/workflows/build.yml`.
2. GitHub spins up a fresh virtual machine (a "runner"), installs Docker on it.
3. The runner downloads the bot's source code.
4. The runner logs into GHCR using a temporary, auto-generated `GITHUB_TOKEN` (it gets one automatically, you don't manage it).
5. The runner builds the Docker image using your `Dockerfile`. Build cache is reused where possible so successive builds are faster.
6. The runner pushes the image to GHCR with two tags:
   - `latest` — always points to the newest version
   - `sha-abc1234` — points to this exact commit (for rollback)
7. The last step: `curl` fires a POST to GitHub's API:
   ```
   POST https://api.github.com/repos/amirkenesbay/money-manager-infra/dispatches
   body: {"event_type":"deploy","client_payload":{"sha":"abc1234..."}}
   ```
   That is "the doorbell ringing" at the infra repo. Auth header uses `INFRA_DISPATCH_TOKEN`.

If any step fails, the run turns red in the Actions tab and the infra repo is never notified. Your server stays on the previous version.

### Phase 2 — Deploy (in the infra repo, ~30 seconds)

1. The infra repo's workflow file `deploy.yml` is listening for the event `repository_dispatch` of type `deploy`. The doorbell from Phase 1 step 7 triggers it.
2. GitHub spins up a second fresh runner.
3. The runner pulls the infra repo code.
4. The runner starts an SSH agent and loads `SSH_PRIVATE_KEY` into it.
5. The runner writes `SSH_KNOWN_HOSTS` to a file so the SSH client trusts the VPS without prompting.
6. The runner SSHes into the VPS as `deploy@93.170.72.240` and runs a block of commands:
   ```bash
   set -euo pipefail              # strict mode: stop on any error
   cd /opt/money-manager          # go to the infra clone on the VPS
   git pull --ff-only             # get the latest docker-compose.yml
   docker compose pull app        # download the new image from GHCR
   docker compose up -d app       # recreate the app container with the new image
   docker compose ps              # print status so the log shows both containers
   ```
7. When the SSH session exits cleanly, the runner exits cleanly, the workflow shows green in the Actions tab.

### Result

A new container is running on the VPS with the updated bot. The MongoDB container is never touched — only `app` is recreated. Users might see the bot be unresponsive for ~5–10 seconds while it restarts; that is the entire downtime.

---

## 6. The two workflow files, explained line by line

### 6.1 `build.yml` (in the bot repo)

```yaml
name: Build and push Docker image
```
The name you see in the Actions tab.

```yaml
on:
  push:
    branches: [master]
  workflow_dispatch:
```
Two triggers: automatic on every push to `master`, plus a manual "Run workflow" button in the UI.

```yaml
jobs:
  build-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
```
One job called `build-push`, runs on a GitHub-provided Ubuntu VM. `permissions` grants the auto-generated token the ability to read the repo code and write to GHCR (needed to push the image).

```yaml
    steps:
      - uses: actions/checkout@v4
```
Downloads the repo source to the runner.

```yaml
      - uses: docker/setup-buildx-action@v3
```
Installs Docker Buildx — a more powerful builder that supports cache and multi-stage builds nicely.

```yaml
      - name: Log in to GHCR
        uses: docker/login-action@v3
        with:
          registry: ghcr.io
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
```
Logs Docker into GHCR. `GITHUB_TOKEN` is auto-generated for each run; no setup needed.

```yaml
      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ghcr.io/amirkenesbay/money-manager-ai
          tags: |
            type=raw,value=latest
            type=sha,prefix=sha-,format=short
```
Decides what tags to apply: `latest` and `sha-<first 7 chars of commit>`.

```yaml
      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          secrets: |
            gpr_user=${{ github.actor }}
            gpr_key=${{ secrets.GITHUB_TOKEN }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
```
The actual build. Passes the GitHub token as a **BuildKit secret** — so it can be used during build (to fetch private dependencies from GitHub Packages) but is NOT baked into the final image layers. Cache uses GitHub Actions cache backend, making rebuilds fast.

```yaml
      - name: Trigger infra deploy
        run: |
          curl -fsSL -X POST \
            -H "Accept: application/vnd.github+json" \
            -H "Authorization: Bearer ${{ secrets.INFRA_DISPATCH_TOKEN }}" \
            -H "X-GitHub-Api-Version: 2022-11-28" \
            https://api.github.com/repos/amirkenesbay/money-manager-infra/dispatches \
            -d '{"event_type":"deploy","client_payload":{"sha":"${{ github.sha }}"}}'
```
The "doorbell ring." `curl -f` makes the step fail on HTTP 4xx/5xx, so if the token expires we find out immediately.

### 6.2 `deploy.yml` (in the infra repo)

```yaml
name: Deploy to VPS

on:
  repository_dispatch:
    types: [deploy]
  push:
    branches: [master]
    paths:
      - 'docker-compose.yml'
      - 'volumes/**'
      - '.github/workflows/deploy.yml'
  workflow_dispatch:
```
Three triggers:
- **`repository_dispatch`** — the doorbell from the bot repo.
- **`push` + `paths`** — if you edit the compose file or volumes directly in the infra repo, the deploy also runs. Editing README alone doesn't trigger it (because README isn't in the path list).
- **`workflow_dispatch`** — manual "Run workflow" button.

```yaml
jobs:
  deploy:
    name: Pull image and restart app on VPS
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
```
Download infra repo source to the runner.

```yaml
      - name: Setup SSH agent
        uses: webfactory/ssh-agent@v0.9.0
        with:
          ssh-private-key: ${{ secrets.SSH_PRIVATE_KEY }}
```
Loads the CI SSH private key into an SSH agent running on the runner. After this, any `ssh` command on the runner uses this key automatically.

```yaml
      - name: Add VPS to known_hosts
        run: |
          mkdir -p ~/.ssh
          echo "${{ secrets.SSH_KNOWN_HOSTS }}" >> ~/.ssh/known_hosts
          chmod 644 ~/.ssh/known_hosts
```
Tells SSH "I trust this server's fingerprint." Without this, SSH would ask an interactive question ("are you sure?") and the workflow would hang forever.

```yaml
      - name: Deploy on VPS
        run: |
          ssh ${{ secrets.SSH_USER }}@${{ secrets.SSH_HOST }} bash <<'EOF'
          set -euo pipefail
          cd /opt/money-manager
          git pull --ff-only
          docker compose pull app
          docker compose up -d app
          docker compose ps
          EOF
```
SSH into the VPS and run those commands. `<<'EOF' … EOF` is a bash "heredoc" — a multi-line string passed as stdin to the `bash` running on the remote server.

- `set -euo pipefail` → stop immediately on any error, treat unset variables as errors, propagate failures through pipes.
- `git pull --ff-only` → refuse to create merge commits; if local and remote diverged, stop. Safer than a plain `git pull`.
- `docker compose pull app` → only pull the `app` image. Mongo is not touched.
- `docker compose up -d app` → recreate only the app container with the new image. `-d` means detached (runs in the background).
- `docker compose ps` → print container status so the workflow log shows what's running.

---

## 7. Secrets — what is stored where and why

Secrets are encrypted values GitHub injects into workflow runs. They are never printed in logs.

### Bot repo (`amirkenesbay/money-manager-ai`)

| Secret name | Purpose |
|---|---|
| `INFRA_DISPATCH_TOKEN` | Fine-grained PAT scoped to the infra repo. Used by `build.yml` to ring the doorbell that triggers deploy. |

`GITHUB_TOKEN` is auto-generated per run — you don't create it.

### Infra repo (`amirkenesbay/money-manager-infra`)

| Secret name | Value | Purpose |
|---|---|---|
| `SSH_PRIVATE_KEY` | Contents of `~/.ssh/money-manager-deploy-ci` | Private half of the CI-only SSH key. Lets the runner log into the VPS. |
| `SSH_HOST` | `93.170.72.240` | The VPS IP. |
| `SSH_USER` | `deploy` | The Linux user the workflow connects as. |
| `SSH_KNOWN_HOSTS` | Output of `ssh-keyscan 93.170.72.240` | The VPS's public host fingerprints, so SSH trusts it non-interactively. |

### On the VPS (`/opt/money-manager/.env`)

Not a GitHub secret — a plain file on the server. Contains the bot's runtime secrets:
- `BOT_TOKEN` — Telegram bot token
- `GEMINI_API_KEY` — Google Gemini API key
- `MONGO_ROOT_USERNAME` / `MONGO_ROOT_PASSWORD` — Mongo admin
- `MONGO_USERNAME` / `MONGO_PASSWORD` — Mongo app user

**Back this file up somewhere safe.** If you lose it and the server dies, you have to regenerate all of those values.

---

## 8. How to verify a deploy worked

### Fastest — look at the Actions tabs

- Bot build: https://github.com/amirkenesbay/money-manager-ai/actions
- Infra deploy: https://github.com/amirkenesbay/money-manager-infra/actions

Both runs should be green.

### Proof the container restarted

```bash
ssh deploy@93.170.72.240 "docker inspect money-manager-ai --format '{{.Image}} started={{.State.StartedAt}}'"
```

This prints the SHA of the image currently running and when it started. Run it before and after a push — both should change.

### Proof the bot itself updated

Send `/start` to the bot in Telegram. Or do whatever interaction exercises your change.

### Tail logs on the server

```bash
ssh deploy@93.170.72.240 "cd /opt/money-manager && docker compose logs -f --tail=50 app"
```

Ctrl+C to stop tailing.

---

## 9. How to roll back

You pushed something broken. The bot is down. How to get the previous version back.

### Option A — redeploy a specific good image (fastest, ~15 seconds)

Every build tags the image with `sha-<commit>` in addition to `latest`. Find a known-good commit SHA from the bot repo's commit history, then:

```bash
ssh deploy@93.170.72.240
cd /opt/money-manager
# Temporarily override the image tag, for example sha-abc1234:
docker pull ghcr.io/amirkenesbay/money-manager-ai:sha-abc1234
docker tag  ghcr.io/amirkenesbay/money-manager-ai:sha-abc1234 \
            ghcr.io/amirkenesbay/money-manager-ai:latest
docker compose up -d app
exit
```

That tells Docker "pretend this old image is the new `latest`" and restarts the container with it. The next real deploy will overwrite `latest` again. Quick and dirty — use in an emergency.

### Option B — revert the commit in Git (clean, ~3 minutes)

```bash
# in money-manager-ai
git revert <bad-commit-sha>
git push
```

The revert is a new commit that undoes the bad one. The normal pipeline rebuilds and redeploys. Preferred because Git history stays honest — the revert is auditable.

### Option C — manual deploy of a specific tag via infra repo

Edit `docker-compose.yml` in the infra repo and pin the image:

```yaml
services:
  app:
    image: ghcr.io/amirkenesbay/money-manager-ai:sha-abc1234
```

Commit and push. Because `docker-compose.yml` is in the deploy-trigger `paths`, this automatically redeploys with the pinned version. To resume tracking `latest`, change it back and push again.

### How to identify which image is bad

```bash
ssh deploy@93.170.72.240 "docker inspect money-manager-ai --format '{{.Image}}'"
```

Shows the SHA of the currently-running image. Match it to a build in GitHub Actions by the short SHA in the tag.

---

## 10. When things go wrong — common failures and fixes

### "Permission denied (publickey)" in deploy step

The CI SSH key is not in `authorized_keys` on the VPS anymore, or `SSH_PRIVATE_KEY` secret does not match.

Fix: regenerate the key pair locally, put the new public half in `/home/deploy/.ssh/authorized_keys`, update the `SSH_PRIVATE_KEY` secret in the infra repo.

### "cannot open '.git/FETCH_HEAD': Permission denied"

Someone cloned `/opt/money-manager` as root. The `deploy` user cannot write to `.git/`.

Fix:
```bash
ssh deploy@93.170.72.240
sudo chown -R deploy:deploy /opt/money-manager
```

### "unauthorized" on `docker compose pull`

The GHCR package is private and the VPS is not logged in, or the login credentials expired.

Fix: easiest is to keep the package public (we did). If for some reason it becomes private, on the VPS:
```bash
echo "<GITHUB_PAT_with_read:packages>" | docker login ghcr.io -u amirkenesbay --password-stdin
```
as the `deploy` user.

### "dial tcp: lookup ghcr.io ... i/o timeout"

Transient DNS hiccup on the VPS. We saw this happen once. Usually resolves on the next run.

Fix: just re-trigger the workflow. If it persists, on the VPS run `resolvectl status` and check `/etc/resolv.conf` — the DNS resolver may need restarting (`sudo systemctl restart systemd-resolved`).

### Build succeeds, but deploy never runs

The doorbell didn't ring, or the infra repo didn't hear it.

- Check the last step of the build run in the Actions tab. If `curl` returned 401, the `INFRA_DISPATCH_TOKEN` PAT expired or was revoked.
- Check the infra repo's Actions tab. If nothing appears there at all, double-check the `repository_dispatch.types` in `deploy.yml` matches the `event_type` in the `curl` payload (`deploy` on both sides).

### Bot doesn't respond after deploy

Check logs:
```bash
ssh deploy@93.170.72.240 "cd /opt/money-manager && docker compose logs --tail=100 app"
```

Most common culprits:
- A missing environment variable (check `.env` on the server matches what the new code expects).
- Mongo not reachable (check `docker compose ps` — is `mongo` `healthy`?).
- A Telegram 409 Conflict — another process is polling with the same bot token. Stop all other instances, wait 30s, restart.

### "Terminated by other getUpdates request" (Telegram 409)

A leftover long-polling connection from a previous run is still alive on Telegram's side.

Fix:
```bash
ssh deploy@93.170.72.240 "cd /opt/money-manager && docker compose stop app"
# From your laptop, hit the API 3–5 times to flush the stale session:
curl "https://api.telegram.org/bot<TOKEN>/getUpdates"
curl "https://api.telegram.org/bot<TOKEN>/getUpdates"
curl "https://api.telegram.org/bot<TOKEN>/getUpdates"
ssh deploy@93.170.72.240 "cd /opt/money-manager && docker compose up -d app"
```

---

## 11. Security notes

- **The CI SSH key is passwordless.** It has to be — the robot can't type a password. Mitigation: its scope is limited to one user on one server, and if ever compromised it's one line to delete in `authorized_keys`.
- **The `.env` file on the VPS is the crown jewels.** Anyone with shell access to the VPS as `deploy` can read it. Keep a safe backup, but don't commit it anywhere.
- **GitHub secrets are encrypted at rest** and decrypted only inside workflow runs. They never appear in logs (even if your workflow tries to `echo` them, GitHub masks the value with `***`). Trust that, but don't deliberately leak them.
- **Never paste passwords or tokens into chat/email/Slack.** If you must share, use a password manager's secure share link. If a password does leak, rotate it (change it everywhere) immediately.
- **The PAT expires.** It's valid for 1 year from creation. Set a calendar reminder to rotate it before it dies, otherwise deploys will suddenly start failing at the `Trigger infra deploy` step.

---

## 12. Future recommendations

Things that work fine today but could be improved when the project grows.

### 12.1 Add automated tests to the build

Right now `build.yml` only compiles the image. Add a test step before the push:

```yaml
      - name: Run tests
        run: ./gradlew test
```

Put it between `checkout` and `Build and push`. A red test means no image gets pushed.

### 12.2 Automated rollback on failure

If a newly deployed container crash-loops, we currently need to notice manually. A small improvement to `deploy.yml`:

```bash
docker compose up -d app
sleep 15
if ! docker compose ps app | grep -q "Up"; then
  echo "New container failed, rolling back"
  docker tag ghcr.io/amirkenesbay/money-manager-ai:sha-<previous> \
             ghcr.io/amirkenesbay/money-manager-ai:latest
  docker compose up -d app
  exit 1
fi
```

Needs a way to remember the previous SHA — one idea: store it in a file on the VPS before pulling the new one.

### 12.3 Staging environment

Today, every push goes straight to production. For a hobby bot, fine. If it becomes critical: set up a second VPS, a `staging` branch, and have `deploy.yml` in a `staging-infra` repo deploy there. Promote to prod only after manual check.

### 12.4 Observability (logging + metrics)

Planned but not done. Add to `docker-compose.yml`:
- **Promtail** — collects container logs and ships them to Loki
- **Loki** — stores logs
- **Grafana** — dashboards for logs and metrics
- **node-exporter** — basic server metrics (CPU, RAM, disk)

Inspiration: `/media/LocalDisk/work/PET/projects/telegram-bots/infra/career-bot-infra` has this stack already wired up.

### 12.5 Healthcheck for the app container

Mongo has a healthcheck; the app does not. Add a simple one:

```yaml
  app:
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 5s
      retries: 3
```

(Requires exposing Spring Boot's actuator health endpoint.) This gives `docker compose ps` meaningful status instead of just "Up."

### 12.6 Renew to Node.js 24 actions

GitHub is deprecating Node 20 for actions on 2026-09-16. Once `actions/checkout@v5` or similar v5 tags are stable, bump versions in both workflows. The deprecation warnings in the Actions tab tell you when.

### 12.7 Secret rotation schedule

- Bot token (`BOT_TOKEN`): rotate via @BotFather if ever leaked, and at least once a year.
- Gemini API key: rotate via Google Cloud console yearly.
- `INFRA_DISPATCH_TOKEN` (PAT): has 1-year expiration, renew before it dies.
- CI SSH key: low priority since it's single-purpose, but rotating annually is a good habit.

---

## 13. Glossary

**Action** — A reusable piece of workflow logic, like `actions/checkout@v4`. Think of them as Lego blocks.

**Container** — A running instance of a Docker image. A single image can run many containers.

**`docker compose`** — A tool that reads `docker-compose.yml` and starts/stops several containers together. Makes multi-service apps easy to manage.

**`git pull --ff-only`** — Fetch and apply updates from the remote, but only if local history is a strict ancestor of remote (no merge). Safer for automation.

**Heredoc** — Bash syntax `<<EOF ... EOF` for passing a multi-line string as input to a command.

**Image** — A template that Docker uses to start containers. Versioned by tags (`latest`, `sha-abc1234`).

**Long-polling** — How Telegram's Bot API works by default: the bot asks Telegram "any updates?" in a loop. Only one client can long-poll a given token at a time — that's why you see 409 Conflict when two instances run.

**PAT (Personal Access Token)** — An authentication credential that acts on behalf of a GitHub user. Fine-grained PATs scope permissions tightly (specific repos + specific permissions), which is safer than classic PATs.

**`repository_dispatch`** — A GitHub event you can fire from one repo to trigger a workflow in another. The "doorbell" in our setup.

**Runner** — A fresh virtual machine that GitHub Actions provides per job. It's destroyed after the job finishes, so no state carries over between runs.

**`set -euo pipefail`** — Bash strict mode. Without it, a failing command mid-script is silently ignored — deadly in automation.

**SSH agent** — A small background process that holds your decrypted SSH keys in memory, so `ssh` can use them without asking for a passphrase each time.

**Tag** — A human-readable label on a Docker image. `latest` is a convention (not a promise of "newest"), `sha-abc1234` pins to a specific build.

**Workflow** — A YAML file in `.github/workflows/` that defines when and how a GitHub Actions job runs.

---

**Last updated:** 2026-04-18
**Pipeline verified working via end-to-end push test on 2026-04-17.**
