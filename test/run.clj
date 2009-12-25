(set! *warn-on-reflection* true)

(use 'clj-unit.core)
(require-and-run-tests
  'ring.handler.dump-test
  'ring.middleware.lint-test
  'ring.middleware.file-test
  'ring.middleware.file-info-test
  'ring.middleware.static-test
  'ring.middleware.reload-test
  'ring.middleware.stacktrace-test
  'ring.middleware.params-test)
