/* eslint-disable no-var */
var path = require('path')

var baseConfig = require('./webpack.config')

var config = {}
Object.keys(baseConfig).forEach(function(key) {
    config[key] = baseConfig[key]
})

config.entry = {
    test: [path.join(__dirname, 'webpack.test.bootstrap.js')]
}

config.output = {
    path: path.join(__dirname, './build'),
    filename: '[name].js'
}


module.exports = config
