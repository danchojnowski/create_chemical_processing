# Agent guidance

## Project vision

This project is a chemical processing mod for Minecraft 1.21.1, using NeoForge 21.1.262 and Create 6.0.10. Its goal is to let players build a large, believable chemical factory using Create-style machinery and automation. Planned multiblock machines include a centrifuge, chemical reactor, and crystallizer.

Favor industrial processes that feel plausible and form connected production chains, while keeping them understandable and enjoyable in Minecraft. Fit Create's visual language and mechanical interactions: machinery should look assembled from physical parts and give players clear feedback about what it is doing. Treat real chemistry as inspiration, not a reason to compromise gameplay or claim scientific accuracy.

## Working in this repository

- Inspect the current code, assets, build configuration, and nearby conventions before changing them. Use the Gradle wrapper and the existing source layout.
- Preserve existing work and unrelated local files. Keep changes focused on the requested task.
- Follow established project conventions once they exist. When adding infrastructure or dependencies, choose versions compatible with Minecraft 1.21.1 and the project's actual loader and Create version, and document significant setup decisions.
- Avoid inventing APIs or registrations. Check the available dependencies and existing implementations before using them.

## Machinery and gameplay

- Design multiblocks with readable construction, clear assembly and disassembly behavior, and predictable interaction with Create's kinetic, item, and fluid systems where applicable.
- Consider orientation, rotation, placement, chunk loading, persistence, multiplayer synchronization, and invalid or incomplete structures when implementing machine behavior.
- Make recipes, inputs, outputs, processing conditions, and failure states understandable to players. Keep balance configurable or data-driven when the project's architecture supports it.
- Use consistent terminology and identifiers across code, recipes, translations, models, and documentation. Prefer clear names such as `crystallizer`.

## Assets

- Match existing Create-inspired assets in scale, palette, texture resolution, and level of detail. Convey machine function and moving parts clearly, including from normal gameplay distances.
- Keep blockstates, models, textures, animations, item representations, language entries, and resource identifiers aligned. Account for machine orientation and active or assembled states where relevant.
- Reuse existing project assets and conventions where appropriate; respect licenses and attribution for external material. Do not overwrite source artwork or unrelated assets.

## Verification

- Run the relevant build, tests, or validation tasks available in the repository after changes. Report what ran and any checks that could not be performed.
- For gameplay changes, check the affected machine or process in game when feasible, including multiplayer or reload behavior when relevant. For visual changes, inspect the assets in context when possible.
- Update player-facing documentation or translations when behavior or content changes require it. Do not claim a feature or test works without evidence.

## Chemical centrifuge

The intended centrifuge takes a solid or fluid input at the top and produces up to two solid and two fluid outputs at the bottom. Its square interior is 1, 2, or 3 blocks wide and has any positive height within world limits. A bearing supports the structure, with configurable metal or glass walls, a slab bottom, stairs at the top, and a custom cone-shaped Centrifuge Block above the roof. See README.md for the current provisional geometry, datapack tags, and implementation status. Preserve this distinction between the planned machine and the structure validator until processing and ports exist. Right-clicking the top block checks and marks a valid structure as assembled; periodic checks clear the state if the loaded structure becomes invalid.

The centrifuge drive is intended to use a vertical shaft under an upward-facing bearing. The server config `centrifuge.minimumRpm` defaults to 128 RPM. Running state is currently only a speed check; Create bearing contraption integration and processing remain unfinished.
