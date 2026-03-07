# AppBooster Android AI Coding Guidelines

You are an expert Android engineer specializing in high-performance, maintainable Kotlin applications. Generate code that is efficient, well-documented, and follows Clean Architecture with MVVM + UDF.

---

## Core Principles

1. **Performance First** – Every decision prioritizes UI smoothness (60fps) and memory efficiency
2. **Clean Architecture** – Strict layer separation: Presentation → Domain → Data
3. **Unidirectional Data Flow (UDF)** – State flows down, events flow up
4. **Modern Kotlin** – Use latest language features, coroutines, and Flow

---

## Tech Stack

| Category | Technology |
|----------|------------|
| Language | Kotlin 2.0+ |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (constructor injection only) |
| Async | Coroutines + Flow (StateFlow/SharedFlow) |
| Navigation | Navigation Compose |
| Storage | DataStore Preferences |
| Testing | JUnit5, MockK, Turbine |

---

## 1. Architecture & Layer Separation

### Project Structure
```
com.tony.appbooster/
├── data/           # Repositories impl, DataSources, DTOs
│   ├── client/     # External API clients
│   └── repository/ # Repository implementations
├── domain/         # Pure Kotlin - NO Android imports
│   ├── client/     # Data source interfaces
│   ├── model/      # Domain entities & value objects
│   ├── repository/ # Repository interfaces
│   └── usecase/    # Business logic units
├── presentation/   # Android/Compose layer
│   ├── navigation/ # Nav graphs, Screen definitions
│   ├── screen/     # Composable screens
│   ├── ui/         # Theme, components, resources
│   └── viewmodel/  # ViewModels + UiModels
└── di/             # Hilt modules
```

### Layer Rules

**Domain Layer (Pure Kotlin)**
- ❌ No Android framework imports (`Context`, `Log`, etc.)
- ✅ Only Kotlin stdlib, coroutines, and domain models
- ✅ Use cases have single responsibility: one public `invoke()` function
- ✅ Repository interfaces defined here, implementations in Data layer

**Data Layer**
- ✅ Implements domain repository interfaces
- ✅ Maps DTOs → Domain models via explicit mapper functions
- ✅ Catches all exceptions and wraps in `Resource.Error`
- ✅ Data sources abstract external dependencies (network, DB, ADB)

**Presentation Layer**
- ✅ ViewModels depend only on UseCases or Repository interfaces
- ✅ Composables receive state and emit events (stateless when possible)
- ❌ No business logic in Composables – delegate to ViewModel

---

## 2. Documentation Standards (Complete KDoc)

Write **complete, professional** documentation. Document all public APIs thoroughly.

### Class/Interface KDoc
```kotlin
/**
 * Orchestrates wireless ADB pairing and connection establishment.
 * Validates device connectivity before optimization workflows.
 *
 * @property adbRepository Repository for ADB connection operations.
 * @property settingsRepository Repository for retrieving stored configuration.
 * @constructor Creates use case with required repository dependencies.
 */
class ConnectAdbUseCase @Inject constructor(
    private val adbRepository: AdbRepository,
    private val settingsRepository: SettingsRepository
)
```

### Function KDoc (complete with all params)
```kotlin
/**
 * Executes ADB connection flow using stored configuration.
 *
 * Retrieves connection parameters from settings, validates them,
 * and attempts to establish a wireless ADB connection.
 *
 * @param ipAddress Target device IP address for connection.
 * @param port ADB port number (default: 5555).
 * @param timeout Connection timeout in milliseconds.
 * @return [Resource.Success] with connection status on success,
 *         [Resource.Error] with [ResourceError] describing the failure.
 * @see AdbRepository.connect
 */
suspend operator fun invoke(
    ipAddress: String,
    port: Int = DEFAULT_ADB_PORT,
    timeout: Long = CONNECTION_TIMEOUT_MS
): Resource<ConnectionStatus>
```

### Data Class KDoc
```kotlin
/**
 * Represents the current ADB connection state and metadata.
 *
 * @property isConnected Whether device is currently connected.
 * @property deviceName Connected device name, null if disconnected.
 * @property ipAddress Device IP address used for connection.
 * @property port ADB port number.
 * @property connectedAt Timestamp of connection establishment.
 */
data class AdbConnectionState(
    val isConnected: Boolean,
    val deviceName: String?,
    val ipAddress: String,
    val port: Int,
    val connectedAt: Long?
)
```

### Rules
- ✅ Every public class, interface, function, and property has KDoc
- ✅ First line: business purpose (not restating the name)
- ✅ Document ALL `@param` parameters with clear descriptions
- ✅ Document `@return` for all non-Unit return types
- ✅ Document `@property` for data class fields
- ✅ Use `@throws` for expected exceptions
- ✅ Use `@see` to reference related classes/functions
- ✅ Keep descriptions concise but complete (1-3 lines per item)
- ❌ Don't document private/internal functions unless complex
- ❌ Don't repeat parameter names as descriptions

### Inline Comments
```kotlin
// Fallback to default port when config unavailable (handles first-run scenario)
val port = config?.port ?: DEFAULT_ADB_PORT
```
- ✅ Explain *why*, not *what*
- ✅ Use for workarounds, non-obvious logic, or important decisions
- ❌ Don't comment obvious code

---

## 3. MVVM + Unidirectional Data Flow (UDF)

### ViewModel Pattern
```kotlin
@HiltViewModel
class FeatureViewModel @Inject constructor(
    private val someUseCase: SomeUseCase,
    navigationManager: NavigationManager
) : BaseViewModel<FeatureUiModel>(navigationManager) {

    init {
        observeData()
    }
    
    // Events from UI
    fun onActionClicked() {
        launchUiStateUpdate(
            dataFetchBlock = { someUseCase() },
            processSuccess = { data -> 
                uiState.value.data?.copy(items = data) ?: FeatureUiModel(items = data)
            }
        )
    }
}
```

### UiModel Pattern
```kotlin
/**
 * Immutable UI state for the Dashboard screen.
 */
data class DashboardUiModel(
    val connectionState: AdbConnectionState = AdbConnectionState.Disconnected,
    val logs: List<String> = emptyList(),
    val progress: OptimizationProgress = OptimizationProgress()
)
```

### Rules
- ✅ Single `StateFlow<UIState<UiModel>>` per ViewModel
- ✅ UiModel is immutable (`val` only, use `copy()` for updates)
- ✅ Events are ViewModel functions called from Composables
- ✅ Extend `BaseViewModel` for standardized state/error handling
- ❌ Never expose MutableStateFlow to UI

---

## 4. Composable Patterns (Performance-Optimized & Material 3 Expressive)

### UI Design Philosophy
- **Material Design 3 Expressive** – Follow the latest M3 Expressive guidelines for modern, dynamic UI
- **Never be lazy with UI** – Put maximum effort into visual polish and user experience
- **User-friendly first** – Prioritize intuitive interactions and clear visual hierarchy
- **Smooth 60fps** – Balance visual richness with performance

### Screen Structure
```kotlin
/**
 * Feature screen composable handling state collection and content rendering.
 *
 * @param viewModel ViewModel instance provided by Hilt.
 */
@Composable
fun FeatureScreen(viewModel: FeatureViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    AppBaseScreen(uiState = uiState) { model ->
        FeatureContent(
            model = model,
            onAction = viewModel::onActionClicked
        )
    }
}

/**
 * Stateless content composable for the Feature screen.
 *
 * @param model Current UI state model.
 * @param onAction Callback for user action events.
 */
@Composable
private fun FeatureContent(
    model: FeatureUiModel,
    onAction: () -> Unit
) {
    // Stateless UI implementation with M3 Expressive styling
}
```

### Material Design 3 Expressive Guidelines

**Color & Theming**
```kotlin
// ✅ Use M3 dynamic color with expressive tonal palettes
MaterialTheme(
    colorScheme = dynamicColorScheme(context)  // Dynamic color when available
) { content() }

// ✅ Apply expressive color roles for emphasis
Surface(
    color = MaterialTheme.colorScheme.primaryContainer,
    tonalElevation = 2.dp
) { content() }

// ✅ Use expressive shapes from M3
Card(
    shape = MaterialTheme.shapes.large,  // Expressive rounded corners
    colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    )
) { content() }
```

**Typography & Emphasis**
```kotlin
// ✅ Use M3 typography scale with expressive hierarchy
Text(
    text = title,
    style = MaterialTheme.typography.headlineMedium,  // Bold headlines
    color = MaterialTheme.colorScheme.onSurface
)

Text(
    text = subtitle,
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.colorScheme.onSurfaceVariant  // Subtle secondary text
)
```

**Components & Spacing**
```kotlin
// ✅ Use M3 Expressive components with proper spacing
FilledTonalButton(
    onClick = onAction,
    shape = MaterialTheme.shapes.medium,
    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
) {
    Icon(Icons.Filled.PlayArrow, contentDescription = null)
    Spacer(Modifier.width(8.dp))
    Text("Start Optimization")
}

// ✅ Generous padding and breathing room
Column(
    modifier = Modifier.padding(24.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
) { content() }
```

### Performance Rules

**Stability & Recomposition**
```kotlin
// ✅ Stable - primitives, immutable data classes, lambdas
data class ItemUiModel(val id: String, val name: String)

// ✅ Lambda stability - use method references or remember
onClick = viewModel::onItemClicked  // Stable reference
onClick = remember { { viewModel.onItemClicked() } }  // When needed

// ❌ Avoid unstable collections in parameters
fun BadComponent(items: MutableList<Item>)  // Unstable!
```

**Lazy Lists**
```kotlin
LazyColumn(
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)  // M3 spacing
) {
    items(
        items = data,
        key = { it.id }  // ✅ Always provide stable keys
    ) { item ->
        ItemRow(item)
    }
}
```

**Derived State**
```kotlin
// ✅ Use derivedStateOf for computed values that read other states
val showButton by remember {
    derivedStateOf { scrollState.value > threshold }
}

// ✅ Use remember with keys for expensive computations
val processed = remember(rawData) { processData(rawData) }
```

**Side Effects**
```kotlin
// ✅ LaunchedEffect for suspending work tied to composition
LaunchedEffect(key) { 
    viewModel.loadData() 
}

// ✅ SideEffect for non-suspending work every recomposition
SideEffect { 
    analytics.trackScreen() 
}

// ✅ DisposableEffect for cleanup
DisposableEffect(key) {
    val listener = createListener()
    onDispose { listener.remove() }
}
```

### Animation (Required for All State Changes)
Animations are mandatory for professional UI. Use appropriate durations for smoothness without sluggishness.

**Content Transitions**
```kotlin
// ✅ AnimatedContent for content swaps with M3 motion
AnimatedContent(
    targetState = state,
    transitionSpec = {
        fadeIn(animationSpec = tween(300, easing = EaseOutCubic)) togetherWith
        fadeOut(animationSpec = tween(200, easing = EaseInCubic))
    },
    label = "content_transition"
) { targetState -> ... }

// ✅ AnimatedVisibility for show/hide with M3 expressive motion
AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(tween(300)) + slideInVertically(
        initialOffsetY = { it / 4 },
        animationSpec = tween(300, easing = EaseOutBack)
    ),
    exit = fadeOut(tween(200)) + slideOutVertically(
        targetOffsetY = { -it / 4 },
        animationSpec = tween(200, easing = EaseInCubic)
    )
) { ... }
```

**Value Animations**
```kotlin
// ✅ Animate state changes smoothly
val alpha by animateFloatAsState(
    targetValue = if (visible) 1f else 0f,
    animationSpec = tween(300, easing = EaseOutCubic),
    label = "alpha_animation"
)

val elevation by animateDpAsState(
    targetValue = if (isPressed) 2.dp else 8.dp,
    animationSpec = spring(stiffness = Spring.StiffnessMedium),
    label = "elevation_animation"
)
```

**Motion Guidelines**
```kotlin
// ✅ M3 Expressive motion specs (quick but not abrupt)
private object MotionTokens {
    val DurationShort = 200  // Quick feedback
    val DurationMedium = 300  // Standard transitions
    val DurationLong = 450  // Emphasis/complex transitions
    
    val EasingEmphasized = EaseOutCubic
    val EasingStandard = EaseInOutCubic
}

// ❌ Never snap between states without animation
// ❌ Avoid durations > 500ms (feels sluggish)
// ❌ Avoid durations < 100ms (feels jarring)
```

### Interactive Feedback
```kotlin
// ✅ Provide visual feedback for all interactions
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()

Surface(
    onClick = onClick,
    interactionSource = interactionSource,
    tonalElevation = animateDpAsState(
        if (isPressed) 0.dp else 4.dp,
        label = "press_elevation"
    ).value
) { ... }

// ✅ Ripple effects on clickable elements (automatic with M3 components)
```

### Previews (Required for All Composables)
```kotlin
@Preview(name = "Light", showBackground = true)
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Preview(name = "Large Font", fontScale = 1.5f, showBackground = true)
@Composable
private fun FeatureContentPreview() {
    AppBoosterTheme {
        FeatureContent(
            model = FeatureUiModel(/* preview data */),
            onAction = {}
        )
    }
}
```

---

## 5. Use Case Pattern

```kotlin
/**
 * Retrieves and combines ADB connection parameters from settings.
 */
class GetAdbConnectionConfigUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository
) {
    /**
     * @return Flow emitting current ADB configuration wrapped in [Resource].
     */
    operator fun invoke(): Flow<Resource<AdbConfig>> = 
        settingsRepository.observeAdbConfig()
            .map { config -> Resource.Success(config) }
            .catch { e -> emit(Resource.Error(ResourceError.LogicError(e.message))) }
}
```

### Rules
- ✅ Single public `operator fun invoke()`
- ✅ Constructor injection for dependencies
- ✅ Return `Resource<T>` or `Flow<Resource<T>>`
- ✅ Thin orchestration – delegate to repositories
- ❌ No Android dependencies

---

## 6. Error Handling

### Resource Pattern
```kotlin
sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(val data: ResourceError) : Resource<Nothing>()
}

sealed class ResourceError {
    data class LogicError(val errorMessage: String?, val errorCode: String? = null) : ResourceError()
    data class NetworkError(val errorMessage: String?, val exception: Throwable? = null) : ResourceError()
    data class DatabaseError(val message: String) : ResourceError()
    data object UnknownError : ResourceError()
}
```

### Rules
- ✅ Catch exceptions in Data layer, wrap in `Resource.Error`
- ✅ Use `runCatching` + `fold` for clean error handling
- ✅ UI receives errors through `UIState.error` and displays via dialog/snackbar
- ❌ Never use `!!` – use `?.let`, `?:`, or sealed types
- ❌ Never let exceptions propagate to UI layer uncaught

```kotlin
// ✅ Clean error handling in repository
suspend fun fetchData(): Resource<Data> = runCatching {
    dataSource.getData()
}.fold(
    onSuccess = { Resource.Success(it.toDomain()) },
    onFailure = { Resource.Error(ResourceError.NetworkError(it.message)) }
)
```

---

## 7. Hilt Dependency Injection

### Module Patterns
```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindRepository(impl: RepositoryImpl): Repository
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {
    @Provides @Singleton
    fun provideUseCase(repo: Repository): UseCase = UseCase(repo)
}
```

### Rules
- ✅ Use `@Binds` for interface→implementation bindings
- ✅ Use `@Provides` for complex construction or third-party types
- ✅ Constructor injection with `@Inject` for ViewModels and classes
- ✅ Scope appropriately: `@Singleton`, `@ViewModelScoped`, `@ActivityScoped`
- ❌ Never use field injection

---

## 8. Kotlin Best Practices

### Immutability
```kotlin
// ✅ Immutable by default
val items: List<Item> = listOf(...)
data class State(val count: Int, val items: List<Item>)

// ❌ Avoid mutable state outside controlled scenarios
var mutableList = mutableListOf<Item>()  // Bad
```

### Null Safety
```kotlin
// ✅ Safe calls and elvis operator
val name = user?.profile?.name ?: "Unknown"

// ✅ Scope functions for null handling
user?.let { processUser(it) }

// ❌ Never use !!
val name = user!!.name  // Forbidden
```

### Coroutines
```kotlin
// ✅ Inject dispatchers for testability
class Repository @Inject constructor(
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    suspend fun fetch() = withContext(ioDispatcher) { ... }
}

// ✅ Use viewModelScope in ViewModels
viewModelScope.launch { ... }

// ❌ Never use GlobalScope
GlobalScope.launch { ... }  // Forbidden

// ❌ Never hardcode dispatchers
withContext(Dispatchers.IO) { ... }  // Bad – inject instead
```

### Flow
```kotlin
// ✅ StateFlow for UI state
private val _state = MutableStateFlow(initialState)
val state: StateFlow<State> = _state.asStateFlow()

// ✅ Collect safely in Composables
val state by viewModel.state.collectAsState()

// ✅ Combine flows
combine(flow1, flow2) { a, b -> Result(a, b) }
```

---

## 9. Testing

### Naming Convention
```kotlin
@Test
fun `given valid config when connect invoked then returns success`() {
    // Arrange
    coEvery { repository.connect(any()) } returns Resource.Success(Unit)
    
    // Act
    val result = useCase()
    
    // Assert
    assertThat(result).isInstanceOf(Resource.Success::class.java)
}
```

### Flow Testing with Turbine
```kotlin
@Test
fun `given data updates when observing then emits all values`() = runTest {
    viewModel.state.test {
        assertThat(awaitItem().status).isEqualTo(UIStatus.IDLE)
        viewModel.loadData()
        assertThat(awaitItem().status).isEqualTo(UIStatus.LOADING)
        assertThat(awaitItem().status).isEqualTo(UIStatus.SUCCESS)
    }
}
```

---

## 10. Forbidden Patterns

| ❌ Forbidden | ✅ Use Instead |
|-------------|---------------|
| `!!` operator | `?.let`, `?:`, sealed types |
| `GlobalScope` | `viewModelScope`, injected scope |
| `Dispatchers.IO` hardcoded | Inject `@IoDispatcher` |
| Magic numbers/strings | Constants or resources |
| `AsyncTask`, `Loader` | Coroutines |
| XML layouts, DataBinding | Jetpack Compose |
| Logic in Composables | ViewModel |
| Mutable public state | Immutable + copy() |
| Field injection | Constructor injection |
| Static state snaps | Animated transitions |

---

## Quick Reference

### Creating a New Feature

1. **Domain**: Define model, repository interface, use case
2. **Data**: Implement repository, add to Hilt module
3. **Presentation**: Create UiModel, ViewModel, Screen
4. **DI**: Wire up in appropriate Hilt module
5. **Navigation**: Add route to Screen sealed class
6. **Tests**: Unit tests for ViewModel and UseCase

### File Naming
- `*UseCase.kt` – Domain use cases
- `*Repository.kt` – Domain interfaces
- `*RepositoryImpl.kt` – Data implementations
- `*DataSource.kt` – Domain data source interfaces
- `*ViewModel.kt` – Presentation ViewModels
- `*UiModel.kt` – UI state models
- `*Screen.kt` – Composable screens
- `*Module.kt` – Hilt DI modules
- `*Status.kt` – Sealed state/status types that drive composables (e.g. `HeroCardStatus.kt`)

---

## 11. Component File Separation

### Rule: Models and State Types Live in Their Own Files

Never embed data classes, sealed classes/interfaces, or enums inside a composable file.
Every type that represents a component's state or input data must have its own dedicated file.

```
components/
├── HeroCardStatus.kt       ← sealed interface / enum with all variants
├── HeroResultPanel.kt      ← composable(s) only – no top-level data types
└── DashboardStats.kt       ← composable(s) only
```

- ✅ One file per public type: `HeroCardStatus.kt` contains only `HeroCardStatus`
- ✅ Composable files contain only `@Composable` functions and their private config helpers
- ✅ Private internal config data classes (e.g. `HeroResultConfig`) may stay inside the composable file because they are not public API
- ❌ Never declare a public `data class`, `sealed class/interface`, or `enum class` inside a composable file

### Rule: Generalize Repeated Composables with a Sealed Status Type

When multiple composables share the same visual structure but differ only in data or
visual tokens (icon, colour, title, button visibility), replace them with:

1. A **sealed interface/class** capturing every variant and its data (`*Status.kt`)
2. A **single reusable composable** driven by that sealed type

```kotlin
// ✅ One sealed type describes all variants
sealed interface HeroCardStatus {
    data class Completed(val processedCount: Int, ...) : HeroCardStatus
    data class Canceled(val processedCount: Int, ...) : HeroCardStatus
    data class AllOptimized(val optimizedCount: Int, ...) : HeroCardStatus
}

// ✅ One composable handles all variants – no duplication
@Composable
fun HeroResultPanel(
    status: HeroCardStatus,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit,
    onRunAgain: () -> Unit = {}
) { ... }
```

- ✅ Visual token resolution is delegated to a private `*Config` data class resolved via `remember*Config(status)`
- ✅ The `when(status)` branch lives in the config resolver, not scattered across the composable body
- ❌ Do not maintain separate `CompletedContent`, `CanceledContent`, `AllOptimizedContent` composables that are structurally identical
