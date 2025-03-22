# Overview

The application will:
1. Process the PDF and extract match information
2. Create output files in the same directory as the input file:
   - A raw text file with the extracted text (filename-debug.txt)
   - A CSV file formatted for Google Calendar import (filename.csv)


# Build on Mac OS
```
brew install tessaract
./gradlew build
```

# Prepare
## Link tessaract library
Gradle options contain this property so it can find tessaract library on mac os.

    org.gradle.jvmargs=-Djna.library.path=/usr/local/lib

https://stackoverflow.com/questions/21394537/tess4j-unsatisfied-link-error-on-mac-os-x#30724844

## Get a BTV.de account
To download the plan for your team, you need to have an account on BTV.de. There you go to 

    Start > Ansicht > Download > Vereinsspielplan.pdf


## Download Spielplan
e.g. for Summer 2025: https://btv.liga.nu/cgi-bin/WebObjects/nuLigaDokumentTENDE.woa/wa/nuDokument?dokument=ScheduleReportFOP&club=23227&season=14843


# Run
```bash
# Run the application with the path to your PDF file
./gradlew run --args="data/2025-03-22-tc-dorfen.pdf"
```
