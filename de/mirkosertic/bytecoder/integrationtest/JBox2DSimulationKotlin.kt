/*
 * Copyright 2018 Mirko Sertic
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
package de.mirkosertic.bytecoder.integrationtest

import de.mirkosertic.bytecoder.api.Export
import de.mirkosertic.bytecoder.api.Import
import de.mirkosertic.bytecoder.api.web.AnimationFrameCallback
import de.mirkosertic.bytecoder.api.web.CanvasRenderingContext2D
import de.mirkosertic.bytecoder.api.web.EventListener
import de.mirkosertic.bytecoder.api.web.ClickEvent
import de.mirkosertic.bytecoder.api.web.HTMLButton
import de.mirkosertic.bytecoder.api.web.HTMLCanvasElement
import de.mirkosertic.bytecoder.api.web.Window
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.collision.shapes.ShapeType
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.*
import org.jbox2d.dynamics.joints.RevoluteJointDef

object JBox2DSimulationKotlin {

    private var scene: Scene? = null
    private var renderingContext2D: CanvasRenderingContext2D? = null
    private var animationCallback: AnimationFrameCallback? = null
    private var window: Window? = null

    class Scene {
        val world: World
        private var axis: Body? = null
        private var reel: Body? = null
        private var lastCalculated: Long = 0
        private val startTime: Long

        init {
            world = World(Vec2(0f, -9.8f))
            initAxis()
            initReel()
            joinReelToAxis()
            initBalls()
            lastCalculated = System.currentTimeMillis()
            startTime = lastCalculated
        }

        private fun initAxis() {
            val axisDef = BodyDef()
            axisDef.type = BodyType.STATIC
            axisDef.position = Vec2(3f, 3f)
            axis = world.createBody(axisDef)

            val axisShape = CircleShape()
            axisShape.radius = 0.02f
            axisShape.m_p.set(0f, 0f)

            val axisFixture = FixtureDef()
            axisFixture.shape = axisShape
            axis!!.createFixture(axisFixture)
        }

        private fun initReel() {
            val reelDef = BodyDef()
            reelDef.type = BodyType.DYNAMIC
            reelDef.position = Vec2(3f, 3f)
            reel = world.createBody(reelDef)

            val fixture = FixtureDef()
            fixture.friction = 0.5f
            fixture.restitution = 0.4f
            fixture.density = 1f

            val parts = 30
            for (i in 0 until parts) {
                val shape = PolygonShape()
                val angle1 = i / parts.toDouble() * 2.0 * Math.PI
                val x1 = 2.7 * Math.cos(angle1)
                val y1 = 2.7 * Math.sin(angle1)
                val angle2 = (i + 1) / parts.toDouble() * 2.0 * Math.PI
                val x2 = 2.7 * Math.cos(angle2)
                val y2 = 2.7 * Math.sin(angle2)
                val angle = (angle1 + angle2) / 2
                val x = 0.01 * Math.cos(angle)
                val y = 0.01 * Math.sin(angle)

                shape.set(arrayOf(Vec2(x1.toFloat(), y1.toFloat()), Vec2(x2.toFloat(), y2.toFloat()), Vec2((x2 - x).toFloat(), (y2 - y).toFloat()), Vec2((x1 - x).toFloat(), (y1 - y).toFloat())), 4)
                fixture.shape = shape
                reel!!.createFixture(fixture)
            }
        }

        private fun initBalls() {
            val ballRadius = 0.15f

            val ballDef = BodyDef()
            ballDef.type = BodyType.DYNAMIC
            val fixtureDef = FixtureDef()
            fixtureDef.friction = 0.3f
            fixtureDef.restitution = 0.3f
            fixtureDef.density = 0.2f
            val shape = CircleShape()
            shape.m_radius = ballRadius
            fixtureDef.shape = shape

            for (i in 0..5) {
                for (j in 0..5) {
                    val x = (j + 0.5f) * (ballRadius * 2 + 0.01f)
                    val y = (i + 0.5f) * (ballRadius * 2 + 0.01f)
                    ballDef.position.x = 3 + x
                    ballDef.position.y = 3 + y
                    var body = world.createBody(ballDef)
                    body.createFixture(fixtureDef)

                    ballDef.position.x = 3 - x
                    ballDef.position.y = 3 + y
                    body = world.createBody(ballDef)
                    body.createFixture(fixtureDef)

                    ballDef.position.x = 3 + x
                    ballDef.position.y = 3 - y
                    body = world.createBody(ballDef)
                    body.createFixture(fixtureDef)

                    ballDef.position.x = 3 - x
                    ballDef.position.y = 3 - y
                    body = world.createBody(ballDef)
                    body.createFixture(fixtureDef)
                }
            }
        }

        private fun joinReelToAxis() {
            val jointDef = RevoluteJointDef()
            jointDef.bodyA = axis
            jointDef.bodyB = reel
            world.createJoint(jointDef)
        }

        fun calculate() {
            val currentTime = System.currentTimeMillis()
            var timeToCalculate = (currentTime - lastCalculated).toInt()
            val relativeTime = currentTime - startTime
            while (timeToCalculate > 10) {
                val period = ((relativeTime + 5000) / 10000).toInt()
                reel!!.applyTorque(if (period % 2 == 0) 8f else -8f)
                world.step(0.01f, 20, 40)
                lastCalculated += 10
                timeToCalculate -= 10
            }
            lastCalculated = System.currentTimeMillis()
        }
    }

    @Export("main")
    @JvmStatic
    fun main(args: Array<String>?) {
        scene = Scene()
        window = Window.window()
        val document = window!!.document()
        val theCanvas = document.getElementById<HTMLCanvasElement>("benchmark-canvas")
        renderingContext2D = theCanvas.getContext("2d")

        animationCallback = AnimationFrameCallback {
            val theStart = System.currentTimeMillis()

            statsBegin()

            scene!!.calculate()

            render()

            statsEnd()

            val theDuration = (System.currentTimeMillis() - theStart).toInt()
            logRuntime(theDuration)

            window!!.requestAnimationFrame(animationCallback)
        }

        val button = document.getElementById<HTMLButton>("button")
        button.addEventListener("click", EventListener<ClickEvent> {
            button.disabled(true)
            window!!.requestAnimationFrame(animationCallback)
        })
    }

    @Import(module = "debug", name = "logRuntime")
    @JvmStatic
    external fun logRuntime(aValue: Int)

    @Import(module = "stats", name = "begin")
    @JvmStatic
    external fun statsBegin()

    @Import(module = "stats", name = "end")
    @JvmStatic
    external fun statsEnd()

    private fun render() {
        renderingContext2D!!.setFillStyle("white")
        renderingContext2D!!.setStrokeStyle("black")
        renderingContext2D!!.fillRect(0f, 0f, 600f, 600f)
        renderingContext2D!!.save()
        renderingContext2D!!.translate(0f, 600f)
        renderingContext2D!!.scale(1f, -1f)
        renderingContext2D!!.scale(100f, 100f)
        renderingContext2D!!.setLineWidth(0.01f)
        var body: Body? = scene!!.world.bodyList
        while (body != null) {
            val center = body.position
            renderingContext2D!!.save()
            renderingContext2D!!.translate(center.x, center.y)
            renderingContext2D!!.rotate(body.angle)
            var fixture: Fixture? = body.fixtureList
            while (fixture != null) {
                val shape = fixture.shape
                if (shape.type == ShapeType.CIRCLE) {
                    val circle = shape as CircleShape
                    renderingContext2D!!.beginPath()
                    renderingContext2D!!.arc(circle.m_p.x.toDouble(), circle.m_p.y.toDouble(), circle.radius.toDouble(), 0.0, Math.PI * 2, true)
                    renderingContext2D!!.closePath()
                    renderingContext2D!!.stroke()
                } else if (shape.type == ShapeType.POLYGON) {
                    val poly = shape as PolygonShape
                    val vertices = poly.vertices
                    renderingContext2D!!.beginPath()
                    renderingContext2D!!.moveTo(vertices[0].x, vertices[0].y)
                    for (i in 1 until poly.vertexCount) {
                        renderingContext2D!!.lineTo(vertices[i].x, vertices[i].y)
                    }
                    renderingContext2D!!.closePath()
                    renderingContext2D!!.stroke()
                }
                fixture = fixture.next
            }
            renderingContext2D!!.restore()
            body = body.next
        }
        renderingContext2D!!.restore()
    }
}
