# Todo attachments (local backend)

An attachment has a metadata row in MySQL and a file on disk. The disk filename
is a generated UUID, never a user-supplied filename. This keeps normal names for
display without allowing them to choose a filesystem path.

## API

Base:
`/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/attachments`

| Method | Suffix | Request / result |
| --- | --- | --- |
| POST | / | Multipart part `file`; 201 AttachmentDto |
| GET | / | 200 array, oldest first with ID tie-break |
| GET | /{attachmentId}/download | Authenticated binary download |
| DELETE | /{attachmentId} | 204; immediately hides attachment, queues disk cleanup |

Upload one file per request; a Todo can have multiple attachments.
Metadata contains id, todoId, fileName, sizeBytes, uploadedBy,
uploaderDisplayName, and createdAt. Storage keys and disk paths are not exposed.

Example:

```sh
curl -H "Authorization: Bearer YOUR_TOKEN" \
  -F "file=@receipt.pdf" \
  http://localhost:8080/api/workspaces/your-workspace/projects/your-project/todos/TODO_UUID/attachments
```

All operations derive identity from the security context and validate the full
workspace/project/Todo chain. Attachment lookups include both attachment ID and
Todo ID.

- Project members can upload, list and download.
- Workspace SUPER_ADMIN can list/download/moderate without project membership,
  but must join the project to upload.
- Deletion requires current project access and uploader identity or project-admin
  or workspace-SUPER_ADMIN privileges. Former members cannot download/delete.
- Deleted/nonexistent/cross-Todo attachments return 404, including repeat DELETE.
- Empty files or invalid filenames return 400. Files exceeding limits return 413.
- All file types are accepted as opaque bytes. Downloads always use
  application/octet-stream, attachment disposition, nosniff, and no-store.
  This version provides no inline rendering, content inspection, or malware scanning.

## Size settings

These are per-file limits, not a total workspace storage quota.

- Workspace default: 10 MiB (10,485,760 bytes).
- Application cap: 25 MiB (26,214,400 bytes).
- Multipart request cap: 26 MiB, allowing overhead.
- GET /api/workspaces/{workspaceId}/attachment-settings returns
  `maxFileBytes` and `applicationMaxFileBytes` to workspace members.
- PUT at the same route accepts `{"maxFileBytes": 5242880}`.
  Only workspace SUPER_ADMIN may change it; valid range is 1 through the app cap.
- Changing a limit affects future uploads only.

## Storage and cleanup

`ATTACHMENT_DIRECTORY` defaults to ./data/attachments (ignored by Git).
Resolve it relative to the backend working directory, or set an absolute path.
Keep this directory private; do not serve it through static web hosting.
Back up both this directory and the database.

AttachmentStorage separates storage operations from permissions and metadata
so a cloud implementation can replace LocalAttachmentStorage later.

Uploads enforce the byte limit while streaming, not just using the advertised
multipart size. They hold the Todo write lock, so Todo deletion cannot race the
metadata creation. Transaction rollback removes the file; incomplete writes
also clean up their partial file.

DELETE marks the metadata as deleted. Todo deletion sets the attachment's
todo_id to NULL through the foreign key. Both are persistent cleanup records.
Every minute, a worker deletes up to 100 files and then their metadata rows.
Failures retain metadata and retry on subsequent runs, including after restart.
An already-open download may finish after deletion.

An hourly scan removes unreferenced UUID files older than 24 hours, covering
process crashes during uploads or failed rollback cleanup. Upload transactions
have a 120-second timeout. This assumes a single application using this local
directory; multi-instance/cloud coordination is deferred.

## Verification

The regression suite covers access, URL mismatches, moderation, limits,
multipart/response behavior, rollback cleanup, storage traversal prevention,
retry behavior and workspace settings.

AttachmentMysqlTest is opt-in. Point it only at an EMPTY disposable schema:

```text
mvn -Dtest=AttachmentMysqlTest -Dattachment.test.jdbc.url=jdbc:mysql://127.0.0.1:33328/attachments_test test
```

It uses a local root account with an empty password, applies the actual V15
migration, hard-deletes a Todo, and verifies cleanup removes both disk bytes
and metadata. Normal application-context tests can migrate the configured
application database; exclude them when testing without that side effect.

Frontend attachment UI, comment attachments, previews, storage quotas, and file
version history are separate follow-up work.
