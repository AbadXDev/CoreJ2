# OpenJDK Mobile patches (CoreJ2)

Applied on top of Gluon OpenJDK Mobile (`421bb8f0…`) via
`scripts/openjdk-mobile/apply-patches.sh` before `04-build-static-libs.sh`.

| Patch | Why |
|-------|-----|
| `0001-static-jimage-direct-symbols.patch` | Hard refs for `JIMAGE_*` / `JDK_Canonicalize` (avoid PC=0) |
| `0002-ios-boot-path-lib-modules.patch` | Canonical `%/lib/modules` boot path |
| `0003-ios-rtld-default-process-handle.patch` | iOS `RTLD_DEFAULT` for dlsym |
| `0004-static-zip-inflate-fully.patch` | Hard ref `ZIP_InflateFully` |
| `0005-ios-operating-system-enum.patch` | Map build OS `ios` → `OperatingSystem.MACOS` |

After `0005`, rebuild the modules image (`build-jlink-java-home.sh`) so
`java.base` inside `lib/modules` picks up the class change.
