package de.mirkosertic.bytecoder.api.web.webgl;

import de.mirkosertic.bytecoder.api.OpaqueMethod;
import de.mirkosertic.bytecoder.api.OpaqueReferenceType;
import de.mirkosertic.bytecoder.api.web.FloatArray;
import de.mirkosertic.bytecoder.api.web.HTMLCanvasElement;
import de.mirkosertic.bytecoder.api.web.HTMLImageElement;
import de.mirkosertic.bytecoder.api.web.Int16Array;
import de.mirkosertic.bytecoder.api.web.Int8Array;
import de.mirkosertic.bytecoder.api.web.IntArray;

public interface WebGLRenderingContext extends OpaqueReferenceType {

    void clear(int mask);

    void clearDepth(float depth);

    void clearColor(float red, float blue, float green, float alpha);

    void uniform3i(WebGLUniformLocation location, int x, int y, int z);

    void lineWidth(float width);

    void deleteShader(WebGLShader shader);

    void detachShader(WebGLProgram program, WebGLShader shader);

    void vertexAttrib3f(int index, float x, float y, float z);

    void compileShader(WebGLShader shader);

    void texParameterfv(int target, int pname, FloatArray params);

    void stencilFunc(int func, int ref, int mask); 

    void deleteFramebuffer(WebGLFrameBuffer framebuffer);

    WebGLTexture createTexture();

    void bindAttribLocation(WebGLProgram program, int index, String name);

    void enableVertexAttribArray(int index); 

    void releaseShaderCompiler(); 

    void uniform2f(WebGLUniformLocation location, float x, float y);

    WebGLActiveInfo getActiveAttrib(WebGLProgram program, int index);

    WebGLActiveInfo getActiveUniform(WebGLProgram program , int index);

    String getActiveAttrib(WebGLProgram program , int index, IntArray size, IntArray type);

    WebGLFrameBuffer createFramebuffer();

    void uniformMatrix2fv(WebGLUniformLocation location, boolean transpose, FloatArray value);

    void uniformMatrix2fv(WebGLUniformLocation location, int count, boolean transpose, FloatArray value, int offset);

    void uniform2fv(WebGLUniformLocation location, FloatArray v);

    void uniform4iv(WebGLUniformLocation location, IntArray v);

    void colorMask(boolean red, boolean green, boolean blue, boolean alpha);

    void polygonOffset(float factor, float units);

    void viewport(int x, int y, int width, int height);

    void getProgramiv(WebGLProgram program , int pname, IntArray params);

    void getBooleanv(int pname, Int8Array params);

    void getBufferParameteriv(int target, int pname, IntArray params);

    void deleteTexture(WebGLTexture texture);

    void getVertexAttribiv(int index, int pname, IntArray params);

    void vertexAttrib4fv(int index, FloatArray values);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, Int8Array pixels);

    void texSubImage2D(int target, int level, int xoffset, int yoffset, int format, int type, HTMLCanvasElement htmlCanvasElement);

    void deleteRenderbuffers(int n, IntArray renderbuffers);

    void getTexParameteriv(int target, int pname, IntArray params);

    void genTextures(int n, IntArray textures);

    void stencilOpSeparate(int face, int fail, int zfail, int zpass);

    void uniform2i(WebGLUniformLocation location, int x, int y);

    int checkFramebufferStatus(int target);

    void deleteTextures(int n, IntArray textures);

    void bindRenderbuffer(int target, WebGLRenderBuffer renderbuffer);

    void texParameteriv(int target, int pname, IntArray params);

    void vertexAttrib4f(int index, float x, float y, float z, float w);

    void deleteBuffers(int n, IntArray buffers);

    String getProgramInfoLog(WebGLProgram program);

    boolean isRenderbuffer(WebGLRenderBuffer renderbuffer);

    void frontFace(int mode);

    void uniform1iv(WebGLUniformLocation location, int count, IntArray v);

    void uniform1iv(WebGLUniformLocation location, IntArray v);

    void uniform1iv(WebGLUniformLocation location, int count, IntArray v, int offset);

    void bindTexture(int target, WebGLTexture texture);

    WebGLUniformLocation getUniformLocation(WebGLProgram program , String name);

    void pixelStorei(int pname, int param);

    void hint(int target, int mode);

    void framebufferRenderbuffer(int target, int attachment, int renderbuffertarget, WebGLRenderBuffer renderbuffer);

    void uniform1f(WebGLUniformLocation location, float x);

    void depthMask(boolean flag);

    void blendColor(float red, float green, float blue, float alpha);

    void uniformMatrix4fv(WebGLUniformLocation location, boolean transpose, FloatArray value);

    void bufferData(int target, FloatArray data, int usage);

    void bufferData(int target, Int16Array data, int usage);

    void validateProgram(WebGLProgram program);

    void texParameterf(int target, int pname, float param);

    boolean isFramebuffer(WebGLFrameBuffer framebuffer);

    void deleteBuffer(WebGLBuffer buffer);

    void shaderSource(WebGLShader shader, String sourcecode);

    void vertexAttrib2fv(int index, FloatArray values);

    void deleteFramebuffers(int n, IntArray framebuffers);

    void uniform4fv(WebGLUniformLocation location, FloatArray v);

    void compressedTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int imageSize, Int8Array data);

    void generateMipmap(int target);

    void deleteProgram(WebGLProgram program);

    void framebufferTexture2D(int target, int attachment, int textarget, WebGLTexture texture, int level);

    WebGLRenderBuffer createRenderbuffer();

    void attachShader(WebGLProgram program, WebGLShader shader);

    void bindBuffer(int target, WebGLBuffer buffer);

    void shaderBinary(int n, IntArray shaders, int binaryformat, Int8Array binary, int length);

    void disable(int cap);

    void getRenderbufferParameteriv(int target, int pname, IntArray params);

    String getShaderInfoLog(WebGLShader shader);

    String getActiveUniform(WebGLProgram program , int index, IntArray size, IntArray type);

    boolean isShader(int shader);

    void uniform1i(WebGLUniformLocation location, int x);

    void blendEquationSeparate(int modeRGB, int modeAlpha);

    void scissor(int x, int y, int width, int height);

    WebGLProgram createProgram();

    void uniformMatrix3fv(WebGLUniformLocation location, boolean transpose, FloatArray value);

    void getTexParameterfv(int target, int pname, FloatArray params);

    void getTexParameter(int target, int pname);

    void vertexAttrib1f(int index, float x);

    void uniform1fv(WebGLUniformLocation location, FloatArray v);

    void uniform3iv(WebGLUniformLocation location, IntArray v);

    void texImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, Int8Array pixels);

    void texImage2D(int target, int level, int internalformat, int format, int type, HTMLCanvasElement canvas);

    void texImage2D(int target, int level, int internalformat, int format, int type, HTMLImageElement image);

    void vertexAttrib3fv(int index, FloatArray values);

    void blendFunc(int sfactor, int dfactor);

    boolean isEnabled(int cap);

    int getAttribLocation(WebGLProgram program , String name);

    void depthRangef(float zNear, float zFar);

    void flush();

    void sampleCoverage(float value, boolean invert);

    void copyTexSubImage2D(int target, int level, int xoffset, int yoffset, int x, int y, int width, int height);

    void getShaderiv(WebGLShader shader, int pname, IntArray params);

    void getUniformfv(WebGLProgram program , WebGLUniformLocation location, FloatArray params);

    void uniform4f(WebGLUniformLocation location, float x, float y, float z, float w);

    void depthFunc(int func);

    boolean isBuffer(WebGLBuffer buffer);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, Int8Array ptr);

    void vertexAttribPointer(int index, int size, int type, boolean normalized, int stride, int ptr);

    void stencilMaskSeparate(int face, int mask);

    void drawElements(int mode, int count, int type, Int8Array indices);

    void drawElements(int mode, int count, int type, int indices);

    void texParameteri(int target, int pname, int param); 

    void useProgram(WebGLProgram program);

    void finish();

    void getIntegerv(int pname, IntArray params);

    void blendEquation(int mode); 

    void uniform4i(WebGLUniformLocation location, int x, int y, int z, int w); 

    void vertexAttrib1fv(int index, FloatArray values);

    void uniform3fv(WebGLUniformLocation location, FloatArray v);

    void vertexAttrib2f(int index, float x, float y); 

    void activeTexture(int texture);

    void cullFace(int mode); 

    void clearStencil(int s); 

    void getFloatv(int pname, FloatArray params);

    void drawArrays(int mode, int first, int count); 

    void bindFramebuffer(int target, WebGLFrameBuffer framebuffer);

    int getError();

    void bufferSubData(int target, int offset, int size, Int8Array data);

    void bufferSubData(int target, int offset, FloatArray data);

    void bufferSubData(int target, int offset, Int16Array data);

    void copyTexImage2D(int target, int level, int internalformat, int x, int y, int width, int height, int border);

    Boolean isProgram(WebGLProgram program );

    void stencilOp(int fail, int zfail, int zpass);

    void disableVertexAttribArray(int index);

    void genBuffers(int n, IntArray buffers);

    void getAttachedShaders(WebGLProgram program, int maxcount, Int8Array count, IntArray shaders);

    void genRenderbuffers(int n, IntArray renderbuffers);

    void renderbufferStorage(int target, int internalformat, int width, int height);

    void uniform3f(WebGLUniformLocation location, float x, float y, float z);

    void readPixels(int x, int y, int width, int height, int format, int type, Int8Array pixels);

    void stencilMask(int mask);

    void blendFuncSeparate(int srcRGB, int dstRGB, int srcAlpha, int dstAlpha);

    void getShaderPrecisionFormat(int shadertype, int precisiontype, IntArray range, IntArray precision);

    boolean isTexture(WebGLTexture texture);

    void getVertexAttribfv(int index, int pname, FloatArray params);

    void getVertexAttribPointerv(int index, int pname, Int8Array pointer);

    WebGLShader createShader(int type);

    void stencilFuncSeparate(int face, int func, int ref, int mask);

    String getString(int name);

    void compressedTexImage2D(int target, int level, int internalformat, int width, int height, int border, int imageSize, Int8Array data);

    void uniform2iv(WebGLUniformLocation location, IntArray v);

    WebGLBuffer createBuffer();

    void enable(int cap);

    void getUniformiv(WebGLProgram program , WebGLUniformLocation location, IntArray params);

    void getFramebufferAttachmentParameteriv(int target, int attachment, int pname, IntArray params);

    void deleteRenderbuffer(WebGLRenderBuffer renderbuffer);

    void genFramebuffers(int n, IntArray framebuffers);

    void linkProgram(WebGLProgram program);

    void getParameter(int pname);

    int getShaderParameteri(WebGLShader shader, int pname);

    @OpaqueMethod("getParameter")
    float getParameterf(int pname);

    @OpaqueMethod("getParameter")
    int getParameteri(int pname);

    @OpaqueMethod("getParameter")
    boolean getParameterb(int pname);

    @OpaqueMethod("getParameter")
    String getParameterString(int pname); 

    @OpaqueMethod("getShaderParameter")
    boolean getShaderParameterBoolean(WebGLShader shader, int pname);

    @OpaqueMethod("getShaderParameter")
    int getShaderParameterInt(WebGLShader shader, int pname);

    @OpaqueMethod("getProgramParameter")
    boolean getProgramParameterBoolean(WebGLProgram program , int pname);

    @OpaqueMethod("getProgramParameter")
    int getProgramParameterInt(WebGLProgram program , int pname);
}

