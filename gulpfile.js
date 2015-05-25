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

gulp.task('jsx', function () {
    browserify(paths.src.js + '/app.js')
        .transform(reactify)
        .bundle()
        .pipe(source('bundle.js'))
        .pipe(gulp.dest(paths.dist.js));
});

gulp.task('watch', ['jsx'], function () {
    gulp.watch(paths.src.js + '/**/*.js', ['jsx']);
});

gulp.task('default', ['watch']);
