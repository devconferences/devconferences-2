var webpack = require('webpack');

module.exports = {
    output: {
        path: './src/main/resources/app/js/',
        publicPath: '/js/',
        filename: 'bundle.js',
        library: 'DevConferences',
        libraryTarget: 'umd'
    },
    entry: {
        app: ['./src/main/resources/app/_js/app.js']
    },
    resolve: {
        extensions: ['', '.js', '.jsx']
    },
    module: {
        loaders: [
            {
                test: /\.js$/,
                exclude: /node_modules/,
                loader: 'react-hot!babel-loader?stage=0&optional=runtime'
            }
        ]
    },
    plugins: [
        new webpack.HotModuleReplacementPlugin(),
        new webpack.NoErrorsPlugin()
    ]
};