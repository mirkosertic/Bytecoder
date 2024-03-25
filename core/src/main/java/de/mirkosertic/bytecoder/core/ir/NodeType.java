/*
 * Copyright 2022 Mirko Sertic
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.mirkosertic.bytecoder.core.ir;

public enum NodeType {
    Goto, Unwind, FrameDebugInfo, MethodInvocation, TableSwitch, ReturnValue, ClassInitialization, SetInstanceField, LookupSwitch, SetClassField, Copy, Return, MonitorExit, Region, TryCatch, LineNumberDebugInfo, ArrayStore, Nop, If, USHR, Variable, PHI, PrimitiveInt, PrimitiveFloat, MethodReference, FieldReference, MethodType, PrimitiveLong, PrimitiveDouble, PrimitiveShort, Cast, MethodArgument, Rem, PrimitiveClassReference, RuntimeClass, Add, NullTest, ReferenceTest, NumericalTest, New, CaughtException, BootstrapMethod, SHL, ReadClassField, This, MethodInvocationExpression, NullReference, Mul, TypeReference, CMP, RuntimeClassOf, XOr, ObjectString, InvokeDynamicExpression, SHR, InstanceOf, TypeConversion, Sub, ResolveCallsite, ArrayLength, NewArray, Reinterpret, And, EnumValuesOf, Or, ArrayLoad, ReadInstanceField, Div, Neg, MonitorEnter
}
