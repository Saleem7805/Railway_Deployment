# LMS final feature and error-fix update — 2026-09-25

## Mentor tasks
- Mentor has a visible **Tasks** menu/page.
- Mentor can assign a task to selected learners or an entire batch.
- Batch assignment now sends the real MongoDB batch ID instead of the display batch code.
- Mentor can see **Tasks I assigned**, including tasks that have not yet been submitted.
- Mentor can open/view an assigned task before submission.
- Assignment details now include the brief and resource links in the task thread.
- Task creation is restricted to learners visible to the mentor.
- Learner task submission/update continues to notify the assigning mentor.

## Mentor check-in / check-out
- Added a dedicated **Mentor Check in / Check out** page and navigation entry.
- Mentor can check in and check out from the page.
- Existing heartbeat tracking remains active.
- Admin report continues to show mentor and learner attendance together.
- Hardened the attendance report for older records with missing/null close fields.
- Existing `/api/staff/study` null-response error remains fixed by avoiding `Map.of()` for nullable values.

## Super Admin videos
- Super Admin video library now includes a **Watch** action for videos posted by Super Admin.
- Shared video library remains available to Super Admin, Admin, Mentor and Learner.
- Fixed a null-safe video-library response so videos without a duration do not cause a server error.

## Existing error fixes retained
- Duplicate JAVA bundle/course insertion is handled without an unhandled MongoDB duplicate-key failure.
