# Releasing

Releases are built and published by the [Release workflow](../.github/workflows/release.yml).
Pushing a version tag runs the tests and lint, builds a signed APK and App Bundle, and publishes a
GitHub release with those files, a `SHA256SUMS.txt` and auto-generated release notes.

## One-time setup: signing key

Android apps must always be signed with the **same key**. Otherwise, updates won't install over
an older version. Create the key once and keep a backup somewhere safe (e.g. a password manager).
If you lose it, you can't ship updates to existing installs.

1. Create a keystore (any machine with a JDK):

   ```sh
   keytool -genkeypair -v -keystore sudoku-release.jks -alias sudoku \
     -keyalg RSA -keysize 4096 -validity 10000
   ```

   It asks for a password and your name. Use the same password for the key when asked.

2. Encode it for GitHub:

   ```sh
   base64 -w0 sudoku-release.jks > sudoku-release.jks.b64     # Linux
   base64 -i sudoku-release.jks -o sudoku-release.jks.b64     # macOS
   ```

3. In the repository on GitHub, go to **Settings → Secrets and variables → Actions → New repository
   secret** and add:

   | Secret                      | Value                                   |
   |-----------------------------|-----------------------------------------|
   | `SIGNING_KEYSTORE_BASE64`   | Contents of `sudoku-release.jks.b64`    |
   | `SIGNING_KEYSTORE_PASSWORD` | The keystore password                   |
   | `SIGNING_KEY_ALIAS`         | `sudoku` (the alias used above)         |
   | `SIGNING_KEY_PASSWORD`      | The key password (same as above)        |

   Then delete the `.b64` file. Never commit the keystore.

The workflow stops with a clear error if a secret is missing, and it refuses to publish an APK
signed with the debug key.

## Publishing a release

```sh
git checkout main && git pull
git tag v1.0.0
git push origin v1.0.0
```

Watch the run under the repository's **Actions** tab. When it finishes, the release is on the
**Releases** page.

- **Version numbers come from the tag.** `v1.2.3` becomes versionName `1.2.3` and versionCode
  `10203` (major × 10000 + minor × 100 + patch), so every newer tag gets a higher code. Minor and
  patch go up to 99.
- **Pre-releases:** a tag with a suffix, like `v1.1.0-beta.1`, is published as a pre-release.
  It shares its versionCode with `v1.1.0`. To update a beta install to the final version,
  uninstall the beta first, or bump the patch number.
- **Re-running:** to rebuild an existing tag (e.g. after fixing a secret), run the workflow
  manually from the Actions tab (**Run workflow**) and enter the tag. The existing release is
  updated.
- **Release notes** are generated from the pull requests and commits since the previous tag.
  Labels on pull requests (`feature`, `bug`, …) group them, see
  [`.github/release.yml`](../.github/release.yml).
- **Crash reports:** release builds are minified with R8. The `mapping.txt` needed to decode stack
  traces is kept as a workflow artifact for 90 days.

## Files in a release

| File                     | Use                                                         |
|--------------------------|-------------------------------------------------------------|
| `sudoku-<version>.apk`   | Install directly on a phone (allow installs from the browser/files app) |
| `sudoku-<version>.aab`   | Upload to Google Play                                       |
| `SHA256SUMS.txt`         | Checksums to verify the downloads                           |

## Local release builds

Without the signing environment variables, `./gradlew assembleRelease` signs with the debug key so
the APK still installs for testing. To sign with the real key locally:

```sh
export SIGNING_KEYSTORE_PATH=/path/to/sudoku-release.jks
export SIGNING_KEYSTORE_PASSWORD=... SIGNING_KEY_ALIAS=sudoku SIGNING_KEY_PASSWORD=...
./gradlew assembleRelease -PversionName=1.0.0 -PversionCode=10000
```
