---
name: ssutime-desktop-store-release
description: SSUTime Windows Desktop Microsoft Store release workflow. Use when the user explicitly asks to release, deploy, publish, or upload the Desktop or Windows app. Automatically bumps the committed Desktop patch version, commits Desktop changes on the desktop branch, creates a desktop-v release, and submits the MSIX for Microsoft Store certification.
---

# SSUTime Desktop Store Release

Follow this workflow only for the Windows `desktopApp`.

## Guardrails

- Apply `desktopApp/AGENTS.md`.
- Work only on the `desktop` branch. Treat it as the main development and release branch for Desktop.
- Do not modify, stage, build, version, tag, or release the Android `app` to make a Desktop release.
- Use `desktop-v<MAJOR.MINOR.PATCH>` tags. Never use `android-v` or a bare `v` tag.
- Build the MSIX from the immutable release tag commit and verify that commit belongs to `desktop`.
- Do not create or publish an MSI/EXE installer.
- Do not attach the Store MSIX to a public GitHub Release or website.
- Do not purchase, request, or configure a production code signing certificate for Store-only MSIX distribution.
- Never commit a local test certificate, private key, Partner Center client secret, or downloaded Store credential.
- Do not claim Store certification is complete until Partner Center reports that result.

## Store Identity

The manifest must keep these Partner Center values:

- Package Identity Name: `Campo.1711AB9C2595`
- Package Identity Publisher: `CN=BC44C2C8-25C2-4313-917E-619FF08BC787`
- Publisher display name: `Campo`
- Store ID: `9N8DJHBJDRGR`

If Partner Center displays different values later, stop and ask before changing the committed manifest.

## Version

1. Read the current Desktop version from `desktopApp/version.txt` independently of the Android version.
2. Unless the user specifies a version, increment the patch component by exactly 1. If specified, require a version greater than the current Desktop version.
3. Write the selected `MAJOR.MINOR.PATCH` value back to `desktopApp/version.txt` before the release commit.
4. Use the same value for the `desktop-v<version>` tag; the workflow converts it to the four-part MSIX version `<version>.0`.
5. Confirm the selected tag does not already exist locally, remotely, or as a GitHub Release.

## Preflight

1. Inspect tracked changes and exclude suspicious or unrelated changes.
2. Confirm the current branch is `desktop`. If not, stop before modifying or committing files.
3. Confirm `.github/workflows/desktop-store-package.yml` exists.
4. Confirm the Store Identity in `desktopApp/src/main/msix/AppxManifest.xml`.
5. Run:

```bash
./gradlew :desktopApp:test :desktopApp:createDistributable -PdesktopVersion=<version>
```

The local `createDistributable` result is only a compile/package-image check when not running on Windows. The final MSIX must be created and verified by the Windows workflow.

## Commit And Push

1. Review `git status --short` and ensure Android paths are not part of the Desktop release changes.
2. Stage only Desktop-owned paths, including `desktopApp/**` and `.github/workflows/desktop-store-package.yml`. Never use `git add .` or `git add -u` for a Desktop release.
3. Include intended new Desktop files, the `desktopApp/version.txt` bump, and all uncommitted Desktop changes approved for the release.
4. Commit with a concise Korean Desktop release message and push the `desktop` branch without rewriting history.
5. Confirm the pushed commit is the intended release commit. Do not create an Android commit, tag, or Release.

## Release Notes

- Find the latest stable `desktop-v` release only. Never use an Android release as the Desktop comparison baseline.
- Describe user-visible Windows Desktop changes and Store-relevant fixes.
- Exclude Android-only changes and internal build details.
- Write separate `## English` and `## 한국어` sections to `desktopApp/release-notes/desktop-v<version>.md` and include that file in the release commit.

## Create Release

Create and push a tag that points to the intended `desktop` commit:

```bash
git tag desktop-v1.1.14 <desktop-release-commit>
git push origin desktop-v1.1.14
```

The tag triggers `Windows Desktop Store Release`. The workflow creates the GitHub Release from the committed bilingual notes, keeps the MSIX as a private workflow artifact, uploads it to Microsoft Store, and commits the submission to request certification. It never attaches the package to the public GitHub Release.

## Automatic Store Submission

The published free Store product uses Microsoft Store Developer CLI automation. Store these values in the protected `microsoft-store` GitHub Environment, never in the repository:

- `AZURE_AD_APPLICATION_CLIENT_ID`
- `AZURE_AD_APPLICATION_SECRET`
- `AZURE_AD_TENANT_ID`
- `SELLER_ID`

The Entra application must be connected to Partner Center with the Manager role. The workflow runs `msstore publish` with Store ID `9N8DJHBJDRGR`, which uploads the MSIX and requests certification. Report the resulting submission status, but do not claim approval until Partner Center reports certification success.
