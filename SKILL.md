---
name: ssutime-deploy-release
description: SSUTime Android app release workflow. Use when the user asks in Korean or English to deploy, release, publish, upload to Play Store, or says "배포해줘", "릴리즈 올려줘", "플레이스토어 배포해줘". Guides an agent to verify the app compiles, commit and push the current repository changes, generate user-facing release notes from commits since the previous GitHub Release, create a GitHub Release, and let GitHub Actions publish to Google Play production.
---

# SSUTime Deploy Release

Follow this workflow when the user asks to deploy SSUTime.

## Guardrails

- Work from the repository root.
- Do not commit secrets or local-only files. In particular, never add `local.properties`, `app/google-services.json`, `app/service-account.json`, keystores, build outputs, or files already ignored by `.gitignore`.
- Commit only already-tracked changes. Ignore unversioned files by default, even if they appear in `git status --short` as `??`.
- Inspect `git status --short` before committing. If tracked changes look suspicious, ask before including them.
- Do not create a GitHub Release unless compile/test checks pass and `git push` succeeds.
- Do not claim the Play Store review is complete. The GitHub Action submits the production release for review with `status: completed`; Google may still approve, reject, or hold it.

## Preflight

1. Confirm the Play release workflow exists:
   - `.github/workflows/google-play-production.yml`
   - It should trigger on GitHub Release `published`.
2. Confirm required GitHub Repository Secrets are expected:
   - `ANDROID_GOOGLE_SERVICE_JSON`
   - `ANDROID_FIREBASE_GOOGLE_SERVICES_JSON`
   - `ANDROID_UPLOAD_KEYSTORE_BASE64`
   - `ANDROID_UPLOAD_KEYSTORE_PASSWORD`
   - `ANDROID_UPLOAD_KEY_ALIAS`
   - `ANDROID_UPLOAD_KEY_PASSWORD` only if the key password differs from the keystore password.
   - `POSTHOG_API_KEY`
3. Bump the committed app version before the final release commit:
   - In `app/build.gradle.kts`, increase the hardcoded `versionCode` value by exactly 1.
   - Increase the hardcoded `versionName` by one patch step unless the user specified a version. Example: `1.0.7` -> `1.0.8`.
   - Use the bumped `versionName` as the GitHub Release tag with a `v` prefix, for example `v1.0.7`.
   - Do not rely on CI to derive app versions from the GitHub Release tag; the Play build uses the committed Gradle version.
4. Run checks:

```bash
./gradlew test :app:compileReleaseKotlin :app:bundleRelease :app:assembleRelease
```

If any check fails, fix the issue or report the failure. Stop before commit/release.

## Commit And Push

1. Review changes:

```bash
git status --short
git diff --stat
```

2. Add tracked changes only. Do not add `??` unversioned files unless the user explicitly asks. A safe default is:

```bash
git add -u
git add .github/workflows/google-play-production.yml SKILL.md
```

Only add new tracked-intended files that are part of release automation or source changes. Never use `git add .` when unversioned local files are present.
3. Commit with a concise Korean message. Example:

```bash
git commit -m "릴리즈 배포 준비"
```

4. Push the current branch:

```bash
git push
```

If push fails because the remote has moved, pull/rebase only after inspecting the situation. Do not overwrite remote history.

## Release Notes

Generate release notes after pushing.

1. Find the latest GitHub Release:

```bash
gh release list --limit 10
```

2. Get the latest release tag and commits since then:

```bash
gh release view <latest-tag> --json tagName,publishedAt,name
git log <latest-tag>..HEAD --no-merges --pretty=format:'- %s'
```

If there is no previous GitHub Release, use the project history that is reasonable for the first release.

3. Write release notes in Korean for general users, not developers.

Include:
- User-visible feature additions
- UI/design changes
- Bug fixes users can understand
- Notification, widget, login, assignment, AI summary, or release behavior changes when user-visible

Exclude:
- Internal architecture details
- Refactors
- Dependency bumps
- CI/build changes unless they affect users
- Implementation terms like API repository, DTO, polling, Gradle, ProGuard, R8
- Commit hashes and PR numbers

The release body must be Korean because it is used as the Play Store `ko-KR` patch note. Keep it concise because the Play Store release notes are generated from the GitHub Release body and truncated to 500 characters by CI. Use Korean bullets. Example:

```markdown
- AI 요약 카드에서 예상 소요시간을 확인할 수 있어요.
- 위젯의 업데이트 정보와 새로고침 버튼 배치를 개선했어요.
- 과제 완료 상태가 더 안정적으로 반영되도록 수정했어요.
```

## Create The GitHub Release

Use the bumped app version from `app/build.gradle.kts` unless the user specified a release tag.

1. Read `versionName` from `app/build.gradle.kts` after the version bump.
2. Use a `v` prefix, for example `v1.0.7`.
3. Create the release with the generated notes:

```bash
gh release create v1.0.7 --target <current-branch-or-sha> --title "v1.0.7" --notes-file /tmp/ssutime-release-notes.md
```

Creating the GitHub Release starts the `Google Play Production Release` GitHub Action.

## After Release Creation

Watch or inspect the workflow:

```bash
gh run list --workflow "Google Play Production Release" --limit 5
gh run view <run-id> --log
```

Report:
- Commit hash pushed
- Release tag and URL
- Whether the GitHub Action started and current status
- Whether the Play upload step succeeded, failed, or is still running

If the workflow fails, inspect logs, fix the cause, commit/push, and create a new release or rerun only if the failure is transient.
