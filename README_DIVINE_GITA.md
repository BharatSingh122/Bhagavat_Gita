# Divine Gita: XML + Kotlin (MVVM + Room)

## Kaise chalayein (How to run)
1. Android Studio me yeh folder kholiye, **Sync Project with Gradle Files** dabaiye.
2. `app` configuration run kijiye. Pehli launch par Splash screen `assets/*.json` se Room database bharti hai.
3. Unit test: `gradlew testDebugUnitTest` (Sloka of the Day logic).

## Tech
- Kotlin (AGP 9 built-in Kotlin), XML layouts, ViewBinding, Fragments, RecyclerView, Material 3
- MVVM: Fragment → ViewModel (StateFlow) → Repository → Room DAO
- Room + KSP, DataStore (onboarding flag, translation language)
- Navigation Component (`res/navigation/nav_graph.xml`) + BottomNavigationView
- Manual DI: `di/AppContainer.kt` (no Hilt)

## Structure
```
com.divine.gita
├── GitaApplication.kt         creates AppContainer
├── di/AppContainer.kt         Room DB, repositories, preferences, seeder, use case
├── domain/                    models, repository interfaces, DailySlokaCalculator, GetDailySlokaUseCase
├── data/
│   ├── local/                 GitaDatabase, Converters, entity/Entities.kt, dao/Daos.kt
│   ├── seed/DatabaseSeeder.kt assets JSON → Room (first launch, org.json)
│   ├── repository/            *RepositoryImpl + mappers
│   └── preferences/           DataStore
└── ui/
    ├── MainActivity.kt        NavHost + bottom bar + edge-to-edge insets
    ├── common/                adapters (SlokaAdapter, QuoteAdapter), extensions, share/copy helpers
    ├── splash/ onboarding/ home/ chapters/ sloka/ quotes/ chalisa/ mood/ favorites/ settings/
```

## Sloka of the Day
`index = (Calendar.DAY_OF_YEAR - 1) % totalSlokas` → `SELECT * FROM slokas ORDER BY chapter_number, verse_number LIMIT 1 OFFSET :index`.
HomeFragment `onResume` par date dobara check karta hai, isliye midnight ke baad naya sloka aata hai.

## Data
`app/src/main/assets/` ki JSON files `scripts/prepare_assets.py` se bani hain (raw verse + translation + quotes + chalisa JSON se).
Mood → verse mapping script ke `MOODS` list me hai.

## Publish se pehle
- `chalisa.json` me har deity ki sirf 1–2 lines hain: poori chalisa add karein.
- `res/values/strings.xml` me `privacy_policy_url` badlein.
- applicationId `com.divine.gita` hai (`app/build.gradle.kts`): chahein to badal lein.
- Room `version` badhane par Migration likhein taaki favourites bache rahein.
