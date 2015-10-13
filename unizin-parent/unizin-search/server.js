/* eslint-disable no-var, no-console */

var webpack = require('webpack')
var WebpackDevServer = require('webpack-dev-server')
var config = require('./webpack.config')


// Start watching and bundling tests here
var tests = require('./webpack.test.config')
var testsCompiler = webpack(tests)

testsCompiler.watch({}, function (err) {
    if (err) console.log(err)
    //console.log('Test file bundled');
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
