package com.meituan.robust.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

public class InfoWriter {
    /**
     * infoFile, output info
     */
    protected final String        infoPath;
    protected final File          infoFile;

    /**
     * 首次使用时初始化
     */
    protected Writer infoWrite;

    public InfoWriter(String infoPath) throws IOException {
        this.infoPath = infoPath;

        if (infoPath != null) {
            this.infoFile = new File(infoPath);
            if (!infoFile.getParentFile().exists()) {
                infoFile.getParentFile().mkdirs();
            }
        } else {
            this.infoFile = null;
        }

    }

    public void writeLinesToInfoFile(List<String> lines) throws IOException {
        for (String line : lines) {
            writeLineToInfoFile(line);
        }
    }

    public void writeLineToInfoFile(String line) {
        if (infoPath == null || line == null || line.length() == 0) {
            return;
        }
        try {
            checkWriter();
            infoWrite.write(line);
            infoWrite.write("\n");
            infoWrite.flush();
        } catch (Exception e) {
            throw new RuntimeException("write info file error, infoPath:" + infoPath + " content:" + line, e);
        }
    }

    private void checkWriter() throws IOException {
        if (infoWrite == null) {
            this.infoWrite = new BufferedWriter(new FileWriter(infoFile, false));
        }

    }

    public void close() {
        try {
            if (infoWrite != null) infoWrite.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
