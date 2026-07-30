---
name: ssutime-desktop-store-release
description: SSUTime Windows Desktop Microsoft Store MSIX release workflow. Use when the user explicitly asks to release, deploy, publish, or upload the Desktop or Windows app to Microsoft Store. Creates a desktop-v release from develop, builds an unsigned Store MSIX on Windows, and keeps the package out of public GitHub Release assets.
---

# SSUTime Desktop Store Release

Follow this workflow only for the Windows `desktopApp`.

## Guardrails

- Apply `desktopApp/AGENTS.md`.
- Do not modify the Android `app` to make a Desktop release.
- Use `desktop-v<MAJOR.MINOR.PATCH>` tags. Never use `android-v` or a bare `v` tag.
- Build the MSIX from the immutable release tag commit and verify that commit belongs to `develop`.
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

1. Choose the Desktop version independently from the Android version.
2. Use `MAJOR.MINOR.PATCH` for the Git tag, for example `desktop-v1.1.13`.
3. The MSIX manifest version must be the corresponding four-part value, for example `1.1.13.0`.
4. Confirm the tag does not already exist locally, remotely, or as a GitHub Release.

## Preflight

1. Inspect tracked changes and exclude suspicious or unrelated changes.
2. Confirm `.github/workflows/desktop-store-package.yml` exists.
3. Confirm the Store Identity in `desktopApp/src/main/msix/AppxManifest.xml`.
4. Run:

```bash
./gradlew :desktopApp:test :desktopApp:createDistributable -PdesktopVersion=<version>
./gradlew :app:assembleDebug
```

The local `createDistributable` result is only a compile/package-image check when not running on Windows. The final MSIX must be created and verified by the Windows workflow.

## Commit And Push

1. Commit the intended tracked changes and any new Desktop release files.
2. Push the current `develop` branch without rewriting history.
3. Confirm the pushed commit is the intended release commit.

## Release Notes

- Find the latest stable `desktop-v` release only. Never use an Android release as the Desktop comparison baseline.
- Describe user-visible Windows Desktop changes and Store-relevant fixes.
- Exclude Android-only changes and internal build details.
- Write separate `## English` and `## 한국어` sections.

## Create Release

Create a GitHub Release whose tag points to the intended `develop` commit:

```bash
gh release create desktop-v1.1.13 \
  --target <develop-release-commit> \
  --title "Windows v1.1.13" \
  --notes-file /tmp/ssutime-desktop-release-notes.md
```

The Release triggers `Windows Desktop Store Package`. The workflow uploads the MSIX only as a private workflow artifact and never as a public Release asset.

## First Store Submission

For the first release:

1. Wait for `Windows Desktop Store Package` to succeed.
2. Download the `SSUTime-<version>-Microsoft-Store` Actions artifact.
3. Upload the `.msix` file manually in Partner Center.
4. Complete the Store listing, privacy policy, age rating, screenshots, certification notes, and availability.
5. Report the Partner Center submission status without claiming approval prematurely.

## Later Automatic Updates

Only after the product is published and live, configure Microsoft Store Developer CLI automation. Store these values as protected GitHub Environment secrets, never in the repository:

- `AZURE_AD_APPLICATION_CLIENT_ID`
- `AZURE_AD_APPLICATION_SECRET`
- `AZURE_AD_TENANT_ID`
- `SELLER_ID`

The Entra application must be connected to Partner Center with the Manager role. Automatic update submission currently applies only to free Store products, so verify that condition before enabling it.
