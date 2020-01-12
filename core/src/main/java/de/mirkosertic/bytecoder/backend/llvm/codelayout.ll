target triple = "wasm32-unknown-unknown"

@global_name = global i32 2047

define void @internal() "export_name(abc)" {
    ret void
}

define void()* @funcptr() {
    ret void()* @internal ; returns index in table of @internal
}

define i32 @_start() {
    %x = call void()* @funcptr();
    call void() %x()  ; this generates a call_indirect in webassembly
	ret i32 42
}
