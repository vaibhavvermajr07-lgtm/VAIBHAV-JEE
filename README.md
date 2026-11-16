# VAIBHAV JEE — Android project

A Kotlin + Jetpack Compose Android app starter for the VAIBHAV JEE preparation command centre. Premium dark navy interface with blue/gold accents and IIT Bombay mission branding. **No AI features or AI integrations.**

## Included in this build
- Home dashboard and quick navigation
- Mission Jeet class schedule with manual class entry
- Homework / DPP list with completion toggle
- Backlog recovery list with manual entry
- Starter Physics, Chemistry and Mathematics chapter checklist
- Daily study planner
- JEE test-centre sections (test categories / setup placeholders)
- Basic activity analytics
- Local persistence for classes, homework and backlogs using SharedPreferences
- Android local notification channels and AlarmManager reminders for dated classes, homework and backlog entries
- Android 13+ notification permission request and reminder rescheduling after reboot
- GitHub Actions workflow to build a debug APK in the cloud

## Build on Android phone (without Android Studio)
1. Upload this project folder to a new GitHub repository.
2. Open the repository's **Actions** tab and allow workflows if GitHub asks.
3. Run **Build VAIBHAV JEE APK** (or push a commit to trigger it).
4. Wait for the workflow to finish successfully.
5. Open the completed workflow run, find **Artifacts**, and download `VAIBHAV-JEE-debug-apk`.
6. Extract the ZIP and install `app-debug.apk` on your Android phone. Allow installation from the browser/file manager if Android asks.

## Local Android Studio build
Open the project folder in Android Studio, sync Gradle, then Build > Build Bundle(s) / APK(s) > Build APK(s). Use JDK 17.

## Reminder format
When adding a class, homework item or backlog, begin its detail with a local date/time, for example:
`2026-11-17 16:00 • Chapter 3 notes`

Classes: reminders 30 and 10 minutes before. Homework: 24 hours and 2 hours before. Backlog: reminder at the entered time. Android battery restrictions can defer inexact alarms; grant notification permission.

## Important scope notes
This is a working starter project, not a fully production-complete JEE platform. Test screens are placeholders and do not contain an official question bank or timed exam engine yet. The syllabus list is a starter sample, not a verified exhaustive exam-year syllabus. Cloud sync, account login, PDF/OCR timetable import, recurring class editing, detailed test analytics, backup/restore and production signing are not included yet. Data currently stays on this device.
