var gulp = require('gulp');
var browserify = require('browserify');
var reactify = require('reactify');
var source = require('vinyl-source-stream');

var paths = {
    src: {
        js: './src/main/resources/app/_js'
    },
    dist: {
        js: './src/main/resources/app/js'
    }
};

gulp.task('browserify', function () {
    browserify(paths.src.js + '/index.js')
        .transform(reactify)
        .bundle()
        .pipe(source('bundle.js'))
        .pipe(gulp.dest(paths.dist.js));
});

gulp.task('default', ['browserify']);
