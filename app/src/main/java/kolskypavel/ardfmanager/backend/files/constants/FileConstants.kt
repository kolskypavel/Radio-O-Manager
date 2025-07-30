package kolskypavel.ardfmanager.backend.files.constants

object FileConstants {
    const val OCM_START_CSV_COLUMNS = 3
    const val CATEGORY_CSV_COLUMNS = 11

    const val TEMPLATE_TEXT_RESULTS = "templates/textResultsTemplate.tmpl"
    const val TEMPLATE_TEXT_CATEGORY = "templates/textResCatHeader.tmpl"
    const val TEMPLATE_TEXT_COMPETITOR = "templates/textResCompSimple.tmpl"
    const val TEMPLATE_TEXT_COMPETITOR_SPLITS = "templates/textResCompSplits.tmpl"
    const val TEMPLATE_HTML_RESULTS = "templates/htmlResultsTemplate.tmpl"
    const val TEMPLATE_HTML_CATEGORY = "templates/htmlResCatHeader.tmpl"
    const val TEMPLATE_HTML_COMPETITOR = "templates/htmlResComp.tmpl"
    const val TEMPLATE_HTML_SPLIT = "templates/htmlResSplit.tmpl"

    const val KEY_TAB = "{{TAB}}"

    //TEMPLATE RACE KEYS
    const val KEY_TITLE_RESULTS = "{{title_results}}"
    const val KEY_TITLE_RACE_NAME = "{{title_race_name}}"
    const val KEY_TITLE_RACE_DATE = "{{title_race_date}}"
    const val KEY_TITLE_RACE_START_TIME = "{{title_race_start_time}}"
    const val KEY_TITLE_RACE_LEVEL = "{{title_race_level}}"
    const val KEY_TITLE_RACE_BAND = "{{title_race_band}}"
    const val KEY_TITLE_CATEGORY = "{{category}}"
    const val KEY_TITLE_LIMIT = "{{title_limit}}"
    const val KEY_TITLE_LENGTH = "{{title_length}}"
    const val KEY_TITLE_CONTROLS = "{{title_controls}}"

    const val KEY_RACE_NAME = "{{race_name}}"
    const val KEY_RACE_DATE = "{{race_date}}"
    const val KEY_RACE_START_TIME = "{{race_start_time}}"
    const val KEY_RACE_LEVEL = "{{race_level}}"
    const val KEY_RACE_BAND = "{{race_band}}"
    const val KEY_RACE_RESULTS = "{{race_results}}"

    const val KEY_TITLE_PLACE = "{{title_place}}"
    const val KEY_TITLE_NAME = "{{title_name}}"
    const val KEY_TITLE_INDEX = "{{title_index}}"
    const val KEY_TITLE_RUN_TIME = "{{title_run_time}}"
    const val KEY_TITLE_POINTS = "{{title_points}}"

    // TEMPLATE CATEGORY KEYS
    const val KEY_CAT_NAME = "{{cat_name}}"
    const val KEY_CAT_LIMIT = "{{cat_limit}}"
    const val KEY_CAT_LENGTH = "{{cat_length}}"
    const val KEY_CAT_CONTROLS = "{{cat_controls}}"

    // TEMPLATE RESULT KEYS
    const val KEY_COMP_PLACE = "{{comp_place}}"
    const val KEY_COMP_NAME = "{{comp_name}}"
    const val KEY_COMP_INDEX = "{{comp_index}}"
    const val KEY_COMP_RUN_TIME = "{{comp_run_time}}"
    const val KEY_COMP_POINTS = "{{comp_points}}"
    const val KEY_COMP_CONTROLS = "{{comp_controls}}"
    const val KEY_COMP_SPLITS = "{{comp_splits}}"

    const val KEY_COMP_SPLITS_CODES = "{{comp_splits_codes}}"
    const val KEY_COMP_SPLITS_TIMES = "{{comp_splits_times}}"

    const val KEY_COMP_SPLIT_CODE = "{{comp_split_code}}"
    const val KEY_COMP_SPLIT_TIME = "{{comp_split_time}}"

    const val KEY_GENERATED_WITH = "{{generated_with}}"
    const val KEY_VERSION = "{{software_version}}"

    const val KEY_TITLE_RESULTS_SPLITS = "{{title_results_splits}}"
    const val KEY_RACE_RESULTS_SPLITS = "{{race_results_splits}}"

    // HTML CONSTANTS
    const val HTML_TABLE_START = "<table>"
    const val HTML_TABLE_END = "</table>"
    const val HTML_EMPTY_ROW = "<tr><td></td></tr>"
    const val HTML_DOUBLE_BREAK = "<br/><br/>"

    const val HTML_SPLITS_CODE = "<td class=\"split_code\">{{comp_split_code}}</td>"
    const val HTML_SPLITS_TIME = "<td class=\"split_time\">{{comp_split_time}}</td>"

    const val HTML_MAX_SPLIT_COLUMNS = 10
}