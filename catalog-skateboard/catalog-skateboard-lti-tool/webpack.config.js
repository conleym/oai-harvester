/* eslint no-var: [0] */
var path = require('path')
var webpack = require('webpack')
var ExtractTextPlugin = require("extract-text-webpack-plugin")
var extractText = new ExtractTextPlugin("catalog_search.css")

var devtool = 'source-map'
var env = {
    NODE_ENV: process.env.NODE_ENV
}
for (var key in env) {
    var value = (process.env[key] !== undefined ? process.env[key] : env[key])
    // Environment variables always come in as strings, so they may have to be
    // converted
    if (value === 'true') {
        value = true
    } else if (value === 'false') {
        value = false
    } else if (!isNaN(parseInt(value, 10))) {
        value = parseInt(value, 10)
    }

    env[key] = JSON.stringify(value)
}

var css = '[path][name]---[local]---[hash:base64:5]'
var optionalPlugins = []
if (process.env.NODE_ENV === 'production') {
    css = '[hash:base64]'
    devtool = 'cheap-source-map'
    optionalPlugins.push(
        new webpack.optimize.UglifyJsPlugin({
            sourceMap: false,
            compress: { warnings: false }
        })
    )
}


module.exports = {
    context: path.join(__dirname, 'src', 'main', 'js'),
    devtool: devtool,
    entry: {
        catalog_search: "./main.js",
    },
    module: {
        loaders: [
            {
                test: /\.css$/,
                loader: extractText.extract(
                    'css-loader?localIdentName=' + css
                )
            },
            {
                test   : /\.scss$/,
                loader: extractText.extract([
                    'css-loader?sourceMap&localIdentName=' + css,
                    'resolve-url',
                    'sass?sourceMap'
                ])
            },
            {
                test: /\.jsx?$/,
                exclude: /(node_modules|bower_components)/,
                loader: 'babel',
                query: {
                    cacheDirectory: true,
                    optional: [
                        'runtime',
                        // This is currently a Stage 1 proposal. It might change
                        // going forward, but it seems simple enough to be to be
                        // worth the risk.
                        //
                        // I'm adding it to support "static something = ..." in
                        // class definitions.
                        'es7.classProperties',
                        // This is currently a Stage 1 proposal. It might change
                        // going forward, but it seems obvious enough and has a
                        // parallel with arrays that I think it's worth the
                        // risk.
                        //
                        // var { foo, bar, ...baz } = obj
                        // foo = obj.foo
                        // bar = obj.bar
                        // baz = _.omit(obj, 'foo', 'bar')
                        'es7.objectRestSpread',
                    ]
                }
            }
        ]
    },
    plugins: [
        new webpack.DefinePlugin({
            'process.env': env
        }),
        new webpack.NoErrorsPlugin(),
        extractText
    ].concat(optionalPlugins),
    output: {
        path: path.join(__dirname, 'target', 'classes', 'skin', 'resources'),
        filename: '[name].js',
        sourceMapFilename: "[hash].[file].map",
        chunkFilename: '[hash].[id].engage.js',
    },
    devServer: {
        // contentBase: 'dist/release/',
        port: 9595,
        inline: true,
    },
}
