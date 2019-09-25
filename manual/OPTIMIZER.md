# Optimizer effectiveness

Step                       |JBox2D JS Demo  |JBox2D Wasm Demo
---------------------------|----------------|--------------------
Unoptimized                |2.051.489 bytes |1.226.186 bytes
Linear Register Allocation |1.582.659 bytes |1.081.372 bytes
Constant Inlining          |1.487.327 bytes |1.010.403 bytes
Inefficient If-Conditions  |1.473.286 bytes |1.001.294 bytes
Inefficient Field-Read     |1.401.204 bytes |  929.177 bytes
Inefficient Field-Write    |1.370.353 bytes |  909.742 bytes
Inefficient Invocations    |1.261.180 bytes |  857.123 bytes
Inefficient Array-Access   |1.246.460 bytes |  851.354 bytes
Inefficient Binary Expr.   |1.175.820 bytes |  818.940 bytes

Reference: 2019-08-30 build:

Testcase          |Total     
------------------|----------------
JBox2D JS Demo    |1.080.438 bytes 
JBox2D Wasm Demo  |  772.196 bytes    
