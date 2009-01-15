(use 'clj-unit.core)
(require-and-run-tests
  'ring.builder-test
  'ring.dump-test
  'ring.lint-test
  'ring.file-test
  'ring.file-info-test
  'ring.static-test
  'ring.reload-test
  'ring.backtrace-test)
