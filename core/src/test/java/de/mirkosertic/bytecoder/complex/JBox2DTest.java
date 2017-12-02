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
package de.mirkosertic.bytecoder.complex;

import org.jbox2d.callbacks.ContactImpulse;
import org.jbox2d.callbacks.ContactListener;
import org.jbox2d.collision.*;
import org.jbox2d.collision.broadphase.DynamicTree;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.CircleContact;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.pooling.IDynamicStack;
import org.jbox2d.pooling.normal.DefaultWorldPool;
import org.jbox2d.pooling.normal.OrderedStack;
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

            world.setContactListener(new ContactListener() {
                @Override
                public void beginContact(Contact contact) {
                    System.out.println("Begin contact");
                }

                @Override
                public void endContact(Contact contact) {
                    System.out.println("end contact");
                }

                @Override
                public void preSolve(Contact contact, Manifold oldManifold) {
                }

                @Override
                public void postSolve(Contact contact, ContactImpulse impulse) {
                }
            });
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
    public void testCircleContactStack() {
        System.out.println("A");
        DefaultWorldPool thePool = new DefaultWorldPool(10, 10);
        System.out.println("B");
        IDynamicStack<Contact> theCircleStack = thePool.getCircleContactStack();
        System.out.println("C");
        Contact theContact = theCircleStack.pop();
        System.out.println("D");
        Assert.assertTrue(theContact instanceof CircleContact);
        System.out.println("E");
    }

    @Test
    public void testSceneAndRun() {
        long theStart = System.currentTimeMillis();
        Scene theScene = new Scene();
        long startTime = 0;
        long currentTime = 600;
        long lastCalculated = 500;
        int timeToCalculate = (int) (currentTime - lastCalculated);
        long relativeTime = currentTime - startTime;
        while (timeToCalculate > 10) {
            System.out.println("Calculating");
            System.out.println((long) timeToCalculate);
            int period = (int) ((relativeTime + 5000) / 10000);
            theScene.reel.applyTorque(period % 2 == 0 ? 8f : -8f);
            theScene.world.step(0.01f, 20, 40);
            lastCalculated += 10;
            timeToCalculate -= 10;
            System.out.println("done");
        }
        long theTotalDuration = System.currentTimeMillis() - theStart;
        System.out.println("Ran");
        System.out.println(theTotalDuration);
    }

    @Test
    public void testLinkDefaultWorld() {
        Class theLalal = DefaultWorldPool.class;
        //DefaultWorldPool thePool = new DefaultWorldPool(1, 1);

        OrderedStack theStack = new OrderedStack<Vec2>(1, 1) {
            @Override
            protected Vec2 newInstance() {
                //return new Vec2();
                return null;
            }
        };
    }

    @Test
    public void testDistance() {
        Distance theDistance = new Distance();
    }

    @Test
    public void testCollision() {
        Collision theCollision = new Collision(null);
    }

    @Test
    public void testLinkShapeType() {
        ShapeType[] theTypes = ShapeType.values();
        Assert.assertEquals(4, theTypes.length, 0);
    }

    @Test
    public void testManifoldConstructor() {
        ManifoldPoint thePoint = new ManifoldPoint();
        Assert.assertNotNull(thePoint.id);
    }

    @Test
    public void testDynamicTree() {
        DynamicTree theTree = new DynamicTree();
        theTree.createProxy(new AABB(), "TEST");
    }
}