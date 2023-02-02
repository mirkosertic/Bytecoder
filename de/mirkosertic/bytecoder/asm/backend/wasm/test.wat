(module $bytecoder
    (type $t0 (func (param i32) (param f32) (result i32)))
    (type $t1 (struct (field $a i32)))
    (type $t2 (struct_subtype (field $a i32) (field $b f32) $t1))
    )
