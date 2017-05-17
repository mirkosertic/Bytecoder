/*
 * Copyright 2017 Mirko Sertic
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
package de.mirkosertic.bytecoder.core;

import de.mirkosertic.bytecoder.classlib.java.lang.TRuntimeException;
import de.mirkosertic.bytecoder.classlib.org.junit.TAssert;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.pooling.normal.DefaultWorldPool;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import de.mirkosertic.bytecoder.unittest.BytecoderUnitTestRunner;

@RunWith(BytecoderUnitTestRunner.class)
public class JBox2DTest {

    public class Scene {
        private World world;
        private Body axis;
        private Body reel;
        private long lastCalculated;
        private long startTime;

        public Scene() {
            world = new World(new Vec2(0, -9.8f));
            initAxis();
            initReel();
            joinReelToAxis();
            initBalls();
            lastCalculated = System.currentTimeMillis();
            startTime = lastCalculated;
        }

        private void initAxis() {
            BodyDef axisDef = new BodyDef();
            axisDef.type = BodyType.STATIC;
            axisDef.position = new Vec2(3, 3);
            axis = world.createBody(axisDef);

            CircleShape axisShape = new CircleShape();
            axisShape.setRadius(0.02f);
            axisShape.m_p.set(0, 0);

            FixtureDef axisFixture = new FixtureDef();
            axisFixture.shape = axisShape;
            axis.createFixture(axisFixture);
        }

        private void initReel() {
            BodyDef reelDef = new BodyDef();
            reelDef.type = BodyType.DYNAMIC;
            reelDef.position = new Vec2(3, 3);
            reel = world.createBody(reelDef);

            FixtureDef fixture = new FixtureDef();
            fixture.friction = 0.5f;
            fixture.restitution = 0.4f;
            fixture.density = 1;

            int parts = 30;
            for (int i = 0; i < parts; ++i) {
                PolygonShape shape = new PolygonShape();
                double angle1 = i / (double) parts * 2 * Math.PI;
                double x1 = 2.7 * Math.cos(angle1);
                double y1 = 2.7 * Math.sin(angle1);
                double angle2 = (i + 1) / (double) parts * 2 * Math.PI;
                double x2 = 2.7 * Math.cos(angle2);
                double y2 = 2.7 * Math.sin(angle2);
                double angle = (angle1 + angle2) / 2;
                double x = 0.01 * Math.cos(angle);
                double y = 0.01 * Math.sin(angle);

                shape.set(new Vec2[] { new Vec2((float) x1, (float) y1), new Vec2((float) x2, (float) y2),
                        new Vec2((float) (x2 - x), (float) (y2 - y)), new Vec2((float) (x1 - x), (float) (y1 - y)) }, 4);
                fixture.shape = shape;
                reel.createFixture(fixture);
            }
        }

        private void initBalls() {
            float ballRadius = 0.15f;

            BodyDef ballDef = new BodyDef();
            ballDef.type = BodyType.DYNAMIC;
            FixtureDef fixtureDef = new FixtureDef();
            fixtureDef.friction = 0.3f;
            fixtureDef.restitution = 0.3f;
            fixtureDef.density = 0.2f;
            CircleShape shape = new CircleShape();
            shape.m_radius = ballRadius;
            fixtureDef.shape = shape;

            for (int i = 0; i < 5; ++i) {
                for (int j = 0; j < 5; ++j) {
                    float x = (j + 0.5f) * (ballRadius * 2 + 0.01f);
                    float y = (i + 0.5f) * (ballRadius * 2 + 0.01f);
                    ballDef.position.x = 3 + x;
                    ballDef.position.y = 3 + y;
                    Body body = world.createBody(ballDef);
                    body.createFixture(fixtureDef);

                    ballDef.position.x = 3 - x;
                    ballDef.position.y = 3 + y;
                    body = world.createBody(ballDef);
                    body.createFixture(fixtureDef);

                    ballDef.position.x = 3 + x;
                    ballDef.position.y = 3 - y;
                    body = world.createBody(ballDef);
                    body.createFixture(fixtureDef);

                    ballDef.position.x = 3 - x;
                    ballDef.position.y = 3 - y;
                    body = world.createBody(ballDef);
                    body.createFixture(fixtureDef);
                }
            }
        }

        private void joinReelToAxis() {
            RevoluteJointDef jointDef = new RevoluteJointDef();
            jointDef.bodyA = axis;
            jointDef.bodyB = reel;
            world.createJoint(jointDef);
        }

        public void calculate() {
            long currentTime = System.currentTimeMillis();
            int timeToCalculate = (int) (currentTime - lastCalculated);
            long relativeTime = currentTime - startTime;
            while (timeToCalculate > 10) {
                int period = (int) ((relativeTime + 5000) / 10000);
                reel.applyTorque(period % 2 == 0 ? 8f : -8f);
                world.step(0.01f, 20, 40);
                lastCalculated += 10;
                timeToCalculate -= 10;
            }
        }

        public int timeUntilNextStep() {
            return (int) Math.max(0, lastCalculated + 10 - System.currentTimeMillis());
        }

        public World getWorld() {
            return world;
        }
    }


    @Test
    public void testSceneAndRun() {
        Scene theScene = new Scene();
        theScene.calculate();
    }

    @Test
    public void testLinkDefaultWorld() {
        DefaultWorldPool thePool = new DefaultWorldPool(10, 10);
    }

    @Test
    public void testLinkShapeType() throws TRuntimeException {
        ShapeType[] theTypes = ShapeType.values();
        Assert.assertEquals(4, theTypes.length, 0);
    }
}

/**


 [sertic@sertic normal]$ javap -c -p DefaultWorldPool\$1
 DefaultWorldPool$10.class  DefaultWorldPool$11.class  DefaultWorldPool$12.class  DefaultWorldPool$13.class  DefaultWorldPool$1.class
 [sertic@sertic normal]$ javap -c -p DefaultWorldPool\$1.class
 Compiled from "DefaultWorldPool.java"
 class org.jbox2d.pooling.normal.DefaultWorldPool$1 extends org.jbox2d.pooling.normal.MutableStack<org.jbox2d.dynamics.contacts.Contact> {
 final org.jbox2d.pooling.normal.DefaultWorldPool this$0;

 org.jbox2d.pooling.normal.DefaultWorldPool$1(org.jbox2d.pooling.normal.DefaultWorldPool, int);
 Code:
 0: aload_0
 1: aload_1
 2: putfield      #1                  // Field this$0:Lorg/jbox2d/pooling/normal/DefaultWorldPool;
 5: aload_0
 6: iload_2
 7: invokespecial #2                  // Method org/jbox2d/pooling/normal/MutableStack."<init>":(I)V
 10: return

 protected org.jbox2d.dynamics.contacts.Contact newInstance();
 Code:
 0: new           #3                  // class org/jbox2d/dynamics/contacts/PolygonContact
 3: dup
 4: aload_0
 5: getfield      #1                  // Field this$0:Lorg/jbox2d/pooling/normal/DefaultWorldPool;
 8: invokestatic  #4                  // Method org/jbox2d/pooling/normal/DefaultWorldPool.access$000:(Lorg/jbox2d/pooling/normal/DefaultWorldPool;)Lorg/jbox2d/pooling/IWorldPool;
 11: invokespecial #5                  // Method org/jbox2d/dynamics/contacts/PolygonContact."<init>":(Lorg/jbox2d/pooling/IWorldPool;)V
 14: areturn

 protected java.lang.Object newInstance();
 Code:
 0: aload_0
 1: invokevirtual #6                  // Method newInstance:()Lorg/jbox2d/dynamics/contacts/Contact;
 4: areturn


 */