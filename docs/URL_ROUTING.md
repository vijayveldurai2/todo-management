# Flat browser URLs

- Platform pages: `/_/{page}`
- Workspace pages: `/{workspaceSlug}/_/{page}`
- Project views/pages: `/{workspaceSlug}/{projectSlug}/_/{page}`
- Todo: `/{workspaceSlug}/{projectSlug}/{displayId}`

The `_` segment is structural, never an entity slug. No reserved-word blocklist. Lowercase only; no case normalization or redirects. Display IDs must match `^[a-z]+-\d+$` and resolve by exact stored value inside the authorized project. A todo URL never includes its current view, board or status.

API resolver: `GET /api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/resolve/{displayId}`. Existing UUID CRUD routes remain API endpoints; browser URL rules do not change those endpoints. Malformed identifiers return 404; project authorization still applies. Exact stored-value checks protect against case-insensitive database collations.

New todo IDs use lowercase ASCII prefixes. Existing stored IDs and project slugs are not rewritten. Historical uppercase IDs require an explicit migration decision before strict lowercase links can address them. Projects with non-letter prefixes need correction before creating a conforming ID; they now receive a validation error instead of generating an unresolvable identifier.

Frontend settings/notifications routes currently show placeholders, not complete features. Frontend demo IDs are allocated once on creation and stored separately from internal IDs; backend integration will supply production IDs.
