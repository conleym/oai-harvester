function setupSaucieReporter(tape) {
    // Force tape to create the harness now with the default stream
    tape.getHarness()

    let log = ''
    tape.createStream().on('data', function(line) {
        log += line
    })

    let tests = 0
    let fail = 0
    let pass = 0
    tape.createStream({ objectMode: true }).on('data', function (row) {
        if (row.ok != null) {
            tests++
            if (row.ok) {
                pass++
            } else {
                fail++
            }
        }
    })

    return function() {
        return {
            passed: ( fail == 0 ),
            "custom-data": {
                log
            }
        }
    }
}

// I think this should be run as early as possible.
global.JSTestingReporterSL = setupSaucieReporter(require('tape'))

const context = require.context("./src", true, /__test__\/\S+\.jsx?$/)
context.keys().forEach(context)
