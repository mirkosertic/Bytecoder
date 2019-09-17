# Optimizer effectiveness

Step                       |JBox2D JS Demo  |JBox2D Wasm Demo
---------------------------|----------------|--------------------
Unoptimized                |2.051.489 bytes |1.226.186 bytes
Linear Register Allocation |1.582.659 bytes |1.081.372 bytes
Constant Inlining          |1.487.327 bytes |1.010.403 bytes

Reference: 2019-08-30 build:

Testcase          |Total     
------------------|----------------
JBox2D JS Demo    |1.080.438 bytes 
JBox2D Wasm Demo  |  772.196 bytes    
