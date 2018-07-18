package com.rusel.RCTBluetoothSerial;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;

// String testdata = 'testdata';
// InputStream stream = new ByteArrayInputStream(exampleString.getBytes(StandardCharsets.UTF_8))
interface IBluetoothInputStreamProcessor {

    void onConnected(InputStream inputStream);
}

public class BluetoothFileSaver implements IBluetoothInputStreamProcessor {

    private final String mFileName;
    private InputStream mInputStream = null;
    private OutputStream mOutputStream = null;
    private RCTBluetoothSerialModule mModule = null;
    private long mFileSize;
    private int mFileSizeLoaded;
    private int mHeadersSize = 4;

    BluetoothFileSaver(String name, RCTBluetoothSerialModule module) {
        this.mModule = module;
        this.mFileName = name;
    }

    @Override
    public void onConnected(InputStream inputStream) {
        this.mInputStream = inputStream;
        byte[] buffer = new byte[1024];

        try {
            do {

                int bufferSize = mInputStream.read(buffer);

                if(bufferSize == -1)
                    continue; // or use break

                int offset = 0;
                if (mOutputStream == null) {
                    mOutputStream = new FileOutputStream(mFileName);
                    offset = mHeadersSize;
                    readHeaders(buffer);
                }

                mFileSizeLoaded += bufferSize - offset;
                mOutputStream.write(buffer, offset, bufferSize - offset);
                if (mModule != null) {
                    mModule.onFileChunkLoaded(this.getFileLoadPercent());
                }
            } while (mFileSize > mFileSizeLoaded);
            mOutputStream.flush();
            mOutputStream.close();
            if (mModule != null) {
                mModule.onFileLoaded();
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    private double getFileLoadPercent() {
        return mFileSizeLoaded > 0 ? mFileSizeLoaded * 100 / mFileSize : 0;
    }

    private void readHeaders(byte[] headers) {
        parseFileSize(headers);
    }

    private void parseFileSize(byte[] headers) {
        byte[] sizeBuffer = Arrays.copyOf(headers, mHeadersSize);
        this.mFileSize = bytesToLong(sizeBuffer);
    }

    private long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(mHeadersSize);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getInt();
    }
}

