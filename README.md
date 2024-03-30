# Lawndesk Fork by Rikumi

Lawndesk is an Android launcher without app drawer based on an ancient (Android O) version of Lawnchair.

This fork changes some of the default options, making it more modern and Pixel-ish. Some features are removed only to allow the build to pass.

### Building the App
- Create a `local.properties` file and write your Android SDK location into it:
    ```
    # Example
    sdk.dir=/Users/your_username_here/Library/Android/sdk/
    ```
- Set JAVA_HOME environment variable to a JDK 11 instance home to be compatible with the ancient code.
- Run `./gradlew build` and check the results. **Note: passing the whole build task is not required; check `build/outputs/apk` for the artifacts.**

## License
Lawndesk is distributed under the [*GPLv3* license](https://www.gnu.org/licenses/gpl-3.0.en.html).

## Credit
- [Lawnchair Launcher](https://github.com/LawnchairLauncher/Lawnchair)
- [Lawndesk](https://github.com/renzhn/Lawndesk)
