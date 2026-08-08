- Android 앱을 제품 기능, 상태 전이 및 시각 디자인의 기준 구현으로 삼는다.
- Android 기능 또는 화면을 변경할 때 대응하는 Desktop 구현에 미치는 영향을 함께 검토한다.
- Desktop 관련 작업에는 `desktopApp/AGENTS.md`의 지침을 함께 적용한다.

## 브랜치와 플랫폼 경계

- Android의 기준 개발·배포 브랜치는 `develop`이다.
- Windows Desktop의 기준 개발·배포 브랜치는 `desktop`이다. Desktop 작업은 `desktop`에서 수행한다.
- Android 변경을 Desktop에 반영할 때는 `desktop`에서 `develop`을 merge한 뒤 Desktop 구현을 갱신한다.
- Desktop 작업이나 Windows 배포에서는 `app/**`, Android 버전, Android 릴리스 workflow를 수정·스테이징·배포하지 않는다.
- Android 작업이나 Google Play 배포에서는 `desktopApp/**`, Desktop 버전, Desktop 릴리스 workflow를 수정·스테이징·배포하지 않는다.
- 한 플랫폼의 배포 요청으로 다른 플랫폼의 버전, 태그 또는 GitHub Release를 만들지 않는다.
