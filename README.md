# RAMUS

[English](#english) | [Русский](#русский)

## English

### Project updated by t.me/denypanic



**Java-based IDEF0 & DFD Modeler**

<img width="1792" alt="Screenshot 2019-11-18 at 11 14 26" src="https://user-images.githubusercontent.com/2261228/69039713-23c56d00-09f5-11ea-99c5-b6714efe3037.png">

<img width="1792" alt="Screenshot 2019-11-18 at 11 14 59" src="https://user-images.githubusercontent.com/2261228/69039723-27f18a80-09f5-11ea-9a8d-508069ce7bbd.png">

---

## How to Start the Application

### Step 1: Install JDK

Download and install the [Oracle JDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).

### Step 2: Run the Application

In the console, navigate to the project folder and run:

```bash
./gradlew runLocal
```

### Step 3: Test the Application

#### For Linux (Tested on Ubuntu 20.04 and Fedora 34)

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/Vitaliy-Yakovchuk/ramus.git
   ```

2. **Navigate to the Project Folder:**

   ```bash
   cd ramus
   ```

3. **Run the Application:**

   ```bash
   ./gradlew runLocal
   ```

### Optional: Create a Shortcut to Launch the Application

1. Open your `.bash_aliases` file:
   ```bash
   nano ~/.bash_aliases
   ```

2. Add the following alias to easily launch the application:

   ```bash
   alias ramus='cd ~/path/to/ramus/folder/ && ./gradlew runLocal &'
   ```

3. Save the file and reload it:

   ```bash
   source ~/.bash_aliases
   ```

4. Now, you can simply run `ramus` in the terminal to launch the application.

---

## macOS: Build .app and .dmg

This project contains Gradle tasks to build a native macOS `.app` bundle and a styled drag‑and‑drop `.dmg` (with background, proper icon layout, and mac‑style shortcuts).

### Requirements

- macOS 10.13+ with Command Line Tools (`xcode-select --install`).
- JDK 11 to build and run Gradle tasks. The produced app can embed its own JRE.
- Built‑in macOS tools used by tasks: `hdiutil`, `osascript`, `sips`, `iconutil`.

### Quick Start

- Build styled DMG with embedded JREs (Intel + Apple Silicon):
  - `./gradlew macDmgWithJreStyled`
  - Output: `build/macos/Ramus.dmg`, app: `build/macos/Ramus.app`
  - The task will auto‑download JREs from Adoptium if not provided locally.

- Build styled DMG without JRE (smaller download):
  - `./gradlew macDmgStyled`
  - Launcher auto‑downloads a JRE on the user’s first run into `~/Library/Application Support/Ramus/jre/<arch>`.

### App Bundle Only

- Create `.app` without JRE: `./gradlew macBundle`
- Create `.app` with embedded JREs: `./gradlew macBundleWithJre`

### Icon and Background

- App icon (.icns):
  - Place a square PNG at `dest/izpack/icon.png` and run `./gradlew macIcon`.
  - Or put your `.icns` at `dest/macos/ramus.icns`.

- DMG background:
  - Default location: `dest/macos/dmg-background.png`.
  - Generate gradient + title (optional logo):
    - `./gradlew genDmgBackground -PbgWidth=1440 -PbgHeight=900 -PbgText="Ramus" -PbgLogoPng=/path/logo.png`
  - Generate background with QR (1440×900):
    - `./gradlew genDmgBackgroundQr -PqrData="https://t.me/denypanic"`

The DMG window is sized to the background image so it’s fully visible. Icon positions are calculated from the background size (Ramus.app on the left, Applications on the right). Only `Ramus.app` and the `Applications` symlink are visible in the DMG.

### Java 11 and Gradle wrapper

Gradle wrapper in this repo is `6.9.4`, which must run on JDK 11/17 (not JDK 20+). If you have multiple JDKs installed and see an error like “Unsupported class file major version 67”, force Gradle to use JDK 11:

```bash
brew install --cask temurin@11   # if JDK 11 is not installed

export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"

# Stop daemons that may run under a different JDK
./gradlew --stop

# Verify wrapper is using JDK 11
./gradlew --no-daemon -Dorg.gradle.java.home="$JAVA_HOME" -v

# Build DMG
./gradlew --no-daemon -Dorg.gradle.java.home="$JAVA_HOME" macDmgWithJreStyled
```

You can also pin JDK 11 for Gradle permanently by adding to `~/.gradle/gradle.properties`:

```
org.gradle.java.home=/Library/Java/JavaVirtualMachines/temurin-11.jdk/Contents/Home
```

### Embedded JRE Options

By default, `macDmgWithJreStyled` downloads latest Temurin 11 JRE for both architectures via Adoptium API. You can override sources:

- Local JRE directories (must contain `bin/java`):
  - `./gradlew macDmgWithJreStyled -PjreX64=/path/to/x64/Contents/Home -PjreArm64=/path/to/arm64/Contents/Home`
- Environment variables:
  - `JRE_X64_DIR`, `JRE_ARM64_DIR`
- Pre‑seed folders:
  - Put extracted JREs to `dest/macos/jre/x86_64` and `dest/macos/jre/arm64`
- Custom URLs:
  - `-PjreX64Url=... -PjreArm64Url=...` (JRE or JDK archives)

### macOS UX Adaptation

- mac menu bar and app name are set (`apple.laf.useScreenMenuBar`, `apple.awt.application.name`).
- Shortcuts mapped to Command on macOS (Ctrl on Windows/Linux automatically becomes Cmd on mac).
- Launcher uses `-XstartOnFirstThread` and supports offline/on‑demand JRE.

### Signing and Notarization (optional)

- Code sign the app:
  - `codesign --deep --force --options runtime --sign "Developer ID Application: YOUR NAME (TEAMID)" build/macos/Ramus.app`
- Verify:
  - `codesign --verify --deep --strict --verbose=2 build/macos/Ramus.app`
- Notarize and staple (requires Apple ID credentials or notarytool profile):
  - `xcrun notarytool submit build/macos/Ramus.dmg --apple-id <id> --team-id <team> --keychain-profile <profile> --wait`
  - `xcrun stapler staple build/macos/Ramus.app`

Contact if you want Gradle tasks for automated signing/notarization.

### Troubleshooting

- `hdiutil: resource busy` during convert:
  - The build already detaches the mounted volume and waits; if it happens, rerun or ensure no Finder window is locking the mount.
- DMG shows `.background` or `.fseventsd`:
  - Build hides/cleans these before convert; if Finder shows hidden files globally, `.background` may still appear in list view but it’s hidden.
- App opens but Java not found:
  - Use the DMG with embedded JRE (`macDmgWithJreStyled`) or ensure network is available for first run of the slim DMG (`macDmgStyled`).
- Title block: PROJECT value not shown:
  - Set project name via Model Properties → General → Project name, then press OK.
  - On small page sizes (A4) earlier versions could hide the value; this was fixed. Update to the latest sources and rebuild.
  - The label “PROJECT:” is always printed; the value is drawn next to it (inline) if space is tight, or centered in the reserved area otherwise.

### macOS: Open .rsf files with Ramus

- The app bundle declares the `rsf` document type in `Info.plist`, so Finder can associate `.rsf` files with Ramus.
- First launch `build/macos/Ramus.app` once so the system registers the document type; then double‑clicking a `.rsf` opens it in Ramus.
- To make Ramus the default app for `.rsf`: Finder → select any `.rsf` → Cmd+I → “Open with” → “Ramus” → “Change All…”.
- The app supports the macOS “Open With…” event: if Ramus уже запущен, файл откроется в существующем окне.

## Русский

### Запуск приложения

1) Установите JDK 11.

2) Из корня проекта запустите:
```bash
brew install --cask temurin@11
```
После установки Java 11 локально для сборки проекта используем эту версию.

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
```

Билдим проект.

```bash
./gradlew runLocal
```

Если Gradle ругается на “Unsupported class file major version …”, значит он запустился под новой Java. Форсируйте JDK 11 для Gradle:

```bash
./gradlew --stop
export JAVA_HOME=$(/usr/libexec/java_home -v 11)
export PATH="$JAVA_HOME/bin:$PATH"
./gradlew --no-daemon -Dorg.gradle.java.home="$JAVA_HOME" macDmgWithJreStyled
```

### Сборка под macOS: .app и .dmg

- Styled DMG с встроенной JRE (Intel + Apple Silicon):
  - `./gradlew macDmgWithJreStyled`
  - Результат: `build/macos/Ramus.dmg`, приложение: `build/macos/Ramus.app`
  - При необходимости Gradle сам скачает JRE через Adoptium API.

- Styled DMG без JRE (меньше размер):
  - `./gradlew macDmgStyled`
  - При первом запуске лаунчер скачает JRE в `~/Library/Application Support/Ramus/jre/<arch>`.

- Только .app:
  - Без JRE: `./gradlew macBundle`
  - С JRE внутри: `./gradlew macBundleWithJre`

### Иконка и фон DMG

- Иконка приложения (`.icns`):
  - Положите квадратный PNG в `dest/izpack/icon.png` и выполните `./gradlew macIcon`,
    или положите готовый `dest/macos/ramus.icns`.

- Фон DMG:
  - Файл по умолчанию: `dest/macos/dmg-background.png`.
  - Генерация градиента + заголовка (опционально с логотипом):
    - `./gradlew genDmgBackground -PbgWidth=1440 -PbgHeight=900 -PbgText="Ramus" -PbgLogoPng=/полный/путь/logo.png`
  - Генерация фона с QR (1440×900):
    - `./gradlew genDmgBackgroundQr -PqrData="https://t.me/denypanic"`

Окно Finder в DMG автоматически подгоняется под размеры фоновой картинки, чтобы фон был виден целиком. В окне остаются только `Ramus.app` и ярлык `Applications`.

### Настройка JRE

- По умолчанию `macDmgWithJreStyled` скачивает Temurin 11 JRE для x86_64 и arm64. Можно переопределить источники:
  - Локальные директории (должен быть `bin/java`):
    - `./gradlew macDmgWithJreStyled -PjreX64=/путь/к/x64/Contents/Home -PjreArm64=/путь/к/arm64/Contents/Home`
  - Переменные окружения: `JRE_X64_DIR`, `JRE_ARM64_DIR`
  - Предварительно распакованные папки: `dest/macos/jre/x86_64`, `dest/macos/jre/arm64`
  - Прямые ссылки: `-PjreX64Url=... -PjreArm64Url=...` (JRE или JDK архивы)

### Адаптация под macOS

- Системное меню macOS и имя приложения настроены (`apple.laf.useScreenMenuBar`, `apple.awt.application.name`).
- Горячие клавиши автоматически маппятся на Command (Cmd) вместо Ctrl.
- Лаунчер использует `-XstartOnFirstThread`, поддерживает офлайн JRE и автоматическую догрузку.

### Открытие файлов .rsf (macOS)

- Приложение объявляет тип документа `.rsf` в `dest/macos/Info.plist`, поэтому Finder предлагает открыть такие файлы в Ramus.
- После первого запуска `Ramus.app` двойной клик по `.rsf` откроет файл в Ramus. Чтобы сделать Ramus приложением по умолчанию: Finder → Выбрать `.rsf` → Cmd+I → “Открыть в программе” → “Ramus” → “Изменить всё…”.
- Если приложение уже запущено, файл открывается в текущем окне (обработчик `Open Files`).

### Подпись и нотаризация (опционально)

- Подписать приложение:
  - `codesign --deep --force --options runtime --sign "Developer ID Application: YOUR NAME (TEAMID)" build/macos/Ramus.app`
- Проверить подпись:
  - `codesign --verify --deep --strict --verbose=2 build/macos/Ramus.app`
- Отправить на нотацию и «прошить»:
  - `xcrun notarytool submit build/macos/Ramus.dmg --apple-id <id> --team-id <team> --keychain-profile <profile> --wait`
  - `xcrun stapler staple build/macos/Ramus.app`

### Частые проблемы

- `hdiutil: resource busy` во время convert:
  - Сборка автоматически размонтирует том и ждёт; если всё же произошло — закройте окно Finder с томом и повторите.
- В окне DMG видны `.background` или `.fseventsd`:
  
- В шапке не отображается значение «ПРОЕКТ:»:
  - Укажите «Название проекта» в Свойства модели → Главные → Название проекта и нажмите OK.
  - На узких листах (A4) старые версии могли скрывать значение — это исправлено. Пересоберите приложение.
  - Лейбл «ПРОЕКТ:» печатается всегда; значение выводится сразу после лейбла, либо по центру выделенного места (если хватает ширины).
  - Сборка скрывает/удаляет служебные папки перед упаковкой. При включённом показе скрытых файлов Finder может отображать их как скрытые — в нормальном режиме они не видны.
- Приложение запустилось, но нет Java:
  - Используйте DMG со встроенной JRE (`macDmgWithJreStyled`) или обеспечьте доступ в интернет для авто‑догрузки при первом запуске (`macDmgStyled`).
