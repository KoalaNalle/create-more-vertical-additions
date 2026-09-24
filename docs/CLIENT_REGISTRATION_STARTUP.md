# Vertical Belts client registration investigation

Date: 2026-09-24. Local branch: `fix/client-registration-order`.

## Finding

The original entry point declares its Registrate blocks during static class initialization,
before its constructor attaches the mod event bus. The belt's `clientExtension` listener
therefore enters Registrate's shared deferred-listener table.

The startup regression check demonstrates an actual defect in the original code: with
the repository's declared dependencies, Minecraft finishes loading but the vertical
belt still has the empty default client extension. Its custom model and renderer are
present. The check fails on the original artifact and passes after this change.

The previously recorded Dune full-pack crash is a different manifestation of the same
registration path: NeoForge rejected two distinct `BeltBlock.RenderProperties` instances
for `cmverticaladditions:vertical_belt`. Its event listener list contains three separate
`OneTimeEventReceiver` wrappers; the second fails during `BlockBuilder` registration.
There is only one explicit `clientExtension` declaration in Vertical Belts.

### Pack interaction and confidence

Inspection of the actual bundled Registrate `MC1.21-1.3.0+67` shows a shared deferred
table and global `seenModBus` flag. Deferred rows remain after being flushed.
Azimuth 1.4.9 patches `OneTimeEventReceiver.addModListener` with
`RegistrateOTERFix.azimuth$flushPerOwner`. The inspected bytecode uses a shared plain
`HashSet`, a separate `contains`/`add` pair, and replays the retained owner row. This
bookkeeping is unsynchronized across parallel mod loading.

That interaction provides a plausible route to replaying a queued registration twice.
The historical thread interleaving has **not** been reproduced or proven, so this report
does not attribute the crash exclusively to Azimuth. The local early-registration defect
and the historical duplicate-registration failure are established independently.

## Fix

Attach Registrate's event bus first, then initialize a separate `VerticalAdditionsContent`
holder. Its guard rejects accidental initialization before the bus is available. Client
listeners now attach directly to their owner's bus and do not enter the shared queue.

The block, block entity and item registry IDs, block properties, assets, recipes and belt
logic are unchanged. Internal references use the new content holder. Java consumers of
the old public `VerticalAdditions.VERTICAL_*` fields would need to update their imports;
this is not a change to world-save registry IDs.

No exception suppression, removal of the belt extension, dependency upgrade or Azimuth
modification is used. Upstream attribution and licensing metadata are retained.

## Live regression check

`runStartupCheckClient` loads a separate development-only test mod. It waits for the
client's initial resource loading to finish, verifies the actual belt extension, baked
model and renderer, and checks that this Registrate owner has no deferred callbacks.
It writes `startup-check.json`, exits the client, and fails the Gradle task on a failed
or missing report. It opens no world. It also records the loaded mod versions and screen.
Pack onboarding screens are allowed; in Baseline A this is Vista's welcome screen.

The harness uses reflection against two version-specific internals (`waitingModListeners`
and `gameLoadFinished`). It is intentionally a 1.21.1 regression check, not a production
runtime feature. The test classes/resources are excluded from the shipped mod JAR and
from the ordinary client/server runs. A desktop graphics environment is required.

### Reproduce

Use Java 21 and a new run name for each invocation:

```powershell
.\gradlew.bat build
.\gradlew.bat runStartupCheckClient '-PstartupCheckName=fixed-1' --no-configuration-cache
```

To test a packaged artifact, pass an absolute JAR path:

```powershell
$jar = (Resolve-Path .\build\libs\cmverticaladditions-neoforge-1.0.0.jar).Path
.\gradlew.bat runStartupCheckClient '-PstartupCheckName=packaged-1' "-PstartupReferenceJar=$jar" --no-configuration-cache
```

To test an external pack, additionally pass `-PstartupPackMods=<absolute mod directory>`
and the desired NeoForge version, for example `-Pneo_version=21.1.248`. That directory
must contain the pack's Create/Ponder/Flywheel dependencies. Its JARs are copied into
the isolated `build/startup-check/<name>/mods` directory. The repository's normal runtime
mod dependencies are omitted for this test. `cmverticaladditions-*.jar` is explicitly
excluded from the copy because source or `startupReferenceJar` supplies the mod under
test. Rename nonstandard duplicates or prepare a separate input snapshot yourself.
The source directory is never changed and an existing destination/report is refused.

For a before/after comparison, build the original revision in a separate checkout and
pass that JAR via `startupReferenceJar`; no reset or replacement of working source is needed.
Keep `--no-configuration-cache` for the live test task. A startup failure may leave
Minecraft's error screen open; close that test window to finish the failing task.

## Evidence and limits

Machine-readable reports and artifact hashes are in `validation/client-registration-2026-09-24/`.
Full diagnostic logs and disposable game directories remain ignored under `build/`.

- Original source: `7444b22c8268788c65a167c53e721e0df58d753a`, upstream
  [rekales/create-more-vertical-additions](https://github.com/rekales/create-more-vertical-additions).
- Original and final `build` tasks passed. The original repository has no Java unit tests;
  `test NO-SOURCE` is not counted as test coverage.
- Original declared-dependency client: regression **failed** as expected (one deferred
  listener and missing belt extension), despite reaching the menu.
- Fixed declared-dependency client: **passed** (zero deferred listeners, correct extension,
  model and renderer).
- Full-pack results are recorded individually in the evidence summary. The pack retains
  Azimuth and uses Minecraft 1.21.1, NeoForge 21.1.248 and Create 6.0.10.

| Run | Subject | Result | Deferred callbacks |
| --- | --- | --- | ---: |
| `original-1` | Original JAR, declared dependencies | Expected failure: missing extension | 1 |
| `fixed-default-1` | Fixed source, declared dependencies | Pass | 0 |
| `fixed-pack-3` | Fixed source, full pack | Pass | 0 |
| `fixed-pack-4` | Fixed packaged JAR, full pack | Pass | 0 |
| `fixed-pack-5` | Fixed packaged JAR, full pack | Pass | 0 |

All three completed full-pack regression checks passed, including two using the packaged
artifact. Every passing check found the expected extension, model and renderer. This
limited repetition supports the fix but cannot establish a statistical failure rate.

Full-pack test input consists of the 116 non-Vertical-Belts external JARs from the frozen
Dune Development Pack Baseline A, plus the built Dune 0.6.0-dev.4 JAR. Vertical Belts is
supplied by the candidate under test, and the development check adds one mod (173 loaded
mod IDs total). Fresh test directories use generated default configuration and no world.
This is a software-pack startup compatibility check, not a benchmark replication.

Harness setup attempts are retained but not counted as passing tests: an initial
accessibility screen prevented the original title-screen check; a classpath-based pack
load missed Sodium's bootstrap; a subsequent successful pack load remained on Vista's
welcome screen. Normal mods-folder discovery and load-completion detection corrected
these test setup issues. None was a new duplicate-extension crash.

These checks cover startup registration and resource loading. They do not validate item
transport, existing-world loading or every possible mod-loading schedule. The intermittent
historical exception has not been deterministically reproduced in this investigation.
At the close of this investigation, the working Dune pack and its immutable Baseline A
were unchanged, and the replacement artifact had not yet been deployed or published.
The evidence summary records that pre-publication snapshot; subsequent publication or
installation does not change these captured test results.

## API references

- [Registrate OneTimeEventReceiver source](https://github.com/tterrag1098/Registrate/blob/1.21.1/dev/src/main/java/com/tterrag/registrate/util/OneTimeEventReceiver.java)
- [CreateRegistrate source](https://github.com/Creators-of-Create/Create/blob/mc1.21.1/dev/src/main/java/com/simibubi/create/foundation/data/CreateRegistrate.java)
- [ModDevGradle documentation](https://github.com/neoforged/ModDevGradle/blob/main/README.md)

The actual local +67 Registrate and Azimuth 1.4.9 artifacts were also inspected directly;
moving upstream source links are background references, not substitutes for those versions.
