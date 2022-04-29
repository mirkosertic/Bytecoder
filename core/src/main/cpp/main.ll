; ModuleID = 'main.c'
source_filename = "main.c"
target datalayout = "e-m:e-p:32:32-i64:64-n32:64-S128"
target triple = "wasm32-unnkown-unknown"

%class.Runtimeclass = type { i8 }

@_ZN9Testclass20__java__runtimeclassE = external local_unnamed_addr global %class.Runtimeclass*, align 4
@_ZN6Object20__java__runtimeclassE = external local_unnamed_addr global %class.Runtimeclass*, align 4

; Function Attrs: norecurse
define hidden i32 @main() local_unnamed_addr #0 personality i8* bitcast (i32 (...)* @__gxx_personality_v0 to i8*) {
  %1 = load %class.Runtimeclass*, %class.Runtimeclass** @_ZN6Object20__java__runtimeclassE, align 4, !tbaa !2
  %2 = icmp eq %class.Runtimeclass* %1, null
  br i1 %2, label %3, label %5

3:                                                ; preds = %0
  %4 = call dereferenceable(1) i8* @_Znwm(i32 1) #2
  store i8* %4, i8** bitcast (%class.Runtimeclass** @_ZN6Object20__java__runtimeclassE to i8**), align 4, !tbaa !2
  br label %5

5:                                                ; preds = %3, %0
  %6 = load %class.Runtimeclass*, %class.Runtimeclass** @_ZN9Testclass20__java__runtimeclassE, align 4, !tbaa !2
  %7 = icmp eq %class.Runtimeclass* %6, null
  br i1 %7, label %8, label %10

8:                                                ; preds = %5
  %9 = call dereferenceable(1) i8* @_Znwm(i32 1) #2
  store i8* %9, i8** bitcast (%class.Runtimeclass** @_ZN9Testclass20__java__runtimeclassE to i8**), align 4, !tbaa !2
  br label %10

10:                                               ; preds = %8, %5
  ret i32 40
}

; Function Attrs: nobuiltin nofree
declare noalias nonnull i8* @_Znwm(i32) local_unnamed_addr #1

declare i32 @__gxx_personality_v0(...)

attributes #0 = { norecurse "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="none" "less-precise-fpmad"="false" "min-legal-vector-width"="0" "no-infs-fp-math"="false" "no-jump-tables"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="generic" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #1 = { nobuiltin nofree "correctly-rounded-divide-sqrt-fp-math"="false" "disable-tail-calls"="false" "frame-pointer"="none" "less-precise-fpmad"="false" "no-infs-fp-math"="false" "no-nans-fp-math"="false" "no-signed-zeros-fp-math"="false" "no-trapping-math"="false" "stack-protector-buffer-size"="8" "target-cpu"="generic" "unsafe-fp-math"="false" "use-soft-float"="false" }
attributes #2 = { builtin }

!llvm.module.flags = !{!0}
!llvm.ident = !{!1}

!0 = !{i32 1, !"wchar_size", i32 4}
!1 = !{!"Ubuntu clang version 10.0.1-++20211003085942+ef32c611aa21-1~exp1~20211003090334.2"}
!2 = !{!3, !3, i64 0}
!3 = !{!"any pointer", !4, i64 0}
!4 = !{!"omnipotent char", !5, i64 0}
!5 = !{!"Simple C++ TBAA"}
