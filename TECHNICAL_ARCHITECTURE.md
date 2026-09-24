# DevPulse AI — Technical Architecture

This document details the system design, architectural patterns, concurrency mechanisms, and data flows implemented in the **DevPulse AI** native Android application.

---

## 🏗️ Architectural Overview (MVVM + Clean Architecture)

DevPulse AI follows the **MVVM (Model-View-ViewModel)** architectural pattern, decoupled into clean layers to enforce separation of concerns, testability, and scalability.

```
       +-----------------------------------------------------+
       |                  PRESENTATION LAYER                 |
       |  Jetpack Compose Views (Screens & Custom Canvas)   |
       +------------------------------------+----------------+
                                            | Observes StateFlow
                                            v
       +-----------------------------------------------------+
       |                   VIEWMODEL LAYER                   |
       |     ProfileViewModel   |   LoginViewModel           |
       +------------------------------------+----------------+
                                            | Calls Repository (Coroutines)
                                            v
       +-----------------------------------------------------+
       |                   REPOSITORY LAYER                  |
       |                 GitHubRepository                    |
       +-----------------+------------------+----------------+
                         |                  |
                         v (Local Token)    v (HTTP Calls)
       +-----------------+-------+  +-------+----------------+
       |   DATA LAYER (BuildConfig)|  | DATA LAYER (Retrofit)  |
       |      local.properties   |  |   GitHubApiService     |
       +-------------------------+  +------------------------+
```

### 1. Presentation Layer (UI & Custom Graphics)
- **Jetpack Compose**: 100% declarative UI built with Material 3.
- **Custom Canvas Rendering**: High-performance, lightweight vector drawing of visual charts (Bezier curves, donut progress segments, rounded bar layouts) using Compose `Canvas` APIs, avoiding third-party dependency overhead and minimizing layout passes.
- **State Observation**: Screens observe ViewModel states reactively using `collectAsStateWithLifecycle()` or `collectAsState()` to trigger recompositions only when state changes.

### 2. ViewModel Layer (State Machines)
- **StateFlow & UI State**: ViewModel uses sealed interfaces (`ProfileUiState`) and exposes read-only states (`asStateFlow()`) to guarantee state unidirectionality (UDF).
- **Coroutine Scopes**: Launches long-running business flows inside the lifecycle-aware `viewModelScope`, automatically cleaning up active jobs during configuration changes or view destruction.

### 3. Repository Layer (Data Processing & Coordination)
- **Parallel Dispatching**: Combines multi-request responses using Kotlin Coroutine async operations to execute parallel requests under the `Dispatchers.IO` thread pool.
- **Error Mapping**: Translates raw network exception codes (403, 404, 500) and Java `IOExceptions` into structured domain exceptions (`RateLimitException`, `UserNotFoundException`, `NetworkException`).

### 4. Data Layer (APIs & Secrets Configuration)
- **Retrofit & OkHttp**: Handles serialization, deserialization (using Gson converters), and HTTP interceptor setups.
- **Local Secret Compilation**: Injects private tokens dynamically through generated `BuildConfig` structures at compile time via Gradle task definitions.

---

## ⚡ Concurrency & Network Lifecycle

The network fetching routine is optimized for low-latency completion and safety against network degradation:

```kotlin
suspend fun getFullProfileData(username: String): GitHubDataPackage = withContext(Dispatchers.IO) {
    val token = com.devpulse.ai.BuildConfig.GITHUB_TOKEN
    if (token.isBlank()) {
        throw GitHubTokenMissingException("GitHub token not configured.")
    }

    try {
        // Run network IO operations concurrently
        val userDeferred = async { apiService.getUser(username) }
        val reposDeferred = async { apiService.getRepos(username) }
        val followersDeferred = async { apiService.getFollowers(username) }
        val followingDeferred = async { apiService.getFollowing(username) }

        GitHubDataPackage(
            user = userDeferred.await(),
            repos = reposDeferred.await(),
            followers = followersDeferred.await(),
            following = followingDeferred.await()
        )
    } catch (e: HttpException) {
        // JSON parsing of body error payloads + mapping to typed exceptions
    }
}
```

### Key Technical Patterns:
1. **Parallelism via `async`**: Minimizes retrieval times by resolving independent resources (User details, Repositories list, Followers, Following) concurrently. The total wait time is bounded by the slowest single API endpoint rather than the sum of all four.
2. **Resource Cleanup**: If any of the async requests fails, the enclosing `coroutineScope` cancels all other running siblings (Structured Concurrency), preventing network socket leaks.
3. **OkHttp Dynamic Interceptor**:
   - Injects the `Authorization: Bearer <GITHUB_TOKEN>` header dynamically.
   - Appends `Accept: application/vnd.github+json` to tell GitHub's server to output version-locked JSON.
   - Injects `User-Agent: DevPulse-AI` to comply with GitHub API user-agent requirements.

---

## 📊 Business Logic (Heuristics Analysis Engine)

The calculator located in [AnalysisEngine.kt](file:///Users/pawaneswaran/Desktop/Work/PROJECTS/Devpulse/app/src/main/java/com/devpulse/ai/utils/AnalysisEngine.kt) processes the raw API responses.

### 1. Normalization of Dates
Dates returned by the GitHub API are ISO 8601 strings (e.g. `2026-07-28T12:00:00Z`). To ensure crash-free formatting across different Android API levels:
- The engine uses a custom parse-fallback parser.
- It formats dates into `MMM DD, YYYY` (e.g., `Jul 28, 2026`) using `SimpleDateFormat` under the UTC TimeZone, avoiding dependence on Java 8 `java.time` APIs on older Android runtimes without desugaring overhead.

### 2. Category Heuristics Scoring
Scores (0-98) are computed using a weighted evaluation algorithm:
- **Backend Score**: Evaluated by tracking repository languages (Kotlin, Java, Go, Rust, C++, Python) combined with project description tags containing keywords like `database`, `api`, `server`, `backend`, `grpc`, `rest`.
- **Frontend Score**: Calculated by tracking languages (TypeScript, JavaScript) combined with keywords like `ui`, `frontend`, `css`, `html`, `react`, `compose`, `flutter`, `component`.
- **DevOps Score**: Evaluated against configuration markers (`Dockerfile`, `.github/workflows`, `k8s`) and keywords like `ci/CD`, `docker`, `kubernetes`, `cloud`, `terraform`.
- **AI/ML Score**: Evaluated against data science languages (Python, R) and keywords like `model`, `tensor`, `dataset`, `pytorch`, `machine learning`, `nlp`, `ai`.
- **Open Source Score**: Derived from repository popularity indicators (accumulated stargazers and forks count).
- **Problem Solving**: Structured around codebase age, commit consistency, and algorithm-based keywords in titles.

### 3. Career Roadmap Inferences
The engine calculates the weakest score among the evaluated domains and recommends a tailored roadmap:
- It creates a chronologically linked tree of learning milestones.
- Renders the node pathway visually using an interactive Compose Canvas vertical dash connector.

---

## 🎨 Custom Drawing (Compose Canvas & Animations)

Rather than embedding heavy charts libraries, DevPulse AI draws all visual graphics directly onto Compose Canvas scopes using coordinate math:

- **Concentric Donut Chart (`PremiumDonutChart`)**: Uses `drawArc()` with custom starting angles and sweeps. Segments are animated concurrently using dynamic sweep state values backed by `animateFloatAsState`.
- **Bezier Trend Graph (`PremiumLineChart`)**: Computes canvas coordinates and draws a path. A cubic Bezier line is formed by calculating control points between nodes, filled with a neon gradient brush (`Brush.verticalGradient`) underneath, and masked with clip paths.
- **Rounded Bar Chart (`PremiumBarChart`)**: Uses `drawRoundRect()` to draw vertical bars representing repository sizes, complete with glowing borders and dynamic heights.

---

## 🔐 Secure Key Generation (Gradle Build Phase)

To prevent security leaks, the Personal Access Token is isolated from git source code files:
1. `local.properties` contains `GITHUB_TOKEN=ghp_...`.
2. [app/build.gradle.kts](file:///Users/pawaneswaran/Desktop/Work/PROJECTS/Devpulse/app/build.gradle.kts) reads the file during the configuration phase.
3. Gradle generates the `BuildConfig` class in the build directory:
   ```java
   public final class BuildConfig {
       public static final String GITHUB_TOKEN = "ghp_...";
   }
   ```
4. Kotlin source files read `BuildConfig.GITHUB_TOKEN` directly, ensuring secrets remain safe in the local filesystem workspace.
