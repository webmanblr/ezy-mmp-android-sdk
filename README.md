# EzyMMP Android SDK Library

`ezymmp` is a lightweight Gradle library for Android apps to attribute app installs and track events (such as purchases, signups, etc.) back to EzyURL short links.

## Installation

### Option A: Via JitPack (Recommended for GitHub repos)

1. Add the JitPack repository to your root `settings.gradle.kts` (or `build.gradle`):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

2. Add the dependency to your app module's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.github.webmanblr:ezy-mmp-android-sdk:1.0.3")
    // Required for install attribution:
    implementation("com.android.installreferrer:installreferrer:2.2")
}
```

---

### Option B: Local Maven / AAR Integration

If published to your local Maven repository or private Maven feed:

```kotlin
dependencies {
    implementation("io.ezyurl:ezymmp:1.0.0")
    implementation("com.android.installreferrer:installreferrer:2.2")
}
```

---

## Quick Usage

### 1. Initialize in `Application.onCreate()`

```kotlin
import io.ezyurl.mmp.EzyMMP

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Initialize EzyMMP SDK with your API key
        // Default endpoint is https://ezyurl.io/api/v1/sdk
        EzyMMP.init(
            context = this,
            apiKey = "ezkey_YOUR_API_KEY"
        )
    }
}
```

### 2. Track Purchases & Revenue

```kotlin
EzyMMP.getInstance().trackPurchase(
    revenue = 799.00,
    currency = "INR",
    transactionId = "TXN_123456789",
    extraData = mapOf("plan" to "premium_monthly")
)
```

### 3. Track Custom Events

```kotlin
EzyMMP.getInstance().trackEvent("signup", mapOf("method" to "google"))
```
