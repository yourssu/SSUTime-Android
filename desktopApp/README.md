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
- 앱이 실행 중일 때 Windows 시스템 트레이를 통한 마감 알림
- Windows DPAPI를 이용한 자동 로그인 암호 보호

## 플랫폼 예외

- Compose Desktop JVM에는 FCM 클라이언트가 공식 제공되지 않으므로 FCM 토큰 등록과 FCM 백그라운드 수신을 수행하지 않는다.
- Android 위젯과 전화 형태의 전체 화면 알림은 Windows에서 제공하지 않으며, 관련 UI 공간도 남기지 않는다.
- 마감 알림을 켜면 창을 닫아도 시스템 트레이에서 앱이 계속 실행된다. 완전히 종료하려면 트레이 메뉴의 종료를 사용한다.

## 검증과 패키징

```shell
./gradlew :desktopApp:test
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

MSI는 Windows 환경에서 다음 태스크로 생성한다.

```shell
./gradlew :desktopApp:packageMsi
```
