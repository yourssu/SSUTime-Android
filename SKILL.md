---
name: ssutime-deploy-release
description: SSUTime Android app release workflow. Use when the user asks in Korean or English to deploy, release, publish, upload to Play Store, or says "배포해줘", "릴리즈 올려줘", "플레이스토어 배포해줘" in the Android app context. Guides an agent to verify the app compiles, commit and push the current repository changes, generate user-facing release notes from commits since the previous Android GitHub Release, create an android-v GitHub Release, and let GitHub Actions publish to Google Play production. Do not use this workflow for Windows Desktop or Microsoft Store releases.
---

# SSUTime Deploy Release

Follow this workflow when the user asks to deploy SSUTime.

## Guardrails

- Work from the repository root and only from the `develop` branch.
- This workflow releases only the Android `app` to Google Play. If the user explicitly asks for a Windows, Desktop, MSIX, or Microsoft Store release, do not create an Android release and follow `desktopApp/SKILL.md` instead.
- Never modify, stage, bump, tag, or release `desktopApp/**`, `desktopApp/version.txt`, or `.github/workflows/desktop-store-package.yml` during an Android release.
- Keep release namespaces separate:
  - Android production: `android-v<version>`
  - Windows Desktop production: `desktop-v<version>`
- Never create a bare `v<version>` tag. It does not identify the target platform and can trigger or confuse the wrong release automation.
- Keep existing historical bare `v<version>` Android releases intact. Do not rename or delete published releases and tags merely to adopt the new namespace.
- Do not commit secrets or local-only files. In particular, never add `local.properties`, `app/google-services.json`, `app/service-account.json`, keystores, build outputs, or files already ignored by `.gitignore`.
- Commit only already-tracked changes. Ignore unversioned files by default, even if they appear in `git status --short` as `??`.
- Inspect `git status --short` before committing. If tracked changes look suspicious, ask before including them.
- Do not create a GitHub Release unless compile/test checks pass and `git push` succeeds.
- Do not claim the Play Store review is complete. The GitHub Action submits the production release for review with `status: completed`; Google may still approve, reject, or hold it.

## Preflight

1. Confirm the Play release workflow exists:
   - `.github/workflows/google-play-production.yml`
   - It should trigger on GitHub Release `published`.
   - Its publish job must require an `android-v` tag and reject prereleases.
   - Its checkout step must explicitly check out `github.event.release.tag_name`, so the uploaded Android bundle is built from the immutable release tag rather than a moving branch head.
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
   - Use the bumped `versionName` as the GitHub Release tag with an `android-v` prefix, for example `android-v1.0.7`.
   - Do not rely on CI to derive app versions from the GitHub Release tag; the Play build uses the committed Gradle version.
   - Do not read or change the independent Desktop version in `desktopApp/version.txt`.
4. Set the Play in-app update priority before the final release commit:
   - If the user explicitly says `인앱 업데이트 대상`, set `.github/workflows/google-play-production.yml` `inAppUpdatePriority` to `5`.
   - If the user explicitly says `인앱 업데이트 비대상`, set `inAppUpdatePriority` to `0`.
   - An explicit user selection overrides automatic classification. If the request contains both selections or is otherwise contradictory, stop and ask which one applies.
   - If the user does not specify either selection, inspect the release changes and commits to decide whether they include an LMS API version change. Treat changes to the `libs.lms` dependency version, LMS API client compatibility code, or other LMS API version migration work as an LMS API version change.
   - When automatically classified, set `inAppUpdatePriority` to `5` for an LMS API version change so the app's immediate in-app update flow can run for that version. Set it to `0` for all other releases.
   - Do not leave an automatically classified non-target release at `4` or `5`, because that would trigger the app's immediate update prompt.
   - Commit and push the workflow priority value together with the version bump and release changes. The GitHub Release workflow uses the committed workflow file, so this value must be correct before creating the GitHub Release.
   - Report whether the final release was handled as an in-app update target or non-target and the resulting priority value.
5. Run checks:

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

2. Add Android tracked changes only. Do not add `??` unversioned files unless the user explicitly asks. A safe default is:

```bash
git add -u -- . ':(exclude)desktopApp/**' ':(exclude).github/workflows/desktop-store-package.yml'
git add .github/workflows/google-play-production.yml SKILL.md
```

Only add new tracked-intended files that are part of release automation or source changes. Never use `git add .` when unversioned local files are present.
3. Commit with a concise Korean message. Example:

```bash
git commit -m "릴리즈 배포 준비"
```

4. Push the `develop` branch:

```bash
git push origin develop
```

If push fails because the remote has moved, pull/rebase only after inspecting the situation. Do not overwrite remote history.

## Release Notes

Generate release notes after pushing.

1. Find the latest stable Android GitHub Release. Do not use an unfiltered latest release because a `desktop-v` release may be newer:

```bash
gh release list \
  --limit 100 \
  --json tagName,publishedAt,isDraft,isPrerelease \
  --jq '.[] | select(.isDraft == false and .isPrerelease == false) | select(.tagName | startswith("android-v")) | [.publishedAt, .tagName] | @tsv'
```

2. From the filtered results, use the most recently published `android-v` tag and inspect commits since then:

```bash
gh release view <latest-android-tag> --json tagName,publishedAt,name
git log <latest-android-tag>..HEAD --no-merges --pretty=format:'- %s'
```

For the first namespaced Android release only, there may be no `android-v` release yet because historical Android releases used a bare `v<version>` tag. In that case, find the newest stable legacy tag and use it once as the Android release-note baseline:

```bash
gh release list \
  --limit 100 \
  --json tagName,publishedAt,isDraft,isPrerelease \
  --jq '.[] | select(.isDraft == false and .isPrerelease == false) | select(.tagName | test("^v[0-9]+\\.[0-9]+\\.[0-9]+$")) | [.publishedAt, .tagName] | @tsv'
```

After an `android-v` release exists, never fall back to a bare tag. If neither an `android-v` release nor a valid legacy Android release exists, use the project history that is reasonable for the first Android release. Exclude Desktop-only changes from Android user release notes even if those commits are in the inspected range.

3. Write release notes for general users, not developers, in every language supported by the app.

Supported release note languages currently include:
- English (`en`, default)
- Korean (`ko-KR`)

Use these exact section headings in the GitHub Release body:
- `## English`
- `## 한국어`

The Google Play production workflow parses those sections and creates separate Play Store patch note files:
- `distribution/whatsnew/whatsnew-en-US`
- `distribution/whatsnew/whatsnew-ko-KR`

If either section is missing or empty, the release workflow fails. Do not put multiple languages in the same section; English-speaking users should see only English notes, and Korean users should see only Korean notes.

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

The release body must include separate sections for each supported language so Play Store notes can be provided consistently for all localized store listings. Keep each language concise because each Play Store release note file is truncated to 500 characters by CI. Use user-facing bullets in each language. Example:

```markdown
## English

- You can now check estimated time in AI summary cards.
- Improved widget update information and refresh button placement.
- Fixed assignment completion status to update more reliably.

## 한국어

- AI 요약 카드에서 예상 소요시간을 확인할 수 있어요.
- 위젯의 업데이트 정보와 새로고침 버튼 배치를 개선했어요.
- 과제 완료 상태가 더 안정적으로 반영되도록 수정했어요.
```

## Create The GitHub Release

Use the bumped app version from `app/build.gradle.kts` unless the user specified a release tag.

1. Read `versionName` from `app/build.gradle.kts` after the version bump.
2. Form the tag as `android-v<version>`, for example `android-v1.0.7`.
3. Confirm the exact tag does not already exist locally, remotely, or as a GitHub Release.
4. Create the release with the generated notes:

```bash
gh release create android-v1.0.7 --target <current-branch-or-sha> --title "Android v1.0.7" --notes-file /tmp/ssutime-release-notes.md
```

Creating the `android-v` GitHub Release starts the `Google Play Production Release` GitHub Action. A `desktop-v` release must not start this workflow.

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
