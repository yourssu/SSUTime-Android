# Windows Desktop Microsoft Store 릴리즈

## 배포 요청 문구

- `안드로이드로 배포해줘. 인앱 업데이트 대상`: Android 앱을 우선순위 `5`로 Google Play에 배포한다.
- `안드로이드로 배포해줘. 인앱 업데이트 비대상`: Android 앱을 우선순위 `0`으로 Google Play에 배포한다.
- `윈도우 배포해줘`: Desktop 앱을 Microsoft Store용 MSIX로 배포한다.
- `Desktop <version> 버전을 Microsoft Store로 배포해줘`: 지정한 버전으로 Desktop 릴리즈를 준비한다.

Windows 배포 요청에는 `Windows`, `윈도우`, `Desktop`, `MSIX` 또는 `Microsoft Store` 중 하나를 반드시 포함한다. Desktop 배포 자동화는 `desktopApp/SKILL.md`를 따르며 Android 배포 스킬을 실행하지 않는다.

## 브랜치 운영

- Android의 기준 개발·배포 브랜치는 `develop`이다.
- Desktop의 기준 개발·배포 브랜치는 `desktop`이다.
- Android 변경을 반영할 때는 `desktop`에서 `develop`을 merge한 후 필요한 Desktop 구현을 진행한다.
- Windows 배포에서는 Desktop 변경만 커밋하며 Android 소스, Android 버전 및 Google Play workflow를 수정하거나 배포하지 않는다.
- Android 배포에서는 Desktop 버전과 Windows Store workflow를 변경하지 않는다.

## 배포 형식

- SSUTime Desktop의 공식 배포 경로는 Microsoft Store용 x64 MSIX다.
- MSI/EXE와 웹사이트 직접 다운로드는 공식 배포 산출물로 만들지 않는다.
- Store 제출용 MSIX에는 공개 신뢰 코드 서명 인증서가 필요하지 않다.
- Microsoft Store가 인증을 통과한 패키지를 서명하고 호스팅하며 사용자 업데이트를 제공한다.
- 로컬 설치 테스트용 자체 서명 인증서는 제품 배포 인증서가 아니며 저장소에 보관하지 않는다.

## Store Identity

Partner Center가 제공한 다음 값은 패키지 manifest에서 유지한다.

| 항목 | 값 |
| --- | --- |
| Package/Identity/Name | `Campo.1711AB9C2595` |
| Package/Identity/Publisher | `CN=BC44C2C8-25C2-4313-917E-619FF08BC787` |
| Package/Properties/PublisherDisplayName | `Campo` |
| Store ID | `9N8DJHBJDRGR` |

Store ID는 MSIX manifest 항목이 아니라 Partner Center 제출과 Store 제품 페이지 식별에 사용한다.

## 태그와 버전

- Android 프로덕션 릴리즈: `android-v<version>`
- Windows Desktop 프로덕션 릴리즈: `desktop-v<version>`
- Desktop의 현재 버전은 `desktopApp/version.txt`에서 관리한다.
- 버전이 지정되지 않은 Windows 배포 요청은 현재 Desktop 버전의 patch를 1 올리고 릴리스 커밋에 포함한다.
- Desktop 태그는 `MAJOR.MINOR.PATCH` 형식을 사용한다.
- MSIX Identity는 네 자리 버전을 요구하므로 마지막 구성요소 `0`을 붙인다.
- 예: `desktop-v1.1.13` → `1.1.13.0`
- 각 MSIX 버전 구성요소는 `0`에서 `65535` 범위여야 한다.
- Store에 제출하는 새 버전은 이전 제출보다 커야 한다.
- 릴리즈 태그가 가리키는 커밋에서 패키지를 빌드하고, 가변적인 브랜치 HEAD를 다시 빌드하지 않는다.

## 패키징 구조

1. Compose의 `:desktopApp:createDistributable`로 Windows 실행 이미지와 전용 JVM runtime을 만든다.
2. `desktopApp/scripts/package-msix.ps1`가 실행 이미지, Store manifest 및 아이콘을 staging 디렉터리에 구성한다.
3. Windows SDK의 `MakeAppx.exe`로 unsigned MSIX를 생성한다.
4. 생성한 MSIX를 다시 unpack해 Store Identity와 버전을 검증한다.
5. SHA-256 체크섬을 생성한다.

MSIX는 다음 Desktop 권한 모델을 사용한다.

- `uap10:RuntimeBehavior="packagedClassicApp"`
- `uap10:TrustLevel="mediumIL"`
- `rescap:Capability Name="runFullTrust"`

이 모델은 Compose Desktop의 Win32/JVM 실행, 시스템 트레이, 단일 인스턴스 및 Windows 알림 동작을 유지하기 위해 필요하다. 설치 디렉터리는 수정하지 않고 사용자 상태는 `%APPDATA%\SSUTime`에 저장한다.

## Windows CI

`.github/workflows/desktop-store-package.yml`은 다음 경우 실행한다.

- `desktop` 이력의 `desktop-v<version>` 태그가 push된 경우
- Actions에서 `desktop` 브랜치를 선택해 수동 복구 실행한 경우

Release 실행에서는 태그 버전이 `desktopApp/version.txt`와 일치하고 태그 커밋이 `desktop` 이력에 속하는지 확인한다. 생성한 `.msix`와 `.sha256`은 30일 동안 GitHub Actions artifact로 보관하며 공개 GitHub Release에는 첨부하지 않는다.

## Microsoft Store 자동 제출

workflow는 MSIX 생성 후 Microsoft 공식 Store Developer CLI를 설정하고 Store ID `9N8DJHBJDRGR`에 패키지를 업로드한다. `msstore publish`가 제출을 확정해 Microsoft 인증 심사를 요청하며, workflow 마지막에 현재 제출 상태를 조회한다.

Partner Center와 연결한 Microsoft Entra 애플리케이션에 Manager 역할을 부여하고 다음 값을 GitHub의 `microsoft-store` 보호 Environment secret으로 등록한다.

- `AZURE_AD_APPLICATION_CLIENT_ID`
- `AZURE_AD_APPLICATION_SECRET`
- `AZURE_AD_TENANT_ID`
- `SELLER_ID`

비밀 값은 문서, workflow, 채팅 또는 GitHub Release 본문에 기록하지 않는다. 누락되면 workflow는 Store 업로드 전에 실패한다. 자동 업데이트 제출은 게시된 무료 제품에서 지원되며, Partner Center가 인증 통과를 표시하기 전에는 배포 완료로 보고하지 않는다.

Store 심사용 계정이 필요한 앱이므로 Partner Center 인증 참고사항에는 정상적인 테스트 로그인 방법을 별도로 유지한다. 실제 개인 계정의 비밀번호를 저장소나 Release 본문에 적지 않는다.

## 앱 내 업데이트 동작

- Store 설치 앱의 업데이트는 Microsoft Store가 담당한다.
- 앱 자체에서 GitHub Release API로 MSI/MSIX를 내려받거나 설치하지 않는다.
- 업데이트 확인 UI가 필요하면 Store 제품 페이지 `9N8DJHBJDRGR`을 여는 방식으로 구현한다.
- Store가 제공하지 않는 임의의 구버전 다운로드 기능은 제품 기능으로 제공하지 않는다.
