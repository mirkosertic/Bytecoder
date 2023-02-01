(module
  (type $vt_resolver (func (param i32) (result funcref)))

  (type $runtimetype_s (struct (field $vt_resolver (ref $vt_resolver))))

  (type $lava$lang$Object_s (struct (field $runtimetype (ref $runtimetype_s))))

  (type $java$lang$String_s (struct_subtype (field $runtimetype (ref $runtimetype_s)) (field mut $value i32) $lava$lang$Object_s))

  (global $$lava$lang$Object_rt (ref $runtimetype_s)
    (struct.new $runtimetype_s (ref.func $lava$lang$Object_vt))
  )

  (global $$lava$lang$String_rt (ref $runtimetype_s)
    (struct.new $runtimetype_s (ref.func $lava$lang$Object_vt))
  )

  (func $lava$lang$Object_vt (param $mid i32) (result funcref)
     (return (ref.func $lava$lang$Object_vt))
  )

  (func $new_string (result (ref $java$lang$String_s))
    (struct.new $java$lang$String_s
        (global.get $$lava$lang$String_rt)
        (i32.const 10)
    )
  )

  (func $new_object (result (ref $lava$lang$Object_s))
    (struct.new $lava$lang$Object_s
        (global.get $$lava$lang$Object_rt)
    )
  )
)