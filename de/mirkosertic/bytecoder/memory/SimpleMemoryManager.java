/*
 * Copyright 2020 Mirko Sertic
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
package de.mirkosertic.bytecoder.memory;

import java.io.PrintStream;

public class SimpleMemoryManager {

    static class Chunk {
        int position;
        int size;
        Chunk prev;
        Chunk next;
        int gcEpoc;

        public Chunk(final int position, final int size, final Chunk prev, final Chunk next, final int gcEpoc) {
            this.position = position;
            this.size = size;
            this.prev = prev;
            this.next = next;
            this.gcEpoc = gcEpoc;
        }
    }

    private Chunk freeList;
    private Chunk allocatedList;

    public SimpleMemoryManager(final int totalMemory) {
        freeList = new Chunk(0, totalMemory, null, null, 0);
        allocatedList = null;
    }

    public int freeMemory() {
        int result = 0;
        Chunk current = freeList;
        while (current != null) {
            result += current.size;
            current = current.next;
        }
        return result;
    }

    public int allocated() {
        int result = 0;
        Chunk current = allocatedList;
        while (current != null) {
            result += current.size;
            current = current.next;
        }
        return result;
    }

    public int malloc(final int size) {
        return malloc_internal(size + 16) + 16;
    }

    private int malloc_internal(final int size) {
        Chunk current = freeList;
        while (current != null) {
            if (current.size >= size) {
                final int remaining = current.size - size;
                if (remaining < 16) {
                    // We have an exact match
                    final Chunk allocatedListHead = allocatedList;
                    final Chunk allocated = new Chunk(current.position, current.size, null, allocatedListHead, 0);
                    if (allocatedListHead != null) {
                        allocatedListHead.prev = allocated;
                    }
                    allocatedList = allocated;

                    if (current.prev != null) {
                        current.prev.next = current.next;
                    } else {
                        freeList = current.next;
                    }

                    if (current.next != null) {
                        current.next.prev = current.prev;
                    }

                    return allocated.position;
                }
                if (remaining > 16) {
                    // We can use split this chunk

                    // We create the allocated chunk by appending it to the list of
                    // allocated blocks
                    final Chunk allocatedListHead = allocatedList;
                    final Chunk allocated = new Chunk(current.position, size, null, allocatedListHead, 0);
                    if (allocatedListHead != null) {
                        allocatedListHead.prev = allocated;
                    }
                    allocatedList = allocated;

                    // Now we split the original block
                    if (current.prev != null) {
                        final Chunk newFree = new Chunk(current.position + size, remaining, current.prev, current.next, 0);
                        current.prev.next = newFree;
                        if (current.next != null) {
                            current.next.prev = newFree;
                        }
                    } else {
                        final Chunk newFree = new Chunk(current.position + size, remaining, null, current.next, 0);
                        if (current.next !=  null) {
                            current.next.prev = newFree;
                        }
                        freeList = newFree;
                    }

                    return allocated.position;
                }
            }
            current = current.next;
        }
        return -1;
    }

    public void free(final int position) {
        free_internal(position - 16);
    }

    private void free_internal(final int position) {
        Chunk current = allocatedList;
        while (current != null) {
            if (current.position == position) {

                // Remove from allocation list
                if (current.prev != null) {
                    current.prev.next = current.next;
                } else {
                    allocatedList = current.next;
                }

                if (current.next != null) {
                    current.next.prev = current.prev;
                }

                // Prepend to the list of free blocks
                current.next = freeList;
                freeList.prev = current;
                current.prev = null;
                freeList = current;

                return;
            }

            current = current.next;
        }
    }

    public void GC() {
        Chunk current = allocatedList;
        while (current != null) {
            final Chunk next = current.next;

            free_internal(current.position);

            current = next;
        }
    }

    public void printDebug(final PrintStream ps) {
        ps.println("digraph memory {");

        ps.println("    start_free[label=\"Start of free list\"];");
        ps.println("    start_allocated[label=\"Start of allocated list\"];");

        ps.println("start_free -> free_" + System.identityHashCode(freeList) + ";");
        ps.println("start_allocated -> allocated_" + System.identityHashCode(allocatedList) + ";");
        {
            ps.println("subgraph cluster_0 {");
            ps.println("   label=\"Free list\";");
            ps.println("   style=filled;");
            ps.println("   color=chartreuse3;");
            Chunk current = freeList;
            while (current != null) {

                ps.print("   free_");
                ps.print(System.identityHashCode(current));
                ps.print("[label=\"Pos. : ");
                ps.print(current.position);
                ps.print(" Size: ");
                ps.print(current.size);
                ps.println("\"];");

                if (current.next != null) {
                    ps.print("   free_");
                    ps.print(System.identityHashCode(current));
                    ps.print(" -> free_");
                    ps.print(System.identityHashCode(current.next));
                    ps.println(" [label=\"Next\",color=blue4];");
                }
                if (current.prev != null) {
                    ps.print("   free_");
                    ps.print(System.identityHashCode(current));
                    ps.print(" -> free_");
                    ps.print(System.identityHashCode(current.prev));
                    ps.println(" [label=\"Prev\"];");
                }

                current = current.next;
            }
            ps.println("}");
        }

        {
            ps.println("subgraph cluster_1 {");
            ps.println("   label=\"Allocated list\";");
            ps.println("   style=filled;");
            ps.println("   color=coral3;");
            Chunk current = allocatedList;
            while (current != null) {

                ps.print("   allocated_");
                ps.print(System.identityHashCode(current));
                ps.print("[label=\"Pos. : ");
                ps.print(current.position);
                ps.print(" Size: ");
                ps.print(current.size);
                ps.print(" GCEpoc: ");
                ps.print(current.gcEpoc);
                ps.println("\"];");

                if (current.next != null) {
                    ps.print("   allocated_");
                    ps.print(System.identityHashCode(current));
                    ps.print(" -> allocated_");
                    ps.print(System.identityHashCode(current.next));
                    ps.println(" [label=\"Next\",color=blue4];");
                }
                if (current.prev != null) {
                    ps.print("   allocated_");
                    ps.print(System.identityHashCode(current));
                    ps.print(" -> allocated_");
                    ps.print(System.identityHashCode(current.prev));
                    ps.println(" [label=\"Prev\"];");
                }

                current = current.next;
            }
            ps.println("}");
        }

        ps.println("}");
    }
}