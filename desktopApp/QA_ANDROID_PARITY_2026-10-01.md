# Android → Windows 반영 및 QA 기록 (2026-10-01)

## 범위

- `desktop`에서 로컬 `develop`을 merge했다. merge commit: `617cc4c`.
- LMS-API 충돌은 Android 기준인 1.6.10으로 해결했다.
- 아래 대응 구현은 `desktopApp/**`에만 변경했다. Android 소스는 merge로 들어온 기준 구현이며 추가 수정하지 않았다.
- Android/Desktop 버전, 배포 workflow, 태그, Store 제출, GitHub Release는 변경하거나 실행하지 않았다.
- 기존 IDE 변경은 보존했다. 작업 완료 시 Desktop 후속 변경은 검토 가능한 working tree에 남아 있다.

## 기능 및 상태 전이 대조

| Android 기준 | Windows 반영 / 기존 구현 재확인 | 확인 근거 |
| --- | --- | --- |
| Analytics, MainContainer | 신규 사용자 식별에는 hashed distinct ID와 이전 `$anon_distinct_id` 연결. logout reset, `$is_identified`, login/alarm/cyber 이벤트 속성 일치. home 진입 시점·탭 출처, 동일 탭 클릭, 중복 disconnect 방지. 신규 attachment/calendar/banner/lab/withdraw 이벤트 연결. 종료 전 bounded flush. | DesktopAnalyticsTest: identify/event 계약. 테스트 telemetry는 sink로 차단 |
| CyberTodoMapper, CyberRepository | LMS-API 1.6.10 강의·퀴즈·과제. 완료/재제출 상태, 과목명 괄호 제거, 한국/영문 오전·오후, Seoul 날짜 → UTC. 연결 실패를 성공으로 처리하던 기존 코드 수정. | CyberTodoMapperTest, deadline parsing tests |
| LmsRefreshRepository 스마트 캐시 | 일반 과제의 일시 누락 보존, unlock_at/마감 고려, hidden/submitted 중복 제거, cyber 불완전 조회 시 기존 자료 유지. 정상 완료된 빈 조회와 연결 해제는 실제로 제거. backend report에서 cyber 제외. | Android smart merge tests를 Desktop 로직에 적용, 추가 cache tests |
| MainScreen, Deadline utils | 서울 달력 날짜 D-Day, offset/UTC/시간대 없는 날짜 파싱. D-2/D-3 색상, 안정적인 마감·이름 정렬. cyber 배너 닫기 저장/연결 배지, 버튼·문구·스피너 정렬. | deadline/cyber/cache tests, 화면 렌더링 |
| 제출 목록 정렬 | 실제 submittedAt 내림차순. 동일 시간 또는 미기록 항목은 원래 순서 보존. 마감 시각을 제출 시각으로 취급하지 않음. 제출 파일 다운로드 이벤트. | DesktopSubmittedSortTest |
| TodoDetail Screen/ViewModel | 분석 요청 조건 완화. 비어 있지 않은 서버 요약은 provisional 상태라도 표시. 실제 성공 내용이 있을 때만 AI 탭 노출. 알 수 없는 소요시간 숨김, 강의시간 라벨, 소요분 표시. 캐시·분석 응답의 첨부 링크 유지, 다운로드 행과 이벤트. LMS/Cyber 목적지 이벤트 분리. 상세 스크롤과 숨김 확인 유지. | MockEngine 401→재인증, 빈/provisional 요약, Compose 실제 summary 탭/파일 클릭 테스트와 screenshot |
| Navigation | 탭 재선택은 상세/제출/공지/My 서브 화면 루트로 복귀. Calendar 월 초기화. Calendar에서 선택한 할 일 상세 진입 유지. Cyber 연결 성공 시 profile 복귀. | Compose home 재선택 테스트, 상태 흐름 코드 대조 |
| Notice | 공지가 있는 과목 먼저, 첫 unread 선택. read ID를 저장해 새로고침 후 유지. 읽음 처리로 과목 순서가 변해도 선택한 과목을 ID로 유지. mock 안내 제거. | Compose unread → expand/read 업데이트 회귀 테스트 |
| MyPage, withdrawal | 학번 표시 제거 기준 유지, 이용안내/실험실 문구, 실험실 설정 이벤트. 실제 withdrawal API에 연결. 확인 dialog, 중복 요청 방지, 실패 후 재시도, 401/403 재인증, 성공 후 로그아웃. 프로필 loading/empty 구별. | API method/path/auth/500 테스트, Compose confirm/busy/failure/retry 테스트 |
| 로그인/세션/저장 | 자동로그인 OFF로 로그인해도 현재 세션 자격증명 유지. logout 때 cyber 자격증명·연결·실험실·익명 ID 정리, 사이버 세션을 오래된 state로 되살리는 버그 수정. cancellation을 재로그인 실패로 오인하지 않음. | 기존 session store/login tests + API retry test, Main 상태 흐름 대조 |
| Calendar 및 언어 | 날짜 파싱 통일, 일정 정렬, 월 이동 이벤트. 월·요일·일정 수·날짜 dialog·이전/다음 접근성·마감시간·탭 tooltip 한국어/영문 리소스 적용. 공유된 문구를 Android와 동기화. | 기존 calendar tests, resource compilation |
| 새 할 일 알림 | 첫 로딩 제외 신규 task queue, 18시 수집 알림/만료 정리, 전달 성공시에만 queue 소비. 기존 마감 알림 중복 키, 설정 및 숨긴 창 독립 스케줄러 재확인. | DesktopAdditionalCacheTest와 기존 notifier tests |

## Windows 대체 구현

- Android AlarmManager/FCM 대신 앱 프로세스에서 15분 간격 데이터 확인, 60초 간격 마감/신규 알림 확인 + Windows toast 사용. 창을 숨겨도 Compose app scope가 유지된다. 프로세스가 완전히 종료된 경우 Android와 같은 수신/예약 실행은 제공하지 않는다.
- Android 첨부파일 download/Intent는 기본 Windows 브라우저로 실제 링크를 연다.
- Android bottom navigation / gesture는 기존 별도 Desktop navigation/뒤로가기 UI를 사용하되 루트 복귀 상태 전이를 맞췄다.
- Android runtime 알림 권한 UI는 Windows 알림 설정과 기존 토글 경로로 대체한다.
- 단일 인스턴스 named mutex/event와 시스템 tray를 Windows에서 직접 테스트했다. 알림/자동로그인 설정과 무관하게 tray 생성, 메뉴 open/exit 호출, 실제 icon 제거, 두 번째 실행 activation 및 lock 해제를 확인했다.

## Android 전용으로 제외

- Android 홈 화면 widget/Glance, 전화·통화 권한 확인, Android bottom sheet/edge-to-edge/OS back gesture 자체는 Windows에 이식하지 않는다. task 정보는 Windows 기존 창에서 제공하며 불필요한 모바일 권한 UI를 추가하지 않았다.
- Google Play 배포, Android 버전 변경, Android Sentry secret/workflow 변경은 Desktop 작업 범위에서 제외했다.
- Windows Sentry 연결은 사용자 명시 요청으로 제외했다. Desktop 코드와 의존성에 Sentry 연결이 없음을 확인했다.

## 최종 검증

2026-10-01 Windows 호스트에서 실행:

```powershell
.\gradlew.bat :desktopApp:test :desktopApp:createDistributable --console=plain
git diff --check
```

- BUILD SUCCESSFUL. 84 tests, failures 0, errors 0, skipped 0.
- createDistributable: `desktopApp/build/compose/binaries/main/app`.
- 기존 버전으로 MakeAppx MSIX pack/unpack 및 manifest 검증 성공: `desktopApp/build/msix/SSUTime-1.1.21-windows-x64.msix`, package version 1.1.21.0. 버전을 올리거나 게시하지 않았다.
- Compose 화면을 실제 렌더링하고 `build/qa/todo-summary.png`, `notice-read.png`, `withdraw-retry.png`를 시각 확인했다. 소요시간/첨부 행/읽음 선택 유지/탈퇴 오류 재시도 화면 확인.
- Windows JNA single instance 및 실제 AWT system tray 회귀 검증 포함.
- 기존 deprecated Compose tooltip/UI test API 경고가 남아 있으나 빌드와 테스트에 실패는 없다.

## 미완료 / 추가 운영 확인이 필요한 사항

1. **Sentry — 사용자 요청으로 제외**: 2026-10-01 사용자가 Windows에서는 Sentry를 연결하지 않도록 명시했다. Desktop에는 Sentry SDK 의존성, DSN, 초기화 및 예외 전송 코드를 추가하지 않는다. 기존 승인 요청은 철회된 것으로 처리하며 승인 대기 항목이 아니다. PostHog 구현은 유지한다.
2. **Windows toast 클릭**: 누락되어 있던 활성화 브리지를 추가했다. toast protocol activation → MSIX `ssutime` protocol/Parameters → JVM main args → 기존 named-event single instance 창 복원 → PostHog `notification_tap` 연결. URI에는 무작위 UUID만 담고 분석용 속성은 사용자 데이터 폴더에 저장한다. 프로세스 간 큐 전달/1회 소비/invalid URI/7일 만료 테스트 및 MakeAppx manifest 검증을 통과했다. 설치된 MSIX에서 실제 toast 클릭에 의한 복원/서버 이벤트 수신은 추가 확인 대상이다.
3. **운영 계정/외부 서비스**: 실제 사용자 계정 로그인·Cyber 수집·요약 생성·withdrawal 성공·PostHog 서버 수신은 수행하지 않았다. API 실패/재인증 상태는 MockEngine으로 검증했다. 실제 회원탈퇴는 QA 목적으로 호출하지 않았다.
4. **설치/OS 통합**: MSIX 설치/업데이트, 실제 toast 표시/클릭, 닫기/Alt+F4/최소화/숨김 상태 toast 수신의 end-to-end 테스트는 하지 않았다. 코드 수명주기와 tray/single instance test 범위만 확인했다. createDistributable 성공은 Store package 인증 성공을 뜻하지 않는다.

## 지원 근거 (공식 자료)

- [LMS-API JVM 구현](https://github.com/chlwhdtn03/LMS-API)
- [PostHog identify SDK specification](https://github.com/PostHog/sdk-specs/blob/main/openspec/specs/identify/spec.md)
- [Compose Multiplatform 지원](https://kotlinlang.org/docs/multiplatform/compose-compatibility-and-versioning.html)
- [Firebase Cloud Messaging 클라이언트](https://firebase.google.com/docs/cloud-messaging)
- [Windows desktop toast/COM activation](https://learn.microsoft.com/en-us/windows/apps/design/shell/tiles-and-notifications/send-local-toast-desktop-cpp-wrl)
- [Windows packaged desktop activation extensions](https://learn.microsoft.com/en-us/windows/apps/desktop/modernize/desktop-to-uwp-extensions)

- [Windows toast protocol activation schema](https://learn.microsoft.com/en-us/uwp/schemas/tiles/toastschema/element-toast)

## 검토한 develop 신규 커밋

범위: 기존 Desktop HEAD `fa6311b` 이후 `develop`. 배포/모바일 전용 변경은 위 제외 범위로 분류하고 나머지 사용자 동작을 대응 구현과 대조했다.

```text
60398a2 LMS-API 최신화 및 숭싸대 퀴즈, 과제 포함
809a245 LMS_LINK_CLICK 수정 및 D-Day QA
3c2b82c 안드로이드 1.2.3 배포 및 Sentry 토큰 연결
4cf0ef8 안드로이드 1.2.2 배포 준비
60ed314 안드로이드 1.2.1 배포 준비
f044bd7 QA찐막
d7a2dbb tab 프로퍼티
a88c953 tab 프로퍼티
3122736 새로 추가된 할 일 알람 전송
fca8f8e 제출한 과제 정렬
71a0be1 마이페이지 학번 제거
ea8930a AI 요약 탭 뜨지 않던 문제 해결
3ce2348 탈퇴 확인 팝업
00db219 사이버대학 할일 사라짐 문제
d92baa8 posthog 수정
1ad885b ios 스타일 로딩스피너 적용
9c0a4b3 숭사대 마감기한 파싱 오류 해결
90ac814 카테고리 섹션 패딩 축소
5801067 뒤로가기 버튼 위치 이동
a8e33b9 숭사대 바로가기 타이핑
fa0e4ed 캘린더뷰 동작개선
81a5d14 강의 시간 표기로 수정
9c5f157 AI 요약 전송조건 완화
b24a42a 예상 소요시간 알 수 없을땐 표시안함
7d97d91 숨기기 라이팅 수정
7d904b4 위젯 안정성 향상
befd558 숭싸대 연결 뱃지 컬러 수정
86d26fe 마이페이지 설정과 이용안내로 수정
91d9d3a 스마트 캐시 병합 적용(과제 새로고침)
f6ae194 숭싸대 연결하기 아이콘 색 수정 및 탈퇴 구현, unlock_at 고려
acd0e1d 숨긴 할일 화면 되돌아가기 버튼
a76135e 알림 설정 로컬에 임시 값 저장
c5e91af 툴팁 분리
81c06e1 바텀시트 지지직 해결
d14e313 MainScreen 하단 패딩 제거
0085c5b 타이핑 수정
8623e1b 하단 nav 클릭 시 루트로 복귀
cb010c1 view_home 전송 타이밍 수정 - 전에는 로딩이 다 끝나야 view_home이 전송됐음
81e4cb9 숭싸대 뱃지 표시
04484bd 할일 D-day 컬러 및 표기방식 수
1e20435 숭싸대 타임존 LMS와 일치
06a1473 숭싸대 로그인 팝업 수정 및 로그인 캐싱 앱단에서 조치
a65b7de 공지가 있는 과목을 우선 표시하도록 수정
786d536 숭싸대 과목명 괄호 제거
16b5f98 온보딩 숭싸대 문구 수정
38d2acc 위젯 새로고침 버튼 제거
0ab3d70 툴팁이 보이지 않는 문제 개선 텍스트 색 직접지정
a9b1b4a 뒤로 가기 제스쳐를 할 경우 화면이 겹쳐보이는 문제 수정
42d950b AI 요약 탭 성공했을 경우에만 표시
be7cfd2 과제 상세 세로 스크롤 적용
2791338 전화알림 권한 제대로 검사하도록 수정
```

## Windows 1.1.22 배포 준비 추가 검증

- 사용자 Windows 배포 요청에 따라 Desktop patch를 1.1.22로 올렸다. Android 버전은 변경하지 않았다.
- 한국어 기본 환경과 영문 Windows JVM 환경에서 각각 Desktop 테스트 84개 통과.
- 공식 actionlint 1.7.12로 Windows workflow 구문·식 검증 통과.
- Store 자동 제출 job을 `microsoft-store` Environment에 연결했고, 기존 태그만 지정하는 수동 복구 입력으로 immutable commit 빌드를 유지한다.
- Sentry 연결은 사용자 요청대로 제외한다.
