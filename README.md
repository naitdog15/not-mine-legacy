# Not Mine Legacy

Not Mine Legacy (NML) is the shared compatibility and service layer for the Not Mine Legacy module mods. It is a library: on its own it adds no items, blocks, entities or world generation, so installing it by itself changes very little in the game.

Its job is to hold the pieces that more than one NML module needs, so that each module does not have to reinvent them. Other NML modules, and mods outside this project, can connect to it when they need one of these services. Connecting to NML is always optional for a mod that uses it: the library is not a hard requirement of anything it does not explicitly list as a dependency.

## What it provides

The public entry points that other mods can call are:

- `ModChannel` (`com.temog.notminelegacy.network`): a shared network channel helper. Each mod registers its own channel under its own mod id, so packet ids from different mods cannot collide.
- `XpMath` (`com.temog.notminelegacy.xp`): experience-point helper maths, including the level curve, orb partitioning, and player XP accounting.
- `EnvironmentSampler` (`com.temog.notminelegacy.environment`): a stateless read of a position's surroundings (lava, water, fire, temperature, sky access, biome and age), returned as an `EnvironmentSample`.

The source also contains a speech-bubble service, a village key registry, and compatibility bridges for a number of other mods. Those bridges are only active when the mod they target is present.

## Dependencies

Not Mine Legacy declares no required dependencies in its metadata. Every integration below is optional: it is only used when the mod is installed, and nothing breaks when it is missing.

Optional integrations:

- Curios
- AeroBlender
- TerraBlender
- Vanilla Backport
- Quark
- Aether
- Citadel
- Sea Dwellers
- Deeper Oceans
- Regions Unexplored
- Tinkers' Construct

Client-side only, optional:

- Particular
- GeckoLib

## Installation

Put the Not Mine Legacy jar in your `mods` folder, alongside any other NML module you want. Nothing needs to be configured for the library itself to start.

## Licence

Not Mine Legacy is released under the MIT licence. See the `LICENSE` file in this repository for the full text.

## Third-party software

The Not Mine Legacy jar does not include the mods it integrates with, or the libraries they ship. Each optional integration above is detected at runtime, and the mod it refers to is downloaded and licensed separately by its own authors. Minecraft and Forge are likewise not included, and remain under their own licences.

Please check the licence of each dependency you install alongside this mod. Nothing in this licence changes the terms of those projects.

## Credits

Temog.
