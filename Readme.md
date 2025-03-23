# Overview

The application will:

1. Process a BTV Spielplan (PDF, e.g. filename.pdf) and extract match information and create a Google Calendar
   importable CSV file.
2. Create output files in the same directory as the input file:
    - A raw text file with the extracted text (filename-debug.txt)
    - A CSV file formatted for Google Calendar import (filename.csv)

# Build

```
./gradlew build
```

# Prepare

- you need a BTV.de account for your club
- To download the plan for your team, you need to have an account on BTV.de.
- Login there and go to: Start > Ansicht > Download > Vereinsspielplan.pdf

## Download Spielplan

e.g. for Summer
2025: https://btv.liga.nu/cgi-bin/WebObjects/nuLigaDokumentTENDE.woa/wa/nuDokument?dokument=ScheduleReportFOP&club=23227&season=14843

# Run

```bash
# Run the application with the path to your PDF file
./gradlew run --args="data/2025-03-22-tc-dorfen.pdf"
```
