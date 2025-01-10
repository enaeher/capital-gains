# Capital-Gains Exercise

## How to run the code

Run this program with `clj -X capital-gains/run`, e.g.:

```
$ clj -X capital-gains/run < test/resources/multiline-input.json
[{"tax":0},{"tax":0},{"tax":0}]
[{"tax":0},{"tax":10000},{"tax":0}]
```

## How to run the tests

Run the tests with `clj -X:test`, e.g.:

```
$ clj -X:test

Running tests in #{"test"}

Testing capital-gains-test

Ran 2 tests containing 10 assertions.
0 failures, 0 errors.
```

## Implementation notes

### Numbers and math

The requirements state:

> The program should round to the nearest hundredth values (two
> decimal places) when dealing with decimal numbers.

Floating-point numbers (including Java doubles) cannot represent many
monetary values precisely, and errors can be introduced when
performing math using these numbers, so I've chosen to convert the
input unit costs to integer numbers of cents. I then perform explicit
rounding (to the nearest cent) after the two operations which may
result in non-integer numbers of cents:

  - calculating a new weighted average
  - calculating the tax due as a ratio of the profit

After processing, I divide by 100 to convert cents back into dollars
for the JSON output.

Note: the output examples are not consistent in the way numbers are
formatted. In some examples all monetary amounts are printed to two
decimal places, even when the decimal values are zero, e.g. in Case 1:

```json
[{"tax": 0.00},{"tax": 0.00},{"tax": 0.00}]
```

Other examples don't include unnecessary decimal places, e.g. in Case
9:

```json
[{"tax":0},{"tax":0},{"tax":0},{"tax":0},{"tax":0},{"tax":0},{"tax":1000},{"tax":2400}]
```

My implementation does not print unnecessary decimal digits in the
output, following the Case 9 example.

### Performance and scaling

My implementation uses `line-seq` to lazily read the input one line
(or chunk of lines) at a time, which should allow it to process input
files which are too large to fit in memory. However, it allocates a
string for each line before doing any JSON parsing; a cleverer use of
the JSON parser in a streaming mode might be able to squeeze a little
more performance by allocating fewer strings. In this implementation
I've opted for simplicity.

Because it reads an entire line at a time, my implementation relies on
each individual line being small enough to fit into memory. If I
needed to support very long lines, I'd want to read each line lazily,
one operation (or chunk of operations) at a time.

The JSON parser I chose is
[clojure.data.json](https://github.com/clojure/data.json), which
provides a nice interface, is a first-party tool from the core Clojure
team, and has no external dependencies. This library is considered
slow by some in the Clojure community; I did a few tests with
[Jsonista](https://github.com/metosin/jsonista) and did not see a
measurable performance improvement. But there may be gains possible
here with further experimentation.
