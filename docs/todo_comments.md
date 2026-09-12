# Todo comments API

All routes are under
`/api/workspaces/{workspaceSlug}/projects/{projectSlug}/todos/{todoId}/comments`.
Identity comes from the security context; request user IDs are never trusted.

| Method | Suffix | Result | Access |
| --- | --- | --- | --- |
| GET | / | 200, flat chronological list | Project member or workspace SUPER_ADMIN |
| POST | / | 201, created comment | Current project member, including admins only when members |
| PATCH | /{commentId} | 200, updated comment | Author only, including former members |
| DELETE | /{commentId} | 204 | Author including former members, project admin, or workspace SUPER_ADMIN |

The workspace, project, Todo and comment must match the URL. Mismatches return
404. A missing or cross-Todo parent on creation returns 400. Unauthorized
mutations return 403 before checking whether the comment is already deleted.

## Content

POST accepts `contentJson`, `contentPlainText`, and optional `parentCommentId`.
PATCH requires both content fields; `parentCommentId` is ignored and cannot
reparent a comment.

Valid example:

```json
{
  "contentJson": {
    "type": "doc",
    "content": [
      {"type": "paragraph", "content": [{"type": "text", "text": "Hello"}]}
    ]
  },
  "contentPlainText": "Hello"
}
```

Plaintext must be non-blank. JSON must be an object with `type: "doc"` and a
non-empty `content` array; every direct child must be an object with a non-blank
string `type`. Missing fields, JSON null, {}, [], scalars, an empty content
array, or child nodes without a type return 400. Nested editor nodes are not
deeply validated. Plaintext/JSON equivalence is deliberately a trusted client
contract, matching descriptions: the server does not derive plaintext or
validate nested text. A client must not submit an empty editor document with
fabricated non-blank plaintext.

Comment request/response trees use Jackson 3, matching Spring MVC. Persistence
uses the existing Jackson 2 tree mapping; the service and shared mapper bridge
the two explicitly. HTTP tests verify structured JSON in both directions.

## Responses and deletion

CommentDto contains id, todoId, authorId, authorDisplayName, authorAvatarUrl,
parentCommentId, contentJson, contentPlainText, isDeleted, createdAt, updatedAt.
The shared mapper returns JSON null and "[comment deleted]" for deleted
comments. It never returns retained content for a deleted row. No parent
content, moderation identity, or moderation timestamps are returned.

Replies to deleted comments are allowed. Soft deletion preserves all content
and child replies, and records the initial deletedAt/deletedBy. Authorized
repeated deletion returns 204 without changing audit fields. Editing a deleted
comment returns 409. A former member cannot list the thread but can edit or
delete their own comment; PATCH returns only that comment.

JPA @Version rejects overlapping stale writes; optimistic-lock failures,
including failures raised at transaction commit, map to HTTP 409. This is
server transaction concurrency protection, not browser draft version checking.

Listing sorts by createdAt then id, with a matching database index and author
fetch graph. Pagination is deferred.

## Hard deletion and database verification

The Todo foreign key cascades; the reply foreign key does NOT recursively
cascade. TodoService locks the Todo, detaches its comments' parent links, then
deletes the Todo in one transaction. Comment creation takes the same Todo lock
so it cannot insert a reply during cleanup. Direct SQL Todo deletion must perform
the same cleanup; do not bypass the service with a raw delete of threaded Todos.
The reply relationship is immutable through the API; the bulk cleanup is solely
for hard deletion.

CommentMysqlTest is opt-in and requires an EMPTY disposable MySQL schema:

```text
mvn -Dtest=Comment*Test -Dcomment.test.jdbc.url=jdbc:mysql://127.0.0.1:33328/comments_test test
```

The fixture uses root with an empty password on an isolated local server. It
creates minimal parent tables and applies the actual V14 migration without
dropping existing tables. It checks a 25-comment chain, retained storage after
soft deletion, and stale JPA version rejection. Never point it at application
data. The normal application context test uses the configured datasource and
can run Flyway migrations.
