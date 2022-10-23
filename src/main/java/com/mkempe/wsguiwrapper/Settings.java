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

import org.tomlj.Toml;
import org.tomlj.TomlParseResult;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Settings {
    private static final Settings INSTANCE = new Settings();

    private int port = 8082;
    private String background = null;
    private Color textColor = null;
    private Color matrixColor = null;

    private Settings() {
        Path config = Paths.get("config.toml");
        if (!config.toFile().exists()) {
            try {
                InputStream is = Settings.class.getResourceAsStream("/defaultConfig.toml");
                OutputStream os = Files.newOutputStream(config.toFile().toPath());
                byte[] buff = new byte[1024];
                int length;

                while ((length = is.read(buff)) > 0) {
                    os.write(buff, 0, length);
                }

                is.close();
                os.close();
            } catch (IOException e) {
                System.err.println("Config: Error writing default config file");
                return;
            }
        }

        TomlParseResult toml;
        try {
            toml = Toml.parse(config);
        } catch (IOException e) {
            System.err.println("Config: Error reading config file");
            return;
        }
        toml.errors().forEach(error -> System.err.println("Config: " + error.toString()));

        Long port = toml.getLong("network.port");
        if (port != null && port < Integer.MAX_VALUE && port > 0) {
            this.port = port.intValue();
        }

        //TODO verify that file exists
        String background = toml.getString("ui.background");
        if (background != null && !background.isEmpty()) {
            this.background = background;
        }


        //TODO make this nicer in the config file
        Long textColorR = toml.getLong("ui.text_color.r");
        Long textColorG = toml.getLong("ui.text_color.g");
        Long textColorB = toml.getLong("ui.text_color.b");
        if (verifyColor(textColorR) && verifyColor(textColorG) && verifyColor(textColorB)) {
            this.textColor = new Color(textColorR.intValue(), textColorG.intValue(), textColorB.intValue());
        }

        Long matrixColorR = toml.getLong("ui.matrix_color.r");
        Long matrixColorG = toml.getLong("ui.matrix_color.g");
        Long matrixColorB = toml.getLong("ui.matrix_color.b");
        if (verifyColor(matrixColorR) && verifyColor(matrixColorG) && verifyColor(matrixColorB)) {
            this.matrixColor = new Color(matrixColorR.intValue(), matrixColorG.intValue(), matrixColorB.intValue());
        }
    }

    public boolean verifyColor(Long color) {
        return color != null && color <= 0xff && color >= 0;
    }

    public static Settings getInstance() {
        return INSTANCE;
    }

    public int getPort() {
        return port;
    }

    public String getBackground() {
        return background;
    }

    public Color getTextColor() {
        return textColor;
    }

    public Color getMatrixColor() {
        return matrixColor;
    }
}
