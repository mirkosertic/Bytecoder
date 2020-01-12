target triple = "wasm32-unknown-unknown"

@global_name = global i32 2047

define void @internal() "export_name(abc)" {
    ret void
}

define i32 @_start() {
	  ret i32 42
}
