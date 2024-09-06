# Lawndesk Fork by Rikumi

Lawndesk is an Android launcher without app drawer based on an ancient (Android O) version of Lawnchair.

This fork changes some of the default options, making it more modern and Pixel-ish. Some features are removed only to allow the build to pass.

### Building the App
- Create a `local.properties` file and write your Android SDK location into it (SDK Version 28 is recommended):
    ```
    # Example
    sdk.dir=/Users/your_username_here/Library/Android/sdk/
    ```
- Set JAVA_HOME environment variable to a JDK 11 instance home to be compatible with the ancient code.
- Run `./gradlew assembleAospLawnchairDebug` to build. The artifact is at `build/outputs/apk/aospLawnchair/debug/Lawndesk-1.0.apk`. Example:
    ```
    JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home PATH=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home/bin:$PATH ./gradlew assembleAospLawnchairDebug && adb install build/outputs/apk/aospLawnchair/debug/Lawndesk-1.0.apk
    ```

## License
Lawndesk is distributed under the [*GPLv3* license](https://www.gnu.org/licenses/gpl-3.0.en.html).

## Credit
- [Lawnchair Launcher](https://github.com/LawnchairLauncher/Lawnchair)
- [Lawndesk](https://github.com/renzhn/Lawndesk)
