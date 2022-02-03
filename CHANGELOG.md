**36.0.0:**

- Recipe injection check, if the recipe already exists.
- Localization changed "shift" to "sneak" to be more friendly with different keybindings
- New versioning numbering, still compatible with semver, but now the major number is equal to the major of the used minecraft forge version

**39.0.0:**

- Update to 1.18.1
- Recipe injection check, if the recipe already exists.
- Localization changed "shift" to "sneak" to be more friendly with different keybindings

**38.0.0:**

- Update to 1.18
- New versioning numbering, still compatible with semver, but now the major number is equal to the major of the used minecraft forge version

**1.2.0-1.16.5:**

- Once again fixed recipe creation. This time the server crashes on world load due to RecipeManager#replaceRecipes(...) being @OnlyIn(Dist.CLIENT) in MC-1.16.5, but not in higher versions

**1.1.0-1.17.1:**

- Fixed creative items having to large default trace_width
- Battery level and trace_width now are stored as int and so can reach 2147483647 (2^31-1)
- Changed license field within the mods.toml and added a tiny description

**1.1.0-1.16.5:**

- Port to 1.16.5
- Fixed creative items having to large default trace_width
- Battery level and trace_width now are stored as int and so can reach 2147483647 (2^31-1)

**1.0.0:**

- Initial Release
