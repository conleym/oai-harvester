/* eslint-disable no-var, no-console */

var webpack = require('webpack')
var WebpackDevServer = require('webpack-dev-server')
var config = require('./webpack.config')


// Start watching and bundling tests here
var tests = require('./webpack.test.config')
var testsCompiler = webpack(tests)

testsCompiler.watch({}, function (err, stats) {
    if (err) console.log(err)
    if (stats.hasErrors()) {
        var jsonStats = stats.toJson()
        console.warn(jsonStats.errors.join('\n'))
    }
})


var port = config.devServer.port

// Primary app
new WebpackDevServer(webpack(config), {
    // publicPath: config.output.path,

    https: true,
    port: 9595,
    inline: true,
    // historyApiFallback: true,
    stats: {colors: true}
})
.listen(port, 'localhost', function (err) {
    if (err) {
        console.log(err)
    }

    console.log('Listening at localhost:' + port)
})
