# Farming Contract Helper

A RuneLite plugin for Old School RuneScape that shows your current Farming Guild contract in one place. It tells you which crop you were given, which patch it goes in, the seed or sapling you need, the gardener protection payment, and whether you have those items.

The plugin is read-only. It reads game state and draws text, and never clicks, plants, pays or sends input.

## Features

- Side panel with the contract crop, patch type and location, required seeds and protection payment
- Item check across inventory, bank and Seed Vault, based on what the client has actually seen
- Patch status (not planted, growing, ready, diseased, dead) and an estimated completion time
- Optional compact overlay and a highlight on the contract patch
- Contract is saved per account, so it survives logouts, world hops and switching characters

## How it works

There is no varbit for the farming contract, so the plugin reads Guildmaster Jane's dialogue when she assigns or repeats a contract, the same way RuneLite's Time Tracking plugin does. If Time Tracking already knows your contract, that value is used as a starting point. Patch state is decoded from the farming patch varbits.

Bank and Seed Vault counts are only as fresh as the last time you opened them. The plugin never marks an item as missing from a container it has not seen.

## Build and run

Requires JDK 11 or newer. The Gradle wrapper is included.

```bash
./gradlew build    # compile and run tests
./gradlew run      # start a RuneLite dev client with the plugin loaded
```

On Windows in cmd, use `gradlew build` and `gradlew run`.

## Tests

89 unit tests cover crop to patch and seed mappings, protection payments, dialogue parsing, contract state changes, account switching, tick arithmetic and the patch decode tables. Parsing and tick logic are pure functions, so most tests run without a game client.

## Tech

Java 11, RuneLite client API, Gradle, JUnit, Lombok.

## Third party code

`src/main/java/com/farmingcontract/farming/` contains `Produce`, `CropState`, `PatchState` and `PatchImplementation` from RuneLite's Time Tracking plugin. Those classes are package-private in RuneLite, so an external plugin cannot use them directly. They are included under their original BSD-2-Clause licence with the copyright notices kept (Abex, NotFoxtrot). Changes are limited to the package name, access modifiers and removing patch types the guild does not use.

## Licence

BSD-2-Clause. See [LICENSE](LICENSE).
