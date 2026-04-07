# Contracts2 Modernization Plan

## Overview
Modernize Contracts2 using Java 8 features and port improved patterns from Logiscript (config system, packet handling, code quality).

---

## Phase 1: Config System Overhaul
Port the `SimpleConfig*` API from Logiscript, replacing the old YAML `FileConfiguration` approach.

- [ ] **1.1** Rewrite `ConfigManager` to use `SimpleConfigContainer`, `SimpleConfigBool`, `SimpleConfigInt`, `SimpleConfigLong`
  - Replace `getMainConfig().getBoolean("debug-mode")` style calls with typed static getters like `ConfigManager.isDebugMode()`
  - Add clamping for numeric values (e.g. timer ranges, max counts)
  - Add `reload()` support
- [ ] **1.2** Update all call sites across the codebase to use the new typed getters
  - `Contracts.java` — `onEnable()` and `onServerCreated()` references
  - `EventManager.java` — any config reads
  - `ContractDataManager.java` — auto-generation limits
  - `BlueprintUtils.java` — blueprint update interval, mob constraints
  - Commands that read config values

---

## Phase 2: Java 8 Language Modernization

- [ ] **2.1** Replace anonymous inner classes with lambdas
  - `Contracts.onServerCreated()` — `StarRunnable` anonymous class
  - `EventManager.initialize()` — 3 `Listener<>` anonymous classes
  - `EventManager.initialize()` — `GUICallback` and `GUIActivationHighlightCallback` anonymous classes
  - `ContractsScrollableList` — `Comparator` anonymous classes
- [ ] **2.2** Replace manual iteration with streams
  - `DataManager.getFromUUID()` → `stream().filter().findFirst().orElse(null)`
  - `DataManager.sendDataToAllPlayers()` → `forEach`
  - `DataManager.sendAllDataToPlayer()` → `forEach`
  - `ContractDataManager.getContractsOfType()` → `stream().filter().collect()`
  - `PlayerDataManager.getServerCache()` → `stream().collect(Collectors.toSet())`
  - `ActiveContractDataManager.getServerCache()` → same
  - `ContractData.ContractType.fromString()` → `Arrays.stream().filter().findFirst()`
- [ ] **2.3** Use `Optional` where appropriate
  - `DataManager.getFromUUID()` — return type or internal usage
  - `DataManager.getDataManager()` — factory method null returns

---

## Phase 3: Packet Simplification
Reduce reflection usage and improve type safety in the networking layer.

- [ ] **3.1** Remove reflection-based constructor instantiation in `SendDataPacket.readPacketData()`
  - Use a factory/switch pattern like `ContractData.readContract()` already does for contracts
  - Extend this pattern to `PlayerData` and `ActiveContractData`
- [ ] **3.2** Add missing `ACTIVE_CONTRACT_DATA` handling to `SendDataPacket` process methods
  - Currently only handles `PLAYER_DATA` and `CONTRACT_DATA` in the switch

---

## Phase 4: Code Cleanup

- [ ] **4.1** Remove commented-out code blocks
  - `BountyContract.java` — ~100 lines of commented mob spawning logic
  - `ContractsScrollableList.java` — ~150 lines of commented button logic
  - `FlavorUtils.java` — commented lemmatization code
- [ ] **4.2** Fix broken/incomplete command implementations
  - `CompleteContractsCommand` — references undefined `contract.getClaimants()` and `ContractDataManager.completeContract()`
  - `ListContractsCommand` — references undefined contract properties
- [ ] **4.3** Fix version sync between `gradle.properties` and `mod.json`
  - Already fixed (both at 2.2.0 now)
- [ ] **4.4** Remove deleted test files from tracking
  - `Test.java` and `RandomContractTest.java` are staged for deletion

---

## Phase 5: Minor Improvements

- [ ] **5.1** Use `ConcurrentHashMap` for thread-safe client caches (ported pattern from Logiscript)
- [ ] **5.2** Add private constructor to `ConfigManager` to enforce static-only usage
- [ ] **5.3** Clean up `DataManager.getDataManager()` factory — currently compares against data classes instead of manager classes

---

## Execution Order
1. Phase 1 (Config) — foundational, many other files depend on config reads
2. Phase 2 (Java 8) — can be done file-by-file
3. Phase 3 (Packets) — isolated to networking package
4. Phase 4 (Cleanup) — safe to do anytime
5. Phase 5 (Minor) — polish pass
