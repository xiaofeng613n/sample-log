package common;

//import source.logcategorydir.TailDirConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 */
public class BufferedRandomAccessFile extends RandomAccessFile {

    public final static int BufferSize = 1028 * 8;//TailDirConfig.readBufferSize;

    private byte[] cacheBuffer;

    private int availableRead;

    private int buffPos;

    public BufferedRandomAccessFile(String file, String mode) throws FileNotFoundException {

        super(file, mode);
        cacheBuffer = new byte[BufferSize];
        availableRead = 0;
        buffPos = 0;
    }


    @Override
    public int read() throws IOException {
        if (buffPos >= availableRead) {
            availableRead = batchReadChunk();
            if (availableRead == -1) {
                return -1;
            }
        }
        buffPos++;
        return cacheBuffer[buffPos - 1];
    }

    @Override
    public long getFilePointer() throws IOException {
        return super.getFilePointer() + buffPos;
    }

    @Override
    public void seek(long pos) throws IOException {
        if (availableRead != -1
                && pos < (super.getFilePointer() + availableRead)
                && pos > super.getFilePointer()) {
            Long diff = (pos - super.getFilePointer());
            if (diff < Integer.MAX_VALUE) {
                buffPos = diff.intValue();
            } else {
                throw new IOException("something wrong w/ seek");
            }
        } else {
            buffPos = 0;
            super.seek(pos);
            availableRead = batchReadChunk();
        }
    }


    public final String readLineUseDefault() throws IOException {
        StringBuilder input = new StringBuilder();
        int c = -1;
        boolean eol = false;

        while (!eol) {
            switch (c = read()) {
                case -1:
                case '\n':
                    eol = true;
                    break;
                case '\r':
                    eol = true;
                    long cur = getFilePointer();
                    if ((read()) != '\n') {
                        seek(cur);
                    }
                    break;
                default:
                    input.append((char)c);
                    break;
            }
        }

        if ((c == -1) && (input.length() == 0)) {
            return null;
        }
        return input.toString();

    }

    @Override
    public synchronized  void close() throws IOException {
        if (buffPos > 0 && availableRead != -1) {
            super.seek(getFilePointer());
        }
        super.close();
    }

    private synchronized int batchReadChunk() throws IOException {
        long pos = super.getFilePointer() + buffPos;

        super.seek(pos);
        int read = super.read(cacheBuffer);
        buffPos = 0;
        super.seek(pos);

        return read;
    }
}
