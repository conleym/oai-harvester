/* eslint-disable no-var */

var SAUCE_USERNAME = process.env.SAUCE_USERNAME
var SAUCE_ACCESS_KEY = process.env.SAUCE_ACCESS_KEY
var TESTEM_PORT = process.env.TESTEM_PORT

if (TESTEM_PORT == null) {
    TESTEM_PORT = 11001
}

// SAUCE_CONNECT_DOWNLOAD_ON_INSTALL=true npm install saucie -g
// https://github.com/airportyh/testem/tree/master/examples/saucelabs
if (SAUCE_USERNAME == null || SAUCE_ACCESS_KEY == null) {
    SAUCE_USERNAME = 'Asa_Unizin'
    SAUCE_ACCESS_KEY = "08b66ac7-3e85-444f-a6bd-a9d33eefa103"
}
function saucie() {
    var args = Array.prototype.slice.call(arguments)
    return ([
        'saucie',
        '--url "http://localhost:' + TESTEM_PORT + '"',
        '--username ' + SAUCE_USERNAME,
        '--accesskey ' + SAUCE_ACCESS_KEY,
        // Making your test sharable means that it is only accessible to people
        // having valid link and it is not listed on publicly available pages on
        // saucelabs.com or indexed by search engines.

        // https://docs.saucelabs.com/reference/test-configuration/#job-visibility
        '--visibilitySL=share',
    ].concat(args)).join(' ')
}

module.exports = {
    "framework": "tap",
    "src_files": ["build/test.js"],
    "launch_in_dev": [ ],
    "launch_in_ci": [
        "SL_Chrome",
        "SL_IE_11"
    ],
    "launchers": {
        "SL_Chrome": {
            "command": saucie(),
            "protocol": "tap"
        },
        "SL_IE_11": {
            "command": saucie(
                "--browserNameSL='internet explorer'",
                "--versionSL='11'",
                "--platformSL='Windows 10'"
            ),
            "protocol": "tap"
        }
    }

}
