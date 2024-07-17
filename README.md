<p align="center"><img src=".idea/icon.png" alt="Logo" width="100"></p>
<h2 align="center">CreateFabricYarned</h2>

This project is to remap CreateFabric source code to Yarn mappings.
The main branch is where the instructions for this repository and how to use it are stored.

## Other branches include:

- `<version>/upstream`: Used to synchronize upstream repository.
- `<version>/temp`: Used to temporarily store the source code for remapping to yarn.
- `<version>/yarn`: CreateFabric source code after remapping.

## How to use
Pull the source code from `<version>/upstream`, then use [loom's `migratemappings`](https://fabricmc.net/wiki/tutorial:migratemappings) to convert the source code to yarn (after conversion, please manually fix the mixin),
after the conversion is complete, create a new `<version>/temp` branch for sending PRs to the `<version>/yarn` branch. Delete `<version>/temp` branch after PR merge
