# TODOs

* Write more tests
* Better IF/THEN/ELSE handling
* Fix irreducible control flows

# Measures

What|Relooper|Stackifier|Stackifier is 
JBox2D JS file size|1.381.974 Bytes|1.095.751 Bytes|~ 20 % smaller
JBox2D WASM file size|784.081 Bytes|752.281|~ 4 % smaller

What|50%|75%|95%|98%|99% 
JBox2D JS relooper|7 ms|9ms|13ms|17ms|17.01ms
JBox2D WASM relooper|5ms|6ms|10ms|14ms|17.01ms
JBox2D JS stackifier|6ms|7ms|11.05ms|15.02ms|17ms
JBox2D WASM stackifier|5ms|6ms|10ms|14ms|15.01ms