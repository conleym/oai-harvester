import path from 'path'
import glob from 'glob'

const testFiles = glob.sync(path.join(__dirname, '**/*-test.js?(x)'))

testFiles.map(require)
