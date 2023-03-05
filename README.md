<H1 align="center">WidgetTest</H1>

## Technology stack

1. Android native technologies

    * Kotlin language.

    * Compose for UI layout.

2. 3rd party dependencies

    * Core

        * [Firebase](https://firebase.google.com/docs/android/setup) (Auth, Analytics, Crashlytics, Firestore, Cloud Functions).

        * [Adapty](https://adapty.io/sdk/android) as a core monetization SDK.

    * UI 

        * [CameraX](https://developer.android.com/training/camerax) for camera.

        * [ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer) for playing video.
        
        * [Coil](https://coil-kt.github.io/coil/) for image loading.
        
        * [Accomponist Pager](https://google.github.io/accompanist/pager/) for pager view.
        
        * [Lottie](https://github.com/airbnb/lottie-android) for playing animations.

    
    * Threding
        
        * [Coroutines](https://developer.android.com/kotlin/coroutines) for threding

## How to run

You can download this application [here](https://play.google.com/store/apps/details?id=com.smartfoxlab.locket.widget)

## Architecture 

1. The project is built with [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) in mind by utilizing Presentation, Domain, and Data layers. 

2. MVVM as a UI pattern provides an easy way to react to UI updates and act accordingly.


