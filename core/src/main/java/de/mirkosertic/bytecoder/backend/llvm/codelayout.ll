target triple = "wasm32-unknown-unknown"

attributes #2 = { "wasm-export-name"="globaldata" }
@global_name = global i32 2047 #2

attributes #0 = { "wasm-export-name"="abc" }
define void @internal() #0 {
    ret void
}

attributes #1 = { "wasm-import-name"="def" "wasm-import-module"="mod" }
declare void @external() #1

define void()* @funcptr() {
    ret void()* @internal ; returns index in table of @internal
}

define i32 @_start() {
entry:
    %x = call void()* @funcptr();
    call void() %x()  ; this generates a call_indirect in webassembly
    call void() @external()
	ret i32 42
}
