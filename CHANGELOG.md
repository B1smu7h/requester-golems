# Changelog

## 1.1.0

### Added
- Persistent public IDs for Requester Golems.
- Golem ownership, renaming, and explicit claiming for legacy golems.
- Global and per-Requester-Chest worker assignment.
- Assignment controls and assigned-golem summaries in the existing Requester Chest interface.
- Golem identity/configuration screen with status and upgrade-slot placeholders.

### Improved
- Refined the identity and assignment UI.
- Prevented the assignment text field from closing the screen when typing the E key.
- Fixed active-request cancellation click routing.
- Removed successful rename and assignment notifications from chat.

### Notes
- Upgrade slots are placeholders; upgrades are planned for a future release.
- ItemStack component-bearing items may not match source chest contents correctly. This known limitation is deferred to a future version.
