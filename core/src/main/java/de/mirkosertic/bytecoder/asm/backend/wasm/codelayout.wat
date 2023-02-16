(module
  (type $simplefunction (func (param i32) (result i32)))

  (type $vt_resolver (func (param i32) (result i32)))

  (type $runtimetype_s (struct (field $vt_resolver (ref $vt_resolver))))

  (type $java$lang$Object_s (struct (field $runtimetype (ref $runtimetype_s))))

  (type $java$lang$String_s (struct_subtype (field $runtimetype (ref $runtimetype_s)) (field $value (mut i32)) $java$lang$Object_s))

  (type $dummyextern
    (struct
        (field $nativeObject (mut externref))
    )
  )

  (func $textnull
    (drop
        (struct.new $dummyextern
            (ref.null extern)
        )
    )
  )

  (global $java$lang$Object_rt (ref $runtimetype_s)
    (struct.new $runtimetype_s (ref.func $lava$lang$Object_vt))
  )

  (global $java$lang$String_rt (ref $runtimetype_s)
    (struct.new $runtimetype_s (ref.func $lava$lang$Object_vt))
  )

  (func $lava$lang$Object_vt (param $mid i32) (result i32)
    (return (i32.const 10))
  )

  (func $new_string (result (ref $java$lang$String_s))
    (struct.new $java$lang$String_s
        (global.get $java$lang$String_rt)
        (i32.const 10)
    )
  )

  (func $new_object (result (ref $java$lang$Object_s))
    (struct.new $java$lang$Object_s
        (global.get $java$lang$Object_rt)
    )
  )

  (func $dosomething (type $simplefunction) (param $a i32) (result i32)
    (return (i32.const 200))
  )

  (table 100 funcref)
  (elem (i32.const 10) $dosomething)

  (func $setinstance (param $obj (ref $java$lang$String_s))
    (struct.set $java$lang$String_s $value
        (local.get $obj)
        (i32.const 10)
    )
  )

  (func $virtualcall (param $obj (ref $java$lang$Object_s)) (result i32)
    (return
        (call_indirect (type $simplefunction)
            (call_ref $vt_resolver
                (i32.const 42)
                (struct.get $runtimetype_s $vt_resolver
                    (ref.cast_static $runtimetype_s
                        (struct.get $java$lang$Object_s $runtimetype (local.get $obj))
                    )
                )
            )
            (i32.const 100)
        )
    )
  )

  ;; Test mit downcast https://github.com/WebAssembly/reference-types/blob/master/proposals/reference-types/Overview.md
  (type $int32array (array (mut i32)))

  (func $extractstring (param $str (ref $java$lang$Object_s)) (result i32)
    (drop (array.new_default $int32array (i32.const 10)))
    (struct.get $java$lang$String_s $value
        (ref.cast_static $java$lang$String_s
            (local.get $str)
        )
    )
  )
)
