SBI Inspection App - COMPLETE V5

This complete project uses the exact SBI logo image supplied by the user in the Android UI and inspection-report PDF header.

# SBI Inspection App - Complete V4

Professional Java-only Android inspection application for field visits, cross selling, loans & advances, Customer 360, pending work, master customer import and PDF reporting.

## Build
- Compile SDK 35
- Target SDK 35
- Minimum SDK 23
- Java 17
- Gradle 8.7
- No AndroidX dependency

## V4 updates
- PDF report text uses Arial font at 14pt throughout report text, tables, header/footer and detail rows.
- Long report remarks remain wrapped across pages.
- Pending Work now has visible MODIFY and DELETE buttons on every task.
- Pending Work modification updates the stored task and reschedules its notification when a future due time is supplied.
- Delete uses a confirmation dialog.
- Existing MARK COMPLETE action remains available for incomplete tasks.
- Reports continue to show generated files, saved path and OPEN PDF.

## Application modules
- Government Office Visits
- Cross Selling: SBI LIFE, SBI GENERAL, SBI MUTUAL FUND
- Loans & Advances: LOANS, SMA, NPA
- Customer 360
- Pending Work
- Master Customer File
- Reports
- Settings

## Report requirements
Reports include SBI-style branding, branch details, visiting official details, designation, PF ID, signature, GPS, date/time, tabular inspection information and attached photographs where available.

## Important
The project is intended to be built by GitHub Actions using `.github/workflows/main.yml`. Local Android SDK/Gradle compilation is not available in the generation environment.


V6 report update: Consolidated PDF now renders every visit as a complete aligned detail table matching the individual inspection fields, including controlling person, account, contact, address, purpose, interest/IRAC, GPS, date/time and full visiting officials remarks. Photographs are intentionally excluded from the consolidated PDF. Individual inspection PDFs retain photographs. Report text uses Arial 14 pt.
