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

package com.mkempe.wsguiwrapper.asm;

import com.mkempe.wsguiwrapper.Settings;
import org.objectweb.asm.*;

import java.awt.*;

import static org.objectweb.asm.Opcodes.*;

public class wsDisplayGuiAdapter extends ClassVisitor {
    private static final int api = ASM9;

    public wsDisplayGuiAdapter(ClassVisitor classVisitor) {
        super(api, classVisitor);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        if (name.equals("<init>") && descriptor.equals("()V")) {
            return new initMethodAdapter(api, mv);
        }
        if (name.equals("initComponents") && descriptor.equals("()V")) {
            return new initComponentsMethodAdapter(api, mv);
        }

        return mv;
    }

    protected static class initMethodAdapter extends MethodVisitor {

        public initMethodAdapter(int api, MethodVisitor methodVisitor) {
            super(api, methodVisitor);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (owner.equals("java/lang/StringBuilder") && name.equals("toString")) {
                // Append port number to string displaying hostname
                mv.visitLdcInsn(":");
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
                mv.visitMethodInsn(INVOKESTATIC, "wsGUI/IOServer", "getPort", "()I", false);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false);
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }
    }

    private static class initComponentsMethodAdapter extends MethodVisitor {
        boolean foundImageIcon = false;

        public initComponentsMethodAdapter(int api, MethodVisitor mv) {
            super(api, mv);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            if (Settings.getInstance().getTextColor() != null && owner.equals("wsGUI/wsDisplayGUI") &&
                    name.equals("pack") && descriptor.equals("()V")) {

                // TODO change button border color, maybe segment display border as well (maybe remove borders?)


                // Change text color
                Color textColor = Settings.getInstance().getTextColor();
                mv.visitFieldInsn(GETFIELD, owner, "jLabel1", "Ljavax/swing/JLabel;");
                Util.insertColor(mv, textColor.getRed(), textColor.getGreen(), textColor.getBlue());
                mv.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/JLabel", "setForeground", "(Ljava/awt/Color;)V", false);

                mv.visitVarInsn(ALOAD, 0);
                mv.visitFieldInsn(GETFIELD, owner, "jLabel3", "Ljavax/swing/JLabel;");
                Util.insertColor(mv, textColor.getRed(), textColor.getGreen(), textColor.getBlue());
                mv.visitMethodInsn(INVOKEVIRTUAL, "javax/swing/JLabel", "setForeground", "(Ljava/awt/Color;)V", false);

                mv.visitVarInsn(ALOAD, 0);
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                return;
            }

            if (!foundImageIcon) {
                super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
            } else if (opcode == INVOKESPECIAL && owner.equals("javax/swing/ImageIcon") &&
                    name.equals("<init>") && descriptor.equals("(Ljava/net/URL;)V")) {
                foundImageIcon = false;
                mv.visitInsn(DUP);
                mv.visitLdcInsn(Settings.getInstance().getBackground());
                mv.visitMethodInsn(opcode, owner, name, "(Ljava/lang/String;)V", isInterface);
            }
        }


        @Override
        public void visitTypeInsn(int opcode, String type) {
            if (Settings.getInstance().getBackground() != null && opcode == NEW &&
                    type.equals("javax/swing/ImageIcon")) {
                foundImageIcon = true;
            }

            super.visitTypeInsn(opcode, type);
        }

        @Override
        public void visitInsn(int opcode) {
            if (!foundImageIcon)
                super.visitInsn(opcode);
        }

        @Override
        public void visitVarInsn(int opcode, int varIndex) {
            if (!foundImageIcon)
                super.visitVarInsn(opcode, varIndex);
        }

        @Override
        public void visitLdcInsn(Object value) {
            if (!foundImageIcon)
                super.visitLdcInsn(value);
        }
    }
}
