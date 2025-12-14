# Work Log

## 2025-12-13

### Java TypeTag 에러 해결 및 Lombok 설정 최적화

#### 문제 상황
- `BookLoanController.java` 컴파일 중 다음 에러 발생:
  ```
  java.lang.ExceptionInInitializerError: Exception java.lang.NoSuchFieldException:
  com.sun.tools.javac.code.TypeTag :: UNKNOWN [in thread "RepositoryUpdater.worker"]
  ```
- Lombok과 MapStruct 어노테이션 프로세서 간 호환성 문제로 확인

#### 해결 작업

1. **build.gradle 의존성 수정**
   - Lombok 버전을 명시적으로 `1.18.30`으로 지정
   - Lombok 선언을 MapStruct보다 먼저 오도록 순서 변경
   - 파일 위치: `booker-server/build.gradle:46-53`

   ```gradle
   // Lombok (must be declared before MapStruct)
   compileOnly 'org.projectlombok:lombok:1.18.30'
   annotationProcessor 'org.projectlombok:lombok:1.18.30'

   // Mapping
   implementation 'org.mapstruct:mapstruct:1.5.5.Final'
   annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.5.Final'
   annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
   ```

2. **Gradle 의존성 새로고침 및 빌드**
   - 명령어: `./gradlew clean build --refresh-dependencies`
   - 컴파일 성공 확인

3. **테스트 에러 해결**
   - `RBACTestControllerIntegrationTest.java` 에러 발견
   - 원인: Keycloak 제거 시 `RBACTestController` 클래스는 삭제되었으나 테스트 파일만 남아있음
   - 조치: 고아 테스트 파일 삭제
   - 파일: `booker-server/src/test/java/com/bookerapp/core/presentation/controller/RBACTestControllerIntegrationTest.java`

4. **IDE 설정 (VS Code)**
   - 문제: IDE가 Lombok이 생성한 `getUserId()` 메서드를 인식하지 못함
   - 해결: "Java: Clean Java Language Server Workspace" 명령 2회 실행
   - Gradle 빌드는 정상 동작 확인 (`BUILD SUCCESSFUL`)

#### 결과
- ✅ TypeTag 에러 완전히 해결
- ✅ Gradle 빌드 성공 (`./gradlew clean build -x test`)
- ✅ 컴파일 성공 (`:compileJava`, `:compileTestJava`)
- ⚠️ 일부 통합 테스트는 MySQL 연결 필요 (12개 테스트 실패, DB 미실행으로 인한 것)

#### 참고사항
- Java 17 환경
- Spring Boot 3.2.6
- Lombok 1.18.30 + MapStruct 1.5.5.Final 조합 사용
