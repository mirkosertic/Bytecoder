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
import org.jbox2d.collision.AABB;
import org.jbox2d.collision.Collision;
import org.jbox2d.collision.Distance;
import org.jbox2d.collision.Manifold;
import org.jbox2d.collision.ManifoldPoint;
import org.jbox2d.collision.broadphase.DynamicTree;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.collision.shapes.ShapeType;
import org.jbox2d.common.*;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.jbox2d.dynamics.FixtureDef;
import org.jbox2d.dynamics.SolverData;
import org.jbox2d.dynamics.World;
import org.jbox2d.dynamics.contacts.CircleContact;
import org.jbox2d.dynamics.contacts.Contact;
import org.jbox2d.dynamics.joints.RevoluteJointDef;
import org.jbox2d.dynamics.joints.WeldJoint;
import org.jbox2d.dynamics.joints.WheelJoint;
import org.jbox2d.pooling.IDynamicStack;
import org.jbox2d.pooling.IWorldPool;
import org.jbox2d.pooling.arrays.IntArray;
import org.jbox2d.pooling.arrays.Vec2Array;
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
                System.out.println("New reel part");
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

    static class TestSolver {

        private IWorldPool pool;
        private float m_frequencyHz;
        private float m_dampingRatio;
        private float m_bias;

        // Solver shared
        private Vec2 m_localAnchorA;
        private Vec2 m_localAnchorB;
        private float m_referenceAngle;
        private float m_gamma;
        private Vec3 m_impulse;


        // Solver temp
        private int m_indexA;
        private int m_indexB;
        private Vec2 m_rA;
        private Vec2 m_rB;
        private Vec2 m_localCenterA;
        private Vec2 m_localCenterB;
        private float m_invMassA;
        private float m_invMassB;
        private float m_invIA;
        private float m_invIB;
        private Mat33 m_mass;

        public void solveVelocityConstraints(final SolverData data) {
            Vec2 vA = data.velocities[m_indexA].v;
            float wA = data.velocities[m_indexA].w;
            Vec2 vB = data.velocities[m_indexB].v;
            float wB = data.velocities[m_indexB].w;

            float mA = m_invMassA, mB = m_invMassB;
            float iA = m_invIA, iB = m_invIB;

            final Vec2 Cdot1 = pool.popVec2();
            final Vec2 P = pool.popVec2();
            final Vec2 temp = pool.popVec2();
            if (m_frequencyHz > 0.0f) {
                float Cdot2 = wB - wA;

                float impulse2 = -m_mass.ez.z * (Cdot2 + m_bias + m_gamma * m_impulse.z);
                m_impulse.z += impulse2;

                wA -= iA * impulse2;
                wB += iB * impulse2;

                Vec2.crossToOutUnsafe(wB, m_rB, Cdot1);
                Vec2.crossToOutUnsafe(wA, m_rA, temp);
                Cdot1.addLocal(vB).subLocal(vA).subLocal(temp);

                final Vec2 impulse1 = P;
                Mat33.mul22ToOutUnsafe(m_mass, Cdot1, impulse1);
                impulse1.negateLocal();

                m_impulse.x += impulse1.x;
                m_impulse.y += impulse1.y;

                vA.x -= mA * P.x;
                vA.y -= mA * P.y;
                wA -= iA * Vec2.cross(m_rA, P);

                //vB.x += mB * P.x;
                //vB.y += mB * P.y;
                // wB += iB * Vec2.cross(m_rB, P);
            } else {
                Vec2.crossToOutUnsafe(wA, m_rA, temp);
                Vec2.crossToOutUnsafe(wB, m_rB, Cdot1);
                Cdot1.addLocal(vB).subLocal(vA).subLocal(temp);
                float Cdot2 = wB - wA;

                final Vec3 Cdot = pool.popVec3();
                Cdot.set(Cdot1.x, Cdot1.y, Cdot2);

                final Vec3 impulse = pool.popVec3();
                Mat33.mulToOutUnsafe(m_mass, Cdot, impulse);
                impulse.negateLocal();
                m_impulse.addLocal(impulse);

                P.set(impulse.x, impulse.y);

                vA.x -= mA * P.x;
                vA.y -= mA * P.y;
                // wA -= iA * (Vec2.cross(m_rA, P) + impulse.z);

                vB.x += mB * P.x;
                vB.y += mB * P.y;
                //wB += iB * (Vec2.cross(m_rB, P) + impulse.z);

                //pool.pushVec3(2);
            }

            data.velocities[m_indexA].v.set(vA);
            data.velocities[m_indexA].w = wA;
            data.velocities[m_indexB].v.set(vB);
            data.velocities[m_indexB].w = wB;

            pool.pushVec2(3);
        }
    }

    static class TestSolver2 {

        private IWorldPool pool;
        private float m_frequencyHz;
        private float m_dampingRatio;
        private float m_bias;

        // Solver shared
        private Vec2 m_localAnchorA;
        private Vec2 m_localAnchorB;
        private float m_referenceAngle;
        private float m_gamma;
        private Vec3 m_impulse;


        // Solver temp
        private int m_indexA;
        private int m_indexB;
        private Vec2 m_rA;
        private Vec2 m_rB;
        private Vec2 m_localCenterA;
        private Vec2 m_localCenterB;
        private float m_invMassA;
        private float m_invMassB;
        private float m_invIA;
        private float m_invIB;
        private Mat33 m_mass;

        public void solveVelocityConstraints(final SolverData data) {
            Vec2 vA = data.velocities[m_indexA].v;
            float wA = data.velocities[m_indexA].w;
            Vec2 vB = data.velocities[m_indexB].v;
            float wB = data.velocities[m_indexB].w;

            float mA = m_invMassA, mB = m_invMassB;
            float iA = m_invIA, iB = m_invIB;

            final Vec2 Cdot1 = pool.popVec2();
            final Vec2 P = pool.popVec2();
            final Vec2 temp = pool.popVec2();
            if (m_frequencyHz > 0.0f) {
                float Cdot2 = wB - wA;

                float impulse2 = -m_mass.ez.z * (Cdot2 + m_bias + m_gamma * m_impulse.z);
                m_impulse.z += impulse2;

                wA -= iA * impulse2;
                wB += iB * impulse2;

                Vec2.crossToOutUnsafe(wB, m_rB, Cdot1);
                Vec2.crossToOutUnsafe(wA, m_rA, temp);
                Cdot1.addLocal(vB).subLocal(vA).subLocal(temp);

                final Vec2 impulse1 = P;
                Mat33.mul22ToOutUnsafe(m_mass, Cdot1, impulse1);
                impulse1.negateLocal();

                m_impulse.x += impulse1.x;
                m_impulse.y += impulse1.y;

                vA.x -= mA * P.x;
                vA.y -= mA * P.y;
                wA -= iA * Vec2.cross(m_rA, P);

                vB.x += mB * P.x;
                vB.y += mB * P.y;
                wB += iB * Vec2.cross(m_rB, P);
            } else {
                Vec2.crossToOutUnsafe(wA, m_rA, temp);
                Vec2.crossToOutUnsafe(wB, m_rB, Cdot1);
                Cdot1.addLocal(vB).subLocal(vA).subLocal(temp);
                float Cdot2 = wB - wA;

                final Vec3 Cdot = pool.popVec3();
                Cdot.set(Cdot1.x, Cdot1.y, Cdot2);

                final Vec3 impulse = pool.popVec3();
                Mat33.mulToOutUnsafe(m_mass, Cdot, impulse);
                impulse.negateLocal();
                m_impulse.addLocal(impulse);

                P.set(impulse.x, impulse.y);

                vA.x -= mA * P.x;
                vA.y -= mA * P.y;
                wA -= iA * (Vec2.cross(m_rA, P) + impulse.z);

                vB.x += mB * P.x;
                vB.y += mB * P.y;
                wB += iB * (Vec2.cross(m_rB, P) + impulse.z);

                pool.pushVec3(2);
            }

            //    data.velocities[m_indexA].v.set(vA);
            data.velocities[m_indexA].w = wA;
            //    data.velocities[m_indexB].v.set(vB);
            data.velocities[m_indexB].w = wB;

            pool.pushVec2(3);
        }
    }

    public class TestShape {

        private final Vec2 pool1 = new Vec2();
        private final Vec2 pool2 = new Vec2();
        public int m_count;
        /**
         * Local position of the shape centroid in parent body frame.
         */
        public final Vec2 m_centroid = new Vec2();

        /**
         * The vertices of the shape. Note: use getVertexCount(), not m_vertices.length, to get number of
         * active vertices.
         */
        public final Vec2 m_vertices[];

        /**
         * The normals of the shape. Note: use getVertexCount(), not m_normals.length, to get number of
         * active normals.
         */
        public final Vec2 m_normals[];

        public TestShape() {

            m_count = 0;
            m_vertices = new Vec2[Settings.maxPolygonVertices];
            for (int i = 0; i < m_vertices.length; i++) {
                m_vertices[i] = new Vec2();
            }
            m_normals = new Vec2[Settings.maxPolygonVertices];
            for (int i = 0; i < m_normals.length; i++) {
                m_normals[i] = new Vec2();
            }
            m_centroid.setZero();
        }

        public final void complexlogic(final Vec2[] verts, final int num, final Vec2Array vecPool,
                              final IntArray intPool) {

            int i0 = 1;
            int[] hull = new int[Settings.maxPolygonVertices];

            int m = 0;
            int ih = i0;

            int counter = 0;
            while (true) {

                counter++;
                if (counter>5) {
                    break;
                }

                hull[m] = ih;

                int ie = 0;
                for (int j = 1; j < num; ++j) {

                    if (ie == ih) {
                        ie = j;
                        continue;
                    }

                    Vec2 r = pool1.set(verts[ie]).subLocal(verts[hull[m]]);
                    Vec2 v = pool2.set(verts[j]).subLocal(verts[hull[m]]);

                    float c = Vec2.cross(r, v);
                    if (c < 0.0f) {
                        ie = j;
                    }

                    if (c == 0.0f && v.lengthSquared() > r.lengthSquared()) {
                        ie = j;
                    }
                }

                ++m;
                ih = ie;

                System.out.println("End of loop");
                System.out.println(ie);
                System.out.println(i0);
                if (ie == i0) {
                    break;
                }
            }
            System.out.println("Finished");
        }

        private void computeCentroidToOut(Vec2[] m_vertices, int m_count, Vec2 m_centroid) {
        }

        private void setAsBox(float v, float v1) {
        }
    }

    @Test
    public void testShape() {
        TestShape theShape = new TestShape();
        theShape.complexlogic(new Vec2[] {new Vec2(-1, -1), new Vec2(1, -1), new Vec2(1, 1), new Vec2(-1, 1)}, 4, null, null);
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
        int counter = 0;
        while (timeToCalculate > 10 && counter < 10) {
            System.out.println("Calculating");
            System.out.println((long) timeToCalculate);
            int period = (int) ((relativeTime + 5000) / 10000);
            theScene.reel.applyTorque(period % 2 == 0 ? 8f : -8f);
            theScene.world.step(0.01f, 20, 40);
            lastCalculated += 10;
            timeToCalculate -= 10;
            System.out.println("done");
            counter++;
        }
        long theTotalDuration = System.currentTimeMillis() - theStart;
        System.out.println("Ran");
        System.out.println(theTotalDuration);
    }

    @Test
    public void testLinkDefaultWorld() {
        DefaultWorldPool thePool = new DefaultWorldPool(1, 1);
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

    @Test
    public void testCircleShape() {
        CircleShape axisShape = new CircleShape();
        axisShape.setRadius(0.02f);
        axisShape.m_p.set(0, 0);
    }

    @Test
    public void testNewWorld() {
        World world = new World(new Vec2(0, -9.8f));

        BodyDef axisDef = new BodyDef();
        axisDef.type = BodyType.STATIC;
        axisDef.position = new Vec2(3, 3);
        Body axis = world.createBody(axisDef);

        CircleShape axisShape = new CircleShape();
        axisShape.setRadius(0.02f);
        axisShape.m_p.set(0, 0);

        FixtureDef axisFixture = new FixtureDef();
        axisFixture.shape = axisShape;
        axis.createFixture(axisFixture);

    }
}