# SSUTime Desktop

`desktopApp`은 Android `app`을 의존하지 않는 Compose Desktop JVM 애플리케이션이다. 화면은 Android 구현을 기준으로 이 모듈 안에서 별도로 구현하며, 할 일과 API 요청 모델은 기존 `data` 모듈을 함께 사용한다.

## 실행

Android Studio에서 Gradle 동기화 후 실행 구성의 **SSUTime Desktop**을 선택해 실행한다. 터미널에서는 다음 명령을 사용할 수 있다.

```shell
./gradlew :desktopApp:run
```

QA 중 실제 사용자 상태와 분리하려면 임시 데이터 경로를 지정할 수 있다.

```shell
SSUTIME_DATA_DIR=/tmp/ssutime-desktop-qa ./gradlew :desktopApp:run
```

## 제공 기능

- LMS 및 SSUTime 백엔드를 함께 사용하는 로그인과 자동 로그인
- LMS 할 일 새로고침, 로컬 캐시, 로딩 진행률, 오류 및 재시도
- 제출 완료 목록과 AI 과제 요약
- 프로필, 문의하기, 약관, 개인정보처리방침 및 로그아웃
- 창이 숨겨진 상태에서도 `SSUTime` 앱 이름으로 표시되는 Windows 마감 알림
- Windows DPAPI를 이용한 자동 로그인 암호 보호

## 트레이 동작

- 프로그램이 실행되면 알림 설정 여부와 관계없이 시스템 트레이에 상주한다.
- 실행 파일을 다시 실행해도 새 프로세스나 트레이 아이콘을 만들지 않고, 이미 실행 중인 창을 복원해 전면에 표시한다.
- 닫기 버튼과 `Alt+F4`는 프로그램을 종료하지 않고 창을 트레이로 숨긴다.
- 최소화 버튼은 Windows 기본 동작대로 창을 작업 표시줄에 최소화한다.
- 트레이 아이콘을 더블 클릭하거나 트레이 메뉴의 **SSUTime 열기**를 선택하면 창이 복원된다.
- 프로그램을 완전히 종료하려면 트레이 아이콘을 우클릭하고 **종료**를 선택한다.
- 마감 알림을 켜 두면 창이 트레이에 숨겨진 동안에도 앱이 실행되며 지정된 시간에 알림을 확인한다.
- Windows 알림을 클릭하면 숨겨지거나 최소화된 SSUTime 창을 복원해 전면에 표시한다.
- 알림을 끄거나 로그아웃해도 트레이 프로그램 자체는 종료되지 않는다.

## 플랫폼 예외

- Compose Desktop JVM에는 FCM 클라이언트가 공식 제공되지 않으므로 FCM 토큰 등록과 FCM 백그라운드 수신을 수행하지 않는다.
- Android 위젯과 전화 형태의 전체 화면 알림은 Windows에서 제공하지 않으며, 관련 UI 공간도 남기지 않는다.

## 검증

```shell
./gradlew :desktopApp:test
```

Compose가 제공하는 Windows 실행 이미지는 Windows 환경에서 다음 태스크로 생성한다.

```shell
./gradlew :desktopApp:createDistributable -PdesktopVersion=1.0.0
```

Microsoft Store 제출용 MSIX는 실행 이미지를 만든 뒤 Windows PowerShell에서 생성한다.

```powershell
./desktopApp/scripts/package-msix.ps1 -Version 1.0.0
```

생성 위치는 `desktopApp/build/msix/SSUTime-1.0.0-windows-x64.msix`다.

## Microsoft Store 릴리즈

- Desktop 개발과 배포는 `desktop` 브랜치를 기준으로 하며, `develop`의 Android 변경은 필요할 때 `desktop`으로 merge한다.
- Windows 배포는 Android 소스와 버전을 변경하지 않는다.
- Windows Desktop 프로덕션 릴리즈는 `desktop-v<version>` 태그를 사용한다.
- 현재 Desktop 버전은 `desktopApp/version.txt`에서 Android와 독립적으로 관리한다.
- `desktop-v1.0.0` 태그는 MSIX 패키지 버전 `1.0.0.0`으로 변환한다.
- Desktop MSIX는 `desktop` 이력의 `desktop-v` 태그 커밋에서 Windows runner로 생성한다.
- 태그와 같은 이름의 `desktopApp/release-notes/desktop-v<version>.md`로 영어·한국어 GitHub Release 노트를 자동 게시한다.
- Store Identity는 Partner Center가 발급한 `Campo.1711AB9C2595`와 `CN=BC44C2C8-25C2-4313-917E-619FF08BC787`을 사용한다.
- Store ID는 `9N8DJHBJDRGR`이다.
- Store 제출용 MSIX는 공개 GitHub Release 자산이나 웹사이트 다운로드 파일로 배포하지 않는다.
- GitHub Actions가 MSIX를 Microsoft Store에 업로드하고 인증 심사를 자동 요청한다.
- Microsoft Store가 심사를 통과한 MSIX의 배포 서명, 호스팅 및 업데이트를 담당한다.

상세한 배포 순서는 [RELEASE.md](RELEASE.md)를 따른다.
