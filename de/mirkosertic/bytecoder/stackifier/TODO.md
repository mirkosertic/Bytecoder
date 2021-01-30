# TODOs

* Write more tests
* Better IF/THEN/ELSE handling
* Fix irreducible control flows
* What to do with Head-To-Head crossings? 

# Measures

What|Relooper|Stackifier|Stackifier is 
JBox2D JS file size|1.383.294 Bytes|1.096.279 Bytes|~ 20 % smaller
JBox2D Wasm file size|784.341 Bytes|754.392|~ 4 % smaller

What|50%|75%|95%|98%|99% 
JBox2D JS relooper|7 ms|9ms|13ms|17ms|17.01ms
JBox2D Wasm relooper|5ms|6ms|10ms|14ms|17.01ms

JBox2D JS stackifier|6ms|7ms|11.05ms|15.02ms|17ms
JBox2D Wasm stackifier|5ms|6ms|10ms|14ms|15.01ms