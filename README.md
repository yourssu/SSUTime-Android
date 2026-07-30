# SSUTime

숭실대학교 LMS 과제와 퀴즈 마감 정보를 확인하고 알림으로 받아볼 수 있는 Android 앱입니다.

## Android 배포

코드를 모두 작성하셨나요?

레포지토리 루트의 `SKILL.md`는 Android와 Google Play 배포 전용입니다. Codex 또는 LLM에게 아래처럼 요청하면 됩니다.

```text
배포해줘
```

명시적으로 요청하려면 다음 문구도 사용할 수 있습니다.

```text
Android 배포해줘
```

인앱 즉시 업데이트 대상을 직접 지정하려면 다음처럼 요청합니다.

```text
안드로이드로 배포해줘. 인앱 업데이트 대상
```

```text
안드로이드로 배포해줘. 인앱 업데이트 비대상
```

- `대상`: Google Play `inAppUpdatePriority`를 `5`로 설정한다.
- `비대상`: Google Play `inAppUpdatePriority`를 `0`으로 설정한다.
- 대상 여부를 생략하면 LMS API 버전 변경 여부를 배포 스킬이 검사해 자동으로 판단한다.

그러면 자동화 가이드를 따라 다음 작업을 진행합니다.

- `versionCode`를 1 올립니다.
- `versionName`을 패치 버전 기준으로 올립니다.
- 빌드와 테스트를 실행합니다.
- 변경사항을 커밋하고 원격 브랜치에 푸시합니다.
- 사용자 관점의 릴리즈 노트를 작성합니다.
- GitHub Release를 생성합니다.
- GitHub Actions를 통해 Google Play 프로덕션 배포를 제출합니다.

Google Play 업로드 이후 실제 승인 여부와 노출 시점은 Google Play 심사 상태에 따라 달라질 수 있습니다.

## Windows Desktop 배포

Windows Desktop은 `desktopApp/SKILL.md`의 Microsoft Store MSIX 배포 절차를 사용합니다. Android 배포와 혼동되지 않도록 Windows 또는 Desktop을 반드시 명시합니다.

```text
윈도우 배포해줘
```

또는 버전을 지정할 수 있습니다.

```text
Desktop 1.1.13 버전을 Microsoft Store로 배포해줘
```

이 요청은 `desktop-v<version>` 릴리즈를 만들고 Windows GitHub Actions에서 Store 제출용 MSIX를 생성합니다. 최초 Store 제출은 Actions artifact를 내려받아 Partner Center에서 수동으로 진행하고, 앱이 Store에 게시된 뒤에는 별도 Store 자격 증명으로 업데이트 제출 자동화를 연결할 수 있습니다.
