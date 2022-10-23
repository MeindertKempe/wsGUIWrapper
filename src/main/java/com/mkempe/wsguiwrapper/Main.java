/*
 * SPDX-License-Identifier: BSD-3-Clause
 *
 * Copyright (c) 2022 Meindert Kempe
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     1. Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *     2. Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *     3. Neither the name of the copyright holder nor the names of its contributors
 *        may be used to endorse or promote products derived from this software
 *        without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 */

package com.mkempe.wsguiwrapper;

import com.mkempe.wsguiwrapper.asm.IOServerAdapter;
import com.mkempe.wsguiwrapper.asm.LedMatrixAdapter;
import com.mkempe.wsguiwrapper.asm.wsDisplayGuiAdapter;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;


public class Main {

    public static void main(String[] args) {
        int port = Settings.getInstance().getPort();
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-p":
                case "--port":
                    try {
                        port = Integer.parseInt(args[++i]);
                    } catch (NumberFormatException e) {
                        System.err.println("Invalid port number: " + args[i]);
                    }
                    break;
                default:
                    System.err.println("Unknown argument: " + args[i]);
            }
        }


        ClassWriter wsDisplayCw = null;
        ClassWriter ioServerCw = null;
        ClassWriter ledMatrixCw = null;
        try {
            // Transform wsDisplayGUI
            ClassReader wsDisplayCr = new ClassReader("wsGUI.wsDisplayGUI");
//            wsDisplayCw = new ClassWriter(wsDisplayCr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            wsDisplayCw = new ClassWriter(wsDisplayCr, 0);
            wsDisplayCr.accept(new wsDisplayGuiAdapter(wsDisplayCw), 0);

            // Transform IOServer
            ClassReader ioServerCr = new ClassReader("wsGUI.IOServer");
//            ioServerCw = new ClassWriter(ioServerCr, ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
            ioServerCw = new ClassWriter(ioServerCr, 0);
            ioServerCr.accept(new IOServerAdapter(ioServerCw), 0);

            // Transform LedMatrix
            ClassReader ledMatrixCr = new ClassReader("modules.LedMatrix");
            ledMatrixCw = new ClassWriter(ledMatrixCr, 0);
            ledMatrixCr.accept(new LedMatrixAdapter(ledMatrixCw), 0);
        } catch (IOException | NullPointerException e) {
            System.err.println("Failed to transform bytecode");
            e.printStackTrace();
        }


        try {
            // Use reflection to access ClassLoader defineClass
            ClassLoader cl = ClassLoader.getSystemClassLoader();
            Class<?> cls = Class.forName("java.lang.ClassLoader");
            Method defineClass = cls.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class);
            defineClass.setAccessible(true);

            // Define modified classes
            Object[] clsArgs;
            if (wsDisplayCw != null) {
                clsArgs = new Object[]{"wsGUI.wsDisplayGUI", wsDisplayCw.toByteArray(), 0, wsDisplayCw.toByteArray().length};
                defineClass.invoke(cl, clsArgs);
            }
            if (ioServerCw != null) {
                clsArgs = new Object[]{"wsGUI.IOServer", ioServerCw.toByteArray(), 0, ioServerCw.toByteArray().length};
                defineClass.invoke(cl, clsArgs);
            }
            if (ledMatrixCw != null) {
                clsArgs = new Object[]{"modules.LedMatrix", ledMatrixCw.toByteArray(), 0, ledMatrixCw.toByteArray().length};
                defineClass.invoke(cl, clsArgs);
            }

            defineClass.setAccessible(false);
        } catch (NoSuchMethodException | ClassNotFoundException | IllegalAccessException |
                 InvocationTargetException e) {
            System.err.println("Couldn't access ClassLoader");
            e.printStackTrace();
        }

        // Set new port number using reflection
        try {
            Field portField = wsGUI.IOServer.class.getDeclaredField("port");
            portField.setAccessible(true);
            portField.setInt(null, port);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            System.err.println("Couldn't access port number");
            e.printStackTrace();
        }

        // Call original jar main function.
        wsGUI.wsDisplayGUI.main(null);
    }
}