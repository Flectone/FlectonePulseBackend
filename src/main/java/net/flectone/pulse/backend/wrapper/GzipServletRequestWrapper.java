package net.flectone.pulse.backend.wrapper;

import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class GzipServletRequestWrapper extends HttpServletRequestWrapper {

    private final byte[] body;

    public GzipServletRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
        body = decompress(request.getInputStream());
    }

    private byte[] decompress(InputStream inputStream) throws IOException {
        try (GZIPInputStream gzipIn = new GZIPInputStream(inputStream);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[1024];
            int len;
            while ((len = gzipIn.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            return out.toByteArray();
        }
    }

    @Override
    public ServletInputStream getInputStream() {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(body);

        return new ServletInputStream() {
            @Override
            public int read() {
                return byteStream.read();
            }

            @Override
            public boolean isFinished() {
                return byteStream.available() == 0;
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setReadListener(ReadListener readListener) {
            }
        };
    }

    @Override
    public BufferedReader getReader() {
        return new BufferedReader(new InputStreamReader(getInputStream()));
    }
}

