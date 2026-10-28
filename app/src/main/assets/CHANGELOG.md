## [0.3.1] - 2026-10-28
### Added
- Distinguishing between final and temp results in the XML export
- Updated library versions to match GPlay minimal requirements

### Fixed
- Fixed incorrect handling of BEACON and SEPARATOR punches (not printing, disappearing when editing punches etc.)
- Fixed HTML result export
- Fixed automatic printing if not set
- Fixed unnecessary double printing of finish tickets when printing manually
- Fixed display of long names in readout detail
- Fixed CSV imports and exports

## [0.3.0] - 2026-09-06
### Added
- Added startlist drawing options
- Added startlist and final results upload for ROBis result service
- Added possibility of extra space on top of finish ticket print
- Added bulk moving of competitors from one category to another
- Added orange icon for result service with error
- Updated targetSdk version to 36 (Android 16)

### Fixed
- Fixed incorrect start time overwriting with drawn start times
- Fixed the category length to meters
- Fixed translation of category header in results
- Races sort descending in main menu
- Added missing beacon to the list of aliases
- Added missing SEPARATOR and BEACON control types to JSON export
- Modified startlist export to match ROBis format

### Removed
- Removed unused week field on punch edits (not used by SI)

## [0.2.0] - 2026-05-17
### Added
- Added check time to readout detail

### Fixed
- Fixed readout sorting
- Fixed ROBis live and final results
- Bugfixes + library updates

## [0.1.1] - 2026-03-04
### Added
- Added import from online providers (only ROBIS now)
- Simplified race creation by moving all options under one expandable button

### Fixed
- Fixed input of category length and climb - in meters
- Fixed back button on main screen
- Fixed multiple JSON schema related issues

## [0.1.0] - 2025-23-11
### Added
- Changed name to Radio-O Manager
- Added SHORT and OTHER to race options 
- Added edit dialog to race data import
- Added duplicate SI card read options (ignore/overwrite/create new)
- Added delay between printing two finish tickets
- Added XML import of categories and export of results
- Added OResults and OFeed to result export services, delay can now be configured
- Added sounds after readout for error and rented SI cards
- Added SI rent overview to competitor table

### Removed
- Removed automatic category assignment
- Removed stop on invalid from data import options

### Fixed
- Fixed freezing when printing
- Fixed readout notification
- Moved result recalculation to menu to prevent freezing
- Adjusted export to new version of ARDF JSON

## [0.0.3] - 2025-09-29
### Added
- Added JSON export to results
- Added swipe up action to recalculate results
- Added Race data import validation
- Control aliases can now be 6 letters long and contain a slash (/)
- Added multiple tests

### Fixed
- Fixed calculation of run times and time to limit
- Fixed display of data among races
- Fixed SI5 time calculation with drawn times
- Fixed sorting to be locale based
- Stability improvements

## [0.0.2] - 2025-09-06
### Added
- Added double result ticket printing option
- Added crashlytics reports

### Fixed
- Fixed alias printing and printer setup
- Fixed start time loading from startlist
- Fixed wrong time limit evaluation
- Fixed time input for race start time
- Added null checks in retrieving current race to prevent app crashes

## [0.0.1] - 2025-08-25
### Added
- Initial release of the app - beta version

---