Setup
======
50M records, running with profiling
results for parallelism 8!

Case1
=====
Condition: `#input.field2 % 1000000 == 0` - interpreted
Variables: `GEO, NUMERIC, DATE`
Listeners: `Logging`
Time: 93393

Case2
=====
Condition: `#input.field2() % 1000000 == 0L` - compiled
Variables: `GEO, NUMERIC, DATE`
Listeners: `Logging`
Time: 84539

Case3
=====
Condition: `#input.field2() % 1000000 == 0L` - compiled
Variables: `None`
Listeners: `None`
Time: 50324

Case3
=====
Condition: `#input.field2() % 1000000 == 0L` - compiled
Variables: Without meta + optimization
Listeners: `None`
Histogram: ExponentiallyDecayingReservoir!!
Time: 31051